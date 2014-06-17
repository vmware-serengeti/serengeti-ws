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
package com.vmware.bdd.service.job.software;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.SyncHostsUtils;

public class SoftwareManagementStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(SoftwareManagementStep.class);
   private ClusterManager clusterManager;
   private ManagementOperation managementOperation;
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   private boolean checkVMStatus = false;

   public IExclusiveLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String targetName =
            getJobParameters(chunkContext).getString(
                  JobConstants.TARGET_NAME_JOB_PARAM);
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      if (targetName == null) {
         targetName = clusterName;
      }
      String jobName = chunkContext.getStepContext().getJobName();
      logger.info("target : " + targetName + ", operation: "
            + managementOperation + ", jobname: " + jobName);

      boolean vmPowerOn = false;
      String vmPowerOnStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.IS_VM_POWER_ON);
      if (vmPowerOnStr != null) {
         logger.info("vm original status is power on? "
               + vmPowerOnStr);
         vmPowerOn = Boolean.parseBoolean(vmPowerOnStr);
      }

      if (checkVMStatus && targetName.split("-").length == 3 && !vmPowerOn) {
         return RepeatStatus.FINISHED;
      }

      // Only check host time for configure (config, start, disk fix, scale up)
      // operation and create (resume only) operation
      if (ManagementOperation.CONFIGURE.equals(managementOperation) ||
            JobConstants.RESUME_CLUSTER_JOB_NAME.equals(jobName)) {
         logger.info("Start to check host time.");
         List<NodeEntity> nodes = lockClusterEntityMgr.getClusterEntityMgr().findAllNodes(clusterName);
         Set<String> hostnames = new HashSet<String>();
         for (NodeEntity node : nodes) {
            hostnames.add(node.getHostName());
         }
         ClusterCreate clusterSpec = clusterManager.getClusterSpec(clusterName);

         SyncHostsUtils.SyncHosts(clusterSpec, hostnames);
      }

      StatusUpdater statusUpdater =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));

      String appManager = lockClusterEntityMgr.getClusterEntityMgr().findByName(clusterName).getAppManager();

      ISoftwareManagementTask task = null;

//      if (pluginEntity != null && (pluginEntity.getProvider().equals(SoftwareMgtProvider.CLOUDERA_MANAGER)
//            || pluginEntity.getProvider().equals(SoftwareMgtProvider.AMBARI))) {
      if (!CommonUtil.isBlank(appManager)) {
         ClusterBlueprint clusterBlueprint =
               lockClusterEntityMgr.getClusterEntityMgr().toClusterBluePrint(
                     clusterName);

         task =
               SoftwareManagementTaskFactory.createExternalMgtTask(targetName,
                     managementOperation, clusterBlueprint, appManager,
                     statusUpdater, lockClusterEntityMgr);

         logger.info((new Gson()).toJson(clusterBlueprint));
      } else {

         // get command work directory
         File workDir = CommandUtil.createWorkDir(getJobExecutionId(chunkContext));

         // update work directory in job context
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CURRENT_COMMAND_WORK_DIR, workDir.getAbsolutePath());

         boolean needAllocIp = true;
         if (ManagementOperation.DESTROY.equals(managementOperation)) {
            needAllocIp = false;
         }
         String specFilePath = null;

         if (managementOperation.ordinal() != ManagementOperation.DESTROY
               .ordinal()) {
            // write cluster spec file
            File specFile =
                  clusterManager.writeClusterSpecFile(targetName, workDir,
                        needAllocIp);
            specFilePath = specFile.getAbsolutePath();
         }

         task = SoftwareManagementTaskFactory.createIronfanTask(targetName,
            specFilePath, statusUpdater, managementOperation,
            lockClusterEntityMgr);
      }

      Map<String, Object> ret = task.call();

      if (!(Boolean) ret.get("succeed")) {
         String errorMessage = (String) ret.get("errorMessage");
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CURRENT_ERROR_MESSAGE, errorMessage);
         throw TaskException.EXECUTION_FAILED(errorMessage);
      }

      return RepeatStatus.FINISHED;
   }

   public ClusterManager getClusterManager() {
      return clusterManager;
   }

   public void setClusterManager(ClusterManager clusterManager) {
      this.clusterManager = clusterManager;
   }

   /**
    * @return the managementOperation
    */
   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   /**
    * @param managementOperation
    *           the managementOperation to set
    */
   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }

   /**
    * @return the checkVMStatus
    */
   public boolean isCheckVMStatus() {
      return checkVMStatus;
   }

   /**
    * @param checkVMStatus
    *           the checkVMStatus to set
    */
   public void setCheckVMStatus(boolean checkVMStatus) {
      this.checkVMStatus = checkVMStatus;
   }

}
