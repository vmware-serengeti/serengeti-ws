/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.placement.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractCluster;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.AuAssert;

/**
 * a virtual group combines node groups that both have the instance_per_host
 * constraint and are strictly associated (should stay on the same set of hosts)
 *
 * @author tli
 *
 */
public class VirtualGroup implements Comparable<VirtualGroup> {

   private ClusterCreate cluster;

   private List<NodeGroupCreate> nodeGroups;

   private List<VirtualNode> vNodes;

   // at least one node group in this virtual group is referenced by others
   // which means it should be placed first
   private boolean referred;

   private String referToGroup;

   // strict associated to another group?
   private Boolean strictAssociated;

   public VirtualGroup(ClusterCreate cluster) {
      super();

      this.cluster = cluster;
      this.nodeGroups = new ArrayList<NodeGroupCreate>();
      this.vNodes = new ArrayList<VirtualNode>();
      this.referred = false;
   }

   public ClusterCreate getCluster() {
      return cluster;
   }

   public void setCluster(ClusterCreate cluster) {
      this.cluster = cluster;
   }

   public void addNodeGroup(NodeGroupCreate nodeGroup) {
      nodeGroups.add(nodeGroup);
   }

   public List<NodeGroupCreate> getNodeGroups() {
      return nodeGroups;
   }

   public void setNodeGroups(List<NodeGroupCreate> nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public void addVNode(VirtualNode vNode) {
      vNodes.add(vNode);
   }

   public void addVNodes(List<VirtualNode> vNodes) {
      vNodes.addAll(vNodes);
   }

   public List<VirtualNode> getvNodes() {
      return vNodes;
   }

   public void setvNodes(List<VirtualNode> vNodes) {
      this.vNodes = vNodes;
   }

   public boolean isReferred() {
      return referred;
   }

   public void setReferred(boolean referred) {
      this.referred = referred;
   }

   public String getReferToGroup() {
      return referToGroup;
   }

   public void setReferToGroup(String referToGroup) {
      this.referToGroup = referToGroup;
   }

   public boolean isStrictAssociated() {
      return strictAssociated == null ? false : strictAssociated;
   }

   public void setStrictAssociated(boolean strictAssociated) {
      this.strictAssociated = strictAssociated;
   }

   /**
    * get joint abstract cluster all node groups inside this virtual group
    *
    * @return
    */
   public List<AbstractCluster> getJointAbstractClusters() {
      Set<String> sharedVcClusterNames = null;

      // get shared vc cluster name between all node groups
      for (NodeGroupCreate nodeGroup : nodeGroups) {
         Set<String> vcClusterNameSet = new HashSet<String>();

         for (VcCluster vcCluster : nodeGroup.getVcClusters(cluster)) {
            vcClusterNameSet.add(vcCluster.getName());
         }

         if (sharedVcClusterNames == null) {
            sharedVcClusterNames = vcClusterNameSet;
         } else {
            sharedVcClusterNames.retainAll(vcClusterNameSet);
         }
      }

      if (sharedVcClusterNames == null || sharedVcClusterNames.size() == 0) {
         return null;
      }

      List<AbstractCluster> clusters = new ArrayList<AbstractCluster>();

      // transform (cluster name <-> rp name list) map to AbsractCluster object list
      for (String name : sharedVcClusterNames) {
         clusters.add(new AbstractCluster(name));
      }

      return clusters;
   }

   public int getTotalStorage() {
      int total = 0;

      for (VirtualNode node : this.vNodes) {
         total += node.getStorage();
      }
      return total;
   }

   public GroupRacks getGroupRacks() {
      /*
       * basic rule: if group A is strictly associated with group B, then A should follow
       * B's rack policy
       */
      NodeGroupCreate primary = getPrimaryGroup();
      if (primary.getPlacementPolicies() != null) {
         return primary.getPlacementPolicies().getGroupRacks();
      } else {
         return null;
      }
   }

   /**
    * if any node group inside this vGroup is strictly associated with another
    * node group, return that group. Otherwise, return itself.
    *
    * This method is used in Rack policy process
    *
    * @return
    */
   public NodeGroupCreate getPrimaryGroup() {
      /*
       *  mixed with multiple node groups, there must be a group that is strictly
       *  associated by others.
       */
      if (this.nodeGroups.size() > 1) {
         for (NodeGroupCreate nodeGroup : this.nodeGroups) {
            if (nodeGroup.getReferredGroup() == null) {
               // return the strictly associated group
               AuAssert.check(nodeGroup.getPlacementPolicies() != null);
               return nodeGroup;
            }
         }
      }

      /*
       * the rack policy in the slave node groups will be override by the primary group,
       * i.e., the group they strictly associated with.
       */
      if (this.referToGroup != null
            && Boolean.TRUE.equals(this.strictAssociated)) {
         return cluster.getNodeGroup(referToGroup);
      }

      return this.nodeGroups.get(0);
   }

   public boolean hasInstancePerHostPolicy() {
      if (nodeGroups.get(0).getPlacementPolicies() != null
            && nodeGroups.get(0).getPlacementPolicies().getInstancePerHost() != null)
         return true;

      return false;
   }

   @Override
   public int compareTo(VirtualGroup o) {
      if (this.getTotalStorage() > o.getTotalStorage())
         return 1;
      else if (this.getTotalStorage() == o.getTotalStorage())
         return -1;
      else
         return 0;
   }

   public List<String> getNodeGroupNames() {
      List<String> nodeGroupNames = new ArrayList<String>(nodeGroups.size());
      for (NodeGroupCreate nodeGroup : nodeGroups) {
         nodeGroupNames.add(nodeGroup.getName());
      }
      return nodeGroupNames;
   }
}
