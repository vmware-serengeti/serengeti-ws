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
package com.vmware.bdd.manager;

import static org.testng.AssertJUnit.assertTrue;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.IpBlock;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.specpolicy.ClusterSpecFactory;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.TestResourceCleanupUtils;
import com.vmware.vim.binding.vim.Folder;

@ContextConfiguration(locations = {
      "file:../serengeti/WebContent/WEB-INF/spring/root-context.xml",
      "file:../serengeti/WebContent/WEB-INF/spring/datasource-context.xml",
      "file:../serengeti/WebContent/WEB-INF/spring/spring-batch-context.xml",
      "file:../serengeti/WebContent/WEB-INF/spring/tx-context.xml",
      "file:../serengeti/WebContent/WEB-INF/spring/manager-context.xml",
      "file:src/test/resources/spring/serengeti-jobs-context.xml"})
public class TestClusteringJobs extends
      AbstractTransactionalTestNGSpringContextTests {
   private static final Logger logger = Logger
         .getLogger(TestClusteringJobs.class);
   private static final String TEST_VC_CLUSTER = "test.vc.cluster";
   private static final String TEST_VC_RESOURCEPOOL = "test.vc.resourcepool";
   private static final String TEST_VC_DATASTORE_SPEC =
         "test.vc.datastore.spec";
   private static final String TEST_VC_DATASTORE_TYPE =
         "test.vc.datastore.type";
   private static final String TEST_DHCP_VC_PORTGROUP =
         "test.dhcp.vc.portgroup";
   private static final String TEST_STATIC_END_IP = "test.static.end.ip";
   private static final String TEST_STATIC_START_IP = "test.static.start.ip";
   private static final String TEST_STATIC_VC_PORTGROUP =
         "test.static.vc.portgroup";
   private static final String TEST_STATIC_NETMASK = "test.static.netmask";
   private static final String TEST_STATIC_GATEWAY = "test.static.gateway";
   private static final String TEST_STATIC_DNS2 = "test.static.dns2";
   private static final String TEST_STATIC_DNS1 = "test.static.dns1";
   private static final String TEST_STATIC_NETWORK_NAME = "clusteringJobs-net2";
   private static final String TEST_DHCP_NETWORK_NAME = "clusteringJobs-net1";
   private static final String TEST_DATASTORE_NAME = "clusteringJobs-ds1";
   private static final String TEST_RP_NAME = "clusteringJobs-rp1";
   private static final String TEST_STATIC_IP_CLUSTER_NAME = "testClusterJobs";
   private static final String TEST_DHCP_CLUSTER_NAME = "testDhcpClusterJobs";

   private static String staticDns2;
   private static String staticDns1;
   private static String staticGateway;
   private static String staticNetMask;
   private static String staticPortgroup;
   private static String endIp;
   private static String startIp;
   private static String dhcpPortgroup;
   private static DatastoreType datastoreType;
   private static String datastoreSpec;
   private static String vcRP;
   private static String vcCluster;

   @Autowired
   private JobManager jobManager;
   @Autowired
   private IResourcePoolService resPoolSvc;
   @Autowired
   private IDatastoreService dsSvc;
   @Autowired
   private INetworkService netSvc;
   @Autowired
   private ClusterManager clusterMgr;
   @Autowired
   private ClusterEntityManager clusterEntityMgr;
   @Autowired
   private RackInfoManager rackMgr;
   @Autowired
   private IClusteringService clusterSvc;

   private static List<IpBlock> ipBlocks;
   private static TestResourceCleanupUtils cleanUpUtils;

   public static String stepsToString(Collection<StepExecution> ses) {
      StringBuilder sb = new StringBuilder();
      for (StepExecution se : ses) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append(se.getStepName()).append(":").append(se.getStatus())
               .append("-").append(se.getExecutionContext());
      }
      return sb.toString();
   }

   @BeforeClass(groups = { "TestClusteringJobs" })
   public void setup() throws Exception {
      Properties testProperty = new Properties();
      testProperty.load(new FileInputStream(
      "src/test/resources/vc-test.properties"));
      staticDns1 = testProperty.getProperty(TEST_STATIC_DNS1);
      staticDns2 = testProperty.getProperty(TEST_STATIC_DNS2);
      staticGateway = testProperty.getProperty(TEST_STATIC_GATEWAY);
      staticNetMask = testProperty.getProperty(TEST_STATIC_NETMASK);
      staticPortgroup = testProperty.getProperty(TEST_STATIC_VC_PORTGROUP);
      startIp = testProperty.getProperty(TEST_STATIC_START_IP);
      endIp = testProperty.getProperty(TEST_STATIC_END_IP);
      dhcpPortgroup = testProperty.getProperty(TEST_DHCP_VC_PORTGROUP);
      datastoreType =
         DatastoreType.valueOf(testProperty
               .getProperty(TEST_VC_DATASTORE_TYPE));
      datastoreSpec = testProperty.getProperty(TEST_VC_DATASTORE_SPEC);
      vcRP = testProperty.getProperty(TEST_VC_RESOURCEPOOL);
      vcCluster = testProperty.getProperty(TEST_VC_CLUSTER);

      // init vc context
      clusterSvc.init();
      cleanUpUtils = new TestResourceCleanupUtils();
      cleanUpUtils.setDsSvc(dsSvc);
      cleanUpUtils.setNetSvc(netSvc);
      cleanUpUtils.setResPoolSvc(resPoolSvc);
      removeRackInfo();
      // remove VMs understand test resource pool
      removeVMs();
      // remove clusters
      removeClusters();
      removeResources();
      try {
         resPoolSvc.addResourcePool(TEST_RP_NAME, vcCluster, vcRP);
      } catch (Exception e) {
         logger.error("ignore create resource pool exception. ", e);
      }
      List<String> specs = new ArrayList<String>();
      String[] dsSpecs = datastoreSpec.split(",");
      for (String dsSpec : dsSpecs) {
         specs.add(dsSpec);
      }
      try {
         dsSvc.addDataStores(TEST_DATASTORE_NAME, datastoreType, specs);
      } catch (Exception e) {
         logger.error("ignore create datastore exception. ", e);
      }
      try {
         netSvc.addDhcpNetwork(TEST_DHCP_NETWORK_NAME, dhcpPortgroup);
      } catch (Exception e) {
         logger.error("ignore create network exception. ", e);
      }
      ipBlocks = new ArrayList<IpBlock>();
      IpBlock ipBlock = new IpBlock();
      ipBlock.setBeginIp(startIp);
      ipBlock.setEndIp(endIp);
      ipBlocks.add(ipBlock);
      try {
         netSvc.addIpPoolNetwork(TEST_STATIC_NETWORK_NAME, staticPortgroup,
               staticNetMask, staticGateway, staticDns1, staticDns2, ipBlocks);
      } catch (Exception e) {
         logger.error("ignore create ip pool exception. ", e);
      }
   }

   private void removeRackInfo() {
      rackMgr.removeAllRacks();
   }

   private static void removeResources() {
      cleanUpUtils.removeRP(TEST_RP_NAME);
      cleanUpUtils.removeDatastore(TEST_DATASTORE_NAME);
      cleanUpUtils.removeNetwork(TEST_STATIC_NETWORK_NAME);
      cleanUpUtils.removeNetwork(TEST_DHCP_NETWORK_NAME);
   }

   private void removeClusters() {
      cleanUpUtils.removeCluster(clusterEntityMgr, TEST_STATIC_IP_CLUSTER_NAME);
      cleanUpUtils.removeCluster(clusterEntityMgr, TEST_DHCP_CLUSTER_NAME);
   }

   private static void removeVMs() {
      final VcResourcePool vcRp =
            VcResourceUtils.findRPInVCCluster(vcCluster, vcRP);
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected boolean isTaskSession() {
            return true;
         }

         @Override
         protected Void body() throws Exception {
            for (VcVirtualMachine vm : vcRp.getChildVMs()) {
               // assume only two vm under serengeti vApp, serengeti server and template
               if (vm.isPoweredOn()) {
                  vm.powerOff();
               }
               vm.destroy();
            }
            return null;
         }
      });
   }

   @AfterClass(groups = { "TestClusteringJobs" })
   public void tearDown() {
      // tear down
      removeResources();
      clusterSvc.destroy();
   }

   @Test(groups = { "TestClusteringJobs" })
   @Transactional(propagation = Propagation.NEVER)
   public void testCreateCluster() throws Exception {
      ClusterCreate createSpec = new ClusterCreate();
      createSpec.setName(TEST_STATIC_IP_CLUSTER_NAME);
      createSpec.setType(ClusterType.HDFS_MAPRED);
      createSpec.setNetworkName(TEST_STATIC_NETWORK_NAME);
      createSpec.setDistro("apache");
      createSpec.setDistroVendor(Constants.DEFAULT_VENDOR);
      long jobExecutionId = clusterMgr.createCluster(createSpec);
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getStatus() == ClusterStatus.PROVISIONING,
            "Cluster status should be PROVISIONING, but got "
                  + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      cluster = clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getInstanceNum() == 5,
            "Cluster instance number should be 5, but got "
                  + cluster.getInstanceNum());
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.RUNNING,
            "Cluster status should be RUNNING, but got " + cluster.getStatus());
      checkIpRange(cluster);
      checkVcFolders(TEST_STATIC_IP_CLUSTER_NAME);
      checkVcResourePools(cluster, ConfigInfo.getSerengetiUUID() + "-" 
            + TEST_STATIC_IP_CLUSTER_NAME);
   }

   private void checkVcFolders(final String folderName) {
      String rootFolderName = ConfigInfo.getSerengetiRootFolder();
      String serverMobId =
            Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      List<String> folderList = new ArrayList<String>(1);
      folderList.add(rootFolderName);
      Folder rootFolder =
            VcResourceUtils.findFolderByNameList(serverVm.getDatacenter(),
                  folderList);
      Folder childFolder =
            VcResourceUtils.findFolderByName(rootFolder, folderName);
      Assert.assertNotNull(childFolder, "Folder " + folderName
            + " is not exist.");
   }

   private void checkVcResourePools(ClusterRead cluster, final String rpName) {
      VcResourcePool rp = VcResourceUtils.findRPInVCCluster(vcCluster, vcRP);
      List<VcResourcePool> children = rp.getChildren();
      boolean found = false;
      VcResourcePool clusterRp = null;
      for (VcResourcePool child : children) {
         if (child.getName().equals(rpName)) {
            found = true;
            clusterRp = child;
            break;
         }
      }
      Assert.assertTrue(found, "Resource pool " + rpName + " is not created.");
      found = false;
      String groupRpName = cluster.getNodeGroups().get(0).getName();
      for (VcResourcePool groupRp : clusterRp.getChildren()) {
         if (groupRp.getName().equals(groupRpName)) {
            found = true;
            break;
         }
      }
      Assert.assertTrue(found, "Resource pool " + groupRpName
            + " is not created.");
   }

   private void checkIpRange(ClusterRead cluster) {
      List<NodeGroupRead> groups = cluster.getNodeGroups();
      List<String> ipAddresses = IpBlock.getIpAddressFromIpBlock(ipBlocks);
      for (NodeGroupRead group : groups) {
         for (NodeRead node : group.getInstances()) {
            String nodeIp = node.getIp();
            Assert.assertTrue(ipAddresses.contains(nodeIp),
                  "Ip address " + nodeIp + 
                  " for node " + node.getName() + 
                  " should be in the test ip range.");
            Assert.assertTrue(
                  node.getStatus().equals(NodeStatus.VM_READY.toString()),
                  "expected VM_READY, but got " + node.getStatus());
         }
      }
   }

   private void stopVmAfterStarted(String vmName, 
         long jobExecutionId) throws InterruptedException, Exception {
      int retry = 0;
      while (retry <= 0) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED============");
            break;
         }
         if (TaskRead.Status.FAILED.equals(tr.getStatus())
               || TaskRead.Status.STOPPED.equals(tr.getStatus())) {
            logger.info("===========FAILED============");
            break;
         }
         boolean stopped = stopVcVm(vmName);
         if (stopped) {
            break;
         } else {
            continue;
         }
      }
   }

   private boolean stopVcVm(String vmName) {
      final VcVirtualMachine vm = VcResourceUtils.findVmInVcCluster(
            vcCluster, vcRP, vmName);
      boolean stopped = VcContext.inVcSessionDo(new VcSession<Boolean>() {
         @Override
         protected boolean isTaskSession() { 
            return true;
         }
         @Override
         protected Boolean body() throws Exception {
            if (vm != null && vm.isPoweredOn()) {
               vm.powerOff();
               logger.info("power off vm: " + vm.getName() + " to make cluster creation failed.");
               return true;
            } else {
               return false;
            }
         }
      });
      return stopped;
   }

   private void waitTaskFinished(long jobExecutionId)
         throws InterruptedException, Exception {
      while (true) {
         Thread.sleep(50);
         TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
         if (TaskRead.Status.COMPLETED.equals(tr.getStatus())) {
            logger.info("===========COMPLETED============");
            break;
         }
         if (TaskRead.Status.FAILED.equals(tr.getStatus())
               || TaskRead.Status.STOPPED.equals(tr.getStatus())) {
            break;
         }
      }
   }

   private void assertTaskFailed(long jobExecutionId) {
      TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
      Assert.assertTrue(TaskRead.Status.FAILED.equals(tr.getStatus()), 
            "Should get task finished successful, but got " + tr.getStatus());
   }

   private void assertTaskSuccess(long jobExecutionId) {
      TaskRead tr = jobManager.getJobExecutionStatus(jobExecutionId);
      Assert.assertTrue(TaskRead.Status.COMPLETED.equals(tr.getStatus()), 
            "Should get task finished successful, but got " + tr.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testCreateCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testCreateClusterFailed() throws Exception {
      ClusterCreate createSpec = ClusterSpecFactory.createDefaultSpec(
            ClusterType.HDFS_MAPRED, Constants.DEFAULT_VENDOR);
      createSpec.setName(TEST_DHCP_CLUSTER_NAME);
      createSpec.setNetworkName(TEST_DHCP_NETWORK_NAME);
      createSpec.setDistro("apache");
      NodeGroupCreate worker = createSpec.getNodeGroup("worker");
      worker.setInstanceNum(1);
      long jobExecutionId = clusterMgr.createCluster(createSpec);
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getStatus() == ClusterStatus.PROVISIONING,
            "Cluster status should be PROVISIONING, but got "
                  + cluster.getStatus());
      stopVmAfterStarted(TEST_DHCP_CLUSTER_NAME + "-worker-0", jobExecutionId);
      waitTaskFinished(jobExecutionId);
      cluster = clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getInstanceNum() == 3,
            "Cluster instance number should be 3, but got "
                  + cluster.getInstanceNum());
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.PROVISION_ERROR,
            "Cluster status should be PROVISION_ERROR, but got " + cluster.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testCreateClusterFailed" })
   @Transactional(propagation = Propagation.NEVER)
   public void testClusterResume() throws Exception {
      long jobExecutionId = clusterMgr.resumeClusterCreation(TEST_DHCP_CLUSTER_NAME);
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getStatus() == ClusterStatus.PROVISIONING,
            "Cluster status should be PROVISIONING, but got "
                  + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      cluster = clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getInstanceNum() == 3,
            "Cluster instance number should be 3, but got "
                  + cluster.getInstanceNum());
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.RUNNING,
            "Cluster status should be RUNNING, but got " + cluster.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testClusterResume" })
   @Transactional(propagation = Propagation.NEVER)
   public void testClusterResizeFailed() throws Exception {
      long jobExecutionId = clusterMgr.resizeCluster(TEST_DHCP_CLUSTER_NAME, "worker", 2);
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getStatus() == ClusterStatus.UPDATING,
            "Cluster status should be UPDATING, but got "
                  + cluster.getStatus());
      stopVmAfterStarted(TEST_DHCP_CLUSTER_NAME + "-worker-1", jobExecutionId);
      waitTaskFinished(jobExecutionId);
      assertTaskFailed(jobExecutionId);
      assertDefinedInstanceNum(TEST_DHCP_CLUSTER_NAME, "worker", 1);

      cluster = clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.RUNNING,
            "Cluster status should be RUNNING, but got " + cluster.getStatus());
      assertTaskFailed(jobExecutionId);
   }

   private void assertDefinedInstanceNum(String clusterName, 
         String groupName, int num) {
      NodeGroupEntity group = clusterEntityMgr.findByName(clusterName, groupName);
      Assert.assertTrue(
            group.getDefineInstanceNum() == num,
            "Group defined instance number should be " + num +
            ", but got " + group.getDefineInstanceNum());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testClusterResizeFailed" })
   @Transactional(propagation = Propagation.NEVER)
   public void testClusterResizeSuccess() throws Exception {
      stopVcVm(TEST_DHCP_CLUSTER_NAME + "-worker-0");
      long jobExecutionId = clusterMgr.resizeCluster(TEST_DHCP_CLUSTER_NAME, "worker", 2);
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(
            cluster.getStatus() == ClusterStatus.UPDATING,
            "Cluster status should be UPDATING, but got "
                  + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      assertTaskSuccess(jobExecutionId);
      assertDefinedInstanceNum(TEST_DHCP_CLUSTER_NAME, "worker", 2);
      NodeEntity node = clusterEntityMgr.findByName(TEST_DHCP_CLUSTER_NAME, 
            "worker", TEST_DHCP_CLUSTER_NAME + "-worker-0");
      Assert.assertTrue(node.getStatus() == NodeStatus.POWERED_OFF,
            "Stopped vm " + TEST_DHCP_CLUSTER_NAME + "-worker-0" +
            " status should be Powered Off, but got " + node.getStatus());
      cluster = clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.RUNNING,
            "Cluster status should be RUNNING, but got " + cluster.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testCreateCluster" })
   public void testDeleteUsedDatastore() {
      try {
         clusterMgr.getClusterConfigMgr().getDatastoreMgr()
               .deleteDatastore(TEST_DATASTORE_NAME);
         assertTrue("should get exception for datastore is used by cluster",
               false);
      } catch (VcProviderException e) {
         e.printStackTrace();
         assertTrue("get exception for datastore is used by cluster.", true);
      }
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testDeleteUsedDatastore" })
   public void testDeleteUsedRP() {
      try {
         clusterMgr.getClusterConfigMgr().getRpMgr()
               .deleteResourcePool(TEST_RP_NAME);
         assertTrue(
               "should get exception for resource pool is used by cluster",
               false);
      } catch (VcProviderException e) {
         e.printStackTrace();
         assertTrue("get exception for resource pool is used by cluster.", true);
      }
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testDeleteUsedRP" })
   public void testGetClusterRead() {
      ClusterEntity cluster =
            clusterEntityMgr.findByName(TEST_STATIC_IP_CLUSTER_NAME);
      assertTrue(cluster != null);
      ClusterRead clusterRead =
            clusterEntityMgr.toClusterRead(TEST_STATIC_IP_CLUSTER_NAME);
      assertTrue("parse ClusterRead object from cluster entity should work.",
            clusterRead != null);
      logger.info((new Gson()).toJson(clusterRead));
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testGetClusterRead" })
   @Transactional
   public void testGetClusterUsedResources() {
      ClusterEntity cluster =
            clusterEntityMgr.findByName(TEST_STATIC_IP_CLUSTER_NAME);

      assertTrue(cluster != null);
      assertTrue(
            "the cluster should have five instances, however the real number is "
                  + cluster.getRealInstanceNum(),
            cluster.getRealInstanceNum() == 5);
      Set<String> patterns = new HashSet<String>();
      String[] dsSpecs = datastoreSpec.split(",");
      for (String dsSpec : dsSpecs) {
         patterns.add(dsSpec);
      }
      assertTrue(
            "the cluster should use datastore " + patterns,
            CommonUtil.matchDatastorePattern(patterns,
                  cluster.getUsedVcDatastores()));
      Set<VcResourcePoolEntity> usedRps = cluster.getUsedRps();

      Set<String> usedRpNames = new HashSet<String>();
      for (VcResourcePoolEntity usedRp : usedRps) {
         usedRpNames.add(usedRp.getName());
      }
      assertTrue("the cluster should use resource pool " + TEST_RP_NAME
            + ", but actually used " + usedRpNames,
            usedRpNames.contains(TEST_RP_NAME));
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testGetClusterUsedResources" })
   public void testGetGroupFromName() {
      ClusterEntity cluster =
            clusterEntityMgr.findByName(TEST_STATIC_IP_CLUSTER_NAME);
      NodeGroupEntity group = clusterEntityMgr.findByName(cluster, "master");
      assertTrue(group != null);
      logger.info("get group master " + group);
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testGetGroupFromName" })
   public void testLimitCluster() {
      //      try {
      //         Long id = clusterManager.limitCluster(CLUSTER_NAME, NODEGROUP_NAME, 1);
      //         TaskEntity task = TaskEntity.findById(id);
      //         task.getTaskListener().onMessage(getSampleMsg(CLUSTER_NAME, true));
      //         assertTrue("task should succeed", waitForTask(task));
      //         ClusterEntity cluster =
      //               ClusterEntity.findClusterEntityByName(CLUSTER_NAME);
      //         assertTrue("cluster " + CLUSTER_NAME + " should be running, but get status: " + cluster.getStatus(),
      //               cluster.getStatus().equals(ClusterStatus.RUNNING));
      //      } catch (Exception e) {
      //      }
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testLimitCluster" })
   public void testResourcePoolList() {
      List<ResourcePoolRead> rps =
            clusterMgr.getClusterConfigMgr().getRpMgr()
                  .getAllResourcePoolForRest();
      logger.info("got resource pools: " + rps);
      for (ResourcePoolRead rp : rps) {
         if (rp.getRpName().equals(TEST_RP_NAME)) {
            logger.info("got resource pool related nodes: " + rp.getNodes().length);
            assertTrue("should get more than or equals to 5, but got " + rps.get(0).getNodes().length, 
                  rps.get(0).getNodes().length >= 5);
         }
      }
  }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testLimitCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testDupCreateCluster() throws Exception {
      ClusterCreate createSpec = new ClusterCreate();
      createSpec.setName(TEST_STATIC_IP_CLUSTER_NAME);
      createSpec.setType(ClusterType.HDFS_MAPRED);
      try {
         clusterMgr.createCluster(createSpec);
         Assert.assertTrue(false, "Cluster creation should throw exception.");
      } catch (Exception e) {
         e.printStackTrace();
         Assert.assertTrue(true, "got expected exception.");
      }
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testDupCreateCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testStopCluster() throws Exception {
      long jobExecutionId = clusterMgr.stopCluster(TEST_STATIC_IP_CLUSTER_NAME);

      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.STOPPING,
            "Cluster status should be STOPPING, but got " + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      cluster = clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.STOPPED,
            "Cluster status should be STOPPED, but got " + cluster.getStatus());
      NodeRead node = cluster.getNodeGroups().get(0).getInstances().get(0);
      Assert.assertTrue(node.getStatus().equals(NodeStatus.POWERED_OFF.toString()),
            "Node " + node.getName() + 
            " status should be Powered Off, but got " + node.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testStopCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testDupStopCluster() throws Exception {
      try {
         clusterMgr.stopCluster(TEST_STATIC_IP_CLUSTER_NAME);
         Assert.assertTrue(false, "Cluster stop should throw exception.");
      } catch (Exception e) {
         e.printStackTrace();
         Assert.assertTrue(true, "got expected exception.");
      }
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.STOPPED,
            "Cluster status should be STOPPED, but got " + cluster.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testDupStopCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testStartCluster() throws Exception {
      long jobExecutionId =
            clusterMgr.startCluster(TEST_STATIC_IP_CLUSTER_NAME);

      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.STARTING,
            "Cluster status should be STARTING, but got " + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      cluster = clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.RUNNING,
            "Cluster status should be RUNNING, but got " + cluster.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testStartCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testDupStartCluster() throws Exception {
      try {
         clusterMgr.startCluster(TEST_STATIC_IP_CLUSTER_NAME);
         Assert.assertTrue(false, "Cluster start should throw exception.");
      } catch (Exception e) {
         e.printStackTrace();
         Assert.assertTrue(true, "got expected exception.");
      }
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.RUNNING,
            "Cluster status should be RUNNING, but got " + cluster.getStatus());
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testDupStartCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testConfigIOShares() throws Exception {
      clusterMgr.prioritizeCluster(TEST_STATIC_IP_CLUSTER_NAME, null,
            Priority.HIGH);
      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      for (NodeGroupRead nodeGroup : cluster.getNodeGroups()) {
         Assert.assertTrue(nodeGroup.getIoShares().equals(Priority.HIGH),
               "Node group " + nodeGroup.getName()
                     + " should have HIGH io share level");
      }
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testConfigIOShares" })
   @Transactional(propagation = Propagation.NEVER)
   public void testDeleteCluster() throws Exception {
      long jobExecutionId =
            clusterMgr.deleteClusterByName(TEST_STATIC_IP_CLUSTER_NAME);

      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.DELETING,
            "Cluster status should be DELETING, but got " + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      try {
         cluster =
               clusterMgr.getClusterByName(TEST_STATIC_IP_CLUSTER_NAME, false);
         Assert.assertTrue(false, "Cluster should not be found.");
      } catch (BddException e) {
         if (e.getErrorId().equals("NOT_FOUND")) {
            Assert.assertTrue(true);
         } else {
            e.printStackTrace();
            Assert.assertTrue(false, "Got unexpected exception");
         }
      }
      assertChildRPRemoved(ConfigInfo.getSerengetiUUID() + "-" 
            + TEST_STATIC_IP_CLUSTER_NAME);
      assertFolderRemoved(TEST_STATIC_IP_CLUSTER_NAME);
   }

   private void assertChildRPRemoved(String rpName) {
      VcResourcePool rp = VcResourceUtils.findRPInVCCluster(vcCluster, vcRP);
      List<VcResourcePool> children = rp.getChildren();
      boolean found = false;
      for (VcResourcePool child : children) {
         if (child.getName().equals(rpName)) {
            found = true;
            break;
         }
      }
      Assert.assertFalse(found, "Resource pool " + rpName + " is not removed.");
   }

   private void assertFolderRemoved(String folderName) throws Exception {
      String rootFolderName = ConfigInfo.getSerengetiRootFolder();
      String serverMobId =
         Configuration.getString(Constants.SERENGETI_SERVER_VM_MOBID);
      VcVirtualMachine serverVm = VcCache.get(serverMobId);
      List<String> folderList = new ArrayList<String>(1);
      folderList.add(rootFolderName);
      Folder rootFolder = VcResourceUtils.findFolderByNameList(
            serverVm.getDatacenter(),  folderList);
      Folder childFolder = VcResourceUtils.findFolderByName(rootFolder, folderName);
      Assert.assertNull(childFolder, "Folder " + folderName + " is not removed.");
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testDeleteCluster" })
   @Transactional(propagation = Propagation.NEVER)
   public void testDupDeleteCluster() throws Exception {
      try {
         clusterMgr.deleteClusterByName(TEST_STATIC_IP_CLUSTER_NAME);
         Assert.assertTrue(false, "Cluster should not be found.");
      } catch (BddException e) {
         if (e.getSection().equals("NOT_FOUND")) {
            Assert.assertTrue(true);
            return;
         }
         e.printStackTrace();
      }
   }

   @Test(groups = { "TestClusteringJobs" }, dependsOnMethods = { "testClusterResizeSuccess" })
   @Transactional(propagation = Propagation.NEVER)
   public void testDeleteDhcpCluster() throws Exception {
      long jobExecutionId =
            clusterMgr.deleteClusterByName(TEST_DHCP_CLUSTER_NAME);

      ClusterRead cluster =
            clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
      Assert.assertTrue(cluster.getStatus() == ClusterStatus.DELETING,
            "Cluster status should be DELETING, but got " + cluster.getStatus());
      waitTaskFinished(jobExecutionId);
      try {
         cluster =
               clusterMgr.getClusterByName(TEST_DHCP_CLUSTER_NAME, false);
         Assert.assertTrue(false, "Cluster should not be found.");
      } catch (BddException e) {
         if (e.getErrorId().equals("NOT_FOUND")) {
            Assert.assertTrue(true);
            return;
         }
         e.printStackTrace();
      }
   }
}
