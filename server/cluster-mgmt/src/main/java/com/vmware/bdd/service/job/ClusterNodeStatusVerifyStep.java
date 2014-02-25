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
package com.vmware.bdd.service.job;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.service.job.software.ManagementOperation;

public class ClusterNodeStatusVerifyStep extends TrackableTasklet {
   private ManagementOperation managementOperation;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      Boolean deleted =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_DELETE_VM_OPERATION_SUCCESS,
                  Boolean.class);
      Boolean created =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_CREATE_VM_OPERATION_SUCCESS,
                  Boolean.class);
      if (deleted != null && !deleted) {
         logger.error("Failed to delete nodes violating placement policy.");
         throw ClusteringServiceException.DELETE_CLUSTER_VM_FAILED(clusterName);
      }
      if (created != null && !created) {
         // vm creation is finished, and with error happens, throw exception here to stop following steps
         throw ClusteringServiceException.VM_CREATION_FAILED(clusterName);
      }
      Boolean verifyStatus =
         getFromJobExecutionContext(chunkContext,
               JobConstants.VERIFY_NODE_STATUS_RESULT_PARAM,
               Boolean.class);
      if (created != null && (verifyStatus == null || !verifyStatus)) {
         // throw creation exception here, and query detail node error message from node entity
         throw ClusteringServiceException.VM_CREATION_FAILED(clusterName);
      }
      if (managementOperation != null) {
         Boolean success =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);
         if ((success != null && !success)
               || (verifyStatus == null || !verifyStatus)) {
            // throw creation exception here, and query detail node error message from node entity
            throw ClusteringServiceException.CLUSTER_OPERATION_FAILED(clusterName);
         }
      }
      return RepeatStatus.FINISHED;
   }

   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }
}
