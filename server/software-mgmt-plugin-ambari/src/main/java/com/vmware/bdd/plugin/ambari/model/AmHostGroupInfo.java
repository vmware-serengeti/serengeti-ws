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
package com.vmware.bdd.plugin.ambari.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;

public class AmHostGroupInfo {

   private String configGroupName;

   private String nodeGroupName;

   private int cardinality;

   private Set<String> hosts;

   private int volumesCount;

   private List<String> roles;

   private List<Map<String, Object>> configurations;

   // Just for cluster resize
   private Map<String, Set<String>> tag2Hosts = new HashMap<String, Set<String>> ();

   public AmHostGroupInfo(AmNodeDef node, AmNodeGroupDef nodeGroup, Map<String, String> configTypeToService) {
      // Generate a new Ambari hostGroup with name <NODE_GROUP_NAME>_vol<VOLUMES_COUNT> to distinguish different volumes of all nodes in the same group from spec file for Ambari Blueprint
      this.configGroupName = nodeGroup.getName() + "_vol" + node.getVolumesCount();

      this.nodeGroupName = nodeGroup.getName();
      this.cardinality = 1;
      this.roles = nodeGroup.getRoles();
      this.volumesCount = node.getVolumesCount();

      List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
      if (!nodeGroup.getConfigurations().isEmpty()) {
         configurations.addAll(nodeGroup.getConfigurations());
      }
      if (!node.getConfigurations().isEmpty()) {
         configurations.addAll(node.getConfigurations());
      }
      this.configurations = configurations;

      Set<String> hosts = new HashSet<String>();
      hosts.add(node.getFqdn());
      this.hosts = hosts;

      if (configTypeToService != null) {
         for(String service : getServices(configTypeToService, configurations)) {
            this.tag2Hosts.put(service, hosts);
         }
      }
   }


   private static Set<String> getServices(Map<String, String> configTypeToService, List<Map<String, Object>> configurations) {
      Set<String> services = new HashSet<String> ();
      List<Map<String, Object>> configs = configurations;
      for (Map<String, Object> map : configs) {
         for (String type : map.keySet()) {
            String service = configTypeToService.get(type + ".xml");
            if (service != null) {
               services.add(service);
            }
         }
      }
      return services;
   }

   public String getConfigGroupName() {
      return configGroupName;
   }

   public void setConfigGroupName(String configGroupame) {
      this.configGroupName = configGroupame;
   }

   public int getCardinality() {
      return cardinality;
   }

   public void setCardinality(int cardinality) {
      this.cardinality = cardinality;
   }

   public Set<String> getHosts() {
      return hosts;
   }

   public void setHosts(Set<String> hosts) {
      this.hosts = hosts;
   }


   public int getVolumesCount() {
      return volumesCount;
   }

   public void setVolumesCount(int volumesCount) {
      this.volumesCount = volumesCount;
   }

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public String getNodeGroupName() {
      return nodeGroupName;
   }

   public void setNodeGroupName(String nodeGroupName) {
      this.nodeGroupName = nodeGroupName;
   }

   public Map<String, Set<String>> getTag2Hosts() {
      return tag2Hosts;
   }

   public void setTag2Hosts(Map<String, Set<String>> tag2Hosts) {
      this.tag2Hosts = tag2Hosts;
   }

   public void addNewHost(AmNodeDef node) {
     this.cardinality = this.cardinality + 1;
     String host = node.getFqdn();
     this.hosts.add(host);
     addNewHost2Tag(host);
   }

   private void addNewHost2Tag(String host) {
      for (String tag : this.tag2Hosts.keySet()) {
         Set<String> hosts = this.tag2Hosts.get(tag);
         if (hosts == null) {
            hosts = new HashSet<String> ();
         }
         hosts.add(host);
         this.tag2Hosts.put(tag, hosts);
      }
   }

   public String getStringCardinality() {
      return String.valueOf(this.cardinality);
   }

   public void updateConfigGroupName(String configGroupame) {
      this.configGroupName = configGroupame;
   }

   public ApiHostGroup toApiHostGroupForClusterBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();
      apiHostGroup.setName(this.configGroupName);
      apiHostGroup.setCardinality(this.getStringCardinality());
      apiHostGroup.setConfigurations(this.configurations);

      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      for (String host : this.hosts) {
         ApiHost apiHost = new ApiHost();
         apiHost.setFqdn(host);
         apiHosts.add(apiHost);
      }
      apiHostGroup.setApiHosts(apiHosts);

      return apiHostGroup;
   }

   public ApiHostGroup toApiHostGroupForBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();
      apiHostGroup.setName(this.configGroupName);
      apiHostGroup.setCardinality(this.getStringCardinality());
      apiHostGroup.setConfigurations(this.configurations);

      List<ApiComponentInfo> apiComponents = new ArrayList<ApiComponentInfo>();
      for (String componentName : this.roles) {
         ApiComponentInfo apiComponent = new ApiComponentInfo();
         apiComponent.setName(componentName);
         apiComponents.add(apiComponent);
      }
      apiHostGroup.setApiComponents(apiComponents);

      return apiHostGroup;
   }

   public void removeOldTag(String tag) {
      this.tag2Hosts.remove(tag);
   }

   public boolean removeOldHostFromTag(String host, String tag) {
      if (this.tag2Hosts.get(tag) == null) {
         return false;
      } else {
         boolean isRemoved = this.tag2Hosts.get(tag).remove(host);
         if (this.tag2Hosts.get(tag).isEmpty()) {
            removeOldTag(tag);
         }
         return isRemoved;
      }
   }
}
