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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
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
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.manager.i18n.Messages;
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

   protected static String configurationPrefix = "appmanager.factoryclass.";
   private static String appmanagerTypesKey = "appmanager.types";

   private static String appmgrConnTimeOutKey = "appmanager.connect.timeout.seconds";
   
   // the value of wait time to connect to application manager, with default 30 seconds
   private static int waitTimeForAppMgrConn = Configuration.getInt(appmgrConnTimeOutKey, Constants.APPMGR_CONNECT_TIMEOUT_SECONDS);

   public void setAppManagerService(IAppManagerService appManagerService) {
      this.appManagerService = appManagerService;
   }

   protected int getCacheSize() {
      return cache.size();
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }
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

      logger.info("Check AppManager already exist: " + appManagerAdd.getName());

      if (appManagerService.findAppManagerByName(appManagerAdd.getName()) != null) {
         logger.error("Name " + appManagerAdd.getName() + " already exists.");
         throw SoftwareManagerCollectorException.DUPLICATE_NAME(appManagerAdd.getName());
      }

      String sslCertificate = appManagerAdd.getSslCertificate();
      if (!CommonUtil.isBlank(sslCertificate)) {
         saveAppMgrCertificate(sslCertificate);
      }

      loadSoftwareManager(appManagerAdd);

      // add to meta-db through AppManagerService
      logger.info("Add app manager to meta-db.");

      try {
         appManagerService.addAppManager(appManagerAdd);
      } catch (SoftwareManagerCollectorException ex) {
         cache.remove(appManagerAdd.getName());
         throw ex;
      }
      catch (Exception ex) {
         cache.remove(appManagerAdd.getName());
         throw BddException.wrapIfNeeded(ex, Messages.getString("SW_MGR_COLLECTOR.FAILED_WRITE_META_DB"));
      }
   }


   private String getPrivateKey() {
      if (privateKey == null) {
         privateKey = loadPrivateKey(Constants.SERENGETI_PRIVATE_KEY_FILE);
      }
      return privateKey;
   }

   protected static String loadPrivateKey(String path) {
      try {
         return CommonUtil.dataFromFile(path);
      } catch (IOException e) {
         String errMsg = String.format(
               Messages.getString("SW_MGR_COLLECTOR.FAIL_READ_PRI_KEY"), path);
         logger.error(errMsg, e);

         //should be an internal exception instead of an external one. lixl
         throw new SWMgrCollectorInternalException(e,errMsg);
//            throw SoftwareManagerCollectorException.PRIVATE_KEY_READ_ERROR(e,Constants.SERENGETI_PRIVATE_KEY_FILE);
      }
   }

   protected void setPrivateKey(String value) {
      privateKey = value;
   }

   /**
    *
    * @param appManagerEntity
    * @return
    */
   private SoftwareManager loadSoftwareManager(AppManagerEntity appManagerEntity) {
      if (appManagerEntity.getName().equals(Constants.IRONFAN)) {
         SoftwareManager ironfanSoftwareManager = new DefaultSoftwareManagerImpl();
         cache.put(Constants.IRONFAN, ironfanSoftwareManager);
         return ironfanSoftwareManager;
      } else {
         // Do not block initialization in case of Exception
         return loadSoftwareManager(toAppManagerAdd(appManagerEntity));
      }
   }

   /**
    * wrap cache hit, instantiate, connection check and cache add together to simplify currency issue
    * @param appManagerAdd
    * @return
    */
   protected synchronized SoftwareManager loadSoftwareManager(AppManagerAdd appManagerAdd) {
      // we need to check the server connection each time we get a application manager, otherwise
      // it will cause a lot of trouble later when accessing it.
      String appMgrType = appManagerAdd.getType();
      String name = appManagerAdd.getName();
      if ( !appMgrType.equals(Constants.IRONFAN) ) {
         // check the server connection before do the real connection to the application manager.
         // this is to avoid long time waiting of socket connect when the server is shutdown or
         // even does not exist at all.
         checkServerConnection( name, appManagerAdd.getUrl() );
      }

      if (cache.containsKey(appManagerAdd.getName())) {
         return cache.get(appManagerAdd);
      }

      String factoryClassName = Configuration.getString(configurationPrefix + appManagerAdd.getType());
      if (CommonUtil.isBlank(factoryClassName)) {
         String errMsg = String.format(Messages.getString("SW_MGR_COLLECTOR.APP_MGR_FACTORY_UNDEFINED"), appManagerAdd.getType());

         logger.error(errMsg);
         throw new SWMgrCollectorInternalException(null, errMsg);
         //should be internal exception, lixl
//         throw SoftwareManagerCollectorException.CLASS_NOT_DEFINED(appManagerAdd.getType());
      }
      logger.info("Factory class name is " + factoryClassName);

      SoftwareManagerFactory softwareManagerFactory = null;
      try {
         Class<? extends SoftwareManagerFactory> clazz = ReflectionUtils.getClass(factoryClassName, SoftwareManagerFactory.class);
         logger.info("Factory class loaded.");
         softwareManagerFactory = ReflectionUtils.newInstance(clazz);
      } catch (Exception e) {
         String errMsg = String.format(Messages.getString("SW_MGR_COLLECTOR.CANNT_INSTANTIATE_APP_MGR_FACTORY"), factoryClassName);
         logger.error(errMsg, e);
         throw new SWMgrCollectorInternalException(e, errMsg);
//         throw SoftwareManagerCollectorException.CAN_NOT_INSTANTIATE(e, factoryClassName);
      }

      logger.info("Start to invoke application manager factory to create application manager.");
      SoftwareManager softwareManager = null;

      try {
         softwareManager =
               softwareManagerFactory.getSoftwareManager(appManagerAdd.getUrl(), appManagerAdd
                     .getUsername(), appManagerAdd.getPassword().toCharArray(),
                     getPrivateKey());
      } catch (Exception ex) {
         //TODO the handling is not clear: was it a connection failure (external), or some instantiating error (internal). lixl
         logger.error("Create application manager failed: " + ex.getMessage(), ex);
         throw SoftwareManagerCollectorException.CONNECT_FAILURE(
               appManagerAdd.getName(), ExceptionUtils.getRootCauseMessage(ex));
      }

      validateSoftwareManager(appManagerAdd.getName(), softwareManager);

      logger.info("The appmgr " + appManagerAdd.getName() + " can be reached and will be created.");

      cache.put(appManagerAdd.getName(), softwareManager);

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
         if ( !softwareManager.echo() ) {
            logger.error("Application manager "
                  + name
                  + " status is unhealthy. Please check application manager console for more details.");
            throw SoftwareManagerCollectorException.ECHO_FAILURE(name);
         }
      } catch (SoftwareManagementPluginException e) {
         //TODO we won't catch anything here! consider to remove it, lixl
         logger.error("Cannot connect to application manager "
               + name + ", check the connection information.", e);
         throw SoftwareManagerCollectorException.CONNECT_FAILURE(name,
               e.getMessage());
      }
      validateSoftwareManagerVersion(softwareManager);
   }

   private void validateSoftwareManagerVersion(SoftwareManager softwareManager) throws SoftwareManagerCollectorException {
      softwareManager.validateServerVersion();
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

      AppManagerEntity appManagerEntity = appManagerService.findAppManagerByName(name);
      if (appManagerEntity == null) {
         logger.error("Cannot find app manager " + name);
         throw SoftwareManagerCollectorException.APPMANAGER_NOT_FOUND(name);
      } else {
         String appMgrType = appManagerEntity.getType();
         if ( !appMgrType.equals(Constants.IRONFAN) ) {
            // check the server connection before do the real connection to the application manager.
            // this is to avoid long time waiting of socket connect when the server is shutdown or
            // even does not exist at all.
            checkServerConnection( name, appManagerEntity.getUrl() );
         }

         if (cache.containsKey(name)) {
            return cache.get(name);
         }
         return loadSoftwareManager(appManagerEntity);
      }
   }

   /**
    * Get software manager instance by cluster name (do not call this api before
    * cluster information is written to meta-db
    *
    * @param name
    * @return null if cluster name does not have a corresponding software
    *         manager instance
    */
   public SoftwareManager getSoftwareManagerByClusterName(String name) {
      ClusterEntity clusterEntity = clusterEntityManager.findByName(name);
      if (clusterEntity == null) {
         logger.warn("Can't find cluster with name: " + name);
         return null;
      }
      return getSoftwareManager(clusterEntity.getAppManager());
   }

   public synchronized void loadSoftwareManagers() {

      boolean defaultMgrExists = false;
      List<AppManagerEntity> appManagers = appManagerService.findAll();
      for (AppManagerEntity appManager : appManagers) {
         if(!defaultMgrExists) {
            defaultMgrExists = Constants.IRONFAN.equals(appManager.getName());
         }
         // if any appmgr cannot be connected, we should not block the initialization, or
         // the serengeti-ws server will fail startup
         try {
            loadSoftwareManager(appManager);
         } catch (Exception e) {
            logger.error("One of the appliation manager cannot be loaded: " + appManager.getName(), e);
         }
      }

      if(!defaultMgrExists) {
         AppManagerAdd appManagerAdd = new AppManagerAdd();
         appManagerAdd.setName(Constants.IRONFAN);
         appManagerAdd.setDescription(Constants.IRONFAN_DESCRIPTION);
         appManagerAdd.setType(Constants.IRONFAN);
         appManagerAdd.setUrl("");
         appManagerAdd.setUsername("");
         appManagerAdd.setPassword("");
         appManagerAdd.setSslCertificate("");

         appManagerService.addAppManager(appManagerAdd);

         try {
            loadSoftwareManager(new AppManagerEntity(appManagerAdd));
         } catch (Exception e) {
            logger.error("One of the appliation manager cannot be loaded: " + appManagerAdd.getName(), e);
         }
      }
   }

   /**
    * @param appManager
    * @return
    */
   public static AppManagerAdd toAppManagerAdd(AppManagerEntity appManager) {
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
      for (AppManagerRead appManagerRead: appManagerReads) {
         updateManagedClusters(appManagerRead);
      }
      logger.debug("got all app managers");
      return appManagerReads;
   }

   public AppManagerRead getAppManagerRead(String appManagerName) {
      AppManagerRead appManagerRead =
            appManagerService.getAppManagerRead(appManagerName);
      if (appManagerRead == null) {
         logger.error("Cannot find app manager " + appManagerName);
         throw SoftwareManagerCollectorException.APPMANAGER_NOT_FOUND(appManagerName);
      } else {
         setAppManagerReadDynamicProperties(appManagerRead);
         return appManagerRead;
      }
   }

   private void updateManagedClusters(AppManagerRead appManagerRead) {
      appManagerRead.setManagedClusters(clusterEntityManager
            .findByAppManager(appManagerRead.getName()));
   }

   private void updateVersion(AppManagerRead appManagerRead) {
      String softMgrVersion = "UNKNOWN";

      final SoftwareManager softwareManager = this.getSoftwareManager(appManagerRead.getName());
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

      String result = (String)CommonUtil.waitForThreadResult(futureResult, waitTimeForAppMgrConn);
      if (null != result) {
         softMgrVersion = result;
      }
      exec.shutdown();

      appManagerRead.setVersion(softMgrVersion);
   }

   /**
    * @param appManagerRead
    */
   private void setAppManagerReadDynamicProperties(AppManagerRead appManagerRead) {
      updateManagedClusters(appManagerRead);
      updateVersion(appManagerRead);
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
         logger.error("Cannot modify default application manager.");
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
         saveAppMgrCertificate(sslCertificate);
      }

      logger.info("Load software manager using new properties " + appManagerAdd);
      cache.remove(name);
      loadSoftwareManager(appManagerAdd);

      logger.info("Modify meta db");
      appManagerService.modifyAppManager(appManagerAdd);

      logger.debug("successfully modified app manager " + appManagerAdd);
   }

   private void saveAppMgrCertificate(String certificate) {
      saveSslCertificate(certificate, Constants.APPMANAGER_KEYSTORE_PATH);
   }

   /**
    * TODO this method has to be reverted:
    * because if the target path is not accessible, it will load cert from the default keystore in java home,
    * but still try to write it to the non accessible path.
    * @param certificate
    * @param keyStorePath
    */
   protected static void saveSslCertificate(String certificate, String keyStorePath) {
      Certificate[] certs;
      //parse certificates
      try {
         if (CommonUtil.isBlank(certificate)) {
            throw SoftwareManagerCollectorException.BAD_CERT(null);
         }

         byte[] certBytes = Base64
               .decodeBase64(certificate
                     .replaceAll("-----BEGIN CERTIFICATE-----", "")
                     .replaceAll("-----END CERTIFICATE-----", "")
                     .getBytes());

         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         Collection c = cf.generateCertificates(new ByteArrayInputStream(certBytes));
         certs = new Certificate[c.toArray().length];

         if (c.size() == 0) {
            throw SoftwareManagerCollectorException.BAD_CERT(null);
         } else if (c.size() == 1) {
            certs[0] = cf.generateCertificate(new ByteArrayInputStream(certBytes));
         } else {
            certs = (Certificate[]) c.toArray();
         }
      } catch (CertificateException e){
         throw SoftwareManagerCollectorException.BAD_CERT(e);
      }

      //load & save keystore
      OutputStream out = null;
      try {
         KeyStore keyStore = CommonUtil.loadAppMgrKeyStore(keyStorePath);
         if (keyStore == null) {
            logger.error(Messages.getString("SW_MGR_COLLECTOR.CANNT_READ_KEYSTORE"));
            throw new SWMgrCollectorInternalException(Messages.getString("SW_MGR_COLLECTOR.CANNT_READ_KEYSTORE"));
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
         out = new FileOutputStream(keyStorePath + Constants.APPMANAGER_KEYSTORE_FILE);
         keyStore.store(new BufferedOutputStream(out), Constants.APPMANAGER_KEYSTORE_PASSWORD);
      }catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
         logger.error(Messages.getString("SW_MGR_COLLECTOR.FAIL_SAVE_CERT"), e);
         throw new SWMgrCollectorInternalException(e, Messages.getString("SW_MGR_COLLECTOR.FAIL_SAVE_CERT"));
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

   /**
   *
   * @param appMgrName
   * @param urlStr
   */
  private void checkServerConnection(String appMgrName, String urlStr) {
     URL url = null;
     try {
        url = new URL(urlStr);
     } catch (MalformedURLException e) {
        logger.error("Url parse error: " + e.getMessage());
        throw SoftwareManagerCollectorException.CONNECT_FAILURE(appMgrName,
              e.getMessage());
     }

     final String host = url.getHost();
     final int port = url.getPort();

     logger.info("Check the connection to the application manager.");
     boolean connectOK = CommonUtil.checkServerConnection(host, port, waitTimeForAppMgrConn);
     if ( !connectOK ) {
        logger.error("Cannot connect to application manager "
              + appMgrName + ", check the connection information.");
        throw SoftwareManagerCollectorException.CONNECT_FAILURE(appMgrName,
              "Failed to connect to the server.");
     }
  }

}
