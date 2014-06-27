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
package com.vmware.bdd.plugin.ambari.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.ambari.api.model.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostGroup;

public class AmNodeDef  implements Serializable{

   private static final long serialVersionUID = 5585914239769234047L;

   @Expose
   private String name;

   @Expose
   private String ip;

   @Expose
   private String fqdn;

   @Expose
   private String rackInfo;

   @Expose
   private Map<String, Object> configurations;

   @Expose
   private List<String> components;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getIp() {
      return ip;
   }

   public void setIp(String ip) {
      this.ip = ip;
   }

   public String getFqdn() {
      return fqdn;
   }

   public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
   }

   public String getRackInfo() {
      return rackInfo;
   }

   public void setRackInfo(String rackInfo) {
      this.rackInfo = rackInfo;
   }

   public Map<String, Object> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(Map<String, Object> configurations) {
      this.configurations = configurations;
   }

   public List<String> getComponents() {
      return components;
   }

   public void setComponents(List<String> components) {
      this.components = components;
   }

   public ApiHostGroup toApiHostGroupForBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();

      apiHostGroup.setName(name);
      apiHostGroup.setCardinality("1");

      if (configurations != null) {
         List<Map<String, Object>> apiConfigurations = new ArrayList<Map<String, Object>>();
         apiConfigurations.add(configurations);
         apiHostGroup.setConfigurations(apiConfigurations);
      }

      List<ApiComponentInfo> apiComponents = new ArrayList<ApiComponentInfo>();
      for (String componentName : components) {
         ApiComponentInfo apiComponent = new ApiComponentInfo();
         apiComponent.setName(componentName);
         apiComponents.add(apiComponent);
      }
      apiHostGroup.setApiComponents(apiComponents);
      return apiHostGroup;
   }

   public ApiHostGroup toApiHostGroupForClusterBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();

      apiHostGroup.setName(name);

      if (configurations  != null) {
         List<Map<String, Object>> apiConfigurations = new ArrayList<Map<String, Object>>();
         apiConfigurations.add(configurations);
         // TODO volumns
         apiHostGroup.setConfigurations(apiConfigurations);
      }

      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      ApiHost apiHost = new ApiHost();
      apiHost.setFqdn(fqdn);
      apiHosts.add(apiHost);
      apiHostGroup.setApiHosts(apiHosts);
      return apiHostGroup;
   }

}
