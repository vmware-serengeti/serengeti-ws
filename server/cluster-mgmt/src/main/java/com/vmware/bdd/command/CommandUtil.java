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

import java.io.File;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.utils.AuAssert;

public class CommandUtil {
   private static final Logger logger = Logger.getLogger(CommandUtil.class);

   static File taskRootDir;
   private static final String TASK_ID_STR = "${task_id}";
   private static String routeKeyFormat = "task." + TASK_ID_STR;

   static {
      String taskRootDirStr = System.getProperty("serengeti.home.dir");
      if (taskRootDirStr == null) {
         taskRootDirStr = "/tmp/serengeti/";
      }

      taskRootDir = new File(taskRootDirStr, "logs/task/");

      logger.info("setting task work dir to: " + taskRootDir.getAbsolutePath());
      if (!taskRootDir.exists()) {
         logger.info("task root directory does not exist, try to create one");
         taskRootDir.mkdirs();
      }

      routeKeyFormat = Configuration.getString("task.rabbitmq.routekey_fmt",
            routeKeyFormat);
      AuAssert.check(routeKeyFormat.contains(TASK_ID_STR));
   }

   public static File createWorkDir(long executionId) {
      File path = new File(taskRootDir, Long.toString(executionId));
      if (!path.exists()) {
         path.mkdirs();
      }

      String dirs[] = path.list();
      long lastCmdId = 0;
      for (String dir : dirs) {
         long cmdId = Long.parseLong(dir);
         if (lastCmdId < cmdId) {
            lastCmdId = cmdId;
         }
      }
      Long nextCmdId = lastCmdId + 1;
      path = new File(path, nextCmdId.toString());
      path.mkdir();

      return path;
   }

}
