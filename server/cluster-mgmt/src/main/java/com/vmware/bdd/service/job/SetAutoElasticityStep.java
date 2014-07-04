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

import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.IExecutionService;

public class SetAutoElasticityStep extends TrackableTasklet {
   IClusteringService clusteringService;
   IExecutionService executionService;
   boolean refreshAllNodes;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, 
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);

      boolean success = true;
      if (clusteringService.isSupportVHM(clusterName)) {
         success = clusteringService.setAutoElasticity(clusterName, refreshAllNodes);         
      }
      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_EXISTING_NODES_JOB_PARAM, success);
      if (!success) {
         throw TaskException.EXECUTION_FAILED("failed to enable auto elasticity for cluster " + clusterName);
      }
      return RepeatStatus.FINISHED;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public IExecutionService getExecutionService() {
      return executionService;
   }

   public void setExecutionService(IExecutionService executionService) {
      this.executionService = executionService;
   }

   public void setRefreshAllNodes(boolean refreshAllNodes) {
      this.refreshAllNodes = refreshAllNodes;
   }

   public boolean getRefreshAllNodes() {
      return refreshAllNodes;
   }
}
