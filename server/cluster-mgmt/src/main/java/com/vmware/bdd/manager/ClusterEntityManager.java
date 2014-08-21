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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import com.vmware.bdd.exception.BddException;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.aop.annotation.RetryTransaction;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.INodeDAO;
import com.vmware.bdd.dal.INodeGroupDAO;
import com.vmware.bdd.dal.IServerInfoDAO;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NicEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.ServerInfoEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.software.mgmt.thrift.GroupData;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
import com.vmware.bdd.software.mgmt.thrift.ServerData;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

@Transactional(readOnly = true)
public class ClusterEntityManager implements IClusterEntityManager, Observer {
   private static final Logger logger = Logger
         .getLogger(ClusterEntityManager.class);

   private IClusterDAO clusterDao;

   private INodeGroupDAO nodeGroupDao;

   private INodeDAO nodeDao;

   private INetworkDAO networkDAO;

   private IServerInfoDAO serverInfoDao;
   private SoftwareManagerCollector softwareManagerCollector;

   public IServerInfoDAO getServerInfoDao(){
      return serverInfoDao;
   }

   @Autowired
   public void setServerInfoDao(IServerInfoDAO serverInfoDao) {
      this.serverInfoDao = serverInfoDao;
   }

   public IClusterDAO getClusterDao() {
      return clusterDao;
   }

   @Autowired
   public void setClusterDao(IClusterDAO clusterDao) {
      this.clusterDao = clusterDao;
   }

   public INodeGroupDAO getNodeGroupDao() {
      return nodeGroupDao;
   }

   @Autowired
   public void setNodeGroupDao(INodeGroupDAO nodeGroupDao) {
      this.nodeGroupDao = nodeGroupDao;
   }

   public INodeDAO getNodeDao() {
      return nodeDao;
   }

   @Autowired
   public void setNodeDao(INodeDAO nodeDao) {
      this.nodeDao = nodeDao;
   }

   public INetworkDAO getNetworkDAO() {
      return networkDAO;
   }

   @Autowired
   public void setNetworkDAO(INetworkDAO networkDAO) {
      this.networkDAO = networkDAO;
   }

   @Autowired
   public void setSoftwareManagerCollector(SoftwareManagerCollector softwareManagerCollector) {
        this.softwareManagerCollector = softwareManagerCollector;
   }

   public ClusterEntity findClusterById(Long id) {
      return clusterDao.findById(id);
   }

   public NodeGroupEntity findNodeGroupById(Long id) {
      return nodeGroupDao.findById(id);
   }

   public NodeEntity findNodeById(Long id) {
      return nodeDao.findById(id);
   }

   public ClusterEntity findByName(String clusterName) {
      return clusterDao.findByName(clusterName);
   }

   public NodeGroupEntity findByName(String clusterName, String groupName) {
      return nodeGroupDao.findByName(clusterDao.findByName(clusterName),
            groupName);
   }

   public NodeGroupEntity findByName(ClusterEntity cluster, String groupName) {
      return nodeGroupDao.findByName(cluster, groupName);
   }

   public NodeEntity findByName(String clusterName, String groupName,
         String nodeName) {
      return nodeDao.findByName(findByName(clusterName, groupName), nodeName);
   }

   public NodeEntity findByName(NodeGroupEntity nodeGroup, String nodeName) {
      return nodeDao.findByName(nodeGroup, nodeName);
   }

   public NodeEntity findNodeByName(String nodeName) {
      return nodeDao.findByName(nodeName);
   }

   public List<String> findByAppManager(String appManagerName) {
      return clusterDao.findClustersByAppManager(appManagerName);
   }

   public List<ClusterEntity> findAllClusters() {
      return clusterDao.findAll();
   }

   public List<NodeGroupEntity> findAllGroups(String clusterName) {
      List<ClusterEntity> clusters = new ArrayList<ClusterEntity>();
      ClusterEntity cluster = clusterDao.findByName(clusterName);
      clusters.add(cluster);

      return nodeGroupDao.findAllByClusters(clusters);
   }

   public List<NodeEntity> findAllNodes(String clusterName) {
      return clusterDao.getAllNodes(clusterName);
   }

