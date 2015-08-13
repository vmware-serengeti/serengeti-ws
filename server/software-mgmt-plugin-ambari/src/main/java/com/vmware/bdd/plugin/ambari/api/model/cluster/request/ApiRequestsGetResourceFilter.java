/*
 * **************************************************************************
 *  * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  **************************************************************************
 */
package com.vmware.bdd.plugin.ambari.api.model.cluster.request;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiRequestsGetResourceFilter {

   @Expose
   @SerializedName("service_name")
   private String serviceName;

   @Expose
   @SerializedName("component_name")
   private String componentName;

   @Expose
   @SerializedName("hosts")
   private List<String> hosts;

   public String getServiceName() {
      return serviceName;
   }

   public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
   }

   public String getComponentName() {
      return componentName;
   }

   public void setComponentName(String componentName) {
      this.componentName = componentName;
   }

   public List<String> getHosts() {
      return hosts;
   }

   public void setHosts(List<String> hosts) {
      this.hosts = hosts;
   }

}
