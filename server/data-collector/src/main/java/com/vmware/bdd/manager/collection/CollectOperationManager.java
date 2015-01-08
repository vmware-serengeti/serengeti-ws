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

import com.vmware.bdd.plugin.ambari.api.model.cluster.TaskStatus;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;

import com.vmware.bdd.service.collection.ITimelyCollectionService;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectOperationManager {
   private static final Logger logger = Logger.getLogger(CollectOperationManager.class);
   ITimelyCollectionService timelyCollectionService;

   private static List<Map<String, Object>> operations = new LinkedList<>();

   public ITimelyCollectionService getTimelyCollectionService() {
      return timelyCollectionService;
   }

   public void setTimelyCollectionService(
         ITimelyCollectionService timelyCollectionService) {
      this.timelyCollectionService = timelyCollectionService;
   }

   public static void storeOperationParameters(MethodInvocationProceedingJoinPoint joinPoint, Long returnValue) {
      try {
         Map<String, Object> operation = getCommonParameters(joinPoint);
         operation.put("operation_status", TaskStatus.IN_PROGRESS);
         operation.put("task_id", returnValue);
         synchronized (operations) {
            operations.add(operation);
         }
      } catch (Throwable t) {
         logger.error("Got exception when store operation parameters.", t);
      }
   }

   public static void storeOperationParameters(MethodInvocationProceedingJoinPoint joinPoint) {
      try {
         Map<String, Object> operation = getCommonParameters(joinPoint);
         operation.put("end_time", operation.get("begin_time"));
         operation.put("operation_status", TaskStatus.COMPLETED);
         synchronized (operations) {
            operations.add(operation);
         }
      } catch (Throwable t) {
         logger.error("Got exception when store operation parameters.", t);
      }
   }

   private static Map<String, Object> getCommonParameters(MethodInvocationProceedingJoinPoint joinPoint) {
      HashMap<String, Object> operation = new HashMap<>();
      operation.put("id", CommonUtil.getUUID());
      Object[] args = joinPoint.getArgs();
      MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
      operation.put("operation_name", methodSignature.getName());
      long timeStamp = System.currentTimeMillis();
      operation.put("begin_time", timeStamp);

      Class[] paramTypes = methodSignature.getParameterTypes();
      Map<String, Map> parameters = new HashMap<>();
      for (int i = 0; i < paramTypes.length; i++) {
         Object arg = args[i];
         if (arg == null) {
            continue;
         }
         HashMap<Class, Object> parameter = new HashMap();
         parameter.put(paramTypes[i], args[i]);
         parameters.put("arg" + i, parameter);
      }
      operation.put("operation_parameters", parameters);
      return  operation;
   }

}
