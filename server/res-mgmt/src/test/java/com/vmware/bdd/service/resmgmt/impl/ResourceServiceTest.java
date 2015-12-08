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
package com.vmware.bdd.service.resmgmt.impl;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vmware.bdd.service.utils.MockVcContext;
import com.vmware.bdd.service.utils.MockVcInventory;
import mockit.*;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.dal.IDatastoreDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.VcDatastoreEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.entity.resmgmt.ResourceReservation;
import com.vmware.bdd.exception.VcProviderException;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */

public class ResourceServiceTest extends BaseResourceTest {

   private static final Logger logger = Logger
         .getLogger(ResourceServiceTest.class);

   private ResourceService resSvc;

   @Mocked
   private IResourcePoolDAO rpDao;
   @Mocked
   private IDatastoreDAO dsDao;
   @Mocked
   private INetworkDAO networkDao;

   private boolean vcInitialized;

   @Override
   @BeforeClass
   public void init() {
      resSvc = new ResourceService();
      //Mockit.setUpMock(MockResourceService.class);
      Mockit.setUpMock(MockVcContext.class);
      Mockit.setUpMock(MockVcInventory.class);
   }

   private void initVirtualCenter() {
      if (!vcInitialized) {
         super.init();
         vcInitialized = true;
      }
   }
/*
   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetResourcePoolByName() {
      initVirtualCenter();
      new Expectations() {
         {
            VcResourcePoolEntity vcRPEntity = new VcResourcePoolEntity();
            vcRPEntity.setVcCluster("cluster-ws");
            vcRPEntity.setVcResourcePool("jarred");
            rpDao.findByName(anyString);
            result = vcRPEntity;
         }
      };
      resSvc.setRpDao(rpDao);
      VcResourcePool vcRP = resSvc.getResourcePoolByName("defaultRP");
      Assert.assertNotNull(vcRP);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testIsRPExistInVc() {
      initVirtualCenter();
      boolean exist = resSvc.isRPExistInVc("cluster-ws", "jarred");
      Assert.assertTrue(exist);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetAvailableRPs() {
      initVirtualCenter();
      new Expectations() {
         {
            VcResourcePoolEntity vcRPEntity = new VcResourcePoolEntity();
            vcRPEntity.setVcCluster("cluster-ws");
            vcRPEntity.setVcResourcePool("jarred");
            List<VcResourcePoolEntity> entities =
                  new ArrayList<VcResourcePoolEntity>();
            entities.add(vcRPEntity);
            rpDao.findAllOrderByClusterName();
            result = entities;
         }
      };
      resSvc.setRpDao(rpDao);
      List<VcResourcePool> vcRPs = resSvc.getAvailableRPs();
      Assert.assertEquals(vcRPs.size(), 1);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetDatastoreByName() {
      logger.debug("test getDatastoreByName");
      initVirtualCenter();
      dsDao = new MockUp<IDatastoreDAO>() {
         @Mock
         List<VcDatastoreEntity> findByName(String name) {
            List<VcDatastoreEntity> dss = new ArrayList<VcDatastoreEntity>();
            VcDatastoreEntity dsEntity = new VcDatastoreEntity();
            dsEntity.setName("testSharedStore");
            dsEntity.setType(DatastoreType.SHARED);
            dsEntity.setVcDatastore("datastore1");
            dss.add(dsEntity);
            return dss;
         }
      }.getMockInstance();
      resSvc.setDsDao(dsDao);
      Collection<VcDatastore> dss =
            resSvc.getDatastoreByName("testSharedStore");
      Assert.assertEquals(dss.size(), 1);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetAvailableDS() {
      initVirtualCenter();
      dsDao = new MockUp<IDatastoreDAO>() {
         @Mock
         List<VcDatastoreEntity> findAllSortByName() {
            List<VcDatastoreEntity> dss = new ArrayList<VcDatastoreEntity>();
            VcDatastoreEntity dsEntity = new VcDatastoreEntity();
            dsEntity.setName("testSharedStore");
            dsEntity.setType(DatastoreType.SHARED);
            dsEntity.setVcDatastore("datastore1");
            dss.add(dsEntity);
            return dss;
         }
      }.getMockInstance();
      resSvc.setDsDao(dsDao);
      Collection<VcDatastore> dss = resSvc.getAvailableDSs();
      Assert.assertEquals(dss.size(), 1);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testIsDatastoreAccessibleByCluster() {
      initVirtualCenter();
      dsDao = new MockUp<IDatastoreDAO>() {
         @Mock
         List<VcDatastoreEntity> findByName(String name) {
            List<VcDatastoreEntity> dss = new ArrayList<VcDatastoreEntity>();
            VcDatastoreEntity dsEntity = new VcDatastoreEntity();
            dsEntity.setName("testSharedStore");
            dsEntity.setType(DatastoreType.SHARED);
            dsEntity.setVcDatastore("datastore1");
            dss.add(dsEntity);
            return dss;
         }
      }.getMockInstance();
      resSvc.setDsDao(dsDao);
      boolean result =
            resSvc.isDatastoreAccessibleByCluster("testSharedStore",
                  "cluster-ws");
      Assert.assertEquals(result, true);
   }


   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetNetworkByName() {
      initVirtualCenter();
      new Expectations() {
         {
            NetworkEntity network = new NetworkEntity();
            network.setPortGroup("wdc-vhadp-pub1-1g");
            networkDao.findNetworkByName(anyString);
            result = network;
         }
      };
      resSvc.setNetworkDao(networkDao);
      VcNetwork vcNetwork = resSvc.getNetworkByName("testNetwork");
      Assert.assertNotNull(vcNetwork);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testIsNetworkExistInVc() {
      initVirtualCenter();
      boolean result = resSvc.isNetworkExistInVc("wdc-vhadp-pub1-1g");
      Assert.assertTrue(result);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetAvailableNetworks() {
      initVirtualCenter();
      new Expectations() {
         {
            NetworkEntity network = new NetworkEntity();
            network.setPortGroup("wdc-vhadp-pub1-1g");
            List<NetworkEntity> networks = new ArrayList<NetworkEntity>();
            networks.add(network);
            networkDao.findAllNetworks();
            result = networks;
         }
      };
      resSvc.setNetworkDao(networkDao);
      List<VcNetwork> vcNetworks = resSvc.getAvailableNetworks();
      Assert.assertNotNull(vcNetworks);
      Assert.assertEquals(vcNetworks.size(), 1);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testIsNetworkSharedInCluster() {
      initVirtualCenter();
      new Expectations() {
         {
            NetworkEntity network = new NetworkEntity();
            network.setPortGroup("wdc-vhadp-pub1-1g");
            networkDao.findNetworkByName(anyString);
            result = network;
         }
      };
      resSvc.setNetworkDao(networkDao);
      boolean result =
            resSvc.isNetworkSharedInCluster("testNetwork", "cluster-ws");
      Assert.assertTrue(result);
   }

   @Test(groups = { "res-mgmt", "dependsOnVC" })
   public void testGetHostsByRpName() {
      initVirtualCenter();
      new Expectations() {
         {
            VcResourcePoolEntity vcRPEntity = new VcResourcePoolEntity();
            vcRPEntity.setVcCluster("cluster-ws");
            vcRPEntity.setVcResourcePool("jarred");
            rpDao.findByName(anyString);
            result = vcRPEntity;
         }
      };
      resSvc.setRpDao(rpDao);
      List<VcHost> hosts = resSvc.getHostsByRpName("defaultRP");
      Assert.assertNotNull(hosts);
      Assert.assertEquals(hosts.size(), 1);
   }

   @Test(groups = { "res-mgmt" })
   public void testReserveResource() {
      ResourceReservation resReservation = new ResourceReservation();
      resReservation.setClusterName("testCluster");
      UUID id = resSvc.reserveResoruce(resReservation);
      Assert.assertNotNull(id);
      resSvc.commitReservation(id);
   }

   @Test(groups = { "res-mgmt" },expectedExceptions = VcProviderException.class)
   public void testReserveResourceWithException() {
      ResourceReservation resReservation = new ResourceReservation();
      resReservation.setClusterName("testCluster");
      UUID id = resSvc.reserveResoruce(resReservation);
      Assert.assertNotNull(id);
      resReservation.setClusterName("testCluster2");
      id = resSvc.reserveResoruce(resReservation);

   }
*/
   @Test(groups = { "res-mgmt" })
   public void testReserveResourceAndCommitResource() {
      ResourceReservation resReservation = new ResourceReservation();
      resReservation.setClusterName("testCluster");
      UUID id = resSvc.reserveResource(resReservation);
      Assert.assertNotNull(id);
      resSvc.commitReservation(id);
   }

