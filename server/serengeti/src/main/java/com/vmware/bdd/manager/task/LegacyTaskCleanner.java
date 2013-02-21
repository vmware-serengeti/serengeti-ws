/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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

import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.manager.TaskManager;
import com.vmware.bdd.utils.AuAssert;

public class LegacyTaskCleanner extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(LegacyTaskCleanner.class);

   private String taskManagerCookie;
   private TaskManager taskManager;

   public LegacyTaskCleanner(String taskManagerCookie, TaskManager taskManager) {
      this.taskManagerCookie = taskManagerCookie;
      this.taskManager = taskManager;
   }

   @Override
   public void doWork() throws Exception {
      AuAssert.check(taskManagerCookie != null, "should never be null");

      List<TaskEntity> tasks = DAL.inRwTransactionDo(new Saveable<List<TaskEntity>>() {
         @Override
         public List<TaskEntity> body() throws Exception {
            TaskRead.Status[] badStatus = { TaskRead.Status.CREATED };
            for (TaskEntity task : TaskEntity.findAllByStatus(badStatus,
                  taskManagerCookie)) {
               logger.warn("clean up bad lagacy atask: " + task);
               task.setStatus(TaskRead.Status.FAILED);
            }

            /*
             * also retry RUNNING tasks, to make cluster status consistent with
             * tasks.
             */
            TaskRead.Status[] healthStatus = { TaskRead.Status.SUBMITTED,
                  TaskRead.Status.RUNNING };
            return TaskEntity.findAllByStatus(healthStatus, taskManagerCookie);
         }
      });

      /*
       * put this out of transaction because it's not idepotent, i.e. in case of
       * retry.
       */
      for (TaskEntity task : tasks) {
         if (task.isRetry()) {
            logger.info("re-submit lagacy task: " + task);
            taskManager.submit(task, new CommandTaskWorker());            
         }
      }
   }

   @Override
   public void onStart() {
      logger.debug("start to scan and clean up lagacy tasks");
   }

   @Override
   public void onException(Throwable t) {
      logger.debug("failed to scan and clean up lagacy tasks");
   }

   @Override
   public void onFinish() {
      logger.debug("finish scanning and cleanning up lagacy tasks");
   }
}
