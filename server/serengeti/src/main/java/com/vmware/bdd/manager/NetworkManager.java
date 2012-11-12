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
package com.vmware.bdd.manager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.IpAllocEntryRead;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.HadoopNodeEntity;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.exception.UniqueConstraintViolationException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.IpAddressUtil;

public class NetworkManager implements Serializable {
   private static final long serialVersionUID = 1271195142772906091L;
   private static final Logger logger = Logger.getLogger(NetworkManager.class);

   public NetworkEntity addDhcpNetwork(final String name, final String portGroup) {
      try {
         return DAL.autoTransactionDo(new Saveable<NetworkEntity>() {
            @Override
            public NetworkEntity body() throws Exception {
               NetworkEntity network = new NetworkEntity(name, portGroup,
                     AllocType.DHCP, null, null, null, null);
               network.insert();;
               network.validate();
               return network;
            }
         });
      } catch (UniqueConstraintViolationException ex) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS(ex, "network", name);
      }
   }

   public NetworkEntity addIpPoolNetwork(final String name, final String portGroup,
         final String netmask, final String gateway, final String dns1,
         final String dns2, final List<IpBlock> ipBlocks) {
      try {
         return DAL.autoTransactionDo(new Saveable<NetworkEntity>() {
            @Override
            public NetworkEntity body() throws Exception {
               NetworkEntity network = new NetworkEntity(name, portGroup,
                     AllocType.IP_POOL, netmask, gateway, dns1, dns2);
               network.insert();;

               List<IpBlockEntity> blocks = new ArrayList<IpBlockEntity>(ipBlocks.size());
               for (IpBlock ib : ipBlocks) {
                  IpBlockEntity blk = new IpBlockEntity(null,
                        IpBlockEntity.FREE_BLOCK_OWNER_ID, BlockType.FREE, IpAddressUtil
                              .getAddressAsLong(ib.getBeginIp()), IpAddressUtil
                              .getAddressAsLong(ib.getEndIp()));
                  blocks.add(blk);
               }

               network.addIpBlocks(blocks);
               network.validate();
               return network;
            }
         });
      } catch (UniqueConstraintViolationException ex) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS(ex, "network", name);
      }
   }

   public NetworkEntity getNetworkEntityByName(final String name) {
      return DAL.autoTransactionDo(new Saveable<NetworkEntity>() {
         @Override
         public NetworkEntity body() throws Exception {
            return NetworkEntity.findNetworkByName(name);
         }
      });
   }

   public NetworkRead getNetworkByName(final String name, final boolean withDetails) {
      return DAL.autoTransactionDo(new Saveable<NetworkRead>() {
         @Override
         public NetworkRead body() throws Exception {
            NetworkEntity entity = getNetworkEntityByName(name);

            if (entity != null) {
               return convert(entity, withDetails);
            }

            return null;
         }
      });
   }

   public List<NetworkEntity> getAllNetworkEntities() {
      return DAL.autoTransactionDo(new Saveable<List<NetworkEntity>>() {
         @Override
         public List<NetworkEntity> body() throws Exception {
            return NetworkEntity.findAllNetworks();
         }
      });
   }

   public List<NetworkRead> getAllNetworks(final boolean withDetails) {
      return DAL.autoTransactionDo(new Saveable<List<NetworkRead>>() {
         @Override
         public List<NetworkRead> body() throws Exception {
            List<NetworkRead> networks = new ArrayList<NetworkRead>();
            for (NetworkEntity net : getAllNetworkEntities()) {
               networks.add(convert(net, withDetails));
            }
            return networks;
         }
      });
   }

   private void assertNetworkNotUsed(NetworkEntity network) throws NetworkException {
      if (!network.getClusters().isEmpty()) {
         logger.error("can not change network, network re");
         List<String> clusterNames = new ArrayList<String>();
         for (ClusterEntity entity : network.getClusters()) {
            clusterNames.add(entity.getName());
         }
         throw NetworkException.NETWORK_IN_USE(clusterNames);
      }

      if (ConfigInfo.isDebugEnabled()) {
         if (network.getAllocType() == AllocType.IP_POOL
               && network.getClusters().isEmpty()) {
            AuAssert.check(network.getTotal() == network.getFree(),
                  "total = " + network.getTotal() + ", free = " + network.getFree());
         }
      }
   }

   public void updateNetwork(NetworkEntity network, AllocType allocType, String netmask,
         String gateway, String dns1, String dns2) {
      assertNetworkNotUsed(network);

      switch (allocType) {
      case DHCP:
         if (network.getAllocType() == AllocType.IP_POOL) {
            network.setAllocType(AllocType.DHCP);
            network.setNetmask(null);
            network.setGateway(null);
            network.setDns1(null);
            network.setDns2(null);
            for (IpBlockEntity block : network.getIpBlocks()) {
               block.delete();
            }
            network.setIpBlocks(new ArrayList<IpBlockEntity>(0));
         }
         break;
      case IP_POOL:
         network.setAllocType(AllocType.IP_POOL);
         network.setNetmask(netmask);
         network.setGateway(gateway);
         network.setDns1(dns1);
         network.setDns2(dns2);

         break;
      default:
         AuAssert.unreachable();
      }

      network.validate();
   }

   public void removeNetwork(NetworkEntity network) {
      assertNetworkNotUsed(network);

      for (IpBlockEntity block : network.getIpBlocks()) {
         block.delete();
      }

      network.delete();
   }

   public void removeNetwork(final String networkName) {
      DAL.autoTransactionDo(new Saveable<NetworkRead>() {
         @Override
         public NetworkRead body() throws Exception {
            NetworkEntity net = getNetworkEntityByName(networkName);
            if (net != null) {
               removeNetwork(net);
            } else {
               throw BddException.NOT_FOUND("network", networkName);
            }

            return null;
         }
      });
   }

   public void addIpBlocks(NetworkEntity network, List<IpBlockEntity> toAdd) {
      network.addIpBlocks(toAdd);
      network.validate();
   }

   public void removeIpBlocks(NetworkEntity network, List<IpBlockEntity> toRemove) {
      network.removeIpBlocks(toRemove);
   }

   /**
    * Allocate IP addresses from a network for a cluster.
    * 
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @param count
    *           IP count
    * @return allocated IP addresses
    */
   public List<IpBlockEntity> alloc(NetworkEntity network, long clusterId, long count) {
      return network.alloc(clusterId, count);
   }

   /**
    * Get all allocated IP blocks allocated from a specified network.
    * 
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @return IP blocks list
    */
   public List<IpBlockEntity> getAllocatedIpBlocks(NetworkEntity network, long clusterId) {
      return network.findAllAssignedIpBlocks(clusterId);
   }

   /**
    * Free the specified IP blocks, these IP blocks must be allocated from this
    * network.
    * 
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    * @param ipBlocks
    *           IP blocks
    */
   public void free(NetworkEntity network, long clusterId, List<IpBlockEntity> ipBlocks) {
      network.free(clusterId, ipBlocks);
   }

   /**
    * Try to free all the ip blocks of an cluster if any assigned.
    * 
    * @param network
    *           network
    * @param clusterId
    *           cluster id
    */
   public void free(NetworkEntity network, long clusterId) {
      network.free(clusterId);
   }

   private NetworkRead convert(NetworkEntity net, boolean withDetails) {
      AuAssert.check(DAL.isInTransaction());

      NetworkRead nr = new NetworkRead();
      nr.setName(net.getName());
      nr.setPortGroup(net.getPortGroup());
      if (net.getAllocType() == AllocType.IP_POOL) {
         nr.setDhcp(false);
         nr.setDns1(net.getDns1());
         nr.setDns2(net.getDns2());
         nr.setGateway(net.getGateway());
         nr.setNetmask(net.getNetmask());

         List<IpBlock> allBlocks = new ArrayList<IpBlock>();
         List<IpBlock> freeBlocks = new ArrayList<IpBlock>();
         List<IpBlock> assignedBlocks = new ArrayList<IpBlock>();
         for (IpBlockEntity blk : net.findAllIpBlocks()) {
            IpBlock ib = new IpBlock();
            ib.setBeginIp(blk.getBeginAddress());
            ib.setEndIp(blk.getEndAddress());
            allBlocks.add(ib);
         }

         for (IpBlockEntity blk : net.findAllFreeIpBlocks()) {
            IpBlock ib = new IpBlock();
            ib.setBeginIp(blk.getBeginAddress());
            ib.setEndIp(blk.getEndAddress());
            freeBlocks.add(ib);
         }

         for (IpBlockEntity blk : net.findAllAssignedIpBlocks()) {
            IpBlock ib = new IpBlock();
            ib.setBeginIp(blk.getBeginAddress());
            ib.setEndIp(blk.getEndAddress());
            assignedBlocks.add(ib);
         }

         nr.setAllIpBlocks(allBlocks);
         nr.setFreeIpBlocks(freeBlocks);
         nr.setAssignedIpBlocks(assignedBlocks);
      } else {
         nr.setDhcp(true);
      }

      if (withDetails) {
         List<IpAllocEntryRead> ipAllocEntries =
               new ArrayList<IpAllocEntryRead>();
         List<HadoopNodeEntity> nodes =
               HadoopNodeEntity.findByNodeGroups(NodeGroupEntity
                     .findByClusters(net.getClusters()));

         for (HadoopNodeEntity node : nodes) {
            if (node.getIpAddress() == null || node.getIpAddress().isEmpty()) {
               // in case of errors during node creation (if possible)
               continue;
            }
            IpAllocEntryRead e = new IpAllocEntryRead();

            e.setClusterId(node.getNodeGroup().getCluster().getId());
            e.setClusterName(node.getNodeGroup().getCluster().getName());
            e.setNodeGroupId(node.getNodeGroup().getId());
            e.setNodeGroupName(node.getNodeGroup().getName());
            e.setNodeId(node.getId());
            e.setNodeName(node.getVmName());
            e.setIpAddress(node.getIpAddress());

            ipAllocEntries.add(e);
         }

         nr.setIpAllocEntries(ipAllocEntries);
      }

      return nr;
   }
}
