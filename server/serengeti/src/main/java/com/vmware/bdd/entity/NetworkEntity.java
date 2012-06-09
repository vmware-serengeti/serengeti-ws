/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.criterion.Restrictions;

import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.IpBlockEntity.EqualBlockTypePredicate;
import com.vmware.bdd.entity.IpBlockEntity.EqualOwnerPredicate;
import com.vmware.bdd.entity.IpBlockEntity.IsTransientPredicate;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Functor;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Functor.Predicate;
import com.vmware.bdd.utils.IpAddressUtil;

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "network_seq", allocationSize = 1)
@Table(name = "network")
public class NetworkEntity extends EntityBase implements Comparable<NetworkEntity> {
   public enum AllocType {
      DHCP, IP_POOL
   }

   private static final Logger logger = Logger.getLogger(NetworkEntity.class);

   @Column(name = "name", unique = true, nullable = false)
   private String name;

   @Column(name = "port_group", nullable = false)
   private String portGroup;

   /**
    * Use cascade save to simplify the code. Since we almost always need to
    * update this when network is being modified, there is little overhead.
    */
   @OneToMany(mappedBy = "network", fetch = FetchType.LAZY)
   @Cascade({ CascadeType.SAVE_UPDATE, CascadeType.REMOVE })
   private List<IpBlockEntity> ipBlocks;

   /**
    * Related clusters which have assigned IPs, the relationship is only used
    * for 'show details' of a network, and is built at the cluster creation side
    * instead of inside alloc method. So this class does not guarantee that the
    * cluster list exactly matches the list contained in IpBlockEntity list
    * (though the cluster class should guarantee the correctness).
    */
   @OneToMany(mappedBy = "network", fetch = FetchType.LAZY)
   private List<ClusterEntity> clusters;

   @Enumerated(EnumType.STRING)
   @Column(name = "alloc_type", nullable = false)
   private AllocType allocType;

   @Column(name = "netmask")
   private String netmask;

   @Column(name = "gateway")
   private String gateway;

   @Column(name = "dns1")
   private String dns1;

   @Column(name = "dns2")
   private String dns2;

   @Column(name = "total")
   private Long total;

   @Column(name = "free")
   private Long free;

   public NetworkEntity() {
   }

   public NetworkEntity(String name, String portGroup,
         AllocType allocType, String netmask, String gateway, String dns1, String dns2) {
      this.name = name;
      this.portGroup = portGroup;
      this.ipBlocks = new ArrayList<IpBlockEntity>();
      this.allocType = allocType;
      this.netmask = netmask;
      this.gateway = gateway;
      this.dns1 = dns1;
      this.dns2 = dns2;
      this.total = 0L;
      this.free = 0L;

      validate();
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getPortGroup() {
      return portGroup;
   }

   public void setPortGroup(String portGroup) {
      this.portGroup = portGroup;
   }

   public List<IpBlockEntity> getIpBlocks() {
      return ipBlocks;
   }

   public void setIpBlocks(List<IpBlockEntity> ipBlocks) {
      this.total = IpBlockEntity.count(ipBlocks);
      this.free = IpBlockEntity.count(IpBlockEntity.filter(ipBlocks,
            EqualBlockTypePredicate.IS_FREE));
      this.ipBlocks = ipBlocks;
   }

   public List<ClusterEntity> getClusters() {
      return clusters;
   }

   public AllocType getAllocType() {
      return allocType;
   }

   public void setAllocType(AllocType allocType) {
      this.allocType = allocType;
   }

   public String getNetmask() {
      return netmask;
   }

   public void setNetmask(String netmask) {
      this.netmask = netmask;
   }

   public String getGateway() {
      return gateway;
   }

   public void setGateway(String gateway) {
      this.gateway = gateway;
   }

   public String getDns1() {
      return dns1;
   }

   public void setDns1(String dns1) {
      this.dns1 = dns1;
   }

   public String getDns2() {
      return dns2;
   }

   public void setDns2(String dns2) {
      this.dns2 = dns2;
   }

   @Override
   public int compareTo(NetworkEntity o) {
      return getName().compareTo(o.getName());
   }

   @Override
   public int hashCode() {
      return getName().hashCode();
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof NetworkEntity && this.compareTo((NetworkEntity) o) == 0;
   }

   public void validate() {
      // validate the rest api layer parameter check
      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(getPortGroup() != null);
         AuAssert.check(getAllocType() != null);

         if (getAllocType() == AllocType.DHCP) {
            AuAssert.check(getNetmask() == null);
            AuAssert.check(getGateway() == null);
            AuAssert.check(getDns1() == null);
            AuAssert.check(getDns2() == null);
         } else {
            AuAssert.check(getNetmask() != null);
            AuAssert.check(getGateway() != null);

            try {
               InetAddress.getByName(getNetmask());
               InetAddress.getByName(getGateway());
               InetAddress.getByName(getDns1());
               InetAddress.getByName(getDns2());
            } catch (UnknownHostException e) {
               AuAssert.unreachable();
            }
         }
      }

      if (getAllocType() == AllocType.IP_POOL) {
         long netmask = IpAddressUtil.getAddressAsLong(this.getNetmask());
         long gateway = IpAddressUtil.getAddressAsLong(this.getGateway());

         if (ConfigInfo.isDebugEnabled()) {
            AuAssert.check(IpAddressUtil.isValidNetmask(netmask));
            AuAssert.check(IpAddressUtil.isValidIp(this.getGateway()));
         }

         long networkAddr = netmask & gateway;
         for (IpBlockEntity blk : getIpBlocks()) {
            if (!IpAddressUtil.networkContains(networkAddr, netmask, blk.getBeginIp())) {
               logger.error("ip block is not in the range of the network: " + blk);
               throw NetworkException.IP_OUT_OF_RANGE(blk.getBeginAddress());
            };
            if (!IpAddressUtil.networkContains(networkAddr, netmask, blk.getEndIp())) {
               logger.error("ip block is not in the range of the network: " + blk);
               throw NetworkException.IP_OUT_OF_RANGE(blk.getEndAddress());
            };
            if (gateway >= blk.getBeginIp() && gateway <= blk.getEndIp()) {
               logger.error("ip block overlaps with gateway: " + blk);
               throw NetworkException.IP_BLOCK_CONTAINS_GATEWAY(blk);
            }
         }
      }
   }

