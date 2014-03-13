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
package com.vmware.bdd.service.job.software;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.Constants;

public class SoftwareManagementStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(SoftwareManagementStep.class);
   private ClusterManager clusterManager;
   private ManagementOperation managementOperation;
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;

   public IExclusiveLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }


   public ISoftwareManagementTask createCommandTask(String clusterName,
         String specFileName, StatusUpdater statusUpdater) {
      return SoftwareManagementTaskFactory.createCommandTask(clusterName,
            specFileName, statusUpdater, managementOperation,
            lockClusterEntityMgr);
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String targetName =
            getJobParameters(chunkContext).getString(
                  JobConstants.TARGET_NAME_JOB_PARAM);
      if (targetName == null) {
         targetName =
               getJobParameters(chunkContext).getString(
                     JobConstants.CLUSTER_NAME_JOB_PARAM);
      }
      String jobName = chunkContext.getStepContext().getJobName();
      logger.info("target : " + targetName + ", operation: "
            + managementOperation + ", jobname: " + jobName);

      // Only check host time for create (create, resize, resume)
      // and configure (config, start, disk fix, scale up) operation
      if (ManagementOperation.CREATE.equals(managementOperation) ||
            ManagementOperation.CONFIGURE.equals(managementOperation)) {
         List<NodeEntity> nodes = lockClusterEntityMgr.getClusterEntityMgr().findAllNodes(targetName);
         Set<String> hostnames = new HashSet<String>();
         for (NodeEntity node : nodes) {
            hostnames.add(node.getHostName());
         }
         ClusterCreate clusterSpec = clusterManager.getClusterSpec(targetName);

         int maxTimeDiffInSec = Constants.MAX_TIME_DIFF_IN_SEC;
         if (clusterSpec.checkHBase())
            maxTimeDiffInSec = Constants.MAX_TIME_DIFF_IN_SEC_HBASE;
         List<String> outOfSyncHosts = new ArrayList<String>();
         for (String hostname : hostnames) {
            int hostTimeDiffInSec =
                  VcResourceUtils.getHostTimeDiffInSec(hostname);
            if (Math.abs(hostTimeDiffInSec) > maxTimeDiffInSec) {
               logger.info("Host " + hostname + " has a time difference of "
                     + hostTimeDiffInSec
                     + " seconds and is dropped from placement.");
               outOfSyncHosts.add(hostname);
            }
         }
         if (!outOfSyncHosts.isEmpty()) {
            logger.error("Time on host " + outOfSyncHosts
                  + "is out of sync which will lead to failure, "
                  + "synchronize the time on these hosts with "
                  + "Serengeti management server and try again.");
            throw TaskException.HOST_TIME_OUT_OF_SYNC(outOfSyncHosts);
         }
      }

      StatusUpdater statusUpdater =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));

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
      ISoftwareManagementTask task =
            createCommandTask(targetName, specFilePath, statusUpdater);

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

}
