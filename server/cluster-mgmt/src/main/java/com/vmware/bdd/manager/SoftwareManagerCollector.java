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
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.service.resmgmt.IAppManagerService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;
import com.vmware.bdd.utils.CommonUtil;

@Service
public class SoftwareManagerCollector {

   private static final Logger logger = Logger
         .getLogger(SoftwareManagerCollector.class);

   @Autowired
   private IAppManagerService pluginService;

   private Map<String, SoftwareManager> cache =
         new HashMap<String, SoftwareManager>();

   private static String configurationPrefix = "appmanager.factoryclass.";

   /**
    * Software manager name will be unique inside of BDE. Otherwise, creation
    * will fail. The plugin information should be persisted in meta-db
    *
    * @param pluginAdd
    */
   public synchronized void createSoftwareManager(AppManagerAdd pluginAdd) {

      if (pluginService.findAppManagerByName(configurationPrefix + pluginAdd.getName()) != null) {
         logger.error("Name " + pluginAdd.getName() + " already exists.");
         throw SoftwareManagerCollectorException.DUPLICATE_NAME(pluginAdd
               .getName());
      }

      // Retrieve app manager factory class from serengeti.properties
      String factoryClassName =
            Configuration.getString(pluginAdd.getProvider());
      if (CommonUtil.isBlank(factoryClassName)) {
         logger.error("Factory class for " + pluginAdd.getProvider()
               + " is not defined in serengeti.properties");
         throw SoftwareManagerCollectorException.CLASS_NOT_DEFINED(pluginAdd
               .getProvider());
      }
      Class<?> factoryClass;
      try {
         factoryClass = Class.forName(factoryClassName);
      } catch (ClassNotFoundException e) {
         logger.error("Cannot load factory class " + factoryClassName
               + " in classpath.");
         throw SoftwareManagerCollectorException.CLASS_NOT_FOUND_ERROR(e,
               factoryClassName);
      }
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
      SoftwareManager softwareManager =
            softwareManagerFactory.getSoftwareManager(pluginAdd.getHost(),
                  pluginAdd.getUsername(), pluginAdd.getPassword()
                        .toCharArray(), pluginAdd.getPrivateKey());

      // validate instance is reachable
      if (!softwareManager.echo()) {
         logger.error("Cannot connect to Software Manager "
               + pluginAdd.getName() + ", check the connection information.");
         throw SoftwareManagerCollectorException.ECHO_FAILURE(pluginAdd
               .getName());
      }

      // add to meta-db through PluginService
      pluginService.addAppManager(pluginAdd);
      cache.put(pluginAdd.getName(), softwareManager);
   }

   /**
    * Get software manager instance
    *
    * @param name
    * @return null if the name does not exist
    */
   public synchronized SoftwareManager getSoftwareManager(String name) {
      if (cache.containsKey(name)) {
         return cache.get(name);
      }

      //TODO:
      //it's either not defined or being initialized
      return null;
   }

   public synchronized void loadSoftwareManagers() {
      // TODO: load all software manager instances into memory while the Tomcat service is started
      // Should block request until initialized
   }
}