   public List<NodeEntity> findAllNodes(String clusterName, String groupName) {
      NodeGroupEntity nodeGroup = findByName(clusterName, groupName);
      return new ArrayList<NodeEntity>(nodeGroup.getNodes());
   }

   @Transactional
   @RetryTransaction
   public void insert(ClusterEntity cluster) {
      AuAssert.check(cluster != null);
      clusterDao.insert(cluster);
   }

   @Transactional
   @RetryTransaction
   public void insert(NodeEntity node) {
      AuAssert.check(node != null);
      nodeDao.insert(node);
   }

   @Transactional
   @RetryTransaction
   public void delete(NodeEntity node) {
      AuAssert.check(node != null);
      // remove from parent's collection by cascading
      NodeGroupEntity parent = node.getNodeGroup();
      parent.getNodes().remove(node);
      nodeDao.delete(node);
   }

   @Transactional
   @RetryTransaction
   public void delete(ClusterEntity cluster) {
      AuAssert.check(cluster != null);
      clusterDao.delete(cluster);
   }

   @Transactional
   @RetryTransaction
   public void updateClusterStatus(String clusterName, ClusterStatus status) {
      clusterDao.updateStatus(clusterName, status);
   }

   @Transactional
   @RetryTransaction
   public void updateNodesAction(String clusterName, String action) {
      List<NodeEntity> nodes = clusterDao.getAllNodes(clusterName);
      for (NodeEntity node : nodes) {
         updateNodeAction(node, action);
      }
   }

   @Transactional
   @RetryTransaction
   public void updateNodeAction(NodeEntity node, String action) {
      if (node.needUpgrade(getServerVersion()) && node.canBeUpgrade()) {
         nodeDao.updateAction(node.getMoId(), action);
      }
   }

   @Transactional
   @RetryTransaction
   public void update(ClusterEntity clusterEntity) {
      clusterDao.update(clusterEntity);
   }

   @Transactional
   @RetryTransaction
   public void update(NodeGroupEntity group) {
      nodeGroupDao.update(group);
   }

   @Transactional
   @RetryTransaction
   public void update(NodeEntity node) {
      nodeDao.update(node);
   }

   @Transactional
   @RetryTransaction
   public void updateDisks(String nodeName, List<DiskEntity> diskSets) {
      NodeEntity node = findNodeByName(nodeName);
      for (DiskEntity disk : diskSets) {
         boolean found = false;
         for (DiskEntity old : node.getDisks()) {
            if (disk.getName().equals(old.getName())) {
               found = true;
               old.setDatastoreName(disk.getDatastoreName());
               old.setDatastoreMoId(disk.getDatastoreMoId());
               old.setVmdkPath(disk.getVmdkPath());
               old.setSizeInMB(disk.getSizeInMB());
            }
         }
         if (!found) {
            disk.setNodeEntity(node);
            node.getDisks().add(disk);
         }
      }
   }

   @Transactional
   @RetryTransaction
   public boolean handleOperationStatus(String clusterName,
         OperationStatusWithDetail status, boolean lastUpdate) {
      logger.info("handle operation status: " + status.getOperationStatus());
      boolean finished = status.getOperationStatus().isFinished();
      final Map<String, GroupData> groups = status.getClusterData().getGroups();

      ClusterEntity cluster = findByName(clusterName);
      AuAssert.check(cluster.getId() != null);
      for (NodeGroupEntity group : cluster.getNodeGroups()) {
         for (String groupName : groups.keySet()) {
            if (groupName.equals(group.getName())) {
               for (ServerData serverData : groups.get(groupName)
                     .getInstances()) {
                  logger.debug("server data: " + serverData.getName()
                        + ", action:" + serverData.getAction() + ", status:"
                        + serverData.getStatus());
                  Iterator<NodeEntity> iter = group.getNodes().iterator();
                  while (iter.hasNext()) {
                     NodeEntity oldNode = iter.next();
                     if (oldNode.getVmName().equals(serverData.getName())) {
                        logger.debug("old node:" + oldNode.getVmName()
                              + ", status: " + oldNode.getStatus());
                        oldNode.setAction(serverData.getAction());
                        logger.debug("node status: "
                              + NodeStatus.fromString(serverData.getStatus()));
                        String errorMsg = serverData.getError_msg();
                        if (lastUpdate && errorMsg != null && !errorMsg.isEmpty()) {
                           oldNode.setActionFailed(true);
                           oldNode.setErrMessage(errorMsg);
                           logger.debug("error message: " + errorMsg);
                        }
                        if (!oldNode.isDisconnected()) {
                           oldNode.setStatus(
                                 NodeStatus.fromString(serverData.getStatus()),
                                 false);
                           logger.debug("new node:" + oldNode.getVmName()
                                 + ", status: " + oldNode.getStatus());
                        } else {
                           logger.debug("do not override node status for disconnected node.");
                        }

                        update(oldNode);
                        break;
                     }
                  }
               }
            }
         }
      }
      logger.debug("updated database");
      return finished;
   }

