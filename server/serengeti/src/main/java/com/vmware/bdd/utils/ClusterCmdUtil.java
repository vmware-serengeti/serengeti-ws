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
package com.vmware.bdd.utils;

public class ClusterCmdUtil {
   private static final String CREATE_CLUSTER_CMD = Configuration
         .getNonEmptyString("create_cluster.cmd");
   private static final String DELETE_CLUSTER_CMD = Configuration
         .getNonEmptyString("delete_cluster.cmd");
   private static final String UPDATE_CLUSTER_CMD = Configuration
         .getNonEmptyString("update_cluster.cmd");
   private static final String STOP_CLUSTER_CMD = Configuration
         .getNonEmptyString("stop_cluster.cmd");
   private static final String START_CLUSTER_CMD = Configuration
         .getNonEmptyString("start_cluster.cmd");
   private static final String CONFIGURE_CLUSTER_CMD = Configuration
   .getNonEmptyString("configure_cluster.cmd");

   public static String[] getCreateClusterCmdArray(String clusterName,
         String fileName) {
      // TODO: handling spaces between quote 
      return CREATE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).split(" ");
   }

   public static String[] getDeleteClusterCmdArray(String clusterName,
         String fileName) {
      return DELETE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).split(" ");
   }

   public static String[] getStartClusterCmdArray(String clusterName,
         String fileName) {
      return START_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).split(" ");
   }

   public static String[] getStopClusterCmdArray(String clusterName,
         String fileName) {
      return STOP_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).split(" ");
   }

   public static String[] getUpdatetClusterCmdArray(String clusterName,
         String fileName) {
      return UPDATE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).split(" ");
   }

   public static String[] getConfigureClusterCmdArray(String clusterName,
         String fileName) {
      return CONFIGURE_CLUSTER_CMD.replaceAll(":cluster_name", clusterName)
            .replaceAll(":json_file", fileName).split(" ");
   }
}