   @Override
   public String toString() {
      return "NetworkEntity [getPortGroup()=" + getPortGroup() + ", getIpBlocks()="
            + getIpBlocks() + ", getAllocType()=" + getAllocType() + ", getNetmask()="
            + getNetmask() + ", getGateway()=" + getGateway() + ", getDns1()="
            + getDns1() + ", getDns2()=" + getDns2() + "]";
   }

   public static NetworkEntity findNetworkByName(String name) {
      return DAL.findUniqueByCriteria(NetworkEntity.class,
            Restrictions.eq("name", name));
   }

   public static List<NetworkEntity> findAllNetworks() {
      return DAL.findAll(NetworkEntity.class);
   }

   public List<IpBlockEntity> findAllIpBlocks() {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return IpBlockEntity.merge(IpBlockEntity.dup(getIpBlocks()), true, true, false);
   }

   public List<IpBlockEntity> findAllFreeIpBlocks() {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      List<IpBlockEntity> freeBlocks =
            IpBlockEntity.filter(getIpBlocks(), EqualBlockTypePredicate.IS_FREE);
      Collections.sort(freeBlocks);

      return freeBlocks;
   }

   public List<IpBlockEntity> findAllAssignedIpBlocks() {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      List<IpBlockEntity> assignedBlocks =
            IpBlockEntity.filter(getIpBlocks(), EqualBlockTypePredicate.IS_ASSIGNED);
      Collections.sort(assignedBlocks);

      return assignedBlocks;
   }

   public List<IpBlockEntity> findAllAssignedIpBlocks(Long clusterId) {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      AuAssert.check(clusterId != null &&
            !clusterId.equals(IpBlockEntity.FREE_BLOCK_OWNER_ID),
            "ownerId should be valid");

      Predicate<IpBlockEntity> pred = new EqualOwnerPredicate(clusterId);
      List<IpBlockEntity> assignedBlocks = IpBlockEntity.filter(getIpBlocks(), pred);
      Collections.sort(assignedBlocks);
      return assignedBlocks;
   }

   public long getTotal() {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return this.total;
   }

   public long getFree() {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return this.free;
   }

   public long getAssigned() {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return this.total - this.free;
   }

   public void addIpBlocks(List<IpBlockEntity> ipBlocks) {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      AuAssert.check(ipBlocks.size() > 0, "blocks should not be empty");
      logger.info("adding IP blocks: " + ipBlocks);

      for (IpBlockEntity block : ipBlocks) {
         block.setNetwork(this);
      }

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(IpBlockEntity.filter(ipBlocks,
               EqualBlockTypePredicate.IS_ASSIGNED).size() == 0,
               "the input blocks should be all free");
      }

      // allow overlapped input
      ipBlocks = IpBlockEntity.merge(ipBlocks, false, false, true);
      ipBlocks.addAll(getIpBlocks());
      // do not allow overlapped input with the pool
      ipBlocks = IpBlockEntity.merge(ipBlocks, false, false, false);

