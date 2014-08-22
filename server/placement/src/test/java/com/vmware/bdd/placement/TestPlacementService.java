/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.placement.entity.AbstractDatacenter;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.placement.exception.PlacementException;
import com.vmware.bdd.placement.util.PlacementUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestPlacementService {

   @Test
   public void testDefaultCluster() throws Exception {
      ClusterCreate spec =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.SIMPLE_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());

      PlacementService service = new PlacementService();

      List<BaseNode> nodes =
            service.getPlacementPlan(container, spec, null,
                  new HashMap<String, List<String>>());

      TestPlacementUtil.validate(spec, nodes, false);
   }

   @Test
   public void testClusterWithStrictAssociation() throws Exception {
      ClusterCreate spec =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.DC_SPLIT_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());

      PlacementService service = new PlacementService();

      List<BaseNode> nodes =
            service.getPlacementPlan(container, spec, null,
                  new HashMap<String, List<String>>());

      TestPlacementUtil.validate(spec, nodes, false);
   }

   @Test
   public void testGetBadNodes() throws Exception {
      ClusterCreate cluster =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.DC_SPLIT_CLUSTER_SPEC);

      List<BaseNode> existedNodes =
            TestPlacementUtil
                  .getExistedNodes(TestPlacementUtil.EXISTED_NODE_SPEC);

      PlacementService service = new PlacementService();

      List<BaseNode> bads = service.getBadNodes(cluster, existedNodes);

      Assert.assertTrue(bads.size() == 3,
            "number of bad nodes should be 3, but it's " + bads.size());

      existedNodes.removeAll(bads);
      TestPlacementUtil.validate(cluster, existedNodes, true);
   }

   @Test
   public void testResumeClusterWithStrictAssociation() throws Exception {
      ClusterCreate cluster =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.DC_SPLIT_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());

      List<BaseNode> existedNodes =
            TestPlacementUtil
                  .getExistedNodes(TestPlacementUtil.EXISTED_NODE_SPEC);

      PlacementService service = new PlacementService();

      List<BaseNode> bads = service.getBadNodes(cluster, existedNodes);

      // In real codes, call vm.delete to delete bad nodes
      existedNodes.removeAll(bads);

      List<BaseNode> nodes =
            service.getPlacementPlan(container, cluster, existedNodes,
                  new HashMap<String, List<String>>());

      Assert.assertEquals(nodes.size(), 6);

      existedNodes.addAll(nodes);
      TestPlacementUtil.validate(cluster, existedNodes, false);
   }

   @Test
   public void testResizeCluster() throws Exception {
      ClusterCreate cluster =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.DC_SPLIT_CLUSTER_SPEC);

      // increase data group's instance number by 1
      cluster.getNodeGroup("data").setInstanceNum(
            cluster.getNodeGroup("data").getInstanceNum() + 1);

      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.RESIZE_DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());

      List<BaseNode> existedNodes =
            TestPlacementUtil
                  .getExistedNodes(TestPlacementUtil.RESIZE_NODE_SPEC);

      PlacementService service = new PlacementService();

      List<BaseNode> bads = service.getBadNodes(cluster, existedNodes);

      Assert.assertEquals(bads.size(), 0);

      List<BaseNode> nodes =
            service.getPlacementPlan(container, cluster, existedNodes,
                  new HashMap<String, List<String>>());

      Assert.assertEquals(nodes.size(), 1);

      existedNodes.addAll(nodes);
      TestPlacementUtil.validate(cluster, existedNodes, false);

      // increase compute group's instance by 2
      cluster.getNodeGroup("compute").setInstanceNum(
            cluster.getNodeGroup("compute").getInstanceNum() + 2);
      nodes = service.getPlacementPlan(container, cluster, existedNodes,
            new HashMap<String, List<String>>());

      Assert.assertEquals(nodes.size(), 2);

      existedNodes.addAll(nodes);
      TestPlacementUtil.validate(cluster, existedNodes, false);
   }

   @Test
   public void testRRRackPolicy() throws Exception {
      ClusterCreate cluster =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.WITH_RACK_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.RESIZE_DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());
      container.addRackMap(cluster.getHostToRackMap());

      PlacementService service = new PlacementService();

      List<BaseNode> nodes =
            service.getPlacementPlan(container, cluster, null,
                  new HashMap<String, List<String>>());

      TestPlacementUtil.validate(cluster, nodes, false);

      Map<String, Integer> rackUsage = new HashMap<String, Integer>();
      for (BaseNode node : nodes) {
         if (node.getGroupName().equals("data")) {
            if (!rackUsage.containsKey(node.getTargetRack())) {
               rackUsage.put(node.getTargetRack(), 0);
            }
            rackUsage.put(node.getTargetRack(),
                  rackUsage.get(node.getTargetRack()) + 1);
         }
      }

      for (String rack : rackUsage.keySet()) {
         Assert.assertTrue(rackUsage.get(rack) == 1);
      }
   }

   @Test
   public void testSameRackPolicy() throws Exception {
      ClusterCreate cluster =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.WITH_SAME_RACK_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.RESIZE_DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());
      container.addRackMap(cluster.getHostToRackMap());

      PlacementService service = new PlacementService();

      List<BaseNode> nodes =
            service.getPlacementPlan(container, cluster, null,
                  new HashMap<String, List<String>>());

      TestPlacementUtil.validate(cluster, nodes, false);

      Map<String, Integer> rackUsage = new HashMap<String, Integer>();
      for (BaseNode node : nodes) {
         if (node.getGroupName().equals("data")) {
            if (!rackUsage.containsKey(node.getTargetRack())) {
               rackUsage.put(node.getTargetRack(), 0);
            }
            rackUsage.put(node.getTargetRack(),
                  rackUsage.get(node.getTargetRack()) + 1);
         }
      }

      // assure only one rack is used
      Assert.assertTrue(rackUsage.size() == 1);
   }

   @Test
   public void testFilteredHosts() throws Exception {
      ClusterCreate spec =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.SIMPLE_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());
      List<AbstractHost> allHosts = container.getAllHosts();
      AbstractHost host = allHosts.get(0);
      container.removeHost(host.getName());
      List<String> outOfSyncHosts = new ArrayList<String>();
      outOfSyncHosts.add(host.getName());
      List<String> noNetworkHosts = new ArrayList<String>();
      noNetworkHosts.add(host.getName());
      Map<String, List<String>> filteredHosts = new HashMap<String, List<String>>();
      filteredHosts.put(PlacementUtil.OUT_OF_SYNC_HOSTS, outOfSyncHosts);
      filteredHosts.put(PlacementUtil.NETWORK_NAMES, new ArrayList<String>(Arrays.asList("VM Network")));
      filteredHosts.put(PlacementUtil.NO_NETWORKS_HOSTS, noNetworkHosts);

      PlacementService service = new PlacementService();

      try {
         List<BaseNode> nodes = service.getPlacementPlan(container, spec, null, filteredHosts);
      } catch (PlacementException e) {
         System.out.println(e.getMessage());
         String[] strs = e.getMessage().split("\n");
         Assert.assertEquals(strs[0],
               "No host available for node [hadoop-worker-4, hadoop-worker-5] that meets the placement policy requirements specified for the number of instances per host. Possible fixes:");
         // temporarily disabled, the placement order is different soemtime on Jenkins box
         //Assert.assertEquals(strs[1],
         //      "Node hadoop-worker-0 placed on host 10.1.1.2. Node hadoop-worker-1 placed on host 10.1.1.2. Node hadoop-worker-2 placed on host 10.1.1.3. Node hadoop-worker-3 placed on host 10.1.1.3. ");
         Assert.assertEquals(strs[1],
               "You must synchronize the time of the following hosts [10.1.1.1] with the Serengeti Management Server to use them.");
         Assert.assertEquals(strs[2],
               "You must add these hosts [10.1.1.1] to the network [VM Network] to use them.");
      }
   }

   @Test
   public void testRRRackPolicyFailure() throws Exception {
      ClusterCreate cluster =
            TestPlacementUtil
                  .getSimpleClusterSpec(TestPlacementUtil.RACK_FAILURE_CLUSTER_SPEC);
      AbstractDatacenter dc =
            TestPlacementUtil
                  .getAbstractDatacenter(TestPlacementUtil.RACK_FAILURE_DATACENTER_SPEC);

      Container container = new Container(dc);
      container.SetTemplateNode(TestPlacementUtil.getTemplateNode());
      container.addRackMap(cluster.getHostToRackMap());

      PlacementService service = new PlacementService();

      try {
         List<BaseNode> nodes = service.getPlacementPlan(container, cluster, null,
               new HashMap<String, List<String>>());
      } catch (PlacementException e) {
         System.out.println(e.getMessage());
         String[] strs = e.getMessage().split("\n");
         Assert.assertEquals(strs[0],
               "No host available on the racks [rack1] specified for the node [hadoop-data-0]. Review your topology rack-hosts mapping file and correct as necessary. Possible fixes:");
         Assert.assertEquals(strs[1], "You must add datastores on these hosts [10.1.1.3, 10.1.1.4] to use them with the node group [data].");
      }
   }
}