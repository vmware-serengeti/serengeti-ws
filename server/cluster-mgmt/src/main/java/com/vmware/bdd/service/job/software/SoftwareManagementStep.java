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

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.utils.JobUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.ISoftwareSyncUpService;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.SyncHostsUtils;

public class SoftwareManagementStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(SoftwareManagementStep.class);
   private ClusterManager clusterManager;
   private ManagementOperation managementOperation;
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   private SoftwareManagerCollector softwareMgrs;
   private boolean checkVMStatus = false;
   private ISoftwareSyncUpService serviceSyncup;

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

   public ISoftwareSyncUpService getServiceSyncup() {
      return serviceSyncup;
   }

   @Autowired
   public void setServiceSyncup(ISoftwareSyncUpService serviceSyncup) {
      this.serviceSyncup = serviceSyncup;
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

      serviceSyncup.syncUp(clusterName);
      logger.debug("Try to sync up service status for cluster " + clusterName);

      boolean vmPowerOn = false;
      String vmPowerOnStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.IS_VM_POWER_ON);
      if (vmPowerOnStr != null) {
         logger.info("vm original status is power on? " + vmPowerOnStr);
         vmPowerOn = Boolean.parseBoolean(vmPowerOnStr);
      }

      if (checkVMStatus && targetName.split("-").length == 3 && !vmPowerOn) {
         return RepeatStatus.FINISHED;
      }

      // Only check host time for cluster config, disk fix, scale up (management
      // operation configure), start (management operation start) and create
      // (resume only)
      SoftwareManager softwareMgr = null;
      try {
         softwareMgr = softwareMgrs.getSoftwareManagerByClusterName(clusterName);
      } catch (SoftwareManagerCollectorException e) {
         if (ManagementOperation.PRE_DESTROY.equals(managementOperation) ||
               ManagementOperation.DESTROY.equals(managementOperation)) {
            return RepeatStatus.FINISHED;
         }
         throw e;
      }
      if (ManagementOperation.CONFIGURE.equals(managementOperation)
            || ManagementOperation.START.equals(managementOperation)
            || JobConstants.RESUME_CLUSTER_JOB_NAME.equals(jobName)) {
         logger.info("Start to check host time.");
         List<NodeEntity> nodes =
               lockClusterEntityMgr.getClusterEntityMgr().findAllNodes(
                     clusterName);
         Set<String> hostnames = new HashSet<String>();
         for (NodeEntity node : nodes) {
            //for software operation, we can only handle VMs who are already VM_READY
            //Add this filter to tolerate some vm failures in cluster start
            boolean force = JobUtils.getJobParameterForceClusterOperation(chunkContext);
            if (force && (node.getStatus() != NodeStatus.VM_READY)) {
               continue;
            }
            hostnames.add(node.getHostName());
         }
         ClusterCreate clusterSpec = clusterManager.getClusterSpec(clusterName);
         SyncHostsUtils.SyncHosts(clusterSpec, hostnames, softwareMgr);
      }

      StatusUpdater statusUpdater =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));

      ISoftwareManagementTask task = null;
      String appMgrName = softwareMgr.getName();
      validateUserExistense();
      if (!Constants.IRONFAN.equals(appMgrName)) {
         task =
               createExternalTask(chunkContext, targetName, clusterName,
                     statusUpdater);
      } else {
         task = createThriftTask(chunkContext, targetName, statusUpdater);
      }

      if (task != null) {
         Map<String, Object> ret = task.call();

         if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.CURRENT_ERROR_MESSAGE, errorMessage);
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.SOFTWARE_MANAGEMENT_STEP_FAILE, true);
            throw TaskException.EXECUTION_FAILED(errorMessage);
         }
      }

      return RepeatStatus.FINISHED;
   }

   //validate whether all services users configured in spec file exist in node
   // throws exception if some users don't exist
   private void validateUserExistense() throws BddException {
      //TODO(qjin): implement
   }

   private ISoftwareManagementTask createThriftTask(ChunkContext chunkContext,
         String targetName, StatusUpdater statusUpdater) {
      ISoftwareManagementTask task;
      // get command work directory
      File workDir = CommandUtil.createWorkDir(getJobExecutionId(chunkContext));

      // update work directory in job context
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CURRENT_COMMAND_WORK_DIR, workDir.getAbsolutePath());

      boolean needAllocIp = true;
      if (ManagementOperation.DESTROY.equals(managementOperation)
            || ManagementOperation.PRE_DESTROY.equals(managementOperation)) {
         needAllocIp = false;
      }
      String specFilePath = null;

      if (!(ManagementOperation.DESTROY.equals(managementOperation) || ManagementOperation.PRE_DESTROY
            .equals(managementOperation))) {
         // write cluster spec file
         File specFile =
               clusterManager.writeClusterSpecFile(targetName, workDir,
                     needAllocIp);
         specFilePath = specFile.getAbsolutePath();
      }

      task =
            SoftwareManagementTaskFactory.createThriftTask(targetName,
                  specFilePath, statusUpdater, managementOperation,
                  lockClusterEntityMgr);
      return task;
   }

   private ISoftwareManagementTask createExternalTask(
         ChunkContext chunkContext, String targetName, String clusterName,
         StatusUpdater statusUpdater) {
      ISoftwareManagementTask task;
      SoftwareManager softwareMgr =
            softwareMgrs.getSoftwareManagerByClusterName(clusterName);
      ClusterBlueprint clusterBlueprint =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_BLUEPRINT_JOB_PARAM,
                  ClusterBlueprint.class);
      if (clusterBlueprint == null) {
         clusterBlueprint =
               lockClusterEntityMgr.getClusterEntityMgr().toClusterBluePrint(
                     clusterName);
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CLUSTER_BLUEPRINT_JOB_PARAM, clusterBlueprint);
      }

      task =
            SoftwareManagementTaskFactory.createExternalMgtTask(targetName,
                  managementOperation, clusterBlueprint, statusUpdater,
                  lockClusterEntityMgr, softwareMgr, chunkContext);
      return task;
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
