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

package com.vmware.bdd.placement.interfaces;

import java.util.List;
import java.util.Map;

import com.google.gson.internal.Pair;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.entity.VirtualGroup;
import com.vmware.bdd.placement.entity.VirtualNode;

/**
 * placement planner interface
 * 
 * planner has states, so one planner for one cluster. Do not mi
 * 
 * @author tli
 * 
 */
public interface IPlacementPlanner {

   /**
    * examine the cluster and find out VMs that violate placement rules and thus
    * should be removed before placement
    * 
    * This method do not require states from a planner, so it can be called from
    * a planner that does not invoke init before.
    * 
    * @param cluster
    * @param existedVms
    *           existing VMs in the clusters
    */
   public List<BaseNode> getBadNodes(ClusterCreate cluster,
         List<BaseNode> existedNodes);

   /**
    * initialize the planner with cluster spec and existed VMs. This step is
    * required before calling any methods below
    * 
    * @param cluster
    * @param template
    * @param existedNodes
    * @param racks
    */
   public void init(ClusterCreate cluster, BaseNode template,
         List<BaseNode> existedNodes, Map<String, String> hostToRackMaps);

   /**
    * create base node, populate it with properties from Node Group and template
    * node, e.g., cpu/mem, network, vm folder, properties that are defined in
    * cluster spec
    * 
    * @param nodeGroup
    * @param index
    * @param template
    * @return
    */
   public BaseNode getBaseNode(ClusterCreate cluster,
         NodeGroupCreate nodeGroup, int index);

   /**
    * pre-process the cluster architecture, put tightly associated node groups
    * into one virtual node group, and combine VMs that should stay on the same
    * host as one virtual node
    * 
    * @return list of virtual groups
    */
   public List<VirtualGroup> getVirtualGroups(List<BaseNode> existedNodes);

   /**
    * select a host from a candidate host (with enough resource) list
    * 
    * @param vNode
    * @param candidates
    * @return
    */
   public AbstractHost selectHost(VirtualNode vNode,
         List<AbstractHost> candidates);

   /**
    * select a vc rp for the virtual node based on the selected host
    * 
    * this methods will try to place nodes from the same node group onto the
    * same vc resource pool
    * 
    * @param vGroup
    * @param cluster
    * @return
    */
   public Pair<String, String> selectVcRp(BaseNode node, AbstractHost host);
   
   /**
    * get the rack this group stays
    * 
    * @param groupName
    * @return
    */
   public List<String> getTargetRacks(String groupName);
}
