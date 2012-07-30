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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.manager.TaskManager;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Configuration;

public class Task extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(Task.class);



   private boolean successed;
   private String errorMessage;
   private boolean mqEnabled;
   private TaskEntity taskEntity; 

   public Task(Long taskId, boolean mqEnabled) {
      this.taskEntity = TaskEntity.findById(taskId);
      this.successed = false;
      this.mqEnabled = mqEnabled;
   }

   public Task(Long taskId) {
      this(taskId, ConfigInfo.isMqEnabled());
   }

   /**
    * Check whether the os is *nix
    * 
    * @return true if *nix
    */
   private boolean isUnix() {
      String osName = System.getProperty("os.name").toLowerCase();
      return (osName.indexOf("linux") >= 0 || osName.indexOf("nix") >= 0);
   }

   /**
    * Kill a Linux process, not portable
    * 
    * @param pid
    */
   private boolean kill(Process proc) {
      try {
         // proc.destroy() does not work

         if (isUnix()) {
            Field field = proc.getClass().getDeclaredField("pid");
            field.setAccessible(true);
            Integer pid = (Integer) field.get(proc);

            String killCmd = Configuration.getString("kill_task.cmd");

            String[] cmdArrayKill = { killCmd, pid.toString() };
            Process procKill = Runtime.getRuntime().exec(cmdArrayKill);
            if (ConfigInfo.isDebugEnabled()) {
               logger.debug("stdout of " + killCmd + " " + pid);
               BufferedReader bufInStream = new BufferedReader(new InputStreamReader(
                     procKill.getInputStream()));
               String line;
               while ((line = bufInStream.readLine()) != null) {
                  logger.debug(line);
               }
               bufInStream.close();
               logger.debug("stderr of " + killCmd + " " + pid);
               bufInStream = new BufferedReader(new InputStreamReader(
                     procKill.getErrorStream()));
               while ((line = bufInStream.readLine()) != null) {
                  logger.debug(line);
               }
               bufInStream.close();
            }
            int exit = procKill.waitFor();
            logger.info("exit value = " + exit);
            if (exit == 0) {
               return true;
            }
         } else {
            logger.warn("The kill operation only support *nix platform");;
         }
      } catch (Throwable t) {
         logger.error("failed to kill process: " + proc, t);
      }

      return false;
   }

   @Override
   public void doWork() throws IOException {
      // log level: info -V, debug -VV
      String[] cmdArray;
      if (logger.isInfoEnabled()) {
         cmdArray = new String[taskEntity.getCmdArray().length + 1];
         int i;
         for (i = 0; i < taskEntity.getCmdArray().length; ++i) {
            cmdArray[i] = taskEntity.getCmdArray()[i];
         }
         cmdArray[i] = logger.isDebugEnabled() ? "-VV" : "-V";
      } else {
         cmdArray = taskEntity.getCmdArray();
      }

      Process proc = Runtime.getRuntime().exec(cmdArray, null, taskEntity.getWorkDir());

      /*
       * Start independent threads instead of dumping the output synchronously
       * to avoid excessive memory usage
       */
      Thread stdoutReaperThread = new Thread(new StreamReaper(proc.getInputStream(),
            new File(taskEntity.getWorkDir(), TaskManager.STDOUT_FILENAME)));
      Thread stderrReaperThread = new Thread(new StreamReaper(proc.getErrorStream(),
            new File(taskEntity.getWorkDir(), TaskManager.STDERR_FILENAME)));

      /*
       * Message processing thread.
       */
      MessageProcessor messageProcessor = null;
      Thread messageProcessorThread = null;
      if (mqEnabled) {
         String routeKey = taskEntity.getMessageRouteKey();
         messageProcessor = new MessageProcessor(taskEntity.getId(),
               ConfigInfo.getMqServerHost(), ConfigInfo.getMqServerPort(),
               ConfigInfo.getMqServerUsername(), ConfigInfo.getMqServerPassword(),
               ConfigInfo.getMqExchangeName(), routeKey, routeKey);
         messageProcessorThread = new Thread(messageProcessor);
         messageProcessorThread.setDaemon(true);
         messageProcessorThread.start();
      }

      stdoutReaperThread.setDaemon(true);
      stderrReaperThread.setDaemon(true);
      stdoutReaperThread.start();
      stderrReaperThread.start();

      boolean exitSuccess = true;
      while (true) {
         try {
            int exitValue = proc.waitFor();
            if (exitValue == 0) {
               successed = true;
               logger.info("command of task " + taskEntity.getId() + " succeeded");
            } else {
               logger.error("command of task " + taskEntity.getId()
                     + " failed, exit value = " + exitValue);
            }

            break;
         } catch (InterruptedException e) {
            // scheduler's shutdownNow is called
            logger.warn("therad interrupted, kill the subprocess", e);
            exitSuccess = kill(proc);

            if (mqEnabled) {
               logger.warn("interrupted, force shutdown message receiver now", e);
               messageProcessor.forceStopNow();
            }
            break;
         }
      }

      try {
         logger.info("joining helper threads for task: " + taskEntity.getId());
         if (mqEnabled) {
            messageProcessor.forceStop();
            try {
               messageProcessorThread.join();
            } catch (InterruptedException e) {
               logger.warn("interrupted, force shutdown message receiver now", e);
               if (mqEnabled) {
                  messageProcessor.forceStopNow();
                  messageProcessorThread.join();
               }
            }
         }

         if (exitSuccess) {
            stdoutReaperThread.join();
            stderrReaperThread.join();
         } else {
            logger.error("the underlying command might be still running: "
                  + taskEntity.getCmdArray());
         }

         logger.info("helper threads joined for task: " + taskEntity.getId());
      } catch (InterruptedException e) {
         logger.error("interrupted, skip to join stdout/stderr dump threads", e);
      }

      // decide task final status
      if (mqEnabled) {
         // judge task status according to message
         successed = messageProcessor.isSuccess();
         if (!successed){
            errorMessage = messageProcessor.getErrorMessage();
         }
      }
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
