/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.spectypes;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.NetConfigInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 12/16/13
 * Time: 3:04 PM
 */
public class NicSpec {

   private String networkName;

   private String portGroupName;

   private String macAddress;

   private String ipv4Address;

   private String ipv6Address;

   private Set<NetTrafficDefinition> netTrafficDefinitionSet;

   public static class NetTrafficDefinition {

      private NetConfigInfo.NetTrafficType trafficType;

      private int index;

      public NetTrafficDefinition() {}

      public NetTrafficDefinition(NetConfigInfo.NetTrafficType trafficType, int index) {
         this.trafficType = trafficType;
         this.index = index;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }
         NetTrafficDefinition netDef = (NetTrafficDefinition) obj;
         if (netDef.getTrafficType().equals(trafficType) && netDef.getIndex() == index) {
            return true;
         }
         return false;
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 17;
         result = prime * result + trafficType.hashCode();
         result = prime * result + index;
         return result;
      }

      public NetConfigInfo.NetTrafficType getTrafficType() {
         return trafficType;
      }

      public void setTrafficType(NetConfigInfo.NetTrafficType trafficType) {
         this.trafficType = trafficType;
      }

      public int getIndex() {
         return index;
      }

      public void setIndex(int index) {
         this.index = index;
      }
   }

   @Override
   public String toString() {
      return (new Gson()).toJson(this);
   }

   public String getNetworkName() {
      return networkName;
   }

   public void setNetworkName(String networkName) {
      this.networkName = networkName;
   }

   public String getPortGroupName() {
      return portGroupName;
   }

   public void setPortGroupName(String portGroupName) {
      this.portGroupName = portGroupName;
   }

   public String getMacAddress() {
      return macAddress;
   }

   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }

   public String getIpv4Address() {
      return ipv4Address;
   }

   public void setIpv4Address(String ipv4Address) {
      this.ipv4Address = ipv4Address;
   }

   public String getIpv6Address() {
      return ipv6Address;
   }

   public void setIpv6Address(String ipv6Address) {
      this.ipv6Address = ipv6Address;
   }

   public Set<NetTrafficDefinition> getNetTrafficDefinitionSet() {
      return netTrafficDefinitionSet;
   }

   public void setNetTrafficDefinitionSet(Set<NetTrafficDefinition> netTrafficDefinitionSet) {
      this.netTrafficDefinitionSet = netTrafficDefinitionSet;
   }

   public void addToNetDefs(NetConfigInfo.NetTrafficType trafficType, int index) {
      NetTrafficDefinition netDef = new NetTrafficDefinition(trafficType, index);
      if (netTrafficDefinitionSet == null) {
         netTrafficDefinitionSet = new HashSet<NetTrafficDefinition>();
      }
      netTrafficDefinitionSet.add(netDef);
   }

   public boolean isPrimaryMgtNetwork() {
      for (NetTrafficDefinition netDef : netTrafficDefinitionSet) {
         if (netDef.getTrafficType().equals(NetConfigInfo.NetTrafficType.MGT_NETWORK)
               && netDef.getIndex() == 0) {
            return true;
         }
      }
      return false;
   }
}
