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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.Base64;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.plugin.ironfan.impl.DefaultSoftwareManagerImpl;
import com.vmware.bdd.service.resmgmt.IAppManagerService;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;
import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

@Service
public class SoftwareManagerCollector implements InitializingBean {

   private static final Logger logger = Logger
         .getLogger(SoftwareManagerCollector.class);

   @Autowired
   private IAppManagerService appManagerService;

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   private ConcurrentHashMap<String, SoftwareManager> cache =
         new ConcurrentHashMap<String, SoftwareManager>();

   private String privateKey = null;

   private static String configurationPrefix = "appmanager.factoryclass.";
   private static String appmanagerTypesKey = "appmanager.types";

   private static String appmgrConnTimeOutKey = "appmanager.connect.timeout.seconds";

   /**
    * Software manager name will be unique inside of BDE. Otherwise, creation
    * will fail. The appmanager information should be persisted in meta-db
    *
    * @param appManagerAdd
    */
   public void createSoftwareManager(AppManagerAdd appManagerAdd) {

      logger.info("First we need check if the appmgr is valid for use.");

      //validate url in appManagerAdd
      List<String> errorMsgs = new ArrayList<String>();
      if (!CommonUtil.validateUrl(appManagerAdd.getUrl(), errorMsgs)) {
         throw SoftwareManagerCollectorException.INVALID_URL(errorMsgs);
      }

      String sslCertificate = appManagerAdd.getSslCertificate();
      if (!CommonUtil.isBlank(sslCertificate)) {
         saveSslCertificate(sslCertificate);
      }

      SoftwareManager softwareManager = loadSoftwareManager(appManagerAdd);

      validateSoftwareManager(appManagerAdd.getName(), softwareManager);

      logger.info("The appmgr can be reached and will be created.");

      // add to meta-db through AppManagerService
      createSoftwareManagerInternal(appManagerAdd, softwareManager);
   }

   /**
    * Software manager name will be unique inside of BDE. Otherwise, creation
    * will fail. The appmanager information should be persisted in meta-db
    *
    * @param appManagerAdd, softwareManager
    */
   private synchronized void createSoftwareManagerInternal(AppManagerAdd appManagerAdd,
		   SoftwareManager softwareManager) {

      logger.info("Start to create application manager for " + appManagerAdd.getName());

      if (appManagerService.findAppManagerByName(appManagerAdd.getName()) != null) {
         logger.error("Name " + appManagerAdd.getName() + " already exists.");
         throw SoftwareManagerCollectorException.DUPLICATE_NAME(appManagerAdd
               .getName());
      }

      cache.put(appManagerAdd.getName(), softwareManager);

      logger.info("Add app manager to meta-db.");

      // add to meta-db through AppManagerService
      appManagerService.addAppManager(appManagerAdd);
   }

   private String getPrivateKey() {
      if (privateKey == null) {
         try {
            privateKey =
                  CommonUtil.dataFromFile(Constants.SERENGETI_PRIVATE_KEY_FILE);
         } catch (FileNotFoundException e) {
            logger.error("Serengeti private key file "
                  + Constants.SERENGETI_PRIVATE_KEY_FILE + " does not exist.",
                  e);
            throw SoftwareManagerCollectorException.PRIVATE_KEY_NOT_FOUND(e,
                  Constants.SERENGETI_PRIVATE_KEY_FILE);
         } catch (IOException e) {
            logger.error("Error in reading Serengeti private key file "
                  + Constants.SERENGETI_PRIVATE_KEY_FILE + ".", e);
            throw SoftwareManagerCollectorException.PRIVATE_KEY_READ_ERROR(e,
                  Constants.SERENGETI_PRIVATE_KEY_FILE);
         }
      }
      return privateKey;
   }

   public SoftwareManager loadSoftwareManager(String appManagerName) {
      AppManagerEntity appManagerEntity =
            appManagerService.findAppManagerByName(appManagerName);
      return loadSoftwareManager(toAppManagerAdd(appManagerEntity)) ;
   }

