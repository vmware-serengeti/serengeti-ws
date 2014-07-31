package com.vmware.bdd.plugin.ambari.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequest;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
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
   //   private static final Logger logger = Logger.getLogger(TestAmbariImpl.class);
   private static AmbariImpl provider;

   @BeforeClass(groups = { "TestAmbariImpl" })
   public static void setup() {
      //provider = new AmbariImpl("10.141.73.103", 8080, "admin", "admin", null);
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
      provider = new AmbariImpl("1.1.1.1", 8080, "admin", "admin", null);
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
      Assert.assertTrue(report.getStatus() == ServiceStatus.STARTED);
      report.getNodeReports();
      for (NodeReport nodeReport : report.getNodeReports().values()) {
         Assert.assertTrue(nodeReport.getStatus() == ServiceStatus.STARTED);
      }
   }

   //@Test(groups = { "TestAmbariImpl" })
   public void testEcho() throws IOException {
      Assert.assertTrue(provider.echo());
   }

   //@Test(groups = { "TestAmbariImpl" })
   public void testGetStatus() throws IOException {
      Assert.assertTrue(provider.getStatus().equals(HealthStatus.Connected));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetSupportedRoles() throws IOException {

   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetSupportedStacks() throws IOException {

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
