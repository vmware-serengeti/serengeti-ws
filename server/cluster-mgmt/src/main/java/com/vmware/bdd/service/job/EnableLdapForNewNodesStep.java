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

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.service.impl.ClusterLdapUserMgmtCfgService;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.utils.CommonUtil;

public class EnableLdapForNewNodesStep extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(EnableLdapForNewNodesStep.class);

   private ClusterLdapUserMgmtCfgService clusterLdapUserMgmtCfgService;
   private ManagementOperation managementOperation;
   private String clusterName;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) {
      clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);

      List<NodeEntity> nodes = findNodesToEnableLdap(chunkContext);

      clusterLdapUserMgmtCfgService.configureUserMgmt(clusterName, nodes);

      putIntoJobExecutionContext(chunkContext, "Enable LDAP successfully", true);

      return RepeatStatus.FINISHED;
   }

   protected List<NodeEntity> findNodesToEnableLdap(ChunkContext chunkContext) throws TaskException {
      List<NodeEntity> foundNodeList = null;
      if ((managementOperation == ManagementOperation.CREATE) ||
            (managementOperation == ManagementOperation.RESUME)) {
         foundNodeList = getClusterEntityMgr().findAllNodes(clusterName);
         return foundNodeList;
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
            if (node.getStatus().ordinal() >= NodeStatus.VM_READY.ordinal()) {
               if (foundNodeList == null) {
                  foundNodeList = new ArrayList<>();
               }
               foundNodeList.add(node);
            }
         }
         return foundNodeList;
      } else {
         throw TaskException.EXECUTION_FAILED("Unknown operation type.");
      }
   }

   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }

   public void setClusterLdapUserMgmtCfgService(ClusterLdapUserMgmtCfgService clusterLdapUserMgmtCfgService) {
      this.clusterLdapUserMgmtCfgService = clusterLdapUserMgmtCfgService;
   }
}