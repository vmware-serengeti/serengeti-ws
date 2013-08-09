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
package com.vmware.bdd.manager;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.ResourceScale;

public class TestScaleManager {
   private ScaleManager scaleMgr;

   //@Test
   public void buildJobParameters() {
      ApplicationContext context =
            new ClassPathXmlApplicationContext("spring/*.xml");
      scaleMgr = context.getBean("scaleManager", ScaleManager.class);
      ResourceScale scale = new ResourceScale("apache2", "worker", 3, 4000);
      scaleMgr.buildJobParameters(scale);
   }

   @Test
   public void scaleNodeGroupResource() {

   }
}
