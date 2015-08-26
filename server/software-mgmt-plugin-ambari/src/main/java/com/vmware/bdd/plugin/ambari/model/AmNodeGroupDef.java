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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;

import com.vmware.bdd.apitypes.LatencyPriority;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;

public class AmNodeGroupDef implements Serializable {

   private static final long serialVersionUID = 4443680719623071084L;

   @Expose
   private String name;

   @Expose
   private List<String> roles;

   @Expose
   private int instanceNum;

   public LatencyPriority getLatencySensitivity() {
      return latencySensitivity;
   }

   public void setLatencySensitivity(
         LatencyPriority latencySensitivity) {
      this.latencySensitivity = latencySensitivity;
   }

   @Expose
   private LatencyPriority latencySensitivity;

   public int getMemSize() {
      return memSize;
   }

   public void setMemSize(int memSize) {
      this.memSize = memSize;
   }

   @Expose
   private int memSize;

   @Expose
   private List<Map<String, Object>> configurations;

   @Expose
   private List<AmNodeDef> nodes;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getRoles() {
      return roles;
   }

   public void setRoles(List<String> roles) {
      this.roles = roles;
   }

   public int getInstanceNum() {
      return instanceNum;
   }

   public void setInstanceNum(int instanceNum) {
      this.instanceNum = instanceNum;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public List<AmNodeDef> getNodes() {
      return nodes;
   }

   public void setNodes(List<AmNodeDef> nodes) {
      this.nodes = nodes;
   }

   public List<ApiHostGroup> toApiHostGroupsForBlueprint() {
      List<ApiHostGroup> apiHostGroups = new ArrayList<ApiHostGroup>();

      for (AmHostGroupInfo amHostGroupInfo: generateHostGroupsInfo()) {
         apiHostGroups.add(amHostGroupInfo.toApiHostGroupForBlueprint());
      }

      return apiHostGroups;
   }

   public List<ApiHostGroup> toApiHostGroupForClusterBlueprint() {
      List<ApiHostGroup> apiHostGroups = new ArrayList<ApiHostGroup>();

      for (AmHostGroupInfo amHostGroupInfo: generateHostGroupsInfo()) {
         apiHostGroups.add(amHostGroupInfo.toApiHostGroupForClusterBlueprint());
      }
      return apiHostGroups;
   }

   public List<AmHostGroupInfo> generateHostGroupsInfo() {
      return generateHostGroupsInfo(null);
   }

   public List<AmHostGroupInfo> generateHostGroupsInfo(Map<String, String> configTypeToService) {
      Map<Integer, AmHostGroupInfo> amHostGroupsInfoMap = new HashMap<Integer, AmHostGroupInfo>();

      for (AmNodeDef node : this.nodes) {
         if (amHostGroupsInfoMap.isEmpty()) {
            AmHostGroupInfo amHostGroupInfo = new AmHostGroupInfo(node, this, configTypeToService);
            amHostGroupsInfoMap.put(amHostGroupInfo.getVolumesCount(), amHostGroupInfo);
         } else {
            AmHostGroupInfo existingAmHostGroupInfo = amHostGroupsInfoMap.get(node.getVolumesCount());
            if (existingAmHostGroupInfo != null) {
               existingAmHostGroupInfo.addNewHost(node);
            } else {
               AmHostGroupInfo newAmHostGroupInfo = new AmHostGroupInfo(node, this, configTypeToService);
               amHostGroupsInfoMap.put(newAmHostGroupInfo.getVolumesCount(), newAmHostGroupInfo);
            }
         }
      }

      // Set the name of Ambari hostGroup to the nodeGroup name from spec file if all nodes have the same volumes count
      if (amHostGroupsInfoMap.size() == 1) {
         amHostGroupsInfoMap.get(this.nodes.get(0).getVolumesCount()).updateConfigGroupName(this.getName());
      }

      List<AmHostGroupInfo> amHostGroupsInfo = new ArrayList<AmHostGroupInfo>();
      amHostGroupsInfo.addAll(amHostGroupsInfoMap.values());

      return amHostGroupsInfo;
   }
}
