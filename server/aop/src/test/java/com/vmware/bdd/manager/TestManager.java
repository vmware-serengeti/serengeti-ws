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
package com.vmware.bdd.manager;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.vmware.bdd.aop.annotation.ClusterEntityConcurrentWriteLock;
import com.vmware.bdd.aop.annotation.ClusterEntityExclusiveWriteLock;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Component
public class TestManager {

   private static final Logger logger = Logger.getLogger(TestManager.class);

   private boolean started = false;

   public void setStarted(boolean started) {
      this.started = started;
   }

   public boolean isStarted() {
      return started;
   }

   @ClusterEntityConcurrentWriteLock
   public void competitiveLock(String clusterName, long sleep) throws Exception {
      logger.info("Competitive method for " + clusterName + " is called.");
      started = true;
      Thread.sleep(sleep);
      System.out.println("Thread Sleep " + sleep + "ms");
   }

   @ClusterEntityExclusiveWriteLock
   public void exclusiveLock(String clusterName, long sleep) throws Exception {
      logger.info("Exclusive method for " + clusterName + " is called.");
      started = true;
      Thread.sleep(sleep);
      System.out.println("Thread Sleep " + sleep + "ms");
   }

   public void noLock(String clusterName, long sleep) throws Exception {
      logger.info("No lock for " + clusterName);
      started = true;
      Thread.sleep(sleep);
      System.out.println("Thread Sleep " + sleep + "ms");
   }


   @ClusterEntityConcurrentWriteLock
   public void competitiveLockFailedOperation(String clusterName, long sleep)
         throws Exception {
      logger.info("Competitive method for " + clusterName + " is called.");
      started = true;
      Thread.sleep(sleep);
      System.out.println("Thread Sleep " + sleep + "ms");
      throw new RuntimeException("Faked exception");
   }

   @ClusterEntityExclusiveWriteLock
   public void exclusiveLockFailedOperation(String clusterName, long sleep)
         throws Exception {
      logger.info("Exclusive method for " + clusterName + " is called.");
      started = true;
      Thread.sleep(sleep);
      System.out.println("Thread Sleep " + sleep + "ms");
      throw new RuntimeException("Faked exception");
   }

}
