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
package com.vmware.bdd.aop.collection;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;

import com.vmware.bdd.manager.collection.CollectionDriver;
import com.vmware.bdd.manager.collection.CollectionDriverManager;
import com.vmware.bdd.manager.collection.DataContainer;
import com.vmware.bdd.utils.PropertiesUtil;

public class JobListenerAdvice {

   static final Logger logger = Logger.getLogger(JobListenerAdvice.class);
   private CollectionDriverManager collectionDriverManager;
   private DataContainer dataContainer;

   public void jobAfter(JoinPoint joinPoint) {
      if (collectionDriverManager == null) {
         return;
      }
      CollectionDriver driver = collectionDriverManager.getDriver();
      if (driver != null) {
         String datacollectionEnable =
               new PropertiesUtil(
                     CollectionDriverManager.getConfigurationFile())
                     .getProperty(driver.getCollectionSwitchName());
         if (datacollectionEnable.trim().equalsIgnoreCase("true")) {
            logger.info("jobAfter() of JobListenerAdvice is running!");
            logger.info("hijacked : " + joinPoint.getSignature().getName());
            Object[] args = joinPoint.getArgs();
            if (args != null) {
               JobExecution jobExecution = (JobExecution) args[0];
               if (jobExecution != null) {
                  //
                  String id = "asynchronization_" + jobExecution.getId();
                  dataContainer.push(id, "end_time", System.currentTimeMillis(),
                        true);
                  ExitStatus exitStatus = jobExecution.getExitStatus();
                  if (exitStatus != null) {
                     dataContainer.push(id, "operation_status",
                           exitStatus.getExitCode(), true);
                  }
               }
            }
         }
      }
   }

   public CollectionDriverManager getCollectionDriverManager() {
      return collectionDriverManager;
   }

   public void setCollectionDriverManager(
         CollectionDriverManager collectionDriverManager) {
      this.collectionDriverManager = collectionDriverManager;
   }

   public DataContainer getDataContainer() {
      return dataContainer;
   }

   public void setDataContainer(DataContainer dataContainer) {
      this.dataContainer = dataContainer;
   }
}
