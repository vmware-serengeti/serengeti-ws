/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.collection.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DataObjectType;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.util.collection.CollectionConstants;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class TestPeriodCollectionService {

   private IResourceInitializerService resourceInitializerService;
   private ClusterManager clusterMgr;
   private IResourceService resourceService;
   private IResourcePoolService resourcePoolService;
   private SoftwareManagerCollector softwareManagerCollector;
   private FakePeriodCollectionService periodCollectionService;
   private ICollectionInitializerService collectionInitializerService;
   private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private final String DEPLOY_TIME = "2014-12-12 07:59:55";

   @BeforeMethod(groups = { "TestPeriodCollectionService" })
   public void setMockup() {
      Mockit.setUpMock(MockCommonUtil.class);
      Mockit.setUpMock(MockVcCache.class);
      Mockit.setUpMock(MockVcContext.class);
      Mockit.setUpMock(MockVcResourceUtils.class);
      periodCollectionService = new FakePeriodCollectionService();
      resourceInitializerService =  Mockito.mock(IResourceInitializerService.class);
      clusterMgr =  Mockito.mock(ClusterManager.class);
      resourceService = Mockito.mock(IResourceService.class);
      resourcePoolService = Mockito.mock(IResourcePoolService.class);
      softwareManagerCollector = Mockito.mock(SoftwareManagerCollector.class);
      collectionInitializerService =  Mockito.mock(ICollectionInitializerService.class);
      periodCollectionService.setCollectionInitializerService(collectionInitializerService);
      periodCollectionService.setClusterMgr(clusterMgr);
      periodCollectionService.setResourceInitializerService(resourceInitializerService);
      periodCollectionService.setResourcePoolService(resourcePoolService);
      periodCollectionService.setResourceService(resourceService);
      periodCollectionService.setSoftwareManagerCollector(softwareManagerCollector);
   }

   @MockClass(realClass = CommonUtil.class)
   public static class MockCommonUtil {
      @Mock
      public static String getUUID() {
         return "resourceId";
      }
   }

   @Test(groups = { "TestPeriodCollectionService" })
   public void testCollectData() throws ParseException {
      testCollectFootPrintData();
      testCollectEnvironmentalInfo();
      testCommonReportsData();
   }

   private void testCollectFootPrintData() throws ParseException {
      List<ResourcePoolRead> resourcePools = new ArrayList<> ();
      ResourcePoolRead rp = Mockito.mock(ResourcePoolRead.class);
      Mockito.when(rp.getRpName()).thenReturn("rp1");
      Mockito.when(rp.getRpVsphereName()).thenReturn("vcrp1");
      Mockito.when(rp.getVcCluster()).thenReturn("cluster1");
      resourcePools.add(rp);
      List<VcDatastore> vcDatastores = new ArrayList<>();
      VcDatastore vcDataStore = Mockito.mock(VcDatastore.class);
      Mockito.when(vcDataStore.getCapacity()).thenReturn(1073741824000L);
      vcDatastores.add(vcDataStore);
      Mockito.when(resourceService.getAvailableDSs()).thenReturn(vcDatastores);
      Mockito.when(collectionInitializerService.getDeployTime()).thenReturn(df.parse(DEPLOY_TIME));
      Mockito.when(resourcePoolService.getAllResourcePoolForRest()).thenReturn(resourcePools);
      Mockito.when(resourceInitializerService.isResourceInitialized()).thenReturn(true);
      List<VcHost> hosts = new ArrayList<>();
      VcHost vcHost1 = Mockito.mock(VcHost.class);
      Mockito.when(vcHost1.getName()).thenReturn("192.168.0.1");
      VcHost vcHost2 = Mockito.mock(VcHost.class);
      Mockito.when(vcHost2.getName()).thenReturn("192.168.0.2");
      hosts.add(vcHost1);
      hosts.add(vcHost2);
      Set<String> rpNames = new HashSet<String>();
      rpNames.add("rp1");
      Mockito.when(resourcePoolService.getAllRPNames()).thenReturn(rpNames);
      Mockito.when(resourceService.getHostsByRpName(Mockito.anyString())).thenReturn(hosts);
      List<ClusterRead> clusters = new ArrayList <>();
      List<NodeGroupRead> nodeGroups = new ArrayList <>();
      List<NodeRead> nodes = new ArrayList<> ();
      ClusterRead clusterRead = Mockito.mock(ClusterRead.class);
      NodeGroupRead nodeGroupRead = Mockito.mock(NodeGroupRead.class);
      Mockito.when(nodeGroupRead.getInstanceNum()).thenReturn(5);
      NodeRead nodeRead = Mockito.mock(NodeRead.class);
      Mockito.when(nodeRead.getMoId()).thenReturn("VirtualMachine:002");
      nodes.add(nodeRead);
      Mockito.when(nodeGroupRead.getInstances()).thenReturn(nodes);
      nodeGroups.add(nodeGroupRead);
      Mockito.when(clusterRead.getNodeGroups()).thenReturn(nodeGroups);
      clusters.add(clusterRead);
      Mockito.when(clusterMgr.getClusters(Mockito.anyBoolean())).thenReturn(clusters);
      Map<String, Map<String, ?>> data = periodCollectionService.collectData(DataObjectType.FOOTPRINT);
      Map<String, ?> fieldsMap = data.get(DataObjectType.FOOTPRINT.getName());
      assertEquals(fieldsMap.get("id"), "resourceId");
      assertEquals(fieldsMap.get("version"), Constants.VERSION);
      assertEquals(fieldsMap.get("deploy_time"), DEPLOY_TIME);
      assertEquals(fieldsMap.get("vc_cpu_quota_size"), 10000);
      assertEquals(fieldsMap.get("vc_mem_quota_size"), 1000L);
      assertEquals(fieldsMap.get("resourcepool_cpu_quota_size"), 10000L);
      assertEquals(fieldsMap.get("resourcepool_mem_quota_size"), 1000L);
      assertEquals(fieldsMap.get("datastore_quota_size"), 1000L);
      assertEquals(fieldsMap.get("is_init_resource"), "true");
      assertEquals(fieldsMap.get("host_num_of_vc"), 9);
      assertEquals(fieldsMap.get("host_num_of_resource_pools"), 2);
      assertEquals(fieldsMap.get("host_num_of_clusters"), 1);
      assertEquals(fieldsMap.get("num_of_hadoop_clusters"), 1);
      assertEquals(fieldsMap.get("num_of_hadoop_nodes"), 5);
   }

   private void testCollectEnvironmentalInfo() {
      SoftwareManager softwareManager = Mockito.mock(SoftwareManager.class);
      List<HadoopStack> hadoopStacks = new ArrayList<> ();
      HadoopStack haddopStack = Mockito.mock(HadoopStack.class);
      Mockito.when(haddopStack.getVendor()).thenReturn(Constants.DEFAULT_VENDOR);
      Mockito.when(haddopStack.getFullVersion()).thenReturn("2.0.0");
      hadoopStacks.add(haddopStack);
      Mockito.when(softwareManager.getSupportedStacks()).thenReturn(hadoopStacks);
      Mockito.when(softwareManagerCollector.getSoftwareManager(Mockito.anyString())).thenReturn(softwareManager);
      List<AppManagerRead> appManagers = new ArrayList<>();
      AppManagerRead defaultAppManager =  Mockito.mock(AppManagerRead.class);
      Mockito.when(defaultAppManager.getType()).thenReturn("Default");
      AppManagerRead clouderaManager =  Mockito.mock(AppManagerRead.class);
      Mockito.when(clouderaManager.getType()).thenReturn("ClouderaManager");
      appManagers.add(defaultAppManager);
      appManagers.add(clouderaManager);
      Mockito.when(softwareManagerCollector.getAllAppManagerReads()).thenReturn(appManagers);
      Map<String, Map<String, ?>> data = periodCollectionService.collectData(DataObjectType.ENVIRONMENTAL_INFORMATION);
      Map<String, ?> fieldsMap = data.get(DataObjectType.ENVIRONMENTAL_INFORMATION.getName());
      assertEquals(fieldsMap.get("id"), "resourceId");
      assertEquals(fieldsMap.get("version_of_vCenter"), "vCenter 5.5.0");
      assertEquals(fieldsMap.get("version_of_ESXi"), "ESXi 5.5.0:1,ESXi 5.1.0:1");
      assertEquals(fieldsMap.get("type_of_storage"), "LOCAL:1,REMOTE:1");
      assertEquals(fieldsMap.get("distros_of_hadoop"), "BIGTOP 2.0.0");
      assertEquals(fieldsMap.get("app_managers"), "Default,ClouderaManager");

      // check the new added vc correlation data
      @SuppressWarnings("unchecked")
      Map<String, Object> vcRelationMap = (Map<String, Object>)fieldsMap.get(CollectionConstants.VC_RELATION);
      assertEquals(vcRelationMap.get("vc_uuid"), "vCenter_uuid");
      assertEquals(vcRelationMap.get("vc_version"), "5.5.0");
   }

   private void testCommonReportsData() {
      Mockito.when(collectionInitializerService.getInstanceId()).thenReturn("instanceId");
      Map<String, Map<String, ?>> data = periodCollectionService.collectData(DataObjectType.COMMONREPORTS);
      Map<String, ?> fieldsMap = data.get(DataObjectType.COMMONREPORTS.getName());
      assertEquals(fieldsMap.get("id"), "instanceId");
      assertEquals(fieldsMap.get("name"), "vSphere Big Data Extensions");
      assertEquals(fieldsMap.get("version"), Constants.VERSION);
      assertEquals(fieldsMap.get("edition"), "Enterprise");
   }
}
