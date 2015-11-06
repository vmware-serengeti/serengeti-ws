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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.testng.annotations.Test;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.Constants;

public class ClusterReadTest {

   @Test
   public void testClusterSort(){
      ClusterRead cluster1 = new ClusterRead();
      cluster1.setName("clusterB");
      ClusterRead cluster2 = new ClusterRead();
      cluster2.setName("clusterA");
      ClusterRead cluster3 = new ClusterRead();
      cluster3.setName("1Cluster");
      ClusterRead[] clusters = new ClusterRead[] {cluster1, cluster2, cluster3};
      Arrays.sort(clusters);
      assertEquals(clusters[0].getName(), "1Cluster");
      assertEquals(clusters[1].getName(), "clusterA");
      assertEquals(clusters[2].getName(), "clusterB");
   }

   @Test
   public void testGetNodeGroupByName() {
      ClusterRead cluster = new ClusterRead();
      assertEquals(null, cluster.getNodeGroupByName("master"));
      NodeGroupRead master = new NodeGroupRead();
      master.setName("master");
      NodeGroupRead client = new NodeGroupRead();
      client.setName("client");
      cluster.setNodeGroups(Arrays.asList(master, client));
      assertEquals(master, cluster.getNodeGroupByName("master"));
   }
}
