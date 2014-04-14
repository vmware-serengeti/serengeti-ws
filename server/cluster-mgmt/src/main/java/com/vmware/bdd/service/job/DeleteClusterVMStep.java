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

import java.util.List;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.utils.JobUtils;

public class DeleteClusterVMStep extends TrackableTasklet {
   IClusteringService clusteringService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);

      StatusUpdater statusUpdator =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));
      logger.info("Start to delete cluster: " + clusterName);
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);
      List<BaseNode> vNodes = JobUtils.convertNodeEntities(null, null, nodes);
      boolean success =
            clusteringService.deleteCluster(clusterName, vNodes, statusUpdator);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_DELETE_VM_OPERATION_SUCCESS, success);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_DELETED_NODES_JOB_PARAM, vNodes);
      return RepeatStatus.FINISHED;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

}
