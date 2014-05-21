package com.vmware.bdd.apitypes;

import com.cloudera.api.model.ApiConfig;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiHost;
import com.google.gson.annotations.Expose;

import java.util.Arrays;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:50 PM
 */
public class CmNodeDef {

   @Expose
   private String nodeId;

   @Expose
   private String ipAddress;

   @Expose
   private String fqdn;

   @Expose
   private String rackId;

   @Expose
   private CmConfigDef[] configs;

   public String getNodeId() {
      return nodeId;
   }

   public void setNodeId(String nodeId) {
      this.nodeId = nodeId;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getFqdn() {
      return fqdn;
   }

   public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
   }

   public String getRackId() {
      return rackId;
   }

   public void setRackId(String rackId) {
      this.rackId = rackId;
   }

   public CmConfigDef[] getConfigs() {
      return configs;
   }

   public void setConfigs(CmConfigDef[] configs) {
      this.configs = configs;
   }

   public ApiHost toCmHost() {
      ApiHost apiHost = new ApiHost();
      apiHost.setHostId(this.nodeId);
      apiHost.setIpAddress(this.ipAddress);
      apiHost.setHostname(this.fqdn);
      apiHost.setRackId(this.rackId);
      apiHost.setConfig(new ApiConfigList());
      return apiHost;
   }
}
