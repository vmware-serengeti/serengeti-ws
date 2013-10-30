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
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.utils.AuAssert;

public class SetPasswordForAllNodesStep extends TrackableTasklet {
   private ISetPasswordService setPasswordService;
   private ClusterConfigManager configMgr;
   private ClusterEntityManager clusterEntityMgr;
   private static final Logger logger = Logger.getLogger(ClusterUpdateDataStep.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) {

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);

      String newPassword = clusterSpec.getPassword();
      //if user didn't set password, return directly
      if (newPassword == null) {
         logger.info("User didn't set password.");
         return RepeatStatus.FINISHED;
      }

      ArrayList<String> ipOfAllNodes = getAllNodeIPs(clusterName);
      if (ipOfAllNodes.isEmpty()) {
         return RepeatStatus.FINISHED;
      }

      ArrayList<String> failedNodes = setPasswordService.setPasswordForNodes(clusterName, ipOfAllNodes, newPassword);
      boolean success = false;
      if (failedNodes == null) {
         success = true;
      } else {
    	  logger.info("failed to set password for " + failedNodes.toString());
      }

      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_EXISTING_NODES_JOB_PARAM, success);
      if (!success) {
         throw TaskException.EXECUTION_FAILED("failed to set password for cluster " + clusterName);
      }
      return RepeatStatus.FINISHED;
   }

   private ArrayList<String> getAllNodeIPs(String clusterName) {
      clusterEntityMgr = getClusterEntityMgr();
      AuAssert.check(clusterEntityMgr != null);
      ArrayList<String> nodeIPs = new ArrayList<String>();
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);
      for (NodeEntity node : nodes) {
         nodeIPs.add(node.getMgtIp());
      }

      return nodeIPs;
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }

   public ClusterEntityManager getClusterEntityManager() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityManager(ClusterEntityManager clusterEntityManager) {
      this.clusterEntityMgr = clusterEntityManager;
   }

   public ISetPasswordService getSetPasswordService() {
      return setPasswordService;
   }

   @Autowired
   public void setSetPasswordService(ISetPasswordService setPasswordService) {
      this.setPasswordService = setPasswordService;
   }
}