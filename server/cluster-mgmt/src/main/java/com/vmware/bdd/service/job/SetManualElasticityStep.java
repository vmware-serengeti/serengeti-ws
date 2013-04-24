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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.command.MessageHandler;
import com.vmware.bdd.command.MessageTask;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.task.VHMReceiveListener;
import com.vmware.bdd.service.IExecutionService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;

public class SetManualElasticityStep extends TrackableTasklet {
   IExecutionService executionService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName = getJobParameters(chunkContext).getString(
            JobConstants.CLUSTER_NAME_JOB_PARAM);
      String nodeGroupNamesJson = getJobParameters(chunkContext).getString(
            JobConstants.GROUP_NAME_JOB_PARAM);
      List<String> nodeGroupNames = new Gson().fromJson(nodeGroupNamesJson,
            new TypeToken<List<String>>() {
            }.getType());
      String hadoopJobTrackerIP = getJobParameters(chunkContext).getString(
            JobConstants.HADOOP_JOBTRACKER_IP_JOB_PARAM);
      Long activeComputeNodeNum = getJobParameters(chunkContext).getLong(
            JobConstants.GROUP_ACTIVE_COMPUTE_NODE_NUMBER_JOB_PARAM);

      StatusUpdater statusUpdater = new DefaultStatusUpdater(jobExecutionStatusHolder,
            getJobExecutionId(chunkContext));

      // submit a MQ task
      MessageHandler listener = new VHMReceiveListener(clusterName, statusUpdater);

      Map<String, Object> sendParam = new HashMap<String, Object>();
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_VERSION, 1);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_CLUSTER_NAME, clusterName);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_JOBTRACKER, hadoopJobTrackerIP);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_INSTANCE_NUM, activeComputeNodeNum);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_NODE_GROUPS, nodeGroupNames);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_SERENGETI_INSTANCE,
            ConfigInfo.getSerengetiRootFolder());
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_RECEIVE_ROUTE_KEY, CommonUtil.getUUID());

      Map<String, Object> ret = executionService.execute(new MessageTask(sendParam,
            listener));

      if (!(Boolean) ret.get("succeed")) {
         String errorMessage = (String) ret.get("errorMessage");
         putIntoJobExecutionContext(chunkContext, JobConstants.CURRENT_ERROR_MESSAGE,
               errorMessage);
         throw TaskException.EXECUTION_FAILED(errorMessage);
      }

      return RepeatStatus.FINISHED;
   }

   public IExecutionService getExecutionService() {
      return executionService;
   }

   public void setExecutionService(IExecutionService executionService) {
      this.executionService = executionService;
   }
}
