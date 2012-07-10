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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.NodeGroup.PlacePolicy;

/**
 * Nodegroup get output. Node group is a set of nodes with same properties, for
 * example, data nodes in hadoop
 */
public class NodeGroupRead {
   @Expose
   private String name;
   @Expose
   private List<String> roles;

   @Expose
   @SerializedName("cpu")
   private int cpuNum;

   @Expose
   @SerializedName("memory")
   private int memCapacityMB;

   @Expose
   private NetworkRead networking;
   @Expose
   private StorageRead storage;

   @Expose
   @SerializedName("instance_num")
   private int instanceNum;

   @Expose
   private List<NodeRead> instances;

   @Expose
   @SerializedName("place_policy")
   private PlacePolicy placePolicy;

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

   public List<NodeRead> getInstances() {
      return instances;
   }

   public void setInstances(List<NodeRead> instances) {
      this.instances = instances;
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

   public int getInstanceNum() {
      return instanceNum;
   }

   public void setInstanceNum(int instanceNum) {
      this.instanceNum = instanceNum;
   }

   public NetworkRead getNetworking() {
      return networking;
   }

   public void setNetworking(NetworkRead networking) {
      this.networking = networking;
   }

   public StorageRead getStorage() {
      return storage;
   }

   public void setStorage(StorageRead storage) {
      this.storage = storage;
   }

   public PlacePolicy getPlacePolicy() {
      return placePolicy;
   }

   public void setPlacePolicy(PlacePolicy placePolicy) {
      this.placePolicy = placePolicy;
   }
}
