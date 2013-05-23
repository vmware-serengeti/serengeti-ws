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

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.exception.ScaleServiceException;
import com.vmware.bdd.service.IScaleService;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */

public class ScaleSingleVMStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(ScaleSingleVMStep.class);

   private IScaleService scaleService;
   private boolean rollback;

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
      String cpuNumberStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.NODE_SCALE_CPU_NUMBER);
      String memorySizeStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.NODE_SCALE_MEMORY_SIZE);
      int cpuNumber = Integer.parseInt(cpuNumberStr);
      long memory = Long.parseLong(memorySizeStr);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.NODE_SCALE_ROLLBACK, false);
      if (rollback) {
         logger.info("rollback vm configuration to original");
         cpuNumber = scaleService.getVmOriginalCpuNumber(nodeName);
         memory = scaleService.getVmOriginalMemory(nodeName);
         putIntoJobExecutionContext(chunkContext,
               JobConstants.NODE_SCALE_ROLLBACK, true);
         chunkContext.getStepContext().getStepExecution().getJobExecution()
               .getExecutionContext()
               .putString(JobConstants.SUB_JOB_FAIL_FLAG, "true");
         chunkContext
               .getStepContext()
               .getStepExecution()
               .getJobExecution()
               .getExecutionContext()
               .putString(JobConstants.CURRENT_ERROR_MESSAGE,
                     "node scale was rollbacked");
      }
      boolean success =
            scaleService.scaleNodeResource(nodeName, cpuNumber, memory);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_OPERATION_SUCCESS, success);
      if (!success) {
         throw ScaleServiceException.COMMON_SCALE_ERROR(nodeName);
      }
      return RepeatStatus.FINISHED;
   }

   /**
    * @return the scaleService
    */
   public IScaleService getScaleService() {
      return scaleService;
   }

   /**
    * @param scaleService
    *           the scaleService to set
    */
   public void setScaleService(IScaleService scaleService) {
      this.scaleService = scaleService;
   }

   /**
    * @return the rollback
    */
   public boolean isRollback() {
      return rollback;
   }

   /**
    * @param rollback
    *           the rollback to set
    */
   public void setRollback(boolean rollback) {
      this.rollback = rollback;
   }

}