   @Transactional
   @RetryTransaction
   public void setClusterStatus(String clusterName, ClusterReport report) {
      // process cluster status
      handleClusterStatus(clusterName, report);
      // process node status
      handleNodeStatus(report, true);
   }

   private void handleClusterStatus(String clusterName, ClusterReport report) {
      ClusterEntity cluster = findByName(clusterName);
      ClusterStatus oldState = cluster.getStatus();
      switch (oldState) {
         case RUNNING:
         case SERVICE_STOPPED:
         case SERVICE_WARNING:
            switch (report.getStatus()) {
               case STARTED:
                  cluster.setStatus(ClusterStatus.RUNNING);
                  break;
               case ALERT:
                  cluster.setStatus(ClusterStatus.SERVICE_WARNING);
                  break;
               case STOPPED:
                  cluster.setStatus(ClusterStatus.SERVICE_STOPPED);
                  break;
               default:
                  break;
            }
            logger.info("Got status " + report.getStatus()
                  + ", change cluster status from " + oldState
                  + " to " + cluster.getStatus());
            break;
         default:
            logger.debug("In status " + cluster.getStatus() +
                  ". Do not change cluster status based on service status change.");
            break;
      }
   }

   @Transactional
   @RetryTransaction
   public boolean handleOperationStatus(String clusterName,
         ClusterReport report, boolean lastUpdate) {
      handleClusterStatus(clusterName, report);
      return handleNodeStatus(report, lastUpdate);
   }

   private boolean handleNodeStatus(ClusterReport report, boolean lastUpdate) {
      boolean finished = report.isFinished();
      ClusterEntity cluster = findByName(report.getName());
      Map<String, NodeReport> nodeReportMap = report.getNodeReports();
      for (NodeGroupEntity group : cluster.getNodeGroups()) {
         for (NodeEntity node : group.getNodes()) {
            NodeReport nodeReport = nodeReportMap.get(node.getVmName());
            if (nodeReport == null) {
               continue;
            }
            if (nodeReport.getStatus() != null) {
               if (!node.isDisconnected() 
                     && node.getStatus().ordinal() >= NodeStatus.VM_READY.ordinal()) {
                  logger.debug("Got node " + node.getVmName() 
                        + " status " + nodeReport.getStatus().toString());
                  NodeStatus oldStatus = node.getStatus();
                  switch (nodeReport.getStatus()) {
                  case STARTED:
                     node.setStatus(NodeStatus.SERVICE_READY, false);
                     break;
                  case UNHEALTHY:
                     node.setStatus(NodeStatus.SERVICE_UNHEALTHY, false);
                     break;
                  case ALERT:
                     if (node.getStatus() != NodeStatus.BOOTSTRAP_FAILED) {
                        node.setStatus(NodeStatus.SERVICE_ALERT, false);
                     }
                     break;
                  case UNKONWN:
                     node.setStatus(NodeStatus.UNKNOWN, false);
                     break;
                  case PROVISIONING:
                  case STOPPED:
                     if (node.getStatus() != NodeStatus.BOOTSTRAP_FAILED) {
                        node.setStatus(NodeStatus.VM_READY, false);
                     }
                     break;
                  default:
                     node.setStatus(NodeStatus.BOOTSTRAP_FAILED, false);
                  }
                  logger.debug("node:" + node.getVmName()
                        + ", status changed from old status: " + oldStatus
                        + " to new status: " + node.getStatus());
               }
            }
            if (nodeReport.isUseClusterMsg() && report.getAction() != null) {
               logger.debug("set node action to:" + report.getAction());
               node.setAction(report.getAction());
            } else if (nodeReport.getAction() != null) {
               node.setAction(nodeReport.getAction());
            }
            if (lastUpdate) {
               if (nodeReport.getErrMsg() != null) {
                  logger.debug("set node error message to:" + report.getAction());
                  node.setErrMessage(nodeReport.getErrMsg());
                  node.setActionFailed(true);
               } else {
                  logger.debug("clear node error message for node " + node.getHostName());
                  node.setErrMessage(null);
                  node.setActionFailed(false);
               }
            }
         }
      }
      return finished;
   }

