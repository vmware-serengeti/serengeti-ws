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
package com.vmware.bdd.plugin.ambari.api.model.bootstrap;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiBootstrapStatus {

   @Expose
   private String status;

   @Expose
   @SerializedName("hostsStatus")
   private List<ApiBootstrapHostStatus> apiBootstrapHostStatus;

   @Expose
   private String log;

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public List<ApiBootstrapHostStatus> getApiBootstrapHostStatus() {
      return apiBootstrapHostStatus;
   }

   public void setApiBootstrapHostStatus(List<ApiBootstrapHostStatus> apiBootstrapHostStatus) {
      this.apiBootstrapHostStatus = apiBootstrapHostStatus;
   }

   public String getLog() {
      return log;
   }

   public void setLog(String log) {
      this.log = log;
   }

}