      setIpBlocks(ipBlocks);
   }

   public void removeIpBlocks(List<IpBlockEntity> ipBlocks) {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      AuAssert.check(ipBlocks.size() > 0, "blocks should not be empty");
      logger.info("remove IP blocks: " + ipBlocks);

      // pre-processing the blocks, allow overlapped IP blocks
      ipBlocks = IpBlockEntity.merge(ipBlocks, true, true, true);
      TreeSet<IpBlockEntity> setToRemove = new TreeSet<IpBlockEntity>(ipBlocks);
      TreeSet<IpBlockEntity> setAssigned = new TreeSet<IpBlockEntity>(
            IpBlockEntity.filter(getIpBlocks(), EqualBlockTypePredicate.IS_ASSIGNED));
      TreeSet<IpBlockEntity> setFree = new TreeSet<IpBlockEntity>(
            IpBlockEntity.filter(getIpBlocks(), EqualBlockTypePredicate.IS_FREE));

      // detecting whether there are in-use IP addresses
      List<IpBlockEntity> diffAssigned =
            IpBlockEntity.subtract(setAssigned, setToRemove);
      if (IpBlockEntity.count(diffAssigned) != IpBlockEntity.count(setAssigned)) {
         logger.error("some of the IP addresses to be removed are currently used");
         throw NetworkException.IP_ADDR_IN_USE();
      }

      /**
       * remove the IPs from the free IP blocks, keep silent if the IP addresses
       * does not exist.
       */
      List<IpBlockEntity> diffFree =
            IpBlockEntity.subtract(setFree, setToRemove);

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(IpBlockEntity.filter(ipBlocks,
               EqualBlockTypePredicate.IS_ASSIGNED).size() == 0,
               "should be all free");
      }

      diffFree.addAll(setAssigned);

      // no need to sort
      setIpBlocks(diffFree);
   }

   /**
    * Allocate IP addresses, the caller must not modify the contents in the
    * returned list.
    * 
    * @param clusterId
    * @param count
    * @return
    */
   public List<IpBlockEntity> alloc(long clusterId, long count) {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      logger.info("alloc request: cluster " + clusterId + ", count " + count);

      if (getFree() < count) {
         logger.error("insufficient IP address resources: requested " + count
               + ", remaining " + getFree());
         throw NetworkException.OUT_OF_IP_ADDR();
      }

      // find all free blocks of this network
      List<IpBlockEntity> freeBlocks = IpBlockEntity.filter(getIpBlocks(),
            EqualBlockTypePredicate.IS_FREE);

      List<IpBlockEntity> allBlocks = IpBlockEntity.filter(getIpBlocks(),
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
            newAssigned.add(new IpBlockEntity(this, clusterId, BlockType.ASSIGNED, curr
                  .getBeginIp(), curr.getBeginIp() + count - 1));

            // add the remaining partial block
            curr.setBeginIp(curr.getBeginIp() + count);
            allBlocks.add(curr);

            count = 0;
         }
      }

      allBlocks.addAll(newAssigned);

      // add the remaining free blocks
      while (iter.hasNext() && allBlocks.add(iter.next()));

      setIpBlocks(allBlocks);
      logger.info("alloc request success: cluster " + clusterId + ", IPs " + newAssigned);
      return IpBlockEntity.dup(newAssigned);
   }

   public void free(long clusterId, List<IpBlockEntity> ipBlocks) {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");
      logger.info("free request: cluster " + clusterId + ", IPs " + ipBlocks);

      Predicate<IpBlockEntity> predEqOwner = new EqualOwnerPredicate(clusterId);
      Predicate<IpBlockEntity> predNotEqOwner =
            Functor.negate(new EqualOwnerPredicate(clusterId));

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(IpBlockEntity.filter(ipBlocks, predNotEqOwner).isEmpty(),
               "blocks owner field should match cluster id: " + clusterId);
      }
      // duplicate the list, to avoid modifying it's contents
      ipBlocks = IpBlockEntity.dup(ipBlocks);

      // pre-processing the blocks, allow overlapped IP blocks
      ipBlocks = IpBlockEntity.merge(ipBlocks, true, true, true);

      List<IpBlockEntity> assignedToOthers = IpBlockEntity.filter(
            IpBlockEntity.filter(getIpBlocks(), EqualBlockTypePredicate.IS_ASSIGNED),
            predNotEqOwner);

      List<IpBlockEntity> assignedToMe = IpBlockEntity
            .filter(getIpBlocks(), predEqOwner);

      List<IpBlockEntity> currentFree = IpBlockEntity.filter(getIpBlocks(),
            EqualBlockTypePredicate.IS_FREE);

      if (ConfigInfo.isDebugEnabled()) {
         AuAssert.check(IpBlockEntity.filter(ipBlocks,
               IsTransientPredicate.NEGATE_INSTANCE).isEmpty(),
               "the input blocks should be transient entities");
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
         blk.delete();
      }

      List<IpBlockEntity> remainingAssignedBlocks =
            IpBlockEntity.subtract(assignedToMe, ipBlocks);

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

      allBlocks = IpBlockEntity.merge(allBlocks, false, false, false);
      setIpBlocks(allBlocks);
      logger.info("free request success: cluster " + clusterId);
   }

   public void free(long clusterId) {
      AuAssert.check(DAL.isInTransaction(), "must be called in txn");
      if (getAllocType() != AllocType.IP_POOL) {
         // DHCP allowed
         return;
      }
      logger.info("free all request: cluster " + clusterId);

      Predicate<IpBlockEntity> pred = new EqualOwnerPredicate(clusterId);
      List<IpBlockEntity> toBeFreed = IpBlockEntity.filter(getIpBlocks(), pred);

      if (!toBeFreed.isEmpty()) {
         // call dup to clear their Hibernate-status
         this.free(clusterId, IpBlockEntity.dup(toBeFreed));
         logger.info("free all request success: cluster " + clusterId);
      }
   }
}
