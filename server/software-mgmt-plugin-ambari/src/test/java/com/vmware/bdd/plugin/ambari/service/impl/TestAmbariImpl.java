package com.vmware.bdd.plugin.ambari.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.service.AmbariImpl;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager.HealthStatus;

public class TestAmbariImpl {
//   private static final Logger logger = Logger.getLogger(TestAmbariImpl.class);
   private static AmbariImpl provider;

   @BeforeClass(groups = { "TestAmbariImpl" })
   public static void setup() {
      provider = new AmbariImpl("10.141.73.103", 8080, "admin", "admin", null);
   }

   @AfterClass(groups = { "TestAmbariImpl" })
   public static void tearDown() {

   }

   @BeforeMethod(groups = { "TestAmbariImpl" })
   public void beforeMethod() {

   }

   @AfterMethod(groups = { "TestAmbariImpl" })
   public void afterMethod() {

   }

   @Test(groups = { "TestAmbariImpl" })
   public void testInitializeCluster() throws IOException {

   }

   private void testStatusQuery() {
      provider = new AmbariImpl("10.141.73.95", 8080, "admin", "admin", null);
      ClusterBlueprint blueprint = new ClusterBlueprint();
      blueprint.setHadoopStack(new HadoopStack());
      blueprint.setName("cluster");
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
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-069.eng.vmware.com");
      node.setName("test_vm_2");
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-110.eng.vmware.com");
      node.setName("test_vm_3");
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-143.eng.vmware.com");
      node.setName("test_vm_4");
      nodes.add(node);
      node = new NodeInfo();
      node.setHostname("wdc-vhadp-pub2-dhcp-73-232.eng.vmware.com");
      node.setName("test_vm_5");
      nodes.add(node);
      ClusterReport report = provider.queryClusterStatus(blueprint);
      Assert.assertTrue(report.getStatus() == ServiceStatus.RUNNING);
      report.getNodeReports();
      for (NodeReport nodeReport : report.getNodeReports().values()) {
         Assert.assertTrue(nodeReport.getStatus() == ServiceStatus.RUNNING);
      }
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

   @Test(groups = { "TestAmbariImpl" })
   public void testGetSupportedStacks() throws IOException {

   }
}
