/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.aop.logging;

import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.vmware.bdd.exception.BddException;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Aspect
public class ExceptionHandlerAspect {
   private static final Logger logger = Logger
         .getLogger(ExceptionHandlerAspect.class);

   @Pointcut("execution(* com.vmware.bdd.service..*.*(..))")
   private void serviceCall() {
   }

   @Pointcut("@annotation(com.vmware.bdd.aop.annotation.DisableExceptionLogging)")
   private void disableExecptionLogging() {
   }

   @Pointcut("execution(* com.vmware.bdd.service..*.*(..)) && !@annotation(com.vmware.bdd.aop.annotation.DisableExceptionLogging)")
   private void loggingExceptionInServiceCall() {
   }

   @AfterThrowing(pointcut = "loggingExceptionInServiceCall()", throwing = "t")
   public void logException(Throwable t) throws Throwable {
      logger.info("Aspect for exception handling");
      BddException ex = BddException.wrapIfNeeded(t, "Service AOP.");
      logger.error("Service error", ex);
      throw t;
   }

}
