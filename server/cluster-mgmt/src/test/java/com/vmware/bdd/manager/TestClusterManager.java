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
package com.vmware.bdd.manager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mockit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.IpConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ClusterManagerException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.MockTmScheduler;
import com.vmware.bdd.service.MockTmScheduler.VmOperation;
import com.vmware.bdd.service.MockVcCache;

@ContextConfiguration(locations = { "classpath:/spring/*-context.xml"})
public class TestClusterManager extends AbstractTestNGSpringContextTests {

   private static final String TEST_CLUSTER_NAME = "testClusterMgr";
   @Autowired
   private IClusterEntityManager clusterEntityMgr;
   @Autowired
   private SoftwareManagerCollector softwareManagerCollector;
   @Autowired
   private ClusterConfigManager clusterConfigMgr;
   @Autowired
   private ClusterManager clusterMgr;

   @BeforeClass
   public void setUp() {
      softwareManagerCollector.loadSoftwareManagers();
   }

   @BeforeMethod(groups = { "TestClusterManager" }, dependsOnGroups = { "TestVmEventManager" })
   public void setMockup() {
      Mockit.setUpMock(MockValidationUtils.class);
      Mockit.setUpMock(MockTmScheduler.class);
      Mockit.setUpMock(MockVcCache.class);
      MockVcCache.setGetFlag(true);
   }

   @AfterMethod
   public void tearDown() {
      Mockit.tearDownMocks();
      cleanUpData();
      MockTmScheduler.cleanFlag();
      MockVcCache.setGetFlag(false);
   }

   private void cleanUpData() {
      ClusterEntity cluster = clusterEntityMgr.findByName(TEST_CLUSTER_NAME);
      if (cluster != null) {
         clusterEntityMgr.delete(cluster);
      }
   }

   @Test(groups = { "TestClusterManager" })
   public void testWriteClusterSpecFileWithUTF8() throws Exception {
      final String dirPath = "src/test/resources";
      final String filePath = dirPath + "/클러스터.json";
      new MockUp<ClusterManager>() {
         @Mock
         public Map<String, Object> getClusterConfigManifest(
               String clusterName, List<String> targets, boolean needAllocIp) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            ClusterCreate cluster = new ClusterCreate();
            NodeGroupCreate master = new NodeGroupCreate();
            master.setName("마스터 노드");
            NodeGroupCreate worker = new NodeGroupCreate();
            worker.setName("协作节点");
            NodeGroupCreate client = new NodeGroupCreate();
            client.setName("クライアント");
            cluster.setNodeGroups(new NodeGroupCreate[] { master, worker,
                  client });
            attrs.put("cluster_definition", cluster);
            return attrs;
         }
      }.getMockInstance();
      ClusterManager clusterManager = new ClusterManager();
      File dir = new File(dirPath);
      clusterManager.writeClusterSpecFile("클러스터-主节点-01", dir, false);
      File file = new File(filePath);
      assertTrue(file.exists());

