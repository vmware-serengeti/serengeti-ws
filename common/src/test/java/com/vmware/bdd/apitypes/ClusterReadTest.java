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
package com.vmware.bdd.apitypes;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.vmware.bdd.spectypes.HadoopRole;
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
   public void testGetNodeGroups() {
      ClusterRead cluster = new ClusterRead();
      NodeGroupRead client = new NodeGroupRead();
      client.setRoles(Arrays.asList(HadoopRole.HADOOP_CLIENT_ROLE.toString()));
      NodeGroupRead worker = new NodeGroupRead();
      worker.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.HADOOP_DATANODE.toString()));
      NodeGroupRead master = new NodeGroupRead();
      master.setRoles(Arrays.asList(HadoopRole.HADOOP_NAMENODE_ROLE.toString(),
            HadoopRole.HADOOP_JOBTRACKER_ROLE.toString()));
      cluster.setNodeGroups(Arrays.asList(client, worker, master));
      assertEquals(3, cluster.getNodeGroups().size());
      assertEquals(master, cluster.getNodeGroups().get(0));
      assertEquals(worker, cluster.getNodeGroups().get(1));
      assertEquals(client, cluster.getNodeGroups().get(2));
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

   @SuppressWarnings("unchecked")
   @Test
   public void testValidateSetManualElasticity() {
      ClusterRead cluster = new ClusterRead();
      cluster.setDistroVendor(Constants.MAPR_VENDOR);
      NodeGroupRead compute = new NodeGroupRead();
      compute.setName("compute");
      compute.setRoles(Arrays.asList(HadoopRole.MAPR_TASKTRACKER_ROLE
            .toString()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(true, cluster.validateSetManualElasticity());
      compute.setRoles(Arrays.asList(
            HadoopRole.MAPR_TASKTRACKER_ROLE.toString(),
            HadoopRole.MAPR_NFS_ROLE.toString()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(false,
            cluster.validateSetManualElasticity(Arrays.asList("compute")));
      cluster.setDistroVendor(Constants.DEFAULT_VENDOR);
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(true, cluster.validateSetManualElasticity());
      compute.setRoles(Arrays.asList(HadoopRole.HADOOP_TASKTRACKER.toString(),
            HadoopRole.TEMPFS_CLIENT_ROLE.toString()));
      cluster.setNodeGroups(Arrays.asList(compute));
      assertEquals(true, cluster.validateSetManualElasticity());
   }

   @Test
   public void testNeedAsyncUpdateParam() {
      ClusterRead cluster = new ClusterRead();
      ElasticityRequestBody requestBody = new ElasticityRequestBody();
      requestBody.setEnableAuto(new Boolean(false));
      requestBody.setActiveComputeNodeNum(new Integer(2));
      assertEquals(true, cluster.needAsyncUpdateParam(requestBody));
      requestBody.setEnableAuto(null);
      cluster.setAutomationEnable(new Boolean(false));
      assertEquals(true, cluster.needAsyncUpdateParam(requestBody));
   }

}
