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
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.dal.IIpBlockDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.impl.IpBlockDAO.EqualBlockTypePredicate;
import com.vmware.bdd.dal.impl.IpBlockDAO.EqualOwnerPredicate;
import com.vmware.bdd.dal.impl.IpBlockDAO.IsTransientPredicate;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Functor;
import com.vmware.bdd.utils.Functor.Predicate;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 * 
 */
@Repository
public class NetworkDAO extends BaseDAO<NetworkEntity> implements INetworkDAO {

   private static final Logger logger = Logger.getLogger(NetworkDAO.class);

   IIpBlockDAO iIpBlockDao;

   public IIpBlockDAO getIIpBlockDao() {
      return iIpBlockDao;
   }

   @Autowired
   public void setIIpBlockDao(IIpBlockDAO iIpBlockDao) {
      this.iIpBlockDao = iIpBlockDao;
   }

   @Override
   @Transactional(readOnly = true)
   public NetworkEntity findNetworkByName(String name) {
      return findUniqueByCriteria(Restrictions.eq("name", name));
   }

   @Override
   @Transactional(readOnly = true)
   public List<NetworkEntity> findAllNetworks() {
      return findAll();
   }

   @Override
   @Transactional(readOnly = true)
   public List<IpBlockEntity> findAllIpBlocks(NetworkEntity entity) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return iIpBlockDao
            .merge(IpBlockEntity.dup(entity.getIpBlocks()), true, true, false);
   }

   @Override
   @Transactional(readOnly = true)
   public List<IpBlockEntity> findAllFreeIpBlocks(NetworkEntity entity) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      List<IpBlockEntity> freeBlocks = IpBlockEntity.filter(entity.getIpBlocks(),
            EqualBlockTypePredicate.IS_FREE);
      Collections.sort(freeBlocks);

      return freeBlocks;
   }

   @Override
   @Transactional(readOnly = true)
   public List<IpBlockEntity> findAllAssignedIpBlocks(NetworkEntity entity) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      List<IpBlockEntity> assignedBlocks = IpBlockEntity.filter(entity.getIpBlocks(),
            EqualBlockTypePredicate.IS_ASSIGNED);
      Collections.sort(assignedBlocks);

      return assignedBlocks;
   }

   @Override
   @Transactional(readOnly = true)
   public List<IpBlockEntity> findAllAssignedIpBlocks(NetworkEntity entity,
         Long clusterId) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      AuAssert.check(
            clusterId != null && !clusterId.equals(IpBlockEntity.FREE_BLOCK_OWNER_ID),
            "ownerId should be valid");

      Predicate<IpBlockEntity> pred = new EqualOwnerPredicate(clusterId);
      List<IpBlockEntity> assignedBlocks = IpBlockEntity.filter(entity.getIpBlocks(),
            pred);
      Collections.sort(assignedBlocks);
      return assignedBlocks;
   }

   @Override
   @Transactional
   public void addIpBlocks(NetworkEntity entity, List<IpBlockEntity> ipBlocks) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      AuAssert.check(ipBlocks.size() > 0, "blocks should not be empty");
      logger.info("adding IP blocks: " + ipBlocks);

      for (IpBlockEntity block : ipBlocks) {
         block.setNetwork(entity);
      }

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(
               IpBlockEntity.filter(ipBlocks, EqualBlockTypePredicate.IS_ASSIGNED)
                     .size() == 0, "the input blocks should be all free");
      }

      // allow overlapped input
      ipBlocks = iIpBlockDao.merge(ipBlocks, false, false, true);
      ipBlocks.addAll(entity.getIpBlocks());
      // do not allow overlapped input with the pool
      ipBlocks = iIpBlockDao.merge(ipBlocks, false, false, true);

      entity.setIpBlocks(ipBlocks);
   }

   @Override
   @Transactional
   public void removeIpBlocks(NetworkEntity entity, List<IpBlockEntity> ipBlocks) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      AuAssert.check(ipBlocks.size() > 0, "blocks should not be empty");
      logger.info("remove IP blocks: " + ipBlocks);

      // pre-processing the blocks, allow overlapped IP blocks
      ipBlocks = iIpBlockDao.merge(ipBlocks, true, true, true);
      TreeSet<IpBlockEntity> setToRemove = new TreeSet<IpBlockEntity>(ipBlocks);
      TreeSet<IpBlockEntity> setAssigned = new TreeSet<IpBlockEntity>(
            IpBlockEntity.filter(entity.getIpBlocks(),
                  EqualBlockTypePredicate.IS_ASSIGNED));
      TreeSet<IpBlockEntity> setFree = new TreeSet<IpBlockEntity>(IpBlockEntity.filter(
            entity.getIpBlocks(), EqualBlockTypePredicate.IS_FREE));

      // detecting whether there are in-use IP addresses
      List<IpBlockEntity> diffAssigned = IpBlockEntity
            .subtract(setAssigned, setToRemove);
      if (IpBlockEntity.count(diffAssigned) != IpBlockEntity.count(setAssigned)) {
         logger.error("some of the IP addresses to be removed are currently used");
         throw NetworkException.IP_ADDR_IN_USE();
      }

      /**
       * remove the IPs from the free IP blocks, keep silent if the IP addresses
       * does not exist.
       */
      List<IpBlockEntity> diffFree = IpBlockEntity.subtract(setFree, setToRemove);

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(
               IpBlockEntity.filter(ipBlocks, EqualBlockTypePredicate.IS_ASSIGNED)
                     .size() == 0, "should be all free");
      }

      diffFree.addAll(setAssigned);

      // no need to sort
      entity.setIpBlocks(diffFree);
   }

   /**
    * Allocate IP addresses, the caller must not modify the contents in the
    * returned list.
    * 
    * @param clusterId
    * @param count
    * @return
    */
   @Override
   @Transactional
   public List<IpBlockEntity> alloc(NetworkEntity entity, long clusterId, long count) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      logger.info("alloc request: cluster " + clusterId + ", count " + count);

      if (entity.getFree() < count) {
         logger.error("insufficient IP address resources: requested " + count
               + ", remaining " + entity.getFree());
         throw NetworkException.OUT_OF_IP_ADDR();
      }

      // find all free blocks of this network
      List<IpBlockEntity> freeBlocks = IpBlockEntity.filter(entity.getIpBlocks(),
            EqualBlockTypePredicate.IS_FREE);

      List<IpBlockEntity> allBlocks = IpBlockEntity.filter(entity.getIpBlocks(),
            EqualBlockTypePredicate.IS_ASSIGNED);
      List<IpBlockEntity> newAssigned = new ArrayList<IpBlockEntity>();

      // sort the list, optional
      Collections.sort(freeBlocks);

      Iterator<IpBlockEntity> iter = freeBlocks.iterator();

      while (count > 0) {
         IpBlockEntity curr = iter.next();
         if (count >= curr.getLength()) {
            // change ownership and record the new assigned block
            curr.setOwnerId(clusterId);
            curr.setType(BlockType.ASSIGNED);
            newAssigned.add(curr);

            count -= curr.getLength();
         } else {
            // record the new assigned block
            newAssigned.add(new IpBlockEntity(entity, clusterId, BlockType.ASSIGNED,
                  curr.getBeginIp(), curr.getBeginIp() + count - 1));

            // add the remaining partial block
            curr.setBeginIp(curr.getBeginIp() + count);
            allBlocks.add(curr);

            count = 0;
         }
      }

      allBlocks.addAll(newAssigned);

      // add the remaining free blocks
      while (iter.hasNext() && allBlocks.add(iter.next()));

      entity.setIpBlocks(allBlocks);
      logger.info("alloc request success: cluster " + clusterId + ", IPs " + newAssigned);
      return IpBlockEntity.dup(newAssigned);
   }

   @Override
   @Transactional
   public void free(NetworkEntity entity, long clusterId, List<IpBlockEntity> ipBlocks) {
      AuAssert.check(entity.getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      logger.info("free request: cluster " + clusterId + ", IPs " + ipBlocks);

      Predicate<IpBlockEntity> predEqOwner = new EqualOwnerPredicate(clusterId);
      Predicate<IpBlockEntity> predNotEqOwner = Functor.negate(new EqualOwnerPredicate(
            clusterId));

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(IpBlockEntity.filter(ipBlocks, predNotEqOwner).isEmpty(),
               "blocks owner field should match cluster id: " + clusterId);
      }
      // duplicate the list, to avoid modifying it's contents
      ipBlocks = IpBlockEntity.dup(ipBlocks);

      // pre-processing the blocks, allow overlapped IP blocks
      ipBlocks = iIpBlockDao.merge(ipBlocks, true, true, true);

      List<IpBlockEntity> assignedToOthers = IpBlockEntity.filter(IpBlockEntity.filter(
            entity.getIpBlocks(), EqualBlockTypePredicate.IS_ASSIGNED), predNotEqOwner);

      List<IpBlockEntity> assignedToMe = IpBlockEntity.filter(entity.getIpBlocks(),
            predEqOwner);

      List<IpBlockEntity> currentFree = IpBlockEntity.filter(entity.getIpBlocks(),
            EqualBlockTypePredicate.IS_FREE);

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(
               IpBlockEntity.filter(ipBlocks, IsTransientPredicate.NEGATE_INSTANCE)
                     .isEmpty(), "the input blocks should be transient entities");
         AuAssert.check(IpBlockEntity.subtract(ipBlocks, assignedToMe).isEmpty(),
               "can only free the IPs that were allocated");
      }

      /**
       * There are three type of Hibernate managed (persistent) entities which
       * need to be handled differently: 1) all other blocks which is assigned
       * to other owners, they will be kept unchanged 2) all the current free
       * ones 3) the one which is assigned to the current owner, all of them
       * will be deleted first and then re-inserted as free or remaining
       * assigned. This is due to subtract does not handle the entity status
       * (i.e. all the result blocks from a subtract operation are transient).
       */
      for (IpBlockEntity blk : assignedToMe) {
         iIpBlockDao.delete(blk);
      }

      List<IpBlockEntity> remainingAssignedBlocks = IpBlockEntity.subtract(assignedToMe,
            ipBlocks);

      if (logger.isDebugEnabled()) {
         logger.debug("assignedToMe: \n" + assignedToMe);
         logger.debug("remainingAssignedBlocks: \n" + remainingAssignedBlocks);
         logger.debug("assignedToOthers: \n" + assignedToOthers);
         logger.debug("currentFree: \n" + currentFree);
      }

      List<IpBlockEntity> allBlocks = assignedToOthers;
      allBlocks.addAll(currentFree);
      allBlocks.addAll(remainingAssignedBlocks);

      for (IpBlockEntity blk : ipBlocks) {
         blk.setOwnerId(IpBlockEntity.FREE_BLOCK_OWNER_ID);
         blk.setType(BlockType.FREE);
         allBlocks.add(blk);
      }

      allBlocks = iIpBlockDao.merge(allBlocks, false, false, false);
      entity.setIpBlocks(allBlocks);
      logger.info("free request success: cluster " + clusterId);
   }

   @Override
   @Transactional
   public void free(NetworkEntity entity, long clusterId) {
      if (entity.getAllocType() != AllocType.IP_POOL) {
         // DHCP allowed
         return;
      }
      logger.info("free all request: cluster " + clusterId);

      Predicate<IpBlockEntity> pred = new EqualOwnerPredicate(clusterId);
      List<IpBlockEntity> toBeFreed = IpBlockEntity.filter(entity.getIpBlocks(), pred);

      if (!toBeFreed.isEmpty()) {
         // call dup to clear their Hibernate-status
         this.free(entity, clusterId, IpBlockEntity.dup(toBeFreed));
         logger.info("free all request success: cluster " + clusterId);
      }
   }
}
