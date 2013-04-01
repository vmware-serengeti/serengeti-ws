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
package com.vmware.bdd.service.impl;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.aop.annotation.DisableExceptionLogging;
import com.vmware.bdd.aop.annotation.RetryTransaction;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.ITestService;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
@Component
public class TestService implements ITestService {

   private static final Logger logger = Logger.getLogger(TestService.class);

   private static int counter = 0;

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.ITestService#test()
    */
   @Override
   @Transactional
   @RetryTransaction(7)
   public void retryFive() {
      logger.info("retryFive:: start");
      counter++;
      if (counter < 5) {
         throw new LockAcquisitionException("sample dal exception",
               new SQLException("1111"));
      }
   }

   @Override
   @Transactional
   @RetryTransaction(10)
   public void retryForever() {
      logger.info("retryForever:: start");
      throw new LockAcquisitionException("sample dal exception",
            new SQLException("1111"));
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.ITestService#normal()
    */
   @Override
   @Transactional
   @RetryTransaction(3)
   public void normal() {
      logger.info("without exception");
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.ITestService#throwException()
    */
   @Override
   public void throwException() {
      throw new RuntimeException("test runtime exception");
   }

   @Override
   public void throwBddException() {
      throw BddException.ALREADY_EXISTS("test", "test object");
   }

   @Override
   @DisableExceptionLogging
   public void throwBddExceptionWithAnnotation() {
      throw BddException.ALREADY_EXISTS("entity", "mock entity");
   }
}
