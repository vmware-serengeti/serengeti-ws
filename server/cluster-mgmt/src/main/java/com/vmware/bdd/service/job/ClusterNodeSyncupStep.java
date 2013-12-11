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

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.utils.JobUtils;

public class ClusterNodeSyncupStep extends TrackableTasklet {

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
      getClusterEntityMgr().syncUp(clusterName, false);
      Boolean success =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);
      if (success != null && !success) {
         // vm option is finished, and with error happens, throw exception here to stop following steps
         throw ClusteringServiceException.CLUSTER_OPERATION_FAILED(clusterName);
      }
      NodeStatus expectedStatus = getFromJobExecutionContext(chunkContext,
            JobConstants.EXPECTED_NODE_STATUS, NodeStatus.class);
      if (expectedStatus != null) {
         logger.info("all node should be in status " + expectedStatus);
         List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(clusterName);
         JobUtils.verifyNodesStatus(nodes, expectedStatus, true);
      }
      return RepeatStatus.FINISHED;
   }
}
