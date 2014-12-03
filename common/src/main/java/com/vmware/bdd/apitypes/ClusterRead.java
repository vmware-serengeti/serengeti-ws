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

import java.util.List;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

/**
 * Cluster get output
 */
public class ClusterRead implements Comparable<ClusterRead> {
   @Expose
   private String name;
   private String externalHDFS;
   private String externalMapReduce;
   private String externalNamenode;
   private String externalSecondaryNamenode;
   private Set<String> externalDatanodes;

   private String localRepoURL;

   @Expose
   private String distro;

   @Expose
   @SerializedName("app_manager")
   private String appManager;

   @Expose
   @SerializedName("distro_vendor")
   private String distroVendor;

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

   @Expose
   @SerializedName("resourcepools")
   private List<ResourcePoolRead> resourcePools;

   private Boolean automationEnable;

   private int vhmMinNum;

   private int vhmMaxNum;

   private Integer vhmTargetNum;

   private String version;

   @Expose
   @SerializedName("disk_priority")
   private Priority ioShares;

   //   private boolean nodeGroupSorted;

   private boolean dcSeperation;

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

   public void setVersion(String version) {
      this.version = version;
   }

   public String getVersion() {
      if (version == null || version.isEmpty()) {
         return Constants.NEED_UPGRADE;
      }
      return version;
   }

   public String getExternalHDFS() {
      return externalHDFS;
   }

   public void setExternalHDFS(String externalHDFS) {
      this.externalHDFS = externalHDFS;
   }

   public String getAppManager() {
      return appManager;
   }

   public void setAppManager(String appManager) {
      this.appManager = appManager;
   }

   public String getExternalMapReduce() {
      return externalMapReduce;
   }

