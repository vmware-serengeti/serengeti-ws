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
package com.vmware.bdd.utils;

public class ConfigInfo {
   private static final boolean debugEnabled = Configuration.getBoolean(
         "serengeti.debug", false);

   private static boolean mqEnabled = false;
   private static String mqExchangeName = "";

   private static String mqServerHost = "127.0.0.1";
   private static int mqServerPort = 5672;
   private static String mqServerUsername;
   private static String mqServerPassword;

   private static String serengetiRootVmFolderPrefix;
   private static String serengetiUUID;
   
   static {
      mqEnabled = Configuration.getBoolean("task.enable_mq", mqEnabled);
      mqExchangeName = Configuration.getString("task.rabbitmq.exchange", mqExchangeName);

      mqServerHost = Configuration.getString("task.rabbitmq.host", mqServerHost);
      mqServerPort = Configuration.getInt("task.rabbitmq.port", mqServerPort);
      mqServerUsername = Configuration.getString("task.rabbitmq.username");
      mqServerPassword = Configuration.getString("task.rabbitmq.password");
      serengetiRootVmFolderPrefix = Configuration.getString("serengeti.root_vm_folder_prefix");
      serengetiUUID = Configuration.getString("serengeti.uuid");
   }

   public static boolean isDebugEnabled() {
      return debugEnabled;
   }

   public static boolean isMqEnabled() {
      return mqEnabled;
   }

   public static void setMqEnabled(boolean mqEnabled) {
      ConfigInfo.mqEnabled = mqEnabled;
   }

   public static String getMqExchangeName() {
      return mqExchangeName;
   }

   public static String getMqServerHost() {
      return mqServerHost;
   }

   public static int getMqServerPort() {
      return mqServerPort;
   }

   public static String getMqServerUsername() {
      return mqServerUsername;
   }

   public static String getMqServerPassword() {
      return mqServerPassword;
   }
   
   public static String getSerengetiUUID() {
      return serengetiUUID;
   }
   
   public static String getSerengetiRootVmFolder() {
      return serengetiRootVmFolderPrefix + "-" + serengetiUUID;
   }
}
