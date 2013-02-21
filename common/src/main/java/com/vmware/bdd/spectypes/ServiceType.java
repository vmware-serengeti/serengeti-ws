/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
import java.util.HashSet;
import java.util.Set;

public enum ServiceType {
   HDFS(EnumSet.of(HadoopRole.HADOOP_NAMENODE_ROLE, HadoopRole.HADOOP_DATANODE)),
   MAPRED(EnumSet.of(HadoopRole.HADOOP_JOBTRACKER_ROLE, HadoopRole.HADOOP_TASKTRACKER)),
   HBASE(EnumSet.of(HadoopRole.HBASE_MASTER_ROLE, HadoopRole.HBASE_REGIONSERVER_ROLE)),
   ZOOKEEPER(EnumSet.of(HadoopRole.ZOOKEEPER_ROLE)),
   HADOOP_CLIENT(EnumSet.of(HadoopRole.HADOOP_CLIENT_ROLE)),
   PIG(EnumSet.of(HadoopRole.PIG_ROLE)),
   HIVE_SERVER(EnumSet.of(HadoopRole.HIVE_SERVER_ROLE)), 
   HIVE(EnumSet.of(HadoopRole.HIVE_ROLE)),
   HBASE_CLIENT(EnumSet.of(HadoopRole.HBASE_CLIENT_ROLE));

   private EnumSet<HadoopRole> roles;
   private ServiceType(EnumSet<HadoopRole> roles) {
      this.roles = roles;
   }

   public EnumSet<HadoopRole> getRoles() {
      return roles;
   }

   public Set<String> getRolesInString() {
      Set<String> strings = new HashSet<String>();
      for (HadoopRole role : roles) {
         strings.add(role.toString());
      }
      return strings;
   }

   public EnumSet<ServiceType> depend() {
      switch(this) {
      case MAPRED:
         return EnumSet.of(HDFS);
      case HBASE:
         return EnumSet.of(HDFS, ZOOKEEPER);
      case HADOOP_CLIENT:
         return EnumSet.of(HDFS);
      case HIVE_SERVER:
      case HIVE:
      case PIG:
         return EnumSet.of(MAPRED);
      case HBASE_CLIENT:
         return EnumSet.of(HBASE);
      default:
         return null;
      }
   }
}
