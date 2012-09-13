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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.task.LegacyTaskCleanner;
import com.vmware.bdd.manager.task.Task;
import com.vmware.bdd.manager.task.TaskListener;
import com.vmware.bdd.manager.task.TaskWorker;
import com.vmware.bdd.utils.Configuration;

public class TaskManager implements InitializingBean, DisposableBean {
   private static final Logger logger = Logger.getLogger(TaskManager.class);
   public static final String STDOUT_FILENAME = "stdout.log";
   public static final String STDERR_FILENAME = "stderr.log";

   private static int poolSize = 10;
   private static int queueSize = 50;
   private static String cookie;

   private static ExecutorService executorService;

   static {
      poolSize = Configuration.getInt("task.threadpool.workers", poolSize);
      poolSize = Math.max(1, poolSize);
      queueSize = Configuration.getInt("task.threadpool.queue_size", queueSize);
      queueSize = Math.max(1, queueSize);

      long keepAliveTimeMs = 60000;
      executorService = new ThreadPoolExecutor(poolSize, poolSize, keepAliveTimeMs,
            TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(queueSize));

      // This changes when executorService is recreated.
      cookie = generateCookie(poolSize, queueSize, keepAliveTimeMs,
            TimeUnit.MICROSECONDS);
      logger.info("generating task manager cookie: " + cookie);
   }

   /**
    * Generate a weak (non-standard) UUID string based on current time,
    * parameters and pseudo random number.
    * 
    * @param params
    *           list of parameters
    * @return UUID string
    */
   private static String generateCookie(Object... params) {
      long timestamp = System.currentTimeMillis();
      long rand = new Random().nextInt();
      long hash = 0;
      for (Object p : params) {
         if (p != null) {
            hash ^= p.hashCode();
         }
      }
      /*
       * |--hash/8b--|--rand-8b--|----ts-16b----|
       * 
       * discard the sign bits of rand
       */
      return new UUID((hash << 32) | (rand & 0xFFFFFFFFL), timestamp).toString();
   }

   public TaskEntity createCmdlineTask(final String[] cmdArray, final TaskListener listener) {
      return DAL.inRwTransactionDo(new Saveable<TaskEntity>() {
         public TaskEntity body() {
            TaskEntity task = new TaskEntity(cmdArray, listener, cookie);
            logger.debug("creating task: " + task);
            DAL.insert(task);
            return task;
         }
      });
   }

   public TaskEntity createMessageTask(final boolean retry, final TaskListener listener) {
      return DAL.inRwTransactionDo(new Saveable<TaskEntity>() {
         public TaskEntity body() {
            TaskEntity task = new TaskEntity(null, listener, cookie);
            task.setRetry(retry);
            logger.debug("creating task: " + task);
            DAL.insert(task);
            return task;
         }
      });
   }

   public void submit(TaskEntity taskEntity, Boolean enableMq, TaskWorker taskWorker) {
      logger.debug("submitting task: " + taskEntity);
      TaskEntity.updateStatus(taskEntity.getId(), Status.SUBMITTED, null);

      try {
         Task task;
         if (enableMq != null) {
            // enable/disable message queue at runtime, mainly for tests
            task = new Task(taskEntity.getId(), enableMq, taskWorker);
         } else {
            task = new Task(taskEntity.getId(), taskWorker);
         }
         executorService.submit(task);
      } catch (RejectedExecutionException e) {
         logger.error("task executor overloaded, task canceled");
         TaskEntity.updateStatus(taskEntity.getId(), Status.FAILED, "task executor overloaded, task canceled");
         throw TaskException.EXECUTOR_OVERLOADED();
      }

      return;
   }

   public void submit(TaskEntity task, TaskWorker taskWorker ) {
      submit(task, null, taskWorker);
   }

   public List<TaskRead> getTasks() {
      List<TaskEntity> taskEntities = DAL
            .inRoTransactionDo(new Saveable<List<TaskEntity>>() {
               @Override
               public List<TaskEntity> body() throws Exception {
                  // we may want to limit the max number and make it ordered
                  return DAL.findAll(TaskEntity.class);
               }
            });

      List<TaskRead> tasks = new ArrayList<TaskRead>();
      for (TaskEntity entity : taskEntities) {
         tasks.add(new TaskRead(entity.getId(), entity.getStatus(),
               entity.getProgress(), entity.getErrorMessage(), entity.getWorkDir()
                     .getAbsolutePath()));
      }

      return tasks;
   }

   public TaskRead getTaskById(final Long taskId) {
      TaskEntity entity = DAL.inRoTransactionDo(new Saveable<TaskEntity>() {
         @Override
         public TaskEntity body() throws Exception {
            return DAL.findById(TaskEntity.class, taskId);
         }
      });

      if (entity != null) {
         return new TaskRead(entity.getId(), entity.getStatus(), entity.getProgress(),
               entity.getErrorMessage(), entity.getWorkDir().getAbsolutePath());
      }

      return null;
   }

   @Override
   public void destroy() throws Exception {
      logger.info("shutting down thread pool executor");
      executorService.shutdownNow();
      executorService.awaitTermination(10, TimeUnit.SECONDS);
   }

   @Override
   public void afterPropertiesSet() throws Exception {
      // run as the 1st job
      logger.info("submit legacy task clean up job");
      executorService.submit(new LegacyTaskCleanner(cookie, this));
   }
}
