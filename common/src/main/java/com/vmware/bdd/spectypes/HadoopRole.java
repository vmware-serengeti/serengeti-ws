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
   //NOTE: when you add a new role, please put it into an appropriate position
   //based on their dependencies. The more dependent, the latter position
   //0 dependency
   ZOOKEEPER_ROLE("zookeeper"),
   HADOOP_JOURNALNODE_ROLE("hadoop_journalnode"), //for cdh4 namenode ha
   //1 dependency
   HADOOP_NAMENODE_ROLE("hadoop_namenode"),
   //2 dependency
   HBASE_MASTER_ROLE("hbase_master"),
   TEMPFS_SERVER_ROLE("tempfs_server"),
   HADOOP_DATANODE("hadoop_datanode"),
   HADOOP_JOBTRACKER_ROLE("hadoop_jobtracker"),
   //3 dependencies
   TEMPFS_CLIENT_ROLE("tempfs_client"),
   HADOOP_TASKTRACKER("hadoop_tasktracker"),
   HBASE_REGIONSERVER_ROLE("hbase_regionserver"),
   //4 dependencies
   HADOOP_CLIENT_ROLE("hadoop_client"),
   HBASE_CLIENT_ROLE("hbase_client"),
   PIG_ROLE("pig"), 
   HIVE_ROLE("hive"),
   HIVE_SERVER_ROLE("hive_server"),
   // mapr
   MAPR_ROLE("mapr"),
   MAPR_CLDB_ROLE("mapr_cldb"),
   MAPR_JOBTRACKER_ROLE("mapr_jobtracker"),
   MAPR_NFS_ROLE("mapr_nfs"),
   MAPR_WEBSERVER_ROLE("mapr_webserver"),
   MAPR_FILESERVER_ROLE("mapr_fileserver"),
   MAPR_TASKTRACKER_ROLE("mapr_tasktracker"),
   MAPR_METRICS_ROLE("mapr_metrics"),
   MAPR_PIG_ROLE("mapr_pig"),
   MAPR_HIVE_ROLE("mapr_hive"),
   MAPR_HIVE_SERVER_ROLE("mapr_hive_server"),
   MAPR_ZOOKEEPER_ROLE("mapr_zookeeper"),
   MAPR_CLIENT("mapr_client"),
   MAPR_MYSQL_SERVER_ROLE("mapr_mysql_server"),
   MAPR_MYSQL_CLIENT_ROLE("mapr_mysql_client");

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

   /**
    * Compare the order of roles according to their dependencies(the enum ordial is very important here)
    *
    *
    */
   public static class RoleComparactor implements Comparator<String> {
      @Override
      public int compare(String role1, String role2) {
         if (role1 == role2) {
            return 0;
         }

         HadoopRole role1Dependency = HadoopRole.fromString(role1);
         HadoopRole role2Dependency = HadoopRole.fromString(role2);
         if (role1Dependency == role2Dependency) {
            return 0;
         }

         //null elements will be sorted behind the list
         if (role1Dependency == null) {
            return 1;
         } else if (role2Dependency == null) {
            return -1;
         }

         if (role1Dependency.ordinal() == role2Dependency.ordinal()) {
            return 0;
         } else {
            return (role1Dependency.ordinal() > role2Dependency.ordinal()) ? 1 : -1;
         }
      }
   }
}
