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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.vmware.bdd.util.collection.CollectionConstants;
import com.vmware.bdd.utils.PropertiesUtil;
import org.apache.log4j.Logger;

import com.vmware.bdd.utils.CommonUtil;

public class TimelyDataProducer implements Runnable {

   private static final Logger logger = Logger.getLogger(TimelyDataProducer.class);

   private CollectOperationManager collectOperationManager;
   private DataContainer dataContainer;
   private CollectionDriver collectionDriver;

   public TimelyDataProducer (DataContainer dataContainer, CollectionDriver driver) {
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
      List<Map<String, Object>> restData = null;
      boolean asynchronous;
      restData = collectOperationManager.consumeOperations();
      logger.debug("TimelyDataProducer is producing data: " + restData);
      if (restData != null && !restData.isEmpty()) {
         for (Map<String, Object> data : restData) {
            asynchronous = false;
            if (data.containsKey(CollectionConstants.TASK_ID)
                    && !CommonUtil.isBlank(String.valueOf((Long) data.get(CollectionConstants.TASK_ID)))) {
                  asynchronous = true;
               }
            collect(data, asynchronous);
            }
         }
   }

   public void collect(Map<String, Object> data, boolean asynchronous) {
      if (data != null && !data.isEmpty()) {
         String id = "";
         if (asynchronous) {
            id = String.valueOf((Long) data.get(CollectionConstants.TASK_ID));
            if (!CommonUtil.isBlank(id)) {
               id = CollectionConstants.ASYNCHRONIZATION_PREFIX + id;
            }
         } else {
            id = (String) data.get(CollectionConstants.OBJECT_ID);
            if (!CommonUtil.isBlank(id)) {
               id = CollectionConstants.SYNCHRONIZATION_PREFIX + id;
            }
         }
         data.remove(CollectionConstants.OBJECT_ID);
         data.put(CollectionConstants.OBJECT_ID, id);
         if (!CommonUtil.isBlank(id)) {
            for (Entry<String, ?> field : data.entrySet()) {
               dataContainer.push(id, field.getKey(), field.getValue());
            }
         }
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
