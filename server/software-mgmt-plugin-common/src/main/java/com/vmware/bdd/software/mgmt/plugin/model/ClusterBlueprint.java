/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.software.mgmt.plugin.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.apitypes.TopologyType;

public class ClusterBlueprint implements Serializable {

   private static final long serialVersionUID = 5545914268769234047L;

   @Expose
   private String name;

   @Expose
   private int instanceNum;

   @Expose
   private List<NodeGroupInfo> nodeGroups;

   @Expose
   private Map<String, Object> configuration;

   @Expose
   private HadoopStack hadoopStack;

   private String externalHDFS;
   private String externalMapReduce;
   private TopologyType topologyPolicy;
   private boolean needToValidateConfig;
   private String externalNamenode;
   private String externalSecondaryNamenode;
   private Set<String> externalDatanodes;

   private Map<String, String> rackTopology;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getInstanceNum() {
      return instanceNum;
   }

   public void setInstanceNum(int instanceNum) {
      this.instanceNum = instanceNum;
   }

   public List<NodeGroupInfo> getNodeGroups() {
      return nodeGroups;
   }

   public void setNodeGroups(List<NodeGroupInfo> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public Map<String, Object> getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Map<String, Object> configuration) {
      this.configuration = configuration;
   }

   public HadoopStack getHadoopStack() {
      return hadoopStack;
   }

   public void setHadoopStack(HadoopStack hadoopStack) {
      this.hadoopStack = hadoopStack;
   }

   public String getExternalHDFS() {
      return externalHDFS;
   }

   public void setExternalHDFS(String externalHDFS) {
      this.externalHDFS = externalHDFS;
   }

   public String getExternalMapReduce() {
      return externalMapReduce;
   }

   public void setExternalMapReduce(String externalMapReduce) {
      this.externalMapReduce = externalMapReduce;
   }

   public boolean isNeedToValidateConfig() {
      return needToValidateConfig;
   }

   public void setNeedToValidateConfig(boolean needToValidateConfig) {
      this.needToValidateConfig = needToValidateConfig;
   }

   public String getExternalNamenode() {
      return externalNamenode;
   }

   public void setExternalNamenode(String externalNamenode) {
      this.externalNamenode = externalNamenode;
   }

   public String getExternalSecondaryNamenode() {
      return externalSecondaryNamenode;
   }

   public void setExternalSecondaryNamenode(String externalSecondaryNamenode) {
      this.externalSecondaryNamenode = externalSecondaryNamenode;
   }

   public Set<String> getExternalDatanodes() {
      return externalDatanodes;
   }

   public void setExternalDatanodes(Set<String> externalDatanodes) {
      this.externalDatanodes = externalDatanodes;
   }

   public TopologyType getTopologyPolicy() {
      return topologyPolicy;
   }

   public void setTopologyPolicy(TopologyType topologyPolicy) {
      this.topologyPolicy = topologyPolicy;
   }

   public boolean hasTopologyPolicy() {
      if (this.topologyPolicy == null || this.topologyPolicy.equals(TopologyType.NONE)) {
         return false;
      }
      return true;
   }

   public Map<String, String> getRackTopology() {
      return rackTopology;
   }

   public void setRackTopology(Map<String, String> rackTopology) {
      this.rackTopology = rackTopology;
   }

}
