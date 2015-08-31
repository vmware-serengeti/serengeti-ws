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
package com.vmware.bdd.apitypes;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;

import com.vmware.bdd.utils.*;

import java.io.Serializable;
import java.util.*;


/**
 * Cluster creation spec
 */
public class NodeGroupAdd implements Serializable {

//   @Expose
//   private String name;
   @Expose
   @SerializedName("groups")
   private NodeGroupCreate[] nodeGroups;

   // This means whether this object already contains the cluster definition.
   // If not, will load the default spec file.
   private Boolean specFile = false;

   public NodeGroupAdd() {
   }

   public NodeGroupAdd(NodeGroupAdd cluster) {
//      this.name = cluster.name;
      this.nodeGroups = cluster.nodeGroups;
   }

//
//   @RestRequired
//   public String getName() {
//      return name;
//   }
//
//   public void setName(String name) {
//      this.name = name;
//   }

   public NodeGroupCreate[] getNodeGroups() {
      return nodeGroups;
   }

   public void setNodeGroups(NodeGroupCreate[] nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public Boolean isSpecFile() {
      return specFile;
   }

   public void setSpecFile(Boolean specFile) {
      this.specFile = specFile;
   }

   public boolean validateNodeGroupPlacementPolicies(
         List<String> failedMsgList, List<String> warningMsgList) {
      boolean valid = true;

      Map<String, NodeGroupCreate> allGroups =
            new TreeMap<String, NodeGroupCreate>();

      if (getNodeGroups() == null) {
         return valid;
      }

      for (NodeGroupCreate nodeGroupCreate : getNodeGroups()) {
         allGroups.put(nodeGroupCreate.getName(), nodeGroupCreate);
      }

      return valid;
   }

   public int totalInstances() {
      int num = 0;

      for (int i = 0; i < nodeGroups.length; i++) {
         num += nodeGroups[i].getInstanceNum();
      }

      return num;
   }

   public NodeGroupCreate getNodeGroup(String name) {
      AuAssert.check(name != null);

      for (NodeGroupCreate nodeGroup : this.nodeGroups) {
         if (nodeGroup.getName().equals(name)) {
            return nodeGroup;
         }
      }
      return null;
   }

   /**
    * Validate nodeGroupCreates member formats and values in the ClusterCreate.
    */
   public void validateNodeGroupAdd(List<String> failedMsgList,
                                     List<String> warningMsgList) {
      // Find NodeGroupCreate array from current ClusterCreate instance.
      NodeGroupCreate[] nodeGroupCreates = getNodeGroups();
      AuAssert.check(nodeGroupCreates != null && nodeGroupCreates.length > 0);

      validateNodeGroupNames();
      // check placement policies
      validateNodeGroupPlacementPolicies(failedMsgList, warningMsgList);

      // check supported storage type: LOCAL/SHARED/TEMPFS For tempfs
      // relationship: if a compute node has
      // strict association with a data node, its disk type can be set to
      // "TEMPFS". Otherwise, it is not
      // allowed to use tempfs as the disk type.
      validateStorageType(failedMsgList);

      // check node group's swapRatio
      validateSwapRatio(nodeGroupCreates, failedMsgList);

      for (NodeGroupCreate nodeGroupCreate : nodeGroupCreates) {
         // check node group's instanceNum
         checkInstanceNum(nodeGroupCreate, failedMsgList);
         // check CPU number and memory capacity
         checkCPUAndMemory(nodeGroupCreate, failedMsgList, warningMsgList);
      }
      if (!warningMsgList.isEmpty()
              && !warningMsgList.get(0).startsWith("Warning: ")) {
         warningMsgList.set(0, "Warning: " + warningMsgList.get(0));
      }
   }

   /**
    * Check if any compute only node group exists.
    */
   public boolean containsComputeOnlyNodeGroups(SoftwareManager softwareManager) {
      for (NodeGroupCreate nodeGroup : this.getNodeGroups()) {
         if (softwareManager.isComputeOnlyRoles(nodeGroup.getRoles())) {
            return true;
         }
      }
      return false;
   }

   private void checkCPUAndMemory(NodeGroupCreate nodeGroup,
         List<String> failedMsgList, List<String> warningMsgList) {
      Integer cpuNum = nodeGroup.getCpuNum();
      Integer memCap = nodeGroup.getMemCapacityMB();
      if (cpuNum != null && cpuNum <= 0) {
         failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
               .append(".").append("cpuNum=").append(cpuNum).append(".")
               .toString());
      }
      if (memCap != null) {
         if (memCap <= 0) {
            failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
                  .append(".").append("memCapacityMB=").append(memCap)
                  .append(".").toString());
         } else {
            // make VM memory value devisible by 4
//            makeVmMemoryDivisibleBy4(nodeGroup, warningMsgList);
         }
      }
   }

   private boolean checkInstanceNum(NodeGroupCreate nodeGroup,
         List<String> failedMsgList) {
      boolean validated = true;
      if (nodeGroup.getInstanceNum() < 0) {
         validated = false;
         collectInstanceNumInvalidateMsg(nodeGroup, failedMsgList);
      }
      return validated;
   }

   private void collectInstanceNumInvalidateMsg(NodeGroupCreate nodeGroup,
         List<String> failedMsgList) {
      failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
            .append(".").append("instanceNum=")
            .append(nodeGroup.getInstanceNum()).append(".").toString());
   }

   public void validateNodeGroupNames() {
      if (nodeGroups != null && nodeGroups.length > 0) {
         StringBuffer invalidNodeGroupNames = new StringBuffer();
         for (NodeGroupCreate nodeGroup : nodeGroups) {
            if (CommonUtil.isBlank(nodeGroup.getName())
                  || !CommonUtil.validateNodeGroupName(nodeGroup.getName())) {
               invalidNodeGroupNames.append("'").append(nodeGroup.getName())
                     .append("'").append(",");
            }
         }
         if (invalidNodeGroupNames.length() > 0) {
            invalidNodeGroupNames.delete(invalidNodeGroupNames.length() - 1,
                  invalidNodeGroupNames.length());
            throw ClusterConfigException
                  .NODE_GROUP_NAME_IS_INVALID(invalidNodeGroupNames.toString());
         }
      } else {
         throw ClusterConfigException.NODE_GROUP_NOT_EXISTING();
      }
   }

   public void validateStorageType(List<String> failedMsgList) {
      for (NodeGroupCreate nodeGroupCreate : getNodeGroups()) {
         StorageRead storageDef = nodeGroupCreate.getStorage();
         if (storageDef != null) {
            String storageType = storageDef.getType();

            if (storageType != null) {
               storageType = storageType.toUpperCase();
               // only support storage type of TEMPFS/LOCAL/SHARED
               if (!storageType.equals(DatastoreType.TEMPFS.toString())
                       && !storageType.equals(DatastoreType.LOCAL.toString())
                       && !storageType.equals(DatastoreType.SHARED.toString())) {
                  failedMsgList.add("Invalid storage type " + storageType
                          + ". " + Constants.STORAGE_TYPE_ALLOWED);
               } else if (storageType.equals(DatastoreType.TEMPFS.toString())) {// tempfs disk type
                  failedMsgList.add(Constants.TEMPFS_NOT_ALLOWED);

               }
            }
         }
      }
   }

   private void validateSwapRatio(NodeGroupCreate[] nodeGroups,
                                  List<String> failedMsgList) {
      boolean validated = true;
      StringBuilder invalidNodeGroupNames = new StringBuilder();
      for (NodeGroupCreate nodeGroup : nodeGroups) {
         if (nodeGroup.getSwapRatio() <= 0) {
            validated = false;
            invalidNodeGroupNames.append(nodeGroup.getName()).append(",");
         }
      }
      if (!validated) {
         StringBuilder errorMsgBuff = new StringBuilder();
         invalidNodeGroupNames.delete(invalidNodeGroupNames.length() - 1,
                 invalidNodeGroupNames.length());
         failedMsgList
                 .add(errorMsgBuff
                         .append("The 'swapRatio' must be greater than 0 in group ")
                         .append(invalidNodeGroupNames.toString()).append(".")
                         .toString());
      }
   }

}
