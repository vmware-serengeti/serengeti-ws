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


import java.util.EnumSet;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;

public enum GroupType {
   ZOOKEEPER_GROUP("zookeeper"),
   JOURNALNODE_GROUP("journalnode"),
   MASTER_GROUP("master"),
   HBASE_MASTER_GROUP("hbase_master"),
   MASTER_JOBTRACKER_GROUP("job_tracker"),
   WORKER_GROUP("worker"),
   CLIENT_GROUP("client"),
   MAPR_MASTER_GROUP("mapr_master"),
   MAPR_WORKER_GROUP("worker"),
   MAPR_CLIENT_GROUP("client"),
   MAPR_ZOOKEEPER_GROUP("mapr_zookeeper"),
   MAPR_MYSQL_SERVER("mapr_mysql_server");

   private String description;

   private GroupType(String description) {
      this.description = description;
   }

   public String toString() {
      return this.description;
   }

   public static GroupType fromString(String desc) {
      if (desc != null) {
         for (GroupType b : GroupType.values()) {
            if (desc.equalsIgnoreCase(b.toString())) {
               return b;
            }
         }
      }
      return null;
   }

   public DatastoreType getStorageEnumType() {
      switch (this) {
      case WORKER_GROUP:
         return DatastoreType.LOCAL;
      case MAPR_WORKER_GROUP:
         return DatastoreType.LOCAL;
      case MAPR_MASTER_GROUP:
         return DatastoreType.LOCAL;
      default:
         return DatastoreType.SHARED;
      }
   }

   public static GroupType fromHadoopRole(EnumSet<HadoopRole> roles) {
      if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE)) {
         return MASTER_GROUP;
      } else if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE)) {
         return MASTER_JOBTRACKER_GROUP;
      } else if (roles.contains(HadoopRole.HADOOP_DATANODE) ||
            roles.contains(HadoopRole.HADOOP_TASKTRACKER) ||
            roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE)) {
         return WORKER_GROUP;
      } else if(roles.contains(HadoopRole.HBASE_MASTER_ROLE)){
         return HBASE_MASTER_GROUP;
      } else if(roles.contains(HadoopRole.ZOOKEEPER_ROLE)){
         return ZOOKEEPER_GROUP;
      } else if (roles.contains(HadoopRole.HADOOP_JOURNALNODE_ROLE)) {
         return JOURNALNODE_GROUP;   
      } else if (roles.contains(HadoopRole.HADOOP_CLIENT_ROLE)) {
         return CLIENT_GROUP;
      } else if (roles.contains(HadoopRole.MAPR_CLDB_ROLE)  && roles.contains(HadoopRole.MAPR_JOBTRACKER_ROLE)
            && roles.contains(HadoopRole.MAPR_WEBSERVER_ROLE) && roles.contains(HadoopRole.MAPR_METRICS_ROLE)) {
         return MAPR_MASTER_GROUP;
      }  else if (roles.contains(HadoopRole.MAPR_TASKTRACKER_ROLE) && !roles.contains(HadoopRole.MAPR_JOBTRACKER_ROLE)) {
         return MAPR_WORKER_GROUP;
      } else if (roles.contains(HadoopRole.MAPR_MYSQL_SERVER_ROLE)) {
         return MAPR_MYSQL_SERVER;
      } else if (roles.contains(HadoopRole.MAPR_ZOOKEEPER_ROLE)) {
         return MAPR_ZOOKEEPER_GROUP;
      } else {
         return MAPR_CLIENT_GROUP;
      }
   }
}