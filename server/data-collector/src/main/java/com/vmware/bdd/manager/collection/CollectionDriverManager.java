/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.manager.collection;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.PropertiesUtil;

public class CollectionDriverManager {

   static final Logger logger = Logger.getLogger(CollectionDriverManager.class);
   private CollectionDriver driver;
   private static File file;

   public CollectionDriverManager() {}
   public CollectionDriverManager(String driverClass, ICollectionInitializerService collectionInitializerService) {
      init(driverClass, collectionInitializerService);
   }

   private void init(String driverClass, ICollectionInitializerService collectionInitializerService) {
      try {
         Class<?> newClass = Class.forName(driverClass);
         if (newClass != null) {
            CollectionDriver collectionDriver =
                  (CollectionDriver) newClass.newInstance();
            registerDriver(collectionDriver);
            setInstanceId(collectionInitializerService.getInstanceId());
         }
      } catch (ClassNotFoundException e) {
         logger.error("Not found class " + driverClass + ": "
               + e.getLocalizedMessage());
      } catch (InstantiationException | IllegalAccessException e) {
         logger.error("Failed to instance class " + driverClass + ": "
               + e.getLocalizedMessage());
      }
   }

   private void registerDriver(CollectionDriver collectionDriver) {
      this.driver = collectionDriver;
      file =
            CommonUtil.getConfigurationFile(driver.getConfigurationFilePath(),
                  "Configuration");
   }

   private void setInstanceId(String instanceId) {
      driver.setInstanceId(instanceId);
   }

   public CollectionDriver getDriver() {
      return driver;
   }

   public synchronized void changeCollectionSwitchStatus(boolean enabled) {
      if (driver != null) {
         logger.debug("In method changeCollectionSwitchStatus of class CollectionDriverManager, enabled is: "
               + enabled);
         try {
            new PropertiesUtil(getConfigurationFile()).setProperty(
                  driver.getCollectionSwitchName(), String.valueOf(enabled))
                  .saveLastKey();
         } catch (IOException e) {
            logger.warn("Fail to change configuration file of data collection: "
                  + e.getLocalizedMessage());
         }
      } else {
         logger.error("In the method changeCollectionSwitchStatus of class CollectionDriverManager, driver is null.");
      }
   }

   public boolean getCollectionSwitchStatus() {
      boolean collectionSwitchStatus = false;
      if (driver != null) {
         File file = getConfigurationFile();
         if (file != null) {
            PropertiesUtil propertiesUtil = new PropertiesUtil(file);
            if (propertiesUtil != null) {
               String status =
                     propertiesUtil.getProperty(driver
                           .getCollectionSwitchName());
               if (status.equalsIgnoreCase("true")) {
                  collectionSwitchStatus = true;
               }
            }
         }

      } else {
         logger.error("In the method changeCollectionSwitchStatus of class CollectionDriverManager, driver is null.");
      }
      return collectionSwitchStatus;
   }

   public static File getConfigurationFile() {
      return file;
   }

}
