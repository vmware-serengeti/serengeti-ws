package com.vmware.bdd.software.mgmt.plugin.model;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ClusterBlueprint implements Serializable{

   private static final long serialVersionUID = 5545914268769234047L;

   @Expose
   private String name;

   @Expose
   private int instanceNum;

   @Expose
   private List<NodeGroupInfo> nodeGroups;

   @Expose
   private Map<String, Object> configuration;

   @Expose
   private HadoopStack hadoopStack;
   private String externalHDFS;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getInstanceNum() {
      return instanceNum;
   }

   public void setInstanceNum(int instanceNum) {
      this.instanceNum = instanceNum;
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

   public String getExternalHDFS() {
      return externalHDFS;
   }

   public void setExternalHDFS(String externalHDFS) {
      this.externalHDFS = externalHDFS;
   }
}
