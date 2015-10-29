/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.utils.JobUtils;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.ISetPasswordService;

public class PostSetPasswordStep extends TrackableTasklet {
   ISetPasswordService setPasswordService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      boolean setPasswordSucceed = getFromJobExecutionContext(chunkContext, JobConstants.SET_PASSWORD_SUCCEED_JOB_PARAM, Boolean.class);
      if (!setPasswordSucceed) {
         String errMsg = "Failed to set password for cluster " + clusterName;
         JobUtils.recordErrorInClusterOperation(chunkContext, errMsg);
         if (!JobUtils.getJobParameterForceClusterOperation(chunkContext)) {
            throw TaskException.EXECUTION_FAILED(errMsg);
         }
      }
      return RepeatStatus.FINISHED;
   }

   public ISetPasswordService getSetPasswordService() {
      return setPasswordService;
   }

   public void setSetPasswordService(ISetPasswordService setPasswordService) {
      this.setPasswordService = setPasswordService;
   }
}
