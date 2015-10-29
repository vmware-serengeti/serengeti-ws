/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.mockito.Mockito;
import org.springframework.batch.core.JobParameters;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


public class TestShrinkManager {
   private ShrinkManager shrinkManager;

   @MockClass(realClass = ClusterEntityManager.class)
   private static class MockedClusterEntityManager {
      @Mock
      public List<NodeEntity> findAllNodes(String clusterName, String groupName) {
         NodeGroupEntity group = new NodeGroupEntity();
         group.setDefineInstanceNum(2);
         NodeEntity node1 = new NodeEntity();
         node1.setStatus(NodeStatus.SERVICE_READY);
         node1.setVmName("node-name-1");
         node1.setNodeGroup(group);
         NodeEntity node2 = new NodeEntity();
         node2.setStatus(NodeStatus.SERVICE_READY);
         node2.setVmName("node-name-2");
         node2.setNodeGroup(group);
         List<NodeEntity> nodes = new ArrayList<>();
         nodes.add(node1);
         nodes.add(node2);
         return nodes;
      };

      @Mock
      public ClusterEntity findByName(String clusterName) {
         ClusterEntity entity = new ClusterEntity(clusterName);
         entity.setStatus(ClusterStatus.RUNNING);
         return entity;
      };

   }

   @BeforeClass
   public void setUp() throws Exception {
      Mockit.setUpMock(MockedClusterEntityManager.class);
      Mockit.setUpMock(MockValidationUtils.class);
      shrinkManager = Mockito.mock(ShrinkManager.class);
      System.out.println("shrink manager is :" + shrinkManager);
   }

   @AfterClass
   public void tearDown() throws Exception {
      Mockit.tearDownMocks();
   }

   @Test
   public void testShrinkNodeGroup() throws Exception {
      Mockito.when(shrinkManager.buildJobParameters(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).
            thenReturn(new ArrayList<JobParameters>());
      System.out.println("Before test, shrink manager is : " + shrinkManager);
      try {
         long result = shrinkManager.shrinkNodeGroup("cluster", "nodeGroup", 1);
         System.out.println("the result is :" + result);
      } catch (ShrinkException e) {
         System.out.println("Got exception: " + e);
      }
   }

}