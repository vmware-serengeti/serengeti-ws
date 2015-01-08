/***************************************************************************
 * Copyright (c) 2013-2014 VMware, Inc. All Rights Reserved.
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Author: Xiaoding Bian
 * Date: 10/14/13
 * Time: 5:03 PM
 */
public class IpConfigInfo extends NetConfigInfo{

   @Expose
   @SerializedName("ip_address")
   private String ipAddress;

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public IpConfigInfo() {
   }

   public IpConfigInfo(NetTrafficType trafficType, String networkName, String portGroupName, String ipAddress) {
      super(trafficType, networkName, portGroupName);
      this.ipAddress = ipAddress;
   }

   public IpConfigInfo(ClusterNetConfigInfo netConfig, String ipAddress) {
      this(netConfig.getTrafficType(), netConfig.getNetworkName(), netConfig.getPortGroupName(), ipAddress);
   }
}
