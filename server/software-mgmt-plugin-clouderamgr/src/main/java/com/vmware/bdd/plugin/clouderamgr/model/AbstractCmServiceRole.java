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
package com.vmware.bdd.plugin.clouderamgr.model;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 7/10/14
 * Time: 11:41 AM
 */
abstract class AbstractCmServiceRole {

   @Expose
   private String name;

   @Expose
   private AvailableServiceRole type;

   @Expose
   private String displayName;

   @Expose
   private Map<String, String> configuration;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public AvailableServiceRole getType() {
      return type;
   }

   public void setType(AvailableServiceRole type) {
      this.type = type;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public Map<String, String> getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Map<String, String> configuration) {
      this.configuration = configuration;
   }

   public void addConfig(String key, String value) {
      if (configuration == null) {
         configuration = new HashMap<String, String>();
      }
      configuration.put(key, value);
   }

   public void addConfigs(Map<String, Object> configMap) {
      if (configuration == null) {
         configuration = new HashMap<String, String>();
      }
      if (configMap != null && configMap.containsKey(type.getDisplayName())) {
         Map<String, String> m = (Map<String, String>) configMap.get(type.getDisplayName());
         for (String k : m.keySet()) {
            configuration.put(k, m.get(k));
         }
      }
   }

   public abstract boolean isService();
   public abstract boolean isRole();
}
