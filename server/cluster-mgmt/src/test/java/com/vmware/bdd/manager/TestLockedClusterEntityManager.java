/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import mockit.Mock;
import mockit.MockUp;
import mockit.Mockit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.aop.lock.LockFactory;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.impl.ClusterSyncService;
import com.vmware.bdd.service.sp.MockClusterEntityManager;

@ContextConfiguration(locations = { "classpath:/spring/*-context.xml", "classpath:/spring/aop.xml" })
public class TestLockedClusterEntityManager extends AbstractTestNGSpringContextTests {

   private MockClusterEntityManager mockedMgr;

   private static class LockTestThread extends Thread {
      private ILockedClusterEntityManager clusterEntityMgr;
      private volatile boolean started = false;

      public LockTestThread(ILockedClusterEntityManager clusterEntityMgr) {
         this.clusterEntityMgr = clusterEntityMgr;
      }

      public boolean isStarted() {
         return started;
      }

      @Override
      public void run() {
         started = true;
         clusterEntityMgr.syncUp(LOCKED_CLUSTER_NAME, false);
      }
   }

   private static final String LOCKED_CLUSTER_NAME = "LockedClusterEntity";
   private static final String UNLOCKED_CLUSTER_NAME = "UnLockedClusterEntity";
   @Autowired
   private IConcurrentLockedClusterEntityManager competitiveLockedMgr;
   @Autowired
   private IExclusiveLockedClusterEntityManager exclusiveLockedMgr;

   @BeforeClass
   public void setup() {
      new MockUp<ClusterSyncService>() {
         @Mock
         public void syncUp(String clusterName, boolean updateClusterStatus)  {
            try {
               Thread.sleep(200);
               System.out.println("ClusterSyncService MOCK: Sleep 200ms.");
            } catch(Exception e) {
               e.printStackTrace(System.out);
            }
         }
      };

      mockedMgr = new MockClusterEntityManager();
      competitiveLockedMgr.setClusterEntityMgr(mockedMgr);
      exclusiveLockedMgr.setClusterEntityMgr(mockedMgr);
      Mockit.setUpMock(MockClusterEntityManager.class);
   }

   @AfterClass
   public void deleteAll() {
   }

   @Test
   public void testConcurrentWrite() {
      competitiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      exclusiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
   }

   @Test
   public void testSequentialInOneThread() throws Exception {
      competitiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      exclusiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      competitiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      exclusiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
   }

   @Test
   public void testConcurrencyInTwoThread() throws Exception {
      LockTestThread t = new LockTestThread(competitiveLockedMgr);
      t.start();
      while (!t.isStarted()) {
         Thread.sleep(10);
      }
      long start = System.currentTimeMillis();
      competitiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      long end = System.currentTimeMillis();
      System.out.println("Lock takes " + (end - start) + "ms for testConcurrencyInTwoThread");
      Assert.assertTrue((end - start) < 150);
      t.join();
   }

   @Test
   public void testExclusiveInTwoThread() throws Exception {
      LockTestThread t = new LockTestThread(exclusiveLockedMgr);
      t.start();
      while (!t.isStarted()) {
         Thread.sleep(10);
      }
      long start = System.currentTimeMillis();
      exclusiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      long end = System.currentTimeMillis();
      System.out.println("Lock takes " + (end - start) + "ms for testExclusiveInTwoThread");
      Assert.assertTrue((end - start) > 150);
      t.join();
   }

   @Test
   public void testExclusiveCompetitiveInTwoThread() throws Exception {
      LockTestThread t = new LockTestThread(exclusiveLockedMgr);
      t.start();
      while (!t.isStarted()) {
         Thread.sleep(10);
      }
      Thread.sleep(40);
      long start = System.currentTimeMillis();
      exclusiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      long end = System.currentTimeMillis();
      System.out.println("Lock takes " + (end - start) + "ms for testExclusiveCompetitiveInTwoThread");
      // the lock time is impacted by previous test case execution.
      Assert.assertTrue((end - start) >= 100, "Expected bigger than 100, but got " + (end - start));
      t.join();
   }

   @Test
   public void testReverseInTwoThread() throws Exception {
      LockTestThread t = new LockTestThread(competitiveLockedMgr);
      t.start();
      while (!t.isStarted()) {
         Thread.sleep(10);
      }
      long start = System.currentTimeMillis();
      exclusiveLockedMgr.removeVmReference(LOCKED_CLUSTER_NAME, "");
      long end = System.currentTimeMillis();
      System.out.println("Lock takes " + (end - start) + "ms testReverseInTwoThread");
      Assert.assertTrue((end - start) > 150);
      t.join();
   }

   @Test
   public void testCompetitiveInTwoThreadForTwoClusters() throws Exception {
      LockTestThread t = new LockTestThread(exclusiveLockedMgr);
      t.start();
      while (!t.isStarted()) {
         Thread.sleep(10);
      }
      long start = System.currentTimeMillis();
      exclusiveLockedMgr.removeVmReference(UNLOCKED_CLUSTER_NAME, "");
      long end = System.currentTimeMillis();
      System.out.println("Lock takes " + (end - start) + "ms for testCompetitiveInTwoThreadForTwoClusters");
      Assert.assertTrue((end - start) < 150);
      t.join();
   }

   @Test
   public void testReleaseDelayed() throws Exception {
      long start = System.currentTimeMillis();
      LockFactory.getClusterLock(LOCKED_CLUSTER_NAME).writeLock().lock();
      System.out.println("Lock exlusively for " + LOCKED_CLUSTER_NAME
            + " separately.");
      LockTestThread t = new LockTestThread(exclusiveLockedMgr);
      t.start();
      Thread.sleep(50);
      LockFactory.getClusterLock(LOCKED_CLUSTER_NAME).writeLock().unlock();
      t.join();
      long end = System.currentTimeMillis();
      System.out.println("Lock takes " + (end - start) + "ms for testReleaseDelayed");
      Assert.assertTrue((end - start) > 230);
   }
}
