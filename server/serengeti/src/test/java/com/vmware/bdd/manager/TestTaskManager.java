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
package com.vmware.bdd.manager;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.TaskRead.Status;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.TaskEntity;

public class TestTaskManager {
   private static final Logger logger = Logger.getLogger(TestTaskManager.class);

   @BeforeMethod
   public void setup() {
   }

   @AfterMethod
   public void tearDown() {

   }

   private boolean isFileEmpty(File file) throws Exception {
      FileInputStream stdoutStream = new FileInputStream(file);
      int b = stdoutStream.read();
      stdoutStream.close();
      return b == -1;
   }

   private TaskEntity execAndWaitTask(String[] cmdArray, int timeout, boolean enableMq) {
      TaskManager taskManager = new TaskManager();
      TaskEntity task = taskManager.createCmdlineTask(cmdArray, new TestListener());
      // task.
      taskManager.submit(task, enableMq);
      while (timeout-- > 0) {
         try {
            DAL.inTransactionRefresh(task);
            logger.info("task status: " + task);
            if (Status.SUCCESS == task.getStatus() || Status.FAILED == task.getStatus()) {
               break;
            }
            Thread.sleep(1000);
         } catch (InterruptedException e) {
         }
      }

      return task;
   }

   @Test
   public void testTaskWithMq() {
      String[] cmdArray = { "echo", "xyz" };
      TaskEntity task = execAndWaitTask(cmdArray, 60, true);
      // failed due to missing message
      assertEquals(Status.FAILED, task.getStatus());
   }

   @Test
   public void testSuccessfulTask() {
      String[] cmdArray = { "touch", "foobar" };
      TaskEntity task = execAndWaitTask(cmdArray, 3, false);
      assertEquals(Status.SUCCESS, task.getStatus());
      File foobar = new File(task.getWorkDir(), "foobar");
      assertTrue(foobar.exists());
   }

   @Test
   public void testFailedTask() {
      String[] cmdArray = { "false" };
      TaskEntity task = execAndWaitTask(cmdArray, 3, false);
      assertEquals(Status.FAILED, task.getStatus());
   }

   @Test
   public void testTaskLogs() throws Exception {
      String[] cmdArray = { "/bin/sh", "-c", "echo stderrmsg 1>&2 | echo stdoutmsg" };
      TaskEntity task = execAndWaitTask(cmdArray, 5, false);
      assertEquals(Status.SUCCESS, task.getStatus());
      assertFalse(isFileEmpty(new File(task.getWorkDir(), TaskManager.STDOUT_FILENAME)));
      assertFalse(isFileEmpty(new File(task.getWorkDir(), TaskManager.STDERR_FILENAME)));
   }

   @Test
   public void testRunMultipleTasks() {
      String[] cmdArray = { "sleep", "1" };
      final int TOTAL_TASK = 3;
      List<TaskEntity> tasks = new ArrayList<TaskEntity>(TOTAL_TASK);
      for (int i = 0; i < TOTAL_TASK; ++i) {
         TaskEntity task = execAndWaitTask(cmdArray, 0, false);
         tasks.add(task);
      }

      final int TIMEOUT = TOTAL_TASK * 2; // max wait time
      long start = System.currentTimeMillis();
      for (int i = 0; i < TOTAL_TASK; ++i) {
         TaskEntity task = tasks.get(i);
         while (true) {
            try {
               Thread.sleep(1000);
               DAL.inTransactionRefresh(task);
               if (Status.SUCCESS == task.getStatus()) {
                  break;
               } else if (Status.FAILED == task.getStatus()) {
                  fail("Task failed");
               }
               long now = System.currentTimeMillis();
               if (now - start > TIMEOUT * 1000) {
                  fail("task execution timeout");
               }
            } catch (InterruptedException e) {
            }
         }
      }
   }
}
