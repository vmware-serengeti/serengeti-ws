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
package com.vmware.bdd.plugin.ambari.api.model.stack2;

public enum ComponentName {

   FALCON_CLIENT(0),
   FALCON_SERVER(1),
   FLUME_HANDLER(2),
   GANGLIA_MONITOR(3),
   GANGLIA_SERVER(4),
   HBASE_CLIENT(5),
   HBASE_MASTER(6),
   HBASE_REGIONSERVER(7),
   HCAT(8),
   DATANODE(9),
   HDFS_CLIENT(10),
   JOURNALNODE(11),
   NAMENODE(12),
   SECONDARY_NAMENODE(13),
   ZKFC(14),
   HIVE_CLIENT(15),
   HIVE_METASTORE(16),
   HIVE_SERVER(17),
   MYSQL_SERVER(18),
   HISTORYSERVER(19),
   MAPREDUCE2_CLIENT(20),
   NAGIOS_SERVER(21),
   OOZIE_CLIENT(22),
   OOZIE_SERVER(23),
   PIG(24),
   SQOOP(25),
   DRPC_SERVER(26),
   NIMBUS(27),
   STORM_REST_API(28),
   STORM_UI_SERVER(29),
   SUPERVISOR(30),
   TEZ_CLIENT(31),
   WEBHCAT_SERVER(32),
   APP_TIMELINE_SERVER(33),
   NODEMANAGER(34),
   RESOURCEMANAGER(35),
   YARN_CLIENT(36),
   ZOOKEEPER_CLIENT(37),
   ZOOKEEPER_SERVER(38),
   HUE_SERVER(39),
   JOBTRACKER(40),
   MAPREDUCE_CLIENT(41),
   TASKTRACKER(42),
   MONITOR_WEBSERVER(43),
   GLUSTERFS_CLIENT(44),
   METRICS_COLLECTOR(45),
   METRICS_MONITOR(46),
   KAFKA_BROKER(47),
   KERBEROS_CLIENT(48),
   KNOX_GATEWAY(49),
   RANGER_ADMIN(50),
   RANGER_USERSYNC(51),
   SLIDER(52),
   SPARK_CLIENT(53),
   SPARK_JOBHISTORYSERVER(54);

   private final int name;

   private ComponentName(int name) {
      this.name = name;
   }

   public boolean isNamenode() {
      switch (ComponentName.values()[this.name]) {
      case NAMENODE:
         return true;
      default:
         return false;
      }
   }

   public boolean isSecondaryNamenode() {
      switch (ComponentName.values()[this.name]) {
      case SECONDARY_NAMENODE:
         return true;
      default:
         return false;
      }
   }

   public boolean isJournalnode() {
      switch (ComponentName.values()[this.name]) {
      case JOURNALNODE:
         return true;
      default:
         return false;
      }
   }

   public boolean isZkfc() {
      switch (ComponentName.values()[this.name]) {
      case ZKFC:
         return true;
      default:
         return false;
      }
   }

}
