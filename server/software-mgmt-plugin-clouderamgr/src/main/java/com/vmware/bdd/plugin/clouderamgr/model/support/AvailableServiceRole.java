/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.model.support;

import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Author: Xiaoding Bian
 * Date: 6/12/14
 * Time: 3:37 PM
 */
public class AvailableServiceRole implements Comparable<AvailableServiceRole> {

   public static final String ROOT_SERVICE = "CLUSTER";

   @Expose
   private String name; // the name used in Cloudera Manager, i.e, "SERVER" for zookeeper server

   @Expose
   private String displayName; // the alias name used in BDE, i.e, "ZOOKEEPER_SERVER" for role "SERVER"

   @Expose(serialize = false)
   private AvailableServiceRole parent;

   @Expose
   private AvailableParcelRepo repository;

   @Expose
   private String versionApiMin;

   @Expose
   private String versionApiMax;

   @Expose
   private String versionCdhMin;

   @Expose
   private String versionCdhMax;

   @Expose(serialize = false)
   private Map<String, AvailableConfiguration> availableConfigurations;

   private List<Dependency> dependencies;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public AvailableServiceRole getParent() {
      return parent;
   }

   public void setParent(String parent) throws IOException {
      this.parent = null;
      if (parent != null) {
         this.parent = AvailableServiceRoleContainer.load(parent);
      }
   }

   public AvailableParcelRepo getRepository() {
      return repository;
   }

   public void setRepository(String repository) {
      this.repository = AvailableParcelRepo.valueOf(repository);
   }

   public String getVersionApiMin() {
      return versionApiMin;
   }

   public void setVersionApiMin(String versionApiMin) {
      this.versionApiMin = versionApiMin;
   }

   public String getVersionApiMax() {
      return versionApiMax;
   }

   public void setVersionApiMax(String versionApiMax) {
      this.versionApiMax = versionApiMax;
   }

   public String getVersionCdhMin() {
      return versionCdhMin;
   }

   public void setVersionCdhMin(String versionCdhMin) {
      this.versionCdhMin = versionCdhMin;
   }

   public String getVersionCdhMax() {
      return versionCdhMax;
   }

   public void setVersionCdhMax(String versionCdhMax) {
      this.versionCdhMax = versionCdhMax;
   }

   public Map<String, AvailableConfiguration> getAvailableConfigurations() {
      return availableConfigurations;
   }

   public void setAvailableConfigurations(List<AvailableConfiguration> configs) {
      this.availableConfigurations = new HashMap<String, AvailableConfiguration>();
      this.dependencies = new ArrayList<Dependency>();
      Pattern serviceDependPattern = Pattern.compile("([a-z]+\\_)+service$");
      for (AvailableConfiguration config : configs) {
         this.availableConfigurations.put(config.getName(), config);
         if (serviceDependPattern.matcher(config.getName()).matches()) {
            try {
               Dependency dependency = new Dependency(config);
               this.dependencies.add(dependency);
            } catch (Exception e) {
            }
         }
      }
   }

   public List<Dependency> getDependencies() {
      return dependencies;
   }

   public boolean isService() {
      return parent != null && parent.getDisplayName().equalsIgnoreCase(ROOT_SERVICE);
   }

   public boolean isRole() {
      return parent != null && parent.isService();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
         return false;
      }
      AvailableServiceRole other = (AvailableServiceRole) o;
      if (this.getName().equalsIgnoreCase(other.getName()) && this.getDisplayName().equalsIgnoreCase(other.getDisplayName())) {
         return true;
      }
      return false;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (name == null? 0 : name.hashCode());
      result = prime * result + (displayName == null? 0 : displayName.hashCode());
      return result;
   }

   @Override
   public int compareTo(AvailableServiceRole other) {
      if (!isService() || !other.isService()) {
         return 0;
      }
      if (dependsOn(other)) {
         return 1;
      }
      if (other.dependsOn(this)) {
         return -1;
      }
      return 0;
   }

   private boolean dependsOn(AvailableServiceRole other) {
      Queue<AvailableServiceRole> queue = new LinkedList<AvailableServiceRole>();
      queue.add(this);
      while (!queue.isEmpty()) {
         AvailableServiceRole item = queue.poll();
         for (Dependency dependency : item.getDependencies()) {
            for (String serviceName : dependency.getServices()) {
               if (serviceName.equals(other.getDisplayName())) {
                  return true;
               }
               try {
                  queue.add(AvailableServiceRoleContainer.load(serviceName));
               } catch (IOException e) {
               }
            }
         }
      }

      return false;
   }

   public static class Dependency {
      private List<String> services;
      private String configKey;
      private boolean required;

      public boolean isRequired() {
         return required;
      }

      public void setRequired(boolean required) {
         this.required = required;
      }

      public List<String> getServices() {
         return services;
      }

      public void setServices(List<String> services) {
         this.services = services;
      }

      public String getConfigKey() {
         return configKey;
      }

      public void setConfigKey(String configKey) {
         this.configKey = configKey;
      }

      public Dependency(AvailableConfiguration config) {
         String[] item = config.getName().split("_");
         this.services = new ArrayList<String>();
         for (int i = 0; i < item.length - 1; i++) {
            this.services.add(item[i].toUpperCase());
         }
         this.configKey = config.getName();
         this.required = config.isRequired();
      }
   }
}
