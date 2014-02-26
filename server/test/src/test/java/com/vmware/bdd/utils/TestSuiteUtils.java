package com.vmware.bdd.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.vmware.bdd.manager.ChefServerManager;

public class TestSuiteUtils {

   /**
    * Must call this to mock roles in chef server before calling createCluster()
    */
   public static void mockChefServerRoles() {
      List<String> rolesList = Arrays.asList("hadoop", "hadoop_client", "hadoop_datanode", "hadoop_initial_bootstrap",
            "hadoop_jobtracker", "hadoop_journalnode", "hadoop_master", "hadoop_namenode", "hadoop_nodemanager",
            "hadoop_resourcemanager", "hadoop_secondarynamenode", "hadoop_tasktracker", "hadoop_worker",
            "hawq-cluster", "hawq-master-facet", "hawq-worker-facet", "hawq_master", "hawq_worker", "hbase_client",
            "hbase_master", "hbase_regionserver", "hive", "hive_server", "mapr", "mapr_cldb", "mapr_client",
            "mapr_fileserver", "mapr_hbase_client", "mapr_hbase_master", "mapr_hbase_regionserver", "mapr_hive",
            "mapr_hive_server", "mapr_jobtracker", "mapr_metrics", "mapr_mysql_server", "mapr_nfs", "mapr_pig",
            "mapr_tasktracker", "mapr_webserver", "mapr_zookeeper", "pig", "postgresql_server", "tempfs_client",
            "tempfs_server", "zookeeper");
      HashSet<String> roles = new HashSet<String>();
      roles.addAll(rolesList);
      ChefServerManager.setAllRoles(roles);
   }
}
