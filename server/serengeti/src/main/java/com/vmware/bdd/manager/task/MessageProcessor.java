/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.BddMessageUtil;
import com.vmware.bdd.utils.RabbitMQConsumer;

class MessageProcessor extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(MessageProcessor.class);
   private Long taskId;

   private TaskListener taskListener;
   private RabbitMQConsumer mqConsumer;
   private volatile boolean done = false;
   private volatile boolean success = false;
   private String errorMessage = null;

   public MessageProcessor(Long taskId, String serverHost, int serverPort,
         String serverUsername, String serverPassword, String exchangeName,
         String queueName, String routeKey) throws IOException {
      super();
      this.taskId = taskId;

      mqConsumer = new RabbitMQConsumer(serverHost, serverPort, serverUsername,
            serverPassword, exchangeName, queueName, routeKey);
   }

   public void forceStopNow() throws IOException {
      if (!done) {
         mqConsumer.forceStopNow();
      }
   }

   public void forceStop() throws IOException {
      if (!done) {
         mqConsumer.forceStop();
      }
   }

   public boolean isDone() {
      return done;
   }

   public boolean isSuccess() {
      return success;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   @Override
   public void doWork() throws IOException {
      mqConsumer.processMessage(new RabbitMQConsumer.MessageListener() {
         @Override
         public boolean onMessage(String message) throws Exception {
            logger.info("processing message: " + message);
            @SuppressWarnings("unchecked")
            Map<String, Object> msgMap = (HashMap<String, Object>) (new Gson())
                  .fromJson(message, new TypeToken<HashMap<String, Object>>() {
                  }.getType());

            boolean finished = (Boolean) msgMap.get(BddMessageUtil.FINISH_FIELD);

            if (finished) {
               success = (Boolean) msgMap.get(BddMessageUtil.SUCCEED_FIELD);
               if (!success) {
                  errorMessage = (String) msgMap.get(BddMessageUtil.ERROR_MSG_FIELD);
               }
            }

            if (!success) {
               double progress = (Double) msgMap.get(BddMessageUtil.PROGRESS_FIELD) / 100;
               // TODO need write throttling?
               TaskEntity.updateProgress(taskId, progress);
            }

            taskListener.onMessage(msgMap);

            return !finished;
         }
      });
   }

   @Override
   public void onStart() throws IOException {
      done = false;
      success = false;
      logger.info("start processing messages for task: " + taskId);

      TaskEntity taskEntity = TaskEntity.findById(taskId);
      AuAssert.check(taskEntity != null);
      taskListener = taskEntity.getTaskListener();
   }

   @Override
   public void onException(Throwable t) {
      logger.error("failed processing messages for task: " + taskId);
   }

   @Override
   public void onFinish() {
      done = true;
      logger.info("finish processing messages for task: " + taskId);
   }
}