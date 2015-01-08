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
 * Date: 10/15/13
 * Time: 11:13 AM
 */
public class ClusterNetConfigInfo extends NetConfigInfo {

   @Expose
   @SerializedName("dns_type")
   private NetworkDnsType dnsType;

   @Expose
   @SerializedName("is_generate_hostname")
   private Boolean isGenerateHostname;

   @Expose
   @SerializedName("hostname_prefix")
   private String hostnamePrefix;

   public ClusterNetConfigInfo(NetTrafficType trafficType, String networkName, String portGroupName, NetworkDnsType dnsType, Boolean isGenerateHostname, String hostnamePrefix) {
      super(trafficType, networkName, portGroupName);
      this.dnsType = dnsType;
      this.isGenerateHostname = isGenerateHostname;
      this.hostnamePrefix = hostnamePrefix;
   }

   public NetworkDnsType getDnsType() {
      return dnsType;
   }

   public void setDnsType(NetworkDnsType dnsType) {
      this.dnsType = dnsType;
   }

   public Boolean getIsGenerateHostname() {
      return isGenerateHostname;
   }

   public void setIsGenerateHostname(Boolean isGenerateHostname) {
      this.isGenerateHostname = isGenerateHostname;
   }

   public String getHostnamePrefix() {
      return hostnamePrefix;
   }

   public void setHostnamePrefix(String hostnamePrefix) {
      this.hostnamePrefix = hostnamePrefix;
   }
}
