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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.utils.JobUtils;

public class ClusterNodeSyncupStep extends TrackableTasklet {
   private IConcurrentLockedClusterEntityManager lockClusterEntityMgr;

   public IConcurrentLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IConcurrentLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

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
      lockClusterEntityMgr.syncUp(clusterName, false);

      setNodeErrorMessages(chunkContext);
      Boolean success =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);
      if (success != null && !success) {
         // wait next step to throw exception
         return RepeatStatus.FINISHED;
      }
      verifyNodeStatus(chunkContext, clusterName);
      return RepeatStatus.FINISHED;
   }

   @Transactional
   private void verifyNodeStatus(ChunkContext chunkContext, String clusterName) {
      Boolean success;
      NodeStatus expectedStatus =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.EXPECTED_NODE_STATUS, NodeStatus.class);
      if (expectedStatus != null) {
         logger.info("all node should be in status " + expectedStatus);
         List<NodeEntity> nodes =
               getClusterEntityMgr().findAllNodes(clusterName);
         success = JobUtils.verifyNodesStatus(nodes, expectedStatus, true, getClusterEntityMgr());
         putIntoJobExecutionContext(chunkContext,
               JobConstants.VERIFY_NODE_STATUS_RESULT_PARAM, success);
      }
   }

   @Transactional
   private void setNodeErrorMessages(ChunkContext chunkContext) {
      List<NodeOperationStatus> nodesStatus =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_NODES_STATUS,
                  new TypeToken<List<NodeOperationStatus>>() {
                  }.getType());
      if (nodesStatus != null) {
         for (NodeOperationStatus node : nodesStatus) {
            NodeEntity entity =
                  getClusterEntityMgr().findNodeByName(node.getNodeName());
            entity.setActionFailed(!node.isSucceed());
            entity.setErrMessage(node.getErrorMessage());
         }
      }
   }
}
