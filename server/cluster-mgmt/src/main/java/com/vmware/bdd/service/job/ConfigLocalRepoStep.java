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
package com.vmware.bdd.service.job;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.ISetLocalRepoService;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class ConfigLocalRepoStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(ConfigLocalRepoStep.class);
   private ClusterManager clusterManager;
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   private SoftwareManagerCollector softwareMgrs;
   private ISetLocalRepoService setLocalRepoService;
   private ManagementOperation managementOperation;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      // This step is only for app manager like ClouderaMgr and Ambari
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);

      SoftwareManager softwareMgr =
            softwareMgrs.getSoftwareManagerByClusterName(clusterName);

      String appMgrName = softwareMgr.getName();
      if (Constants.IRONFAN.equals(appMgrName)) {
         // we do not config any local repo for Ironfan
         return RepeatStatus.FINISHED;
      }

      ClusterCreate clusterConfig =
            clusterManager.getClusterConfigMgr().getClusterConfig(clusterName);
      String localRepoURL = clusterConfig.getLocalRepoURL();
      logger.info("Use the following URL as the local yum server:"
            + localRepoURL);

      if (!CommonUtil.isBlank(localRepoURL)) {
         // Setup local repo file on each node for ClouderaMgr/Ambari.
         logger.info("ConfigLocalRepoStep: start to setup local repo on each node for ClouderaMgr/Ambari.");

         List<NodeEntity> nodes =
               getNodesToBeSetLocalRepo(chunkContext, clusterName);
         String appMgrRepoID =
               Configuration.getString(
                     Constants.SERENGETI_NODE_YUM_CLOUDERA_MANAGER_REPO_ID,
                     Constants.NODE_APPMANAGER_YUM_CLOUDERA_MANAGER_REPO_ID);
         if (appMgrName.equals(Constants.AMBARI_PLUGIN_TYPE)) {
            appMgrRepoID =
                  Configuration.getString(
                        Constants.SERENGETI_NODE_YUM_AMBARI_REPO_ID,
                        Constants.NODE_APPMANAGER_YUM_AMBARI_REPO_ID);
         }

         setLocalRepoService.setLocalRepoForNodes(clusterName, nodes,
               appMgrRepoID, localRepoURL);

      }

      return RepeatStatus.FINISHED;
   }

   private List<NodeEntity> getNodesToBeSetLocalRepo(ChunkContext chunkContext,
         String clusterName) throws TaskException {
      List<NodeEntity> toBeSetLocalRepo = null;
      if ((managementOperation == ManagementOperation.CREATE)
            || (managementOperation == ManagementOperation.RESUME)) {
         toBeSetLocalRepo = getClusterEntityMgr().findAllNodes(clusterName);
         return toBeSetLocalRepo;
      } else if (managementOperation == ManagementOperation.RESIZE) {
         String groupName =
               getJobParameters(chunkContext).getString(
                     JobConstants.GROUP_NAME_JOB_PARAM);
         List<NodeEntity> nodesInGroup =
               clusterEntityMgr.findAllNodes(clusterName, groupName);
         long oldInstanceNum =
               getJobParameters(chunkContext).getLong(
                     JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);
         toBeSetLocalRepo = new ArrayList<>();
         for (NodeEntity node : nodesInGroup) {
            long index = CommonUtil.getVmIndex(node.getVmName());
            if (index < oldInstanceNum) {
               // do not verify existing nodes from last successful deployment
               continue;
            }
            if (node.getStatus().ordinal() == NodeStatus.VM_READY.ordinal()) {
               toBeSetLocalRepo.add(node);
            }
         }
         return toBeSetLocalRepo;
      } else {
         throw TaskException.EXECUTION_FAILED("Unknown operation type.");
      }
   }

   public ClusterManager getClusterManager() {
      return clusterManager;
   }

   public void setClusterManager(ClusterManager clusterManager) {
      this.clusterManager = clusterManager;
   }


   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }

   public ISetLocalRepoService getSetLocalRepoService() {
      return setLocalRepoService;
   }

   public void setSetLocalRepoService(ISetLocalRepoService setLocalRepoService) {
      this.setLocalRepoService = setLocalRepoService;
   }

   public IExclusiveLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

   public SoftwareManagerCollector getSoftwareMgrs() {
      return softwareMgrs;
   }

   @Autowired
   public void setSoftwareMgrs(SoftwareManagerCollector softwareMgrs) {
      this.softwareMgrs = softwareMgrs;
   }

}
