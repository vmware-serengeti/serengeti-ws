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
package com.vmware.bdd.entity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.vmware.bdd.dal.impl.IpBlockDAO.EqualBlockTypePredicate;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.IpAddressUtil;
import org.hibernate.annotations.CascadeType;

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

   @OneToMany(mappedBy = "networkEntity", fetch = FetchType.LAZY)
   private Set<NicEntity> nics;

   /**
    * Related clusters which have assigned IPs, the relationship is only used
    * for 'show details' of a network, and is built at the cluster creation side
    * instead of inside alloc method. So this class does not guarantee that the
    * cluster list exactly matches the list contained in IpBlockEntity list
    * (though the cluster class should guarantee the correctness).
    */

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

   /*
   public List<ClusterEntity> getClusters() {
      return clusters;
   }

   public void setClusters(List<ClusterEntity> clusters) {
      this.clusters = clusters;
   }
   */

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

   public Set<NicEntity> getNics() {
      return nics;
   }

   public void setNics(Set<NicEntity> nics) {
      this.nics = nics;
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

            try {
               InetAddress.getByName(getNetmask());
               if (getGateway() != null) {
                  InetAddress.getByName(getGateway());
               }
               InetAddress.getByName(getDns1());
               InetAddress.getByName(getDns2());
            } catch (UnknownHostException e) {
               AuAssert.unreachable();
            }
         }
      }

      if (getAllocType() == AllocType.IP_POOL && getGateway() != null) {
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



   public long getTotal() {
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return this.total;
   }

   public long getFree() {
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return this.free;
   }

   public long getAssigned() {
      AuAssert.check(getAllocType() == AllocType.IP_POOL,
            "should not be called when IP pool is not used");

      return this.total - this.free;
   }


}
