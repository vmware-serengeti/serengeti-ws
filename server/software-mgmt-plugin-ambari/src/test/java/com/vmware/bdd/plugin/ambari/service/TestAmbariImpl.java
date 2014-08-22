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

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeRootResourceV1;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.plugin.ambari.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager.HealthStatus;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.utils.CommonUtil;

public class TestAmbariImpl {

   private static ApiRootResource apiRootResource;
   private static RootResourceV1 rootResourceV1;
   private static AmbariImpl provider;
   private static ClusterBlueprint blueprint;
   private static ClusterReportQueue reportQueue;

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

      URL url = new URL("http://127.0.0.1:8080");
      provider =
            new AmbariImpl(url, "admin", "admin", "RSA_CERT");
      blueprint =
            SerialUtils.getObjectByJsonString(ClusterBlueprint.class,
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

}
