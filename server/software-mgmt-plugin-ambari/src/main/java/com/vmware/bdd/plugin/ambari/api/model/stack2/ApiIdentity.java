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
package com.vmware.bdd.plugin.ambari.api.model.stack2;

import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiIdentity {

   @Expose
   @SerializedName("principal")
   private Map<String, String> principal;

   @Expose
   @SerializedName("name")
   private String name;

   @Expose
   @SerializedName("keytab")
   private Map<String, Object> keytab;

   @Expose
   @SerializedName("configuration")
   private String configuration;

   @Expose
   @SerializedName("group")
   private Map<String, String> group;

   public Map<String, String> getPrincipal() {
      return principal;
   }

   public void setPrincipal(Map<String, String> principal) {
      this.principal = principal;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Map<String, Object> getKeytab() {
      return keytab;
   }

   public void setKeytab(Map<String, Object> keytab) {
      this.keytab = keytab;
   }

   public String getConfiguration() {
      return configuration;
   }

   public void setConfiguration(String configuration) {
      this.configuration = configuration;
   }

   public Map<String, String> getGroup() {
      return group;
   }

   public void setGroup(Map<String, String> group) {
      this.group = group;
   }

}
