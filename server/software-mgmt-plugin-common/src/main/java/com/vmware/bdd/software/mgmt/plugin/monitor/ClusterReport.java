package com.vmware.bdd.software.mgmt.plugin.monitor;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;

import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 7:34 PM
 */
public class ClusterReport {

   private String name;

   private String action;

   private boolean success;

   private boolean finished;

   private boolean seenByCleaner; // if cleaner has seen this report last round, then clean it this round

   private Map<String, GroupReport> groups; // groupName -> groupReport

   public ClusterReport (ClusterBlueprint clusterBlueprint) {
      // init
   }

}
