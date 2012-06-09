/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.CloudProviderConfigEntity;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.TaskEntity;
import com.vmware.bdd.entity.VcDataStoreEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.utils.BddMessageUtil;


public class TestClusterManager {
   private static final Logger logger = Logger
         .getLogger(TestClusterManager.class);
   private static ClusterManager clusterManager;
   private static final String SAMPLE_JSON_FILE =
         "src/test/resources/sampleMsg.json";
   private static final String CLUSTER_NAME = "hadoop-bj";
   private static final String CLUSTER2_NAME = "hadoop-bj2";

   private static final String RP_NAME = "tliRp1";
   private static final String NETWORK_NAME = "tliDhcpNet";
   private static final String PORT_GROUP_NAME = "CFNetwork";
   private static final String LOCAL_DATASTORE_NAME = "tliLocalDs";
   private static final String SHARED_DATASTORE_NAME = "tliSharedDs";

   private static final String NODEGROUP_NAME = "worker";
   private static final String NODEGROUP2_NAME = "worker2";

   private static final int WORK_GROUP_NEW_NUM = 4;

   static {
      clusterManager = new ClusterManager();

      VcProviderManager vcmgr = new VcProviderManager();
      vcmgr.setRpMgr(new VcResourcePoolManager());
      vcmgr.setDatastoreMgr(new VcDataStoreManager());
      clusterManager.setCloudProviderMgr(vcmgr);

      ClusterConfigManager ccMgr = new ClusterConfigManager();
      ccMgr.setNetworkMgr(new NetworkManager());
      ccMgr.setDistroMgr(new DistroManager());
      ccMgr.setRpMgr(new VcResourcePoolManager());
      ccMgr.setDatastoreMgr(new VcDataStoreManager());

      clusterManager.setClusterConfigMgr(ccMgr);
      clusterManager.setTaskManager(new TaskManager());
      clusterManager.setNetworkManager(new NetworkManager());

      Set<String> rpNames = vcmgr.getRpMgr().getAllRPNames();
      for (String name : rpNames) {
         vcmgr.getRpMgr().deleteResourcePool(name);
      };
   }