   private void setNotExist(NodeEntity node) {
      logger.debug("vm " + node.getVmName()
            + " does not exist. Update node status to NOT_EXIST.");
      node.setStatus(NodeStatus.NOT_EXIST);
      node.resetNicsInfo();
      node.setHostName(null);
      node.setMoId(null);
      if (node.getAction() != null
            && !(node.getAction().equals(Constants.NODE_ACTION_CLONING_VM))
            && !(node.getAction().equals(Constants.NODE_ACTION_CLONING_FAILED))) {
         node.setAction(null);
      }
      update(node);
   }

   @Transactional
   @RetryTransaction
   public void syncUp(String clusterName, boolean updateClusterStatus) {
      List<NodeEntity> nodes = findAllNodes(clusterName);

      boolean allNodesDown = true;
      for (NodeEntity node : nodes) {
         refreshNodeStatus(node, false);
         if (node.getStatus().ordinal() >= NodeStatus.POWERED_ON.ordinal()) {
            allNodesDown = false;
         }
      }

      if (updateClusterStatus && allNodesDown) {
         ClusterEntity cluster = findByName(clusterName);
         if (cluster.getStatus() == ClusterStatus.RUNNING) {
            logger.info("All nodes are powered off, switch cluster status to stopped.");
            cluster.setStatus(ClusterStatus.STOPPED);
         }
      }
   }

   @Transactional
   @RetryTransaction
   public void removeVmReference(String vmId) {
      NodeEntity node = nodeDao.findByMobId(vmId);
      if (node != null) {
         setNotExist(node);
      }
   }

   @Transactional
   @RetryTransaction
   public void syncUpNode(String clusterName, String nodeName) {
      NodeEntity node = findNodeByName(nodeName);
      if (node != null) {
         refreshNodeStatus(node, false);
      }
   }

   @Transactional
   @RetryTransaction
   public List<String> getPortGroupNames(String clusterName) {
      ClusterEntity clusterEntity = clusterDao.findByName(clusterName);
      List<String> portGroups = new ArrayList<String>();
      for (String networkName : clusterEntity.fetchNetworkNameList()) {
         portGroups.add(networkDAO.findNetworkByName(networkName)
               .getPortGroup());
      }
      return portGroups;
   }

   private void refreshNodeStatus(NodeEntity node, boolean inSession) {
      String mobId = node.getMoId();
      if (mobId == null) {
         setNotExist(node);
         return;
      }
      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(mobId);
      if (vcVm == null) {
         // vm is deleted
         setNotExist(node);
         return;
      }
      if (!vcVm.isConnected() || vcVm.getHost().isUnavailbleForManagement()) {
         node.setUnavailableConnection();
         return;
      }
      // TODO: consider more status
      if (!vcVm.isPoweredOn()) {
         node.setStatus(NodeStatus.POWERED_OFF);
         node.resetNicsInfo();
      } else {
         node.setStatus(NodeStatus.POWERED_ON);
      }

      if (vcVm.isPoweredOn()) {
         //update ip address
         for (NicEntity nicEntity : node.getNics()) {
            VcVmUtil.populateNicInfo(nicEntity, node.getMoId(), nicEntity.getNetworkEntity().getPortGroup());
         }

         if (node.nicsReady()) {
            node.setStatus(NodeStatus.VM_READY);
            if (node.getAction() != null
                  && (node.getAction().equals(Constants.NODE_ACTION_WAITING_IP) || node
                        .getAction().equals(Constants.NODE_ACTION_RECONFIGURE))) {
               node.setAction(null);
            }
         }
         String guestHostName = VcVmUtil.getGuestHostName(vcVm, inSession);
         if (guestHostName != null) {
            node.setGuestHostName(guestHostName);
         }
      }
      node.setHostName(vcVm.getHost().getName());
      update(node);
   }

