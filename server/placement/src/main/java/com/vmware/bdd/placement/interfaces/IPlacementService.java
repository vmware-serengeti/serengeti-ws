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

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.spectypes.DiskSpec;


public interface IPlacementService {

   /**
    * execute the placement planning with given resource container
    * 
    * @param container
    * @param cluster
    * @param outOfSyncHots TODO
    * @return list of base node that have detailed provision attributes
    */
   public List<BaseNode> getPlacementPlan(IContainer container,
         ClusterCreate cluster, List<BaseNode> existedNodes, List<AbstractHost> outOfSyncHots);

   /**
    * examine the existing nodes and find out bad nodes that violate placement
    * policies
    * 
    * to resume from previous cluster creation failure, and get the new
    * placement plan, the cluster should be cleaned to remove bad nodes first
    * 
    * @param cluster
    * @param existedNodes
    *           existed VMs
    * @return list of VMs that should be delete
    */
   public List<BaseNode> getBadNodes(ClusterCreate cluster,
         List<BaseNode> existedNodes);

   /**
    * badDisks are disks that are placed on unaccessible datastores. Find replacement
    * disks for them on the target host, given the cluster spec and resource container
    * 
    * @param container
    * @param spec
    * @param groupName
    * @param nodeName
    * @param targetHost
    * @param badDisks
    * @param dsUsage
    * @return
    */
   public List<DiskSpec> getReplacementDisks(IContainer container,
         ClusterCreate spec, String groupName, String nodeName,
         String targetHost, List<DiskSpec> badDisks, Map<String, Integer> dsUsage);
}