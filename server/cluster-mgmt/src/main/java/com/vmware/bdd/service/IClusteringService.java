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
package com.vmware.bdd.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.event.VmEventManager;
import com.vmware.bdd.service.job.NodeOperationStatus;
import com.vmware.bdd.service.job.StatusUpdater;

public interface IClusteringService {

   /**
    * get provision/placement plan.
    * 
    * Exposing this interface would enable users to preview the provision plan
    * and adjust it accordingly in future.
    * 
    * Essentially, the host-vm placement problem is a NP-complete multiple
    * knapsack problem. It's a big effort to implement a dynamic programming
    * algorithm to find the Pareto solution for this kind of problem. So, we
    * introduce a simple greedy algorithm to place node by node, and do not
    * promise to find out a valid solution in some cases.
    * 
    * @param clusterSpec
    * @return list of VM placement plans
    */
   public List<BaseNode> getPlacementPlan(ClusterCreate clusterSpec,
         List<BaseNode> existedNodes);

   /**
    * reserve resource for the cluster
    * 
    * ask resource manager to lock/reserve the resources for one cluster. After
    * the cluster VM creation is finished, either success or failed, user need
    * to release resource
    * 
    * @param clusterName
    */
   public UUID reserveResource(String clusterName);

   /**
    * Commit resource reservation
    * 
    * @param reservationId
    * @throws VcProviderException
    */
   void commitReservation(UUID reservationId) throws VcProviderException;

   /**
    * start a cluster given its name
    * 
    * first ask TM to start all VMs and then call ironfan to bootstrap nodes
    * 
    * @param name
    *           cluster name
    */
   public boolean startCluster(String name,
         List<NodeOperationStatus> failedNodes, StatusUpdater statusUpdator);

   /**
    * stop a cluster
    * 
    * call TM to stop all VMs
    * 
    * @param name
    *           cluster name
    */
   public boolean stopCluster(String name,
         List<NodeOperationStatus> failedNodes, StatusUpdater statusUpdator);

   /**
    * delete a cluster
    * 
    * call TM to delete VMs
    * 
    * @param name
    *           cluster name
    */
   public boolean deleteCluster(String clusterName, List<BaseNode> vNodes,
         StatusUpdater statusUpdator);

   /**
    * This method will remove node violate placement policy
    * 
    * @param cluster
    * @param existingNodes
    * @param deletedNodes
    * @param occupiedIpSets
    * @return
    */
   public boolean removeBadNodes(ClusterCreate cluster,
         List<BaseNode> existingNodes, List<BaseNode> deletedNodes,
         Map<String, Set<String>> occupiedIpSets, StatusUpdater statusUpdator);

   public List<BaseNode> getBadNodes(ClusterCreate cluster,
         List<BaseNode> existingNodes);

   public boolean syncDeleteVMs(List<BaseNode> badNodes,
         StatusUpdater statusUpdator, boolean ignoreUnavailableNodes);

   /**
    *
    * @param networkAdds
    * @param vNodes
    * @param occupiedMgrIps
    * @param reserveRawDisks
    * @param statusUpdater
    * @return
    */
   public boolean createVcVms(List<NetworkAdd> networkAdds,
         List<BaseNode> vNodes, Map<String, Set<String>> occupiedMgrIps,
         boolean reserveRawDisks, StatusUpdater statusUpdater);

   /**
    * Initialize clustering service
    */
   public void init();

   /**
    * Destroy clustering service
    */
   public void destroy();

   /**
    * Set auto elasticity
    * 
    * @param clusterName
    * @return
    */
   public boolean setAutoElasticity(String clusterName, boolean refreshAllNodes);


   /**
    * adjust disk io shares to specified level, NORMAL, HIGH or LOW
    * 
    * @param clusterName
    * @param targetNodes
    * @param ioShares
    * @return
    */
   public  Map<String, String> configIOShares(String clusterName, List<NodeEntity> targetNodes,
         Priority ioShares);

   /**
    * start a node in the cluster
    * 
    * @param clusterName
    * @param nodeName
    * @return
    */
   public boolean startSingleVM(String clusterName, String nodeName,
         StatusUpdater statusUpdator);

   /**
    * stop a node in the cluster
    * 
    * @param clusterName
    * @param nodeName
    * @param statusUpdator
    * @return
    */
   public boolean stopSingleVM(String clusterName, String nodeName,
         StatusUpdater statusUpdator, boolean... vmPoweroff);

   /**
    * get the vm id of template vm
    * 
    * @return
    */
   public String getTemplateVmId();

   /**
    * get the vm name of template vm
    * 
    * @return
    */
   public String getTemplateVmName();

   public VmEventManager getEventProcessor();
}