   public ClusterBlueprint toClusterBluePrint(String clusterName) {
      ClusterEntity clusterEntity = findByName(clusterName);
      ClusterBlueprint blueprint = new ClusterBlueprint();
      Gson gson = new Gson();

      blueprint.setName(clusterEntity.getName());
      blueprint.setInstanceNum(clusterEntity.getRealInstanceNum(true));
      // TODO: topology
      if (clusterEntity.getHadoopConfig() != null) {
         Map<String, Object> clusterConfigs = gson.fromJson(clusterEntity.getHadoopConfig(), Map.class);
         blueprint.setConfiguration(clusterConfigs);
      }

      // set HadoopStack
      HadoopStack hadoopStack = new HadoopStack();
      hadoopStack.setDistro(clusterEntity.getDistro());
      hadoopStack.setVendor(clusterEntity.getDistroVendor());
      hadoopStack.setFullVersion(clusterEntity.getDistroVersion());
      blueprint.setHadoopStack(hadoopStack);

      // set nodes/nodegroups
      List<NodeGroupInfo> nodeGroupInfos = new ArrayList<NodeGroupInfo>();
      for (NodeGroupEntity group : clusterEntity.getNodeGroups()) {
         NodeGroupInfo nodeGroupInfo = toNodeGroupInfo(group);
         nodeGroupInfos.add(nodeGroupInfo);
      }
      blueprint.setNodeGroups(nodeGroupInfos);
      return blueprint;
   }

   private NodeGroupInfo toNodeGroupInfo(NodeGroupEntity group) {
      Gson gson = new Gson();
      NodeGroupInfo nodeGroupInfo = new NodeGroupInfo();
      nodeGroupInfo.setName(group.getName());
      nodeGroupInfo.setInstanceNum(group.getRealInstanceNum(true));
      nodeGroupInfo.setRoles(gson.fromJson(group.getRoles(), List.class));
      if (group.getHadoopConfig() != null) {
         Map<String, Object> groupConfigs = gson.fromJson(group.getHadoopConfig(), Map.class);
         nodeGroupInfo.setConfiguration(groupConfigs);
      }
      if (group.getHaFlag().equalsIgnoreCase(Constants.HA_FLAG_FT) ||
            group.getHaFlag().equalsIgnoreCase(Constants.HA_FLAG_ON)) {
         nodeGroupInfo.setHaEnabled(true);
      }
      nodeGroupInfo.setInstanceType(group.getNodeType());
      nodeGroupInfo.setStorageSize(group.getStorageSize());
      nodeGroupInfo.setStorageType(group.getStorageType().name());

      // set nodes
      List<NodeInfo> nodeInfos = new ArrayList<NodeInfo>();
      for (NodeEntity node : group.getNodes()) {
         NodeInfo nodeInfo = new NodeInfo();
         nodeInfo.setName(node.getVmName());
         nodeInfo.setHostname(node.getGuestHostName());
         nodeInfo.setIpConfigs(node.convertToIpConfigInfo());
         nodeInfo.setRack(node.getRack());
         nodeInfo.setVolumes(node.getDataVolumnsMountPoint());
         nodeInfos.add(nodeInfo);
      }

      nodeGroupInfo.setNodes(nodeInfos);
      return nodeGroupInfo;

   }

   public NodeGroupInfo toNodeGroupInfo(String clusterName, String groupName) {
      NodeGroupEntity group = findByName(clusterName, groupName);
      return toNodeGroupInfo(group);
   }

