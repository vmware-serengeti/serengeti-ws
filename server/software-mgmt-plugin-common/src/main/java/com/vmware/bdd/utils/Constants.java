package com.vmware.bdd.utils;

public class Constants {
   public static final String WRONG_NUM_OF_JOBTRACKER =
      "The number of JobTrackers must be one.";
   public static final String WRONG_NUM_OF_RESOURCEMANAGER =
      "The number of ResourceManagers cannot exceed one.";
   public static final String WRONG_NUM_OF_ZOOKEEPER =
      "The number of ZooKeepers cannot be less than three.";
   public static final String ODD_NUM_OF_ZOOKEEPER =
      "The number of ZooKeepers is recommended to be an odd number.";
   public static final String WORKER_CLIENT_HA_FLAG =
      "The worker or client node group has vSphere High Availability enabled.";
   public static final String REGISONSERVER_DATANODE_SEPERATION =
      "RegionServer and DataNode are separated, which might degrade performance.";
   public static final String WRONG_NUM_OF_JOURNALNODE =
      "The number of journal nodes must be three or more.";
   public static final String ODD_NUM_OF_JOURNALNODE =
      "The number of journal nodes is recommended to be an odd number.";
   public static final String DATA_CLIENT_NODE_JOURNALNODE_COEXIST =
      "Journal nodes cannot be on data nodes or client nodes.";
   public static final String NAMENODE_AUTO_FAILOVER_ZOOKEEPER =
      "To support vSphere High Availability on name nodes, you must configure a ZooKeeper group.";
   public static final String CURRENT_DISTRO_CAN_NOT_SUPPORT_HDFS2 =
      "The current Hadoop distribution does not support HDFS2 High Availability or federation.";
   public static final String WRONG_NUM_OF_NODEGROUPS =
      "Only one node group can include JobTracker, ZooKeeper or HMaster.";
   public static final String NO_NAMENODE_HA =
      "NameNode High Availability is not enabled, but journal nodes appear.";
   public static final String DEFAULT_VENDOR = "Apache";
   public static final String CDH_VENDOR = "CDH";
   public static final String GPHD_VENDOR = "GPHD";
   public static final String HDP_VENDOR = "HDP";
   public static final String MAPR_VENDOR = "MAPR";
   public static final String PHD_VENDOR = "PHD";
   public static final String INTEL_VENDOR = "INTEL";
   public static final String BIGTOP_VENDOR = "BIGTOP";

}
