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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.JobUtils;

public class ClusterNodeStatusVerifyStep extends TrackableTasklet {

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

      if (created != null) {
         // only when node is created, we need to verify node status
         String verifyScope =
            getJobParameters(chunkContext).getString(
                  JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM);
         if (verifyScope != null
               && verifyScope.equals(JobConstants.GROUP_NODE_SCOPE_VALUE)) {
            String groupName =
               getJobParameters(chunkContext).getString(
                     JobConstants.GROUP_NAME_JOB_PARAM);
            long oldInstanceNum =
               getJobParameters(chunkContext).getLong(
                     JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);
            verifyGroupVmReady(clusterName, groupName, oldInstanceNum);
         } else {
            verifyAllVmReady(clusterName);
         }
      }
      return RepeatStatus.FINISHED;
   }

   private void verifyGroupVmReady(String clusterName, String groupName,
         long oldInstanceNum) {
      if (groupName == null) {
         logger.info("No group name specified, ignore node status verification.");
         return;
      }
      List<NodeEntity> nodes =
            getClusterEntityMgr().findAllNodes(clusterName, groupName);
      List<NodeEntity> toBeVerified = new ArrayList<NodeEntity>();
      for (NodeEntity node : nodes) {
         long index = CommonUtil.getVmIndex(node.getVmName());
         if (index < oldInstanceNum) {
            // do not verify existing nodes from last successful deployment
            continue;
         }
         toBeVerified.add(node);
      }
      JobUtils.verifyNodesStatus(toBeVerified, NodeStatus.VM_READY, false);
   }

   private void verifyAllVmReady(String clusterName) {
      List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(clusterName);
      JobUtils.verifyNodesStatus(nodes, NodeStatus.VM_READY, false);
   }

}
