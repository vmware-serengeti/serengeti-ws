/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.cluster;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiComponent {

   @Expose
   private String href;

   @Expose
   @SerializedName("ServiceComponentInfo")
   private ApiComponentInfo serviceComponentInfo;

   @Expose
   @SerializedName("host_components")
   private List<ApiHostComponent> hostComponents;

   @Expose
   @SerializedName("HostRoles")
   private ApiComponentInfo hostComponent;

   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   public ApiComponentInfo getServiceComponentInfo() {
      return serviceComponentInfo;
   }

   public void setServiceComponentInfo(ApiComponentInfo serviceComponentInfo) {
      this.serviceComponentInfo = serviceComponentInfo;
   }

   public List<ApiHostComponent> getHostComponents() {
      return hostComponents;
   }

   public void setHostComponents(List<ApiHostComponent> hostComponents) {
      this.hostComponents = hostComponents;
   }

   public ApiComponentInfo getHostComponent() {
      return hostComponent;
   }

   public void setHostComponent(ApiComponentInfo hostComponent) {
      this.hostComponent = hostComponent;
   }
}
