package com.vmware.bdd.software.mgmt.plugin.model;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class NodeGroupInfo implements Serializable{

   private static final long serialVersionUID = 4443680719513071084L;

   @Expose
   private String name;

   @Expose
   private List<String> roles;

   @Expose
   private int instanceNum;

   @Expose
   private Map<String, Object> configuration;

   @Expose
   private List<NodeInfo> nodes;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }

   public int getInstanceNum() {
      return instanceNum;
   }

   public void setInstanceNum(int instanceNum) {
      this.instanceNum = instanceNum;
   }

   public Map<String, Object> getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Map<String, Object> configuration) {
      this.configuration = configuration;
   }

   public List<NodeInfo> getNodes() {
      return nodes;
   }

   public void setNodes(List<NodeInfo> nodes) {
      this.nodes = nodes;
   }
}
