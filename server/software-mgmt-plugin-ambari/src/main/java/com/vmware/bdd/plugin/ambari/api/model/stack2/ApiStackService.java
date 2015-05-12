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
package com.vmware.bdd.plugin.ambari.api.model.stack2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;

public class ApiStackService {

   @Expose
   private String href;

   @Expose
   @SerializedName("StackServices")
   private ApiStackServiceInfo apiStackServiceInfo;

   @Expose
   @SerializedName("serviceComponents")
   private List<ApiStackComponent> serviceComponents;

   @Expose
   @SerializedName("configurations")
   private List<ApiConfiguration> apiConfigurations;

   // Just for ambari server 2.0
   
   @Expose
   @SerializedName("components")
   private List<ApiStackComponent> components;
   
   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   public ApiStackServiceInfo getApiStackServiceInfo() {
      return apiStackServiceInfo;
   }

   public void setApiStackServiceInfo(ApiStackServiceInfo apiStackServiceInfo) {
      this.apiStackServiceInfo = apiStackServiceInfo;
   }

   public List<ApiStackComponent> getServiceComponents() {
      if (components != null) {
         return components;
      } else {
         return serviceComponents;
      }
   }

   public void setServiceComponents(List<ApiStackComponent> serviceComponents) {
      this.serviceComponents = serviceComponents;
   }

   public List<ApiConfiguration> getApiConfigurations() {
      return apiConfigurations;
   }

   public void setApiConfigurations(List<ApiConfiguration> apiConfigurations) {
      this.apiConfigurations = apiConfigurations;
   }

   public Map<String, String> configTypeToService() {
      Map<String, String> result = new HashMap<>();
      if (apiConfigurations != null) {
         for (ApiConfiguration config : apiConfigurations) {
            if (!result.containsKey(config.getApiConfigurationInfo().getType())) {
               result.put(config.getApiConfigurationInfo().getType(), config
                     .getApiConfigurationInfo().getServiceName());
            }
         }
      }
      return result;
   }

   public Map<String, ApiComponentInfo> componentToInfo() {
      Map<String, ApiComponentInfo> result = new HashMap<>();
      if (getServiceComponents() != null) {
         for (ApiStackComponent component : getServiceComponents()) {
            if (!result.containsKey(component.getApiComponent()
                  .getComponentName())) {
               result.put(component.getApiComponent().getComponentName(),
                     component.getApiComponent());
            }
         }
      }
      return result;
   }
}
