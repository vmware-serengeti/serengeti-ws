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
package com.vmware.bdd.manager.collection;

import org.apache.log4j.Logger;

import com.vmware.bdd.service.collection.ITimelyCollectionService;
import org.aspectj.lang.JoinPoint;

import java.util.HashMap;
import java.util.Map;

public class CollectOperationManager {
   private static final Logger logger = Logger.getLogger(CollectOperationManager.class);
   ITimelyCollectionService timelyCollectionService;

   //restCallId to joinPoint
   private static Map<String, JoinPoint> restCallRawData = new HashMap<>();

   public ITimelyCollectionService getTimelyCollectionService() {
      return timelyCollectionService;
   }

   public void setTimelyCollectionService(
         ITimelyCollectionService timelyCollectionService) {
      this.timelyCollectionService = timelyCollectionService;
   }

   public static void setRestCallRawData(String restCallId, JoinPoint joinPoint) {
      restCallRawData.put(restCallId, joinPoint);
   }
}
