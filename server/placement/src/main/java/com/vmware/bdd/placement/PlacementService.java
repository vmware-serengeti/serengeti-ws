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

package com.vmware.bdd.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.Pair;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupRacks.GroupRacksType;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.entity.VirtualGroup;
import com.vmware.bdd.placement.entity.VirtualNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.interfaces.IContainer;
import com.vmware.bdd.placement.interfaces.IPlacementPlanner;
import com.vmware.bdd.placement.interfaces.IPlacementService;
import com.vmware.bdd.placement.util.PlacementUtil;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.AuAssert;

/**
 * supported placement policies are: instance_per_host, group association, rack
 * association
 *
 * @author tli
 *
 */
@Service
public class PlacementService implements IPlacementService {
   static final Logger logger = Logger.getLogger(PlacementService.class);

   private void placeVirtualGroup(IContainer container, ClusterCreate cluster,
         IPlacementPlanner planner, VirtualGroup vGroup,
         List<BaseNode> placedNodes, Map<String, List<String>> filteredHosts) {
      String targetRack = null;
      if (vGroup.getGroupRacks() != null
            && GroupRacksType.SAMERACK.equals(vGroup.getGroupRacks().getType())) {
         AuAssert.check(vGroup.getGroupRacks().getRacks() != null
               && vGroup.getGroupRacks().getRacks().length == 1);
         targetRack = vGroup.getGroupRacks().getRacks()[0];
      }

      // find out hosts filtered out by datastores
      if (filteredHosts.containsKey(PlacementUtil.NO_DATASTORE_HOSTS)) {
         filteredHosts.remove(PlacementUtil.NO_DATASTORE_HOSTS);
         filteredHosts.remove(PlacementUtil.NO_DATASTORE_HOSTS_NODE_GROUP);
      }
      List<String> dsFilteredOutHosts = new ArrayList<String>();
      if (vGroup.getvNodes().size() != 0) {
         List<String> noDatastoreHosts = container.getDsFilteredOutHosts(vGroup);
         if (null != noDatastoreHosts && !noDatastoreHosts.isEmpty()) {
            filteredHosts.put(PlacementUtil.NO_DATASTORE_HOSTS,
                  noDatastoreHosts);
            filteredHosts.put(PlacementUtil.NO_DATASTORE_HOSTS_NODE_GROUP,
                  vGroup.getNodeGroupNames());
         }
      }

      // place virtual node one by one
      for (VirtualNode vNode : vGroup.getvNodes()) {
         logger.info("placing the virtual node " + vNode.getBaseNodeNames());
         List<AbstractHost> candidates =
               container.getValidHosts(vNode, targetRack);

         if (candidates == null || candidates.size() == 0) {
            logger.error("cannot find candidate hosts from the container "
                  + "to place the virtual node " + vNode.getBaseNodeNames());
            throw PlacementException.OUT_OF_VC_HOST(PlacementUtil.getBaseNodeNames(vNode));
         }

         // select host
         AbstractHost host = planner.selectHost(vNode, candidates);

         if (host == null) {
            logger.error("cannot find a candidate from host lists "
                  + candidates + " for the virtual node "
                  + vNode.getBaseNodeNames());
            // TODO different exception for policy violation
            throw PlacementException.OUT_OF_VC_HOST(PlacementUtil.getBaseNodeNames(vNode));
         }

         // generate placement topology
         for (BaseNode baseNode : vNode.getBaseNodes()) {
            Pair<String, String> rpClusterPair =
                  planner.selectVcRp(baseNode, host);
            String rack = container.getRack(host);
            baseNode.place(rack, rpClusterPair.first, rpClusterPair.second,
                  host);
         }

         // allocate resource from container
         container.allocate(vNode, host);

         logger.info("placed the virtual node on host " + host);
         logger.info("detailed virtual node info is as below " + vNode);

         // done, add to the complete list
         placedNodes.addAll(vNode.getBaseNodes());
      }
   }