   public ClusterRead toClusterRead(String clusterName) {
      return toClusterRead(clusterName, false);
   }

   @SuppressWarnings("rawtypes")
   public ClusterRead toClusterRead(String clusterName,
         boolean ignoreObsoleteNode) {
      ClusterEntity cluster = findByName(clusterName);
      if (cluster == null) {
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }
      ClusterStatus clusterStatus = cluster.getStatus();
      ClusterRead clusterRead = new ClusterRead();
      clusterRead
            .setInstanceNum(cluster.getRealInstanceNum(ignoreObsoleteNode));
      clusterRead.setName(cluster.getName());
      clusterRead.setStatus(clusterStatus);
      clusterRead.setAppManager(cluster.getAppManager());
      clusterRead.setDistro(cluster.getDistro());
      clusterRead.setDistroVendor(cluster.getDistroVendor());
      clusterRead.setTopologyPolicy(cluster.getTopologyPolicy());
      clusterRead.setAutomationEnable(cluster.getAutomationEnable());
      clusterRead.setVhmMinNum(cluster.getVhmMinNum());
      clusterRead.setVhmMaxNum(cluster.getVhmMaxNum());
      clusterRead.setVhmTargetNum(cluster.getVhmTargetNum());
      clusterRead.setIoShares(cluster.getIoShares());
      clusterRead.setVersion(cluster.getVersion());
      if (!CommonUtil.isBlank(cluster.getAdvancedProperties())) {
         Gson gson = new Gson();
         Map<String, String> advancedProperties = gson.fromJson(cluster.getAdvancedProperties(), Map.class);
         clusterRead.setExternalHDFS(advancedProperties.get("ExternalHDFS"));
         clusterRead.setExternalMapReduce(advancedProperties.get("ExternalMapReduce"));
      }

      SoftwareManager softMgr =
            softwareManagerCollector
                  .getSoftwareManager(cluster.getAppManager());
      if (softMgr == null) {
         logger.error("Failed to get softwareManger.");
         // do not throw exception for exporting cluster info
      }
      List<NodeGroupRead> groupList = new ArrayList<NodeGroupRead>();
      for (NodeGroupEntity group : cluster.getNodeGroups()) {
         NodeGroupRead groupRead = group.toNodeGroupRead(ignoreObsoleteNode);
         groupRead.setComputeOnly(false);
         try {
            groupRead.setComputeOnly(softMgr.isComputeOnlyRoles(groupRead.getRoles()));
         } catch (Exception e) {
         }
         groupList.add(groupRead);
      }

      clusterRead.setNodeGroups(groupList);

      Set<VcResourcePoolEntity> rps = cluster.getUsedRps();
      List<ResourcePoolRead> rpReads =
            new ArrayList<ResourcePoolRead>(rps.size());
      for (VcResourcePoolEntity rp : rps) {
         ResourcePoolRead rpRead = rp.toRest();
         rpRead.setNodes(null);
         rpReads.add(rpRead);
      }
      clusterRead.setResourcePools(rpReads);

      if (clusterStatus.isActiveServiceStatus()
            || clusterStatus == ClusterStatus.STOPPED) {
         clusterRead.setDcSeperation(clusterRead.validateSetManualElasticity());
      }

      return clusterRead;
   }

   @Transactional
   @RetryTransaction
   public void refreshNodeByMobId(String vmId, boolean inSession) {
      NodeEntity node = nodeDao.findByMobId(vmId);
      if (node != null) {
         refreshNodeStatus(node, inSession);
      }
   }

   @Transactional
   @RetryTransaction
   public void setNodeConnectionState(String vmName) {
      NodeEntity node = nodeDao.findByName(vmName);
      if (node != null) {
         node.setUnavailableConnection();
      }
   }

   @Transactional
   @RetryTransaction
   public void refreshNodeByMobId(String vmId, String action, boolean inSession) {
      NodeEntity node = nodeDao.findByMobId(vmId);
      if (node != null) {
         node.setAction(action);
         refreshNodeStatus(node, inSession);
      }
   }

