/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.plugin.ironfan.impl.DefaultSoftwareManagerImpl;
import com.vmware.bdd.service.resmgmt.IAppManagerService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

@Service
public class SoftwareManagerCollector {

   private static final Logger logger = Logger
         .getLogger(SoftwareManagerCollector.class);

   @Autowired
   private IAppManagerService appManagerService;

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   private Map<String, SoftwareManager> cache =
         new HashMap<String, SoftwareManager>();

   private String privateKey = null;

   private static String configurationPrefix = "appmanager.factoryclass.";

   /**
    * Software manager name will be unique inside of BDE. Otherwise, creation
    * will fail. The appmanager information should be persisted in meta-db
    *
    * @param appManagerAdd
    */
   public synchronized void createSoftwareManager(AppManagerAdd appManagerAdd) {

      logger.info("Start to create software manager for " + appManagerAdd.getName());

      if (appManagerService.findAppManagerByName(appManagerAdd.getName()) != null) {
         logger.error("Name " + appManagerAdd.getName() + " already exists.");
         throw SoftwareManagerCollectorException.DUPLICATE_NAME(appManagerAdd
               .getName());
      }

      SoftwareManager softwareManager = loadSoftwareManager(appManagerAdd);

      logger.info("Add app manager to meta-db.");
      // add to meta-db through AppManagerService
      appManagerService.addAppManager(appManagerAdd);
   }

   private String getPrivateKey() {
      if (privateKey == null) {
         //TODO: find a proper way to read key from file
         privateKey = CommonUtil.readJsonFile(Constants.SERENGETI_PRIVATE_KEY_FILE);
      }
      return privateKey;
   }
   /**
    * @param appManagerAdd
    * @return
    */
   private SoftwareManager loadSoftwareManager(AppManagerAdd appManagerAdd) {
      // Retrieve app manager factory class from serengeti.properties
      String factoryClassName =
            Configuration.getString(configurationPrefix + appManagerAdd.getType());
      if (CommonUtil.isBlank(factoryClassName)) {
         logger.error("Factory class for " + appManagerAdd.getType()
               + " is not defined in serengeti.properties");
         throw SoftwareManagerCollectorException.CLASS_NOT_DEFINED(appManagerAdd
               .getType());
      }
      logger.info("Factory class name is " + factoryClassName);

      Class<?> factoryClass;
      try {
         factoryClass = Class.forName(factoryClassName);
      } catch (ClassNotFoundException e) {
         logger.error("Cannot load factory class " + factoryClassName
               + " in classpath.");
         throw SoftwareManagerCollectorException.CLASS_NOT_FOUND_ERROR(e,
               factoryClassName);
      }
      logger.info("Factory class loaded.");
      SoftwareManagerFactory softwareManagerFactory = null;
      try {
         softwareManagerFactory =
               (SoftwareManagerFactory) factoryClass.newInstance();
      } catch (InstantiationException e) {
         logger.error("Cannot instantiate " + factoryClassName);
         throw SoftwareManagerCollectorException.CAN_NOT_INSTANTIATE(e,
               factoryClassName);
      } catch (IllegalAccessException e) {
         logger.error(e.getMessage());
         throw SoftwareManagerCollectorException.ILLEGAL_ACCESS(e,
               factoryClassName);
      }

      logger.info("Start to invoke software manager factory to create software manager.");
      SoftwareManager softwareManager = null;
      try {
         softwareManager =
               softwareManagerFactory.getSoftwareManager(appManagerAdd.getUrl(), appManagerAdd
                     .getUsername(), appManagerAdd.getPassword().toCharArray(),
                     getPrivateKey());
      } catch (Exception ex) {
         logger.error("Create software manager failed: " + ex.getMessage());
         throw BddException.INTERNAL(ex, "Create software manager failed.");
      }

      logger.info("Check echo() of software manager.");
      // validate instance is reachable
      if (!softwareManager.echo()) {
         logger.error("Cannot connect to Software Manager "
               + appManagerAdd.getName() + ", check the connection information.");
         throw SoftwareManagerCollectorException.ECHO_FAILURE(appManagerAdd
               .getName());
      }

      cache.put(appManagerAdd.getName(), softwareManager);

      return softwareManager;
   }

   /**
    * Get software manager instance
    *
    * @param name
    * @return null if the name does not exist
    */
   public synchronized SoftwareManager getSoftwareManager(String name) {
      if (CommonUtil.isBlank(name)) {
         return cache.get(Constants.IRONFAN);
      }
      if (cache.containsKey(name)) {
         return cache.get(name);
      }

      //TODO:
      //it's either not defined or being initialized
      return null;
   }

   /**
    * Get software manager instance by cluster name (do not call this api before
    * cluster information is written to meta-db
    *
    * @param name
    * @return null if cluster name does not have a corresponding software
    *         manager instance
    */
   public synchronized SoftwareManager getSoftwareManagerByClusterName(String name) {
      ClusterEntity clusterEntity = clusterEntityManager.findByName(name);
      if (clusterEntity == null) {
         logger.warn("Can't find cluster with name: " + name);
         return null;
      }
      return getSoftwareManager(clusterEntity.getAppManager());
   }

   public synchronized void loadSoftwareManagers() {
      // TODO: load all software manager instances into memory while the Tomcat service is started
      // Should block request until initialized
      // temporarily load ironfan software manager instance here
      AppManagerAdd appManagerAdd;
      if (appManagerService.findAppManagerByName(Constants.IRONFAN) == null) {
         appManagerAdd = new AppManagerAdd();
         appManagerAdd.setName(Constants.IRONFAN);
         appManagerAdd.setDescription(Constants.IRONFAN_DESCRIPTION);
         appManagerAdd.setType(Constants.IRONFAN);
         appManagerAdd.setUrl("http://localhost");
         appManagerAdd.setUsername("n/a");
         appManagerAdd.setPassword("n/a");
         appManagerAdd.setSslCertificate("n/a");
         appManagerService.addAppManager(appManagerAdd);
      }
      SoftwareManager ironfanSoftwareManager = new DefaultSoftwareManagerImpl();
      cache.put(Constants.IRONFAN, ironfanSoftwareManager);

      List<AppManagerEntity> appManagers = appManagerService.findAll();
      for (AppManagerEntity appManager : appManagers) {
         if (!appManager.getName().equals(Constants.IRONFAN)) {
            appManagerAdd = new AppManagerAdd();
            appManagerAdd.setName(appManager.getName());
            appManagerAdd.setDescription(appManager.getDescription());
            appManagerAdd.setType(appManager.getType());
            appManagerAdd.setUrl(appManager.getUrl());
            appManagerAdd.setUsername(appManager.getUsername());
            appManagerAdd.setPassword(appManager.getPassword());
            appManagerAdd.setSslCertificate(appManager.getSslCertificate());
            loadSoftwareManager(appManagerAdd);
         }
      }

   }

   public List<AppManagerRead> getAllAppManagerReads() {
      List<AppManagerRead> appManagerReads =
            appManagerService.getAllAppManagerReads();
      for (AppManagerRead appManagerRead : appManagerReads) {
         appManagerRead.setManagedClusters(clusterEntityManager
               .findByAppManager(appManagerRead.getName()));
      }
      return appManagerReads;
   }

   public AppManagerRead getAppManagerRead(String appManagerName) {
      AppManagerRead appManagerRead =
            appManagerService.getAppManagerRead(appManagerName);
      if (appManagerRead != null) {
         appManagerRead.setManagedClusters(clusterEntityManager
               .findByAppManager(appManagerName));
      }
      return appManagerRead;
   }
}
