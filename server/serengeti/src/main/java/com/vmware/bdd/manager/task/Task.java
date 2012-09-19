/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager.task;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.utils.ConfigInfo;

public class Task extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(Task.class);
   private boolean successed;
   private String errorMessage;
   private boolean mqEnabled;
   private TaskEntity taskEntity; 
   private TaskWorker taskWorker;
   public Task(Long taskId, boolean mqEnabled, TaskWorker taskWorker) {
      this.taskEntity = TaskEntity.findById(taskId);
      this.successed = false;
      this.mqEnabled = mqEnabled;
      this.taskWorker = taskWorker;
   }

   public Task(Long taskId, TaskWorker taskWorker) {
      this(taskId, ConfigInfo.isMqEnabled(), taskWorker);
   }

   @Override
   public void doWork() throws IOException {
      Map<String, Object> result = taskWorker.work(mqEnabled, taskEntity, successed, errorMessage, logger);
      successed = ((Boolean)(result.get("successed"))).booleanValue();
      errorMessage = (String)(result.get("errorMessage"));
   }

   @Override
   public void onStart() {
      logger.info("start executing task: " + taskEntity.getId());
      successed = false;
      errorMessage = null;
      TaskEntity.updateStatus(taskEntity.getId(), Status.RUNNING, null);
   }

   @Override
   public void onException(Throwable t) {
      logger.error("failed to run task: " + taskEntity.getId());
      try {
         taskEntity.getTaskListener().onFailure();
      } catch (Throwable tr) {
         logger.error("failed to call onFaliure", tr);
      }
      TaskEntity.updateStatus(taskEntity.getId(), Status.FAILED,
            "runtime error: " + t.getMessage());
   }

   @Override
   public void onFinish() {
      logger.info("finish to run task: " + taskEntity.getId());
      if (successed) {
         taskEntity.getTaskListener().onSuccess();
         TaskEntity.updateStatus(taskEntity.getId(), Status.SUCCESS, null);
      } else {
         taskEntity.getTaskListener().onFailure();
         TaskEntity.updateStatus(taskEntity.getId(), Status.FAILED, errorMessage);
      }
   }
}
