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
package com.vmware.bdd.service.job;

import com.vmware.bdd.utils.JobUtils;
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
      boolean force = JobUtils.getJobParameterForceClusterOperation(chunkContext);
      if (deleted != null && !deleted) {
         JobUtils.recordErrorInClusterOperation(chunkContext, "Failed to delete nodes violating placement policy.");
         if (!force) {
            throw ClusteringServiceException.DELETE_CLUSTER_VM_FAILED(clusterName);
         }
      }
      if (created != null && !created) {
         // vm creation is finished, and with error happens. If forceClusterOperation enabled, ignore the failure here,
         // Otherwise,throw exceptions and stop following steps
         JobUtils.recordErrorInClusterOperation(chunkContext, "Failed to create VMs.");
         if (!force) {
            throw ClusteringServiceException.VM_CREATION_FAILED(clusterName);
         }
      }
      Boolean verifyStatus =
         getFromJobExecutionContext(chunkContext,
               JobConstants.VERIFY_NODE_STATUS_RESULT_PARAM,
               Boolean.class);
      if (created != null && (verifyStatus == null || !verifyStatus)) {
         // throw creation exception here, and query detail node error message from node entity
         JobUtils.recordErrorInClusterOperation(chunkContext, "Failed to verify VM status.");
         if (!force) {
            throw ClusteringServiceException.VM_CREATION_FAILED(clusterName);
         }
      }
      if (managementOperation != null) {
         Boolean success =
               getFromJobExecutionContext(chunkContext,
                     JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);
         if ((success != null && !success)
               || (verifyStatus == null || !verifyStatus)) {
            // throw creation exception here, and query detail node error message from node entity
            //if is start custer, force to start the cluster even met failures
            String errMsg = "Failed to " + managementOperation + " cluster " + clusterName;
            JobUtils.recordErrorInClusterOperation(chunkContext, errMsg);
            if (!force) {
               throw ClusteringServiceException.CLUSTER_OPERATION_FAILED(clusterName);
            }
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
