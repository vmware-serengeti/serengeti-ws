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
package com.vmware.bdd.aop.rest;

import com.vmware.bdd.manager.collection.CollectOperationManager;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;

/**
 * Created by qjin on 12/23/14.
 */
public class DataCollectorAdvice {
   private static final Logger logger = Logger
         .getLogger(DataCollectorAdvice.class);

   //TODO(qjin): check if there is multi-thread problem for joinpoint
   public void afterRestCallMethod(JoinPoint joinPoint) throws Throwable {
      CollectOperationManager.storeOperationParameters((MethodInvocationProceedingJoinPoint)joinPoint);
      logger.info("save joinPoint " + joinPoint + " to CollectOperationjManager");
   }

   public void afterClusterManagerMethod(JoinPoint joinPoint, Long returnValue) {
      CollectOperationManager.storeOperationParameters((MethodInvocationProceedingJoinPoint)joinPoint, returnValue);
   }
}
