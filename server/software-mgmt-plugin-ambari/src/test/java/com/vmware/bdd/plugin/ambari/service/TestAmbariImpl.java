package com.vmware.bdd.plugin.ambari.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeRootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeStacks2Resource;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.plugin.ambari.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager.HealthStatus;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.utils.CommonUtil;

public class TestAmbariImpl {

   private static ApiRootResource apiRootResource;
   private static RootResourceV1 rootResourceV1;
   private static AmbariImpl provider;
   private static ClusterBlueprint blueprint;
   private static ClusterReportQueue reportQueue;
   private static FakeStacks2Resource stacks2Resource;

   @MockClass(realClass = ApiManager.class)
   public static class MockApiManager {

      public void ApiManager(String amServerHost, int port, String user,
            String password) {

      }

      @Mock
      public ApiStackList getStackList() {
         String stacks =
               new FakeStacks2Resource().readStacks().readEntity(String.class);
         return ApiUtils.jsonToObject(ApiStackList.class, stacks);
      }
   }

   @MockClass(realClass = AmbariManagerClientbuilder.class)
   public static class MockAmbariManagerClientbuilder {

      private AmbariManagerClientbuilder builder =
            new AmbariManagerClientbuilder();

      @Mock
      public AmbariManagerClientbuilder withHost(String host) {
         return builder;
      }

      @Mock
      public AmbariManagerClientbuilder withPort(int port) {
         return builder;
      }

      @Mock
      public AmbariManagerClientbuilder withBaseURL(URL url) {
         return builder;
      }

      @Mock
      public AmbariManagerClientbuilder withUsernamePassword(String user,
            String password) {
         return builder;
      }

      @Mock
      public ApiRootResource build() {
         return apiRootResource;
      }
   }

   @MockClass(realClass = AmClusterValidator.class)
   public static class MockAmClusterValidator {
      @Mock
      public boolean validateBlueprint(ClusterBlueprint blueprint) {
         return true;
      }
   }

   @BeforeClass(groups = { "TestClouderaManagerImpl" }, dependsOnGroups = { "TestClusterDef" })
   public static void setup() throws IOException {
      Mockit.setUpMock(MockAmbariManagerClientbuilder.class);
      Mockit.setUpMock(MockAmClusterValidator.class);

      apiRootResource = Mockito.mock(ApiRootResource.class);

      rootResourceV1 = new FakeRootResourceV1();
      Mockito.when(apiRootResource.getRootV1()).thenReturn(rootResourceV1);

      provider =
            new AmbariImpl("127.0.0.1", 8080, "admin", "admin", "RSA_CERT");
      blueprint =
            SerialUtils.getObjectByJsonString(ClusterBlueprint.class,
                  CommonUtil.readJsonFile("simple_blueprint.json"));

      reportQueue = new ClusterReportQueue();
   }

