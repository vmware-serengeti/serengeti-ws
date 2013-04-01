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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.ITestService;

public class ExceptionHandlerAspectTest {
   private ApplicationContext ctx;
   private ITestService service;

   @BeforeTest
   public void beforeTest() {
      ctx = new ClassPathXmlApplicationContext("META-INF/spring/test-aop-context.xml");
      service = ctx.getBean(ITestService.class);
   }

   @Test
   public void testThrowException() {
      try {
         service.throwException();
      } catch (Throwable t) {
         System.out.println("catch exception:" + t.getMessage());
      }
   }

   @Test(expectedExceptions=BddException.class)
   public void testThrowBddException(){
      service.throwBddException();
   }

   @Test(expectedExceptions=BddException.class)
   public void testThrowBddExceptionWithAnnocation(){
      service.throwBddExceptionWithAnnotation();
   }
}
