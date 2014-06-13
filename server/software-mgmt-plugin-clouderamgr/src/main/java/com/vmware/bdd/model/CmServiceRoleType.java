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
package com.vmware.bdd.model;

import com.vmware.bdd.model.support.AvailableParcelRepo;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 3:31 PM
 */

public enum CmServiceRoleType {

   // Cluster
   CLUSTER(null, "CLUSTER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Gateway
   GATEWAY(null, "GATEWAY", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // HDFS
   HDFS(CLUSTER, "HDFS", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_GATEWAY(HDFS, "GATEWAY", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_NAMENODE(HDFS, "NAMENODE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_SECONDARY_NAMENODE(HDFS, "SECONDARYNAMENODE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_BALANCER(HDFS, "BALANCER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_FAILOVER_CONTROLLER(HDFS, "FAILOVERCONTROLLER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_JOURNALNODE(HDFS, "JOURNALNODE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_HTTP_FS(HDFS, "HTTPFS", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_DATANODE(HDFS, "DATANODE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // YARN
   YARN(CLUSTER, "YARN", AvailableParcelRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_GATEWAY(YARN, "YARN_GATEWAY", AvailableParcelRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_JOB_HISTORY(YARN, "JOBHISTORY", AvailableParcelRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_RESOURCE_MANAGER(YARN, "RESOURCEMANAGER", AvailableParcelRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_NODE_MANAGER(YARN, "NODEMANAGER", AvailableParcelRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   // MapReduce
   MAPREDUCE(CLUSTER, "MAPREDUCE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   MAPREDUCE_GATEWAY(MAPREDUCE, "MAPREDUCE_GATEWAY", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   MAPREDUCE_JOB_TRACKER(MAPREDUCE, "JOBTRACKER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   MAPREDUCE_TASK_TRACKER(MAPREDUCE, "TASKTRACKER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   // Spark
   SPARK(CLUSTER, "SPARK", AvailableParcelRepo.SPARK, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SPARK_MASTER(SPARK, "SPARK_MASTER", AvailableParcelRepo.SPARK, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SPARK_WORKER(SPARK, "SPARK_WORKER", AvailableParcelRepo.SPARK, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Zookeeper
   ZOOKEEPER(CLUSTER, "ZOOKEEPER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   ZOOKEEPER_SERVER(ZOOKEEPER, "SERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // HBase
   HBASE(CLUSTER, "HBASE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_GATEWAY(HBASE, "HBASE_GATEWAY", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_MASTER(HBASE, "MASTER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_THRIFT_SERVER(HBASE, "HBASETHRIFTSERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_REST_SERVER(HBASE, "HBASERESTSERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_REGIONSERVER(HBASE, "REGIONSERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Hive
   HIVE(CLUSTER, "HIVE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HIVE_GATEWAY(HIVE, "HIVE_GATEWAY", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HIVE_METASTORE(HIVE, "HIVEMETASTORE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HIVE_SERVER2(HIVE, "HIVESERVER2", AvailableParcelRepo.CDH, 4, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HIVE_HCATALOG(HIVE, "WEBHCAT", AvailableParcelRepo.CDH, 4, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Solr
   SOLR(CLUSTER, "SOLR", AvailableParcelRepo.SOLR, 4, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SOLR_SERVER(SOLR, "SOLR_SERVER", AvailableParcelRepo.SOLR, 4, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SOLR_INDEXER(CLUSTER, "KS_INDEXER", AvailableParcelRepo.SOLR, 5, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SOLR_INDEXER_HBASE(SOLR_INDEXER, "HBASE_INDEXER", AvailableParcelRepo.SOLR, 5, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Sqoop
   SQOOP(CLUSTER, "SQOOP", AvailableParcelRepo.CDH, 4, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SQOOP_SERVER(SQOOP, "SQOOP_SERVER", AvailableParcelRepo.CDH, 4, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Oozie
   OOZIE(CLUSTER, "OOZIE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   OOZIE_SERVER(OOZIE, "OOZIE_SERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Impala
   IMPALA(CLUSTER, "IMPALA", AvailableParcelRepo.IMPALA, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   IMPALA_STATE_STORE(IMPALA, "STATESTORE", AvailableParcelRepo.IMPALA, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   IMPALA_DAEMON(IMPALA, "IMPALAD", AvailableParcelRepo.IMPALA, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   IMPALA_CATALOGSERVER(IMPALA, "CATALOGSERVER", AvailableParcelRepo.IMPALA, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Flume
   FLUME(CLUSTER, "FLUME", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   FLUME_AGENT(FLUME, "AGENT", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Hue
   HUE(CLUSTER, "HUE", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HUE_SERVER(HUE, "HUE_SERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HUE_BEESWAX_SERVER(HUE, "BEESWAX_SERVER", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   // Client
   CLIENT(CLUSTER, "GATEWAY", AvailableParcelRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED);

   private CmServiceRoleType parent;
   private String id;
   private AvailableParcelRepo respository;
   private int versionApiMin;
   private int versionApiMax;
   private int versionCdhMin;
   private int versionCdhMax;

   private CmServiceRoleType(CmServiceRoleType parent, String id, AvailableParcelRepo respository,
         int versionApiMin, int versionApiMax, int versionCdhMin, int versionCdhMax) {
      this.parent = parent;
      this.id = id;
      this.respository = respository;
      this.versionApiMin = versionApiMin;
      this.versionApiMax = versionApiMax;
      this.versionCdhMin = versionCdhMin;
      this.versionCdhMax = versionCdhMax;
   }

   public CmServiceRoleType getParent() {
      return parent;
   }

   public String getId() {
      return id;
   }

   public AvailableParcelRepo getRepository() {
      return respository;
   }

   public int getVersionApiMin() {
      return versionApiMin;
   }

   public int getVersionApiMax() {
      return versionApiMax;
   }

   public int getVersionCdhMin() {
      return versionCdhMin;
   }

   public int getVersionCdhMax() {
      return versionCdhMax;
   }

   public boolean isValid(int versionApi, int versionCdh) {
      return (versionApi < 0 || (versionApiMin < 0 || versionApi >= versionApiMin)
            && (versionApiMax < 0 || versionApi <= versionApiMax))
            && (versionCdh < 0 || (versionCdhMin < 0 || versionCdh >= versionCdhMin)
            && (versionCdhMax < 0 || versionCdh <= versionCdhMax));
   }

   public static CmServiceRoleType valueOfId(String id) {
      for (CmServiceRoleType type : values()) {
         if (type.getId().equalsIgnoreCase(id)) {
            return type;
         }
      }
      throw new IllegalArgumentException("Unknown " + CmServiceRoleType.class.getName() + " id [" + id + "]");
   }

   public boolean isService() {
      return getParent() != null && !getParent().equals(CLUSTER);
   }

   public boolean isRole() {
      return getParent() != null && getParent().getParent() != null && getParent().getParent().equals(CLUSTER);
   }

   public static boolean isService(String id) {
      return valueOf(id).isService();
   }

   public static boolean isRole(String id) {
      return valueOf(id).isRole();
   }

   public static String serviceOfRole(String id) {
      return valueOfId(id).serviceOfRole();
   }

   public String serviceOfRole() {
      if (!isRole()) {
         throw new IllegalArgumentException("not a role");
      }
      return getParent().getId();
   }

   public static List<String> allServices() {
      List<String> allServices = new ArrayList<String>();
      for (CmServiceRoleType item : CmServiceRoleType.values()) {
         if (item.isService()) {
            allServices.add(item.getId());
         }
      }
      return allServices;
   }

   public static List<String> allRoles() {
      List<String> allRoles = new ArrayList<String>();
      for (CmServiceRoleType item : CmServiceRoleType.values()) {
         if (item.isRole()) {
            allRoles.add(item.getId());
         }
      }
      return allRoles;
   }

   public static List<String> allRolesOfService(String service) {
      List<String> allRoles = new ArrayList<String>();
      for (CmServiceRoleType item : CmServiceRoleType.values()) {
         if (item.isRole() && item.serviceOfRole().equalsIgnoreCase(service)) {
            allRoles.add(item.getId());
         }
      }
      return allRoles;
   }

}