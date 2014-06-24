package com.vmware.bdd.software.mgmt.plugin.monitor;

import java.util.Map;

import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;

/**
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 7:34 PM
 */
public class ClusterReport {

   private ClusterStatus status;
   private String name;

   private String action;

   private boolean success;

   private boolean finished;

   private Map<String, GroupReport> groups; // groupName -> groupReport

   public ClusterReport (ClusterBlueprint clusterBlueprint) {
      // init
   }

}
