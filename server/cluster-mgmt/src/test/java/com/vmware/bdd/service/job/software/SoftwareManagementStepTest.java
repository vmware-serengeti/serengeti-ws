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
package com.vmware.bdd.service.job.software;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SoftwareManagementStepTest {
   private ApplicationContext ctx;

   @BeforeClass
   public void init() {
      ctx = new ClassPathXmlApplicationContext("spring/*-context.xml");
   }

   @Test
   public void createClusterTasklet() {
      SoftwareManagementStep softwareCreateClusterTasklet =
            ctx.getBean("softwareCreateClusterTasklet",
                  SoftwareManagementStep.class);
      ManagementOperation operation =
            softwareCreateClusterTasklet.getManagementOperation();
      Assert.assertEquals(ManagementOperation.CREATE, operation);
   }
}
