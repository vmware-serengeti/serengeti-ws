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
package com.vmware.bdd.service.job.heal;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.service.IClusterHealService;
import com.vmware.bdd.service.job.ClusterUpdateDataStep;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class NodeUpdateDataStep extends TrackableTasklet {

   private static final Logger logger = Logger
         .getLogger(ClusterUpdateDataStep.class);

   @Autowired
   private IClusterHealService healService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String targetNode =
            getJobParameters(chunkContext).getString(
                  JobConstants.SUB_JOB_NODE_NAME);
      String vmId =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.REPLACE_VM_ID, String.class);

      try {
         logger.debug("start update disk info for node " + targetNode);
         healService.updateDiskData(vmId, targetNode);
         
         logger.debug("verify ip address for node " + targetNode);
         healService.verifyNodeStatus(targetNode);         
      } catch (Exception e) {
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CURRENT_ERROR_MESSAGE, e.getMessage());
         throw e;
      }
      
      jobExecutionStatusHolder.setCurrentStepProgress(
            getJobExecutionId(chunkContext), 1.0);
      return RepeatStatus.FINISHED;
   }
}
