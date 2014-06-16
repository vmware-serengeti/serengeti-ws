package com.vmware.bdd.software.mgmt.plugin.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.apitypes.IpConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;

/**
 * Node information, for instance, node ip, roles running on this node, data disks, etc.
 * @author line
 *
 */
public class NodeInfo implements Serializable {

   private static final long serialVersionUID = -6527422807735089543L;

   @Expose
   private String name;

   @Expose
   private String rack;

   @Expose
   private String hostName;

   @Expose
   private Map<NetTrafficType, List<IpConfigInfo>> ipConfigs;

   @Expose
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

   public List<String> getVolumes() {
      return volumes;
   }

   public void setVolumes(List<String> volumes) {
      this.volumes = volumes;
   }
}
