/***************************************************************************

 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.bdd.manager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.HadoopNodeEntity;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NetworkEntity.AllocType;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterManagerException;
import com.vmware.bdd.manager.task.CommandTaskWorker;
import com.vmware.bdd.manager.task.ConfigureClusterListener;
import com.vmware.bdd.manager.task.CreateClusterListener;
import com.vmware.bdd.manager.task.DeleteClusterListener;
import com.vmware.bdd.manager.task.MessageTaskWorker;
import com.vmware.bdd.manager.task.QueryClusterListener;
import com.vmware.bdd.manager.task.StartClusterListener;
import com.vmware.bdd.manager.task.StopClusterListener;
import com.vmware.bdd.manager.task.TaskListener;
import com.vmware.bdd.manager.task.UpdateClusterListener;
import com.vmware.bdd.manager.task.VHMReceiveListener;
import com.vmware.bdd.specpolicy.ClusterSpecFactory;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ClusterCmdUtil;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;

public class ClusterManager {

   static final Logger logger = Logger.getLogger(ClusterManager.class);
   private ClusterConfigManager clusterConfigMgr;
   private CloudProviderManager cloudProviderMgr;
   private NetworkManager networkManager;
   private TaskManager taskManager;

   public ClusterConfigManager getClusterConfigMgr() {
      return clusterConfigMgr;
   }

   public void setClusterConfigMgr(ClusterConfigManager clusterConfigMgr) {
      this.clusterConfigMgr = clusterConfigMgr;
   }

   public CloudProviderManager getCloudProviderMgr() {
      return cloudProviderMgr;
   }

   public void setCloudProviderMgr(CloudProviderManager cloudProviderMgr) {
      this.cloudProviderMgr = cloudProviderMgr;
   }

   public NetworkManager getNetworkManager() {
      return networkManager;
   }

   public void setNetworkManager(NetworkManager networkManager) {
      this.networkManager = networkManager;
   }

   public TaskManager getTaskManager() {
      return taskManager;
   }

   public void setTaskManager(TaskManager taskManager) {
      this.taskManager = taskManager;
   }

   public Map<String, Object> getClusterConfigManifest(
         final String clusterName, List<String> targets) {
      ClusterCreate clusterConfig =
            clusterConfigMgr.getClusterConfig(clusterName);
      Map<String, Object> cloudProvider = cloudProviderMgr.getAttributes();
      ClusterRead read = getClusterByName(clusterName, false);
      Map<String, Object> attrs = new HashMap<String, Object>();
      attrs.put("cloud_provider", cloudProvider);
      attrs.put("cluster_definition", clusterConfig);
      if (read != null) {
         attrs.put("cluster_data", read);
      }
      if (targets != null && !targets.isEmpty()) {
         attrs.put("targets", targets);
      }
      return attrs;
   }

   private void writeJsonFile(Map<String, Object> clusterConfig, File workDir,
         String fileName) {
      Gson gson =
            new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                  .setPrettyPrinting().create();
      String jsonStr = gson.toJson(clusterConfig);

      AuAssert.check(jsonStr != null);
      logger.info("writing cluster manifest in json " + jsonStr + " to file "
            + fileName);

      FileWriter fileStream = null;

      try {
         File file = new File(workDir, fileName);
         fileStream = new FileWriter(file);
         fileStream.write(jsonStr);
      } catch (IOException ex) {
         logger.error(ex.getMessage()
               + "\n failed to write cluster manifest to file " + fileName);
         throw BddException.INTERNAL(ex, "failed to write cluster manifest");
      } finally {
         if (fileStream != null) {
            try {
               fileStream.close();
            } catch (IOException e) {
               logger.error("falied to close output stream " + fileStream, e);
            }
         }
      }
   }

   private Long createClusterMgmtTaskWithErrorSetting(List<String> targets,
         ClusterEntity cluster, TaskListener listener, ClusterStatus initStatus) {
      try {
         return createClusterMgmtTask(targets, cluster, listener, initStatus);
      } catch (BddException e) {
         logger.error("failed to create cluster management task.", e);
         cluster.setStatus(ClusterStatus.ERROR);
         DAL.inTransactionUpdate(cluster);
         throw e;
      }
   }

   private Long createClusterMgmtTaskWithErrorSetting(String target,
         ClusterEntity cluster, TaskListener listener, ClusterStatus initStatus) {
      List<String> targets = new ArrayList<String>(1);
      targets.add(target);
      return createClusterMgmtTaskWithErrorSetting(targets, cluster, listener,
            initStatus);
   }

   private Long createClusterMgmtTaskWithErrorSetting(ClusterEntity cluster,
         TaskListener listener, ClusterStatus initStatus) {
      return createClusterMgmtTaskWithErrorSetting(cluster.getName(), cluster,
            listener, initStatus);
   }

   private Long createClusterMgmtTask(List<String> targets,
         final ClusterEntity cluster, final TaskListener listener, final ClusterStatus initStatus) {
      final String fileName = cluster.getName() + ".json";

      final Map<String, Object> clusterConfig =
            getClusterConfigManifest(cluster.getName(), targets);

      AuAssert.check(clusterConfig != null);

      TaskEntity task = taskManager.createCmdlineTask(null, listener);

      String[] cmdArray =
            listener.getTaskCommand(cluster.getName(), task.getWorkDir()
                  .getAbsolutePath() + "/" + fileName);

      AuAssert.check(cmdArray != null);
      task.setCmdArray(cmdArray);

      DAL.inTransactionUpdate(task);

      HashMap<String, Object> properties = SystemProperties.getManifest();
      SystemProperties.setChannelId(properties, task.getMessageRouteKey());
      clusterConfig.put("system_properties", properties);

      writeJsonFile(clusterConfig, task.getWorkDir(), fileName);

      if (initStatus != null) {
         ClusterEntity.updateStatus(cluster.getName(), initStatus);
      }

      taskManager.submit(task, new CommandTaskWorker());

      StringBuilder cmdStr = new StringBuilder();
      for (String str : cmdArray) {
         cmdStr.append(str).append(" ");
      }

      logger.info("submitted a start cluster task with cmd array: " + cmdStr);

      return task.getId();
   }

   private Long createClusterMgmtTask(ClusterEntity cluster,
         TaskListener listener, ClusterStatus initStatus) {
      List<String> targets = new ArrayList<String>(1);
      targets.add(cluster.getName());
      return createClusterMgmtTask(targets, cluster, listener, initStatus);
   }

   private boolean removeEmptyCluster(final String clusterName) {

      return DAL.inRwTransactionDo(new Saveable<Boolean>() {
         private boolean validated;

         @Override
         public Boolean body() throws Exception {
            ClusterEntity cluster =
                  ClusterEntity.findClusterEntityByName(clusterName);
            AuAssert.check(cluster != null);
            if (cluster.getRealInstanceNum() == 0) {
               logger.warn("cluster real instance number is zero !");
               if (cluster.getNetwork().getAllocType() == NetworkEntity.AllocType.IP_POOL) {
                  networkManager.free(cluster.getNetwork(), cluster.getId());
               }
               cluster.delete();
               validated = true;
            } else {
               validated = false;
            }
            return validated;
         }
      });
   }

   private Long deleteEmptyClusterSuccess(ClusterEntity cluster, DeleteClusterListener listener) {
      TaskEntity task = taskManager.createCmdlineTask(null, listener);
      final String fileName = cluster.getName() + ".json";
      String[] cmdArray =
         listener.getTaskCommand(cluster.getName(), task.getWorkDir()
               .getAbsolutePath() + "/" + fileName);
      AuAssert.check(cmdArray != null);
      task.setCmdArray(cmdArray);
      task.setStatus(Status.SUCCESS);
      task.setProgress(1.0);
      DAL.inTransactionUpdate(task);
      return task.getId();
   }

   public ClusterRead getClusterByName(final String clusterName, final boolean realTime) {
      ClusterEntity cluster = ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      if (realTime && cluster.inStableStatus()) {
         // query real time cluster data
         QueryClusterListener listener = new QueryClusterListener(clusterName);

         // no need to update status, set initStatus to null
         Long taskId = createClusterMgmtTaskWithErrorSetting(cluster, listener, null);
         ClusterCmdUtil.waitForTask(taskId);
      }

      // return the latest data from db, even when the query task failed
      return DAL.autoTransactionDo(new Saveable<ClusterRead>() {
         @Override
         public ClusterRead body() {
            ClusterEntity cluster = ClusterEntity.findClusterEntityByName(clusterName);
            return cluster.toClusterRead();
         }
      });
   }

   public ClusterCreate getClusterSpec(String clusterName) {
      ClusterCreate spec = clusterConfigMgr.getClusterConfig(clusterName);
      spec.setVcClusters(null);
      spec.setTemplateId(null);
      spec.setDistroMap(null);
      spec.setSharedPattern(null);
      spec.setLocalPattern(null);
      spec.setNetworking(null);
      spec.setRpNames(null);
      spec.setDsNames(null);
      spec.setNetworkName(null);
      spec.setName(null);
      spec.setDistro(null);
      spec.setValidateConfig(null);
      spec.setTopologyPolicy(null);
      spec.setHostToRackMap(null);
      NodeGroupCreate[] groups = spec.getNodeGroups();
      if (groups != null) {
         for (NodeGroupCreate group : groups) {
            group.setVcClusters(null);
            group.setGroupType(null);
            group.setRpNames(null);
            group.getStorage().setDsNames(null);
            group.getStorage().setNamePattern(null);
            group.getStorage().setDiskBisect(false);
            group.setVmFolderPath(null);
         }
      }
      return spec;
   }

   public List<ClusterRead> getClusters(Boolean realTime) {
      List<ClusterRead> clusters = new ArrayList<ClusterRead>();
      List<ClusterEntity> clusterEntities = DAL.findAll(ClusterEntity.class);
      for (ClusterEntity entity : clusterEntities) {
         clusters.add(getClusterByName(entity.getName(), realTime));
      }
      return clusters;
   }

   public Long createCluster(ClusterCreate createSpec) throws Exception {
      createSpec = ClusterSpecFactory.getCustomizedSpec(createSpec);

      String name = createSpec.getName();
      logger.info("ClusterManager, creating cluster " + name);

      final ClusterEntity cluster =
            clusterConfigMgr.createClusterConfig(createSpec);

      CreateClusterListener listener = new CreateClusterListener(name);
      try {
         return createClusterMgmtTaskWithErrorSetting(cluster, listener,
               ClusterStatus.PROVISIONING);
      } catch (BddException e) {
         logger.error(
               "Create management task failed. Delete cluster entity, and release resources.",
               e);
         DAL.inTransactionDo(new Saveable<Void>() {
            public Void body() throws Exception {
               NetworkEntity network = cluster.getNetwork();
               DAL.refresh(network);
               if (network.getAllocType() == AllocType.IP_POOL) {
                  networkManager.free(network, cluster.getId());
               }
               cluster.delete();
               return null;
            }
         });
         throw e;
      }
   }

   public Long configCluster(String clusterName, ClusterCreate createSpec)
         throws Exception {
      logger.info("ClusterManager, config cluster " + clusterName);
      ClusterEntity cluster;

      if ((cluster = ClusterEntity.findClusterEntityByName(clusterName)) == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.CONFIGURE_ERROR.equals(cluster.getStatus())) {
         logger.error("can not config cluster: " + clusterName + ", "
               + cluster.getStatus());
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "it should be in RUNNING status");
      }
      clusterConfigMgr.updateAppConfig(clusterName, createSpec);
      ConfigureClusterListener listener =
            new ConfigureClusterListener(clusterName);
      return createClusterMgmtTaskWithErrorSetting(cluster, listener,
            ClusterStatus.CONFIGURING);
   }

   public Long resumeClusterCreation(String clusterName) throws Exception {
      logger.info("ClusterManager, resume cluster creation " + clusterName);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      if (cluster.getStatus() != ClusterStatus.PROVISION_ERROR) {
         logger.error("can not resume creation of cluster: " + clusterName
               + ", " + cluster.getStatus());
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "it should be in PROVISION_ERROR status");
      }

      CreateClusterListener listener = new CreateClusterListener(clusterName);
      return createClusterMgmtTaskWithErrorSetting(cluster, listener,
            ClusterStatus.PROVISIONING);
   }

   public Long deleteClusterByName(String clusterName) throws Exception {
      logger.info("ClusterManager, deleting cluster " + clusterName);

      ClusterEntity cluster;

      if ((cluster = ClusterEntity.findClusterEntityByName(clusterName)) == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())
            && !ClusterStatus.PROVISION_ERROR.equals(cluster.getStatus())
            && !ClusterStatus.CONFIGURE_ERROR.equals(cluster.getStatus())) {
         logger.error("cluster: " + clusterName
               + " cannot be deleted, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.DELETION_NOT_ALLOWED_ERROR(clusterName,
               "it should be in RUNNING/STOPPED/ERROR/PROVISION_ERROR status");
      }
      DeleteClusterListener listener =
            new DeleteClusterListener(clusterName, networkManager);
      if(removeEmptyCluster(clusterName)) {
         return deleteEmptyClusterSuccess(cluster, listener);
      }
      return createClusterMgmtTaskWithErrorSetting(cluster, listener,
            ClusterStatus.DELETING);
   }

   public Long startCluster(String clusterName) throws Exception {
      logger.info("ClusterManager, starting cluster " + clusterName);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      if (ClusterStatus.RUNNING.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName + " is running already");
         throw ClusterManagerException.ALREADY_STARTED_ERROR(clusterName);
      }

      if (!ClusterStatus.STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " cannot be started, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.START_NOT_ALLOWED_ERROR(clusterName,
               "it should be in STOPPED status");
      }

      StartClusterListener listener = new StartClusterListener(clusterName);
      return createClusterMgmtTaskWithErrorSetting(cluster, listener,
            ClusterStatus.STARTING);
   }

   public Long stopCluster(String clusterName) throws Exception {
      logger.info("ClusterManager, stopping cluster " + clusterName);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      if (ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName + " is stopped already");
         throw ClusterManagerException.ALREADY_STOPPED_ERROR(clusterName);
      }

      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " cannot be stopped, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.STOP_NOT_ALLOWED_ERROR(clusterName,
               "it should be in RUNNING status");
      }

      StopClusterListener listener = new StopClusterListener(clusterName);
      return createClusterMgmtTaskWithErrorSetting(cluster, listener,
            ClusterStatus.STOPPING);
   }

   public Long startNodeGroup(String clusterName, String nodeGroupName)
         throws Exception {
      logger.info("ClusterManager, starting node group "
            + ClusterCmdUtil.getFullNodeName(clusterName, nodeGroupName, null));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      NodeGroupEntity group =
            NodeGroupEntity.findNodeGroupEntityByName(cluster, nodeGroupName);
      if (group == null) {
         logger.error("node group " + nodeGroupName + " does not exist");
         throw BddException.NOT_FOUND("node group", nodeGroupName);
      }
      validateIfOnlyTaskTracker(group);

      StartClusterListener listener =
            new StartClusterListener(clusterName, nodeGroupName, null);
      String target =
            ClusterCmdUtil.getFullNodeName(clusterName, nodeGroupName, null);
      return createClusterMgmtTaskWithErrorSetting(target, cluster, listener,
            null);
   }

   public Long stopNodeGroup(String clusterName, String nodeGroupName)
         throws Exception {
      logger.info("ClusterManager, stopping node group "
            + ClusterCmdUtil.getFullNodeName(clusterName, nodeGroupName, null));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      NodeGroupEntity group =
            NodeGroupEntity.findNodeGroupEntityByName(cluster, nodeGroupName);
      if (group == null) {
         logger.error("node group " + nodeGroupName + " does not exist");
         throw BddException.NOT_FOUND("node group", nodeGroupName);
      }
      validateIfOnlyTaskTracker(group);

      StopClusterListener listener =
            new StopClusterListener(clusterName, nodeGroupName, null);
      String target =
            ClusterCmdUtil.getFullNodeName(clusterName, nodeGroupName, null);
      return createClusterMgmtTaskWithErrorSetting(target, cluster, listener,
            null);
   }

   public Long startNode(String clusterName, String nodeGroupName,
         String nodeName) throws Exception {
      logger.info("ClusterManager, starting node "
            + ClusterCmdUtil.getFullNodeName(clusterName, nodeGroupName,
                  nodeName));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      NodeGroupEntity group =
            NodeGroupEntity.findNodeGroupEntityByName(cluster, nodeGroupName);
      if (group == null) {
         logger.error("node group " + nodeGroupName + " does not exist");
         throw BddException.NOT_FOUND("node group", nodeGroupName);
      }
      validateIfOnlyTaskTracker(group);

      if (HadoopNodeEntity.findByName(group, nodeName) == null) {
         logger.error("node " + nodeName + " does not exist");
         throw BddException.NOT_FOUND("node", nodeName);
      }

      StartClusterListener listener =
            new StartClusterListener(clusterName, nodeGroupName, nodeName);
      String target =
            ClusterCmdUtil
                  .getFullNodeName(clusterName, nodeGroupName, nodeName);
      return createClusterMgmtTaskWithErrorSetting(target, cluster, listener,
            null);
   }

   public Long stopNode(String clusterName, String nodeGroupName,
         String nodeName) throws Exception {
      logger.info("ClusterManager, stopping node "
            + ClusterCmdUtil.getFullNodeName(clusterName, nodeGroupName,
                  nodeName));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      NodeGroupEntity group =
            NodeGroupEntity.findNodeGroupEntityByName(cluster, nodeGroupName);
      if (group == null) {
         logger.error("node group " + nodeGroupName + " does not exist");
         throw BddException.NOT_FOUND("node group", nodeGroupName);
      }
      validateIfOnlyTaskTracker(group);

      if (HadoopNodeEntity.findByName(group, nodeName) == null) {
         logger.error("node " + nodeName + " does not exist");
         throw BddException.NOT_FOUND("node", nodeName);
      }

      StopClusterListener listener =
            new StopClusterListener(clusterName, nodeGroupName, nodeName);
      String target =
            ClusterCmdUtil
                  .getFullNodeName(clusterName, nodeGroupName, nodeName);
      return createClusterMgmtTaskWithErrorSetting(target, cluster, listener,
            null);
   }

   public Long resizeCluster(final String clusterName,
         final String nodeGroupName, final int instanceNum) throws Exception {
      logger.info("ClusterManager, updating node group " + nodeGroupName
            + " in cluster " + clusterName + " reset instance number to "
            + instanceNum);

      final ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("cluster", clusterName);
      }

      final NodeGroupEntity group =
            DAL.inRoTransactionDo(new Saveable<NodeGroupEntity>() {
               @Override
               public NodeGroupEntity body() throws Exception {
                  NodeGroupEntity group =
                        NodeGroupEntity.findNodeGroupEntityByName(cluster,
                              nodeGroupName);
                  if (group == null) {
                     logger.error("nodegroup " + nodeGroupName + " of cluster "
                           + clusterName + " does not exist");
                     throw ClusterManagerException
                           .NOGEGROUP_NOT_FOUND_ERROR(nodeGroupName);
                  }

                  // resize of job tracker and name node is not supported
                  List<String> roles = group.getRoleNameList();
                  List<String> unsupportedRoles = new ArrayList<String>();
                  AuAssert.check(!roles.isEmpty(), "roles should not be empty");
                  if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
                     unsupportedRoles.add(HadoopRole.HADOOP_NAMENODE_ROLE
                           .toString());
                  }
                  if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE
                        .toString())) {
                     unsupportedRoles.add(HadoopRole.HADOOP_JOBTRACKER_ROLE
                           .toString());
                  }
                  if (roles.contains(HadoopRole.ZOOKEEPER_ROLE
                        .toString())) {
                     unsupportedRoles.add(HadoopRole.ZOOKEEPER_ROLE
                           .toString());
                  }
                  if (!unsupportedRoles.isEmpty()) {
                     logger.info("can not resize node group with role: "
                           + unsupportedRoles);
                     throw ClusterManagerException
                           .ROLES_NOT_SUPPORTED(unsupportedRoles);
                  }

                  if (!ClusterStatus.RUNNING.equals(cluster.getStatus())) {
                     logger.error("cluster "
                           + clusterName
                           + " can be updated only in RUNNING status, it is now in "
                           + cluster.getStatus() + " status");
                     throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(
                           clusterName, "it should be in RUNNING status");
                  }

                  if (instanceNum <= group.getDefineInstanceNum()) {
                     logger.error("node group " + nodeGroupName
                           + " cannot be shrinked from "
                           + group.getDefineInstanceNum() + " to "
                           + instanceNum + " nodes");
                     throw ClusterManagerException.SHRINK_OP_NOT_SUPPORTED(
                           nodeGroupName, instanceNum,
                           group.getDefineInstanceNum());
                  }

                  Integer instancePerHost = group.getInstancePerHost();
                  if (instancePerHost != null
                        && instanceNum % instancePerHost != 0) {
                     throw BddException.INVALID_PARAMETER(
                           "instance number",
                           new StringBuilder(100).append(instanceNum)
                                 .append(": not divisiable by instancePerHost")
                                 .toString());
                  }

                  group.validateHostNumber(instanceNum);
                  group.validateIfHostCanSatisfied(instanceNum);

                  return group;
               }
            });

      final int oldInstanceNum = group.getDefineInstanceNum();
      group.setDefineInstanceNum(instanceNum);
      DAL.inTransactionUpdate(group);
      UpdateClusterListener listener = new UpdateClusterListener(clusterName);

      try {
         return createClusterMgmtTask(cluster, listener, ClusterStatus.UPDATING);
      } catch (Exception ex) {
         DAL.inTransactionDo(new Saveable<Void>() {
            @Override
            public Void body() {
               NodeGroupEntity group = NodeGroupEntity.findNodeGroupEntityByName(
                     cluster, nodeGroupName);
               if (group == null) {
                  logger.error("nodegroup " + nodeGroupName + " of cluster "
                        + clusterName + " does not exist");
                  throw ClusterManagerException.NOGEGROUP_NOT_FOUND_ERROR(nodeGroupName);
               }
               group.setDefineInstanceNum(oldInstanceNum);
               Set<HadoopNodeEntity> toRemove = new HashSet<HadoopNodeEntity>();
               if (group.getHadoopNodes() != null) {
                  for (HadoopNodeEntity node : group.getHadoopNodes()) {
                     // delete extra nodes in db
                     if (ClusterCmdUtil.getIndexFromNodeName(node.getVmName()) >= oldInstanceNum) {
                        logger.info("delete obsolete nodes " + node.getVmName());
                        toRemove.add(node);
                        DAL.delete(node);
                     }
                  }
                  group.getHadoopNodes().removeAll(toRemove);
               }

               return null;
            }
         });

         throw ex;
      }
   }

   private void validateIfOnlyTaskTracker(NodeGroupEntity group) {
      List<String> roles = group.getRoleNameList();
      AuAssert.check(!roles.isEmpty(), "roles should not be empty");
      roles.remove(HadoopRole.HADOOP_TASKTRACKER.toString());
      if (!roles.isEmpty()) {
         logger.info("can not start node group with role: " + roles);
         throw ClusterManagerException.ROLES_NOT_SUPPORTED(roles);
      }
   }

   /*
    * Validate the limit.
    * Submit a vhm MQ process task.   
    */
   @SuppressWarnings("unchecked")
   public Long limitCluster(final String clusterName,
         final String nodeGroupName, final int activeComputeNodeNum) throws Exception {
      logger.info("Limit active compute node number to" + activeComputeNodeNum);
      ClusterRead cluster = getClusterByName(clusterName, false);
      // cluster must be contain node group
      List<NodeGroupRead> nodeGroups = cluster.getNodeGroups();
      if (nodeGroups == null || nodeGroups.isEmpty()) {
         String msg = "There is no node group in cluster " + clusterName + " . ";
         logger.error(msg);
         throw BddException.INTERNAL(null, msg);
      }
      // cluster must be runing status
      if (!ClusterStatus.RUNNING.equals(cluster.getStatus())) {
         String msg = "Cluster is not running.";
         logger.error(msg);
         throw ClusterManagerException.LIMIT_CLUSTER_NOT_ALLOWED_ERROR(clusterName, msg);
      }
      // node group must be compute only node
      List<String> nodeGroupNames = new ArrayList<String> ();
      if (!cluster.validateLimit(nodeGroupName, nodeGroupNames)) {
         return null;
      }
      // find hadoop job tracker ip
      String hadoopJobTrackerIP="";
      for (NodeGroupRead nodeGroup : nodeGroups) {
         if (nodeGroup.getRoles() != null && nodeGroup.getRoles().contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
            AuAssert.check(nodeGroup.getInstanceNum() == 1, "The Jobtracker only support one instance .");
            hadoopJobTrackerIP = nodeGroup.getInstances().get(0).getIp();
            AuAssert.check(!CommonUtil.isBlank(hadoopJobTrackerIP),"Hadoop jobtracker cannot be null");
            break;
         }
      }
      // submit a MQ task
      TaskListener listener = new VHMReceiveListener(clusterName);
      TaskEntity task = taskManager.createMessageTask(false, listener);
      ClusterEntity.updateStatus(cluster.getName(), ClusterStatus.VHM_RUNNING);
      DAL.inTransactionUpdate(task);
      Map<String,Object> sendParam = new HashMap <String,Object> ();
      sendParam.put(Constants.CLUSTER_LIMIT_INFO_VERSION, 1);
      sendParam.put(Constants.CLUSTER_LIMIT_INFO_CLUSTER_NAME, clusterName);
      sendParam.put(Constants.CLUSTER_LIMIT_INFO_JOBTRACKER, hadoopJobTrackerIP);
      sendParam.put(Constants.CLUSTER_LIMIT_INFO_INSTANCE_NUM, activeComputeNodeNum);
      sendParam.put(Constants.CLUSTER_LIMIT_INFO_NODE_GROUPS, nodeGroupNames);
      sendParam.put(Constants.CLUSTER_LIMIT_INFO_SERENGETI_INSTANCE, ConfigInfo.getSerengetiRootFolder());
      taskManager.submit(task, new MessageTaskWorker(sendParam));

      return task.getId();
   }

   static class SystemProperties {
      private static final String RABBITMQ_CHANNEL = "rabbitmq_channel";
      private static final String RABBITMQ_EXCHANGE = "rabbitmq_exchange";
      private static final String RABBITMQ_PASSWORD = "rabbitmq_password";
      private static final String RABBITMQ_USERNAME = "rabbitmq_username";
      private static final String RABBITMQ_PORT = "rabbitmq_port";
      private static final String RABBITMQ_HOST = "rabbitmq_host";
      private static HashMap<String, Object> systemProperties;
      static {
         systemProperties = new HashMap<String, Object>();
         systemProperties.put(RABBITMQ_HOST, ConfigInfo.getMqServerHost());
         systemProperties.put(RABBITMQ_PORT, ConfigInfo.getMqServerPort());
         systemProperties.put(RABBITMQ_USERNAME,
               ConfigInfo.getMqServerUsername());
         systemProperties.put(RABBITMQ_PASSWORD,
               ConfigInfo.getMqServerPassword());
         systemProperties
               .put(RABBITMQ_EXCHANGE, ConfigInfo.getMqExchangeName());
      }

      public static HashMap<String, Object> getManifest() {
         HashMap<String, Object> result = new HashMap<String, Object>();
         result.putAll(systemProperties);
         return result;
      }

      public static void setChannelId(HashMap<String, Object> properties,
            String channelId) {
         properties.put(RABBITMQ_CHANNEL, channelId);
      }
   }
}
