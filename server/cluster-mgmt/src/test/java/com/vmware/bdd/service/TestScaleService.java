/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.service.impl.ScaleService;
import com.vmware.bdd.service.utils.VcResourceUtils;

public class TestScaleService {
   private static final Logger logger = Logger
         .getLogger(TestScaleService.class);
   private static ScaleService scaleService;
   private static NodeEntity nodeEntity;
   private static NodeGroupEntity ngEntity;
   private static ClusterEntity clusterEntity;

   private static final String CLUSTER_NAME = "scale";
   private static final String GROUP_NAME = "worker";
   private static final String NODE_NAME = CLUSTER_NAME + GROUP_NAME + "0";
   private static final String SWAP_DISK_NAME = "swap.vmdk";
   private static final String DATA_DISK_NAME = "data0.vmdk";
   private static final String DS1_NAME = "DS1";
   private static final String DS1_MOID = "datastore-01";
   private static final String DS2_NAME = "DS2";
   private static final String DS2_MOID = "datastore-02";
   private static final String DS3_NAME = "DS3";
   private static final String DS_PATTERN = "DS.*";

   @MockClass(realClass = VcResourceUtils.class)
   public static class MockVcResourceUtilsForScale {
      @Mock
      public static VcDatastore findDSInVcByName(final String dsName) {
         VcDatastore ds = Mockito.mock(VcDatastore.class);
         Mockito.when(ds.isAccessible()).thenReturn(true);
         Mockito.when(ds.getFreeSpace()).thenReturn(4 * 1024 * 1024 * 1024L);
         Mockito.when(ds.getName()).thenReturn(dsName);
         return ds;
      }

      @Mock
      public static VcHost findHost(final String hostName) {
         List<VcDatastore> datastores = new ArrayList<VcDatastore>(2);

         VcDatastore ds1 = Mockito.mock(VcDatastore.class);
         Mockito.when(ds1.isAccessible()).thenReturn(true);
         Mockito.when(ds1.getName()).thenReturn(DS1_NAME);
         Mockito.when(ds1.getFreeSpace()).thenReturn((long) (4 * 1024 * 1024 * 1024L));

         VcDatastore ds2 = Mockito.mock(VcDatastore.class);
         Mockito.when(ds2.isAccessible()).thenReturn(true);
         Mockito.when(ds2.getName()).thenReturn(DS2_NAME);
         Mockito.when(ds2.getFreeSpace()).thenReturn((long) (8 * 1024 * 1024 * 1024L));

         VcDatastore ds3 = Mockito.mock(VcDatastore.class);
         Mockito.when(ds3.isAccessible()).thenReturn(true);
         Mockito.when(ds3.getName()).thenReturn(DS3_NAME);
         Mockito.when(ds3.getFreeSpace()).thenReturn((long) (10 * 1024 * 1024 * 1024L));

         datastores.add(ds1);
         datastores.add(ds2);
         datastores.add(ds3);

         VcHost host = Mockito.mock(VcHost.class);
         Mockito.when(host.getDatastores()).thenReturn(datastores);

         return host;
      }
   }

   @MockClass(realClass = NodeGroupCreate.class)
   public static class MockNodeGroupCreateForScale {
      @Mock
      public static String[] getImagestoreNamePattern(ClusterCreate cluster, NodeGroupCreate ng) {
         String[] patterns = { DS_PATTERN };
         return patterns;         
      }
   }

   @MockClass(realClass = ClusterCreate.class)
   public static class MockClusterCreateForScale {
      @Mock
      public NodeGroupCreate getNodeGroup(String ngName) {
         return new NodeGroupCreate();
      }
   }

   @AfterMethod(groups = { "TestScaleService" })
   public void cleanFlag() {
      Mockit.tearDownMocks();
   }

   @BeforeMethod(groups = { "TestScaleService" })
   public void setMockup() {
      Mockit.setUpMock(MockVcResourceUtilsForScale.class);
      Mockit.setUpMock(MockNodeGroupCreateForScale.class);
      Mockit.setUpMock(MockClusterCreateForScale.class);
   }

   @BeforeClass(groups = { "TestScaleService" })
   public static void setUp() throws Exception {
      scaleService = new ScaleService();

      // mock getDisks
      Set<DiskEntity> disks = new HashSet<>();
      DiskEntity swapDisk =  new DiskEntity(SWAP_DISK_NAME);
      swapDisk.setVmdkPath(DS1_NAME + "/" + NODE_NAME + "/" + SWAP_DISK_NAME);
      swapDisk.setDatastoreName(DS1_NAME);
      swapDisk.setDatastoreMoId(DS1_MOID);
      swapDisk.setSizeInMB(2 * 1024);
      swapDisk.setDiskType("SWAP");

      DiskEntity dataDisk =  new DiskEntity(DATA_DISK_NAME);
      dataDisk.setVmdkPath(DS2_NAME + "/" + NODE_NAME + "/" + DATA_DISK_NAME);
      dataDisk.setDatastoreName(DS2_NAME);
      dataDisk.setDatastoreMoId(DS2_MOID);
      dataDisk.setSizeInMB(20 * 1024);
      dataDisk.setDiskType("DATA");

      disks.add(swapDisk);
      disks.add(dataDisk);

      nodeEntity = Mockito.mock(NodeEntity.class);
      Mockito.when(nodeEntity.getDisks()).thenReturn(disks);

      clusterEntity = Mockito.mock(ClusterEntity.class);
      Mockito.when(clusterEntity.getName()).thenReturn(CLUSTER_NAME);

      ngEntity = Mockito.mock(NodeGroupEntity.class);
      Mockito.when(ngEntity.getCluster()).thenReturn(clusterEntity);
      Mockito.when(ngEntity.getName()).thenReturn(GROUP_NAME);

      Mockito.when(nodeEntity.getNodeGroup()).thenReturn(ngEntity);

      ClusterConfigManager clusterConfigMgr = Mockito.mock(ClusterConfigManager.class);
      Mockito.when(clusterConfigMgr.getClusterConfig(CLUSTER_NAME)).thenReturn(new ClusterCreate());
      scaleService.setClusterConfigMgr(clusterConfigMgr);
   }

   @Test(groups = { "TestScaleService" })
   public void testFindSwapDisk() {
      logger.info("test findSwapDisk");
      DiskEntity swapDisk = scaleService.findSwapDisk(nodeEntity);
      Assert.assertTrue(swapDisk.getName() == SWAP_DISK_NAME, "swap disk should be " + SWAP_DISK_NAME);
   }

   @Test(groups = { "TestScaleService" }, dependsOnMethods = { "testFindSwapDisk" })
   public void testGetTargetDsForSwapDisk() {
      logger.info("test getTargetDsForSwapDisk");
      DiskEntity swapDisk = scaleService.findSwapDisk(nodeEntity);

      nodeEntity.getNodeGroup();
      VcDatastore ds = scaleService.getTargetDsForSwapDisk(nodeEntity, swapDisk, 3 * 1024);
      Assert.assertTrue(ds.getName() == DS1_NAME, "should select the original DS: " + DS1_NAME);

      ds = scaleService.getTargetDsForSwapDisk(nodeEntity, swapDisk, 6 * 1024);
      Assert.assertTrue(ds.getName() == DS3_NAME, "should select DS: " + DS3_NAME);

      ds = scaleService.getTargetDsForSwapDisk(nodeEntity, swapDisk, 20 * 1024);
      Assert.assertEquals(ds, null);
   }
}
