/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.spectypes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum HadoopRole {

   ACCUMULO16_GARBAGE_SERVER_ROLE("ACCUMULO16_GARBAGE_SERVER"),

   ACCUMULO16_TABLET_SERVER_ROLE("ACCUMULO16_TABLET_SERVER"),

   ACCUMULO16_MASTER_ROLE("ACCUMULO16_MASTER"),

   HDFS_NAMENODE_ROLE("HDFS_NAMENODE"),

   HDFS_SECONDARY_NAMENODE_ROLE("HDFS_SECONDARY_NAMENODE"),

   MAPREDUCE_JOBTRACKER_ROLE("MAPREDUCE_JOBTRACKER"),

   YARN_RESOURCE_MANAGER_ROLE("YARN_RESOURCE_MANAGER"),

   HDFS_JOURNALNODE_ROLE("HDFS_JOURNALNODE"),

   HBASE_MASTER_ROLE("HBASE_MASTER"),

   HBASE_REGION_SERVER_ROLE("HBASE_REGION_SERVER"),

   HBASE_REST_SERVER_ROLE("HBASE_REST_SERVER"),

   HBASE_THRIFT_SERVER_ROLE("HBASE_THRIFT_SERVER"),

   HIVE_METASTORE_ROLE("HIVE_METASTORE"),

   HIVE_SERVER2_ROLE("HIVE_SERVER2"),

   HUE_SERVER_ROLE("HUE_SERVER"),

   IMPALA_CATALOG_SERVER_ROLE("IMPALA_CATALOG_SERVER"),

   OOZIE_SERVER_ROLE("OOZIE_SERVER"),

   SENTRY_SERVER_ROLE("SENTRY_SERVER"),

   SOLR_SERVER_ROLE("SOLR_SERVER"),

   SPARK_HISTORY_SERVER_ROLE("SPARK_HISTORY_SERVER"),

   SPARK_MASTER_ROLE("SPARK_MASTER"),

   SQOOP_SERVER_ROLE("SQOOP_SERVER"),

   ZOOKEEPER_SERVER_ROLE("ZOOKEEPER_SERVER");

   private String description;

   private HadoopRole(String description) {
      this.description = description;
   }

   public String toString() {
      return this.description;
   }

   private static Set<String> hbaseRoleDesc;

   static {
      HadoopRole[] hbaseRoles = new HadoopRole[] { HBASE_MASTER_ROLE, HBASE_REGION_SERVER_ROLE};

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

   private static Set<String> mgmtRoleDesc;

   static {
      HadoopRole[] mgmtRoles =
            new HadoopRole[] { HDFS_NAMENODE_ROLE,
            HDFS_SECONDARY_NAMENODE_ROLE, MAPREDUCE_JOBTRACKER_ROLE,
            YARN_RESOURCE_MANAGER_ROLE, ZOOKEEPER_SERVER_ROLE,
            HBASE_MASTER_ROLE, HDFS_JOURNALNODE_ROLE,
            ACCUMULO16_GARBAGE_SERVER_ROLE,
            ACCUMULO16_TABLET_SERVER_ROLE, ACCUMULO16_MASTER_ROLE,
            HBASE_REST_SERVER_ROLE,
            HBASE_THRIFT_SERVER_ROLE, HIVE_METASTORE_ROLE,
            HIVE_SERVER2_ROLE, HUE_SERVER_ROLE,
            IMPALA_CATALOG_SERVER_ROLE, OOZIE_SERVER_ROLE,
            SENTRY_SERVER_ROLE, SOLR_SERVER_ROLE, SPARK_MASTER_ROLE,
            SPARK_HISTORY_SERVER_ROLE, SQOOP_SERVER_ROLE };

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

   public static HadoopRole fromString(String desc) {
      if (desc != null) {
         for (HadoopRole b : HadoopRole.values()) {
            if (desc.equalsIgnoreCase(b.toString())) {
               return b;
            }
         }
         return null;
      }
      return null;
   }

   public static EnumSet<HadoopRole> getEnumRoles(List<String> roles) {
      EnumSet<HadoopRole> enumRoles = EnumSet.noneOf(HadoopRole.class);
      for (String role : roles) {
         HadoopRole configuredRole = HadoopRole.fromString(role);
         if (configuredRole != null) {
            enumRoles.add(configuredRole);
         }
      }
      return enumRoles;
   }
}
