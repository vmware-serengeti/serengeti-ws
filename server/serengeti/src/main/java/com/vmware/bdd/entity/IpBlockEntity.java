/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Functor;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Functor.Predicate;
import com.vmware.bdd.utils.IpAddressUtil;

@Entity
@SequenceGenerator(name = "IdSequence",
      sequenceName = "ip_block_seq",
      allocationSize = 1)
@Table(name = "ip_block")
public class IpBlockEntity extends EntityBase implements Comparable<IpBlockEntity> {
   public enum BlockType {
      FREE, ASSIGNED
   }

   /*
    * must not be null and not in the range of valid sequence number
    */
   public static Long FREE_BLOCK_OWNER_ID = -1L;
   private static final Logger logger = Logger.getLogger(IpBlockEntity.class);

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "network_id", nullable = false)
   private NetworkEntity network;

   @Enumerated(EnumType.STRING)
   @Column(name = "type", nullable = false)
   private BlockType type;

   @Column(name = "owner_id", nullable = false)
   private Long ownerId;

   // an IP block is a closed interval
   @Column(name = "begin_ip", nullable = false)
   private Long beginIp;

   @Column(name = "end_ip", nullable = false)
   private Long endIp;

   public IpBlockEntity() {
   }

   public IpBlockEntity(NetworkEntity network, Long ownerId, BlockType type,
         long beginIp,
         long endIp) {
      this.network = network;
      this.ownerId = ownerId;
      this.type = type;
      this.beginIp = beginIp;
      this.endIp = endIp;

      validate();
   }

   public NetworkEntity getNetwork() {
      return network;
   }

   public void setNetwork(NetworkEntity network) {
      this.network = network;
   }

   public BlockType getType() {
      return type;
   }

   public void setType(BlockType type) {
      this.type = type;
   }

   public Long getOwnerId() {
      return ownerId;
   }

   public void setOwnerId(Long ownerId) {
      this.ownerId = ownerId;
   }

   public Long getBeginIp() {
      return beginIp;
   }

   public void setBeginIp(Long beginIp) {
      this.beginIp = beginIp;
   }

   public Long getEndIp() {
      return endIp;
   }

   public void setEndIp(Long endIp) {
      this.endIp = endIp;
   }

   public String getBeginAddress() {
      return IpAddressUtil.getAddressFromLong(beginIp).getHostAddress();
   }

   public String getEndAddress() {
      return IpAddressUtil.getAddressFromLong(endIp).getHostAddress();
   }

   public long getLength() {
      return endIp - beginIp + 1;
   }

   /**
    * order by (beginIp, type, owner, endIp)
    */
   @Override
   public int compareTo(IpBlockEntity o) {
      /**
       * compare by beginIp, this is important for merge and subtract method
       */
      int cmp = getBeginIp().compareTo(o.getBeginIp());
      if (cmp != 0) {
         return cmp;
      }

      cmp = getType().compareTo(o.getType());
      if (cmp != 0) {
         return cmp;
      }

      cmp = getOwnerId().compareTo(o.getOwnerId());
      if (cmp != 0) {
         return cmp;
      }

      cmp = getEndIp().compareTo(o.getEndIp());
      if (cmp != 0) {
         return cmp;
      }

      cmp = getNetwork().compareTo(o.getNetwork());
      return cmp;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((beginIp == null) ? 0 : beginIp.hashCode());
      result = prime * result + ((endIp == null) ? 0 : endIp.hashCode());
      result = prime * result + ((network == null) ? 0 : network.hashCode());
      result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof IpBlockEntity && this.compareTo((IpBlockEntity) o) == 0;
   }

   @Override
   public String toString() {
      return "[" + getType() + ", " + getOwnerId() + ", " + getBeginAddress() + ", "
            + getEndAddress() + ", " + getLength() + "]";
   }

   private void validate() {
      // the rest api layer should do the real param check
      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(this.getLength() > 0, "ip block size should > 0");

         AuAssert.check((this.getType() == BlockType.ASSIGNED &&
               !this.getOwnerId().equals(FREE_BLOCK_OWNER_ID)) ||
               (this.getType() == BlockType.FREE &&
               this.getOwnerId().equals(FREE_BLOCK_OWNER_ID)),
               "block type shold match owner id: " + getType() + ", " + getOwnerId());
      }
   }

   /**
    * Check whether the current block contains the other one.
    * 
    * @param o
    *           the other one
    * @return contained or not
    */
   public boolean contains(IpBlockEntity o) {
      return this.getBeginIp() <= o.getBeginIp() && this.getEndIp() >= o.getEndIp();
   }

   /**
    * Check whether the current block is overlapped with the other one.
    * 
    * @param o
    *           the other one
    * @return overlapped or not
    */
   public boolean isOverlapedWith(IpBlockEntity o) {
      return this.getBeginIp() <= o.getEndIp() && o.getBeginIp() <= this.getEndIp();
   }

   /**
    * Check whether the two block can be concatenated as one.
    * 
    * @param next
    *           next should be greater or equal with the current one
    * @return flag
    */
   private boolean canConcatWith(IpBlockEntity next) {
      AuAssert.check(this.getBeginIp() <= next.getBeginIp());

      return this.getEndIp() + 1 >= next.getBeginIp();
   }

   /**
    * Duplicate an entity, just like clone, but clear the id
    * 
    * @return new transient entity
    */
   private IpBlockEntity dup() {
      return new IpBlockEntity(network, ownerId, type, beginIp, endIp);
   }

   /**
    * Duplicate a IP block list, with id field cleared. This is used to create
    * Hibernate transient entities.
    * 
    * @param ipBlocks
    *           list to be duplicated.
    * @return new transient entities
    */
   public static List<IpBlockEntity> dup(Collection<IpBlockEntity> ipBlocks) {
      List<IpBlockEntity> dup = new ArrayList<IpBlockEntity>(ipBlocks.size());

      for (IpBlockEntity blk : ipBlocks) {
         dup.add(blk.dup());
      }

      return dup;
   }

   /**
    * Merge adjacent (overlapping) IP blocks if they have the same type.
    * 
    * Side effects:
    * 
    * 1) this call will modify all the objects in the input list, call
    * <code>dup</code> if this is not desirable.
    * 
    * 2) the objects in the returned list still reference the original objects,
    * and the one which are merged will be deleted if this is called in a active
    * Hibernate session.
    * 
    * @param ipBlocks
    *           source blocks
    * @param ignoreOwner
    *           whether ignore owner
    * @param ignoreType
    *           whether ignore block type
    * @param silentWhenOverlap
    *           whether to keep silent or raise error if blocks are overlapped
    * 
    * @return merged list.
    */
   public static List<IpBlockEntity> merge(List<IpBlockEntity> ipBlocks,
         boolean ignoreOwner, boolean ignoreType, boolean silentWhenOverlap) {
      if (ipBlocks.size() <= 1) {
         return ipBlocks;
      }

      AuAssert.check(ignoreOwner || !ignoreType, "must ignore owner if type is ignored");
      boolean inHibernateSession = DAL.isInTransaction();

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
                     curr.delete();
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

   /**
    * Filter a list according to the predicate functor.
    * 
    * Side effects: the objects in the returned list still referencing the
    * original ones.
    * 
    * @param ipBlocks
    *           input list
    * @param pred
    *           predicate functor
    * @return filtered objects which pass the predicate eveluation (true)
    */
   public static List<IpBlockEntity> filter(Collection<IpBlockEntity> ipBlocks,
         Predicate<IpBlockEntity> pred) {
      List<IpBlockEntity> dest = new ArrayList<IpBlockEntity>(ipBlocks.size());

      for (IpBlockEntity blk : ipBlocks) {
         blk.validate();

         if (pred.evaluate(blk)) {
            dest.add(blk);
         }
      }

      return dest;
   }

   public static class EqualBlockTypePredicate implements Predicate<IpBlockEntity> {
      private BlockType type;

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
      private Long ownerId;

      public EqualOwnerPredicate(Long ownerId) {
         this.ownerId = ownerId;
      }

      @Override
      public boolean evaluate(IpBlockEntity blk) {
         return blk.getOwnerId().equals(ownerId);
      }
   }

   /**
    * Count the IP addresses in the list.
    * 
    * @param ipBlocks
    * @return
    */
   public static long count(Collection<IpBlockEntity> ipBlocks) {
      if (ConfigInfo.isDebugEnabled()) {
         List<IpBlockEntity> dup = IpBlockEntity.dup(ipBlocks);
         // must not throw
         IpBlockEntity.merge(dup, true, true, false);
      }

      long count = 0;
      for (IpBlockEntity blk : ipBlocks) {
         count += blk.getLength();
      }

      return count;
   }

   /**
    * Get the difference set of A and B, i.e the IPs that are in A but are not
    * in B. The two set should be well-formed, i.e. well merged.
    * 
    * @param setA
    *           a list of IP blocks
    * @param setB
    *           a list of IP blocks
    * 
    * @return difference set by A - B
    */
   public static List<IpBlockEntity> subtract(Collection<IpBlockEntity> setA,
         Collection<IpBlockEntity> setB) {
      return subtract(new TreeSet<IpBlockEntity>(setA),
            new TreeSet<IpBlockEntity>(setB));
   }

   /**
    * Get the difference set of A and B, i.e the IPs that are in A but are not
    * in B. The two set should be well-formed, i.e. well merged.
    * 
    * Note: we don't care about the network and block type here, so do not
    * subtract two sets with different network/types and save them.
    * 
    * @param setA
    *           a set of IP blocks sorted by beginIp in ascending order
    * @param setB
    *           a set of IP blocks sorted by beginIp in ascending order
    * 
    * @return difference set by A - B
    */
   public static List<IpBlockEntity> subtract(SortedSet<IpBlockEntity> setA,
            SortedSet<IpBlockEntity> setB) {
      List<IpBlockEntity> diffSet = new ArrayList<IpBlockEntity>();

      NetworkEntity network = null;
      BlockType blockType = null;
      Long ownerId = null;

      Iterator<IpBlockEntity> iterA = setA.iterator();
      Iterator<IpBlockEntity> iterB = setB.iterator();

      long a0 = 1, a1 = 0; // set to invalid block: a0 > a1
      long b0 = 1, b1 = 0;

      while (true) {
         if (a0 > a1) {
            if (!iterA.hasNext()) {
               break;
            }
            IpBlockEntity blockA = iterA.next();
            network = blockA.getNetwork(); // just to get a valid value
            blockType = blockA.getType();
            ownerId = blockA.getOwnerId();
            a0 = IpAddressUtil.getAddressAsLong(blockA.getBeginAddress());
            a1 = IpAddressUtil.getAddressAsLong(blockA.getEndAddress());
         }

         if (b0 > b1) {
            if (!iterB.hasNext()) {
               // then add all remaining blocks
               diffSet.add(new IpBlockEntity(network, ownerId, blockType, a0, a1));
               a0 = a1 + 1;
               continue;
            }
            IpBlockEntity blockB = iterB.next();
            network = blockB.getNetwork();
            blockType = blockB.getType();
            ownerId = blockB.getOwnerId();
            b0 = IpAddressUtil.getAddressAsLong(blockB.getBeginAddress());
            b1 = IpAddressUtil.getAddressAsLong(blockB.getEndAddress());
         }

         if (a0 < b0) {
            if (a1 < b0) {
               diffSet.add(new IpBlockEntity(network, ownerId, blockType, a0, a1));
               a0 = a1 + 1;
               continue;
            } else {
               long end = Math.min(a1, b0 - 1);
               // diff
               diffSet.add(new IpBlockEntity(network, ownerId, blockType, a0, end));
               a0 = b0 = Math.min(a1 + 1, b1 + 1); // skip the A-B and common
            }
         } else {
            b0 = Math.min(a1 + 1, b1 + 1); // skip the B-A and the common
            a0 = Math.max(a0, b0);
         }
      }

      return diffSet;
   }
}
