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
package com.vmware.bdd.apitypes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class VcVmNicInfo {

   @Expose
   private String portgroup;

   @Expose
   @SerializedName("ipaddr")
   private String ipAddress;

   @Expose
   private String fqdn;

   @Expose
   private String device;

   public String getPortgroup() {
      return portgroup;
   }

   public void setPortgroup(String portgroup) {
      this.portgroup = portgroup;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getFqdn() {
      return fqdn;
   }

   public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
   }

   public String getDevice() {
      return device;
   }

   public void setDevice(String device) {
      this.device = device;
   }

}
