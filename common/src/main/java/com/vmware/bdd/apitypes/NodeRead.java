/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.apitypes;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;

/**
 * Node get output
 */
public class NodeRead {
   @Expose
   private String name;

   @Expose
   @SerializedName("moid")
   private String moId;

   @Expose
   @SerializedName("rack")
   private String rack;

   @Expose
   @SerializedName("hostname")
   private String hostName;

   @Expose
   @SerializedName("ip_configs")
   private Map<NetTrafficType, List<IpConfigInfo>> ipConfigs;

   @Expose
   private String status;

   @Expose
   private String action;

   @Expose
   private List<String> roles;

   @Expose
   private long memory;

   @Expose
   private List<String> volumes;

   @Expose
   private int cpuNumber;

   public List<String> getVolumes() {
      return volumes;
   }

   public void setVolumes(List<String> volumes) {
      this.volumes = volumes;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getMoId() {
      return moId;
   }

   public void setMoId(String moId) {
      this.moId = moId;
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

   public String fetchMgtIp() {
      return fetchIpOf(NetTrafficType.MGT_NETWORK);
   }

   public String fetchHdfsIp() {
      return fetchIpOf(NetTrafficType.HDFS_NETWORK);
   }

   public String fetchMapredIp() {
      return fetchIpOf(NetTrafficType.MAPRED_NETWORK);
   }

   private String fetchIpOf(NetTrafficType type) {
      if (ipConfigs == null) {
         return null;
      }
      if (!ipConfigs.containsKey(type)) {
         if (type.equals(NetTrafficType.MGT_NETWORK)) {
            return null;
         } else {
            // in this case, by default use MGT_NETWORK
            return fetchIpOf(NetTrafficType.MGT_NETWORK);
         }
      }
      if (ipConfigs.get(type).get(0).getIpAddress().equals(Constants.NULL_IP)) {
         return null;
      }
      return ipConfigs.get(type).get(0).getIpAddress();
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }

   public long getMemory() {
      return memory;
   }

   public void setMemory(long m) {
      this.memory = m;
   }

   public int getCpuNumber() {
      return cpuNumber;
   }

   public void setCpuNumber(int cpu) {
      this.cpuNumber = cpu;
   }

}
