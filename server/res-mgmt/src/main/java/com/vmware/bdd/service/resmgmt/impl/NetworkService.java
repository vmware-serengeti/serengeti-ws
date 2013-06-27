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
package com.vmware.bdd.service.resmgmt.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.IpAllocEntryRead;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.dal.IIpBlockDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.INodeDAO;
import com.vmware.bdd.dal.INodeGroupDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.IpBlockEntity.BlockType;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.NetworkException;
import com.vmware.bdd.exception.UniqueConstraintViolationException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.IpAddressUtil;

@Service
public class NetworkService implements Serializable, INetworkService {
   private static final long serialVersionUID = 1271195142772906091L;
   private static final Logger logger = Logger.getLogger(NetworkService.class);

   private INetworkDAO networkDao;

   private IIpBlockDAO ipBlockDao;

   private INodeGroupDAO nodeGroupDao;

   private INodeDAO nodeDao;

   private IResourceService resService;

   public INetworkDAO getNetworkDao() {
      return networkDao;
   }

   @Autowired
   public void setNetworkDao(INetworkDAO networkDao) {
      this.networkDao = networkDao;
   }

   @Autowired
   public void setIpBlockDao(IIpBlockDAO ipBlockDao) {
      this.ipBlockDao = ipBlockDao;
   }

   @Autowired
   public void setNodeGroupDao(INodeGroupDAO nodeGroupDao) {
      this.nodeGroupDao = nodeGroupDao;
   }

   @Autowired
   public void setNodeDao(INodeDAO nodeDao) {
      this.nodeDao = nodeDao;
   }

