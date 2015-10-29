/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ironfan.utils;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * Created by qjin on 3/12/15.
 */
public class DefaultUtils {
   private static final Logger logger = Logger.getLogger(DefaultUtils.class);

   public static String getConfDir() {
      return com.vmware.bdd.utils.CommonUtil.getConfDir() + File.separator + Constants.DEFAULT_PLUGIN_NAME;
   }

   public static Process launchProcess(String cmd) {
       if (cmd == null || cmd.isEmpty()) {
           return null;
       }

       Process p = null;
       try {
           ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmd.split(" ")));
           pb.redirectErrorStream();
           p = pb.start();
           p.waitFor();
       } catch (Exception e) {
           p = null;
           logger.error("Failed to execute command " + cmd + " : " + e.getMessage());
       }

       return p;
   }

   public static boolean exec(String cmd) {
      Process p = launchProcess(cmd);
      return p != null && p.exitValue() == 0;
  }
}
