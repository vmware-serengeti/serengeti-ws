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
package com.vmware.bdd.service.job.vm;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.service.IScaleService;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.utils.JobUtils;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */

public class SingleNodeSyncupStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(SingleNodeSyncupStep.class);
   private IScaleService scaleService;

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
      String nodeName =
            getJobParameters(chunkContext).getString(
                  JobConstants.SUB_JOB_NODE_NAME);
      NodeEntity node = getClusterEntityMgr().findNodeByName(nodeName);
      getClusterEntityMgr().syncUpNode(clusterName, nodeName);
      Boolean success =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.NODE_OPERATION_SUCCESS, Boolean.class);
      if (success != null && !success) {
         throw ClusteringServiceException.CLUSTER_OPERATION_FAILED(clusterName);
      }
      NodeStatus expectedStatus =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.EXPECTED_NODE_STATUS, NodeStatus.class);
      if (expectedStatus != null) {
         logger.info("the node should be in status " + expectedStatus);
         JobUtils.verifyNodeStatus(node, expectedStatus, true);
      }
      String cpuNumberStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.NODE_SCALE_CPU_NUMBER);
      String memorySizeStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.NODE_SCALE_MEMORY_SIZE);
      int cpuNumber = 0;
      if (cpuNumberStr != null) {
         cpuNumber = Integer.parseInt(cpuNumberStr);
      }
      boolean rollback =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.NODE_SCALE_ROLLBACK, Boolean.class);
      long memory = 0;
      if (memorySizeStr != null) {
         memory = Long.parseLong(memorySizeStr);
      }
      if (cpuNumber > 0 && !rollback) {
         node.setCpuNum(cpuNumber);
      }
      if (memory > 0) {
         if (!rollback) {
            node.setMemorySize(memory);
         }
         scaleService.updateSwapDisk(nodeName);
      }
      getClusterEntityMgr().update(node);

      return RepeatStatus.FINISHED;
   }

   /**
    * @return the scaleService
    */
   public IScaleService getScaleService() {
      return scaleService;
   }

   /**
    * @param scaleService
    *           the scaleService to set
    */
   public void setScaleService(IScaleService scaleService) {
      this.scaleService = scaleService;
   }
}
