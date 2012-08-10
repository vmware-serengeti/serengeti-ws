/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.DatastoreRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.CloudProviderConfigEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.VcDataStoreEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.VcProviderException;

public class TestVcProviderManager {
   private static VcProviderManager mgr = new VcProviderManager();
   private static VcResourcePoolManager rpMgr = new VcResourcePoolManager();

   static {
      mgr.setRpMgr(rpMgr);
      mgr.setDatastoreMgr(new VcDataStoreManager());
   }

   @BeforeClass
   public static void setup() {
      deleteAll();
   }
   @AfterClass
   public static void tearDown() {
      deleteAll();
   }

   private static void deleteAll() {
      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            List<CloudProviderConfigEntity> attrs =
                  CloudProviderConfigEntity.findAllByType("VC");
            for (CloudProviderConfigEntity attr : attrs) {
               attr.delete();
            }

            List<VcResourcePoolEntity> rps =
                  VcResourcePoolEntity.findAllOrderByClusterName();
            for (VcResourcePoolEntity rp : rps) {
               rp.delete();
            }
            
            List<VcDataStoreEntity> datastores = VcDataStoreEntity.findAllSortByName();
            for (VcDataStoreEntity ds : datastores) {
               ds.delete();
            }
            return null;
         }
      });
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "com.vmware.bdd.manager.TestClusterManager.testDeleteClusterNegative" })
   public void testDefaultValue() {
      System.out.println("datacenter: " + mgr.getDataCenter());
      Assert.assertTrue("datacenter should not be empty.",
            mgr.getDataCenter() != null);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testDefaultValue" })
   public void testGetManifest() {
      String manifest = mgr.getManifest();
      System.out.println(manifest);
      Assert.assertTrue("manifest should contains type",
            manifest.indexOf("type") != -1);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testGetManifest" })
   public void testAddResourcePools() {
      rpMgr.addResourcePool("rp1", "vc_cluster1", "rp1");
      rpMgr.addResourcePool("rp2", "vc_cluster1", "rp2");
      rpMgr.addResourcePool("rp3", "vc_cluster1", "line*");
      rpMgr.addResourcePool("rp4", "vc_cluster2", "line*");
      rpMgr.addResourcePool("rp5", "vc_cluster2", "rp2");
      rpMgr.addResourcePool("rp6", "vc_cluster2", "rp4");
      String manifest = mgr.getManifest();
      System.out.println(manifest);
      Assert.assertTrue(
            "manifest should contains resource pools",
            manifest.indexOf("vc_clusters") != -1
                  && manifest.indexOf("vc_rps") != -1
                  && manifest.indexOf("line*") != -1);
      VcResourcePoolEntity rpEntity = VcResourcePoolEntity.findByClusterAndRp("vc_cluster2", "rp4");
      Assert.assertTrue("should find rp entity", rpEntity != null && rpEntity.getName().equals("rp6"));
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testAddResourcePools" })
   public void testAddSharedDatastores() {
      List<String> rps = new ArrayList<String>();
      rps.add("disk1");
      rps.add("disk2");
      rps.add("shared-disk*");
      mgr.getDatastoreMgr().addDataStores("disks", DatastoreType.SHARED, rps);
      String manifest = mgr.getManifest();
      System.out.println(manifest);
      Assert.assertTrue(
            "manifest should contains datastores",
            manifest
                  .indexOf("\"vc_shared_datastore_pattern\":[\"shared-disk*\",\"disk2\",\"disk1\"]") != -1);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testAddSharedDatastores" })
   public void testNoDuplicateResourcePools() {

      try {
         rpMgr.addResourcePool("rp1", "vc_cluster1", "rp1");
         Assert.assertTrue("should get exception.", false);
      } catch (VcProviderException e) {
         Assert.assertTrue(
               "Should catch resource pool already existed exception.", e
                     .getErrorId().equals("VC_RESOURCE_POOL_ALREADY_ADDED"));
      }
      try {
         rpMgr.addResourcePool("rp2", "vc_cluster4", "rp2");
         Assert.assertTrue("should get exception.", false);
      } catch (BddException e) {
         Assert.assertTrue(
               "Should catch resource pool already existed exception.", e
                     .getErrorId().equals("ALREADY_EXISTS"));
      }
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testNoDuplicateResourcePools" })
   public void testNoDuplicateSharedDatastores() {
      List<String> rps = new ArrayList<String>();
      rps.add("disk1");
      rps.add("disk2");
      rps.add("shared-disk*");
      try {
         mgr.getDatastoreMgr()
               .addDataStores("disks", DatastoreType.SHARED, rps);
         Assert.assertTrue("should get exception", false);
      } catch (BddException e) {
         e.printStackTrace();
         Assert.assertTrue("get expected exception.", true);
      }
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testNoDuplicateSharedDatastores" })
   public void testAddLocalDatastores() {
      List<String> rps = new ArrayList<String>();
      rps.add("locald1");
      rps.add("locald2");
      rps.add("local-d*");
      mgr.getDatastoreMgr().addDataStores("locals", DatastoreType.LOCAL, rps);
      String manifest = mgr.getManifest();
      System.out.println(manifest);
      Assert.assertTrue(
            "manifest should contains datastores",
            manifest
                  .indexOf("\"vc_local_datastore_pattern\":[\"local-d*\",\"locald2\",\"locald1\"]") != -1);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testAddLocalDatastores" })
   public void testNoDuplicateLocalDatastores() {
      List<String> rps = new ArrayList<String>();
      rps.add("locald1");
      rps.add("locald2");
      rps.add("local-d*");
      try {
         mgr.getDatastoreMgr()
               .addDataStores("locals", DatastoreType.LOCAL, rps);
         Assert.assertTrue("should get exception", false);
      } catch (BddException e) {
         e.printStackTrace();
         Assert.assertTrue("get expected exception.", true);
      }
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testNoDuplicateLocalDatastores" })
   public void testDeleteResourcePools() {
      rpMgr.deleteResourcePool("rp1");
      VcResourcePoolEntity entity = VcResourcePoolEntity.findByName("rp1");
      Assert.assertTrue("resource pool should have been deleted.", entity == null);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testDeleteResourcePools" })
   public void testDeleteNonExistedResourcePools() {
      try {
         rpMgr.deleteResourcePool("test-rp");
         Assert.assertTrue("should get exception", false);
      } catch (VcProviderException exception) {
         exception.printStackTrace();
         Assert.assertTrue("get expected exception.", true);
      }
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testDeleteNonExistedResourcePools" })
   public void testListDatastores() {
      List<DatastoreRead> datastores = mgr.getDatastoreMgr().getAllDatastoreReads();
      Assert.assertTrue("should get two datastores", datastores.size() == 2);
      Assert.assertTrue("should get three disks", datastores.get(0).getDatastoreReadDetails().size() == 3);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testListDatastores" })
   public void testGetAllLocalDatastores() {
      Set<String> datastores = mgr.getDatastoreMgr().getAllLocalDatastores();
      System.out.println("got local datastores: " + datastores);
      Assert.assertTrue("should get three datastores", datastores.size() == 3);
   }


   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testGetAllLocalDatastores" })
   public void testGetAllSharedDatastores() {
      Set<String> datastores = mgr.getDatastoreMgr().getAllSharedDatastores();
      System.out.println("got shared datastores: " + datastores);
      Assert.assertTrue("should get three datastores", datastores.size() == 3);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testGetAllSharedDatastores" })
   public void testGetDatastore() {
      DatastoreRead datastore = mgr.getDatastoreMgr().getDatastoreRead("locals");
      Assert.assertTrue("should get three disks", datastore.getDatastoreReadDetails().size() == 3);
      Assert.assertTrue("should get local type", datastore.getType() == DatastoreType.LOCAL);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testGetDatastore" })
   public void testDeleteDatastore() {
      mgr.getDatastoreMgr().deleteDatastore("locals");
      DatastoreRead datastore = mgr.getDatastoreMgr().getDatastoreRead("locals");
      Assert.assertTrue("data store should have been deleted", datastore == null);
   }

   @Test(groups = {"testVCProvider"}, dependsOnMethods = { "testDeleteDatastore" })
   public void testDeleteNonExistedDatastore() {
      try {
         mgr.getDatastoreMgr().deleteDatastore("test-store");
         Assert.assertTrue("should get exception", false);
      } catch (VcProviderException exception) {
         exception.printStackTrace();
         Assert.assertTrue("get expected exception.", true);
      }
   }
}
