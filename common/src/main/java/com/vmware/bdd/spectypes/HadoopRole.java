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
package com.vmware.bdd.spectypes;

import java.util.Comparator;

public enum HadoopRole {
   PIG_ROLE("pig"), 
   HIVE_ROLE("hive"),
   HIVE_SERVER_ROLE("hive_server"), 
   HADOOP_CLIENT_ROLE("hadoop_client"),
   HADOOP_DATANODE("hadoop_datanode"), 
   HADOOP_TASKTRACKER("hadoop_tasktracker"), 
   HADOOP_JOBTRACKER_ROLE("hadoop_jobtracker"), 
   HADOOP_NAMENODE_ROLE("hadoop_namenode"),
   HBASE_MASTER_ROLE("hbase_master"),
   HBASE_REGIONSERVER_ROLE("hbase_regionserver"),
   HBASE_CLIENT_ROLE("hbase_client"),
   ZOOKEEPER_ROLE("zookeeper");

   private String description;

   private HadoopRole(String description) {
      this.description = description;
   }

   public String toString() {
      return this.description;
   }
   
   public boolean shouldRunAfterHDFS() {
      if (this.equals(HadoopRole.HBASE_CLIENT_ROLE) || this.equals(HadoopRole.HBASE_MASTER_ROLE) || this.equals(HadoopRole.HBASE_REGIONSERVER_ROLE)) {
         return true;
      }
      return false;
   }

   public static HadoopRole fromString(String desc) {
      if (desc != null) {
         for (HadoopRole b : HadoopRole.values()) {
            if (desc.equalsIgnoreCase(b.toString())) {
               return b;
            }
         }
      }
      return null;
   }
   
   /*
    * Get the relative dependencies of roles. The more dependent, the bigger value
    * TODO The dependency algorithm will be improved to be more generic in the future. 
    */
   public static int getDependency(String desc) {
      final int MAX_DEPENDENCY = 20; 
      HadoopRole role = fromString(desc);
      if (role != null) {
         switch (role) {
         case HADOOP_NAMENODE_ROLE:
         case ZOOKEEPER_ROLE:
            return 0;
         case HBASE_MASTER_ROLE:
         case HADOOP_DATANODE:
         case HADOOP_JOBTRACKER_ROLE:
            return 1;
         case HADOOP_TASKTRACKER:
         case HBASE_REGIONSERVER_ROLE:
            return 2;
         case HBASE_CLIENT_ROLE:
         case HADOOP_CLIENT_ROLE:
         case PIG_ROLE:
         case HIVE_ROLE:
         case HIVE_SERVER_ROLE:
            return 3;
         default:
            return MAX_DEPENDENCY;
         }
      } else {
         return MAX_DEPENDENCY;
      }
   }

   /**
    * Compare the order of roles according to their dependencies
    *
    *
    */
   public static class RoleComparactor implements Comparator<String> {
      @Override
      public int compare(String role1, String role2) {
         if (role1 == role2) {
            return 0;
         }

         int role1Dependency = HadoopRole.getDependency(role1);
         int role2Dependency = HadoopRole.getDependency(role2);
         if (role1Dependency == role2Dependency) {
            return 0;
         } else {
            return (role1Dependency > role2Dependency) ? 1 : -1;
         }
      }
   }
}