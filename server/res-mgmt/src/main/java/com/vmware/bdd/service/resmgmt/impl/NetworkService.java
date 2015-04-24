/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.IpAllocEntryRead;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.dal.IClusterDAO;
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
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.IpAddressUtil;

@Service
public class NetworkService implements Serializable, INetworkService {
   private static final long serialVersionUID = 1271195142772906091L;
   private static final Logger logger = Logger.getLogger(NetworkService.class);

   private INetworkDAO networkDao;

   private IIpBlockDAO ipBlockDao;

   private IClusterDAO clusterDAO;

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
   public void setClusterDAO(IClusterDAO clusterDAO) {
      this.clusterDAO = clusterDAO;
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
         final String portGroup, final NetworkDnsType dnsType, boolean isGenerateHostname) {
      validateNetworkName(name);
      if (!resService.isNetworkExistInVc(portGroup)) {
         throw VcProviderException.NETWORK_NOT_FOUND(portGroup);
      }
      try {
         if (NetworkDnsType.isOthers(dnsType) || NetworkDnsType.isDynamic(dnsType)) {
            isGenerateHostname = true;
         }
         NetworkEntity network =
               new NetworkEntity(name, portGroup, AllocType.DHCP, null, null,
                     null, null, dnsType, isGenerateHostname);
         networkDao.insert(network);
         network.validate();
         return network;
      } catch (UniqueConstraintViolationException ex) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS(ex, "Network", name);
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
         final String dns1, final String dns2, final List<IpBlock> ipBlocks,
         final NetworkDnsType dnsType, boolean isGenerateHostname) {
      try {
         validateNetworkName(name);
         if (!resService.isNetworkExistInVc(portGroup)) {
            throw VcProviderException.NETWORK_NOT_FOUND(portGroup);
         }
         if (NetworkDnsType.isOthers(dnsType) || NetworkDnsType.isDynamic(dnsType)) {
            isGenerateHostname = true;
         }
         NetworkEntity network =
               new NetworkEntity(name, portGroup, AllocType.IP_POOL, netmask,
                     gateway, dns1, dns2, dnsType, isGenerateHostname);
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

         checkIpBlockOverlap(name, blocks, portGroup);
         networkDao.addIpBlocks(network, blocks);
         network.validate();
         return network;
      } catch (UniqueConstraintViolationException ex) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS(ex, "Network", name);
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

   @Override
   @Transactional
   public List<String> getPortGroupsByNames(final List<String> names) {
      List<String> portGroups = new ArrayList<String>();
      for (String name : names) {
         portGroups.add(getNetworkEntityByName(name).getPortGroup());
      }
      return portGroups;
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

   @Transactional
   public List<String> getAllNetworkNames() {
      List<NetworkEntity> entities = getAllNetworkEntities();
      List<String> networkNames = new ArrayList<String>();
      for (NetworkEntity entity : entities) {
         networkNames.add(entity.getName());
      }
      return networkNames;
   }

   protected void assertNetworkNotUsed(NetworkEntity network)
         throws NetworkException {
      List<ClusterEntity> relevantClusters = findRelevantClusters(network);
      if (!relevantClusters.isEmpty()) {
         logger.error("can not change network, network re");
         List<String> clusterNames = new ArrayList<String>();
         for (ClusterEntity entity : relevantClusters) {
            clusterNames.add(entity.getName());
         }
         throw NetworkException.NETWORK_IN_USE(clusterNames);
      }

      if (ConfigInfo.isDebugEnabled()) {
         if (network.getAllocType() == AllocType.IP_POOL
               && relevantClusters.isEmpty()) {
            AuAssert.check(network.getTotal() == network.getFree(), "total = "
                  + network.getTotal() + ", free = " + network.getFree());
         }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.vmware.bdd.manager.INetworkService#increaseIPs(java.lang.String, 
    * java.util.List<com.vmware.bdd.apitypes.IpBlock>)
    */
   @Override
   @Transactional
   public synchronized void updateNetwork(String networkName, NetworkAdd networkAdd) {
      NetworkEntity network = getNetworkEntityByName(networkName);
      if (network == null) {
         throw NetworkException.NOT_FOUND("Network", networkName);
      }

      // Do not allow updating dnsType and isGenerateHostname if the network has been used
      NetworkDnsType dnsType = networkAdd.getDnsType();
      Boolean isGenerateHostname = networkAdd.getIsGenerateHostname();
      if (dnsType !=  null || isGenerateHostname != null) {
         assertNetworkNotUsed(network);
      }

      // Add IP block when the type is static
      List<IpBlock> ipBlocks = networkAdd.getIpBlocks();
      if (ipBlocks != null) {
         if (network.getAllocType().equals(AllocType.DHCP)) {
            throw NetworkException.IP_CONFIG_NOT_USED_FOR_DHCP();
         }
         long netmask = IpAddressUtil.getAddressAsLong(network.getNetmask());
         IpAddressUtil.verifyIPBlocks(ipBlocks, netmask);
         List<IpBlockEntity> blocks =
               new ArrayList<IpBlockEntity>(ipBlocks.size());
         for (IpBlock ib : ipBlocks) {
            IpBlockEntity blk =
                  new IpBlockEntity(network, IpBlockEntity.FREE_BLOCK_OWNER_ID,
                        BlockType.FREE, IpAddressUtil.getAddressAsLong(ib
                              .getBeginIp()), IpAddressUtil.getAddressAsLong(ib
                                    .getEndIp()));
            blocks.add(blk);
         }
         checkIpBlockOverlap(networkName, blocks, network.getPortGroup());
         networkDao.addIpBlocks(network, blocks);
      }

      if (dnsType == null) {
         dnsType = network.getDnsType();
      } else {
         networkDao.setDnsType(network, dnsType);
      }

      if (NetworkDnsType.isOthers(dnsType) || NetworkDnsType.isDynamic(dnsType)) {
         isGenerateHostname = true;
      } else {
         if (isGenerateHostname == null) {
            isGenerateHostname = network.getIsGenerateHostname();
         }
      }
      networkDao.setIsGenerateHostname(network, isGenerateHostname);

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
         throw BddException.NOT_FOUND("Network", networkName);
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
   private List<ClusterEntity> findRelevantClusters(NetworkEntity net) {
      List<ClusterEntity> relevantClusters = new ArrayList<ClusterEntity>();
      for (ClusterEntity clusterEntity : clusterDAO.findAll()) {
         if (clusterEntity.fetchNetworkNameList().contains(net.getName())) {
            relevantClusters.add(clusterEntity);
         }
      }
      return relevantClusters;
   }

   @Transactional(readOnly = true)
   private NetworkRead convert(NetworkEntity net, boolean withDetails) {
      NetworkRead nr = new NetworkRead();
      nr.setName(net.getName());
      nr.setPortGroup(net.getPortGroup());
      nr.setDnsType(net.getDnsType());
      nr.setIsGenerateHostname(net.getIsGenerateHostname());
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
         List<NodeEntity> nodes = nodeDao.findByNodeGroups(nodeGroupDao.findAllByClusters(findRelevantClusters(net)));

         for (NodeEntity node : nodes) {
            String ip = node.findNic(net).getIpv4Address();
            if (ip == null || ip.equals(Constants.NULL_IPV4_ADDRESS)) {
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
            e.setIpAddress(ip);

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

   private void validateNetworkName(final String name) {
      NetworkEntity networkEntity = networkDao.findNetworkByName(name);
      if (networkEntity != null) {
         logger.error("can not add a network with duplicated name");
         throw BddException.ALREADY_EXISTS("Network", name);
      }
   }

   private void checkIpBlockOverlap(String networkName, List<IpBlockEntity> ipBlockList, String portGroup) {
      // check if the new added ip ranges overlap with existed ip ranges
      Map<String, List<IpBlockEntity>> net2IpBlocks = getExistedIpBlocks4PortGroup(portGroup);
      List<String> overlapNets = new ArrayList<String>();
      Iterator<String> netItr = net2IpBlocks.keySet().iterator();
      for ( IpBlockEntity blk : ipBlockList ) {
         while ( netItr.hasNext() ) {
            String net = netItr.next();
            if ( net.equals(networkName) ) {
               // do not check the ip overlap for the same network
               continue;
            }
            for ( IpBlockEntity exblk : net2IpBlocks.get(net) ) {
               if ( blk.isOverlapedWith(exblk) ) {
                  overlapNets.add(net);
                  break;
               }
            }
         }
      }
      if ( overlapNets.size() > 0 ) {
         // there are overlapped ip ranges, will throw exception
         String overlapNetsStr = "[ ";
         for ( int i=0; i<overlapNets.size(); i++ ) {
            overlapNetsStr += overlapNets.get(i);
            if ( i < overlapNets.size() - 1 ) {
               overlapNetsStr += ", ";
            }
         }
         overlapNetsStr += " ]";
         throw NetworkException.IP_BLOCK_OVERLAP_WITH_NETWORKS(overlapNetsStr);
      }
   }

   private Map<String, List<IpBlockEntity>> getExistedIpBlocks4PortGroup(String portGroup) {
      Map<String, List<IpBlockEntity>> net2IpBlocks = new HashMap<String, List<IpBlockEntity>>();
      List<NetworkEntity> allNetworks = getAllNetworkEntities();
      if ( null != allNetworks ) {
         for ( NetworkEntity network : allNetworks ) {
            List<IpBlockEntity> ipBlockList = network.getIpBlocks();
            AllocType allocType = network.getAllocType();
            String pg = network.getPortGroup();
            // we only check networks with static ips for the given port group
            if ( allocType == AllocType.IP_POOL && pg.equals(portGroup) ) {
               net2IpBlocks.put(network.getName(), ipBlockList);
            }
         }
      }
      return net2IpBlocks;
   }
}