   private void placeVirtualGroupWithSnapshot(IContainer container,
         ClusterCreate cluster, IPlacementPlanner planner, VirtualGroup vGroup,
         List<BaseNode> placedNodes, Map<String, List<String>> filteredHosts) {
      // snap shot environment on placement exceptions
      try {
         placeVirtualGroup(container, cluster, planner, vGroup, placedNodes, filteredHosts);
      } catch (PlacementException e) {
         logger.error("Place cluster " + cluster.getName()
               + " failed. PlacementException: " + e.getMessage());
         snapShotPlacementEnv(container, cluster, placedNodes);
         throw PlacementException.PLACEMENT_ERROR(e, placedNodes, filteredHosts);
      }
   }

   private void snapShotPlacementEnv(IContainer container,
         ClusterCreate cluster, List<BaseNode> placedNodes) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      logger.info("the cluster spec is shown as follows");
      logger.info(gson.toJson(cluster));

      logger.info("The following nodes have been successfully placed");
      for (BaseNode node : placedNodes) {
         logger.info(node.getDetailDesc());
      }

      logger.info("The status of vc hosts after placement are shown as follows");
      for (AbstractHost host : container.getAllHosts()) {
         logger.info(gson.toJson(host));
      }
   }

   @Override
   public List<BaseNode> getPlacementPlan(IContainer container,
         ClusterCreate cluster, List<BaseNode> existedNodes, Map<String, List<String>> filteredHosts) {
      IPlacementPlanner planner = new PlacementPlanner();

      /*
       *  assert the getBadNodes method is called before this method and bad nodes
       *  have been removed
       *
       *  TODO: handle the case when nodes are vMotioned between host before a resize operation
       */
      List<BaseNode> badNodes = planner.getBadNodes(cluster, existedNodes);
      AuAssert.check(badNodes == null || badNodes.size() == 0);

      AuAssert.check(((Container) container).getTemplateNode() != null);

      planner.init(cluster, ((Container) container).getTemplateNode(),
            existedNodes, ((Container) container).getRackMap());

      /*
       * pre-process the cluster and split them into virtual groups, by
       * analyzing the instance_per_host and group association policy
       */
      List<VirtualGroup> vGroups = planner.getVirtualGroups(existedNodes);

      // place virtual groups that are referred by others at first
      List<VirtualGroup> referredGroups = new ArrayList<VirtualGroup>();
      List<VirtualGroup> normalGroups = new ArrayList<VirtualGroup>();
      for (VirtualGroup vGroup : vGroups) {
         if (vGroup.isReferred())
            referredGroups.add(vGroup);
         else
            normalGroups.add(vGroup);
      }

      // bin pack: place vGroups that have larger storage requirement first
      List<BaseNode> placedNodes = new ArrayList<BaseNode>();
      Collections.sort(referredGroups, Collections.reverseOrder());
      for (VirtualGroup vGroup : referredGroups) {
         placeVirtualGroupWithSnapshot(container, cluster, planner, vGroup,
               placedNodes, filteredHosts);
      }

      // bin pack: place vGroups that have larger storage requirement first
      Collections.sort(normalGroups, Collections.reverseOrder());
      for (VirtualGroup vGroup : normalGroups) {
         placeVirtualGroupWithSnapshot(container, cluster, planner, vGroup,
               placedNodes, filteredHosts);
      }

      // ensure the number of nodes is correct
      if (existedNodes != null && existedNodes.size() != 0)
         AuAssert.check(placedNodes.size() == cluster.totalInstances()
               - existedNodes.size());
      else
         AuAssert.check(placedNodes.size() == cluster.totalInstances());
      return placedNodes;
   }

   @Override
   public List<BaseNode> getBadNodes(ClusterCreate cluster,
         List<BaseNode> existedNodes) {
      // remove policy violated VMs
      IPlacementPlanner planner = new PlacementPlanner();
      return planner.getBadNodes(cluster, existedNodes);
   }

   @Override
   public List<DiskSpec> getReplacementDisks(IContainer container,
         ClusterCreate spec, String groupName, String nodeName,
         String targetHost, List<DiskSpec> badDisks,
         Map<String, Integer> dsUsage) {
      // TODO Auto-generated method stub
      return null;
   }
}