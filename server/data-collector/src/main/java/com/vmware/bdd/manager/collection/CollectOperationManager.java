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

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.util.collection.CollectionConstants;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.PropertiesUtil;
import org.apache.log4j.Logger;

import com.vmware.bdd.service.collection.ITimelyCollectionService;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;

import java.util.*;

import com.vmware.bdd.apitypes.DataObjectType;
import org.springframework.batch.core.ExitStatus;

public class CollectOperationManager {

   private static final Logger logger = Logger.getLogger(CollectOperationManager.class);

   private CollectionDriverManager collectionDriverManager;
   private ITimelyCollectionService timelyCollectionService;

   public void sendData(Map<String, Object> rawOperationData) {
      if (collectionDriverManager != null && timelyCollectionService != null) {
         Map<String, Map<String, ?>> data = null;
         Map<String, Map<String, Object>> operationData =
                 timelyCollectionService.collectData(rawOperationData, DataObjectType.OPERATION);
         if (operationData == null) {
            return;
         }
         if (isClusterRelated(rawOperationData)) {
            Map<String, Map<String, Object>> clusterSnapshotData =
                    timelyCollectionService.collectData(rawOperationData, DataObjectType.CLUSTER_SNAPSHOT);
            if (clusterSnapshotData != null) {
               data = timelyCollectionService.mergeData(operationData, clusterSnapshotData);
            }
         } else {
            data =  new HashMap<String, Map<String, ?>>();
            data.putAll(operationData);
         }
         if (data != null && !data.isEmpty()) {
            collectionDriverManager.getDriver().send(data);
         }
      }
   }

   private boolean isClusterRelated(Map<String, ?> operationData) {
      if (!operationData.containsKey(CollectionConstants.OPERATION_NAME)) {
         return false;
      }
      String operationName = (String) operationData.get(CollectionConstants.OPERATION_NAME);
      if (operationName.trim().equals(CollectionConstants.METHOD_CREATE_CLUSTER)
            || operationName.trim().equals(CollectionConstants.METHOD_CONFIG_CLUSTER)
            || operationName.trim().equals(CollectionConstants.METHOD_RESIZE_CLUSTER)
            || operationName.trim().equals(CollectionConstants.METHOD_SCALE_NODE_GROUP_RESOURCE)) {
         return true;
      }
      return false;
   }

   private static List<Map<String, Object>> operations = new LinkedList<>();

   public ITimelyCollectionService getTimelyCollectionService() {
      return timelyCollectionService;
   }

   public void setTimelyCollectionService(
         ITimelyCollectionService timelyCollectionService) {
      this.timelyCollectionService = timelyCollectionService;
   }

   public static void storeOperationParameters(MethodInvocationProceedingJoinPoint joinPoint, Long returnValue) {
      if (!enabledDataCollection()) {
         return;
      }
      try {
         Map<String, Object> operation = getCommonParameters(joinPoint);
         operation.put(CollectionConstants.TASK_ID, returnValue);
         synchronized (operations) {
            operations.add(operation);
         }
      } catch (Throwable t) {
         logger.error("Got exception when store operation parameters.", t);
      }
   }

   public static void storeOperationParameters(MethodInvocationProceedingJoinPoint joinPoint) {
      if (!enabledDataCollection()) {
         return;
      }
      try {
         Map<String, Object> operation = getCommonParameters(joinPoint);
         operation.put(CollectionConstants.OPERATION_END_TIME,
                 operation.get(CollectionConstants.OPERATION_BEGIN_TIME));
         operation.put(CollectionConstants.OPERATION_STATUS, ExitStatus.COMPLETED.getExitCode());
         synchronized (operations) {
            operations.add(operation);
         }
      } catch (Throwable t) {
         logger.error("Got exception when store operation parameters.", t);
      }
   }

   private static boolean enabledDataCollection() {
      String enabled =
              new PropertiesUtil(CollectionDriverManager.getConfigurationFile())
                      .getProperty(CommonUtil.notNull(Configuration.getString(CollectionConstants.DEFAULT_SWITCH_NAME),
                              CollectionConstants.PHONE_HOME_SWITCH_NAME));
      return true == Boolean.parseBoolean(enabled.trim());
   }

   private static Map<String, Object> getCommonParameters(MethodInvocationProceedingJoinPoint joinPoint) {
      HashMap<String, Object> operation = new HashMap<>();
      operation.put(CollectionConstants.OBJECT_ID, CommonUtil.getUUID());
      Object[] args = joinPoint.getArgs();
      MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
      operation.put(CollectionConstants.OPERATION_NAME, methodSignature.getName());
      long timeStamp = System.currentTimeMillis();
      operation.put(CollectionConstants.OPERATION_BEGIN_TIME, timeStamp);
      List<Object> parameters = new ArrayList<>();
      if(args != null) {
         for (Object arg : args) {
            if (arg == null) {
               continue;
            }
            parameters.add(arg);
         }
      }
      operation.put(CollectionConstants.OPERATION_PARAMETERS, parameters);
      return operation;
   }

   public CollectionDriverManager getCollectionDriverManager() {
      return collectionDriverManager;
   }

   public void setCollectionDriverManager(
         CollectionDriverManager collectionDriverManager) {
      this.collectionDriverManager = collectionDriverManager;
   }

   public List<Map<String, Object>> consumeOperations () {
      List<Map<String, Object>> consumptionOperations = new LinkedList<>();
      synchronized (operations) {
         consumptionOperations.addAll(operations);
         operations.clear();
      }
      return consumptionOperations;
   }
}
