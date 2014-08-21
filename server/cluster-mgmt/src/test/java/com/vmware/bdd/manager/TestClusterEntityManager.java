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
package com.vmware.bdd.manager;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupAssociation;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

@ContextConfiguration(locations = { "classpath:/spring/*-context.xml" })
public class TestClusterEntityManager extends AbstractTestNGSpringContextTests {

   private static final String CLUSTER_NAME = "bddCluster";
   private static final String HDFS_GROUP = "hdfs";
   private static final String HDFS_NODE_0 = "bddCluster-hdfs-0";
   private static final String HDFS_NODE_1 = "bddCluster-hdfs-1";
   private static final String COMPUTE_GROUP = "compute";
   private static final String HOST_IP = "10.1.1.1";

   @Autowired
   private IClusterEntityManager clusterEntityMgr;

   @BeforeMethod
   public void setup() {
      ClusterEntity cluster = clusterEntityMgr.findByName(CLUSTER_NAME);
      if (cluster != null)
         clusterEntityMgr.delete(cluster);
      cluster = assembleClusterEntity(CLUSTER_NAME);
      clusterEntityMgr.insert(cluster);
   }

   @AfterMethod
   public void tearDown() {
      ClusterEntity cluster = clusterEntityMgr.findByName(CLUSTER_NAME);
      if (cluster != null)
         clusterEntityMgr.delete(cluster);
   }

   public static ClusterEntity assembleClusterEntity(String clusterName) {
      ClusterEntity cluster = new ClusterEntity(clusterName);
      cluster.setDistro("bigtop");
      cluster.setDistroVendor(Constants.DEFAULT_VENDOR);
      cluster.setTopologyPolicy(TopologyType.NONE);
      cluster.setStatus(ClusterStatus.PROVISIONING);
      cluster.setAutomationEnable(false);
      cluster.setAppManager(Constants.IRONFAN);

      List<NodeGroupEntity> nodeGroups = new LinkedList<NodeGroupEntity>();
      NodeGroupEntity hdfsGroup = new NodeGroupEntity(HDFS_GROUP);

      // add hdfs node group
      hdfsGroup.setCluster(cluster);
      hdfsGroup.setCpuNum(1);
      hdfsGroup.setMemorySize(2048);
      hdfsGroup.setStorageSize(20);
      hdfsGroup.setStorageType(DatastoreType.LOCAL);
      hdfsGroup.setDefineInstanceNum(1);
      hdfsGroup.setHaFlag("on");

      ArrayList<String> roleStr = new ArrayList<String>();
      roleStr.add(HadoopRole.HADOOP_DATANODE.toString());
      hdfsGroup.setRoles((new Gson()).toJson(roleStr));

      // add a hdfs node
      List<NodeEntity> nodes = new LinkedList<NodeEntity>();
      NodeEntity node0 = new NodeEntity();
      node0.setVmName(HDFS_NODE_0);
      node0.setNodeGroup(hdfsGroup);
      nodes.add(node0);

      NodeEntity node1 = new NodeEntity();
      node1.setVmName(HDFS_NODE_1);
      node1.setNodeGroup(hdfsGroup);
      nodes.add(node1);

      hdfsGroup.setNodes(nodes);

      nodeGroups.add(hdfsGroup);

      // add compute node group
      NodeGroupEntity computeGroup = new NodeGroupEntity(COMPUTE_GROUP);

      computeGroup.setCluster(cluster);
      computeGroup.setCpuNum(1);
      computeGroup.setMemorySize(2048);
      computeGroup.setStorageSize(20);
      computeGroup.setStorageType(DatastoreType.SHARED);
      computeGroup.setDefineInstanceNum(1);
      computeGroup.setHaFlag("on");

      roleStr.clear();
      roleStr.add(HadoopRole.HADOOP_TASKTRACKER.toString());
      computeGroup.setRoles((new Gson()).toJson(roleStr));

      Set<NodeGroupAssociation> associations =
            new HashSet<NodeGroupAssociation>();
      NodeGroupAssociation association = new NodeGroupAssociation();

      association.setReferencedGroup(HDFS_GROUP);
      association.setAssociationType(GroupAssociationType.STRICT);
      association.setNodeGroup(computeGroup);

      associations.add(association);

      computeGroup.setGroupAssociations(associations);
      computeGroup.setNodes(new LinkedList<NodeEntity>());

      nodeGroups.add(computeGroup);

      cluster.setNodeGroups(nodeGroups);

      return cluster;
   }

