package com.vmware.bdd.apitypes;

/**
 * Author: Xiaoding Bian
 * Date: 5/21/14
 * Time: 3:31 PM
 */

public enum CmServiceType {

   // Cluster
   CLUSTER(null, "CLUSTER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Gateway
   GATEWAY(null, "GATEWAY", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // HDFS
   HDFS(CLUSTER, "HDFS", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_GATEWAY(HDFS, "HDFS_GATEWAY", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_NAMENODE(HDFS, "NAMENODE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_SECONDARY_NAMENODE(HDFS, "SECONDARYNAMENODE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED),

   HDFS_BALANCER(HDFS, "BALANCER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_FAILOVER_CONTROLLER(HDFS, "FAILOVERCONTROLLER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED),

   HDFS_JOURNALNODE(HDFS, "JOURNALNODE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_HTTP_FS(HDFS, "HTTPFS", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HDFS_DATANODE(HDFS, "DATANODE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // YARN
   YARN(CLUSTER, "YARN", CmServiceTypeRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_GATEWAY(YARN, "YARN_GATEWAY", CmServiceTypeRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_JOB_HISTORY(YARN, "JOBHISTORY", CmServiceTypeRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_RESOURCE_MANAGER(YARN, "RESOURCEMANAGER", CmServiceTypeRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   YARN_NODE_MANAGER(YARN, "NODEMANAGER", CmServiceTypeRepo.CDH, 6, CmServiceDef.VERSION_UNBOUNDED, 5, CmServiceDef.VERSION_UNBOUNDED),

   // MapReduce
   MAPREDUCE(CLUSTER, "MAPREDUCE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   MAPREDUCE_GATEWAY(MAPREDUCE, "MAPREDUCE_GATEWAY", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   MAPREDUCE_JOB_TRACKER(MAPREDUCE, "JOBTRACKER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   MAPREDUCE_TASK_TRACKER(MAPREDUCE, "TASKTRACKER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   // Spark
   SPARK(CLUSTER, "SPARK", CmServiceTypeRepo.SPARK, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SPARK_MASTER(SPARK, "SPARK_MASTER", CmServiceTypeRepo.SPARK, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   SPARK_WORKER(SPARK, "SPARK_WORKER", CmServiceTypeRepo.SPARK, 6, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Zookeeper
   ZOOKEEPER(CLUSTER, "ZOOKEEPER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   ZOOKEEPER_SERVER(ZOOKEEPER, "SERVER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // HBase
   HBASE(CLUSTER, "HBASE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_GATEWAY(HBASE, "HBASE_GATEWAY", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_MASTER(HBASE, "MASTER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_THRIFT_SERVER(HBASE, "HBASETHRIFTSERVER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_REST_SERVER(HBASE, "HBASERESTSERVER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   HBASE_REGIONSERVER(HBASE, "REGIONSERVER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Hive
   HIVE(CLUSTER, "HIVE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), HIVE_GATEWAY(HIVE, "HIVE_GATEWAY", CmServiceTypeRepo.CDH, 3,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED), HIVE_METASTORE(HIVE, "HIVEMETASTORE",
         CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED), HIVE_SERVER2(
         HIVE, "HIVESERVER2", CmServiceTypeRepo.CDH, 4, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), HIVE_HCATALOG(HIVE, "WEBHCAT", CmServiceTypeRepo.CDH, 4,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Solr
   SOLR(CLUSTER, "SOLR", CmServiceTypeRepo.SOLR, 4, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), SOLR_SERVER(SOLR, "SOLR_SERVER", CmServiceTypeRepo.SOLR, 4,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Solr Indexers
   SOLR_INDEXER(CLUSTER, "KS_INDEXER", CmServiceTypeRepo.SOLR, 5, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), SOLR_INDEXER_HBASE(SOLR_INDEXER, "HBASE_INDEXER",
         CmServiceTypeRepo.SOLR, 5, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Sqoop
   SQOOP(CLUSTER, "SQOOP", CmServiceTypeRepo.CDH, 4, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), SQOOP_SERVER(SQOOP, "SQOOP_SERVER", CmServiceTypeRepo.CDH, 4,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Oozie
   OOZIE(CLUSTER, "OOZIE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), OOZIE_SERVER(OOZIE, "OOZIE_SERVER", CmServiceTypeRepo.CDH, 3,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Impala
   IMPALA(CLUSTER, "IMPALA", CmServiceTypeRepo.IMPALA, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), IMPALA_STATE_STORE(IMPALA, "STATESTORE", CmServiceTypeRepo.IMPALA, 3,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED), IMPALA_DAEMON(IMPALA, "IMPALAD",
         CmServiceTypeRepo.IMPALA, 3, CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED), IMPALA_CATALOGSERVER(
         IMPALA, "CATALOGSERVER", CmServiceTypeRepo.IMPALA, 6, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED),

   // Flume
   FLUME(CLUSTER, "FLUME", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), FLUME_AGENT(FLUME, "AGENT", CmServiceTypeRepo.CDH, 3,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED),

   // Hue
   HUE(CLUSTER, "HUE", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED), HUE_SERVER(HUE, "HUE_SERVER", CmServiceTypeRepo.CDH, 3,
         CmServiceDef.VERSION_UNBOUNDED, 4, CmServiceDef.VERSION_UNBOUNDED), HUE_BEESWAX_SERVER(HUE,
         "BEESWAX_SERVER", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4, 4),

   // Client
   CLIENT(CLUSTER, "GATEWAY", CmServiceTypeRepo.CDH, 3, CmServiceDef.VERSION_UNBOUNDED, 4,
         CmServiceDef.VERSION_UNBOUNDED);

   private CmServiceType parent;
   private String id;
   private CmServiceTypeRepo respository;
   private int versionApiMin;
   private int versionApiMax;
   private int versionCdhMin;
   private int versionCdhMax;

   private CmServiceType(CmServiceType parent, String id, CmServiceTypeRepo respository,
         int versionApiMin, int versionApiMax, int versionCdhMin, int versionCdhMax) {
      this.parent = parent;
      this.id = id;
      this.respository = respository;
      this.versionApiMin = versionApiMin;
      this.versionApiMax = versionApiMax;
      this.versionCdhMin = versionCdhMin;
      this.versionCdhMax = versionCdhMax;
   }

   public CmServiceType getParent() {
      return parent;
   }

   public String getId() {
      return id;
   }

   public CmServiceTypeRepo getRepository() {
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

   public boolean isConcrete() {
      return getParent() != null && !getParent().equals(CLUSTER);
   }

   public static CmServiceType valueOfId(String id) {
      for (CmServiceType type : values()) {
         if (type.getId().equals(id)) {
            return type;
         }
      }
      throw new IllegalArgumentException("Unknown " + CmServiceType.class.getName() + " id [" + id + "]");
   }
}