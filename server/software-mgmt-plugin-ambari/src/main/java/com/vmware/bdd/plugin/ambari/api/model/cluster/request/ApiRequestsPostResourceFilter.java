/*
 * **************************************************************************
 *  * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by qjin on 12/6/14.
 */
public class ApiRequestsPostResourceFilter {

   @Expose
   @SerializedName("service_name")
   private String serviceName;

   @Expose
   @SerializedName("component_name")
   private String componentName;

   @Expose
   @SerializedName("hosts")
   private String hosts;

   public ApiRequestsPostResourceFilter(String serviceName, String componentName) {
      this.serviceName = serviceName;
      this.componentName = componentName;
   }

   public ApiRequestsPostResourceFilter(String serviceName, String componentName, String hosts) {
      this(serviceName, componentName);
      this.hosts = hosts;
   }

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

   public String getHosts() {
      return hosts;
   }

   public void setHosts(String hosts) {
      this.hosts = hosts;
   }
}
