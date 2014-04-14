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
import java.util.Map;

import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.IpConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.service.job.software.ManagementOperation;

public class SetPasswordForNewNodesStep extends TrackableTasklet {
   private ISetPasswordService setPasswordService;
   private ClusterConfigManager configMgr;
   private ManagementOperation managementOperation;
   private static final Logger logger = Logger.getLogger(SetPasswordForNewNodesStep.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);

      String newPassword = clusterSpec.getPassword();

      ArrayList<String> nodeIPs = null;
      if (managementOperation == ManagementOperation.CREATE || managementOperation == ManagementOperation.RESIZE) {
         List<BaseNode> addedNodes =
               getFromJobExecutionContext(chunkContext, JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM,
                     new TypeToken<List<BaseNode>>() {
                     }.getType());
         nodeIPs = getAddedNodeIPs(addedNodes);
      } else if (managementOperation == ManagementOperation.RESUME) {
         nodeIPs = getAllNodeIPsFromEntitys(getClusterEntityMgr().findAllNodes(clusterName));
      } else {
         throw TaskException.EXECUTION_FAILED("Unknown operation type.");
      }

      if (nodeIPs == null) {
         throw TaskException.EXECUTION_FAILED("No nodes needed to set password for.");
      }

      ArrayList<String> failedNodes = setPasswordService.setPasswordForNodes(clusterName, nodeIPs, newPassword);
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

   private ArrayList<String> getAllNodeIPsFromEntitys(List<NodeEntity> nodes) {
      if (nodes == null) {
         return null;
      }

      ArrayList<String> nodeIPs = null;
      for (NodeEntity node : nodes) {
         String ip = node.getPrimaryMgtIpV4();
         if (ip != null) {
            if (nodeIPs == null) {
               nodeIPs = new ArrayList<String>();
            }
            nodeIPs.add(ip);
         }
      }

      return nodeIPs;
   }

   private ArrayList<String> getAddedNodeIPs(List<BaseNode> addedNodes) {
      if (addedNodes == null) {
         return null;
      }

      ArrayList<String> nodeIPs = new ArrayList<String>();
      for (BaseNode node : addedNodes) {
         String mgtIp = node.getPrimaryMgtIpV4();
         if (mgtIp.equals(Constants.NULL_IPV4_ADDRESS)) {
            logger.error("Failed to get ip for added nodes");
            return nodeIPs;
         }
         nodeIPs.add(mgtIp);
      }

      return nodeIPs;
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

   @Autowired
   public void setSetPasswordService(ISetPasswordService setPasswordService) {
      this.setPasswordService = setPasswordService;
   }

   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   @Autowired
   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }
}