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
package com.vmware.bdd.service.job.vm;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */

public class StopSingleVMStep extends TrackableTasklet {
   private IClusteringService clusteringService;
   private boolean vmPoweroff = false;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      if (clusterName == null) {
         clusterName =
               getJobParameters(chunkContext).getString(
                     JobConstants.TARGET_NAME_JOB_PARAM).split("-")[0];
      }
      String nodeName =
            getJobParameters(chunkContext).getString(
                  JobConstants.SUB_JOB_NODE_NAME);
      StatusUpdater statusUpdator =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));
      boolean success =
            clusteringService.stopSingleVM(clusterName, nodeName,
                  statusUpdator, vmPoweroff);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.NODE_OPERATION_SUCCESS, success);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.EXPECTED_NODE_STATUS, NodeStatus.POWERED_OFF);
      return RepeatStatus.FINISHED;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   /**
    * @return the vmPoweroff
    */
   public boolean isVmPoweroff() {
      return vmPoweroff;
   }

   /**
    * @param vmPoweroff
    *           the vmPoweroff to set
    */
   public void setVmPoweroff(boolean vmPoweroff) {
      this.vmPoweroff = vmPoweroff;
   }


}
