/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import junit.framework.Assert;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.impl.ClusterHealService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.spectypes.DiskSpec;

public class TestClusterHealService {
   private static final Logger logger = Logger
         .getLogger(TestClusterHealService.class);
   private static ClusterHealService service;

   private static final String LOCAL_DS_NAME_PREFIX = "local-datastore-";
   private static final String LOCAL_DS_MOID_PREFIX = "datastore:100";
   private static final String DATA_DISK_NAME_PREFIX = "data-";
   private static final String NODE_1_NAME = "bj-worker-1";
   private static final String CLUSTER_NAME = "bj";
   private static final String NODE_GROUP_NAME = "worker";
   private static final String HOST_NAME = "bj-dc-01";
   private static final String LOCAL_STORE_PATTERN = "local-datastore-.*";

   @MockClass(realClass = VcResourceUtils.class)
   public static class MockVcResourceUtilsForHeal {
      @Mock
      public static VcHost findHost(final String hostName) {
         List<VcDatastore> datastores = new ArrayList<VcDatastore>(4);
         for (int i = 0; i < 4; i++) {
            VcDatastore ds = Mockito.mock(VcDatastore.class);
            if (i == 0) {
               Mockito.when(ds.isAccessible()).thenReturn(false);
            } else {
               Mockito.when(ds.isAccessible()).thenReturn(true);
            }
            // 100 GB available for each datastore
            Mockito.when(ds.getFreeSpace()).thenReturn(
                  100 * 1024 * 1024 * 1024L);
            Mockito.when(ds.getName()).thenReturn(LOCAL_DS_NAME_PREFIX + i);
            datastores.add(ds);
         }
         VcHost host = Mockito.mock(VcHost.class);
         Mockito.when(host.getDatastores()).thenReturn(datastores);

         Mockito.when(host.getName()).thenReturn(HOST_NAME);

         return host;
      }
   }

   @AfterMethod(groups = { "TestClusterHealService" })
   public void cleanFlag() {
      Mockit.tearDownMocks();
   }

   @BeforeMethod(groups = { "TestClusterHealService" })
   public void setMockup() {
      // mock vcvmutil, return isAccessible as false for datastore mo id ends as "0"
      Mockit.setUpMock(MockVcVmUtil.class);

      Mockit.setUpMock(MockVcResourceUtilsForHeal.class);
   }

   @BeforeClass(groups = { "TestClusterHealService" })
   public static void setUp() throws Exception {
      service = new ClusterHealService();

      // mock cluster entity manager
      IClusterEntityManager entityMgr = Mockito.mock(IClusterEntityManager.class);

      // mock getDisks
      List<DiskEntity> disks = new ArrayList<DiskEntity>();
      for (int i = 0; i < 3; i++) {
         DiskEntity disk = new DiskEntity(DATA_DISK_NAME_PREFIX + i);
         disk.setVmdkPath(LOCAL_DS_MOID_PREFIX + i + "/" + disk.getName());
         disk.setDatastoreName(LOCAL_DS_NAME_PREFIX + i);
         disk.setDatastoreMoId(LOCAL_DS_MOID_PREFIX + i);
         disk.setSizeInMB(20 * 1024);
         disk.setDiskType(DiskType.SYSTEM_DISK.type);
         disks.add(disk);
      }
      Mockito.when(entityMgr.getDisks("bj-worker-1")).thenReturn(disks);

      // mock findByName(String, String, String)
      NodeEntity node = new NodeEntity();
      node.setVmName(NODE_1_NAME);
      node.setHostName(HOST_NAME);
      Mockito.when(
            entityMgr.findByName(CLUSTER_NAME, NODE_GROUP_NAME, NODE_1_NAME))
            .thenReturn(node);
      service.setClusterEntityMgr(entityMgr);

      // mock cluster config manager
      ClusterConfigManager configMgr = Mockito.mock(ClusterConfigManager.class);

      // mock getClusterConfig
      NodeGroupCreate nodeGroup = new NodeGroupCreate();
      nodeGroup.setName(NODE_GROUP_NAME);
      nodeGroup.setStorage(new StorageRead());
      NodeGroupCreate[] nodeGroups = new NodeGroupCreate[] { nodeGroup };

      ClusterCreate spec = new ClusterCreate();
      spec.setName(CLUSTER_NAME);
      spec.setNodeGroups(nodeGroups);
      Set<String> patterns = new HashSet<String>();
      patterns.add(LOCAL_STORE_PATTERN);
      spec.setLocalDatastorePattern(patterns);

      Mockito.when(configMgr.getClusterConfig(CLUSTER_NAME)).thenReturn(spec);
      service.setConfigMgr(configMgr);
   }

   @Test(groups = { "TestClusterHealService" })
   public void testGetBadDisks() {
      logger.info("test getBadDisks");
      List<DiskSpec> badDisks = service.getBadDisks(NODE_1_NAME);
      Assert.assertTrue("disk 0 on local-datastore-0 should be bad",
            badDisks.size() == 1);
   }

   @Test(groups = { "TestClusterHealService" }, dependsOnMethods = { "testGetBadDisks" })
   public void testGetReplacementDisks() {
      List<DiskSpec> badDisks = service.getBadDisks(NODE_1_NAME);
      List<DiskSpec> replacements =
            service.getReplacementDisks(CLUSTER_NAME, NODE_GROUP_NAME,
                  NODE_1_NAME, badDisks);

      Assert.assertTrue(!replacements.isEmpty());
      String newDs = LOCAL_DS_NAME_PREFIX + 3;
      Assert.assertTrue("the replacement disk should be placed to " + newDs,
            newDs.equals(replacements.get(0).getTargetDs()));
   }
}