   /**
    *
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

      SoftwareManagerFactory softwareManagerFactory = null;
      try {
         Class<? extends SoftwareManagerFactory> clazz = ReflectionUtils.getClass(factoryClassName, SoftwareManagerFactory.class);
         logger.info("Factory class loaded.");
         softwareManagerFactory = ReflectionUtils.newInstance(clazz);
      } catch (Exception e) {
         logger.error(e.getMessage());
         throw SoftwareManagerCollectorException.CAN_NOT_INSTANTIATE(e, factoryClassName);
      }

      logger.info("Start to invoke application manager factory to create application manager.");
      SoftwareManager softwareManager = null;
      try {
         softwareManager =
               softwareManagerFactory.getSoftwareManager(appManagerAdd.getUrl(), appManagerAdd
                     .getUsername(), appManagerAdd.getPassword().toCharArray(),
                     getPrivateKey());
      } catch (Exception ex) {
         logger.error("Create application manager failed: " + ex.getMessage(), ex);
         throw SoftwareManagerCollectorException.CONNECT_FAILURE(
               appManagerAdd.getName(), ExceptionUtils.getRootCauseMessage(ex));
      }

      return softwareManager;
   }

   /**
    *
    * @param name
    * @param softwareManager
    */
   private void validateSoftwareManager(String name, final SoftwareManager softwareManager) {
      logger.info("Check echo() of application manager.");
      // validate instance is reachable
      try {
         // if the target ip does not exist or the host is shutdown, it will take about 2 minutes
         // for the socket connection to time out.
         // here we fork a child thread to do the actual connecting action, if it does not succeed
         // within given waiting time(default is 30s), we will consider it to be failure.
         ExecutorService exec = Executors.newFixedThreadPool(1);
         Future<Boolean> futureResult = exec.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
               // TODO Auto-generated method stub
               return softwareManager.echo();
            }
         });

         boolean gotEcho = false;
         Boolean result = (Boolean)waitForThreadResult(futureResult);
         if ( null != result )
         {
            gotEcho = result;
         }
         exec.shutdown();

         if (!gotEcho) {
            logger.error("Application manager "
                  + name
                  + " status is unhealthy. Please check application manager console for more details.");
            throw SoftwareManagerCollectorException.ECHO_FAILURE(name);
         }
      } catch (SoftwareManagementPluginException e) {
         logger.error("Cannot connect to application manager "
               + name + ", check the connection information.", e);
         throw SoftwareManagerCollectorException.CONNECT_FAILURE(name,
               e.getMessage());
      }
   }

   /**
    * Get software manager instance
    *
    * @param name
    * @return null if the name does not exist
    */
   public SoftwareManager getSoftwareManager(String name) {
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
         appManagerAdd.setUrl("");
         appManagerAdd.setUsername("");
         appManagerAdd.setPassword("");
         appManagerAdd.setSslCertificate("");
         appManagerService.addAppManager(appManagerAdd);
      }
      SoftwareManager ironfanSoftwareManager = new DefaultSoftwareManagerImpl();
      cache.put(Constants.IRONFAN, ironfanSoftwareManager);

      List<AppManagerEntity> appManagers = appManagerService.findAll();
      for (AppManagerEntity appManager : appManagers) {
         if (!appManager.getName().equals(Constants.IRONFAN)) {
            appManagerAdd = toAppManagerAdd(appManager);
            // Do not block initialization in case of Exception
            try {
               SoftwareManager softwareManager =
                     loadSoftwareManager(appManagerAdd);
               cache.put(appManager.getName(), softwareManager);
            } catch (Exception e) {
               logger.error("Error loading application manager: " + appManagerAdd,
                     e);
            }
         }
      }

   }

   /**
    * @param appManager
    * @return
    */
   private AppManagerAdd toAppManagerAdd(AppManagerEntity appManager) {
      AppManagerAdd appManagerAdd = new AppManagerAdd();
      appManagerAdd.setName(appManager.getName());
      appManagerAdd.setDescription(appManager.getDescription());
      appManagerAdd.setType(appManager.getType());
      appManagerAdd.setUrl(appManager.getUrl());
      appManagerAdd.setUsername(appManager.getUsername());
      appManagerAdd.setPassword(appManager.getPassword());
      appManagerAdd.setSslCertificate(appManager.getSslCertificate());
      return appManagerAdd;
   }

   public List<AppManagerRead> getAllAppManagerReads() {
      logger.debug("get all app managers");
      List<AppManagerRead> appManagerReads =
            appManagerService.getAllAppManagerReads();
      logger.debug("got all app managers");
      return appManagerReads;
   }

   public AppManagerRead getAppManagerRead(String appManagerName) {
      AppManagerRead appManagerRead =
            appManagerService.getAppManagerRead(appManagerName);
      if (appManagerRead != null) {
         setAppManagerReadDynamicProperties(appManagerRead);
      }
      return appManagerRead;
   }

   private Object waitForThreadResult(Future<?> result) {
      // the default value for the wait time is 30 seconds
      int defaultTime = Constants.APPMGR_CONNECT_TIMEOUT_SECONDS;
      int waitTime = Configuration.getInt(appmgrConnTimeOutKey, defaultTime);
      for ( int i=0; i<waitTime; i++ ) {
         try {
            if ( result.isDone() ) {
               return result.get();
            }
            Thread.sleep(1000);
         } catch (Exception e) {
            logger.error("Unexpected error occurred with threading.");
         }
      }
      return null;
   }

   /**
    * @param appManagerRead
    */
   private void setAppManagerReadDynamicProperties(AppManagerRead appManagerRead) {
      appManagerRead.setManagedClusters(clusterEntityManager
            .findByAppManager(appManagerRead.getName()));

      String softMgrVersion = "UNKNOWN";

      final SoftwareManager softwareManager = this.getSoftwareManager(appManagerRead.getName());
      if (softwareManager != null) {
         // fork a child thread to do the actual connecting action
         // this is to avoid the time out issue for the socket connection when the target host is shutdown
         ExecutorService exec = Executors.newFixedThreadPool(1);
         Future<String> futureResult = exec.submit(new Callable<String>(){
            @Override
            public String call() throws Exception {
               // TODO Auto-generated method stub
               return softwareManager.getVersion();
            }
         });

         String result = (String)waitForThreadResult(futureResult);
         if ( null != result )
         {
            softMgrVersion = result;
         }
         exec.shutdown();

         appManagerRead.setVersion(softMgrVersion);
      }
   }

   public List<String> getAllAppManagerTypes() {
      String[] types = null;
      String appmanagerTypes =
            Configuration.getStrings(appmanagerTypesKey, "");
      if (appmanagerTypes != null) {
         types = appmanagerTypes.split(",");
      }
      return Arrays.asList(types);
   }

   /* (non-Javadoc)
    * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
    */
   @Override
   public void afterPropertiesSet() throws Exception {
      this.loadSoftwareManagers();
   }

   public synchronized void deleteSoftwareManager(String appManagerName) {
      logger.debug("delete app manager " + appManagerName);
      if (Constants.IRONFAN.equals(appManagerName)) {
         logger.error("Cannot delete default application manager.");
         throw SoftwareManagerCollectorException.CAN_NOT_DELETE_DEFAULT();
      }
      appManagerService.deleteAppManager(appManagerName);
      logger.debug("successfully deleted app manager " + appManagerName);
      cache.remove(appManagerName);
      logger.debug("app manager " + appManagerName + " removed from cache");
   }

   public synchronized void modifySoftwareManager(AppManagerAdd appManagerAdd) {
      logger.debug("modify app manager " + appManagerAdd);
      String name = appManagerAdd.getName();
      if (Constants.IRONFAN.equals(name)) {
         logger.error("Cannot delete default application manager.");
         throw SoftwareManagerCollectorException.CAN_NOT_MODIFY_DEFAULT();
      }
      AppManagerEntity appManager = appManagerService.findAppManagerByName(name);
      if (null == appManager) {
         logger.error("Cannot find app manager " + name);
         throw SoftwareManagerCollectorException.APPMANAGER_NOT_FOUND(name);
      }

      //validate url in appManagerAdd
      List<String> errorMsgs = new ArrayList<String>();
      if (!CommonUtil.validateUrl(appManagerAdd.getUrl(), errorMsgs)) {
         throw SoftwareManagerCollectorException.INVALID_URL(errorMsgs);
      }

      String sslCertificate = appManagerAdd.getSslCertificate();
      if (!CommonUtil.isBlank(sslCertificate)) {
         saveSslCertificate(sslCertificate);
      }

      logger.info("Load application manager using new properties " + appManagerAdd);
      SoftwareManager softwareManager = loadSoftwareManager(appManagerAdd);

      logger.info("Validate the new application manager");
      validateSoftwareManager(name, softwareManager);

      logger.info("Modify meta db");
      appManagerService.modifyAppManager(appManagerAdd);
      logger.info("Remove old application manager instance from cache");
      cache.remove(name);
      logger.info("Add new application manager instance into cache");
      cache.put(name, softwareManager);

      logger.debug("successfully modified app manager " + appManagerAdd);
   }

   private void saveSslCertificate(String certificate) {
      OutputStream out = null;
      try {
         KeyStore keyStore = CommonUtil.loadAppMgrKeyStore();
         if (keyStore == null) {
            logger.error("Cannot read appmanager keystore.");
            return;
         }

         byte[] certBytes = Base64
               .decodeBase64(certificate
                     .replaceAll("-----BEGIN CERTIFICATE-----", "")
                     .replaceAll("-----END CERTIFICATE-----", "")
                     .getBytes());

         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         Collection c =
               cf.generateCertificates(new ByteArrayInputStream(certBytes));
         Certificate[] certs = new Certificate[c.toArray().length];

         if (c.size() == 1) {
            certs[0] =
                  cf.generateCertificate(new ByteArrayInputStream(certBytes));
         } else {
            certs = (Certificate[])c.toArray();
         }

         MessageDigest md5 = MessageDigest.getInstance("MD5");
         String md5Fingerprint = "";
         for (Certificate cert : certs) {
            md5.update(cert.getEncoded());
            md5Fingerprint = CommonUtil.toHexString(md5.digest());
            logger.debug("md5 finger print: " + md5Fingerprint);
            logger.debug("added cert: " + cert);
            keyStore.setCertificateEntry(md5Fingerprint, cert);
         }
         out =
               new FileOutputStream(Constants.APPMANAGER_KEYSTORE_PATH
                     + Constants.APPMANAGER_KEYSTORE_FILE);
         keyStore.store(out, Constants.APPMANAGER_KEYSTORE_PASSWORD);
      } catch (CertificateException e) {
         logger.info("Certificate exception: ", e);
      } catch (FileNotFoundException e) {
         logger.info("Cannot find file warning: ", e);
      } catch (NoSuchAlgorithmException e) {
         logger.info("SSL Algorithm error: ", e);
      } catch (IOException e) {
         logger.info("IOException - SSL Algorithm error: ", e);
      } catch (KeyStoreException e) {
         logger.info("Key store error: ", e);
      } finally {
         if (out != null) {
            try {
               out.close();
            } catch (IOException e) {
               logger.warn("Output stream of appmanagers.jks close failed.");
            }
         }
      }
   }
}
