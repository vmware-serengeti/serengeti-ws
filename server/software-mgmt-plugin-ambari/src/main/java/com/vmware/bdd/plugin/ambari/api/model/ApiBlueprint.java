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
package com.vmware.bdd.plugin.ambari.api.model;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiBlueprint {

   @Expose
   private String href;

   @Expose
   private List<Map<String, Object>> configurations;

   @Expose
   @SerializedName("host_groups")
   private List<ApiHostGroup> apiHostGroups;

   @Expose
   @SerializedName("Blueprints")
   private ApiBlueprintInfo apiBlueprintInfo;

   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public ApiBlueprintInfo getApiBlueprintInfo() {
      return apiBlueprintInfo;
   }

   public void setApiBlueprintInfo(ApiBlueprintInfo apiBlueprintInfo) {
      this.apiBlueprintInfo = apiBlueprintInfo;
   }

   public List<ApiHostGroup> getApiHostGroups() {
      return apiHostGroups;
   }

   public void setApiHostGroups(List<ApiHostGroup> apiHostGroups) {
      this.apiHostGroups = apiHostGroups;
   }

}
