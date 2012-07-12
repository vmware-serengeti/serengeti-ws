/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.apitypes;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.VcCluster;

/**
 * Cluster creation parameters
 */
public class NodeGroupCreate {

   @Expose
   private String name;
   private GroupType groupType;
   @Expose
   private List<String> roles;
   @Expose
   @SerializedName("instance_num")
   private int instanceNum;
   @SerializedName("instance_type")
   private InstanceType instanceType;
   @Expose
   private StorageRead storage;
   @Expose
   @SerializedName("cpu")
   private int cpuNum;
   @Expose
   @SerializedName("memory")
   private int memCapacityMB;
   private List<String> rpNames;
   @Expose
   @SerializedName("vc_clusters")
   private List<VcCluster> vcClusters;
   @Expose
   @SerializedName("ha")
   private boolean haFlag;
   @Expose
   private Map<String, Object> configuration;

   public NodeGroupCreate() {
      
   }

   public NodeGroupCreate(NodeGroupCreate group) {
      this.cpuNum = group.cpuNum;
      this.groupType = group.groupType;
      this.haFlag = group.haFlag;
      this.instanceNum = group.instanceNum;
      this.instanceType = group.instanceType;
      this.memCapacityMB = group.memCapacityMB;
      this.name = group.name;
      this.roles = group.roles;
      this.rpNames = group.rpNames;
      this.storage = group.storage;
      this.vcClusters = group.vcClusters;
      this.configuration = group.configuration;
   }

   public Map<String, Object> getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Map<String, Object> configuration) {
      this.configuration = configuration;
   }

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

   public InstanceType getInstanceType() {
      return instanceType;
   }

   public void setInstanceType(InstanceType instanceType) {
      this.instanceType = instanceType;
   }

   public int getCpuNum() {
      return cpuNum;
   }

   public void setCpuNum(int cpuNum) {
      this.cpuNum = cpuNum;
   }

   public int getMemCapacityMB() {
      return memCapacityMB;
   }

   public void setMemCapacityMB(int memCapacityMB) {
      this.memCapacityMB = memCapacityMB;
   }

   public StorageRead getStorage() {
      return storage;
   }

   public void setStorage(StorageRead storage) {
      this.storage = storage;
   }

   public List<String> getRpNames() {
      return rpNames;
   }

   public void setRpNames(List<String> rpNames) {
      this.rpNames = rpNames;
   }

   public boolean isHaFlag() {
      return haFlag;
   }

   public void setHaFlag(boolean haFlag) {
      this.haFlag = haFlag;
   }

   public GroupType getGroupType() {
      return groupType;
   }

   public void setGroupType(GroupType groupType) {
      this.groupType = groupType;
   }

   public List<VcCluster> getVcClusters() {
      return vcClusters;
   }

   public void setVcClusters(List<VcCluster> vcClusters) {
      this.vcClusters = vcClusters;
   }
}