   /*
    * com.vmware.bdd.utils.TestResourceCleanupUtils has a function removeClusters() which
    * will delete all clusters in database, and it's invoked concurrently with this unit test,
    * so it's necessary to guarantee the isolation of these test cases.
    * Otherwise, UT will fail sometimes.
    */
   @Test(groups = { "testClusterEntityManager" })
   @Transactional
   public void testClusterEntityManagerFuns() {
      ClusterEntity cluster = clusterEntityMgr.findByName(CLUSTER_NAME);
      if (cluster != null)
         clusterEntityMgr.delete(cluster);
      testInsertClusterEntity();
      testUpdateClusterEntity();
      testUpdateNodeGroupEntity();
      testDeleteEntity();
   }

   private void testInsertClusterEntity() {
      ClusterEntity cluster = assembleClusterEntity(CLUSTER_NAME);

      clusterEntityMgr.insert(cluster);

      // start validation
      ClusterEntity read = clusterEntityMgr.findByName(CLUSTER_NAME);
      Assert.assertTrue(read != null, "cluster " + CLUSTER_NAME
            + " should exist");

      NodeGroupEntity hdfs = clusterEntityMgr.findByName(cluster, HDFS_GROUP);
      Assert.assertTrue(hdfs != null, "node group " + HDFS_GROUP
            + " should exist");

      NodeEntity hdfsNode = clusterEntityMgr.findByName(hdfs, HDFS_NODE_0);
      Assert.assertTrue(hdfsNode != null, "node " + HDFS_NODE_0
            + " should exist");
   }

   private void testUpdateClusterEntity() {
      ClusterEntity cluster = clusterEntityMgr.findByName(CLUSTER_NAME);
      Assert.assertNotNull(cluster);
      Assert.assertEquals(cluster.getAutomationEnable().booleanValue(), false);
      cluster.setAutomationEnable(true);
      clusterEntityMgr.update(cluster);
      cluster = clusterEntityMgr.findByName(CLUSTER_NAME);
      Assert.assertEquals(cluster.getAutomationEnable().booleanValue(), true);
      cluster.setAutomationEnable(false);
      clusterEntityMgr.update(cluster);
   }

   private void testUpdateNodeGroupEntity() {
      NodeGroupEntity hdfs =
            clusterEntityMgr.findByName(CLUSTER_NAME, HDFS_GROUP);
      Assert.assertNotNull(hdfs);
      hdfs.setDefineInstanceNum(2);
      clusterEntityMgr.update(hdfs);

      Assert.assertTrue(clusterEntityMgr.findByName(CLUSTER_NAME, HDFS_GROUP)
            .getDefineInstanceNum() == 2,
            "defined instance number should be update to 2");
   }

   private void testDeleteEntity() {
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(CLUSTER_NAME);
      for (NodeEntity node : nodes) {
         node.setHostName(HOST_IP);
         clusterEntityMgr.update(node);
      }

      Assert.assertTrue(
            HOST_IP.equals(clusterEntityMgr.findByName(CLUSTER_NAME,
                  HDFS_GROUP, HDFS_NODE_0).getHostName()), "node "
                  + HDFS_NODE_0 + " should be one host " + HOST_IP);

      for (NodeEntity node : nodes) {
         clusterEntityMgr.delete(node);
      }

      Assert.assertTrue(
            clusterEntityMgr.findByName(CLUSTER_NAME, HDFS_GROUP, HDFS_NODE_0) == null,
            "node " + HDFS_NODE_0 + " should be deleted");

      ClusterEntity cluster = clusterEntityMgr.findByName(CLUSTER_NAME);
      clusterEntityMgr.delete(cluster);
      Assert.assertTrue(clusterEntityMgr.findByName(CLUSTER_NAME) == null,
            "cluster " + CLUSTER_NAME + " should be deleted");
   }

   @Test
   public void testHandleExternalOperationStatus() {
      ClusterReport report = new ClusterReport();
      report.setAction("Installing agent...");
      report.setFinished(false);
      report.setName(CLUSTER_NAME);
      report.setProgress(80);
      report.setNodeReports(new HashMap<String, NodeReport>());
      NodeReport node = new NodeReport();
      node.setAction("Installing node agent...");
      node.setName(HDFS_NODE_0);
      report.getNodeReports().put(HDFS_NODE_0, node);

      node = new NodeReport();
      node.setName(HDFS_NODE_1);
      node.setUseClusterMsg(true);
      report.getNodeReports().put(HDFS_NODE_1, node);

      boolean result = clusterEntityMgr.handleOperationStatus(CLUSTER_NAME, report, false);
      Assert.assertFalse(result, 
            "cluster " + CLUSTER_NAME + " operation should not be finished");
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(CLUSTER_NAME);
      Assert.assertTrue("Installing node agent...".equals(nodes.get(0).getAction()),
            "node 1 action should be " + "Installing node agent..." 
            + ", but got " + nodes.get(0).getAction());
      Assert.assertTrue("Installing agent...".equals(nodes.get(1).getAction()),
            "node 1 action should be " + "Installing agent..." 
            + ", but got " + nodes.get(1).getAction());
   }
}
