/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.service.job.vm;

import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.manager.ShrinkException;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.service.job.software.external.ExternalProgressMonitor;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import org.apache.log4j.Logger;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by qjin on 11/15/14.
 */
public class DecommissionSingleVMStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(DecommissionSingleVMStep.class);
   private IClusterEntityManager clusterEntityManager;
   private SoftwareManagerCollector softwareManagerCollector;
   private JobManager jobManager;
   private IExclusiveLockedClusterEntityManager lockedEntityManager;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      String nodeGroupName = getJobParameters(chunkContext).getString(JobConstants.GROUP_NAME_JOB_PARAM);
      String nodeName = getJobParameters(chunkContext).getString(JobConstants.SUB_JOB_NODE_NAME);
      SoftwareManager softwareManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
      NodeInfo nodeInfo = new NodeInfo();
      nodeInfo.setName(nodeName);
      try {
         logger.info("decomissioning " + nodeName);
         putIntoJobExecutionContext(chunkContext, JobConstants.NODE_OPERATION_SUCCESS, false);

         ClusterBlueprint blueprint = clusterEntityManager.toClusterBluePrint(clusterName);
         String targetName = getFromJobExecutionContext(chunkContext, JobConstants.TARGET_NAME_JOB_PARAM, String.class);
         ClusterReportQueue queue = new ClusterReportQueue();
         StatusUpdater statusUpdater = new DefaultStatusUpdater(jobExecutionStatusHolder, getJobExecutionId(chunkContext));
         ExternalProgressMonitor monitor =
              new ExternalProgressMonitor(targetName, queue, statusUpdater,lockedEntityManager);
         Thread progressThread = new Thread(monitor, "ProgressMonitor-" + targetName);
         progressThread.setDaemon(true);
         progressThread.start();

         softwareManager.decommissionNode(blueprint, nodeGroupName, nodeName, queue);
         putIntoJobExecutionContext(chunkContext,JobConstants.NODE_OPERATION_SUCCESS, true);
      } catch (Exception e) {
         logger.error("Got exception when decommissioning " + nodeName + " :", e);
         logger.info("Recommissioning " + nodeName);
         softwareManager.recomissionNode(clusterName, nodeInfo, null);
         //TODO(qjin): can be improved
         JobExecution jobExecution = jobManager.getJobExplorer().getJobExecution(getJobExecutionId(chunkContext));
         jobExecution.setStatus(BatchStatus.FAILED);

         throw ShrinkException.DECOMISSION_FAILED(e, e.getMessage());
      }
      return RepeatStatus.FINISHED;
   }

   public IClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }

   public SoftwareManagerCollector getSoftwareManagerCollector() {
      return softwareManagerCollector;
   }

   public void setSoftwareManagerCollector(SoftwareManagerCollector softwareManagerCollector) {
      this.softwareManagerCollector = softwareManagerCollector;
   }

   public JobManager getJobManager() {
      return jobManager;
   }

   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }


   public IExclusiveLockedClusterEntityManager getLockedEntityManager() {
      return lockedEntityManager;
   }

   @Autowired
   public void setLockedEntityManager(
         IExclusiveLockedClusterEntityManager lockedEntityManager) {
      this.lockedEntityManager = lockedEntityManager;
   }
}
