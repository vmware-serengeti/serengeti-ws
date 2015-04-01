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
package com.vmware.bdd.plugin.ambari.api.manager;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiService;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.service.am.FakeRootResourceV1;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;

import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

public class TestApiManager {
   private ApiManager apiManager;
   private ApiClusterBlueprint apiClusterBlueprint = new ApiClusterBlueprint();
   private String clusterName = "cluster01";

   @BeforeMethod
   public void setUp() throws Exception {
      ApiRootResource apiRootResource = Mockito.mock(ApiRootResource.class);
      RootResourceV1 rootResourceV1 = new FakeRootResourceV1();
      Mockito.when(apiRootResource.getRootV1()).thenReturn(rootResourceV1);
      AmbariManagerClientbuilder clientbuilder = Mockito.mock(AmbariManagerClientbuilder.class);
      Mockito.when(clientbuilder.build()).thenReturn(apiRootResource);
      apiManager = new ApiManager(clientbuilder);
   }

   @AfterMethod
   public void tearDown() throws Exception {
      Mockit.tearDownMocks();
   }

   @Test
   public void testGetStackList() throws Exception {
      Assert.assertTrue(apiManager.getStackList().getApiStacks().size() != 0);
   }

   @Test
   public void testGetStack() throws Exception {
      Assert.assertEquals("HDP", apiManager.getStack("HDP").getApiStackName().getStackName());
   }

   @Test
   public void testReadService() throws Exception {
      Assert.assertEquals("HDFS", apiManager.readService("cluster_01", "HDFS").getServiceInfo().getServiceName());
   }

   @Test
   public void testIsServiceStarted() throws Exception {

   }

   @Test
   public void testGetStackVersionList() throws Exception {
      Assert.assertNotNull(apiManager.getStackVersionList("HDP"));
   }

   @Test
   public void testGetStackVersion() throws Exception {
      Assert.assertEquals("2.1", apiManager.getStackVersion("HDP", "2.1").getApiStackVersionInfo().getStackVersion());
   }

   @Test
   public void testGetStackServiceList() throws Exception {
      Assert.assertNotNull(apiManager.getStackServiceList("HDP", "2.1"));
   }

   @Test
   public void testGetStackServiceListWithComponents() throws Exception {
      Assert.assertNotNull(apiManager.getStackServiceListWithComponents("HDP", "2.1"));
   }

   @Test
   public void testGetStackServiceListWithConfigurations() throws Exception {
      Assert.assertNotNull(apiManager.getStackServiceListWithConfigurations("HDP", "2.1"));
   }

   @Test
   public void testGetStackServiceWithComponents() throws Exception {
      Assert.assertNotNull(apiManager.getStackServiceWithComponents("HDP", "2.1", "HDFS"));
   }

   @Test
   public void testGetStackService() throws Exception {
      Assert.assertEquals("YARN", apiManager.getStackService("HDP", "2.1", "YARN").getApiStackServiceInfo().getServiceName());
   }

   @Test
   public void testGetStackComponentList() throws Exception {
      Assert.assertNotNull(apiManager.getStackComponentList("HDP", "2.1", "YARN").getApiStackServices());
   }

   @Test
   public void testGetStackComponent() throws Exception {
      Assert.assertEquals("DATANODE", apiManager.getStackComponent("HDP", "2.1", "YARN", "DATANODE").getApiComponent().getComponentName());
   }

   @Test
   public void testGetClusterList() throws Exception {
      ApiClusterList apiClusterList = apiManager.getClusterList();
      Assert.assertTrue(apiClusterList.getClusters().size() == 0);
   }

   @Test
   public void testGetCluster() throws Exception {
      ApiCluster apiCluster = apiManager.getCluster(clusterName);
      Assert.assertEquals(apiCluster.getClusterInfo().getClusterName(), clusterName);
   }

   @Test
   public void testGetClusterServices() throws Exception {
      List<ApiService> services = apiManager.getClusterServices(clusterName);
      Assert.assertNotNull(services);
      Assert.assertTrue(!services.isEmpty());
   }

   @Test
   public void testStopAllServicesInCluster() throws Exception {
      ApiRequest apiRequest = apiManager.stopAllServicesInCluster(clusterName);
      Assert.assertNotNull(apiRequest);
   }