      String clusterJson = readJsonFromFile(filePath);
      assertTrue(clusterJson.indexOf("마스터 노드") != -1);
      assertTrue(clusterJson.indexOf("协作节点") != -1);
      assertTrue(clusterJson.indexOf("クライアント") != -1);
      file.delete();
   }

   private String readJsonFromFile(String filePath) throws IOException,
         FileNotFoundException {
      StringBuffer buff = new StringBuffer();
      FileInputStream fis = null;
      InputStreamReader inputStreamReader = null;
      BufferedReader bufferedReader = null;
      try {
         fis = new FileInputStream(filePath);
         inputStreamReader = new InputStreamReader(fis, "UTF-8");
         bufferedReader = new BufferedReader(inputStreamReader);
         String line = "";
         while ((line = bufferedReader.readLine()) != null) {
            buff.append(line);
            buff.append("\n");
         }
      } finally {
         if (fis != null) {
            fis.close();
         }
         if (inputStreamReader != null) {
            inputStreamReader.close();
         }
         if (bufferedReader != null) {
            bufferedReader.close();
         }
      }
      return buff.toString();
   }

   @Test(groups = { "TestClusterManager" }, dependsOnMethods = "testWriteClusterSpecFileWithUTF8")
   public void testAsyncSetParamIoPriorityFailed() throws Exception {
      ClusterEntity cluster =
            TestClusterEntityManager.assembleClusterEntity(TEST_CLUSTER_NAME);
      cluster.setStatus(ClusterStatus.RUNNING);
      clusterEntityMgr.insert(cluster);
      MockTmScheduler.setFlag(VmOperation.RECONFIGURE_VM, false);
      try {
         clusterMgr.asyncSetParam(TEST_CLUSTER_NAME, 3, 1, 4, true,
               Priority.HIGH);
         assertTrue(false, "Should get exception but not.");
      } catch (ClusterManagerException e) {
         List<NodeEntity> nodes =
               clusterEntityMgr.findAllNodes(TEST_CLUSTER_NAME);
         assertTrue(nodes.get(0).isActionFailed(),
               "Should get action failed, but got "
                     + nodes.get(0).isActionFailed());
         assertTrue(nodes.get(0).getErrMessage().endsWith("test failure"),
               "Should get error message: test failure, but got "
                     + nodes.get(0).getErrMessage());
      }
   }

   @Test(groups = { "TestClusterManager" }, dependsOnMethods = "testAsyncSetParamIoPriorityFailed")
   public void testAsyncSetParamAutoElasticityFailed() throws Exception {
      ClusterEntity cluster =
            TestClusterEntityManager.assembleClusterEntity(TEST_CLUSTER_NAME);
      cluster.setStatus(ClusterStatus.RUNNING);
      cluster.setVhmMasterMoid("vm-001");
      int i = 0;
      List<NodeGroupEntity> groups = cluster.getNodeGroups();
      for (NodeGroupEntity group : groups) {
         List<NodeEntity> nodes = group.getNodes();
         for (NodeEntity node : nodes) {
            i++;
            node.setMoId("vm-00" + i);
         }
      }
      clusterEntityMgr.insert(cluster);
      MockTmScheduler.setFlag(VmOperation.RECONFIGURE_VM, true);
      MockTmScheduler.setFlag(VmOperation.AUTO_ELASTICITY, false);
      try {
         clusterMgr.asyncSetParam(TEST_CLUSTER_NAME, 3, 1, 4, true,
               Priority.HIGH);
         assertTrue(false, "Should get exception but not.");
      } catch (ClusterManagerException e) {
      }
   }

   @Test(groups = { "TestClusterManager" }, dependsOnMethods = "testAsyncSetParamAutoElasticityFailed")
   public void testAsyncSetParamAutoElasticitySuccess() throws Exception {
      ClusterEntity cluster =
            TestClusterEntityManager.assembleClusterEntity(TEST_CLUSTER_NAME);
      cluster.setStatus(ClusterStatus.RUNNING);
      cluster.setVhmMasterMoid("vm-001");
      int i = 0;
      List<NodeGroupEntity> groups = cluster.getNodeGroups();
      for (NodeGroupEntity group : groups) {
         List<NodeEntity> nodes = group.getNodes();
         for (NodeEntity node : nodes) {
            i++;
            node.setMoId("vm-00" + i);
         }
      }
      clusterEntityMgr.insert(cluster);
      MockTmScheduler.setFlag(VmOperation.RECONFIGURE_VM, true);
      MockTmScheduler.setFlag(VmOperation.AUTO_ELASTICITY, true);
      clusterMgr.asyncSetParam(TEST_CLUSTER_NAME, 3, 1, 4, true,
            Priority.HIGH);
      assertTrue(true, "Should get exception but not.");
   }

   @Test(groups = { "TestClusterManager" }, dependsOnMethods = "testAsyncSetParamAutoElasticitySuccess")
   public void testGetRackTopology() {
      IClusterEntityManager iclusterEntityManager =
            new MockUp<IClusterEntityManager>() {
               @Mock
               public ClusterRead toClusterRead(String name) {
                  ClusterRead cluster = new ClusterRead();
                  cluster.setTopologyPolicy(TopologyType.NONE);
                  List<NodeGroupRead> nodeGroups =
                        new ArrayList<NodeGroupRead>();
                  NodeGroupRead nodeGroup = new NodeGroupRead();
                  List<NodeRead> nodes = new ArrayList<NodeRead>();
                  NodeRead node = new NodeRead();
                  node.setRack("rack1");
                  node.setHostName("host1.com");
                  Map<NetTrafficType, List<IpConfigInfo>> ipConfigs =
                        new HashMap<NetTrafficType, List<IpConfigInfo>>();
                  IpConfigInfo ipConfigInfo1 =
                        new IpConfigInfo(NetTrafficType.MGT_NETWORK, "nw1",
                              "portgroup1", "192.168.1.100");
                  List<IpConfigInfo> ipConfigInfos1 =
                        new ArrayList<IpConfigInfo>();
                  ipConfigInfos1.add(ipConfigInfo1);
                  IpConfigInfo ipConfigInfo2 =
                        new IpConfigInfo(NetTrafficType.HDFS_NETWORK, "nw2",
                              "portgroup2", "192.168.2.100");
                  IpConfigInfo ipConfigInfo3 =
                        new IpConfigInfo(NetTrafficType.HDFS_NETWORK, "nw3",
                              "portgroup3", "192.168.3.100");
                  List<IpConfigInfo> ipConfigInfos2 =
                        new ArrayList<IpConfigInfo>();
                  ipConfigInfos2.add(ipConfigInfo2);
                  ipConfigInfos2.add(ipConfigInfo3);
                  ipConfigs.put(NetTrafficType.MGT_NETWORK, ipConfigInfos1);
                  ipConfigs.put(NetTrafficType.HDFS_NETWORK, ipConfigInfos2);
                  node.setIpConfigs(ipConfigs);
                  nodes.add(node);
                  nodeGroup.setInstances(nodes);
                  nodeGroups.add(nodeGroup);
                  cluster.setNodeGroups(nodeGroups);
                  return cluster;
               }
            }.getMockInstance();

      new MockUp<ClusterConfigManager>() {
         @Mock
         public boolean validateRackTopologyUploaded(Set<String> hosts, String topology) {
            return true;
         }

         @Mock
         public Map<String, String> buildTopology(List<NodeRead> nodes, String topology) {
            Map<String, String> rackTopology = new HashMap<String, String>();
            Iterator<Map.Entry<NetConfigInfo.NetTrafficType, List<IpConfigInfo>>> ipConfigIt =
                  nodes.get(0).getIpConfigs().entrySet().iterator();
            while (ipConfigIt.hasNext()) {
               Entry<NetTrafficType, List<IpConfigInfo>> entry = ipConfigIt.next();
               for (IpConfigInfo ipConfig : entry.getValue()) {
                  rackTopology.put(ipConfig.getIpAddress(), nodes.get(0).getRack());
               }
            }
            return rackTopology;
         }
      };
      clusterMgr.setClusterEntityMgr(iclusterEntityManager);
      Map<String, String> racksTopology =
            clusterMgr.getRackTopology(TEST_CLUSTER_NAME, "HOST_AS_RACK");
      assertEquals("rack1", racksTopology.get("192.168.1.100"));
   }
}
