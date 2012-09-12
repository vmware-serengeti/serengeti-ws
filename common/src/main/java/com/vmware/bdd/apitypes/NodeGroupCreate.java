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
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks.GroupRacksType;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.Constants;

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
   private String haFlag="off";
   @Expose
   @SerializedName("cluster_configuration")
   private Map<String, Object> configuration;

   @Expose
   @SerializedName("vm_folder_path")
   private String vmFolderPath;

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
      this.vmFolderPath = group.vmFolderPath;
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

   public String getHaFlag() {
      return haFlag;
   }

   public void setHaFlag(String haFlag) {
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

   public String getVmFolderPath() {
      return vmFolderPath;
   }

   public void setVmFolderPath(String vmFolderPath) {
      this.vmFolderPath = vmFolderPath;
   }

   public Integer calculateHostNum() {
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

   public boolean validatePlacementPolicies(ClusterCreate cluster, Map<String, NodeGroupCreate> groups,
         List<String> failedMsgList, List<String> warningMsgList) {
      boolean valid = true;
      PlacementPolicy policies = getPlacementPolicies();
      if (policies != null) {
         if (policies.getInstancePerHost() != null) {
            if (policies.getInstancePerHost() <= 0) {
               valid = false;
               failedMsgList.add(new StringBuilder().append(getName())
                     .append(".placementPolicies.instancePerHost=")
                     .append(policies.getInstancePerHost()).toString());
            } else if (calculateHostNum() < 0) {
               valid = false;
               failedMsgList.add(new StringBuilder().append(getName())
                     .append(".placementPolicies.instancePerHost=")
                     .append(policies.getInstancePerHost())
                     .append(" is not an exact divisor").toString());
            }
         }

         if (policies.getGroupRacks() != null) {
            if (cluster.getTopologyPolicy() == null) {
               warningMsgList.add("Warning: "
                     + Constants.PRACK_NO_TOPOLOGY_TYPE_SPECIFIED);
               cluster.setTopologyPolicy(TopologyType.NONE);
            }
            GroupRacks r = policies.getGroupRacks();
            if (r.getType() == null) {
               r.setType(GroupRacksType.ROUND_ROBIN);
            } else if (r.getType() == GroupRacksType.SAME_RACK
                  && r.getRacks().length != 1) {
               valid = false;
               failedMsgList.add(Constants.PRACK_SAME_RACK_WITH_WRONG_VALUES);
            }

            // warning if storage.type = SHARED
            if (getStorage() == null || getStorage().getType() == null
                  || getStorage().getType().equals(DatastoreType.SHARED.toString())) {
               warningMsgList.add("Warning: " + Constants.PRACK_WITH_SHARED_STORAGE);
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
                     if (policies.getGroupRacks() != null) {
                        warningMsgList.add("Warning: "
                              + Constants.PRACK_WITH_STRICT_ASSOCIATION);
                     }
                     int hostNum = 1;
                     int refHostNum = groups.get(a.getReference()).getInstanceNum();
                     if (calculateHostNum() != null) {
                        hostNum = calculateHostNum();
                     }
                     if (groups.get(a.getReference()).calculateHostNum() != null) {
                        refHostNum = groups.get(a.getReference()).calculateHostNum();
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
