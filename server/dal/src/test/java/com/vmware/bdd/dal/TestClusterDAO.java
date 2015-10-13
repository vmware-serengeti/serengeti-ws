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
package com.vmware.bdd.dal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupAssociation;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;

@ContextConfiguration(locations = {"classpath:/META-INF/spring/*-context.xml"})
public class TestClusterDAO extends AbstractTransactionalTestNGSpringContextTests {

   private static final Logger logger = Logger.getLogger(TestClusterDAO.class);
   private static final String CLUSTER_NAME = "bdd-cluster";
   private static final String HDFS_GROUP = "hdfs";
   private static final String COMPUTE_GROUP = "compute";
   private static final String HADOOP_ROLE = "hadoop";
   private static final String DATA_NODE_ROLE = "hadoop-datanode";
   private static final String COMPUTE_NODE_ROLE = "hadoop-tasktracker";

   @Autowired
   private IClusterDAO clusterDao;
   @Autowired
   private INodeGroupDAO nodeGroupDao;
   @Autowired
   private IResourcePoolDAO resourcePoolDao;

   @BeforeMethod
   public void setup() {

   }

   @AfterClass
   public static void deleteAll() {
   }

   @Transactional
   @Test(groups = { "testClusterEntity" })
   public void testOperations() {
      ClusterEntity cluster = new ClusterEntity(CLUSTER_NAME);
      cluster.setDistro("apache");
      cluster.setTopologyPolicy(TopologyType.NONE);
      cluster.setStatus(ClusterStatus.PROVISIONING);

      Set<NodeGroupEntity> nodeGroups = new HashSet<NodeGroupEntity>();
      NodeGroupEntity hdfsGroup = new NodeGroupEntity(HDFS_GROUP);

      hdfsGroup.setCluster(cluster);
      hdfsGroup.setCpuNum(1);
      hdfsGroup.setMemorySize(2048);
      hdfsGroup.setStorageSize(20);
      hdfsGroup.setStorageType(DatastoreType.LOCAL);
      hdfsGroup.setDefineInstanceNum(2);
      hdfsGroup.setHaFlag("on");

      Set<NodeEntity> hdfsNodes = new HashSet<NodeEntity>();
      NodeEntity hdfsNode1 = new NodeEntity();
      NodeEntity hdfsNode2 = new NodeEntity();
      hdfsNode1.setVmName("hdfsNode1");
      hdfsNode2.setVmName("hdfsNode2");

      VcResourcePoolEntity vcRp1 = new VcResourcePoolEntity();
      vcRp1.setName("RP1");
      vcRp1.setVcCluster("Cluster1");
      vcRp1.setVcResourcePool("VcRp1");
      resourcePoolDao.insert(vcRp1);
      hdfsNode1.setVcRp(vcRp1);

      hdfsNodes.add(hdfsNode1);
      hdfsNodes.add(hdfsNode2);
      hdfsGroup.setNodes(hdfsNodes);

      ArrayList<String> roleStr = new ArrayList<String>();
      roleStr.add(HADOOP_ROLE);
      roleStr.add(DATA_NODE_ROLE);
      hdfsGroup.setRoles((new Gson()).toJson(roleStr));

      nodeGroups.add(hdfsGroup);

      NodeGroupEntity computeGroup = new NodeGroupEntity(COMPUTE_GROUP);

      computeGroup.setCluster(cluster);
      computeGroup.setCpuNum(1);
      computeGroup.setMemorySize(2048);
      computeGroup.setStorageSize(20);
      computeGroup.setStorageType(DatastoreType.SHARED);
      computeGroup.setDefineInstanceNum(1);
      computeGroup.setHaFlag("on");

      Set<NodeEntity> computeNodes = new HashSet<NodeEntity>();
      NodeEntity computeNode1 = new NodeEntity();
      computeNode1.setVmName("computeNode1");
      computeNodes.add(computeNode1);
      computeGroup.setNodes(computeNodes);

      roleStr.clear();
      roleStr.add(HADOOP_ROLE);
      roleStr.add(COMPUTE_NODE_ROLE);
      computeGroup.setRoles((new Gson()).toJson(roleStr));

      Set<NodeGroupAssociation> associations =
            new HashSet<NodeGroupAssociation>();
      NodeGroupAssociation association = new NodeGroupAssociation();

      association.setReferencedGroup(HDFS_GROUP);
      association.setAssociationType(GroupAssociationType.STRICT);
      association.setNodeGroup(computeGroup);

      associations.add(association);

      computeGroup.setGroupAssociations(associations);

      nodeGroups.add(computeGroup);

      cluster.setNodeGroups(nodeGroups);

      // test insert()
      clusterDao.insert(cluster);

      ClusterEntity clusterRead = clusterDao.findByName(CLUSTER_NAME);

      Assert.assertNotNull(clusterRead);
      Assert.assertTrue(clusterRead.getNodeGroups().size() == 2);

      NodeGroupEntity groupRead =
            nodeGroupDao.findByName(clusterRead, COMPUTE_GROUP);

      Assert.assertNotNull(groupRead);
      Assert.assertTrue(groupRead.getCpuNum() == 1);
      Assert.assertTrue(groupRead.getGroupAssociations().size() == 1);

      // test updateStatus()
      clusterDao.updateStatus(CLUSTER_NAME, ClusterStatus.VHM_RUNNING);
      clusterRead = clusterDao.findByName(CLUSTER_NAME);
      Assert.assertTrue(clusterRead.getStatus() == ClusterStatus.VHM_RUNNING);

      // test getAllNodes()
      List<NodeEntity> allNodes = clusterDao.getAllNodes(CLUSTER_NAME);
      logger.info(allNodes.size());
      Assert.assertTrue(allNodes.size() == 3);

      List<String> clusterNames = clusterDao.findClustersByUsedResourcePool("RP1");
      Assert.assertTrue(clusterNames.size() == 1);
      Assert.assertTrue(clusterNames.contains(CLUSTER_NAME));

      clusterDao.delete(clusterRead);
      resourcePoolDao.delete(vcRp1);

   }

}
