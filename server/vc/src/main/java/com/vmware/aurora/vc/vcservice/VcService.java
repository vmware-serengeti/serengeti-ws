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

package com.vmware.aurora.vc.vcservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import com.vmware.bdd.security.tls.TlsClientConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.security.CmsKeyStore;
import com.vmware.aurora.security.ThumbprintTrustManager;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcservice.VcService.MyThreadPoolExecutor.MyBlockingQueue;
import com.vmware.vim.binding.impl.vim.DescriptionImpl;
import com.vmware.vim.binding.impl.vim.ExtensionImpl;
import com.vmware.vim.binding.impl.vim.KeyValueImpl;
import com.vmware.vim.binding.impl.vim.ext.ExtendedProductInfoImpl;
import com.vmware.vim.binding.impl.vim.ext.ManagedEntityInfoImpl;
import com.vmware.vim.binding.impl.vim.ext.SolutionManagerInfoImpl;
import com.vmware.vim.binding.vim.*;
import com.vmware.vim.binding.vim.AuthorizationManager.Role;
import com.vmware.vim.binding.vim.Extension.PrivilegeInfo;
import com.vmware.vim.binding.vim.alarm.AlarmManager;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.Event.EventSeverity;
import com.vmware.vim.binding.vim.ext.ExtendedProductInfo;
import com.vmware.vim.binding.vim.ext.ManagedEntityInfo;
import com.vmware.vim.binding.vim.ext.SolutionManagerInfo;
import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vim.version.version10;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.query.PropertyCollector;
import com.vmware.vim.vmomi.client.Client;
import com.vmware.vim.vmomi.client.http.HttpClientConfiguration;
import com.vmware.vim.vmomi.client.http.HttpConfiguration;
import com.vmware.vim.vmomi.client.http.ThumbprintVerifier;
import com.vmware.vim.vmomi.client.http.impl.HttpConfigurationImpl;
import com.vmware.vim.vmomi.core.exception.CertificateValidationException;
import com.vmware.vim.vmomi.core.exception.InternalException;
import com.vmware.vim.vmomi.core.types.VmodlTypeMap;

/**
 * <code>VcService</code> maintains connection and session with VC server.
 */
public class VcService {
   private static final Logger logger = Logger.getLogger(VcService.class);
   private static final String SERENGETI_EXTENSION_REGISTERED = "serengeti.extension.registered";

   private static final Class<?> version = version10.class;
   private static final int SESSION_TIME_OUT = Configuration.getInt(
         "vc.session_time_out", 120000);
   private static final String SERENGETI_PRIVILEGE_GROUP_NAME = "Serengeti";
   private static final String SERENGETI_PRIVILEGE_ID = "Serengeti.access";
   private static final String SERENGETI_PERMISSION_ROLE = "BDE users";
   /*
    * The following fields are VC login info, initialized once.
    */
   static private boolean configured = false;
   static private boolean vcExtensionRegistered = false;
   static private String vcHost;
   static private int vcPort;
   static private String evsURL;
   static private String evsToken;
   static private String vcThumbprint;
   static private String extKey;
   static private String userName;
   static private String password;
   static private String locale;

   private final String serviceName;  // Internally used session name.
   private final boolean useExecutor; // Whether this VcService uses ThreadPoolExecutor.
   private final int timeoutMillis;   // HTTP timeout

   private ThumbprintVerifier getThumbprintVerifier() {
      return new ThumbprintVerifier() {
         @Override
         public Result verify(String thumbprint) {
            if (thumbprint.equalsIgnoreCase(vcThumbprint)) {
               return Result.MATCH;
            } else {
               return Result.MISMATCH;
            }
         }

         @Override
         public void onSuccess(X509Certificate[] chain, String thumbprint,
               Result verifyResult, boolean trustedChain,
               boolean verifiedAssertions) throws SSLException {
         }
      };
   }

   /**
    * XXX This class is created to debug PR 848988.
    * @author mchen
    */
   static class MyThreadPoolExecutor extends ThreadPoolExecutor {
      static class MyRejectHandler implements RejectedExecutionHandler {
         public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            RejectedExecutionException e = new RejectedExecutionException();
            logger.info("rejecting runnable " + r, e);
            logger.info((executor.isShutdown()? " shutdown " : " running ") +
               ",tasks=" + executor.getTaskCount() +
               ",poolSize=" + executor.getPoolSize() +
               ",maxPoolSize=" + executor.getMaximumPoolSize() +
               ",active=" + executor.getActiveCount() +
               ",queue=" + executor.getQueue() +
               ",qSize=" + executor.getQueue().size() +
               ",qCapacity=" + executor.getQueue().remainingCapacity());
            throw e;
         }
         static public MyRejectHandler getInstance() {
            return new MyRejectHandler();
         }
      }

