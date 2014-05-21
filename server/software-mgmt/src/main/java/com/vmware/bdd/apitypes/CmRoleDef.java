package com.vmware.bdd.apitypes;

import com.google.gson.annotations.Expose;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:56 PM
 */
public class CmRoleDef {
   @Expose
   private String name;

   @Expose
   private String type;

   @Expose
   private String nodeRef; // TODO, relate to node.nodeId

   @Expose
   private CmConfigDef[] configs;

   public CmConfigDef[] getConfigs() {
      return configs;
   }

   public void setConfigs(CmConfigDef[] configs) {
      this.configs = configs;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getNodeRef() {
      return nodeRef;
   }

   public void setNodeRef(String nodeRef) {
      this.nodeRef = nodeRef;
   }

}