   @Autowired
   public void setResService(IResourceService resSvc) {
      this.resService = resSvc;
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#addDhcpNetwork(java.lang.String,
    * java.lang.String)
    */
   @Override
   @Transactional
   public synchronized NetworkEntity addDhcpNetwork(final String name,
         final String portGroup) {
      if (!resService.isNetworkExistInVc(portGroup)) {
         throw VcProviderException.NETWORK_NOT_FOUND(portGroup);
      }
      try {
         NetworkEntity network =
               new NetworkEntity(name, portGroup, AllocType.DHCP, null, null,
                     null, null);
         networkDao.insert(network);
         network.validate();
         return network;
      } catch (UniqueConstraintViolationException ex) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS(ex, "network", name);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#addIpPoolNetwork(java.lang.String,
    * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
    * java.lang.String, java.util.List)
    */
   @Override
   @Transactional
   public synchronized NetworkEntity addIpPoolNetwork(final String name,
         final String portGroup, final String netmask, final String gateway,
         final String dns1, final String dns2, final List<IpBlock> ipBlocks) {
      try {
         if (!resService.isNetworkExistInVc(portGroup)) {
            throw VcProviderException.NETWORK_NOT_FOUND(portGroup);
         }
         NetworkEntity network =
               new NetworkEntity(name, portGroup, AllocType.IP_POOL, netmask,
                     gateway, dns1, dns2);
         networkDao.insert(network);
         List<IpBlockEntity> blocks =
               new ArrayList<IpBlockEntity>(ipBlocks.size());
         for (IpBlock ib : ipBlocks) {
            IpBlockEntity blk =
                  new IpBlockEntity(null, IpBlockEntity.FREE_BLOCK_OWNER_ID,
                        BlockType.FREE, IpAddressUtil.getAddressAsLong(ib
                              .getBeginIp()), IpAddressUtil.getAddressAsLong(ib
                              .getEndIp()));
            blocks.add(blk);
         }

         networkDao.addIpBlocks(network, blocks);
         network.validate();
         return network;
      } catch (UniqueConstraintViolationException ex) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS(ex, "network", name);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#getNetworkEntityByName(java.lang
    * .String)
    */
   @Override
   public NetworkEntity getNetworkEntityByName(final String name) {
      return networkDao.findNetworkByName(name);
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#getNetworkByName(java.lang.String,
    * boolean)
    */
   @Override
   @Transactional
   public NetworkRead getNetworkByName(final String name,
         final boolean withDetails) {
      NetworkEntity entity = getNetworkEntityByName(name);

      if (entity != null) {
         return convert(entity, withDetails);
      }
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.bdd.manager.INetworkService#getAllNetworkEntities()
    */
   @Override
   public List<NetworkEntity> getAllNetworkEntities() {
      return networkDao.findAllNetworks();
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.bdd.manager.INetworkService#getAllNetworks(boolean)
    */
   @Override
   @Transactional
   public List<NetworkRead> getAllNetworks(final boolean withDetails) {
      List<NetworkRead> networks = new ArrayList<NetworkRead>();
      for (NetworkEntity net : getAllNetworkEntities()) {
         networks.add(convert(net, withDetails));
      }
      return networks;
   }

   protected void assertNetworkNotUsed(NetworkEntity network)
         throws NetworkException {
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
            AuAssert.check(network.getTotal() == network.getFree(), "total = "
                  + network.getTotal() + ", free = " + network.getFree());
         }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#updateNetwork(com.vmware.bdd.entity
    * .NetworkEntity, com.vmware.bdd.entity.NetworkEntity.AllocType,
    * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   @Transactional
   public synchronized void updateNetwork(NetworkEntity network,
         AllocType allocType, String netmask, String gateway, String dns1,
         String dns2) {
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
               ipBlockDao.delete(block);
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
      networkDao.update(network);
      network.validate();
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#removeNetwork(com.vmware.bdd.entity
    * .NetworkEntity)
    */
   @Override
   @Transactional
   public synchronized void removeNetwork(NetworkEntity network) {
      assertNetworkNotUsed(network);

      for (IpBlockEntity block : network.getIpBlocks()) {
         ipBlockDao.delete(block);
      }
      networkDao.delete(network);
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#removeNetwork(java.lang.String)
    */
   @Override
   @Transactional
   public synchronized void removeNetwork(final String networkName) {
      NetworkEntity net = getNetworkEntityByName(networkName);
      if (net != null) {
         removeNetwork(net);
      } else {
         throw BddException.NOT_FOUND("network", networkName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#addIpBlocks(com.vmware.bdd.entity
    * .NetworkEntity, java.util.List)
    */
   @Override
   @Transactional
   public synchronized void addIpBlocks(NetworkEntity network, List<IpBlockEntity> toAdd) {
      networkDao.addIpBlocks(network, toAdd);
      network.validate();
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#removeIpBlocks(com.vmware.bdd.entity
    * .NetworkEntity, java.util.List)
    */
   @Override
   @Transactional
   public synchronized void removeIpBlocks(NetworkEntity network,
         List<IpBlockEntity> toRemove) {
      networkDao.removeIpBlocks(network, toRemove);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.bdd.manager.INetworkService#alloc(com.vmware.bdd.entity.
    * NetworkEntity, long, long)
    */
   @Override
   @Transactional
   public synchronized List<IpBlockEntity> alloc(NetworkEntity network, long clusterId,
         long count) {
      return networkDao.alloc(network, clusterId, count);
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#getAllocatedIpBlocks(com.vmware
    * .bdd.entity.NetworkEntity, long)
    */
   @Override
   @Transactional
   public List<IpBlockEntity> getAllocatedIpBlocks(NetworkEntity network,
         long clusterId) {
      return networkDao.findAllAssignedIpBlocks(network, clusterId);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.bdd.manager.INetworkService#free(com.vmware.bdd.entity.
    * NetworkEntity, long, java.util.List)
    */
   @Override
   @Transactional
   public synchronized void free(NetworkEntity network, long clusterId,
         List<IpBlockEntity> ipBlocks) {
      networkDao.free(network, clusterId, ipBlocks);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.bdd.manager.INetworkService#free(com.vmware.bdd.entity.
    * NetworkEntity, long)
    */
   @Override
   @Transactional
   public synchronized void free(NetworkEntity network, long clusterId) {
      network = networkDao.findById(network.getId());
      networkDao.free(network, clusterId);
   }

   @Transactional(readOnly = true)
   private NetworkRead convert(NetworkEntity net, boolean withDetails) {
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
         for (IpBlockEntity blk : networkDao.findAllIpBlocks(net)) {
            IpBlock ib = new IpBlock();
            ib.setBeginIp(blk.getBeginAddress());
            ib.setEndIp(blk.getEndAddress());
            allBlocks.add(ib);
         }

         for (IpBlockEntity blk : networkDao.findAllFreeIpBlocks(net)) {
            IpBlock ib = new IpBlock();
            ib.setBeginIp(blk.getBeginAddress());
            ib.setEndIp(blk.getEndAddress());
            freeBlocks.add(ib);
         }

         for (IpBlockEntity blk : networkDao.findAllAssignedIpBlocks(net)) {
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
         List<NodeEntity> nodes =
               nodeDao.findByNodeGroups(nodeGroupDao.findAllByClusters(net
                     .getClusters()));

         for (NodeEntity node : nodes) {
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

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.bdd.manager.INetworkService#free(com.vmware.bdd.entity.
    * NetworkEntity, long, long)
    */
   @Override
   @Transactional
   public synchronized void free(NetworkEntity network, long clusterId,
         long ipAddr) {
      IpBlockEntity block =
            new IpBlockEntity(network, clusterId, BlockType.ASSIGNED, ipAddr,
                  ipAddr);
      List<IpBlockEntity> ipBlocks = new ArrayList<IpBlockEntity>();
      ipBlocks.add(block);
      networkDao.free(network, clusterId, ipBlocks);
   }
}
