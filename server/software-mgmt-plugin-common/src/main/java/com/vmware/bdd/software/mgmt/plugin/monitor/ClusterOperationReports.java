package com.vmware.bdd.software.mgmt.plugin.monitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * For all 
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 7:50 PM
 */

public class ClusterOperationReports extends Observable {

   private final int CLEAN_INTERVAL = 12 * 60 * 60 * 1000; // 12 hours

   // map operation ID to report info, operation ID should be: clusterName + "_" + Operation + "_" + LaunchTime.
   // i.e, cluster01_create_201405011315, do not use clusterName as ID to avoid potential concurrency issues in future.
   private final Map<String, ClusterReport> reports = new HashMap<String, ClusterReport>();


   /**
    * clean up in-memory reports info that has expired.
    */
   private class Cleaner extends Thread {
      @Override
      public void run() {

      }
   }

   /**
    * set clone=flase for updating, clone=true for querying
    * Note: synchronization
    * @param opsId
    * @param clone
    * @return
    */
   public ClusterReport getClusterReport(String opsId, boolean clone) {
      return null;
   }

   public GroupReport getGroupReport(String opsId, String groupName, boolean clone) {
      return null;
   }

   public NodeReport getNodeReport(String opsId, String nodeName, boolean clone) {
      return null;
   }

   /**
    * find all opsIds of given cluster name, i.e, for remove
    * @param clusterName
    * @return
    */
   public List<String> opsIdsOfCluster(String clusterName) {
      return null;
   }

   public String dump() {
      // dump all reports info;
      return null;
   }

}
