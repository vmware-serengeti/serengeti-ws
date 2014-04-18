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

import java.util.ArrayList;
import java.util.List;

import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.service.job.software.ManagementOperation;

public class SetPasswordForNewNodesStep extends TrackableTasklet {
   private ISetPasswordService setPasswordService;
   private ClusterConfigManager configMgr;
   private ManagementOperation managementOperation;
   private String clusterName;
   private static final Logger logger = Logger.getLogger(SetPasswordForNewNodesStep.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) {
      clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);
      String newPassword = clusterSpec.getPassword();

      List<NodeEntity> nodes = getNodesToBeSetPassword(chunkContext);

      if (nodes == null || nodes.isEmpty()) {
         throw TaskException.EXECUTION_FAILED("No nodes needed to set password for");
      }

      ArrayList<String> failedNodes = setPasswordService.setPasswordForNodes(clusterName, nodes, newPassword);
      boolean success = false;
      if (failedNodes == null) {
         success = true;
      } else {
         logger.info("Failed to set password for " + failedNodes.toString());
      }

      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_EXISTING_NODES_JOB_PARAM, success);
      if (!success) {
         throw TaskException.EXECUTION_FAILED("Failed to set password for nodes " + failedNodes.toString());
      }

      return RepeatStatus.FINISHED;
   }

   private List<NodeEntity> getNodesToBeSetPassword (ChunkContext chunkContext) throws TaskException {
      List<NodeEntity> toBeSetPassword = null;
      if ((managementOperation == ManagementOperation.CREATE) || 
            (managementOperation == ManagementOperation.RESUME)) {
         toBeSetPassword = getClusterEntityMgr().findAllNodes(clusterName);
         return toBeSetPassword;
      } else if (managementOperation == ManagementOperation.RESIZE) {
         String groupName = getJobParameters(chunkContext).getString(JobConstants.GROUP_NAME_JOB_PARAM);
         List<NodeEntity> nodesInGroup = clusterEntityMgr.findAllNodes(clusterName, groupName);
         long oldInstanceNum = getJobParameters(chunkContext).getLong(
               JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);

         for (NodeEntity node : nodesInGroup) {
            long index = CommonUtil.getVmIndex(node.getVmName());
            if (index < oldInstanceNum) {
               // do not verify existing nodes from last successful deployment
               continue;
            }
            if (node.getStatus() == NodeStatus.VM_READY) {
               if (toBeSetPassword == null) {
                  toBeSetPassword = new ArrayList<NodeEntity>();
               }
               toBeSetPassword.add(node);
            }
         }
         return toBeSetPassword;
      } else {
         throw TaskException.EXECUTION_FAILED("Unknown operation type.");
      }
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }

   public ISetPasswordService getSetPasswordService() {
      return setPasswordService;
   }

   public void setSetPasswordService(ISetPasswordService setPasswordService) {
      this.setPasswordService = setPasswordService;
   }

   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }
}