   public void setExternalMapReduce(String externalMapReduce) {
      this.externalMapReduce = externalMapReduce;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public String getDistroVendor() {
      return distroVendor;
   }

   public void setDistroVendor(String distroVendor) {
      this.distroVendor = distroVendor;
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

   public Priority getIoShares() {
      return ioShares;
   }

   public void setIoShares(Priority ioShares) {
      this.ioShares = ioShares;
   }

   public List<NodeGroupRead> getNodeGroups() {
      return nodeGroups;
   }

   public NodeGroupRead getNodeGroupByName(String nodeGroupName) {
      List<NodeGroupRead> nodeGroups = this.getNodeGroups();
      if (nodeGroups != null) {
         for (NodeGroupRead ng : nodeGroups) {
            if (ng.getName().equals(nodeGroupName)) {
               return ng;
            }
         }
      }
      return null;
   }

   public void setNodeGroups(List<NodeGroupRead> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public List<ResourcePoolRead> getResourcePools() {
      return resourcePools;
   }

   public void setResourcePools(List<ResourcePoolRead> resourcePools) {
      this.resourcePools = resourcePools;
   }

   /*
    * Validate the manual elastic parameters, make sure the specified node group is a compute only node group.
    * If user have not specified the node group name, the cluster must contain compute only node.
    */
   public boolean validateSetManualElasticity(List<String>... nodeGroupNames) {
      List<NodeGroupRead> nodeGroups = getNodeGroups();

      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         int count = 0;
         for (NodeGroupRead nodeGroup : getNodeGroups()) {
            if (nodeGroup.isComputeOnly()) {
               if (nodeGroupNames != null && nodeGroupNames.length > 0) {
                  nodeGroupNames[0].add(nodeGroup.getName());
               }
               count++;
            }
         }
         if (count == 0) {
            return false;
         }
      } else {
         return false;
      }
      return true;
   }

   public int retrieveComputeNodeNum() {
      List<NodeGroupRead> nodeGroups = getNodeGroups();
      int count = 0;

      if (nodeGroups != null && !nodeGroups.isEmpty()) {
         for (NodeGroupRead nodeGroup : getNodeGroups()) {
            if (nodeGroup.isComputeOnly()) {
               count = count + nodeGroup.getInstanceNum();
            }
         }
      }
      return count;
   }

   /*
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

   *//**
    * Compare the order of node groups according to their roles
    *
    *
    */
   /*
   private class NodeGroupReadComparactor implements Comparator<NodeGroupRead> {
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

   private int compareBasedOnRoles(List<String> ng1Roles, List<String> ng2Roles) {
      if (ng1Roles == ng2Roles) {
         return 0;
      }
      if (ng1Roles == null || ng1Roles.isEmpty()) {
         return 1;
      } else if (ng2Roles == null || ng2Roles.isEmpty()) {
         return -1;
      }

      int ng1RolePos = findNodeGroupRoleMinIndex(ng1Roles);
      int ng2RolePos = findNodeGroupRoleMinIndex(ng2Roles);
      if (ng1RolePos < ng2RolePos) {
         return -1;
      } else if (ng1RolePos == ng2RolePos) {
         return 0;
      } else {
         return 1;
      }
   }

   private int findNodeGroupRoleMinIndex(List<String> ngRoles) {
      Collections.sort(ngRoles, new RoleComparactor());
      HadoopRole role = HadoopRole.fromString(ngRoles.get(0));
      return (null != role) ? role.ordinal() : -1;
   }
   }
   */
   @Override
   public int compareTo(ClusterRead cluster) {
      if (CommonUtil.isBlank(cluster.getName())) {
         return 1;
      }
      return this.getName().compareTo(cluster.getName());
   }

   public Boolean getAutomationEnable() {
      return automationEnable;
   }

   public void setAutomationEnable(Boolean automationEnable) {
      this.automationEnable = automationEnable;
   }

   public String retrieveVhmMinNum() {
      if (vhmMinNum == -1) {
         return "Unset";
      } else {
         return Integer.toString(vhmMinNum);
      }
   }

   public int getVhmMinNum() {
      return vhmMinNum;
   }

   public void setVhmMinNum(int vhmMinNum) {
      this.vhmMinNum = vhmMinNum;
   }

   public String retrieveVhmMaxNum() {
      if (vhmMaxNum == -1) {
         return "Unset";
      } else {
         return Integer.toString(vhmMaxNum);
      }
   }

   public int getVhmMaxNum() {
      return vhmMaxNum;
   }

   public void setVhmMaxNum(int vhmMaxNum) {
      this.vhmMaxNum = vhmMaxNum;
   }

   public String retrieveVhmTargetNum() {
      if (vhmTargetNum == null || vhmTargetNum == -1) {
         return "N/A";
      } else {
         return vhmTargetNum.toString();
      }
   }

   public Integer getVhmTargetNum() {
      return vhmTargetNum;
   }

   public void setVhmTargetNum(Integer vhmTargetNum) {
      this.vhmTargetNum = vhmTargetNum;
   }

   /*
    * Check if invoke sync or async rest apis: if manual is set and targetNum is not empty;
    * or current elasticity mode is manual and targetNum is not empty, we need to use async
    * rest api since start/stop vms will take some time to complete.
    */
   public boolean needAsyncUpdateParam(ElasticityRequestBody requestBody) {
      Boolean enableAuto = requestBody.getEnableAuto();
      if (enableAuto != null && !enableAuto && targetNumNotEmpty(requestBody)) { //set manual and targetNum != null
         return true;
      } else if (enableAuto == null) { //not set auto
         Boolean existingElasticityMode = this.getAutomationEnable();
         if (existingElasticityMode != null && !existingElasticityMode
               && requestBody.getActiveComputeNodeNum() != null) { // existing is Manual and targetNum != null
            return true;
         }
      }
      return false;
   }

   private boolean targetNumNotEmpty(ElasticityRequestBody requestBody) {
      if (requestBody.getActiveComputeNodeNum() != null
            || this.getVhmTargetNum() != null) {
         return true;
      }
      return false;
   }

   public boolean isDcSeperation() {
      return dcSeperation;
   }

   public void setDcSeperation(boolean dcSeperation) {
      this.dcSeperation = dcSeperation;
   }

   public boolean validateSetParamParameters(Integer targetComputeNodeNum,
         Integer minComputeNodeNum, Integer maxComputeNodeNum) {

      int deployedComputeNodeNum = retrieveComputeNodeNum();
      int vhmMinNum = getVhmMinNum();
      int vhmMaxNum = getVhmMaxNum();

      String minComputeNodeNumStr = "";
      if (minComputeNodeNum == null) {
         if (vhmMinNum != -1) {
            minComputeNodeNumStr = " (" + vhmMinNum + ")";
         }
      } else if (minComputeNodeNum != -1) {
         minComputeNodeNumStr = " (" + minComputeNodeNum + ")";
      }

      String maxComputeNodeNumStr = "";
      if (maxComputeNodeNum == null) {
         if (vhmMaxNum != -1) {
            maxComputeNodeNumStr = " (" + vhmMaxNum + ")";
         }
      } else if (maxComputeNodeNum != -1) {
         maxComputeNodeNumStr = " (" + maxComputeNodeNum + ")";
      }

      //validate the input of minComputeNodeNum
      if (minComputeNodeNum != null && minComputeNodeNum < -1) {
         throw BddException.INVALID_MIN_COMPUTE_NODE_NUM(
               minComputeNodeNum.toString(),
               Integer.toString(deployedComputeNodeNum), maxComputeNodeNumStr);
      }

      //validate the input of maxComputeNodeNum
      if (maxComputeNodeNum != null && maxComputeNodeNum < -1) {
         throw BddException.INVALID_MAX_COMPUTE_NODE_NUM(
               maxComputeNodeNum.toString(),
               Integer.toString(deployedComputeNodeNum), minComputeNodeNumStr);
      }

      //validate the input of targetComputeNodeNum
      if (targetComputeNodeNum != null && targetComputeNodeNum < 0) {
         throw BddException.INVALID_TARGET_COMPUTE_NODE_NUM(
               targetComputeNodeNum.toString(),
               Integer.toString(deployedComputeNodeNum));
      }

      //validate min, max, targetComputeNodeNum should be less than deployed computeNodeNum
      if (minComputeNodeNum != null
            && minComputeNodeNum > deployedComputeNodeNum) {
         throw BddException.INVALID_MIN_COMPUTE_NODE_NUM(
               minComputeNodeNum.toString(),
               Integer.toString(deployedComputeNodeNum), maxComputeNodeNumStr);
      }
      if (maxComputeNodeNum != null
            && maxComputeNodeNum > deployedComputeNodeNum) {
         throw BddException.INVALID_MAX_COMPUTE_NODE_NUM(
               maxComputeNodeNum.toString(),
               Integer.toString(deployedComputeNodeNum), minComputeNodeNumStr);
      }
      if (targetComputeNodeNum != null
            && targetComputeNodeNum > deployedComputeNodeNum) {
         throw BddException.INVALID_TARGET_COMPUTE_NODE_NUM(
               targetComputeNodeNum.toString(),
               Integer.toString(deployedComputeNodeNum));
      }

      //validate minComputeNode <= maxComputeNode
      if ((minComputeNodeNum != null && minComputeNodeNum != -1
            && maxComputeNodeNum != null && maxComputeNodeNum != -1 && minComputeNodeNum > maxComputeNodeNum)
            || (minComputeNodeNum != null && minComputeNodeNum != -1
                  && maxComputeNodeNum == null && vhmMaxNum != -1 && minComputeNodeNum > vhmMaxNum)
            || (minComputeNodeNum == null && vhmMinNum != -1
                  && maxComputeNodeNum != null && maxComputeNodeNum != -1 && vhmMinNum > maxComputeNodeNum)) {
         if (minComputeNodeNum != null && minComputeNodeNum != -1) {
            throw BddException.INVALID_MIN_COMPUTE_NODE_NUM(
                  minComputeNodeNum.toString(),
                  Integer.toString(deployedComputeNodeNum),
                  maxComputeNodeNumStr);
         } else {
            throw BddException.INVALID_MAX_COMPUTE_NODE_NUM(
                  maxComputeNodeNum.toString(),
                  Integer.toString(deployedComputeNodeNum),
                  minComputeNodeNumStr);
         }

      }
      return true;
   }

   public String getLocalRepoURL() {
      return localRepoURL;
   }

   public void setLocalRepoURL(String localRepoURL) {
      this.localRepoURL = localRepoURL;
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

}