   @Test
   public void testStartAllServicesInCluster() throws Exception {
      ApiRequest apiRequest = apiManager.startAllServicesInCluster(clusterName);
      Assert.assertNotNull(apiRequest);
   }

   @Test
   public void testGetClusterServicesNames() throws Exception {
      List<String> serviceNames = apiManager.getClusterServicesNames(clusterName);
      Assert.assertTrue(!serviceNames.isEmpty());
   }

   @Test
   public void testProvisionCluster() throws Exception {
      ApiRequest apiRequest = apiManager.provisionCluster(clusterName, apiClusterBlueprint);
      Assert.assertNotNull(apiRequest);
      Assert.assertEquals(apiRequest.getApiRequestInfo().getClusterName(), clusterName);
   }

   @Test
   public void testGetBlueprintList() throws Exception {
      Assert.assertTrue(apiManager.getBlueprintList().getApiBlueprints().size() == 1);
   }

   @Test
   public void testGetBlueprint() throws Exception {
      Assert.assertEquals("cluster01", apiManager.getBlueprint("cluster01").getApiBlueprintInfo().getBlueprintName());
   }

   @Test
   public void testCreateBlueprint() throws Exception {
      Assert.assertEquals("cluster01", apiManager.createBlueprint("cluster01", null).getApiBlueprintInfo().getBlueprintName());
   }

   @Test
   public void testUpdatePersist() throws Exception {

   }

   @Test
   public void testDeleteHost() throws Exception {

   }

   @Test
   public void testDeleteBlueprint() throws Exception {

   }

   @Test
   public void testDeleteCluster() throws Exception {

   }

   @Test
   public void testGetRequestList() throws Exception {
      Assert.assertNotNull(apiManager.getRequestList(clusterName));
   }

   @Test
   public void testGetRequest() throws Exception {
      Assert.assertNotNull(apiManager.getRequest(clusterName, 1L));
   }

   @Test
   public void testCreateBootstrap() throws Exception {
      Assert.assertEquals(clusterName, apiManager.createBlueprint(clusterName, null).getApiBlueprintInfo().getBlueprintName());
   }

   @Test
   public void testGetBootstrapStatus() throws Exception {
      Assert.assertEquals("SUCCESS", apiManager.getBootstrapStatus(1L).getStatus());
   }

   @Test
   public void testGetRequestWithTasks() throws Exception {
      Assert.assertNotNull(apiManager.getRequestWithTasks(clusterName, 1L).getApiTasks());
   }

   @Test
   public void testGetClusterStatus() throws Exception {
      HadoopStack stack = new HadoopStack();
      stack.setVendor("HDP");
      stack.setFullVersion("2.1");
      Assert.assertEquals("Started", apiManager.getClusterStatus(clusterName, stack).toString());
   }

   @Test
   public void testGetHostStatus() throws Exception {

   }

   @Test
   public void testGetHostsSummaryInfo() throws Exception {

   }

   @Test
   public void testHealthCheck() throws Exception {
      Assert.assertEquals("RUNNING", apiManager.healthCheck());
   }

   @Test
   public void testGetVersion() throws Exception {
      Assert.assertEquals("1.6.0", apiManager.getVersion());
   }

   @Test
   public void testDeleteService() throws Exception {

   }

   @Test
   public void testGetExistingHosts() throws Exception {

   }

   @Test
   public void testAddHostsToCluster() throws Exception {

   }

   @Test
   public void testGetClusterHostsList() throws Exception {

   }

   @Test
   public void testStopAllComponentsInHosts() throws Exception {

   }

   @Test
   public void testDeleteAllComponents() throws Exception {

   }

   @Test
   public void testGetAssociatedConfigGroups() throws Exception {

   }

   @Test
   public void testDeleteConfigGroup() throws Exception {

   }

   @Test
   public void testStartComponents() throws Exception {

   }

   @Test
   public void testCreateConfigGroups() throws Exception {

   }

   @Test
   public void testAddComponents() throws Exception {

   }

   @Test
   public void testInstallComponents() throws Exception {

   }

   @Test
   public void testGetStackWithCompAndConfigs() throws Exception {

   }

   @Test
   public void testGetRegisteredHosts() throws Exception {

   }
}