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
package com.vmware.bdd.plugin.ambari.api.model.stack;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiIdentity;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackComponent;

public class ApiKerberosDescriptor {

   @Expose
   @SerializedName("auth_to_local_properties")
   private List<String> authToLocalProperties;

   @Expose
   @SerializedName("components")
   private List<ApiStackComponent> components;

   @Expose
   @SerializedName("identities")
   private List<ApiIdentity> identities;

   @Expose
   @SerializedName("configurations")
   private List<Map<String, Object>> configurations;

   @Expose
   @SerializedName("name")
   private String name;

   public List<String> getAuthToLocalProperties() {
      return authToLocalProperties;
   }

   public void setAuthToLocalProperties(List<String> authToLocalProperties) {
      this.authToLocalProperties = authToLocalProperties;
   }

   public List<ApiStackComponent> getComponents() {
      return components;
   }

   public void setComponents(List<ApiStackComponent> components) {
      this.components = components;
   }

   public List<ApiIdentity> getIdentities() {
      return identities;
   }

   public void setIdentities(List<ApiIdentity> identities) {
      this.identities = identities;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

}
