/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.vmware.bdd.util.collection.CollectionConstants;
import org.apache.log4j.Logger;

public class DataContainer {

   static final Logger logger = Logger.getLogger(DataContainer.class);
   private int maxLength = 6;
   private static Map<String, Map<String, Object>> dataQueue =
         new ConcurrentHashMap<String, Map<String, Object>>();

   public void push(String id, String key, Object t) {
      push(id, key, t, false);
   }

   public synchronized void push(String id, String key, Object t, boolean force) {

      try {
         while (this.isFinished(id)) {
            logger.debug("Operation data is collected completely.");
            this.wait();
         }
      } catch (InterruptedException e) {
         logger.warn("Wait consume data failed: " + e.getMessage());
      }
      Map<String, Object> object = null;
      if (!dataQueue.isEmpty() && dataQueue.containsKey(id)) {
         object = dataQueue.get(id);
      } else {
         object = new HashMap<String, Object>();
      }
      if (!object.containsKey(key)) {
         object.put(key, t);
      } else if (force) {
         object.remove(key);
         object.put(key, t);
      }
      dataQueue.put(id, object);
      logger.info("key: " + key + ", value: " + t
            + " have been put in operation " + id + " .");
      this.notify();
   }

   public synchronized Map<String, Map<String, Object>> pop() {
      Map<String, Map<String, Object>> operations =
            new ConcurrentHashMap<String, Map<String, Object>>();
      try {
         while (!hasFinished()) {
            logger.debug("Operation data is consumed completely.");
            this.wait();
         }
      } catch (InterruptedException e) {
         logger.warn("Wait push data failed: " + e.getMessage());
      }
      String id = "";
      Map<String, Object> object = null;
      Iterator<Entry<String, Map<String, Object>>> operationIterator =
            dataQueue.entrySet().iterator();
      Entry<String, Map<String, Object>> operation = null;
      while (operationIterator.hasNext()) {
         operation = operationIterator.next();
         if (operation != null) {
            id = operation.getKey();
            if (isFinished(id)) {
               object = operation.getValue();
               operations.put(id, object);
               operationIterator.remove();
            }
         }
      }
      this.notify();
      return operations;
   }

   private boolean hasFinished() {
      boolean finished = false;
      for (Entry<String, Map<String, Object>> operation : dataQueue.entrySet()) {
         if (isFinished(operation.getKey())) {
            finished = true;
            break;
         }
      }
      return finished;
   }

   private int getMaxLength() {
      return maxLength;
   }

   public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
   }

   private boolean isFinished(String id) {
      if (!dataQueue.containsKey(id)) {
         return false;
      }
      if (id.startsWith(CollectionConstants.ASYNCHRONIZATION_PREFIX)) {
         return (getMaxLength() + 1) == dataQueue.get(id).size();
      } else {
         return getMaxLength() == dataQueue.get(id).size();
      }
   }
}