   public NodeEntity getNodeByMobId(String vmId) {
      return nodeDao.findByMobId(vmId);
   }

   @Transactional
   public NodeEntity getNodeWithNicsByMobId(String vmId) {
      NodeEntity nodeEntity = nodeDao.findByMobId(vmId);
      Hibernate.initialize(nodeEntity.getNics());
      return nodeEntity;
   }

   public NodeEntity getNodeByVmName(String vmName) {
      return nodeDao.findByName(vmName);
   }

   public List<NodeEntity> getNodesByHost(String hostName) {
      return nodeDao.findByHostName(hostName);
   }

   @Transactional
   @RetryTransaction
   public void refreshNodeByVmName(String vmId, String vmName, boolean inSession) {
      NodeEntity node = nodeDao.findByName(vmName);
      if (node != null) {
         node.setMoId(vmId);
         refreshNodeStatus(node, inSession);
      }
   }

   @Transactional
   @RetryTransaction
   public void refreshNodeByVmName(String vmId, String vmName,
         String nodeAction, boolean inSession) {
      NodeEntity node = nodeDao.findByName(vmName);
      if (node != null) {
         node.setMoId(vmId);
         node.setAction(nodeAction);
         refreshNodeStatus(node, inSession);
      }
   }

   @Transactional
   @RetryTransaction
   public void updateClusterTaskId(String clusterName, Long taskId) {
      ClusterEntity cluster = clusterDao.findByName(clusterName);
      cluster.setLatestTaskId(taskId);
      clusterDao.update(cluster);
   }

   public List<Long> getLatestTaskIds() {
      List<ClusterEntity> clusters = clusterDao.findAll();
      List<Long> taskIds = new ArrayList<Long>(clusters.size());

      for (ClusterEntity cluster : clusters) {
         taskIds.add(cluster.getLatestTaskId());
      }

      return taskIds;
   }

   public List<DiskEntity> getDisks(String nodeName) {
      NodeEntity node = nodeDao.findByName(nodeName);
      return new ArrayList<DiskEntity>(node.getDisks());
   }

   @Transactional
   @RetryTransaction
   public void cleanupActionError(String clusterName) {
      List<NodeEntity> nodes = findAllNodes(clusterName);
      for (NodeEntity node : nodes) {
         node.cleanupErrorMessage();
      }
   }

   @Transactional
   @RetryTransaction
   public boolean needUpgrade(String clusterName) {
      String serverVersion = getServerVersion();
      String clusterVersion = findByName(clusterName).getVersion();
      List<NodeEntity> nodes = findAllNodes(clusterName);
      boolean allNodesUpgraded = true;
      for (NodeEntity node : nodes) {
         if (node.canBeUpgrade() && node.needUpgrade(serverVersion)) {
            allNodesUpgraded = false;
            break;
         }
      }
      return clusterVersion == null || !serverVersion.equals(clusterVersion) || !allNodesUpgraded;
   }

   @Transactional
   @RetryTransaction
   public String getServerVersion() {
      List<ServerInfoEntity> serverInfoEntities = getServerInfoDao().findAll();
      if (serverInfoEntities.isEmpty()){
         return null;
      }
      ServerInfoEntity serverInfoEntity = serverInfoEntities.get(0);
      String serverVersion = serverInfoEntity.getVersion();
      return serverVersion;
   }

   @Transactional
   @RetryTransaction
   public void storeClusterLastStatus(String clusterName) {
      ClusterStatus clusterStatus = clusterDao.getStatus(clusterName);
      if (!ClusterStatus.UPGRADE_ERROR.equals(clusterStatus) && !ClusterStatus.UPGRADING.equals(clusterStatus)) {
         clusterDao.updateLastStatus(clusterName, clusterStatus);
      }
   }

   @Transactional
   @RetryTransaction
   public void cleanupErrorForClusterUpgrade(String clusterName) {
      List<NodeEntity> nodes = findAllNodes(clusterName);
      for (NodeEntity node : nodes) {
         node.cleanupErrorMessageForUpgrade();
      }
   }
   public void update(Observable o, Object arg) {
      // TODO
   }

}
