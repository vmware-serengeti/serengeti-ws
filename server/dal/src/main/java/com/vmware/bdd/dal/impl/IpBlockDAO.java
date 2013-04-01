/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.dal.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.IIpBlockDAO;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Functor;
import com.vmware.bdd.utils.Functor.Predicate;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Repository
public class IpBlockDAO extends BaseDAO<IpBlockEntity> implements IIpBlockDAO {

   private static final Logger logger = Logger.getLogger(IpBlockDAO.class);

   public static class EqualBlockTypePredicate implements Predicate<IpBlockEntity> {
      private final BlockType type;

      public EqualBlockTypePredicate(BlockType type) {
         this.type = type;
      }

      public static final Predicate<IpBlockEntity> IS_FREE =
            new EqualBlockTypePredicate(BlockType.FREE);
      public static final Predicate<IpBlockEntity> IS_ASSIGNED =
            new EqualBlockTypePredicate(BlockType.ASSIGNED);

      @Override
      public boolean evaluate(IpBlockEntity blk) {
         return blk.getType() == type;
      }
   }

   public static class IsTransientPredicate implements Predicate<IpBlockEntity> {
      public static final Predicate<IpBlockEntity> INSTANCE =
            new IsTransientPredicate();
      public static final Predicate<IpBlockEntity> NEGATE_INSTANCE =
            Functor.negate(new IsTransientPredicate());

      @Override
      public boolean evaluate(IpBlockEntity blk) {
         return blk.getId() == null;
      }
   }

   public static class EqualOwnerPredicate implements Predicate<IpBlockEntity> {
      private final Long ownerId;

      public EqualOwnerPredicate(Long ownerId) {
         this.ownerId = ownerId;
      }

      @Override
      public boolean evaluate(IpBlockEntity blk) {
         return blk.getOwnerId().equals(ownerId);
      }
   }

   @Override
   @Transactional
   public List<IpBlockEntity> merge(List<IpBlockEntity> ipBlocks, boolean ignoreOwner,
         boolean ignoreType, boolean silentWhenOverlap) {
      if (ipBlocks.size() <= 1) {
         return ipBlocks;
      }

      AuAssert.check(ignoreOwner || !ignoreType, "must ignore owner if type is ignored");
      boolean inHibernateSession =
         getSessionFactory().getCurrentSession().getTransaction().isActive();

      // sort in asc order by (beginIp, type, owner)
      Collections.sort(ipBlocks);

      List<IpBlockEntity> newBlocks = new ArrayList<IpBlockEntity>(ipBlocks.size());
      Iterator<IpBlockEntity> iter = ipBlocks.iterator();
      IpBlockEntity prev = iter.next();

      while (iter.hasNext()) {
         IpBlockEntity curr = iter.next();
         if (prev.canConcatWith(curr)) {
            if (prev.isOverlapedWith(curr)) {
               logger.warn("detected overlapped IP blocks: " + prev + ", " + curr);
               if (!silentWhenOverlap) {
                  throw NetworkException.OVERLAPPED_IP_BLOCKS(prev, curr);
               }
            }

            if ((ignoreOwner || prev.getOwnerId().equals(curr.getOwnerId()))
                  && (ignoreType || prev.getType() == curr.getType())) {
               prev.setEndIp(Math.max(curr.getEndIp(), prev.getEndIp()));

               /**
                * If curr is an persistent entity, delete it
                */
               if (curr.getId() != null) {
                  if (inHibernateSession) {
                     /**
                      * The input blocks must be Hibernate-aware entities.
                      */
                     delete(curr);
                  } else {
                     /**
                      * Too dangerous situation, should not allow merge detached
                      * entities outside an active Hibernate transaction.
                      */
                     AuAssert.unreachable();
                  }
               }
               continue;
            }
            /*
             * else skip to next, because the list is ordered by (beginIp,type,
             * owner)
             */
         }

         newBlocks.add(prev);
         prev = curr;
      }
      newBlocks.add(prev);

      return newBlocks;
   }
}