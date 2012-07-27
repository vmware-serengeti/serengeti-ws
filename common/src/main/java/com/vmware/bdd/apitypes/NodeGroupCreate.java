/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.apitypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
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
   @SerializedName("placement_policies")
   private PlacementPolicy placementPolicies;
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
   @SerializedName("cluster_configuration")
   private Map<String, Object> configuration;

   public NodeGroupCreate() {

   }

   public NodeGroupCreate(NodeGroupCreate group) {
      this.cpuNum = group.cpuNum;
      this.groupType = group.groupType;
      this.haFlag = group.haFlag;
      this.instanceNum = group.instanceNum;
      this.instanceType = group.instanceType;
      this.placementPolicies = group.placementPolicies;
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
      if (configuration == null) {
         configuration = new HashMap<String, Object>();
      }
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

   public PlacementPolicy getPlacementPolicies() {
      return placementPolicies;
   }

   public void setPlacementPolicies(PlacementPolicy placementPolicies) {
      this.placementPolicies = placementPolicies;
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

   private Integer getHostNum() {
      Integer hostNumber = null;
      PlacementPolicy policies = getPlacementPolicies();
      if (policies != null && policies.getInstancePerHost() != null &&
          policies.getInstancePerHost() > 0) {
         if (getInstanceNum() % policies.getInstancePerHost() == 0) {
            hostNumber = getInstanceNum() / policies.getInstancePerHost();
         } else {
            hostNumber = -1;
         }
      }

      return hostNumber;
   }

   public boolean validatePlacementPolicies(Map<String, NodeGroupCreate> groups,
         List<String> failedMsgList) {
      boolean valid = true;
      PlacementPolicy policies = getPlacementPolicies();
      if (policies != null) {
         if (policies.getInstancePerHost() != null) {
            if (policies.getInstancePerHost() <= 0) {
               valid = false;
               failedMsgList.add(new StringBuilder().append(getName())
                     .append(".placementPolicies.instancePerHost=")
                     .append(policies.getInstancePerHost()).toString());
            } else if (getHostNum() < 0) {
               valid = false;
               failedMsgList.add(new StringBuilder().append(getName())
                     .append(".placementPolicies.instancePerHost=")
                     .append(policies.getInstancePerHost())
                     .append(" is invalid divisor").toString());
            }
         }
         if (policies.getGroupAssociations() != null) {
            // only support 1 group association now
            if (policies.getGroupAssociations().size() != 1) {
               valid = false;
               failedMsgList.add(new StringBuilder().append(getName())
                     .append(".placementPolicies.groupAssociations.size should be 1")
                     .toString());
            } else {
               GroupAssociation a = policies.getGroupAssociations().get(0);

               if (a.getType() == null) {
                  a.setType(GroupAssociationType.WEAK); // set to default
               }

               if (a.getReference() == null) {
                  valid = false;
                  failedMsgList.add(new StringBuilder().append(getName())
                        .append(".placementPolicies.groupAssociations[0].reference not set").toString());
               } else if (a.getReference().equals(getName())) {
                  valid = false;
                  failedMsgList.add(new StringBuilder()
                        .append(getName())
                        .append(".placementPolicies.groupAssociations[0] refers to itself").toString());
               } else if (!groups.containsKey(a.getReference())) {
                  valid = false;
                  failedMsgList.add(new StringBuilder()
                        .append(getName())
                        .append(".placementPolicies.groupAssociations[0] refers to invalid node group ")
                        .append(a.getReference()).toString());
               } else {
                  /*
                   *  This is normal case, do more checks.
                   *  
                   *  If STRICT is specified, the host number of the current node
                   *  group should not be larger than the referenced one.
                   */
                  if (a.getType() == GroupAssociationType.STRICT) {
                     /*
                      * For the referenced node group, we assume the max node number equals to
                      * instance number when instance per host is unspecified. For the reference
                      * node group, we assume the min node number is 1 when instance per host is
                      * unspecified. This rule follows the underlying placement algorithm.
                      */
                     int hostNum = 1;
                     int refHostNum = groups.get(a.getReference()).getInstanceNum();
                     if (getHostNum() != null) {
                        hostNum = getHostNum();
                     }
                     if (groups.get(a.getReference()).getHostNum() != null) {
                        refHostNum = groups.get(a.getReference()).getHostNum();
                     }
                     if (hostNum > refHostNum) {
                        valid = false;
                        failedMsgList.add(new StringBuilder()
                              .append(getName())
                              .append(".placementPolicies.groupAssociations[0] requires " +
                              		"more hosts than the referenced node group ")
                              .append(a.getReference()).toString());
                     }
                  }
   
                  // current implementation only support sum(in/out degree) <= 1
                  PlacementPolicy refPolicies = groups.get(a.getReference())
                        .getPlacementPolicies();
                  if (refPolicies != null && refPolicies.getGroupAssociations() != null && 
                        !refPolicies.getGroupAssociations().isEmpty()) {
                     valid = false;
                     failedMsgList.add(new StringBuilder()
                           .append(getName())
                           .append(".placementPolicies.groupAssociations[0] refers to node group ")
                           .append(a.getReference())
                           .append(" which also has reference(s)").toString());
                  }
               }
            }
         }
      }

      return valid;
   }
}
