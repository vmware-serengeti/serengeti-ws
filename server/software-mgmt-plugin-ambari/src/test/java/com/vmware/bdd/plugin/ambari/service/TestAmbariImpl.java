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
package com.vmware.bdd.plugin.ambari.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiGetRequestInfo;
import com.vmware.bdd.plugin.ambari.exception.AmException;
import com.vmware.bdd.plugin.ambari.model.AmClusterDef;
import com.vmware.bdd.plugin.ambari.service.am.FakeApiManager;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeRootResourceV1;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.plugin.ambari.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.intf.PreStartServices;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager.HealthStatus;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import com.vmware.bdd.utils.CommonUtil;

public class TestAmbariImpl {

   @MockClass(realClass = ReflectionUtils.class)
   public static class MockReflectionUtils {
      @Mock
      public static PreStartServices getPreStartServicesHook() {
         return new PreStartServices() {
            @Override
            public void preStartServices(String clusterName, List<String> addedNodeNames, boolean force) throws SoftwareManagementPluginException {
            }
            @Override
            public void preStartServices(String clusterName) throws SoftwareManagementPluginException {
            }
         };
      }
   }

   private static AmbariImpl provider;
   private static ClusterBlueprint blueprint;
   private static ClusterReportQueue reportQueue;

   @BeforeClass(groups = { "TestAmbariImpl" })
   public static void setup() throws IOException {
      //Mock static utility using Mockit.
      Mockit.setUpMock(MockReflectionUtils.class);

      ApiRootResource apiRootResource = Mockito.mock(ApiRootResource.class);
      RootResourceV1 rootResourceV1 = new FakeRootResourceV1();
      Mockito.when(apiRootResource.getRootV1()).thenReturn(rootResourceV1);

      AmbariManagerClientbuilder clientbuilder = Mockito.mock(AmbariManagerClientbuilder.class);
      Mockito.when(clientbuilder.build()).thenReturn(apiRootResource);

      AmClusterValidator validator = Mockito.mock(AmClusterValidator.class);
      Mockito.when(validator.validateBlueprint(blueprint)).thenReturn(true);

      provider = new AmbariImpl(clientbuilder, "RSA_CERT");
      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class,
            CommonUtil.readJsonFile("simple_blueprint.json"));

