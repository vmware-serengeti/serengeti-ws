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
package com.vmware.bdd.plugin.clouderamgr.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiHostRef;
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.model.ApiServiceState;
import com.cloudera.api.v1.CommandsResource;
import com.cloudera.api.v6.ClustersResourceV6;
import com.cloudera.api.v6.RolesResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.plugin.clouderamgr.service.cm.FakeClustersResource;
import com.vmware.bdd.plugin.clouderamgr.service.cm.FakeHostsResource;
import com.vmware.bdd.plugin.clouderamgr.service.cm.FakeRolesResource;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudera.api.ApiRootResource;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiHealthSummary;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostList;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleList;
import com.cloudera.api.model.ApiRoleRef;
import com.cloudera.api.v6.RootResourceV6;
import com.cloudera.api.v7.RootResourceV7;
import com.vmware.bdd.plugin.clouderamgr.poller.host.HostInstallPoller;
import com.vmware.bdd.plugin.clouderamgr.service.cm.FakeRootResource;
import com.vmware.bdd.plugin.clouderamgr.service.cm.FakeRootResourceV7;
import com.vmware.bdd.plugin.clouderamgr.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.PreStartServices;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import com.vmware.bdd.utils.CommonUtil;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

/**
 * Author: Xiaoding Bian
 * Date: 7/5/14
 * Time: 9:51 PM
 */
public class TestClouderaManagerImpl {
   private static final Logger logger = Logger.getLogger(TestClouderaManagerImpl.class);

   private static ApiRootResource apiRootResource;
   private static RootResourceV6 rootResourceV6;
   private static RootResourceV7 rootResourceV7;
   private static ClouderaManagerImpl provider;
   private static ClusterBlueprint blueprint;
   private static ClusterReportQueue reportQueue;

   @MockClass(realClass = ClouderaManagerClientBuilder.class)
   public static class MockClouderaManagerClientBuilder {
      private ClouderaManagerClientBuilder builder = new ClouderaManagerClientBuilder();
      @Mock
      public ClouderaManagerClientBuilder withHost(String host) {
         return builder;
      }

      @Mock
      public ClouderaManagerClientBuilder withPort(int port) {
         return builder;
      }

      @Mock
      public ClouderaManagerClientBuilder withUsernamePassword(String user, String password) {
         return builder;
      }

      @Mock
      public ApiRootResource build() {
         return apiRootResource;
      }
   }

   @MockClass(realClass = HostInstallPoller.class)
   public static class MockHostInstallPoller {
      @Mock
      public void setup() {
      }

      @Mock
      public boolean poll() {
         return true;
      }

      @Mock
      public void tearDown() {
      }
   }

   @MockClass(realClass = ReflectionUtils.class)
   public static class MockReflectionUtils {
      @Mock
      public static PreStartServices getPreStartServicesHook() {
         return new PreStartServices() {
            @Override
            public void preStartServices(String clusterName,
                  int maxWaitingSeconds) throws SoftwareManagementPluginException {
            }
         };
      }
   }

   @MockClass(realClass = CmClusterValidator.class)
   public static class MockCmClusterValidator {
      @Mock
      public boolean validateBlueprint(ClusterBlueprint blueprint) {
         return true;
      }
   }

   @BeforeClass( groups = { "TestClouderaManagerImpl" }, dependsOnGroups = { "TestClusterDef" })
   public static void setup() throws IOException {
      Mockit.setUpMock(MockClouderaManagerClientBuilder.class);
      Mockit.setUpMock(MockHostInstallPoller.class);
      Mockit.setUpMock(MockReflectionUtils.class);
      Mockit.setUpMock(MockCmClusterValidator.class);

      apiRootResource = Mockito.mock(ApiRootResource.class);

      rootResourceV6 = new FakeRootResource();
      rootResourceV7 = new FakeRootResourceV7();
      Mockito.when(apiRootResource.getRootV6()).thenReturn(rootResourceV6);
      Mockito.when(apiRootResource.getRootV7()).thenReturn(rootResourceV7);
      Mockito.when(apiRootResource.getCurrentVersion()).thenReturn("v7");

      provider = new ClouderaManagerImpl("127.0.0.1", 7180, "admin", "admin", "RSA_CERT");
      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, CommonUtil.readJsonFile("simple_blueprint.json"));

