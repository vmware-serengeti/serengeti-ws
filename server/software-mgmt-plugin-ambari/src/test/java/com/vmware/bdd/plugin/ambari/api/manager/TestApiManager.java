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

import junit.framework.Assert;
import mockit.Mockit;
import org.mockito.Mockito;
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

   }

   @Test
   public void testGetStack() throws Exception {

   }

   @Test
   public void testReadService() throws Exception {
   }

   @Test
   public void testIsServiceStarted() throws Exception {

   }

   @Test
   public void testGetStackVersionList() throws Exception {

   }

   @Test
   public void testGetStackVersion() throws Exception {

   }

   @Test
   public void testGetStackServiceList() throws Exception {

   }

   @Test
   public void testGetStackServiceListWithComponents() throws Exception {

   }

   @Test
   public void testGetStackServiceListWithConfigurations() throws Exception {

   }

   @Test
   public void testGetStackServiceWithComponents() throws Exception {

   }

   @Test
   public void testGetStackService() throws Exception {
   }

   @Test
   public void testGetStackComponentList() throws Exception {

   }

   @Test
   public void testGetStackComponent() throws Exception {

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

   }

   @Test
   public void testCreateBlueprint() throws Exception {

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

   }

   @Test
   public void testGetRequest() throws Exception {

   }

   @Test
   public void testCreateBootstrap() throws Exception {

   }

   @Test
   public void testGetBootstrapStatus() throws Exception {

   }

   @Test
   public void testGetRequestWithTasks() throws Exception {

   }

   @Test
   public void testGetClusterStatus() throws Exception {

   }

   @Test
   public void testGetHostStatus() throws Exception {

   }

   @Test
   public void testGetHostsSummaryInfo() throws Exception {

   }

   @Test
   public void testHealthCheck() throws Exception {

   }

   @Test
   public void testGetVersion() throws Exception {

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