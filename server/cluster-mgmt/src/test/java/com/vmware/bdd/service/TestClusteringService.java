package com.vmware.bdd.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mockit.Mockit;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.aurora.composition.DiskSchema;
import com.vmware.aurora.composition.NetworkSchema;
import com.vmware.aurora.composition.NetworkSchema.Network;
import com.vmware.aurora.composition.ResourceSchema;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NetworkAdd;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.manager.MockResourceManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.MockTmScheduler.VmOperation;
import com.vmware.bdd.service.impl.ClusteringService;

public class TestClusteringService {
   private static final Logger logger = Logger
         .getLogger(TestClusteringService.class);
   private static ClusteringService service;

   @AfterMethod(groups = { "TestClusteringService" })
   public void cleanFlag() {
      MockTmScheduler.cleanFlag();
      Mockit.tearDownMocks();
   }

   @BeforeMethod(groups = { "TestClusteringService" })
   public void setMockup() {
      Mockit.setUpMock(MockResourceManager.class);
      Mockit.setUpMock(MockTmScheduler.class);
      Mockit.setUpMock(MockVcResourceUtils.class);
      Mockit.setUpMock(MockVcVmUtil.class);
   }

   @BeforeClass(groups = { "TestClusteringService" })
   public static void setUp() throws Exception {
      service = new ClusteringService();
      service.setResMgr(new MockResourceManager());

      // mock a VcVm
      VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
      Mockito.when(vm.getName()).thenReturn("template-vm");
      Mockito.when(vm.getDatacenter()).thenReturn(
            Mockito.mock(VcDatacenter.class));

      // set vcVm field
      Field field = service.getClass().getDeclaredField("templateVm");
      field.setAccessible(true);
      field.set(service, vm);
   }

   @Test(groups = { "TestClusteringService" })
   public void testReserveResource() {
      UUID uuid = service.reserveResource("testCluster");
      try {
         service.reserveResource("testCluster1");
         Assert.assertTrue(false,
               "Should not reserve resource, since one is already reserved resource.");
      } catch (VcProviderException e) {
         Assert.assertTrue(true, "Got expected exception.");
      }

      service.commitReservation(uuid);
   }

   @Test(groups = { "TestClusteringService" }, dependsOnMethods = { "testReserveResource" })
   public void testCreateDhcpVmFolderFailed() {
      NetworkAdd networkAdd = createNetworkAdd();
      List<BaseNode> vNodes = new ArrayList<BaseNode>();
      BaseNode node = new BaseNode("test-master-0");
      ClusterCreate spec = createClusterSpec();
      node.setCluster(spec);
      vNodes.add(node);
      MockTmScheduler.setFlag(VmOperation.CREATE_FOLDER, false);
      try {
         service.createVcVms(networkAdd, vNodes, null, null);
         Assert.assertTrue(false, "should throw exception but not.");
      } catch (Exception e) {
         logger.info(e.getMessage(), e);
         Assert.assertTrue(true, "got expected exception.");
      }
   }

   @Test(groups = { "TestClusteringService" }, dependsOnMethods = { "testCreateDhcpVmFolderFailed" })
   public void testCreateDhcpVmNullResult() {
      NetworkAdd networkAdd = createNetworkAdd();
      List<BaseNode> vNodes = new ArrayList<BaseNode>();
      BaseNode node = new BaseNode("test-master-0");
      ClusterCreate spec = createClusterSpec();
      node.setCluster(spec);
      vNodes.add(node);
      MockTmScheduler.setResultIsNull(true);
      try {
         service.createVcVms(networkAdd, vNodes, null, null);
         Assert.assertTrue(false, "should throw exception but not.");
      } catch (Exception e) {
         logger.info(e.getMessage(), e);
         Assert.assertTrue(true, "got expected exception.");
      }
   }

