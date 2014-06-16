/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public enum HadoopRole {
   //NOTE: when you add a new role, please put it into an appropriate position
   //based on their dependencies. The more dependent, the latter position
   //0 dependency
   ZOOKEEPER_ROLE("zookeeper"),
   HADOOP_JOURNALNODE_ROLE("hadoop_journalnode"), //for Hadoop 2.x Namenode HA
   //1 dependency
   HADOOP_NAMENODE_ROLE("hadoop_namenode"),
   //2 dependency
   HBASE_MASTER_ROLE("hbase_master"),
   TEMPFS_SERVER_ROLE("tempfs_server"),
   HADOOP_JOBTRACKER_ROLE("hadoop_jobtracker"),
   HADOOP_RESOURCEMANAGER_ROLE("hadoop_resourcemanager"),
   HADOOP_DATANODE("hadoop_datanode"),
   //3 dependencies
   TEMPFS_CLIENT_ROLE("tempfs_client"),
   HADOOP_TASKTRACKER("hadoop_tasktracker"),
   HADOOP_NODEMANAGER_ROLE("hadoop_nodemanager"),
   HBASE_REGIONSERVER_ROLE("hbase_regionserver"),
   //4 dependencies
   HADOOP_CLIENT_ROLE("hadoop_client"),
   HBASE_CLIENT_ROLE("hbase_client"),
   PIG_ROLE("pig"),
   HIVE_ROLE("hive"),
   HIVE_SERVER_ROLE("hive_server"),
   // mapr
   MAPR_ROLE("mapr"),
   MAPR_ZOOKEEPER_ROLE("mapr_zookeeper"),
   MAPR_CLDB_ROLE("mapr_cldb"),
   MAPR_JOBTRACKER_ROLE("mapr_jobtracker"),
   MAPR_HBASE_MASTER_ROLE("mapr_hbase_master"),
   MAPR_NFS_ROLE("mapr_nfs"),
   MAPR_WEBSERVER_ROLE("mapr_webserver"),
   MAPR_FILESERVER_ROLE("mapr_fileserver"),
   MAPR_TASKTRACKER_ROLE("mapr_tasktracker"),
   MAPR_HBASE_REGIONSERVER_ROLE("mapr_hbase_regionserver"),
   MAPR_METRICS_ROLE("mapr_metrics"),
   MAPR_PIG_ROLE("mapr_pig"),
   MAPR_HIVE_ROLE("mapr_hive"),
   MAPR_HIVE_SERVER_ROLE("mapr_hive_server"),
   MAPR_CLIENT_ROLE("mapr_client"),
   MAPR_HBASE_CLIENT_ROLE("mapr_hbase_client"),
   MAPR_MYSQL_SERVER_ROLE("mapr_mysql_server"),
   MAPR_MYSQL_CLIENT_ROLE("mapr_mysql_client"),

   // put other predefined roles here or above

   // the last role is the user customized role
   CUSTOMIZED_ROLE("customized_role");

   static final Logger logger = Logger.getLogger(HadoopRole.class);

   private String description;

   private HadoopRole(String description) {
      this.description = description;
   }

   public String toString() {
      return this.description;
   }

   public boolean shouldRunAfterHDFS() {
      if (this.equals(HadoopRole.HBASE_CLIENT_ROLE)
            || this.equals(HadoopRole.HBASE_MASTER_ROLE)
            || this.equals(HadoopRole.HBASE_REGIONSERVER_ROLE)) {
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
         return HadoopRole.CUSTOMIZED_ROLE;
      }
      return null;
   }

   /**
    * If a Role exists in Chef Server, but not predefined in HadoopRole enum, it's a customized role.
    */
   public static boolean isCustomizedRole(String role) {
      return HadoopRole.fromString(role) == HadoopRole.CUSTOMIZED_ROLE;
   }

   /**
    * Compare the order of roles according to their dependencies(the enum ordial
    * is very important here)
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
            return (role1Dependency.ordinal() > role2Dependency.ordinal()) ? 1
                  : -1;
         }
      }
   }

   private static Set<String> mgmtRoleDesc;

   static {
      HadoopRole[] mgmtRoles =
            new HadoopRole[] { ZOOKEEPER_ROLE, HADOOP_JOURNALNODE_ROLE,
                  HADOOP_NAMENODE_ROLE, HBASE_MASTER_ROLE,
                  HADOOP_JOBTRACKER_ROLE, HIVE_SERVER_ROLE, MAPR_CLDB_ROLE,
                  MAPR_JOBTRACKER_ROLE, MAPR_ZOOKEEPER_ROLE,
                  MAPR_HBASE_MASTER_ROLE, MAPR_HIVE_SERVER_ROLE,
                  HADOOP_RESOURCEMANAGER_ROLE };

      mgmtRoleDesc = new HashSet<String>(mgmtRoles.length);

      for (HadoopRole role : mgmtRoles) {
         mgmtRoleDesc.add(role.description);
      }
   }

   public static boolean hasMgmtRole(List<String> roles) {
      for (String role : roles) {
         if (mgmtRoleDesc.contains(role))
            return true;
      }

      return false;
   }

   private static Set<String> hbaseRoleDesc;

   static {
      HadoopRole[] hbaseRoles =
            new HadoopRole[] { HBASE_MASTER_ROLE, HBASE_REGIONSERVER_ROLE, HBASE_CLIENT_ROLE,
                               MAPR_HBASE_MASTER_ROLE, MAPR_HBASE_REGIONSERVER_ROLE, MAPR_HBASE_CLIENT_ROLE };

      hbaseRoleDesc = new HashSet<String>(hbaseRoles.length);

      for (HadoopRole role : hbaseRoles) {
         hbaseRoleDesc.add(role.description);
      }
   }

   public static boolean hasHBaseRole(List<String> roles) {
      for (String role : roles) {
         if (hbaseRoleDesc.contains(role))
            return true;
      }
      return false;
   }
}
