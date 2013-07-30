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
package com.vmware.bdd.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 *
 */
public class ResourceInitializer implements ServletContextListener {

   private static final Logger logger = Logger
         .getLogger(ResourceInitializer.class);
   private IClusteringService clusterSvc;

   /* (non-Javadoc)
    * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
    */
   @Override
   public void contextInitialized(ServletContextEvent sce) {
      try {
         logger.info("ResourceInitializer::contextInitialized started");
         ServletContext sc = sce.getServletContext();
         WebApplicationContext wac =
               WebApplicationContextUtils.getWebApplicationContext(sc);
         initService(wac);
         IResourceInitializerService resInitializerSvc =
               wac.getBean(IResourceInitializerService.class);
         if (!resInitializerSvc.isResourceInitialized()) {
            boolean initResource = Configuration.getBoolean("init_resource");
            logger.info("need initializing resoruce? " + initResource);
            if (initResource) {
               resInitializerSvc.initResource();
            }
            resInitializerSvc.updateOrInsertServerInfo();
         }
         logger.info("ResourceInitializer completed");
      } catch (Throwable t) {
         logger.error("Resource initialization failed.", t);
      }
   }


   /* (non-Javadoc)
    * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
    */
   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      try {
         clusterSvc.destroy();
         VcContext.endSession();
      } catch (Throwable t) {
         logger.error("Resource destroy failed.", t);
      }
   }

   private void initService(WebApplicationContext wac) {
      logger.warn("test message");
      logger.info("init service start");
      clusterSvc = wac.getBean("clusteringService",IClusteringService.class);
      logger.info("got bean");
      clusterSvc.init();
   }

}
