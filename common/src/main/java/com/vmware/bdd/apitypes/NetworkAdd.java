/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * <p>
 * This class is the DTO of Network command.
 * </p>
 */
public class NetworkAdd {
   // Customize the network's name
   private String name;
   // The port group name
   @Expose
   @SerializedName("port_group")
   private String portGroup;
   // The dhcp ip information
   private boolean isDhcp;
   @Expose
   private String type = "static";
   // The dns ip information
   private String dns1;
   private String dns2;
   // The ip information
   private List<IpBlock> ipBlocks;
   // The gateway information
   @Expose
   private String gateway;
   // The netmask information
   @Expose
   private String netmask;
   @Expose
   @SerializedName("dns")
   private List<String> dnsList;
   @Expose
   @SerializedName("ip")
   private List<String> ipStrings;

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
      if (isDhcp) {
         this.type = "dhcp";
      } else {
         this.type = "static";
      }
   }

   public String getType() {
      return type;
   }

   public String getDns1() {
      return dns1;
   }

   public void setDns1(String dns1) {
      this.dns1 = dns1;
      setDnsList();
   }

   private void setDnsList() {
      if (dns1 == null && dns2 != null) {
         dnsList = null;
         return;
      }
      dnsList = new ArrayList<String>();
      if (dns1 != null) {
         dnsList.add(dns1);
      }
      if (dns2 != null) {
         dnsList.add(dns2);
      }
   }

   public String getDns2() {
      return dns2;
   }

   public void setDns2(String dns2) {
      this.dns2 = dns2;
      setDnsList();
   }

   public List<String> getDnsList() {
      return dnsList;
   }

   public List<IpBlock> getIp() {
      return ipBlocks;
   }

   public void setIp(List<IpBlock> ip) {
      this.ipBlocks = ip;
      setIpString();
   }

   private void setIpString() {
      ipStrings = null;
      if (ipBlocks == null || ipBlocks.isEmpty()) {
         return;
      }
      ipStrings = new ArrayList<String>();
      for (IpBlock ipBlock : ipBlocks) {
         ipStrings.add(ipBlock.toString());
      }
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

   @Override
   public String toString() {
      // TODO Auto-generated method stub

      StringBuffer sb = new StringBuffer();
      sb.append("name:").append(this.name).append(",").append("portGroup:")
            .append(portGroup).append(",").append("isDhcp:")
            .append(this.isDhcp()).append(",").append("dns1:")
            .append(this.dns1).append(",").append("dns2:").append(this.dns2)
            .append(",").append("ip:").append(ipBlocks).append(",")
            .append("gateway:").append(this.gateway).append(",")
            .append("netmask:").append(this.netmask);
      return sb.toString();
   }

}
