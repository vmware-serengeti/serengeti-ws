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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.TaskEntity;

public class ClusterCmdUtil {
   private static final Level logLevel = Logger
         .getRootLogger().getLevel();
   private static final String QUERY_CLUSTER_CMD = Configuration
   .getNonEmptyString("query_cluster.cmd");
   private static final String CREATE_CLUSTER_CMD = Configuration
         .getNonEmptyString("create_cluster.cmd");
   private static final String DELETE_CLUSTER_CMD = Configuration
         .getNonEmptyString("delete_cluster.cmd");
   private static final String UPDATE_CLUSTER_CMD = Configuration
         .getNonEmptyString("update_cluster.cmd");
   private static final String STOP_NODES_CMD = Configuration
         .getNonEmptyString("stop_cluster_node.cmd");
   private static final String START_NODES_CMD = Configuration
         .getNonEmptyString("start_cluster_node.cmd");
   private static final String CONFIGURE_CLUSTER_CMD = Configuration
         .getNonEmptyString("configure_cluster.cmd");
   private static final String CONFIGURE_HARDWARE_CMD = Configuration
         .getNonEmptyString("configure_hardware.cmd");

   private static String getLogLevel() {
      if (logLevel.isGreaterOrEqual(Level.ERROR)) {
         return "";
      } else if (logLevel.isGreaterOrEqual(Level.INFO)) {
         return "-V";
      } else { 
         return "-VV";
      }
   }
   
   public static String[] getQueryClusterCmdArray(String clusterName,
         String fileName) {
      return QUERY_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
      
   }

   public static String[] getCreateClusterCmdArray(String clusterName,
         String fileName) {
      // TODO: handling spaces between quote 
      return CREATE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String[] getDeleteClusterCmdArray(String clusterName,
         String fileName) {
      return DELETE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String[] getStartClusterNodesCmdArray(String nodesName,
         String fileName) {
      return START_NODES_CMD.replaceAll(":nodes_name", nodesName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String[] getStopClusterNodesCmdArray(String nodesName,
         String fileName) {
      return STOP_NODES_CMD.replaceAll(":nodes_name", nodesName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String[] getUpdatetClusterCmdArray(String clusterName,
         String fileName) {
      return UPDATE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String[] getConfigureClusterCmdArray(String clusterName,
         String fileName) {
      return CONFIGURE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String[] getConfigureHardwareCmdArray(String clusterName,
         String fileName) {
      return CONFIGURE_HARDWARE_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).replaceAll(":log_level", getLogLevel()).split(" ");
   }

   public static String getFullNodeName(String cluster, String group,
         String node) {
      AuAssert.check(cluster != null && !cluster.isEmpty());
      AuAssert.check(group == null || !group.isEmpty());
      AuAssert.check(node == null || !node.isEmpty());
      AuAssert.check(!(node != null && group == null));

      if (node != null) {
         return node; // node is already the full name
      }

      if (group == null) {
         return cluster;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append(cluster).append("-").append(group);
         return sb.toString();
      }
   }

   public static int getIndexFromNodeName(String node) {
      AuAssert.check(node != null && !node.isEmpty());

      String[] ary = node.split("-");
      AuAssert.check(ary.length == 3);

      return Integer.parseInt(ary[2]);
   }

   /**
    * wait for task to complete
    * @param taskId
    *   task ID
    * @return
    *   true for a successfully executed task, false otherwise
    */
   public static boolean waitForTask(Long taskId) {
      TaskEntity task = TaskEntity.findById(taskId);
      AuAssert.check(task != null);

      while (true) {
         try {
            Thread.sleep(100);
         } catch (InterruptedException e) {
            // ignore
         }
         DAL.inTransactionRefresh(task);
         if (com.vmware.bdd.apitypes.TaskRead.Status.SUCCESS.equals(task
               .getStatus()))
            return true;
         if (com.vmware.bdd.apitypes.TaskRead.Status.FAILED.equals(task
               .getStatus()))
            return false;
      }
   }
}
