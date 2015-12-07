/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.aop.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.vmware.bdd.exception.BddException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

/**
 * Created by xiaoliangl on 11/17/15.
 */
public class UnhandledExceptionLoggingAspect {
   // Obtain a suitable logger.
   private static Logger logger = Logger.getLogger(UnhandledExceptionLoggingAspect.class);

   /**
    * Called between the throw and the catch
    * log REST Arguments on unhandled exception reaches REST layer
    */
   public void logRestArgsOnUnhandledException(JoinPoint joinPoint, Throwable e){
      logger.debug("UnhandledExceptionLoggingAspect is triggered");
      if(e instanceof BddException) {
         logger.debug("BddException reaches REST layer.", e);
         return;
      }

      Signature signature = joinPoint.getSignature();
      String methodName = signature.getName();

      ArrayList<Object> argsList = Lists.newArrayList();
      for(Object arg : joinPoint.getArgs()) {
         //for Http Request and Response, don't care.
         if(arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
            continue;
         }

         argsList.add(arg);
      }

      // if the REST API don't have args, don't care.
      if(CollectionUtils.isNotEmpty(argsList)) {
         ObjectMapper objectMapper = new ObjectMapper();
         try {
            logger.error("An unhandled exception reaches REST layer. Dump REST arguments to help trouble-shooting: "
                  + methodName + ": " + objectMapper.writeValueAsString(argsList));
         } catch (JsonProcessingException ex) {
            logger.error("unexpected json exception", ex);
         }
      }
   }
}
