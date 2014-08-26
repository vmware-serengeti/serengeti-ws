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
import java.net.URL;
import java.util.List;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeRootResourceV1;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.plugin.ambari.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
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
            public void preStartServices(String clusterName,
                                         int maxWaitingSeconds) throws SoftwareManagementPluginException {
            }
         };
      }
   }

   private  AmbariImpl provider;
   private  ClusterBlueprint blueprint;
   private  ClusterReportQueue reportQueue;

   @BeforeTest(groups = { "TestClouderaManagerImpl" }, dependsOnGroups = { "TestClusterDef" })
   public void setup() throws IOException {
      ApiRootResource apiRootResource = Mockito.mock(ApiRootResource.class);
      RootResourceV1 rootResourceV1 = new FakeRootResourceV1();
      Mockito.when(apiRootResource.getRootV1()).thenReturn(rootResourceV1);

      AmbariManagerClientbuilder clientbuilder = Mockito.mock(AmbariManagerClientbuilder.class);
      Mockito.when(clientbuilder.withBaseURL(Matchers.any(URL.class))).thenReturn(clientbuilder);
      Mockito.when(clientbuilder.withHost(Matchers.anyString())).thenReturn(clientbuilder);
      Mockito.when(clientbuilder.withPort(Matchers.anyInt())).thenReturn(clientbuilder);
      Mockito.when(clientbuilder.withUsernamePassword(Matchers.anyString(), Matchers.anyString())).thenReturn(clientbuilder);
      Mockito.when(clientbuilder.build()).thenReturn(apiRootResource);

      AmClusterValidator validator = Mockito.mock(AmClusterValidator.class);
      Mockito.when(validator.validateBlueprint(blueprint)).thenReturn(true);

      Mockit.setUpMock(MockReflectionUtils.class);

      provider = new AmbariImpl(clientbuilder, "RSA_CERT");
      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class,
            CommonUtil.readJsonFile("simple_blueprint.json"));

      reportQueue = new ClusterReportQueue();
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

   private AmbariImpl testValidateServerVersionHelper(String version) {
      AmbariImpl ambari = Mockito.mock(AmbariImpl.class);
      Mockito.when(ambari.getVersion()).thenReturn(version);
      Mockito.when(ambari.validateServerVersion()).thenCallRealMethod();
      Mockito.when(ambari.getType()).thenCallRealMethod();
      return  ambari;
   }

   @Test(groups = { "TestAmbariImpl" })
   public void testValidateServerVersionFailed() {
      String invalidVersion = "1.5.0";
      AmbariImpl ambari = testValidateServerVersionHelper(invalidVersion);
      boolean exceptionExist = false;
      try {
         ambari.validateServerVersion();
      } catch (SoftwareManagementPluginException e) {
         exceptionExist = true;
         String errMsg = "The min supported version of software manager type " + ambari.getType()  + " is " + ambari.MIN_SUPPORTED_VERSION + " but got " + ambari.getVersion() + ".";
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
      } catch (SoftwareManagementPluginException e) {
         exceptionExist = true;
         String errMsg = "The min supported version of software manager type " + ambari.getType() + " is " + ambari.MIN_SUPPORTED_VERSION + " but got " + ambari.getVersion() + ".";
         Assert.assertEquals(e.getMessage(), errMsg);
      }
      Assert.assertTrue(!exceptionExist);
   }

}