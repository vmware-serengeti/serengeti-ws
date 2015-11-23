/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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
   private static final Logger logger = Logger.getLogger(NodeInfo.class);

   @Expose
   private String name;

   @Expose
   private String rack;

   @Expose
   private String hostname;

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

   public String getHostname() {
      if (hostname == null || hostname.isEmpty()
            || "localhost".equalsIgnoreCase(hostname)
            || "localhost.localdomain".equalsIgnoreCase(hostname)) {
         return getMgtFqdn();
      } else {
         return hostname;
      }
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public Map<NetTrafficType, List<IpConfigInfo>> getIpConfigs() {
      return ipConfigs;
   }

   public void setIpConfigs(Map<NetTrafficType, List<IpConfigInfo>> ipConfigs) {
      this.ipConfigs = ipConfigs;
   }

   public String getMgtIpAddress() {
      try {
         return ipConfigs.get(NetTrafficType.MGT_NETWORK).get(0).getIpAddress();
      } catch (Exception e) {
         logger.warn("Failed to get IP address of management network for node " + name + ": " + e.getMessage());
         return null;
      }
   }

   public String getMgtFqdn() {
      try {
         return ipConfigs.get(NetTrafficType.MGT_NETWORK).get(0).getFqdn();
      } catch (Exception e) {
         logger.warn("Failed to get the FQDN of management network for node " + name + ": " + e.getMessage());
         return null;
      }
   }

   public List<String> getVolumes() {
      return volumes;
   }

   public void setVolumes(List<String> volumes) {
      this.volumes = volumes;
   }
}