   @Test(groups = { "TestClusteringService" }, dependsOnMethods = { "testCreateDhcpVmNullResult" })
   public void testCreateDhcpVmCreateVmFail() throws Exception {
      NetworkAdd networkAdd = createNetworkAdd();
      List<BaseNode> vNodes = new ArrayList<BaseNode>();
      BaseNode node = new BaseNode("test-master-0");
      // create cluster spec
      ClusterCreate spec = createClusterSpec();
      node.setCluster(spec);
      node.setNodeGroup(spec.getNodeGroup("master"));
      node.setTargetVcCluster("cluster-ws");
      vNodes.add(node);
      // create vm schema
      VmSchema vmSchema = createVmSchema();
      node.setVmSchema(vmSchema);

      // mock a clusterEntityMgr and node group entity
      NodeGroupEntity nodeGroup = Mockito.mock(NodeGroupEntity.class);
      Mockito.when(nodeGroup.getIoShares()).thenReturn(Priority.High);
      ClusterEntityManager entityMgr = Mockito.mock(ClusterEntityManager.class);
      Mockito.when(entityMgr.findByName("test", "master"))
            .thenReturn(nodeGroup);
      Field field = service.getClass().getDeclaredField("clusterEntityMgr");
      field.setAccessible(true);
      field.set(service, entityMgr);

      MockTmScheduler.setFlag(VmOperation.CREATE_FOLDER, true);
      MockTmScheduler.setFlag(VmOperation.CREATE_VM, false);

      boolean success = service.createVcVms(networkAdd, vNodes, null, null);
      Assert.assertTrue(!success, "should get create vm failed.");
   }

   private NetworkAdd createNetworkAdd() {
      NetworkAdd networkAdd = new NetworkAdd();
      networkAdd.setDhcp(true);
      networkAdd.setPortGroup("testGroup");
      return networkAdd;
   }

   private VmSchema createVmSchema() {
      VmSchema vmSchema = new VmSchema();
      vmSchema.diskSchema = new DiskSchema();
      vmSchema.networkSchema = new NetworkSchema();
      vmSchema.networkSchema.networks = new ArrayList<Network>();
      vmSchema.resourceSchema = new ResourceSchema();
      return vmSchema;
   }

   private ClusterCreate createClusterSpec() {
      ClusterCreate spec = new ClusterCreate();
      spec.setName("test");
      NodeGroupCreate[] nodeGroups = new NodeGroupCreate[1];
      NodeGroupCreate group = new NodeGroupCreate();
      group.setVmFolderPath("root/test/master");
      group.setName("master");
      nodeGroups[0] = group;
      spec.setNodeGroups(nodeGroups);
      return spec;
   }

   @Test(groups = { "TestClusteringService" }, dependsOnMethods = { "testCreateDhcpVmCreateVmFail" })
   public void testCreateDhcpVmCreateVmPass() throws Exception {
      NetworkAdd networkAdd = createNetworkAdd();
      List<BaseNode> vNodes = new ArrayList<BaseNode>();
      BaseNode node = new BaseNode("test-master-0");
      ClusterCreate spec = createClusterSpec();
      node.setCluster(spec);
      node.setNodeGroup(spec.getNodeGroup("master"));
      node.setTargetVcCluster("cluster-ws");
      vNodes.add(node);
      // create vm schema
      VmSchema vmSchema = createVmSchema();
      node.setVmSchema(vmSchema);

      // mock a clusterEntityMgr and node group entity
      NodeGroupEntity nodeGroup = Mockito.mock(NodeGroupEntity.class);
      Mockito.when(nodeGroup.getIoShares()).thenReturn(Priority.High);

      ClusterEntityManager entityMgr = Mockito.mock(ClusterEntityManager.class);
      Mockito.when(entityMgr.findByName("test", "master"))
            .thenReturn(nodeGroup);
      Field field = service.getClass().getDeclaredField("clusterEntityMgr");
      field.setAccessible(true);
      field.set(service, entityMgr);

      MockTmScheduler.setFlag(VmOperation.CREATE_FOLDER, true);
      MockTmScheduler.setFlag(VmOperation.CREATE_VM, true);
      boolean success = service.createVcVms(networkAdd, vNodes, null, null);
      Assert.assertTrue(success, "should get create vm success.");
   }

   @Test(groups = { "TestClusteringService" }, dependsOnMethods = { "testCreateDhcpVmCreateVmPass" })
   public void testConfigIOShares() {
      List<NodeEntity> targetNodes = new ArrayList<NodeEntity>();
      NodeEntity node1 = new NodeEntity();
      node1.setVmName("cluster-data-0");
      node1.setMoId("vm-1101");
      targetNodes.add(node1);
      NodeEntity node2 = new NodeEntity();
      node1.setVmName("cluster-data-1");
      node1.setMoId("vm-1102");
      targetNodes.add(node2);
      MockTmScheduler.setFlag(VmOperation.RECONFIGURE_VM, true);
      int done = service.configIOShares("cluster", targetNodes, Priority.High);
      Assert.assertTrue(done == 2, "2 nodes been configured IO share level");
   }
}
