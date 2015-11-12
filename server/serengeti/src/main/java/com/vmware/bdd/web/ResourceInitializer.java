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
package com.vmware.bdd.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.bdd.plugin.NgcBDERegistrar;
import com.vmware.bdd.restore.PostRestoreHandler;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.collection.ICollectionInitializerService;
import com.vmware.bdd.service.resmgmt.IResourceInitializerService;
import com.vmware.bdd.upgrade.PostUpgradeHandler;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;

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
         initDataCollection(wac);
         IResourceInitializerService resInitializerSvc =
               wac.getBean(IResourceInitializerService.class);
         if (!resInitializerSvc.isResourceInitialized()) {
            boolean initResource = Configuration.getBoolean("init_resource");
            logger.info("need initializing resource? " + initResource);
            if (initResource) {
               resInitializerSvc.initResource();
            }
            resInitializerSvc.updateOrInsertServerInfo();
         }
         logger.info("ResourceInitializer completed");

         // do some post restore work for the first time start after restore
         if ( ConfigInfo.isJustRestored() ) {
            logger.info("handle post restore for user management");
            PostUpgradeHandler postUpgradeHandler = wac.getBean("postUpgradeHandler", PostUpgradeHandler.class);
            postUpgradeHandler.handlePostUpgradeForUsrMgmt();
            logger.info("handle post restore for other functions");
            PostRestoreHandler postRestoreHandler = wac.getBean("postRestoreHandler", PostRestoreHandler.class);
            postRestoreHandler.handlePostRestore();
            // set the just_restore tag to be false
            ConfigInfo.setJustRestored(false);
            ConfigInfo.save();
         }

         // do some post upgrade work for the first time start after upgrade
         if ( ConfigInfo.isJustUpgraded() ) {
            logger.info("handle post upgrade for the first time start after upgrade");
            PostUpgradeHandler postUpgradeHandler = wac.getBean("postUpgradeHandler", PostUpgradeHandler.class);
            postUpgradeHandler.handlePostUpgrade();
            // set the just_upgrade tag to be false
            ConfigInfo.setJustUpgraded(false);
            ConfigInfo.save();
         }
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
      logger.info("init service start");
      clusterSvc = wac.getBean("clusteringService",IClusteringService.class);
      logger.info("got bean");
      clusterSvc.init();
      boolean regWebPlugin = Configuration.getBoolean("reg_web_plugin");
      if(regWebPlugin) {
         try {
            NgcBDERegistrar ngc = new NgcBDERegistrar();
            ngc.initNgcRegistration();
         } catch (Exception e) {
            logger.error("NGC BDE plugin auto-installation failed! You need to manually register the BDE plugin for vCenter.");
            logger.error(e.getMessage());
         }
      }
   }

   private void initDataCollection(WebApplicationContext wac)
         throws ParseException {
      logger.info("ResourceInitializer::initDataCollection started");
      ICollectionInitializerService collectionInitializerService =
            wac.getBean("collectionInitializerService",
                  ICollectionInitializerService.class);
      String deployTimeStr = Configuration.getString("serengeti.deploy_time");
      Date deployTime = null;
      if (CommonUtil.isBlank(deployTimeStr)) {
         deployTime = new Date();
      } else {
         SimpleDateFormat df = new SimpleDateFormat(Constants.DEPLOY_TIME_FORMAT);
         deployTime = df.parse(deployTimeStr);
      }
      collectionInitializerService.setDeployTime(deployTime);
      collectionInitializerService.generateInstanceId();
   }
}
