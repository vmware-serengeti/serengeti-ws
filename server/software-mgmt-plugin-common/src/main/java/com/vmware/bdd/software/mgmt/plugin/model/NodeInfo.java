package com.vmware.bdd.software.mgmt.plugin.model;

import java.util.List;
import java.util.Map;

import com.vmware.bdd.apitypes.IpConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;

public class NodeInfo {
   private String name;
   private String rack;
   private String hostName;
   private Map<NetTrafficType, List<IpConfigInfo>> ipConfigs;
   private List<String> roles;
   private List<String> volumes;
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getRack() {
      return rack;
   }
   public void setRack(String rack) {
      this.rack = rack;
   }
   public String getHostName() {
      return hostName;
   }
   public void setHostName(String hostName) {
      this.hostName = hostName;
   }
   public Map<NetTrafficType, List<IpConfigInfo>> getIpConfigs() {
      return ipConfigs;
   }
   public void setIpConfigs(Map<NetTrafficType, List<IpConfigInfo>> ipConfigs) {
      this.ipConfigs = ipConfigs;
   }
   public List<String> getRoles() {
      return roles;
   }
   public void setRoles(List<String> roles) {
      this.roles = roles;
   }
   public List<String> getVolumes() {
      return volumes;
   }
   public void setVolumes(List<String> volumes) {
      this.volumes = volumes;
   }
}
