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

import java.util.List;
import java.util.UUID;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IClusteringService;

public class CreateClusterPlanStep extends TrackableTasklet {
   private IClusteringService clusteringService;
   private ClusterConfigManager configMgr;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);
      UUID reservationId = clusteringService.reserveResource(clusterName);
      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM, reservationId);
      List<BaseNode> vNodes = clusteringService.getPlacementPlan(clusterSpec, null);
      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM, vNodes);
      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_SPEC_JOB_PARAM, clusterSpec);
      return RepeatStatus.FINISHED;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }

}
