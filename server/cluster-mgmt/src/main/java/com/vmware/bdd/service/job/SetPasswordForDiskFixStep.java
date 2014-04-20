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

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.utils.CommonUtil;

public class SetPasswordForDiskFixStep extends TrackableTasklet {
   private ISetPasswordService setPasswordService;
   private ClusterConfigManager configMgr;
   private static final Logger logger = Logger.getLogger(SetPasswordForDiskFixStep.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) {
      logger.info("set password for disk fix");

      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);

      String newPassword = clusterSpec.getPassword();

      String targetNode = getJobParameters(chunkContext).getString(JobConstants.SUB_JOB_NODE_NAME);
      NodeEntity nodeEntity = clusterEntityMgr.findNodeByName(targetNode);
      if (nodeEntity == null) {
         throw TaskException.EXECUTION_FAILED("No fixed node need to set password for.");
      }

      boolean success = false;
      try {
         success = setPasswordService.setPasswordForNode(clusterName, nodeEntity, newPassword);
         putIntoJobExecutionContext(chunkContext, JobConstants.SET_PASSWORD_SUCCEED_JOB_PARAM, success);
      } catch (Exception e) {
         logger.error("Failed to set password for " + nodeEntity.getVmNameWithIP(), e);
         putIntoJobExecutionContext(chunkContext, JobConstants.SET_PASSWORD_SUCCEED_JOB_PARAM, success);
      }
      return RepeatStatus.FINISHED;
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
}