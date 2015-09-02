/******************************************************************************
 *   Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.job;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.IClusterRequiredServicesRestartService;
import com.vmware.bdd.service.job.software.external.ExternalProgressMonitor;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

public class ClusterRequiredServicesRestartStep extends TrackableTasklet  {

   private IClusterRequiredServicesRestartService clusterRequiredServicesRestartService;
   private IExclusiveLockedClusterEntityManager lockedEntityManager;
   private IClusterEntityManager clusterEntityManager;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);

      ClusterBlueprint blueprint = clusterEntityManager.toClusterBluePrint(clusterName);

      String targetName = getFromJobExecutionContext(chunkContext, JobConstants.TARGET_NAME_JOB_PARAM, String.class);
      ClusterReportQueue queue = new ClusterReportQueue();
      StatusUpdater statusUpdater = new DefaultStatusUpdater(jobExecutionStatusHolder, getJobExecutionId(chunkContext));
      ExternalProgressMonitor monitor = new ExternalProgressMonitor(targetName, queue, statusUpdater, lockedEntityManager);
      Thread progressThread = new Thread(monitor, "ProgressMonitor-" + targetName);
      progressThread.setDaemon(true);
      progressThread.start();

      clusterRequiredServicesRestartService.restart(blueprint, queue);

      return RepeatStatus.FINISHED;
   }

   public IClusterRequiredServicesRestartService getClusterRequiredServicesRestartService() {
      return clusterRequiredServicesRestartService;
   }

   public void setClusterRequiredServicesRestartService(
         IClusterRequiredServicesRestartService clusterRequiredServicesRestartService) {
      this.clusterRequiredServicesRestartService =
            clusterRequiredServicesRestartService;
   }

   public IExclusiveLockedClusterEntityManager getLockedEntityManager() {
      return lockedEntityManager;
   }

   public void setLockedEntityManager(
         IExclusiveLockedClusterEntityManager lockedEntityManager) {
      this.lockedEntityManager = lockedEntityManager;
   }

   public IClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }

}
