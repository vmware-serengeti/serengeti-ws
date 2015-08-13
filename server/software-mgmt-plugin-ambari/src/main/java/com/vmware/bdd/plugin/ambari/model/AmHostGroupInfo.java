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
import java.util.List;
import java.util.Map;

import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;

public class AmHostGroupInfo {

   private String name;

   private int cardinality;

   private List<String> hosts;

   private int volumesCount;

   private List<String> roles;

   private List<Map<String, Object>> configurations;

   public AmHostGroupInfo(AmNodeDef node, AmNodeGroupDef nodeGroup) {
      // Generate a new Ambari hostGroup with name <NODE_GROUP_NAME>_vol<VOLUMES_COUNT> to distinguish different volumes of all nodes in the same group from spec file for Ambari Blueprint
      this.name = nodeGroup.getName() + "_vol" + node.getVolumesCount();

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

      List<String> hosts = new ArrayList<String>();
      hosts.add(node.getFqdn());
      this.hosts = hosts;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getCardinality() {
      return cardinality;
   }

   public void setCardinality(int cardinality) {
      this.cardinality = cardinality;
   }

   public List<String> getHosts() {
      return hosts;
   }

   public void setHosts(List<String> hosts) {
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

   public void addNewHost(AmNodeDef node) {
      this.cardinality = this.cardinality + 1;
      this.hosts.add(node.getFqdn());
   }

   public String getStringCardinality() {
      return String.valueOf(this.cardinality);
   }

   public void updateGroupName(String groupName) {
      this.name = groupName;
   }

   public ApiHostGroup toApiHostGroupForClusterBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();
      apiHostGroup.setName(this.name);
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
      apiHostGroup.setName(this.name);
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
}
