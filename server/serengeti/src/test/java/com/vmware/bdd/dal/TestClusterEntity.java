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
package com.vmware.bdd.dal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeGroupAssociation;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.Saveable;

public class TestClusterEntity {
   private static final Logger logger = Logger
         .getLogger(TestNetworkEntity.class);
   private static final String CLUSTER_NAME = "bdd-cluster";
   private static final String HDFS_GROUP = "hdfs";
   private static final String COMPUTE_GROUP = "compute";
   private static final String HADOOP_ROLE = "hadoop";
   private static final String DATA_NODE_ROLE = "hadoop-datanode";
   private static final String COMPUTE_NODE_ROLE = "hadoop-tasktracker";

   @BeforeMethod
   public void setup() {

   }

   @AfterMethod
   public void tearDown() {

   }

   @AfterClass
   public static void deleteAll() {
   }

   @Test(groups = { "testClusterEntity" })
   public void testInsert() {
      DAL.inRwTransactionDo(new Saveable<Void>() {
         public Void body() {
            ClusterEntity cluster = new ClusterEntity(CLUSTER_NAME);
            cluster.setDistro("apache");

            Set<NodeGroupEntity> nodeGroups = new HashSet<NodeGroupEntity>();
            NodeGroupEntity hdfsGroup = new NodeGroupEntity(HDFS_GROUP);

            hdfsGroup.setCluster(cluster);
            hdfsGroup.setCpuNum(1);
            hdfsGroup.setMemorySize(2048);
            hdfsGroup.setStorageSize(20);
            hdfsGroup.setStorageType(DatastoreType.LOCAL);
            hdfsGroup.setDefineInstanceNum(1);
            hdfsGroup.setHaFlag("on");

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

            DAL.insert(cluster);
            return null;
         }
      });
 
      DAL.inRoTransactionDo(new Saveable<Void>() {
         public Void body() {
            ClusterEntity clusterRead =
                  ClusterEntity.findClusterEntityByName(CLUSTER_NAME);

            Assert.assertNotNull(clusterRead);
            Assert.assertTrue(clusterRead.getNodeGroups().size() == 2);

            NodeGroupEntity groupRead =
                  NodeGroupEntity.findNodeGroupEntityByName(clusterRead,
                        COMPUTE_GROUP);

            Assert.assertNotNull(groupRead);
            Assert.assertTrue(groupRead.getCpuNum() == 1);
            Assert.assertTrue(groupRead.getGroupAssociations().size() == 1);

            return null;
         }
      });
   }

}
