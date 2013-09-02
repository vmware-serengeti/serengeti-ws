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
package com.vmware.bdd.aop.tx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.core.annotation.AnnotationUtils;

import com.vmware.bdd.aop.annotation.RetryTransaction;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.TxRetryException;
import com.vmware.bdd.exception.UniqueConstraintViolationException;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public class RetryTransactionAdvice {

   private static final Logger logger = Logger
         .getLogger(RetryTransactionAdvice.class);

   public void retry(ProceedingJoinPoint pjp) throws Throwable {
      logger.info("retry transaction");
      int retriesLeft = 5;
      String methodName = pjp.getSignature().getName();
      MethodSignature signature = (MethodSignature) pjp.getSignature();
      Method method = signature.getMethod();
      if(method.getDeclaringClass().isInterface()){
         method = pjp.getTarget().getClass().getDeclaredMethod(methodName, method.getParameterTypes());
      }
      Annotation[] annotations = method.getDeclaredAnnotations();
      for (Annotation a : annotations) {
         if (a instanceof RetryTransaction) {
            RetryTransaction retryAnno = (RetryTransaction) a;
            retriesLeft = retryAnno.value();
         }
      }

      RetryTransaction retryTrx = AnnotationUtils.findAnnotation(method, RetryTransaction.class);
      retriesLeft = retryTrx.value();
      Throwable rootCause = null;
      boolean success = false;
      while (!success) {
         try {
            pjp.proceed();
            success = true;
         } catch (Throwable ex) {
            rootCause = (ex instanceof BddException) ? ex.getCause() : ex;
            if (isRetryable(rootCause)) {
               if (retriesLeft > 0) {
                  retriesLeft--;
               } else {
                  throw TxRetryException.wrap(rootCause, false);
               }
            } else if (isUniqViolation(rootCause)) {
               throw UniqueConstraintViolationException
                     .wrap((ConstraintViolationException) rootCause);
            } else {
               throw BddException.wrapIfNeeded(ex,
                     "Exception in a DAL transaction.");
            }
         }
      }
      if (!success) {
         if (rootCause != null) {
            logger.warn("retry transaction failed.", rootCause);
            throw rootCause;
         } else {
            logger.warn("retry transction failed.");
            throw new Exception("retry transaction failed");
         }
      } else {
         if (rootCause != null) {
            logger.warn("retry transaction completed. Failure root cause:"
                  + rootCause.getMessage());
         } else {
            logger.info("normal operation");
         }
      }
   }

   /**
    * @param ex
    *           -- the "real" exception thrown from the transaction body. It can
    *           be null if we throw BddException with no cause.
    * @return true if the given exception corresponds to Hibernate's uniqueness
    *         violation.
    **/
   private boolean isUniqViolation(Throwable ex) {
      final String uniquenessViolation = new String("23505"); // SQL State error
      return ex instanceof ConstraintViolationException
            && ((ConstraintViolationException) ex).getSQLState().equals(
                  uniquenessViolation);
   }

   /**
    * @param ex
    *           -- the "real" exception thrown from the transaction body. It can
    *           be null if we throw BddException with no cause.
    * @return true if the given exception should cause a retry, e.g.
    *         serialization failure or deadlock etc.
    **/
   private boolean isRetryable(Throwable ex) {
      final String psqlDeadlockDetected = new String("40P01"); // SQL State error specific to PSQL
      return ((ex instanceof LockAcquisitionException) || (ex instanceof GenericJDBCException && ((GenericJDBCException) ex)
            .getSQLState().equals(psqlDeadlockDetected)));
   }

}
