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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.CommonUtil;

/**
 * Cluster get output
 */
public class ClusterRead implements Comparable<ClusterRead>{
   public enum ClusterStatus {
      RUNNING, PROVISIONING, PROVISION_ERROR, UPGRADING, UPDATING, DELETING, 
      STOPPED, ERROR, STOPPING, STARTING, CONFIGURING, CONFIGURE_ERROR, NA, 
      VHM_RUNNING, VMRECONFIGURING
   }

   @Expose
   private String name;
   private String externalHDFS;
   @Expose
   private String distro;

   @Expose
   @SerializedName("instance_num")
   private int instanceNum;

   @Expose
   private ClusterStatus status;

   @Expose
   @SerializedName("rack_topology_policy")
   private TopologyType topologyPolicy;

   @Expose
   @SerializedName("groups")
   private List<NodeGroupRead> nodeGroups;

   private boolean nodeGroupSorted;

   public ClusterRead() {

   }

   public ClusterRead(String name, String distro, int instanceNum,
         ClusterStatus status) {
      super();
      this.name = name;
      this.distro = distro;
      this.instanceNum = instanceNum;
      this.status = status;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getExternalHDFS() {
      return externalHDFS;
   }

   public void setExternalHDFS(String externalHDFS) {
      this.externalHDFS = externalHDFS;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public TopologyType getTopologyPolicy() {
      return topologyPolicy;
   }

   public void setTopologyPolicy(TopologyType topologyPolicy) {
      this.topologyPolicy = topologyPolicy;
   }

   public int getInstanceNum() {
      return instanceNum;
   }

   public void setInstanceNum(int instanceNum) {
      this.instanceNum = instanceNum;
   }

   public ClusterStatus getStatus() {
      return status;
   }

   public void setStatus(ClusterStatus status) {
      this.status = status;
   }

   public List<NodeGroupRead> getNodeGroups() {
      NodeGroupReadComparactor comparactor = new NodeGroupReadComparactor();
      //Note: if node groups will be modified by server, please consider collections.unmodifiedList() first. 
      if (!nodeGroupSorted) {
         Collections.sort(nodeGroups, comparactor);
         nodeGroupSorted = true;
      }
      return nodeGroups;
   }

   public void setNodeGroups(List<NodeGroupRead> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   /*
    * Validate the limit,make sure the specified node group is a compute only node group.
    * If user have not specified the node group name,the cluster must contain compute only node.   
    */
   public boolean validateLimit(String nodeGroupName, List<String>... nodeGroupNames) {
      if (!CommonUtil.isBlank(nodeGroupName)) {
         List<NodeGroupRead> nodeGroups = getNodeGroups();
         if(nodeGroups != null && !nodeGroups.isEmpty()){
            List<String> invalidNodeGroup = new ArrayList<String>();
            if(nodeGroupNames != null && nodeGroupNames.length > 0){
               nodeGroupNames[0].add(nodeGroupName);
            }
            NodeGroupRead nodeGroup = matchNodeGroupByName(nodeGroups,nodeGroupName);
            if (nodeGroup == null) {
                invalidNodeGroup.add(nodeGroupName);
            } else if (nodeGroup.getRoles() == null || nodeGroup.getRoles().size() > 2
                || !nodeGroup.getRoles().contains(HadoopRole.HADOOP_TASKTRACKER.toString())
                || (nodeGroup.getRoles().size() == 2 && !nodeGroup.getRoles().contains(HadoopRole.TEMPFS_CLIENT_ROLE.toString()))
                ) {
                   invalidNodeGroup.add(nodeGroupName);
            }
            if (!invalidNodeGroup.isEmpty()) {
               System.out.println("Adjustment failed: The specified node group is not a compute only node group.");
               return false;
            }
         } else {
            System.out.println("Adjustment failed: There is not node group under the cluster " + getName() + " !");
            return false;
         }
      } else {
         int count = 0;
         for(NodeGroupRead nodeGroup : getNodeGroups()) {
            if (nodeGroup.getRoles() != null && nodeGroup.getRoles().contains(HadoopRole.HADOOP_TASKTRACKER.toString()) 
                && (nodeGroup.getRoles().size() == 1 || (nodeGroup.getRoles().size() == 2 && nodeGroup.getRoles().contains(HadoopRole.TEMPFS_CLIENT_ROLE.toString())))) {
               if(nodeGroupNames != null && nodeGroupNames.length > 0){
                  nodeGroupNames[0].add(nodeGroup.getName());
               }
               count ++;
            }
         }
         if(count == 0){
            System.out.println("Adjustment failed: There's no compute only nodes in the cluster.");
            return false;
         }
      }
      return true;
   }

   private NodeGroupRead matchNodeGroupByName(List<NodeGroupRead> nodeGroups,
         String nodeGroupName) {
      NodeGroupRead nodeGoupRead = null;
      for (NodeGroupRead nodeGroup : nodeGroups) {
         if (nodeGroupName.trim().equals(nodeGroup.getName())) {
            nodeGoupRead = nodeGroup;
            break;
         }
      }
      return nodeGoupRead;
   }

   /**
    * Compare the order of node groups according to their roles
    *
    *
    */
   private class NodeGroupReadComparactor implements Comparator<NodeGroupRead> {
      private final String[] roleOrders = { "namenode", "jobtracker",
            "hbase_master", "zookeeper", "datanode", "tasktracker",
            "regionserver", "hadoop_client", "hbase_client", "pig", "hive" };

      @Override
      public int compare(NodeGroupRead ng1, NodeGroupRead ng2) {
         if (ng1 == ng2) {
            return 0;
         }
         //null elements will be sorted behind the list
         if (ng1 == null) {
            return 1;
         } else if (ng2 == null) {
            return -1;
         }

         List<String> ng1Roles = ng1.getRoles();
         List<String> ng2Roles = ng2.getRoles();

         return compareBasedOnRoles(ng1Roles, ng2Roles);
      }

      private int compareBasedOnRoles(List<String> ng1Roles,
            List<String> ng2Roles) {
         if (ng1Roles == ng2Roles) {
            return 0;
         }
         if (ng1Roles == null) {
            return 1;
         } else if (ng2Roles == null) {
            return -1;
         }
         int ng1RolePos = findNodeGroupRole(ng1Roles);
         int ng2RolePos = findNodeGroupRole(ng2Roles);
         if (ng1RolePos < ng2RolePos) {
            return -1; 
         } else if (ng1Roles == ng2Roles) {
            return 0;
         } else {
            return 1;
         }
      }

      private int findNodeGroupRole(List<String> ng1Roles) {
         String ngRolesString = ng1Roles.toString();
         for (int i = 0; i < roleOrders.length; i++) {
            if (ngRolesString.contains(roleOrders[i])) {
               return i;
            }
         }
         return roleOrders.length;
      }
   }

   @Override
   public int compareTo(ClusterRead cluster) {
      if (CommonUtil.isBlank(cluster.getName())) {
         return 1;
      }
      return this.getName().compareTo(cluster.getName());
   }

}
