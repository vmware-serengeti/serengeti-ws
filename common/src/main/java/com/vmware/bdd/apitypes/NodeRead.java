/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
 * Node get output
 */
public class NodeRead {
   @Expose
   private String name;

   @Expose
   @SerializedName("hostname")
   private String hostName;

   @Expose
   @SerializedName("ip_address")
   private String ip;

   @Expose
   private String status;
   @Expose
   private String action;

   @Expose
   private List<String> roles;
   @Expose
   private int totalRAMInMB;
   @Expose
   private int totalCPUInMHz;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getHostName() {
      return hostName;
   }

   public void setHostName(String hostName) {
      this.hostName = hostName;
   }

   public String getIp() {
      return ip;
   }

   public void setIp(String ip) {
      this.ip = ip;
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

   public int getTotalRAMInMB() {
      return totalRAMInMB;
   }

   public void setTotalRAMInMB(int totalRAMInMB) {
      this.totalRAMInMB = totalRAMInMB;
   }

   public int getTotalCPUInMHz() {
      return totalCPUInMHz;
   }

   public void setTotalCPUInMHz(int totalCPUInMHz) {
      this.totalCPUInMHz = totalCPUInMHz;
   }

}