      @SuppressWarnings("serial")
      static class MyBlockingQueue<E> extends LinkedBlockingQueue<E> {
         public MyBlockingQueue() {
            super();
         }
         @Override
         public boolean offer(E e) {
            int oldSize = size();
            int oldRemainingCapacity = remainingCapacity();
            boolean retVal = super.offer(e);
            if (!retVal) {
               logger.info("offer failed: size=" + size() +
                     ",capacity=" + remainingCapacity() +
                     ",oldSize=" + oldSize +
                     ",oldRemainingCapacity=" + oldRemainingCapacity);
            }
            return retVal;
         }
      }

      public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, MyBlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory) {
         super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            threadFactory, MyRejectHandler.getInstance());
      }
   }

   /*
    *  References to VC service objects.
    */
   private class ServiceContents {
      private final long genCount; // generation count of this service instance
      private Client vmomiClient;
      private ThreadPoolExecutor executor;
      private HttpConfiguration httpConfig;
      private ServiceInstance instance = null;
      private ServiceInstanceContent instanceContent = null;
      private SessionManager sessionManager = null;
      private FileManager fileManager = null;
      private VirtualDiskManager vmdkManager = null;
      private OvfManager ovfManager = null;
      private PerformanceManager perfManager = null;
      private TaskManager taskManager = null;
      private OptionManager optionManager = null;
      private PropertyCollector propertyCollector = null;
      private ExtensionManager extensionManager = null;
      private AlarmManager alarmManager = null;
      private AuthorizationManager authorizationManager = null;

      /*
       * A map that caches all managed object proxy objects.
       */
      private final Map<ManagedObjectReference, ManagedObject> moMap =
         new HashMap<ManagedObjectReference, ManagedObject>();

      /*
       * Login VC session and get service objects.
       */
      private ServiceContents(long genCount) throws Exception
      {
         this.genCount = genCount;
         long startNanos = System.nanoTime();
         String sessionTicket = loginAndGetSessionTicket();
         ManagedObjectReference svcRef = new ManagedObjectReference();
         boolean done = false;
         svcRef.setType("ServiceInstance");
         svcRef.setValue("ServiceInstance");
         try {
            initVmomiClient();
            instance = vmomiClient.createStub(ServiceInstance.class, svcRef);

            /*
             * Establish session.
             */
            instanceContent = instance.retrieveContent();
            sessionManager = vmomiClient.createStub(SessionManager.class,
                  instanceContent.getSessionManager());
            /*
             * login for the user
             */
            if (sessionTicket != null) {
               try {
                  sessionManager.loginBySessionTicket(sessionTicket);
               } catch (Exception e) {
                  logger.error("failed to use VC session ticket", e);
                  throw e;
               }
            } else if (userName != null) {
               logger.info("try to login to VC using username and password");
               sessionManager.login(userName, password, locale);
            } else {
               throw VcException.LOGIN_ERROR();
            }

            fileManager = getManagedObject(instanceContent.getFileManager());
            vmdkManager = getManagedObject(instanceContent.getVirtualDiskManager());
            taskManager = getManagedObject(instanceContent.getTaskManager());
            ovfManager = getManagedObject(instanceContent.getOvfManager());
            perfManager = getManagedObject(instanceContent.getPerfManager());
            optionManager = getManagedObject(instanceContent.getSetting());
            propertyCollector = getManagedObject(instanceContent.getPropertyCollector());
            extensionManager = getManagedObject(instanceContent.getExtensionManager());
            alarmManager = getManagedObject(instanceContent.getAlarmManager());
            authorizationManager = getManagedObject(instanceContent.getAuthorizationManager());

            logger.info("VC login on behalf of {" + Thread.currentThread().getName() +
                  ":" + serviceName + "{" + genCount + "}} " +
                  TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos) + "ms");
            done = true;
         } finally {
            // Clean up if we cannot proceed.
            if (!done) {
               cleanup();
            }
         }
      }

      /**
       * Initialize a new vmomiClient, including all required resources, thread
       * pool, etc. Thread names will include service name, generation count and
       * a thread number in the pool as in "Thread[vcService{2}-1]".
       * @throws Exception
       */
      private void initVmomiClient() throws URISyntaxException {
         String threadNamePrefix = serviceName + "{" + genCount + "}-";
         CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(threadNamePrefix);
         threadFactory.setDaemon(true);
         if (useExecutor) {
            executor = new MyThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS,
                              new MyBlockingQueue<Runnable>(), threadFactory);
         } else {
            executor = null;
         }
         URI uri;
         String serviceUrl = getServiceUrl();
         try {
            uri = new URI(serviceUrl);
         } catch (URISyntaxException e) {
            logger.error("Bad VC URL " + serviceUrl + e);
            AuAssert.check(false);
            throw e;
         }
         httpConfig = new HttpConfigurationImpl();
         httpConfig.setTimeoutMs(timeoutMillis);
         httpConfig.setThumbprintVerifier(getThumbprintVerifier());

         if (useExecutor) {
            vmomiClient = Client.Factory.createClient(uri, version, executor, httpConfig);
         } else {
            HttpClientConfiguration clientConfig = HttpClientConfiguration.Factory.newInstance();
            clientConfig.setHttpConfiguration(httpConfig);
            vmomiClient = Client.Factory.createClient(uri, version, clientConfig);
         }
      }

      /*
       * Get & insert a managed object proxy from moMap cache.
       */
      private synchronized <T extends ManagedObject> T
      getManagedObjectInt(ManagedObjectReference moRef) {
         @SuppressWarnings("unchecked")
         T obj = (T)moMap.get(moRef);
         if (obj != null) {
            return obj;
         }
         // The VmodlTypeMap.getVmodlType() is applicable for any vmodl type and is
         // not specific to ManagedObject. We need to cast to Class<T> because VLSI
         // core has changed the interface.
         @SuppressWarnings({ "unchecked"})
         Class<T> clazz = (Class<T>) VmodlTypeMap.Factory.getTypeMap()
                  .getVmodlType(moRef.getType()).getTypeClass();
         obj = vmomiClient.<T>createStub(clazz, moRef);
         moMap.put(moRef, obj);
         return obj;
      }

      /*
       * Get a managed object proxy from moMap cache.
       */
      private <T extends ManagedObject> T
      getManagedObject(ManagedObjectReference moRef) {
         @SuppressWarnings("unchecked")
         T obj = (T)moMap.get(moRef);
         if (obj != null) {
            return obj;
         }
         return this.<T>getManagedObjectInt(moRef);
      }

      /**
       * Logout out of vc and tear down all consumed resources.
       */
      private void cleanup() {
         moMap.clear();
         VcContext.getVcCleaner().logout(serviceName, vmomiClient, sessionManager,
               executor, httpConfig);
      }

      public int getInstanceId() {
         OptionValue[] options = optionManager.getSetting();
         for (OptionValue option : options) {
            if (option.getKey().equals("instance.id")) {
               return (Integer)option.getValue();
            }
         }
         throw VcException.SETTING_ERROR();
      }

      void dumpStatus() {
         logger.info(serviceName + "{" + genCount + "}:" +
               (executor.isShutdown()? " shutdown " : " running ") +
               "tasks=" + executor.getTaskCount() +
               ",qSize=" + executor.getQueue().size() +
               ",qCapacity=" + executor.getQueue().remainingCapacity());
      }
   }

   /*
    * Because VC sessions are shared by multiple threads,
    * we use a generation counter to keep track of the current
    * login-session to prevent multiple logout calls on
    * the same session. There will be exactly one VC session manager
    * logout and clearing of service contents per VC login.
    */
   private volatile long curGenCount = 0;

   /*
    *  VC connection objects. If a connection failed or was closed,
    *  always establish new instances.
    */
   ServiceContents service = null;

   private void setService(ServiceContents service, VcConnectionStatusChangeEvent eventType) {
      AuAssert.check(Thread.holdsLock(this));
      /*
       * Always log an event for an initial attempt: both VcContext is
       * initialized for the first time and this session gets its first life.
       * Otherwise, log an an event only if a service state change has been
       * detected to avoid event storms when VC is persistently down.
       */
      if (eventType != null &&
          ((VcContext.getGenCount() == 1 && curGenCount == 1) ||
           this.service != service)) {
         VcContext.triggerEvent(eventType, serviceName);
      }
      this.service = service;
   }

   private static void initVcConfig() {
      /*
       *  The following configs should be set in
       *  aurora-cms-transient.properties by evs_init.py
       *  on every CMS boot.
       */
      vcHost = Configuration.getString("vim.host");
      vcPort = Configuration.getInt("vim.port", 443);
      evsURL = Configuration.getString("vim.evs_url");
      evsToken = Configuration.getString("vim.evs_token");
      vcThumbprint = Configuration.getString("vim.thumbprint", null);

      /*
       * Extension key is based on CMS instance identifier.
       */
      extKey = "com.vmware.aurora.vcext.instance-" + Configuration.getCmsInstanceId();

      // represent if extension service is already registered
      vcExtensionRegistered = Configuration.getBoolean(SERENGETI_EXTENSION_REGISTERED, false);
      /*
       * The following are not set in config files by default.
       * They can be hard coded manually.
       */
      userName = Configuration.getString("vim.username", null);
      password = Configuration.getString("vim.password", null);
      locale = Configuration.getString("vim.locale", "en");

      configured = true;
   }

   /**
    * Create a VC service instance. The following is done:
    *  - Load the VLSI vmodl context.
    *  - Create a VC client object with VLSI with VC URL,
    *    HTTP configurations and a thread pool.
    *
    *  After initialization, VC session will be established.
    *
    * @param serviceName internal name
    */
   public VcService(String serviceName, boolean useExecutor, int timeoutMillis)
   throws VcException {
      if (!configured) {
         initVcConfig();
      }

      this.serviceName = serviceName;
      this.useExecutor = useExecutor;
      this.timeoutMillis = timeoutMillis;

      synchronized(this) {
         // Connect to VC.
         try {
            initVcSession();
         } catch (Exception e) {
            logger.error("Cannot establish session to VC " +
                  vcHost + ":" + vcPort + " on startup,", e);
         }
      }
   }

   public VcService(String serviceName) {
      this(serviceName, true, SESSION_TIME_OUT);
   }

   public String getServiceName() {
      return serviceName;
   }

   public String getServiceUrl() {
      return "https://" + vcHost + ":" + vcPort + "/sdk";
   }

   public String getClientSessionId() {
      return getServiceContents().vmomiClient.getBinding().getSession().getId();
   }

   /**
    * Returns true if a connection to VC has already been established
    */
   public boolean isConnected() {
      return service != null;
   }


   /*
    * Login once using VC extension key (stored in cms keystore)
    * and retrieve the session ticket.
    */
   private String loginAndGetSessionTicket() {
      URI sdkUri = null;
      if (!vcExtensionRegistered) {
         return null;
      }
      try {
         sdkUri = new URI("https://sdkTunnel:8089/sdk/vimService");
      } catch (URISyntaxException e) {
         logger.error(e);
         return null;
      }
      HttpConfigurationImpl httpConfig = new HttpConfigurationImpl();
      httpConfig.setTimeoutMs(SESSION_TIME_OUT);
      httpConfig.setKeyStore(CmsKeyStore.getKeyStore());
      httpConfig.setDefaultProxy(vcHost, Configuration.getInt("vim.port.http", 80), "http");
      httpConfig.getKeyStoreConfig().setKeyAlias(CmsKeyStore.VC_EXT_KEY);
      httpConfig.getKeyStoreConfig().setKeyPassword(CmsKeyStore.getVCExtPassword());
      httpConfig.setThumbprintVerifier(getThumbprintVerifier());

      TlsClientConfiguration tlsClientConfiguration = new TlsClientConfiguration();
      httpConfig.setEnabledProtocols(tlsClientConfiguration.getSslProtocols());
      HttpClientConfiguration clientConfig = HttpClientConfiguration.Factory.newInstance();
      clientConfig.setHttpConfiguration(httpConfig);
      Client client = Client.Factory.createClient(sdkUri, version, clientConfig);
      SessionManager sm = null;
      try {
         ManagedObjectReference svcRef = new ManagedObjectReference();
         svcRef.setType("ServiceInstance");
         svcRef.setValue("ServiceInstance");
         ServiceInstance si = client.createStub(ServiceInstance.class, svcRef);
         sm = client.createStub(SessionManager.class,
               si.getContent().getSessionManager());
         sm.loginExtensionByCertificate(extKey, "en");
         String ticket = sm.acquireSessionTicket(null);
         logger.info("got session ticket using extension");
         return ticket;
      } catch (Exception e) {
         logger.error("failed to get session ticket using extension", e);
         return null;
      } finally {
         VcContext.getVcCleaner().logout("VcExtensionLogin", client, sm,
               null, httpConfig);
      }
   }

   /**
    * Initializes the VC session. Each session gets a separate vmomi client
    * (http session cookie is kept in vmomi client).
    *
    * Result:
    *   _serviceInstance & other fields initialized if no exception is thrown.
    */
   private void initVcSession() throws VcException {
      try {
         /*
          * Make sure our VC extension has been registered.
          */
         boolean justRegistered = false;
         if (!vcExtensionRegistered) {
            registerExtensionVService();
            justRegistered = true;
         }
         if (vcExtensionRegistered) {
            String ticket = null;
            try {
               ticket = loginAndGetSessionTicket();
            } catch (Exception e) {
               logger.debug("Got exception during login");
            }
            if (ticket == null) {
               logger.info("Failed to login using certificate, try to regist extension once again");
               vcExtensionRegistered = false;
            }
         }
         if (!vcExtensionRegistered) {
            registerExtensionVService();
            justRegistered = true;
         }
         /*
          * attach to the new VC session
          */
         setService(new ServiceContents(++curGenCount), VcConnectionStatusChangeEvent.VC_SESSION_CREATED);
         /* Context callback to communicate session reset. */
         VcContext.serviceReset(this);
         /*
          * Now that we have a valid VMOMI connection, set properties for our extension
          */
         if (vcExtensionRegistered && justRegistered) {
            configureExtensionVService();
         }
      } catch (Exception ex) {
         if (service != null) {
            service.cleanup();
         }
         setService(null, VcConnectionStatusChangeEvent.VC_SESSION_CREATION_FAILURE);
         if (ex instanceof VcException) {
            throw (VcException)ex;
         } else if (ex instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException e = (UndeclaredThrowableException)ex;
            if (e.getUndeclaredThrowable() instanceof CertificateValidationException) {
               throw VcException.LOGIN_ERROR(e.getUndeclaredThrowable());
            }
         } else if (ex instanceof InternalException) {
            InternalException e = (InternalException)ex;
            if (e.getCause() instanceof CertificateValidationException) {
               throw VcException.LOGIN_ERROR(e.getCause());
            }
         }
         throw VcException.LOGIN_ERROR(ex);
      }
   }

   /**
    * Returns a PEM representation of a certificate
    *
    * @param cert A non-null certificate
    * @return The PEM representation
    * @throws CertificateEncodingException, if certificate is malformed
    */
   private static String CertificateToPem(Certificate cert) throws CertificateEncodingException {
      byte[] base64 = Base64.encodeBase64Chunked(cert.getEncoded());
      StringBuffer sb = new StringBuffer();
      sb.append("-----BEGIN CERTIFICATE-----\n");
      sb.append(new String(base64));
      sb.append("-----END CERTIFICATE-----");
      return sb.toString();
   }

   /**
    * This sends a URL POST request to the Extension vService guest API to
    * register a new extension. Upon success, set vcExtensionRegistered to true.
    * Note that the extension will not be fully configured until we log in to VC
    * as this extension and make some VMODL calls to finish the job.
    *
    * Note also that we only need to do this once per CMS install, not once per
    * CMS startup, but it doesn't seem to hurt to do it every time.
    *
    * @synchronized for preventing concurrent call to register EVS.
    */
   private static synchronized void registerExtensionVService() {
      if (vcExtensionRegistered) {
         return;
      }
      logger.debug("Register extension vService at: " + evsURL + " token=" + evsToken);
      Writer output = null;
      BufferedReader input = null;
      try {
         /**
          *  Initialize our own trust manager
          */
         ThumbprintTrustManager thumbprintTrustManager = new ThumbprintTrustManager();
         thumbprintTrustManager.add(vcThumbprint);

         TrustManager[] trustManagers = new TrustManager[]{thumbprintTrustManager};

         HttpClient httpClient = new HttpClient();

         TlsSocketFactory tlsSocketFactory = new TlsSocketFactory(trustManagers);

         Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory)tlsSocketFactory, 443));

         PostMethod method = new PostMethod(evsURL);
         method.setRequestHeader("evs-token", evsToken);

         Certificate cert = CmsKeyStore.getCertificate(CmsKeyStore.VC_EXT_KEY);
         String evsSchema = "http://www.vmware.com/schema/vservice/ExtensionVService";
         String payload =
               "<RegisterExtension xmlns=\"" + evsSchema + "\">\n" +
                     "  <Key>" + extKey + "</Key>\n" +
                     "  <Certificate>\n" +
                     CertificateToPem(cert) + "\n" +
                     "  </Certificate>\n" +
                     "</RegisterExtension>\n";

         RequestEntity requestEntity = new StringRequestEntity(payload, "text/plain", "UTF-8");
         method.setRequestEntity(requestEntity);
         int statusCode = httpClient.executeMethod(method);

         logger.info("status code: " + statusCode);
         for (Header e: method.getResponseHeaders()) {
               logger.debug("Response Header: " + e.getName() + " :" + e.getValue());
         }

         input = new BufferedReader(
               new InputStreamReader(method.getResponseBodyAsStream()));
         for (String str = input.readLine(); str != null; str = input.readLine()) {
            logger.debug("Response: " + str);
         }

         if(statusCode == 200) {
            vcExtensionRegistered = true;
            logger.info("Extension registration request sent successfully");
         } else {
            logger.error("Extension registration request sent error");
         }
      } catch (Exception e) {
         logger.error("Failed Extension registration to " + evsURL, e);
      } finally {
         Configuration.setBoolean(SERENGETI_EXTENSION_REGISTERED, true);
         Configuration.save();
         if (output != null) {
            try {
               output.close();
            } catch (IOException e) {
               logger.error("Failed to close output Writer", e);
            }
         }
         if (input != null) {
            try {
               input.close();
            } catch (IOException e) {
               logger.error("Failed to close input Reader", e);
            }
         }
      }
   }

   private void configureExtensionVService() throws Exception {
      ExtensionManager em = service.extensionManager;


      Extension us = em.findExtension(extKey);
      AuAssert.check(us != null);

      // Describe Aurora itself
      Description desc = new DescriptionImpl();
      desc.setLabel("VMware Serengeti Management Server");
      desc.setSummary("VMware Serengeti Management Server, instance " + Configuration.getCmsInstanceId());
      us.setDescription(desc);
      us.setCompany("VMware, Inc.");
      us.setVersion(Configuration.getNonEmptyString("serengeti.version"));
      us.setShownInSolutionManager(true);
      ExtendedProductInfo extInfo = new ExtendedProductInfoImpl();
      extInfo.setCompanyUrl("http://www.vmware.com");
      us.setExtendedProductInfo(extInfo);
      // XXX: Set health info, any other fields?

      // Describe the entities we manage (DBVM)
      ManagedEntityInfo info = new ManagedEntityInfoImpl();
      info.setType("hadoop node");
      info.setDescription("VMware Serengeti - Node Template");
      //info.setSmallIconUrl("https://*:443/some-16x16.png");
      ManagedEntityInfo[] infos = new ManagedEntityInfo[1];
      infos[0] = info;
      us.setManagedEntityInfo(infos);

      // Generate ResourceInfo
      Extension.ResourceInfo extensionResourceInfo = new ExtensionImpl.ResourceInfoImpl();
      extensionResourceInfo.setLocale("en");
      extensionResourceInfo.setModule("extension");

      KeyValue localizedExt[] = new KeyValue[6];
      localizedExt[0] = new KeyValueImpl();
      localizedExt[0].setKey(us.getKey() + ".label");
      localizedExt[0].setValue(us.getDescription().getLabel());

      localizedExt[1] = new KeyValueImpl();
      localizedExt[1].setKey(us.getKey() + ".summary");
      localizedExt[1].setValue(us.getDescription().getSummary());

      localizedExt[2] = new KeyValueImpl();
      localizedExt[2].setKey("privilege.Serengeti.label");
      localizedExt[2].setValue("Big Data Extensions");

      localizedExt[3] = new KeyValueImpl();
      localizedExt[3].setKey("privilege.Serengeti.summary");
      localizedExt[3].setValue("Big Data Extensions related privileges");

      localizedExt[4] = new KeyValueImpl();
      localizedExt[4].setKey("privilege.Serengeti.access.label");
      localizedExt[4].setValue("Access");

      localizedExt[5] = new KeyValueImpl();
      localizedExt[5].setKey("privilege.Serengeti.access.summary");
      localizedExt[5].setValue("Access Big Data Extensions");

      extensionResourceInfo.setData(localizedExt);

      // Generate event type specifications
      Extension.ResourceInfo eventResourceInfo = new ExtensionImpl.ResourceInfoImpl();
      eventResourceInfo.setLocale("en");
      eventResourceInfo.setModule("event");

      class KeyValueList extends ArrayList<KeyValue> {
         public void add(String key, String value) {
            KeyValue pair = new KeyValueImpl();
            pair.setKey(key);
            pair.setValue(value);
            super.add(pair);
         }
      };

      KeyValueList resourceInfo = new KeyValueList();
      ArrayList<Extension.EventTypeInfo> eventTypes = new ArrayList<Extension.EventTypeInfo>();

      for (EventSeverity severity : Event.EventSeverity.values()) {
         resourceInfo.add("com.vmware.vhadoop.vhm.vc.events."+severity.name()+".label", "BDE notification");
         resourceInfo.add("com.vmware.vhadoop.vhm.vc.events."+severity.name()+".summary", "BDE notification");
         resourceInfo.add("com.vmware.vhadoop.vhm.vc.events."+severity.name()+".category", severity.name());
         resourceInfo.add("com.vmware.vhadoop.vhm.vc.events."+severity.name()+".fullFormat", "{message}");
         resourceInfo.add("com.vmware.vhadoop.vhm.vc.events."+severity.name()+".formatOnVm", "BDE notification");

         Extension.EventTypeInfo event = new ExtensionImpl.EventTypeInfoImpl();
         event.setEventID("com.vmware.vhadoop.vhm.vc.events."+severity.name());
         event.setEventTypeSchema("<EventType><eventTypeID>com.vmware.vhadoop.vhm.vc.events."+severity.name()+"</eventTypeID><description>Status update for a Big Data Extensions compute VM</description><arguments/></EventType>");
         eventTypes.add(event);
      }

      eventResourceInfo.setData(resourceInfo.toArray(new KeyValue[0]));

      us.setResourceList(new Extension.ResourceInfo[] {extensionResourceInfo, eventResourceInfo});
      us.setEventList(eventTypes.toArray(new Extension.EventTypeInfo[0]));
      us.setShownInSolutionManager(true);

      SolutionManagerInfo sm = new SolutionManagerInfoImpl();
      sm.setSmallIconUrl("http://www.vmware.com");
      us.setSolutionManagerInfo(sm);
      //register a privilege for serengeti web client
      PrivilegeInfo loginBDE = new ExtensionImpl.PrivilegeInfoImpl();
      loginBDE.setPrivGroupName(SERENGETI_PRIVILEGE_GROUP_NAME);
      loginBDE.setPrivID(SERENGETI_PRIVILEGE_ID);
      Collection<PrivilegeInfo> privileges = new ArrayList<PrivilegeInfo>();
      privileges.add(loginBDE);
      us.setPrivilegeList(privileges.toArray(new PrivilegeInfo[0]));

      // Push this info into VC
      em.updateExtension(us);
      //register a serengeti role for serengeti web client users
      AuthorizationManager am = service.authorizationManager;
      Role[] roles = am.getRoleList();
      for(int i=0; i < roles.length; i++){
         if(SERENGETI_PERMISSION_ROLE.equals(roles[i].getName())){
            break;
         }
         if(i == roles.length-1){
            logger.info("add a serengeti role:"+SERENGETI_PERMISSION_ROLE);
            am.addRole(SERENGETI_PERMISSION_ROLE, new String[]{SERENGETI_PRIVILEGE_ID});
         }
      }
   }


   /**
    * Synchronized creation of new VC session.
    * @return the VC service object.
    * @throws Exception
    */
   private synchronized ServiceContents
   getServiceContentsLocked() throws VcException {
      if (!isConnected()) {
         initVcSession();
      }
      AuAssert.check(service != null);
      return service;
   }

   /*
    * If already logged into VC, do nothing.
    * Otherwise, try to instantiate a new {\ServiceContents} object
    * and start a new login session.
    *
    * This function speculatively fetches the session without locks
    * and fall back on synchronized method.
    */
   private ServiceContents getServiceContents() throws VcException {
      ServiceContents svc = service;
      if (svc == null) {
         return getServiceContentsLocked();
      }
      return svc;
   }

   /**
    * Dump debug info of the current service.
    */
   public void dumpServiceStatus() {
      ServiceContents svc = service;
      if (svc != null) {
         svc.dumpStatus();
      } else {
         logger.info("vcservice not connected last genCount=" + curGenCount);
      }
   }

   /**
    * Get the generation count of the current service context, if any.
    * @return Generation count, or null.
    */
   public Long getServiceGenCount() {
      ServiceContents svc = service;
      if (svc != null) {
         return svc.genCount;
      } else {
         return null;
      }
   }

   /**
    * Clean up of the current VC service session. Physical vc logout
    * is deferred to VcCleanup thread. This call cannot fail.
    * @param generation count to match the session.
    * @param forced True if we always logout regardless of generation count.
    */
   private synchronized void clearServiceContents(long generation,
         boolean forced, VcConnectionStatusChangeEvent eventType) {
      if (isConnected()) {
         if (forced || service.genCount == generation) {
            /* Report disconnect event before async logout to avoid event reorder. */
            service.cleanup();
            setService(null, eventType);
            AuAssert.check(!isConnected());
         }
      }
   }

   /**
    * Drop connection on the floor without cleaning up.
    * Used for TESTING ONLY to drop VC connection.
    */
   public synchronized void dropConnection() {
      try {
         if(isConnected()) {
            /* For testing, this is synchronous, no VcCleaner. */
            VcContext.triggerEvent(VcConnectionStatusChangeEvent.VC_SESSION_DISCONNECTED, serviceName);
            service.sessionManager.logout();
         }
      } catch (Exception e) {
         logger.info("Got exception trying to drop connection" + e);
      }
   }

   /**
    * Connect to VC if a session has not been established.
    * @return generation number for the VC session
    */
   public long login() throws VcException {
      return getServiceContents().genCount;
   }

   /**
    * Force logout of the current VC session.
    */
   public void logout() {
      clearServiceContents(-1, true, VcConnectionStatusChangeEvent.VC_SESSION_DISCONNECTED);
   }

   /**
    * Same as above, but allows to trigger a custom event. Used, for example,
    * during CMS shutdown.
    * @param eventType
    */
   public void logout(VcConnectionStatusChangeEvent eventType) {
      clearServiceContents(-1, true, eventType);
   }

   /**
    * Logs out the session from VC server iff the genCount of the
    * current VC service matches the login generation of the caller.
    * @param generation Last known VC login generation count by the caller.
    */
   public void logout(long generation) {
      clearServiceContents(generation, false, VcConnectionStatusChangeEvent.VC_SESSION_DISCONNECTED);
   }

   /**
    * Reestablish connection by logging in and out.
    * @param generation Last known VC login generation count
    * @return new VC login generation count
    * @throws VcException
    */
   public long reconnect(long generation) throws VcException {
      logout(generation);
      return login();
   }

   public ServiceInstance getServiceInstance() throws VcException {
      return getServiceContents().instance;
   }

   public ServiceInstanceContent getServiceInstanceContent() throws VcException {
      return getServiceContents().instanceContent;
   }

   public FileManager getFileManager() throws VcException {
      return getServiceContents().fileManager;
   }

   public VirtualDiskManager getVirtualDiskManager() throws VcException {
      return getServiceContents().vmdkManager;
   }

   public TaskManager getTaskManager() throws VcException {
      return getServiceContents().taskManager;
   }

   public OvfManager getOvfManager() throws VcException {
      return getServiceContents().ovfManager;
   }

   public PerformanceManager getPerfManager() throws VcException {
      return getServiceContents().perfManager;
   }

   public AlarmManager getAlarmManager() throws VcException {
      return getServiceContents().alarmManager;
   }

   public int getInstanceId() {
      return getServiceContents().getInstanceId();
   }

   /**
    * @return default property collector for this client.
    * @throws VcException
    */
   public PropertyCollector getPropertyCollector() throws VcException {
      return getServiceContents().propertyCollector;
   }

   /**
    * Given a <code>ManagedObjectReference</code> instance, uses it to fetch the
    * mapping <code>ManagedObject</code>
    *
    * @param moRef
    *           The <code>ManagedObjectReference</code> instance.
    * @return ManagedObject instance
    */
   public <T extends ManagedObject> T getManagedObject(ManagedObjectReference moRef) {
      return getServiceContents().<T>getManagedObject(moRef);
   }

   public String getExtensionKey() {
      return extKey;
   }

   public ExtensionManager getExtensionManager() {
      return getServiceContents().extensionManager;
   }

   public SessionManager getSessionManager() {
      return getServiceContents().sessionManager;
   }

   public String getLocale() {
      return locale;
   }

}
