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
package com.vmware.bdd.manager.collection;

import java.util.Map;
import java.util.Map.Entry;

import com.vmware.bdd.utils.PropertiesUtil;
import org.apache.log4j.Logger;

public class TimelyDataConsumer implements Runnable {

   private static final Logger logger = Logger.getLogger(TimelyDataConsumer.class);

   private DataContainer dataContainer;
   private CollectionDriver collectionDriver;
   private CollectOperationManager collectOperationManager;

   public TimelyDataConsumer (DataContainer dataContainer, CollectionDriver driver) {
      this.dataContainer = dataContainer;
      this.collectionDriver = driver;
   }

   @Override
   public void run() {
      String datacollectionEnable =
              new PropertiesUtil(
                      CollectionDriverManager.getConfigurationFile())
                      .getProperty(collectionDriver.getCollectionSwitchName());
      if (!datacollectionEnable.trim().equalsIgnoreCase("true")) {
         return;
      }
      Map<String, Map<String, Object>> data = null;
         data = dataContainer.pop();
         logger.debug("TimelyDataConsumer is consuming data: " + data);
         if (data != null) {
            consume(data);
         }
   }

   private void consume(Map<String, Map<String, Object>> data) {
      Map<String, Object> object = null;
      for (Entry<String, Map<String, Object>> entry : data.entrySet()) {
         object = entry.getValue();
         collectOperationManager.sendData(object);
      }
   }

   public CollectOperationManager getCollectOperationManager() {
      return collectOperationManager;
   }

   public void setCollectOperationManager(
         CollectOperationManager collectOperationManager) {
      this.collectOperationManager = collectOperationManager;
   }

}
