package com.vmware.bdd.service.job.software;

import java.util.HashMap;
import java.util.Map;

import mockit.Mockit;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.manager.TestClusterEntityManager;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.MockVcCache;
import com.vmware.bdd.service.sp.MockVcContext;
import com.vmware.bdd.software.mgmt.plugin.exception.InfrastructureException;
import com.vmware.bdd.utils.Constants;

@ContextConfiguration(locations = { "classpath:/spring/*-context.xml",
      "classpath:/spring/preconfig-context.xml" })
public class TestPreConfigAop extends AbstractTestNGSpringContextTests {
   private static final Logger logger = Logger
         .getLogger(TestPreConfigAop.class);
   private static final String TEST_CLUSTER_NAME = "aopTestCluster";
   public static final String VM_MOB_PREFIX = "vm:getDiskFormat";
   private static int nodeNum;

   @Autowired
   private MockPreConfigUser user;

   @Autowired
   private IClusterEntityManager clusterEntityMgr;


   @BeforeClass
   public static void init() {
      Scheduler.init(Constants.DEFAULT_SCHEDULER_POOL_SIZE,
            Constants.DEFAULT_SCHEDULER_POOL_SIZE);
   }

   @BeforeMethod
   public void setUp() {
      Mockit.setUpMock(MockVcContext.class);
      Mockit.setUpMock(MockVcCache.class);
      ClusterEntity cluster = clusterEntityMgr.findByName(TEST_CLUSTER_NAME);
      if (cluster != null) {
         clusterEntityMgr.delete(cluster);
      }
      cluster =
            TestClusterEntityManager.assembleClusterEntity(TEST_CLUSTER_NAME);
      int i = 0;
      for (NodeGroupEntity group : cluster.getNodeGroups()) {
         for (NodeEntity node : group.getNodes()) {
            node.setMoId(VM_MOB_PREFIX + i++);
         }
      }
      nodeNum = i;
      clusterEntityMgr.insert(cluster);
   }

   @AfterMethod
   public void tearDown() {
      ClusterEntity cluster = clusterEntityMgr.findByName(TEST_CLUSTER_NAME);
      if (cluster != null) {
         clusterEntityMgr.delete(cluster);
      }
      Mockit.tearDownMocks();
   }

   @Test
   public void testPreConfigEmptyVariable() {
      Thread updator = new VmStatusUpdator(0, nodeNum, false);
      updator.start();

      try {
         user.testAop(TEST_CLUSTER_NAME, 20);
         updator.join();
      } catch (InfrastructureException e) {
         logger.info("Got exception " + e.getFailedMsgList());
         Assert.assertTrue(
               e.getFailedMsgList().toString()
                     .contains("Failed to get disk format status"),
                     "Should get exception: Failed to get disk format status");
      } catch (Exception e) {
         Assert.assertTrue(false, "Unexpected exception" + e.getMessage());
      }
   }

   @Test
   public void testPreConfigFinishedFailed() {
      Thread updator = new VmStatusUpdator(1, nodeNum, false);
      updator.start();

      try {
         user.testAop(TEST_CLUSTER_NAME, 20);
         updator.join();
      } catch (InfrastructureException e) {
         logger.info("Got exception " + e.getFailedMsgList());
         Assert.assertTrue(
               e.getFailedMsgList().toString()
                     .contains("Failed to get disk format status"),
                     "Should get exception: Failed to get disk format status");
      } catch (Exception e) {
         Assert.assertTrue(false, "Unexpected exception" + e.getMessage());
      }
   }

   @Test
   public void testPreConfigInProgress() {
      Thread updator = new VmStatusUpdator(0, nodeNum, true);
      updator.start();

      try {
         user.testAop(TEST_CLUSTER_NAME, 20);
         updator.join();
      } catch (InfrastructureException e) {
         logger.info("Got exception " + e.getFailedMsgList());
         Assert.assertTrue(
               e.getFailedMsgList().toString()
                     .contains("Failed to get disk format status"),
                     "Should get exception: Failed to get disk format status");
      } catch (Exception e) {
         Assert.assertTrue(false, "Unexpected exception" + e.getMessage());
      }
   }

   @Test
   public void testPreConfigFinishedSucc() {
      Thread updator = new VmStatusUpdator(1, nodeNum, true);
      updator.start();

      try {
         user.testAop(TEST_CLUSTER_NAME, 20);
         updator.join();
      } catch (InfrastructureException e) {
         logger.error("Got exception " + e.getFailedMsgList());
         Assert.assertTrue(false,
                     "Should get success result.");
      } catch (Exception e) {
         Assert.assertTrue(false, "Unexpected exception" + e.getMessage());
      }
   }

   private static class VmStatusUpdator extends Thread {
      private int settings;
      private int nodeNum;
      private boolean flag;

      public VmStatusUpdator(int maxSettings, int nodeNum, boolean flag) {
         this.settings = maxSettings;
         this.nodeNum = nodeNum;
         this.flag = flag;
      }

      public void run() {
         try {
            this.sleep(2000);
         } catch (InterruptedException e) {
            logger.error("VM status updator is interrupted.");
            return;
         }
         for (int j = 0; j < nodeNum; j++) {
            VcVirtualMachine vm = MockVcCache.getIgnoreMissing(VM_MOB_PREFIX + j);
            Map<String, String> map = new HashMap<String, String>();
            switch (settings) {
            case 0:
               if (flag) {
                  map.put(Constants.VM_DISK_FORMAT_STATUS_KEY, "1");
               }
               break;
            case 1:
               if (flag) {
                  map.put(Constants.VM_DISK_FORMAT_STATUS_KEY, "0");
               } else {
                  map.put(Constants.VM_DISK_FORMAT_STATUS_KEY, "1");
               }
               break;
            default:
               break;
            }
            Mockito.when(vm.getGuestVariables()).thenReturn(map);
         }
      }
   }
}
