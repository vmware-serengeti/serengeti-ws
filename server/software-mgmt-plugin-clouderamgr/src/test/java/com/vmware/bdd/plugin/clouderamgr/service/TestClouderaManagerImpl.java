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

import com.cloudera.api.ApiRootResource;
import com.cloudera.api.ClouderaManagerClientBuilder;
import com.cloudera.api.v6.RootResourceV6;
import com.google.gson.Gson;
import com.vmware.bdd.plugin.clouderamgr.poller.host.HostInstallPoller;
import com.vmware.bdd.plugin.clouderamgr.service.cm.FakeRootResource;
import com.vmware.bdd.plugin.clouderamgr.utils.SerialUtils;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.utils.CommonUtil;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 7/5/14
 * Time: 9:51 PM
 */
public class TestClouderaManagerImpl {
   private static final Logger logger = Logger.getLogger(TestClouderaManagerImpl.class);

   private static ApiRootResource apiRootResource;
   private static RootResourceV6 rootResourceV6;
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

   @BeforeClass( groups = { "TestClouderaManagerImpl" }, dependsOnGroups = { "TestClusterDef" })
   public static void setup() throws IOException {
      Mockit.setUpMock(MockClouderaManagerClientBuilder.class);
      Mockit.setUpMock(MockHostInstallPoller.class);
      apiRootResource = Mockito.mock(ApiRootResource.class);
      rootResourceV6 = new FakeRootResource();
      Mockito.when(apiRootResource.getRootV6()).thenReturn(rootResourceV6);

      provider = new ClouderaManagerImpl("127.0.0.1", 7180, "admin", "admin", "RSA_CERT");
      blueprint = SerialUtils.getObjectByJsonString(ClusterBlueprint.class, CommonUtil.readJsonFile("simple_blueprint.json"));

      reportQueue = new ClusterReportQueue();
   }

   @BeforeClass( groups = { "TestClouderaManagerImpl" }, dependsOnGroups = { "TestClusterDef" })
   public static void tearDown() throws IOException, InterruptedException {

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
      Assert.assertTrue(stacks.get(1).getDistro().equals("CDH-4.7.0"));
   }

   @Test( groups = { "TestClouderaManagerImpl" })
   public void testGetSupportedRoles() {
      HadoopStack stack = new HadoopStack();
      stack.setDistro("CDH-5.0.1");
      Set<String> roles = provider.getSupportedRoles(stack);
      System.out.println(roles.toString());
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
      List<ClusterReport> reports = reportQueue.pollClusterReport();
      for (ClusterReport report : reports) {
         System.out.println("Action: " + report.getAction() + ", Progress: " + report.getProgress());
      }
   }
}
