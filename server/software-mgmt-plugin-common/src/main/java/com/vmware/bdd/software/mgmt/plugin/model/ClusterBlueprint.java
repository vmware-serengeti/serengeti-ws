package com.vmware.bdd.software.mgmt.plugin.model;

import java.util.List;
import java.util.Map;

public class ClusterBlueprint {
   private String name;
   private Map<String, String> hostToRackMap;
   private List<NodeGroupInfo> nodeGroups;
   private Map<String, Object> configuration;
   private HadoopStack hadoopStack;
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public Map<String, String> getHostToRackMap() {
      return hostToRackMap;
   }
   public void setHostToRackMap(Map<String, String> hostToRackMap) {
      this.hostToRackMap = hostToRackMap;
   }
   public List<NodeGroupInfo> getNodeGroups() {
      return nodeGroups;
   }
   public void setNodeGroups(List<NodeGroupInfo> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }
   public Map<String, Object> getConfiguration() {
      return configuration;
   }
   public void setConfiguration(Map<String, Object> configuration) {
      this.configuration = configuration;
   }
   public HadoopStack getHadoopStack() {
      return hadoopStack;
   }
   public void setHadoopStack(HadoopStack hadoopStack) {
      this.hadoopStack = hadoopStack;
   }
}