   private void testStatusQuery() {
      provider = new AmbariImpl("1.1.1.1", 8080, "admin", "admin", null);
      ClusterBlueprint blueprint = new ClusterBlueprint();
      blueprint.setHadoopStack(new HadoopStack());
      blueprint.getHadoopStack().setVendor("HDP");
      blueprint.getHadoopStack().setFullVersion("2.1");
      blueprint.setName("am_default");
      List<NodeGroupInfo> nodeGroups = new ArrayList<NodeGroupInfo>();
      NodeGroupInfo group = new NodeGroupInfo();
      nodeGroups.add(group);
      blueprint.setNodeGroups(nodeGroups);
      group.setInstanceNum(5);
      List<NodeInfo> nodes = new ArrayList<NodeInfo>();
      group.setNodes(nodes);
      List<String> roles = new ArrayList<String>();
      group.setRoles(roles);
      NodeInfo node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-72-147.eng.vmware.com");
      node.setName("test_vm_1");
      node.setVolumes(new ArrayList<String>());
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-069.eng.vmware.com");
      node.setName("test_vm_2");
      node.setVolumes(new ArrayList<String>());
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-110.eng.vmware.com");
      node.setName("test_vm_3");
      node.setVolumes(new ArrayList<String>());
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-143.eng.vmware.com");
      node.setName("test_vm_4");
      node.setVolumes(new ArrayList<String>());
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-232.eng.vmware.com");
      node.setName("test_vm_5");
      node.setVolumes(new ArrayList<String>());
      nodes.add(node);
      ClusterReport report = provider.queryClusterStatus(blueprint);
      Assert.assertTrue(report.getStatus() == ServiceStatus.STARTED);
      report.getNodeReports();
      for (NodeReport nodeReport : report.getNodeReports().values()) {
         Assert.assertTrue(nodeReport.getStatus() == ServiceStatus.STARTED);
      }
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetName() {
      Assert.assertEquals(provider.getName(), Constants.AMBARI_PLUGIN_NAME);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetDescription() {
      // TODO
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetType() {
      Assert.assertEquals(provider.getType(), Constants.AMBARI_PLUGIN_NAME);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetSupportedStacks() {
      // TODO
      //provider.getSupportedStacks();
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testEcho() throws IOException {
      Assert.assertTrue(provider.echo());
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetStatus() throws IOException {
      Assert.assertTrue(provider.getStatus().equals(HealthStatus.Connected));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetSupportedRoles() throws IOException {

   }

   private void testScaleOut() {
      String sshKey = null;
      try {
         sshKey = CommonUtil.dataFromFile("/localpath");
      } catch (Exception e) {
      }
      URL url = null;
      try {
         url = new URL("http://1.1.1.1:8080");
      } catch (Exception e) {
         e.printStackTrace();
      }
      provider = new AmbariImpl(url, "admin", "admin", sshKey);
      ClusterBlueprint blueprint = new ClusterBlueprint();
      blueprint.setHadoopStack(new HadoopStack());
      blueprint.getHadoopStack().setFullVersion("2.1");
      blueprint.getHadoopStack().setVendor("HDP");
      blueprint.setName("am3");
      List<NodeGroupInfo> nodeGroups = new ArrayList<NodeGroupInfo>();
      NodeGroupInfo group = new NodeGroupInfo();
      nodeGroups.add(group);
      blueprint.setNodeGroups(nodeGroups);
      group.setInstanceNum(5);
      group.setRoles(new ArrayList<String>());
      group.getRoles().add("DATANODE");
      group.getRoles().add("HDFS_CLIENT");
      group.getRoles().add("NODEMANAGER");
      group.getRoles().add("YARN_CLIENT");
      group.getRoles().add("MAPREDUCE2_CLIENT");
      group.getRoles().add("ZOOKEEPER_CLIENT");
      List<NodeInfo> nodes = new ArrayList<NodeInfo>();
      group.setNodes(nodes);
      NodeInfo node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-238.eng.vmware.com");
      node.setName("test_vm_1");
      List<String> volumes = new ArrayList<>();
      node.setVolumes(volumes);
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-72-139.eng.vmware.com");
      node.setName("test_vm_2");
      volumes = new ArrayList<>();
      node.setVolumes(volumes);
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-082.eng.vmware.com");
      node.setName("test_vm_3");
      volumes = new ArrayList<>();
      node.setVolumes(volumes);
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-093.eng.vmware.com");
      node.setName("test_vm_4");
      volumes = new ArrayList<>();
      node.setVolumes(volumes);
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-72-198.eng.vmware.com");
      node.setName("am3-worker-4");
      nodes.add(node);
      volumes = new ArrayList<>();
      volumes.add("/mnt/scsi-36000c295951affd8a255d3201a7dd9dd-part1");
      node.setVolumes(volumes);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-178.eng.vmware.com");
      node.setName("am3-worker-5");
      nodes.add(node);
      volumes = new ArrayList<>();
      volumes.add("/mnt/scsi-36000c295951affd8a255d3201a7dd9dd-part1");
      node.setVolumes(volumes);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-157.eng.vmware.com");
      node.setName("am3-worker-3");
      nodes.add(node);
      volumes = new ArrayList<>();
      volumes.add("/mnt/scsi-36000c295951affd8a255d3201a7dd9dd-part1");
      node.setVolumes(volumes);
      List<String> addedNodeNames = new ArrayList<String>();
      addedNodeNames.add("am3-worker-4");
      addedNodeNames.add("am3-worker-5");
      addedNodeNames.add("am3-worker-3");
      ClusterReportQueue reports = new ClusterReportQueue();
      try {
         provider.scaleOutCluster(blueprint, addedNodeNames, reports);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
