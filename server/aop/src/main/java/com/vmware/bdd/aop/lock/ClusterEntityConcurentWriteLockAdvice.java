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
package com.vmware.bdd.aop.lock;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 * 
 */
public class ClusterEntityConcurentWriteLockAdvice {

   private static final Logger logger = Logger
         .getLogger(ClusterEntityConcurentWriteLockAdvice.class);


   private void lockConcurrentWrite(String clusterName) {
      LockFactory.getClusterLock(clusterName).readLock().lock();
      logger.debug("Cluster" + clusterName + " competitive write is locked.");
   }

   private void unlockConcurrentWrite(String clusterName) {
      LockFactory.getClusterLock(clusterName).readLock().unlock();
      logger.debug("Cluster" + clusterName
            + " competitive write is unlocked.");
   }

   public Object lock(ProceedingJoinPoint pjp) throws Throwable {
      Object[] objs = pjp.getArgs();
      String clusterName = (String)objs[0];
      logger.info("Lock competitive write for cluster " + clusterName);
      boolean locked = false;
      try {
         lockConcurrentWrite((String)clusterName);
         locked = true;
         return pjp.proceed();
      } finally {
         if (locked) {
            unlockConcurrentWrite(clusterName);
            logger.info("Unlock competitive write for cluster " + clusterName);
         }
      }
   }
}
