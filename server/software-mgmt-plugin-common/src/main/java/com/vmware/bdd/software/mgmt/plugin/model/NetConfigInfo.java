/***************************************************************************
 * Copyright (c) 2013 VMware, Inc. All Rights Reserved.
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Author: Xiaoding Bian
 * Date: 10/15/13
 * Time: 11:13 AM
 */
public class NetConfigInfo {
   @Expose
   @SerializedName("port_group_name")
   private String portGroupName;

   @Expose
   @SerializedName("network_name")
   private String networkName;

   @Expose
   @SerializedName("traffic_type")
   private NetTrafficType trafficType;

   public NetConfigInfo() {
   }

   public NetConfigInfo(NetTrafficType trafficType, String networkName, String portGroupName) {
      this.trafficType = trafficType;
      this.networkName = networkName;
      this.portGroupName = portGroupName;
   }

   public String getPortGroupName() {
      return portGroupName;
   }

   public void setPortGroupName(String portGroupName) {
      this.portGroupName = portGroupName;
   }

   public String getNetworkName() {
      return networkName;
   }

   public void setNetworkName(String networkName) {
      this.networkName = networkName;
   }

   public NetTrafficType getTrafficType() {
      return trafficType;
   }

   public void setTrafficType(NetTrafficType trafficType) {
      this.trafficType = trafficType;
   }

   public enum NetTrafficType{
      MGT_NETWORK, HDFS_NETWORK, MAPRED_NETWORK
   }
}
