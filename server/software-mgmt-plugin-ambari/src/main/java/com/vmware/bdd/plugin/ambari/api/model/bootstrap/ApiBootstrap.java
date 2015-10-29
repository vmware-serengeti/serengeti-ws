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
package com.vmware.bdd.plugin.ambari.api.model.bootstrap;

import java.util.List;

import com.google.gson.annotations.Expose;

public class ApiBootstrap {

   @Expose
   private boolean verbose;

   @Expose
   private String sshKey;

   @Expose
   private List<String> hosts;

   @Expose
   private String user;

   @Expose
   private String status;

   @Expose
   private String log;

   @Expose
   private Long requestId;

   // Just for ambari server 2.0
   @Expose
   private String userRunAs;

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getLog() {
      return log;
   }

   public void setLog(String log) {
      this.log = log;
   }

   public Long getRequestId() {
      return requestId;
   }

   public void setRequestId(Long requestId) {
      this.requestId = requestId;
   }

   public boolean isVerbose() {
      return verbose;
   }

   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }

   public String getSshKey() {
      return sshKey;
   }

   public void setSshKey(String sshKey) {
      this.sshKey = sshKey;
   }

   public List<String> getHosts() {
      return hosts;
   }

   public void setHosts(List<String> hosts) {
      this.hosts = hosts;
   }

   public String getUser() {
      return user;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public String getUserRunAs() {
      return userRunAs;
   }

   public void setUserRunAs(String userRunAs) {
      this.userRunAs = userRunAs;
   }

}
