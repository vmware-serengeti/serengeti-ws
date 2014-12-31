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
import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;

import com.vmware.bdd.service.collection.ITimelyCollectionService;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectOperationManager {
   private static final Logger logger = Logger.getLogger(CollectOperationManager.class);
   ITimelyCollectionService timelyCollectionService;

   //restCallId to joinPoint
   private static Map<String, MethodInvocationProceedingJoinPoint> restCallRawData = new ConcurrentHashMap<>();

   public ITimelyCollectionService getTimelyCollectionService() {
      return timelyCollectionService;
   }

   public void setTimelyCollectionService(
         ITimelyCollectionService timelyCollectionService) {
      this.timelyCollectionService = timelyCollectionService;
   }

   public static void setRestCallRawData(String restCallId, MethodInvocationProceedingJoinPoint joinPoint) {
      restCallRawData.put(restCallId, joinPoint);
   }

   public static List<Map<String, Object>> parseRestCallRawData() {
      List<Map<String, Object>> restCalls = null;

      Iterator iterator = restCallRawData.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry entry = (Map.Entry) iterator.next();
         if (entry != null) {
            assert (entry.getKey() != null);
            if (restCalls == null) {
               restCalls = new ArrayList<>();
            }
            restCalls.add(getOperation(entry));
            restCallRawData.remove(entry.getKey());
         }
      }
      return restCalls;
   }

   private static Map<String, Object> getOperation(Map.Entry<String, MethodInvocationProceedingJoinPoint> entry) {
      HashMap<String, Object> operation = new HashMap<>();
      operation.put("operation_id", entry.getKey());
      MethodInvocationProceedingJoinPoint joinPoint = entry.getValue();
      operation.put("operation_name", joinPoint.getSignature().getName());
      long timeStamp = System.currentTimeMillis();
      operation.put("begin_time", timeStamp);
      operation.put("end_time", timeStamp);

      //TODO(qjin): need to be improved to make the operation_status accurate
      operation.put("operation_status", TaskStatus.COMPLETED);
      MethodSignature signature = (MethodSignature)joinPoint.getSignature();
      Object[] args = joinPoint.getArgs();
      Class[] paramTypes = signature.getParameterTypes();

      Map<String, Map> parameters = new HashMap<>();
      for (int i = 0; i < paramTypes.length; i++) {
         if (paramTypes[i].toString().contains("HttpServerletResponse")) {
            if (!isSyncedRequest((HttpServletResponse)args[i])) {
               operation.remove("end_time");
            }
         }
         Object arg = args[i];
         if (arg == null) {
            continue;
         }
         HashMap<Class, Object> parameter = new HashMap();
         parameter.put(paramTypes[i], args[i]);
         parameters.put("arg" + i, parameter);
      }
      operation.put("operation_parameters", parameters);
      return operation;
   }

   private static boolean isSyncedRequest(HttpServletResponse response) {
      Collection<String> headerNames = response.getHeaderNames();
      for (String header: headerNames) {
         if (header.equals(Constants.RESPONSE_HEADER_LOCATION)) {
            //if has "Location" header, it is an asyn rest call
            //TODO(qjin): consider if it can be improved to check the sync request
            logger.info("header is: " + response.getHeader(header));
            return false;
         }
      }
      return true;
   }
}