   @BeforeClass
   public static void setup() {
      deleteAllResources();
      VcProviderManager vcProvider =
            (VcProviderManager) clusterManager.getCloudProviderMgr();

      vcProvider.getRpMgr().addResourcePool(RP_NAME, "Cluster1", "rp1");

      List<String> sharedStores = new ArrayList<String>();
      sharedStores.add("share1");
      sharedStores.add("share2");
      vcProvider.getDatastoreMgr().addDataStores(SHARED_DATASTORE_NAME,
            DatastoreType.SHARED, sharedStores);
      List<String> localStores = new ArrayList<String>();
      localStores.add("local1");
      localStores.add("vmfs*");
      vcProvider.getDatastoreMgr().addDataStores(LOCAL_DATASTORE_NAME,
            DatastoreType.LOCAL, localStores);

      DAL.inTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
            clusterManager.getNetworkManager().addDhcpNetwork(NETWORK_NAME,
                  PORT_GROUP_NAME);

            return null;
         }
      });
   }

   @AfterClass
   public static void tearDown() {
      // remove resources
      DAL.inTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
            NetworkEntity net =
                  clusterManager.getNetworkManager().getNetworkEntityByName(
                        NETWORK_NAME);
            clusterManager.getNetworkManager().removeNetwork(net);
            return null;
         }
      });
      clusterManager.getClusterConfigMgr().getDatastoreMgr()
            .deleteDatastore(LOCAL_DATASTORE_NAME);
      clusterManager.getClusterConfigMgr().getDatastoreMgr()
            .deleteDatastore(SHARED_DATASTORE_NAME);
      clusterManager.getClusterConfigMgr().getRpMgr().deleteResourcePool(RP_NAME);
   }

   private static void deleteAllResources() {
      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            List<CloudProviderConfigEntity> attrs =
                  CloudProviderConfigEntity.findAllByType("VC");
            for (CloudProviderConfigEntity attr : attrs) {
               attr.delete();
            }

            List<VcResourcePoolEntity> rps =
                  VcResourcePoolEntity.findAllOrderByClusterName();
            for (VcResourcePoolEntity rp : rps) {
               rp.delete();
            }
            
            List<VcDataStoreEntity> datastores = VcDataStoreEntity.findAllSortByName();
            for (VcDataStoreEntity ds : datastores) {
               ds.delete();
            }
            return null;
         }
      });
   }

   @SuppressWarnings("unchecked")
   private Map<String, Object> getSampleMsg(String clusterName, boolean success) {
      Gson gson = new Gson();

      File file = new File(SAMPLE_JSON_FILE);
      StringBuffer msg = new StringBuffer();
      try {
         BufferedReader reader = new BufferedReader(new FileReader(file));
         String line;
         while ((line = reader.readLine()) != null) {
            msg.append(line.trim());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      Map<String, Object> map =
            gson.fromJson(msg.toString(),
                  (new HashMap<String, Object>()).getClass());

      ((HashMap<String, Object>) map.get(BddMessageUtil.CLUSTER_DATA_FIELD))
            .put(BddMessageUtil.CLUSTER_NAME_FIELD, clusterName);

      if (success) {
         map.put(BddMessageUtil.FINISH_FIELD, true);
         map.put(BddMessageUtil.SUCCEED_FIELD, true);
      }

      return map;
   }

   private boolean waitForTask(TaskEntity task) throws Exception {
      while (true) {
         Thread.sleep(200);
         DAL.inTransactionRefresh(task);
         if (com.vmware.bdd.apitypes.TaskRead.Status.SUCCESS.equals(task
               .getStatus()))
            return true;
         if (com.vmware.bdd.apitypes.TaskRead.Status.FAILED.equals(task
               .getStatus()))
            return false;
      }
   }

   public void testGetManifest() {
      Map<String, Object> attrs =
            clusterManager.getClusterConfigManifest("hadoop-sprintA");
      Gson gson = new Gson();
      assertTrue("manifest should contains cluster name",
            gson.toJson(attrs).indexOf("hadoop-sprintA") != -1);
      System.out.println(gson.toJson(attrs));
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "com.vmware.bdd.dal.TestDAL.testFind" })
//   @Test(groups = {"testClusterManager"})
   public void testCreateCluster() throws Exception {
      ClusterCreate createSpec = new ClusterCreate();
      createSpec.setName(CLUSTER_NAME);
      createSpec.setNetworkName(NETWORK_NAME);
      Long id = clusterManager.createCluster(createSpec);
      TaskEntity task = TaskEntity.findById(id);
      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(CLUSTER_NAME);

      assertNotNull(task);
      assertNotNull(cluster);
      assertEquals("cluster hadoop-bj should in PROVISIONING status",
            ClusterStatus.PROVISIONING, cluster.getStatus());

      // mock. sent task listener an in-progress message
      task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, false));

      // mock, sent task listener a finish-success message .
      task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, true));

      assertTrue("task should succeed", waitForTask(task));

      DAL.inTransactionRefresh(cluster);
      assertEquals("cluster hadoop-bj should be in RUNNING status",
            ClusterStatus.RUNNING, cluster.getStatus());
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testCreateCluster" })
   public void testGetClusterUsedResources() {
      DAL.inTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() {
            ClusterEntity cluster =
                  ClusterEntity.findClusterEntityByName(CLUSTER_NAME);

            assertTrue(cluster != null);
            assertTrue(
                  "the cluster should have two instances, however the real number is "
                        + cluster.getRealInstanceNum(),
                  cluster.getRealInstanceNum() == 2);
            assertTrue("the cluster should use datastore1", cluster
                  .getUsedVcDatastores().contains("datastore1"));
            VcResourcePoolEntity rp = VcResourcePoolEntity.findByName(RP_NAME);
            assertTrue(
                  "the cluster should use resource pool Cluster1-rp1", cluster
                        .getUsedRps().contains(rp));
            return null;
         }
      });
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testGetClusterUsedResources" })
   public void testCreateClusterNegative() throws Exception {
      try {
         ClusterCreate createSpec = new ClusterCreate();
         createSpec.setName(CLUSTER_NAME);
         clusterManager.createCluster(createSpec);
         fail("creating cluster " + CLUSTER_NAME
               + " should fail, since it exist already");
      } catch (Exception e) {
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testCreateClusterNegative" })
   public void testResourcePoolList() {
      List<ResourcePoolRead> rps =
            clusterManager.getClusterConfigMgr().getRpMgr()
                  .getAllResourcePoolForRest();
      logger.info("got resource pools: " + rps);
      logger.info("got resource pool nodes: " + rps.get(0).getNodes().length);
      assertTrue("should get two nodes",
            rps.get(0).getNodes().length == 2);
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testResourcePoolList" })
   public void testDeleteUsedRP() {
      try {
         clusterManager.getClusterConfigMgr().getRpMgr()
               .deleteResourcePool(RP_NAME);
         assertTrue(
               "should get exception for resource pool is used by cluster",
               false);
      } catch (VcProviderException e) {
         e.printStackTrace();
         assertTrue(
               "get exception for resource pool is used by cluster.", true);
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testCreateCluster" })
   public void testGetGroupFromName() {
      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
      NodeGroupEntity group =
            NodeGroupEntity.findNodeGroupEntityByName(cluster, "master");
      assertTrue(group != null);
      logger.info("get group master " + group);
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testCreateCluster" })
   public void testDeleteUsedDatastore() {
      try {
         clusterManager.getClusterConfigMgr().getDatastoreMgr()
               .deleteDatastore("testLocalStore");
         assertTrue(
               "should get exception for datastore is used by cluster", false);
      } catch (VcProviderException e) {
         e.printStackTrace();
         assertTrue("get exception for datastore is used by cluster.",
               true);
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testCreateCluster" })
   public void testGetClusterRead() {
      DAL.inTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() {
            ClusterEntity cluster =
                  ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
            assertTrue(cluster != null);
            ClusterRead clusterRead = cluster.toClusterRead();
            assertTrue(
                  "parse ClusterRead object from cluster entity should work.",
                  clusterRead != null);
            logger.info((new Gson()).toJson(clusterRead));
            return null;
         }
      });
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testGetClusterRead" })
   public void testStopCluster() throws Exception {
      Long id = clusterManager.stopCluster(CLUSTER_NAME);
      TaskEntity task = TaskEntity.findById(id);

      // mock, sent task listener a finish-success message .
      task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, true));

      assertTrue("task should succeed", waitForTask(task));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
      assertTrue("cluster " + CLUSTER_NAME + " should be stopped",
            cluster.getStatus().equals(ClusterStatus.STOPPED));
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testStopCluster" })
   public void testStopClusterNegative() {
      try {
         clusterManager.stopCluster(CLUSTER_NAME);
         fail("start a running cluster " + CLUSTER_NAME + " should fail");
      } catch (Exception e) {
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testStopClusterNegative" })
   public void testStartCluster() throws Exception {
      Long id = clusterManager.startCluster(CLUSTER_NAME);
      TaskEntity task = TaskEntity.findById(id);
      // mock, sent task listener a finish-success message .
      task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, true));

      assertTrue("task should succeed", waitForTask(task));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
      assertTrue("cluster " + CLUSTER_NAME + " should be running",
            cluster.getStatus().equals(ClusterStatus.RUNNING));
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testStartCluster" })
   public void testStartClusterNegative() {
      try {
         clusterManager.startCluster(CLUSTER_NAME);
         fail("start a running cluster " + CLUSTER_NAME + " should fail");
      } catch (Exception e) {
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testStartClusterNegative" })
   public void testResizeCluster() throws Exception {
      Long id =
            clusterManager.resizeCluster(CLUSTER_NAME, NODEGROUP_NAME,
                  WORK_GROUP_NEW_NUM);
      TaskEntity task = TaskEntity.findById(id);
      // mock, sent task listener a finish-success message .
      task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, true));

      assertTrue("task should succeed", waitForTask(task));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
      assertTrue("cluster " + CLUSTER_NAME + " should be running, but get status: " + cluster.getStatus(),
            cluster.getStatus().equals(ClusterStatus.RUNNING));
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testResizeCluster" })
   public void testResizeClusterNegative() {
      try {
         clusterManager.resizeCluster(CLUSTER_NAME, NODEGROUP_NAME, 1);
         fail("shrink a node group" + NODEGROUP_NAME + " should fail");
      } catch (Exception e) {
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testResizeClusterNegative" })
   public void testResizeClusterNegative2() {
      try {
         clusterManager.resizeCluster(CLUSTER_NAME, NODEGROUP2_NAME, 4);
         fail("resize a not-existing node group" + NODEGROUP_NAME
               + " should fail");
      } catch (Exception e) {
      }
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testResizeClusterNegative2" })
   public void testDeleteCluster() throws Exception {
      Long id = clusterManager.deleteClusterByName(CLUSTER_NAME);
      TaskEntity task = TaskEntity.findById(id);
      task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, true));

      assertTrue("task should succeed", waitForTask(task));

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
      assertTrue("hadoop-bj cluster should be delete and removed in db",
            cluster == null);
   }

   @Test(groups = {"testClusterManager"}, dependsOnMethods = { "testDeleteCluster" })
   public void testDeleteClusterNegative() {
      try {
         clusterManager.deleteClusterByName(CLUSTER2_NAME);
         fail("delete " + CLUSTER2_NAME
               + " cluster should fail, since it is not exist");
      } catch (Exception e) {
      }
   }
}