      reportQueue = new ClusterReportQueue();
   }

   @AfterClass( groups = { "TestClouderaManagerImpl" }, dependsOnGroups = { "TestClusterDef" })
   public static void tearDown() throws IOException, InterruptedException {
      Mockit.tearDownMocks();
   }

   @BeforeMethod(groups = { "TestClouderaManagerImpl" })
   public void setupBeforeMethod() {
   }

   @Test( groups = { "TestClouderaManagerImpl" })
   public void testGetName() {
      Assert.assertEquals(provider.getName(), "ClouderaManager");
   }

   @Test( groups = { "TestClouderaManagerImpl" })
   public void testGetSupportedStacks() {
      List<HadoopStack> stacks = provider.getSupportedStacks();
      Assert.assertTrue(stacks.get(0).getDistro().equals("CDH-5.0.2"));
      Assert.assertTrue(stacks.get(0).getRoles().contains("YARN_NODE_MANAGER"));
      Assert.assertTrue(stacks.get(1).getDistro().equals("CDH-4.7.0"));
      Assert.assertFalse(stacks.get(1).getRoles().contains("YARN_NODE_MANAGER"));
   }

   @Test( groups = { "TestClouderaManagerImpl" })
   public void testGetSupportedConfigs() {
      HadoopStack stack = new HadoopStack();
      stack.setDistro("CDH-5.0.1");
      String configs = provider.getSupportedConfigs(stack);
      System.out.println(configs);
   }

   @Test( groups = { "TestClouderaManagerImpl" })
   public void testCreateCluster() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      // FakeParcelsResource#getParcelResource should print the right parcel version
      provider.createCluster(blueprint, reportQueue);
      provider.reconfigCluster(blueprint, reportQueue);
      provider.onDeleteCluster(blueprint, reportQueue);
      provider.deleteCluster(blueprint, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         System.out.println("Action: " + report.getAction() + ", Progress: " + report.getProgress());
      }
   }

   @Test( groups = { "TestClouderaManagerImpl" })
   public void testCreateNnHa() throws IOException {
      ClusterBlueprint haSpec = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, CommonUtil.readJsonFile("namenode_ha.json"));
      provider.createCluster(haSpec, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         System.out.println("Action: " + report.getAction() + ", Progress: " + report.getProgress());
      }
   }

   @Test( groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testCreateCluster" })
   public void testQueryClusterStatus() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      ApiHostList hostList = rootResourceV6.getHostsResource().readHosts(DataView.SUMMARY);
      List<ApiHost> hosts = hostList.getHosts();
      hosts.get(0).setHealthSummary(ApiHealthSummary.BAD);
      hosts.get(1).setHealthSummary(ApiHealthSummary.CONCERNING);
      hosts.get(2).setHealthSummary(ApiHealthSummary.GOOD);
      hosts.get(3).setHealthSummary(ApiHealthSummary.NOT_AVAILABLE);
      List<ApiRoleRef> roleRefs = new ArrayList<>();
      String hdfsServiceName = blueprint.getName() + "_HDFS";
      String yarnServiceName = blueprint.getName() + "_YARN";
      ApiRoleList roleList =
            rootResourceV6.getClustersResource()
                  .getServicesResource(blueprint.getName())
                  .getRolesResource(hdfsServiceName).readRoles();
      for (ApiRole role : roleList.getRoles()) {
         if (role.getHostRef().getHostId().equals(hosts.get(2).getHostId())) {
            ApiRoleRef roleRef = new ApiRoleRef();
            roleRef.setClusterName(blueprint.getName());
            roleRef.setServiceName(hdfsServiceName);
            roleRef.setRoleName(role.getName());
            roleRefs.add(roleRef);
         }
      }
      roleList =
            rootResourceV6.getClustersResource()
                  .getServicesResource(blueprint.getName())
                  .getRolesResource(yarnServiceName).readRoles();
      for (ApiRole role : roleList.getRoles()) {
         if (role.getHostRef().getHostId().equals(hosts.get(2).getHostId())) {
            ApiRoleRef roleRef = new ApiRoleRef();
            roleRef.setClusterName(blueprint.getName());
            roleRef.setServiceName(yarnServiceName);
            roleRef.setRoleName(role.getName());
            roleRefs.add(roleRef);
         }
      }
      hosts.get(2).setRoleRefs(roleRefs);
      ClusterReport report = provider.queryClusterStatus(blueprint);
      Assert.assertTrue(report.getStatus() != null
            && report.getStatus() == ServiceStatus.STOPPED,
            "Status should be stopped, but got " + report.getStatus());
      System.out.println("Status: " + report.getStatus());
   }

   @Test( groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testQueryClusterStatus" })
   public void testScaleOutCluster() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      // FakeParcelsResource#getParcelResource should print the right parcel version
      List<String> addedNodeNames = new ArrayList<>();
      for (NodeGroupInfo groupInfo : blueprint.getNodeGroups()) {
         int instanceNum = groupInfo.getNodes().size();
         if (instanceNum > 1) {
            addedNodeNames.add(groupInfo.getNodes().get(instanceNum - 1).getName());
         }
      }
      provider.scaleOutCluster(blueprint, addedNodeNames, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         System.out.println("Action: " + report.getAction() + ", Progress: " + report.getProgress());
         if (report.isFinished()) {
            Assert.assertTrue(report.isSuccess(), "Should get success result.");
         }
      }
   }

   //Todo(qjin):check whether return in early stage
   @Test( groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testScaleOutCluster" })
   public void testStartStartedCluster() {
      Mockit.setUpMock(MockReflectionUtils.class);
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      provider.startCluster(blueprint, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         if (report.isFinished()) {
            System.out.println("Starting Services finished.");
            Assert.assertEquals(ServiceStatus.STARTED, report.getStatus(), "Cluster status should be STARTED");
            Assert.assertEquals("", report.getAction(), "Cluster action should be empty");
            Assert.assertTrue(report.isSuccess(), "Should get success result.");
         }
      }
   }

   @Test( groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testStartStartedCluster" })
   public void testStopStartedCluster() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      provider.onStopCluster(blueprint, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         if (!CommonUtil.isBlank(report.getAction())) {
            Assert.assertEquals(reports.get(0).getAction(), "Stopping Services", "Should get Stopping Services action.");
            System.out.println("BDE is Stopping Services");
         }
         if (report.isFinished()) {
            System.out.println("Stopping Services finished.");
            Assert.assertEquals(ServiceStatus.STOPPED, report.getStatus(), "Cluster status should be STOPPED");
            Assert.assertEquals("", report.getAction(), "Cluster action should be empty");
            Assert.assertTrue(report.isSuccess(), "Should get success result.");
         }
      }
   }

   @Test( groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testStopStartedCluster" })
   public void testStopStoppedCluster() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      provider.onStopCluster(blueprint, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         if (report.isFinished()) {
            System.out.println("Stopping Services finished.");
            Assert.assertEquals(ServiceStatus.STOPPED, report.getStatus(), "Cluster status should be STOPPED");
            Assert.assertEquals("", report.getAction(), "Cluster action should be empty");
            Assert.assertTrue(report.isSuccess(), "Should get success result.");
         }
      }
   }

   @Test( groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testStopStoppedCluster" })
   public void testStartStoppedCluster() {
      Mockit.setUpMock(MockReflectionUtils.class);
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      provider.startCluster(blueprint, reportQueue);
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         if (!CommonUtil.isBlank(report.getAction())) {
            Assert.assertEquals(reports.get(0).getAction(), "Starting Services", "Should get Stopping Services action.");
            System.out.println("BDE is Starting Services");
         }
         if (report.isFinished()) {
            System.out.println("Starting Services finished.");
            Assert.assertEquals(ServiceStatus.STARTED, report.getStatus(), "Cluster status should be STARTED");
            Assert.assertEquals("", report.getAction(), "Cluster action should be empty");
            Assert.assertTrue(report.isSuccess(), "Should get success result.");
         }
      }
   }

   @Test (groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testStartStoppedCluster" })
   public void testStopClusterGotException() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      ClustersResourceV6 resourceV6 = Mockito.mock(FakeClustersResource.class);
      Mockito.when(resourceV6.stopCommand(blueprint.getName())).thenThrow(
            SoftwareManagementPluginException.STOP_CLUSTER_FAILED(null, null, blueprint.getName()));
      ServicesResourceV6 servicesResourceV6 = Mockito.mock(ServicesResourceV6.class);
      List<ApiService> serviceList = new ArrayList<>();
      ApiService hdfs = new ApiService();
      hdfs.setName("HDFS");
      hdfs.setServiceState(ApiServiceState.STARTED);
      serviceList.add(hdfs);
      ApiServiceList apiServiceList = new ApiServiceList();
      apiServiceList.setServices(serviceList);
      Mockito.when(servicesResourceV6.readServices((DataView) anyObject())).thenReturn(apiServiceList);

      List<ApiRole> roleList = new ArrayList<>();
      ApiRole role = new ApiRole();
      role.setName("NAMENODE");
      role.setRoleState(ApiRoleState.STARTED);
      role.setType(role.getName());
      ApiHostRef hostRef = new ApiHostRef("host1");
      role.setHostRef(hostRef);
      roleList.add(role);
      RolesResourceV6 rolesResourceV6 = new FakeRolesResource(roleList);
      Mockito.when(servicesResourceV6.getRolesResource(hdfs.getName())).thenReturn(rolesResourceV6);
      Mockito.when(resourceV6.getServicesResource(blueprint.getName())).thenReturn(servicesResourceV6);

      ApiClusterList apiClusterList = new ApiClusterList();
      ApiCluster apiCluster = new ApiCluster();
      apiCluster.setName(blueprint.getName());
      apiClusterList.add(apiCluster);
      Mockito.when(resourceV6.readClusters((DataView) anyObject())).thenReturn(apiClusterList);

      RootResourceV6 fakeRootResourceV6 = Mockito.mock(FakeRootResource.class);
      Mockito.when(fakeRootResourceV6.getClustersResource()).thenReturn(resourceV6);

      FakeHostsResource fakeHostsResource = Mockito.mock(FakeHostsResource.class);
      ApiHost apiHost = Mockito.mock(ApiHost.class);
      Mockito.when(apiHost.getIpAddress()).thenReturn("127.0.0.1");
      Mockito.when(fakeHostsResource.readHost(anyString())).thenReturn(apiHost);
      Mockito.when(fakeRootResourceV6.getHostsResource()).thenReturn(fakeHostsResource);

      Mockito.when(apiRootResource.getRootV6()).thenReturn(fakeRootResourceV6);

      boolean exceptionExist = false;
      try {
         ClouderaManagerImpl mockedProvider = new ClouderaManagerImpl("127.0.0.1", 7180, "admin", "admin", "RSA_CERT");
         mockedProvider.onStopCluster(blueprint, reportQueue);
      } catch (SoftwareManagementPluginException e) {
         exceptionExist = true;
         Assert.assertEquals(e.getMessage(), "An exception happens when App_Manager (ClouderaManager) tries to stop the cluster: cluster01.");
      }
      Assert.assertTrue(exceptionExist);
      Mockito.when(apiRootResource.getRootV6()).thenReturn(rootResourceV6);
   }

   @Test (groups = { "TestClouderaManagerImpl" }, dependsOnMethods = { "testStopClusterGotException" })
   public void testStartClusterGotException() {
      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      provider.onStopCluster(blueprint, reportQueue);

      blueprint.getHadoopStack().setDistro("CDH-5.0.1");
      ClustersResourceV6 resourceV6 = Mockito.mock(FakeClustersResource.class);
      Mockito.when(resourceV6.startCommand(blueprint.getName())).thenThrow(
            SoftwareManagementPluginException.START_CLUSTER_FAILED(null, null, blueprint.getName()));
      ServicesResourceV6 servicesResourceV6 = Mockito.mock(ServicesResourceV6.class);
      List<ApiService> serviceList = new ArrayList<>();
      ApiService hdfs = new ApiService();
      hdfs.setName("HDFS");
      hdfs.setServiceState(ApiServiceState.STOPPED);
      serviceList.add(hdfs);
      ApiServiceList apiServiceList = new ApiServiceList();
      apiServiceList.setServices(serviceList);
      Mockito.when(servicesResourceV6.readServices((DataView) anyObject())).thenReturn(apiServiceList);

      List<ApiRole> roleList = new ArrayList<>();
      ApiRole role = new ApiRole();
      role.setName("NAMENODE");
      role.setRoleState(ApiRoleState.STOPPED);
      role.setType(role.getName());
      ApiHostRef hostRef = new ApiHostRef("host1");
      role.setHostRef(hostRef);
      roleList.add(role);
      RolesResourceV6 rolesResourceV6 = new FakeRolesResource(roleList);
      Mockito.when(servicesResourceV6.getRolesResource(hdfs.getName())).thenReturn(rolesResourceV6);
      Mockito.when(resourceV6.getServicesResource(blueprint.getName())).thenReturn(servicesResourceV6);

      ApiClusterList apiClusterList = new ApiClusterList();
      ApiCluster apiCluster = new ApiCluster();
      apiCluster.setName(blueprint.getName());
      apiClusterList.add(apiCluster);
      Mockito.when(resourceV6.readClusters((DataView) anyObject())).thenReturn(apiClusterList);

      RootResourceV6 fakeRootResourceV6 = Mockito.mock(FakeRootResource.class);
      Mockito.when(fakeRootResourceV6.getClustersResource()).thenReturn(resourceV6);

      FakeHostsResource fakeHostsResource = Mockito.mock(FakeHostsResource.class);
      ApiHost apiHost = Mockito.mock(ApiHost.class);
      Mockito.when(apiHost.getIpAddress()).thenReturn("127.0.0.1");
      Mockito.when(fakeHostsResource.readHost(anyString())).thenReturn(apiHost);
      Mockito.when(fakeRootResourceV6.getHostsResource()).thenReturn(fakeHostsResource);

      Mockito.when(apiRootResource.getRootV6()).thenReturn(fakeRootResourceV6);

      boolean exceptionExist = false;
      try {
         ClouderaManagerImpl mockedProvider = new ClouderaManagerImpl("127.0.0.1", 7180, "admin", "admin", "RSA_CERT");
         mockedProvider.startCluster(blueprint, reportQueue);
      } catch (SoftwareManagementPluginException e) {
         exceptionExist = true;
         Assert.assertEquals(e.getMessage(), "App_Manager (ClouderaManager) fails to start the cluster: cluster01.");
      }
      Assert.assertTrue(exceptionExist);
      Mockito.when(apiRootResource.getRootV6()).thenReturn(rootResourceV6);
   }

   private ClouderaManagerImpl testValidateServerVersionHelper(String version) {
      ClouderaManagerImpl mockedProvider = Mockito.mock(ClouderaManagerImpl.class);
      Mockito.when(mockedProvider.getVersion()).thenReturn(version);
      Mockito.when(mockedProvider.validateServerVersion()).thenCallRealMethod();
      Mockito.when(mockedProvider.getType()).thenCallRealMethod();
      return mockedProvider;
   }

   @Test( groups = { "TestClouderaManagerImpl" } )
   public void testValidateServerVersionFailed() {
      String invalidVersion = "4.9.0";
      ClouderaManagerImpl mockedProvider = testValidateServerVersionHelper(invalidVersion);
      boolean exceptionExists = false;
      try {
         mockedProvider.validateServerVersion();
      } catch (SoftwareManagerCollectorException e) {
         exceptionExists = true;
         String expectedErrMsg =  "The min supported version of software manager type " + mockedProvider.getType() +
               " is " + mockedProvider.MIN_SUPPORTED_VERSION + " but got " + mockedProvider.getVersion() + ".";
         Assert.assertEquals(e.getMessage(), expectedErrMsg);
      }
      Assert.assertTrue(exceptionExists);
   }

   @Test( groups = { "TestClouderaManagerImpl" } )
   public void testValidateServerVersionSucceed() {
      String validVersion = "5.0.0";
      ClouderaManagerImpl mockedProvider = testValidateServerVersionHelper(validVersion);
      boolean exceptionExists = false;
      try {
         mockedProvider.validateServerVersion();
      } catch (SoftwareManagerCollectorException e) {
         exceptionExists = true;
         String expectedErrMsg =  "The min supported version of software manager type " + mockedProvider.getType() +
               " is " + mockedProvider.MIN_SUPPORTED_VERSION + " but got " + mockedProvider.getVersion() + ".";
         Assert.assertEquals(e.getMessage(), expectedErrMsg);
      }
      Assert.assertFalse(exceptionExists);
   }

   @Test ( groups = {"TestClouderaManagerImpl"}, dependsOnMethods = { "testStartStoppedCluster" } )
   public void testOnDeleteNodes() {
      List<String> nodeNames = new ArrayList<>();
      List<NodeGroupInfo> nodeGroupInfos = blueprint.getNodeGroups();
      for (NodeGroupInfo info : nodeGroupInfos) {
         List<NodeInfo> nodeInfos = info.getNodes();
         for (NodeInfo nodeInfo : nodeInfos) {
            if (!CommonUtil.isBlank(nodeInfo.getName())) {
               nodeNames.add(nodeInfo.getName());
            }
         }
      }
      Assert.assertTrue(!nodeNames.isEmpty());
      Assert.assertTrue(provider.onDeleteNodes(blueprint, nodeNames));
   }

   @Test (groups = { "TestClouderaManagerImpl" })
   public void testGetSummaryErrorMsg() {
      Method method = null;
      try {
         method = ClouderaManagerImpl.class.getDeclaredMethod("getSummaryErrorMsg", ApiCommand.class, String.class);
         method.setAccessible(true);
         ApiCommand apiCommand = new ApiCommand();
         apiCommand.setId(new Long(1));
         CommandsResource fakeCommandResource = new CommandsResource() {
            @Override
            public ApiCommand readCommand(long commandId) {
               ApiCommand command = new ApiCommand();
               command.setId(new Long(1));
               command.setResultMessage("TestGetSummaryErrorMsg");
               return command;
            }
            @Override
            public ApiCommand abortCommand(long commandId) {
               return null;
            }
         };
         RootResourceV6 mockRootResourceV6 = Mockito.mock(RootResourceV6.class);
         Mockito.when(mockRootResourceV6.getCommandsResource()).thenReturn(fakeCommandResource);
         Mockito.when(apiRootResource.getRootV6()).thenReturn(mockRootResourceV6);
         ClouderaManagerImpl localProvider = new ClouderaManagerImpl("127.0.0.1", 7180, "admin", "admin", "RSA_CERT");
         Object errMsg = method.invoke(localProvider, apiCommand, "http://domain");
         Assert.assertTrue(errMsg instanceof String);
         Assert.assertEquals(errMsg, "TestGetSummaryErrorMsg. Please refer to http://domain/cmf/command/1/details for details");
         //should not throw exception, if got exception, use assertTrue(false) to break ut actively
      } catch (NoSuchMethodException e) {
         Assert.assertTrue(false);
      } catch (InvocationTargetException e) {
         Assert.assertTrue(false);
      } catch (IllegalAccessException e) {
         Assert.assertTrue(false);
      } finally {
         if (method != null) {
            method.setAccessible(false);
         }
         Mockito.when(apiRootResource.getRootV6()).thenReturn(rootResourceV6);
      }
   }

}