   @Test(groups = { "res-mgmt" })
   public void testReserveResourceAndCommitResource2() {
      ResourceReservation resReservation = new ResourceReservation();
      resReservation.setClusterName("testCluster");
      UUID id = resSvc.reserveResource(resReservation);
      Assert.assertNotNull(id);
      resSvc.commitReservation(id);
      resReservation.setClusterName("testCluster2");
      id = resSvc.reserveResource(resReservation);
      Assert.assertNotNull(id);
      resSvc.commitReservation(id);
   }

   @Test(groups = { "res-mgmt" })
   public void testReserveResourceAndCancelReservation() {
      ResourceReservation resReservation = new ResourceReservation();
      resReservation.setClusterName("testCluster");
      UUID id = resSvc.reserveResource(resReservation);
      Assert.assertNotNull(id);
      resSvc.cancleReservation(id);
   }

   @Test(groups = { "res-mgmt" })
   public void testConcurrentReserve() {
      ExecutorService es = Executors.newFixedThreadPool(20);
      ReservationTestThread[] threads = new ReservationTestThread[10];
      for(int i = 0; i< threads.length; i++) {
         threads[i] = new ReservationTestThread();
         es.submit(threads[i]);
      }
      es.shutdown();
      try {
         es.awaitTermination(30, TimeUnit.SECONDS);
      } catch(InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Test(groups = { "res-mgmt" }, dependsOnMethods = { "testConcurrentReserve" })
   public void testConcurrentReserveOrder() {
      final AtomicInteger counter = new AtomicInteger(0);
      ExecutorService es = Executors.newFixedThreadPool(10,
            new ThreadFactory() {
               @Override
               public Thread newThread(Runnable r) {
               Thread t = new Thread(r);
               t.setName("T" + counter.incrementAndGet());
               return t;
            }
      });
      ReservationTestThread[] threads = new ReservationTestThread[10];
      for(int i = 0; i< threads.length; i++) {
         threads[i] = new ReservationTestThread();
         try {
            Thread.sleep(50);
         } catch(Exception e) {
            e.printStackTrace();
         }
         es.submit(threads[i]);
      }
      es.shutdown();
      try {
         es.awaitTermination(30, TimeUnit.SECONDS);
      } catch(InterruptedException e) {
         e.printStackTrace();
      }
      for(int i = 1; i < threads.length; i++) {
         Assert.assertTrue(threads[i-1].reservedTime < threads[i].reservedTime);
      }
   }

   class ReservationTestThread extends Thread{
      public long reservedTime = 0;
      public long threadStartTime = 0;

      @Override
      public void run() {
         String name = Thread.currentThread().getName();
         ResourceReservation resReservation = new ResourceReservation();
         resReservation.setClusterName("testCluster");
         threadStartTime = new java.util.Date().getTime();
         System.out.println(name + ":  started");
         UUID id = resSvc.reserveResource(resReservation);
         reservedTime = new java.util.Date().getTime();
         System.out.println(name + ":  reserved");
         try {
            Thread.sleep(2000);
         } catch(InterruptedException e) {
            e.printStackTrace(System.out);
         }
         if(new Random().nextBoolean()) {
            resSvc.commitReservation(id);
            System.out.println(name + ":  committed");
         } else {
            resSvc.cancleReservation(id);
            System.out.println(name + ":  canceled");
         }
      }
   }
}
