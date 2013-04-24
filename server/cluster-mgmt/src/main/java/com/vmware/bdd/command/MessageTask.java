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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.manager.RuntimeConnectionManager;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;

public class MessageTask implements Callable<Map<String, Object>> {
   private static final Logger logger = Logger.getLogger(MessageTask.class);

   private Map<String, Object> sendParam;
   boolean mqEnabled;
   boolean succeed;
   String errorMessage;
   MessageHandler messageHandler;

   private RuntimeConnectionManager runtimeConnectionManager;

   public MessageTask(Map<String, Object> sendParam, MessageHandler messageHandler,
         boolean mqEnabled) {
      this.sendParam = sendParam;
      this.messageHandler = messageHandler;
      this.mqEnabled = mqEnabled;
   }

   public MessageTask(Map<String, Object> sendParam, MessageHandler messageHandler) {
      this(sendParam, messageHandler, true);
   }

   @Override
   public Map<String, Object> call() throws Exception {
      Map<String, Object> result = new HashMap<String, Object>();

      Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().create();
      String jsonStr = gson.toJson(sendParam);
      AuAssert.check(jsonStr != null);
      logger.info("send " + jsonStr + " to rabbitMQ. ");
      // Send message
      if (runtimeConnectionManager == null) {
         runtimeConnectionManager = new RuntimeConnectionManager();
      }
      if (runtimeConnectionManager.getRuntimeChannel() == null
            || !runtimeConnectionManager.getRuntimeChannel().isOpen()) {
         runtimeConnectionManager.init();
      }
      /*
       * Message processing thread.
       */
      MessageProcessor messageProcessor = null;
      Thread messageProcessorThread = null;
      if (mqEnabled) {
         messageProcessor =
               new MessageProcessor(
                     ConfigInfo.getMqServerHost(),
                     ConfigInfo.getMqServerPort(),
                     ConfigInfo.getMqServerUsername(),
                     ConfigInfo.getMqServerPassword(),
                     ConfigInfo.getRuntimeMqExchangeName(),
                     "",
                     (String) sendParam
                           .get(Constants.SET_MANUAL_ELASTICITY_INFO_RECEIVE_ROUTE_KEY),
                     true, messageHandler);
         messageProcessorThread = new Thread(messageProcessor);
         messageProcessorThread.setDaemon(true);
         messageProcessorThread.start();

         try {
            Thread.sleep(2000);
         } catch (InterruptedException e) {
            logger.error(e.getMessage());
         }
         runtimeConnectionManager.sendMessage(
               ConfigInfo.getRuntimeMqSendRouteKey(), jsonStr);
         messageProcessor.graceStop(300000);
         try {
            messageProcessorThread.join();
            logger.info("helper threads joined for message task");
         } catch (InterruptedException e) {
            logger.warn("interrupted, force shutdown message receiver now", e);
            try {
               messageProcessor.forceStopNow();
               messageProcessorThread.join();
            } catch (InterruptedException e1) {
               logger.error(e1);
            }
         }

         // udge task status according to message
         succeed = messageProcessor.isSuccess();
         if (!succeed) {
            errorMessage = messageProcessor.getErrorMessage();
            if (CommonUtil.isBlank(errorMessage)) {
               errorMessage = "No error message from VHM.";
               logger.error(errorMessage);
            }
         }
      }
      runtimeConnectionManager.destroy();
      logger.info("Task status [succeed => " + succeed + ",errorMessage =>"
            + errorMessage);
      result.put("succeed", succeed);
      result.put("errorMessage", errorMessage);
      return result;
   }

}
