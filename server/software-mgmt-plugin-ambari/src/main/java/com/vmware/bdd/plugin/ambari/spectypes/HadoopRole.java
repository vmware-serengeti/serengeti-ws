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
package com.vmware.bdd.plugin.ambari.spectypes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum HadoopRole {

   NAMENODE_ROLE("NAMENODE"),

   SECONDARY_NAMENODE_ROLE("SECONDARY_NAMENODE"),

   JOBTRACKER_ROLE("JOBTRACKER"),

   RESOURCEMANAGER_ROLE("RESOURCEMANAGER"),

   HISTORYSERVER_ROLE("HISTORYSERVER"),

   FALCON_SERVER_ROLE("FALCON_SERVER"),

   NAGIOS_SERVER_ROLE("NAGIOS_SERVER"),

   GANGLIA_SERVER_ROLE("GANGLIA_SERVER"),

   JOURNALNODE_ROLE("JOURNALNODE"),

   HBASE_MASTER_ROLE("HBASE_MASTER"),

   HBASE_REGIONSERVER_ROLE("HBASE_REGIONSERVER"),

   HBASE_CLIENT_ROLE("HBASE_CLIENT"),

   HIVE_METASTORE_ROLE("HIVE_METASTORE"),

   HIVE_SERVER_ROLE("HIVE_SERVER"),

   MYSQL_SERVER_ROLE("MYSQL_SERVER"),

   OOZIE_SERVER_ROLE("OOZIE_SERVER"),

   DRPC_SERVER_ROLE("DRPC_SERVER"),

   NIMBUS_ROLE("NIMBUS"),

   STORM_REST_API_ROLE("STORM_REST_API"),

   STORM_UI_SERVER_ROLE("STORM_UI_SERVER"),

   WEBHCAT_SERVER_ROLE("WEBHCAT_SERVER"),

   HUE_SERVER_ROLE("HUE_SERVER"),

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
      HadoopRole[] hbaseRoles = new HadoopRole[] { HBASE_MASTER_ROLE, HBASE_REGIONSERVER_ROLE, HBASE_CLIENT_ROLE};

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
            new HadoopRole[] { FALCON_SERVER_ROLE, GANGLIA_SERVER_ROLE,
            HBASE_MASTER_ROLE, JOURNALNODE_ROLE, NAMENODE_ROLE,
            SECONDARY_NAMENODE_ROLE, RESOURCEMANAGER_ROLE, JOBTRACKER_ROLE, HIVE_METASTORE_ROLE,
            HIVE_SERVER_ROLE, MYSQL_SERVER_ROLE, HISTORYSERVER_ROLE,
            NAGIOS_SERVER_ROLE, OOZIE_SERVER_ROLE, DRPC_SERVER_ROLE,
            NIMBUS_ROLE, STORM_REST_API_ROLE, STORM_UI_SERVER_ROLE,
            WEBHCAT_SERVER_ROLE,
            ZOOKEEPER_SERVER_ROLE, HUE_SERVER_ROLE };

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
