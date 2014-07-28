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
package com.vmware.bdd.plugin.ambari.api.model.cluster;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiHostGroup {

   @Expose
   private String name;

   @Expose
   private List<Map<String, Object>> configurations;

   @Expose
   @SerializedName("components")
   private List<ApiComponentInfo> ApiComponents;

   @Expose
   private String cardinality;

   @Expose
   @SerializedName("hosts")
   private List<ApiHost> apiHosts;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getCardinality() {
      return cardinality;
   }

   public void setCardinality(String cardinality) {
      this.cardinality = cardinality;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public List<ApiComponentInfo> getApiComponents() {
      return ApiComponents;
   }

   public void setApiComponents(List<ApiComponentInfo> apiComponents) {
      ApiComponents = apiComponents;
   }

   public List<ApiHost> getApiHosts() {
      return apiHosts;
   }

   public void setApiHosts(List<ApiHost> apiHosts) {
      this.apiHosts = apiHosts;
   }
}
