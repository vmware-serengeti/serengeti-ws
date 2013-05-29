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
package com.vmware.bdd.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.RabbitMQConsumer;
import com.vmware.bdd.utils.TracedRunnable;

public class VHMMessageProcessor extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(VHMMessageProcessor.class);

   private MessageHandler messageHandler;
   private RabbitMQConsumer mqConsumer;
   private volatile boolean done = false;
   private volatile boolean success = false;
   private String errorMessage = null;

   public VHMMessageProcessor(String serverHost, int serverPort, String serverUsername,
         String serverPassword, String exchangeName, String queueName, String routeKey,
         boolean getQueue, MessageHandler messageHandler) throws IOException {
      super();
      this.messageHandler = messageHandler;

      mqConsumer = new RabbitMQConsumer(serverHost, serverPort, serverUsername,
            serverPassword, exchangeName, queueName, routeKey, getQueue);
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

   public void graceStop(long timeMs) throws IOException {
      if (!done) {
         mqConsumer.graceStop(timeMs);
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

            boolean finished = (Boolean) msgMap.get(Constants.FINISH_FIELD);

            if (finished) {
               success = (Boolean) msgMap.get(Constants.SUCCEED_FIELD);
               if (!success) {
                  errorMessage = (String) msgMap.get(Constants.ERROR_MSG_FIELD);
               }
            }

            // TODO: consider timeout issue
            if (!success && messageHandler != null) {
               double progress = (Double) msgMap.get(Constants.PROGRESS_FIELD) / 100;
               //String progressMsg = (String) msgMap.get(Constants.PROGRESS_MESSAGE_FIELD);
               messageHandler.setProgress(progress);
            }

            if (messageHandler != null) {
               messageHandler.onMessage(msgMap);
            }

            return !finished;
         }
      });
   }

   @Override
   public void onStart() throws IOException {
      done = false;
      success = false;
   }

   @Override
   public void onException(Throwable t) {
      logger.error("failed processing messages", t);
   }

   @Override
   public void onFinish() {
      done = true;
   }
}
