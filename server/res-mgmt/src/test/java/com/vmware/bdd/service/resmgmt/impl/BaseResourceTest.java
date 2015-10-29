/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.resmgmt.impl;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.worker.CmsWorker;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.vcservice.VcContext;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public abstract class BaseResourceTest {

   private static final Logger logger = Logger
         .getLogger(BaseResourceTest.class);

   protected ApplicationContext ctx;

   public void init() {
      initVC();
      ctx = new ClassPathXmlApplicationContext("META-INF/spring/*-context.xml");
   }

   /**
    *
    */
   public void initVC() {
      try {
         Configuration
               .approveBootstrapInstanceId(Configuration.BootstrapUsage.ALLOWED);
         //Configuration.approveBootstrapInstanceId(Configuration.BootstrapUsage.FINALIZED);

         VcContext.initVcContext();
//         CmsWorker.addPeriodic(new SyncInventoryRequest());
         if (VcContext.isServiceConnected()) {
            VcInventory.loadInventory();
         }
         logger.info("loaded vc inventory");
      } catch (Throwable t) {
         logger.error("Init VC failed", t);
      }
   }

   public void destroy() {
      VcContext.endSession();
   }
}
