package com.vmware.bdd.manager.task;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.manager.RuntimeConnectionManager;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;

public class MessageTaskWorker implements TaskWorker {
   
   private Map<String,Object> sendParam;
   @Autowired
   private RuntimeConnectionManager runtimeConnectionManager;

   public MessageTaskWorker(Map<String,Object> sendParam) {
      this.sendParam = sendParam;
   }
   
   @Override
   public Map<String, Object> work(boolean mqEnabled, TaskEntity taskEntity,
         boolean successed, String errorMessage, Logger logger)
         throws IOException {
      Map<String, Object> result = new HashMap<String, Object>();
      
      Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
               .setPrettyPrinting().create();
      String jsonStr = gson.toJson(sendParam);
      AuAssert.check(jsonStr != null);
      logger.info("send " + jsonStr + " to rabbitMQ. ");
      //Send message
      if(runtimeConnectionManager == null) {
         runtimeConnectionManager = new RuntimeConnectionManager();
      }
      if(runtimeConnectionManager.getRuntimeChannel() == null || !runtimeConnectionManager.getRuntimeChannel().isOpen()){
         runtimeConnectionManager.init();
      }
      /*
       * Message processing thread.
       */
      MessageProcessor messageProcessor = null;
      Thread messageProcessorThread = null;
      if (mqEnabled) {
         messageProcessor =
               new MessageProcessor(taskEntity.getId(),
                     ConfigInfo.getMqServerHost(),
                     ConfigInfo.getMqServerPort(),
                     ConfigInfo.getMqServerUsername(),
                     ConfigInfo.getMqServerPassword(),
                     ConfigInfo.getRuntimeMqExchangeName(), "",
                     ConfigInfo.getRuntimeMqReceiveRouteKey(), true);
         messageProcessorThread = new Thread(messageProcessor);
         messageProcessorThread.setDaemon(true);
         messageProcessorThread.start();

         try {
            Thread.sleep(2000);
         } catch (InterruptedException e) {
            logger.error(e.getMessage());
         }
         runtimeConnectionManager.sendMessage(ConfigInfo.getRuntimeMqSendRouteKey(), jsonStr);
         messageProcessor.forceStop();
         try {
            messageProcessorThread.join();
            logger.info("helper threads joined for task: " + taskEntity.getId());
         } catch (InterruptedException e) {
            logger.warn("interrupted, force shutdown message receiver now", e);
            try {
               messageProcessor.forceStopNow();
               messageProcessorThread.join();
            } catch (InterruptedException e1) {
               logger.error(e1);
            }
         }

         //udge task status according to message
         successed = messageProcessor.isSuccess();
         if (!successed) {
            errorMessage = messageProcessor.getErrorMessage();
            if(CommonUtil.isBlank(errorMessage)){
               errorMessage = "Cannot find message from VHM.";
               logger.error(errorMessage);
            }
         }
      }
      runtimeConnectionManager.destroy();
      logger.info("Task status [successed => " + successed + ",errorMessage =>"
            + errorMessage);
      result.put("successed", successed);
      result.put("errorMessage", errorMessage);
      return result;
   }

}
