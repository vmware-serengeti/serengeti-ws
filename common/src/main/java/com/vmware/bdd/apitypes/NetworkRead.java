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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Network get output
 */
public class NetworkRead {
   private String name;

   @Expose
   @SerializedName("port_group")
   private String portGroup;

   private boolean isDhcp;

   private String dns1;

   private String dns2;

   private List<IpBlock> allIpBlocks;

   private List<IpBlock> freeIpBlocks;

   private List<IpBlock> assignedIpBlocks;

   private String gateway;

   private String netmask;

   /**
    * valid when "show details" option is on
    */
   private List<IpAllocEntryRead> ipAllocEntries;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getPortGroup() {
      return portGroup;
   }

   public void setPortGroup(String portGroup) {
      this.portGroup = portGroup;
   }

   public boolean isDhcp() {
      return isDhcp;
   }

   public void setDhcp(boolean isDhcp) {
      this.isDhcp = isDhcp;
   }

   public String getDns1() {
      return dns1;
   }

   public void setDns1(String dns1) {
      this.dns1 = dns1;
   }

   public String getDns2() {
      return dns2;
   }

   public void setDns2(String dns2) {
      this.dns2 = dns2;
   }

   public List<IpBlock> getAllIpBlocks() {
      return allIpBlocks;
   }

   public void setAllIpBlocks(List<IpBlock> allIpBlocks) {
      this.allIpBlocks = allIpBlocks;
   }

   public List<IpBlock> getFreeIpBlocks() {
      return freeIpBlocks;
   }

   public void setFreeIpBlocks(List<IpBlock> freeIpBlocks) {
      this.freeIpBlocks = freeIpBlocks;
   }

   public List<IpBlock> getAssignedIpBlocks() {
      return assignedIpBlocks;
   }

   public void setAssignedIpBlocks(List<IpBlock> assignedIpBlocks) {
      this.assignedIpBlocks = assignedIpBlocks;
   }

   public String getGateway() {
      return gateway;
   }

   public void setGateway(String gateway) {
      this.gateway = gateway;
   }

   public String getNetmask() {
      return netmask;
   }

   public void setNetmask(String netmask) {
      this.netmask = netmask;
   }

   public List<IpAllocEntryRead> getIpAllocEntries() {
      return ipAllocEntries;
   }

   public void setIpAllocEntries(List<IpAllocEntryRead> ipAllocEntries) {
      this.ipAllocEntries = ipAllocEntries;
   }
   public String findDhcpOrIp(){
      return isDhcp() ? "dhcp" : "static ip";
   }
}