      reportQueue = new ClusterReportQueue();
   }

   @AfterClass(groups = { "TestAmbariImpl" })
   public static void tearDown() {
      //clean mock static utility using Mockit.
      Mockit.tearDownMocks();
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
      List<HadoopStack> supportedStacks = provider.getSupportedStacks();
      for (HadoopStack supportedStack : supportedStacks) {
         if (supportedStack.getFullVersion().equals(blueprint.getHadoopStack().getFullVersion())) {
            Assert.assertEquals(supportedStack.getDistro(), blueprint.getHadoopStack().getDistro());
            Assert.assertEquals(supportedStack.getVendor(), blueprint.getHadoopStack().getVendor());
         }
      }
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testEcho() {
      Assert.assertTrue(provider.echo());
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetStatus() {
      Assert.assertTrue(provider.getStatus().equals(HealthStatus.Connected));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testGetSupportedConfigs() {
      System.out.println("Supported configurations: " + provider.getSupportedConfigs(blueprint.getHadoopStack()));
      Assert.assertNotNull(provider.getSupportedConfigs(blueprint.getHadoopStack()));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testCreateCluster() {
      Assert.assertTrue(provider.createCluster(blueprint, reportQueue));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testExportBlueprint() {
      // TODO
   }

   @Test(groups = { "TestAmbariImpl" }, dependsOnMethods = { "testCreateCluster" })
   public void testStatusQuery() {
      ClusterReport report = provider.queryClusterStatus(blueprint);
      Assert.assertTrue(report.getStatus().equals(ServiceStatus.STARTED), "Should get started cluster status");
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStopNotProvisioinedCluster() {
      Assert.assertTrue(provider.onStopCluster(blueprint, reportQueue));
   }

   private AmbariManagerClientbuilder makeClientBuilder() {
      ApiRootResource apiRootResource = Mockito.mock(ApiRootResource.class);
      RootResourceV1 rootResourceV1 = new FakeRootResourceV1();
      Mockito.when(apiRootResource.getRootV1()).thenReturn(rootResourceV1);

      AmbariManagerClientbuilder clientbuilder = Mockito.mock(AmbariManagerClientbuilder.class);
      Mockito.when(clientbuilder.build()).thenReturn(apiRootResource);
      return clientbuilder;
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStopClusterNotProvisionedByBDE() {
      AmbariImpl ambari = Mockito.mock(AmbariImpl.class);
      Mockito.when(ambari.isProvisioned(blueprint.getName())).thenReturn(true);
      Mockito.when(ambari.onStopCluster(blueprint, reportQueue)).thenCallRealMethod();
      try {
         ambari.onStopCluster(blueprint, reportQueue);
      } catch (SoftwareManagementPluginException e) {
         Assert.assertNotNull(e.getCause());
         String expectedErrMsg = "App_Manager (" + Constants.AMBARI_PLUGIN_NAME + ") fails to stop the cluster " +
               blueprint.getName() + ": Cannot stop a cluster that is not provisioned by Big Data Extension.";
         Assert.assertEquals(e.getCause().getMessage(), expectedErrMsg);
      }
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStopAlreadyStoppedCluster() {
      AmbariImpl spy = Mockito.spy(provider);

      AmbariManagerClientbuilder clientbuilder = makeClientBuilder();
      ApiManager apiManager = new FakeApiManager(clientbuilder);

      Mockito.when(spy.isProvisioned(blueprint.getName())).thenReturn(true);
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());

      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      Assert.assertTrue(spy.onStopCluster(blueprint, reportQueue));
      spy.setApiManager(backup);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStopStartedCluster() {
      AmbariImpl spy = Mockito.spy(provider);
      AmbariManagerClientbuilder clientbuilder = makeClientBuilder();
      ApiManager apiManager = new FakeApiManager(clientbuilder) {
         @Override
         public ApiRequest stopAllServicesInCluster(String clusterName) throws AmbariApiException {
            ApiRequest apiRequest = new ApiRequest();
            apiRequest.setApiRequestInfo(new ApiGetRequestInfo());
            return apiRequest;
         }
      };

      Mockito.when(spy.isProvisioned(blueprint.getName())).thenReturn(true);
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());
      try {
         Mockito.when(spy.doSoftwareOperation(Mockito.anyString(), Mockito.<ApiRequest>any(),
               Mockito.<ClusterReport>any(), Mockito.<ClusterReportQueue>any())).thenReturn(true);
      } catch (Exception e) {
      }
      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      Assert.assertTrue(spy.onStopCluster(blueprint, reportQueue));
      spy.setApiManager(backup);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testForceDeleteClusterWithNoEcho() {
      AmbariImpl spy = Mockito.spy(provider);
      Mockito.when(spy.echo()).thenReturn(false);
      Assert.assertTrue(spy.onDeleteCluster(blueprint, reportQueue));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testDeleteUnprovisionedCluster() {
      Assert.assertTrue(provider.onDeleteCluster(blueprint, reportQueue));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testDeleteClusterNotProvisionedByBDE() {
      AmbariImpl spy = Mockito.spy(provider);
      Mockito.when(spy.echo()).thenReturn(true);
      Mockito.when(spy.isProvisioned(Mockito.anyString())).thenReturn(true);
      Mockito.doReturn(false).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());
      Assert.assertTrue(spy.onDeleteCluster(blueprint, reportQueue));
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testForceDeleteClusterWhenStopFailed() {
      AmbariImpl spy = Mockito.spy(provider);
      Mockito.when(spy.echo()).thenReturn(true);
      Mockito.when(spy.isProvisioned(Mockito.anyString())).thenReturn(true);
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());
      Mockito.doReturn(false).when(spy).onStopCluster(Mockito.<ClusterBlueprint>any(), Mockito.<ClusterReportQueue>any());
      ApiManager apiManager = new FakeApiManager(makeClientBuilder());
      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      Assert.assertTrue(spy.onDeleteCluster(blueprint, reportQueue));
      spy.setApiManager(backup);
   }

   @Test
   public void testForceDeleteClusterWhenStopSucceed() {
      AmbariImpl spy = Mockito.spy(provider);
      Mockito.when(spy.echo()).thenReturn(true);
      Mockito.when(spy.isProvisioned(Mockito.anyString())).thenReturn(true);
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());
      Mockito.doReturn(true).when(spy).onStopCluster(Mockito.<ClusterBlueprint>any(), Mockito.<ClusterReportQueue>any());
      ApiManager apiManager = new FakeApiManager(makeClientBuilder());
      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      Assert.assertTrue(spy.onDeleteCluster(blueprint, reportQueue));
      spy.setApiManager(backup);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStartUnprovisionedCluster() {
      try {
         provider.startCluster(blueprint, reportQueue);
      } catch (AmException e) {
         String expectedErrMsg = "The Cluster (" + blueprint.getName() + ") has not been provisioned.";
         Assert.assertEquals(e.getMessage(), expectedErrMsg);
      }
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStartClusterNotProvisionedByBDE() {
      AmbariImpl ambari = Mockito.mock(AmbariImpl.class);
      Mockito.when(ambari.isProvisioned(blueprint.getName())).thenReturn(true);
      Mockito.when(ambari.startCluster(blueprint, reportQueue, false)).thenCallRealMethod();
      try {
         ambari.startCluster(blueprint, reportQueue);
      } catch (SoftwareManagementPluginException e) {
         String expectedErrMsg = "Can not start a cluster (" + blueprint.getName() + ") that is not provisioned by Big Data Extension.";
         Assert.assertEquals(e.getMessage(), expectedErrMsg);
      }
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStartAlreadyStartedCluster() {
      AmbariImpl spy = Mockito.spy(provider);

      AmbariManagerClientbuilder clientbuilder = makeClientBuilder();
      ApiManager apiManager = new FakeApiManager(clientbuilder);

      Mockito.when(spy.isProvisioned(blueprint.getName())).thenReturn(true);
      provider.isProvisioned(blueprint.getName());
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());

      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      Assert.assertTrue(spy.startCluster(blueprint, reportQueue));
      spy.setApiManager(backup);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStartClusterFailed() {
      AmbariImpl spy = Mockito.spy(provider);

      AmbariManagerClientbuilder clientbuilder = makeClientBuilder();
      ApiManager apiManager = new FakeApiManager(clientbuilder) {
         @Override
         public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException {
            throw AmbariApiException.RESPONSE_EXCEPTION(400, "Faked exception");
         }
      };

      Mockito.when(spy.isProvisioned(blueprint.getName())).thenReturn(true);
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());
      Mockito.doReturn(1).when(spy).getRequestMaxRetryTimes();
      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      try {
         spy.startCluster(blueprint, reportQueue);
      } catch (SoftwareManagementPluginException e) {
         Assert.assertEquals(e.getCause().getMessage(), "Ambari server error: Faked exception.");
      }
      spy.setApiManager(backup);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testStartStoppedCluster() {
      AmbariImpl spy = Mockito.spy(provider);

      AmbariManagerClientbuilder clientbuilder = makeClientBuilder();
      ApiManager apiManager = new FakeApiManager(clientbuilder) {
         @Override
         public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException {
            ApiRequest apiRequest = new ApiRequest();
            apiRequest.setApiRequestInfo(new ApiGetRequestInfo());
            return apiRequest;
         }
      };
      try {
         Mockito.when(spy.doSoftwareOperation(Mockito.anyString(), Mockito.<ApiRequest>any(),
               Mockito.<ClusterReport>any(), Mockito.<ClusterReportQueue>any())).thenReturn(true);
      } catch (Exception e) {
      }
      Mockito.when(spy.isProvisioned(blueprint.getName())).thenReturn(true);
      Mockito.doReturn(true).when(spy).isClusterProvisionedByBDE(Mockito.<AmClusterDef>any());
      Mockito.doReturn(1).when(spy).getRequestMaxRetryTimes();
      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      Assert.assertTrue(spy.startCluster(blueprint, reportQueue));
      spy.setApiManager(backup);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testDoSoftwareOperation() {
      ClusterReport clusterReport = new ClusterReport(blueprint);
      AmbariImpl spy = Mockito.spy(provider);
      ApiManager apiManager = new FakeApiManager(makeClientBuilder());
      ApiManager backup = spy.getApiManager();
      spy.setApiManager(apiManager);
      ApiRequest request = new ApiRequest();
      ApiGetRequestInfo requestInfo = new ApiGetRequestInfo();
      request.setApiRequestInfo(requestInfo);
      try {
         spy.doSoftwareOperation(blueprint.getName(), request, clusterReport, reportQueue);
      } catch (Exception e) {
         e.printStackTrace();
         Assert.assertTrue(e.getMessage().contains("Failed to execute request: "));
      }
      spy.setApiManager(backup);
   }

   private AmbariImpl testValidateServerVersionHelper(String version) {
      AmbariImpl ambari = Mockito.mock(AmbariImpl.class);
      Mockito.when(ambari.getVersion()).thenReturn(version);
      Mockito.when(ambari.validateServerVersion()).thenCallRealMethod();
      Mockito.when(ambari.getType()).thenCallRealMethod();
      return  ambari;
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testOnDeleteNodes() {
      List<String> nodesToDelete = new ArrayList<>();
      nodesToDelete.add(blueprint.getNodeGroups().get(0).getNodes().get(0).getHostname());
      Assert.assertTrue(provider.onDeleteNodes(blueprint, nodesToDelete));
   }

   private final static String[][] invalidServerVersion = new String[][] {
         {"UNKNOWN"}
   };

   @DataProvider(name = "TestAmbariImpl.InvalidServerVersion")
   public String[][] getInvalidServerVersion() {
      return invalidServerVersion;
   }

   @Test( groups = { "TestAmbariImpl" }, dataProvider = "TestAmbariImpl.InvalidServerVersion")
   public void testValidateServerVersionFailed(String invalidVersion) {
      AmbariImpl ambari = testValidateServerVersionHelper(invalidVersion);
      boolean exceptionExist = false;
      try {
         ambari.validateServerVersion();
      } catch (SoftwareManagerCollectorException e) {
         exceptionExist = true;
         String errMsg = "The software manager type " + ambari.getType() + " version " + invalidVersion  + " is not supported yet.";
         Assert.assertEquals(e.getMessage(), errMsg);
      }
      Assert.assertTrue(exceptionExist);
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testValidateServerVersionSucceed() {
      String validVersion = "1.6.0";
      AmbariImpl ambari = testValidateServerVersionHelper(validVersion);
      boolean exceptionExist = false;
      try {
         ambari.validateServerVersion();
      } catch (SoftwareManagerCollectorException e) {
         exceptionExist = true;
         String errMsg = "The software manager type " + ambari.getType() + " version " + validVersion  + " is not supported yet.";
         Assert.assertEquals(e.getMessage(), errMsg);
      }
      Assert.assertTrue(!exceptionExist);
   }

   @Test(groups = { "TestAmbariImpl" }, dependsOnMethods = { "testStatusQuery" })
   public void testClusterScaleOut() {
      ClusterReportQueue queue = new ClusterReportQueue();
      List<String> addedNodeNames = new ArrayList<String>();
      addedNodeNames.add("cluster01-backup-0");
      addedNodeNames.add("cluster01-worker-0");
      Assert.assertTrue(provider.scaleOutCluster(blueprint, addedNodeNames, queue));
   }
}
