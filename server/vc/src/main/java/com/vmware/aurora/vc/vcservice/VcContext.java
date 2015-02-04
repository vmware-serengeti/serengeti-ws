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

package com.vmware.aurora.vc.vcservice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcTask;
import com.vmware.aurora.vc.VcTaskMgr;
import com.vmware.aurora.vc.vcevent.VcEventListener;
import com.vmware.aurora.vc.vcservice.VcConnectionStatusChangeEvent.VcConnectionStatusChangeCallback;
import com.vmware.vim.binding.vim.AboutInfo;
import com.vmware.vim.binding.vim.fault.NotAuthenticated;
import com.vmware.vim.binding.vmodl.LocalizableMessage;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.RuntimeFault;
import com.vmware.vim.vmomi.core.exception.InternalException;
import com.vmware.vim.vmomi.core.exception.UnmarshallException;
import com.vmware.vim.vmomi.core.types.VmodlContext;

/**
 * Here is a general discussion on VC interface.
 *
 * VC MANAGED AND DATA OBJECTS
 *
 * There are two types of objects in VC: managed objects and data objects.
 *
 * Managed objects provide interfaces for performing queries and run
 * tasks on an VC object. These are API level interfaces that don't actually
 * contain data.
 *
 * Data objects contain values of VC objects at a given instance. It is
 * obtained through queries on managed objects. Data objects may nest
 * and accessing child objects do not require new VC queries.
 *
 * All managed objects related to Aurora are mapped to subclasses of
 * {@link VcObject}. Most VC operations are performed through methods of
 * these classes. One exception is {@link VcFileManager}, which contains
 * some services for file operations.
 *
 * {@link VcObject} contains snapshots of VC object data through
 * its member variables that point to VC data objects.
 * Once initialized, all data referenced by its member variables
 * can be accessed without new queries to VC. We (aurora) can
 * add access functions to get a sub-data-object or a field of it.
 *
 * Refer to comments in class {@link VcObject} for more info.
 *
 * VC SESSIONS
 *
 * In order to deal with problems with VC connection interruption
 * and errors, we enclose a section of code that accesses VC with
 * wrapper class so that the code can be retried if exceptions are
 * thrown due to VC connection errors. The section of code wrapped
 * in a {@link body()} function is refered to as VC session.
 * An abstract class {@link VcSession} defines the abstraction of
 * the session for execution in a VC context. Depending on different
 * VC operations, one or two of the following methods may be used
 * to program VC accesses.
 *
 * VC OPERATIONS
 *
 * In general, there are two types of VC operations.
 *
 * 1. Queries that only read VC states.
 *    These operations are stateless and can be repeated without
 *    side effects. We support having these queries running in
 *    a VcSession through {@link inVcSessionDo}. Such a session
 *    can be embedded in SagaSteps that run CMS transactions.
 *    Refer to {@link com.vmware.aurora.vc.test.CreateRbSaga.CreateRbStep}
 *    for code example.
 * 2. Queries that modifies VC states or run as VC tasks.
 *    These operations need to run in a Saga Step that extends
 *    {@link VcTaskSagaStep}. This Saga step cannot do CMS transactions
 *    in the execution. Refer to {@link VcTaskSagaStep} for usage
 *    and {@link com.vmware.aurora.vc.test.VmOpSaga} for code examples.
 *
 */

/**
 * {@link VcContext} class manages VC service life cycle.
 *
 * All accesses to VC should run from VC context.
 */
public class VcContext {
   private static Logger logger = Logger.getLogger(VcContext.class);

   /* vc connectivity requirements. */
   private final static int vcRequiredMajorVersion = 5; // Starting with VC 5.0.
   private final static String vcApiName = "VirtualCenter";
   private final static String esxApiName = "HostAgent";

   /*
    * The global VC service object representing a VC session, initialized
    * when system is connected to VC.
    */
   private static VcService vcService = null;

   /* A global VC session dedicated to Event Listener. */
   private static VcService eventService = null;
   private static VcEventListener eventListener = null;
   private static VcTaskMgr taskMgr = new VcTaskMgr();

   /* A global VC session dedicated to long running VC calls. */
   private static VcService longCallService = null;
   private static VcLongCallHandler longCallHandler = null;

   private static VcCleaner vcCleaner;
   private static String vcVersion;

   private static boolean initialized = false;    // Configured & initialized.
   private static boolean shutdown = false;       // Shutdown request received.
   private static boolean fatalInitError = false; // Retrying will not help.
   private static int genCount = 0;               // Init attempt genCount.
   private static Map<VcConnectionStatusChangeEvent, VcConnectionStatusChangeCallback> statusChangeEventCallback =
      new HashMap<VcConnectionStatusChangeEvent, VcConnectionStatusChangeCallback>();

   private static int eventCount = 0;

   /**
    * A unique identifier of the VC server, initialized once when vcService
    * is created.
    */
   private static String serverGuid = null;
   public static String getServerGuid() { return serverGuid; }

   /**
    * Return VC version if a connection is established.
    * @return vc version
    */
   public static String getVcVersion() {
      return vcVersion;
   }

   /**
    * Are we in the process of shutting down VcContext?
    * @return true if shutdown was requested
    */
   public static synchronized boolean isShuttingDown() {
      return shutdown;
   }

   /**
    * Report the connection state with vCenter.
    * @return true if connected with vCenter
    */
   public static boolean isServiceConnected() {
      VcService svc = vcService;
      return svc != null && svc.isConnected();
   }

   /**
    * Returns global event listener.
    * @return event listener
    */
   public static VcEventListener getEventListener() {
      return eventListener;
   }

   /**
    * @return VC long call handler
    */
   public static VcLongCallHandler getVcLongCallHandler() {
      return longCallHandler;
   }

   /**
    * This should only be called from {\link VcSession}.
    * @return global task manager.
    */
   public static VcTaskMgr getGlobalTaskMgr() {
      return taskMgr;
   }

   /**
    * A thread local variable to represent the current VC session.
    *
    *   In Aurora's terminology, a VC session is a chunk of code that
    *   accesses VC in a single thread. We try to repeat a VC session
    *   if the underlying code generates recoverable exceptions,
    *   such as VC connection failures.
    *
    *   For stateful VC operations that rely on vmodl's session,
    *   such as property collector, events collector, they shouldn't
    *   use VC session wrappers.
    */
   private static ThreadLocal<VcService> tVcService = new ThreadLocal<VcService>() ;
   private static ThreadLocal<VcSession<?>> tVcSession = new ThreadLocal<VcSession<?>>();

   /**
    * @return a TLS VC session object.
    * null if not accessed from a VC session.
    */
   public static VcSession<?> getSession() {
      return tVcSession.get();
   }

   /**
    * @return a TLS service object to access VC,
    * null if not accessed from a VC session.
    */
   public static VcService getService() {
      return tVcService.get();
   }

   /**
    * @return a TLS task manager object to issue tasks to VC,
    * null if not accessed from a VC task session.
    */
   public static VcTaskMgr getTaskMgr() {
      return getSession().getTaskMgr();
   }

   /**
    * @return VcCleaner a cleaning service for vc logout requests
    */
   public static VcCleaner getVcCleaner() {
      AuAssert.check(vcCleaner != null);
      return vcCleaner;
   }

   /**
    * @return true if the current thread in a VC session
    */
   public static boolean isInSession() {
      return tVcService.get() != null;
   }

   /**
    * @return true if the current thread in a VC task session
    */
   public static boolean isInTaskSession() {
      return isInSession() && getSession().isTaskSession();
   }

   /**
    * Returns the number of times the context was re-initialized.
    * @return genCount
    */
   public static int getGenCount() {
      return genCount;
   }

   /**
    * Triggers a VC connection status change event.
    * @param type               event type
    * @param serviceName        vc service name, maybe null
    */
   public static void triggerEvent(VcConnectionStatusChangeEvent event, String serviceName) {
      /*
       * XXX Because vc sessions can be established from various loops, make sure we
       * don't abuse the database too much. Delete once we have confidence.
       */
      if (++eventCount > 10000) {
         logger.error("Too many VC events");
         return;
      }
      VcConnectionStatusChangeCallback callback = statusChangeEventCallback.get(event);
      if (callback != null) {
         callback.call(event, serviceName);
      }
   }

   /**
    * Called if initVc fails for any reason: cleans up any partially initialized
    * state, logs event and throws an unchecked exception with given errorId.
    * @param e                  exception to re-throw
    * @param eventType          event to log, if any
    * @param fatalIniError      true for fatal errors (retry will not help)
    */
   private static void initFailure(VcException e, VcConnectionStatusChangeEvent eventType,
         boolean fatalInitError) {
      cleanup(eventType);
      VcContext.fatalInitError = fatalInitError;  // Dont't retry if true.
      AuAssert.check(!initialized);
      throw e;
   }

   private static void checkVcVersion(AboutInfo info) {
      if (esxApiName.equals(info.getApiType())) {
         initFailure(VcException.CONNECTING_TO_HOSTD(),
               VcConnectionStatusChangeEvent.VC_VERSION_HOSTD, true);
      } else if (!vcApiName.equals(info.getApiType())) {
         initFailure(VcException.CONNECTING_TO_INVALID_PRODUCT(),
               VcConnectionStatusChangeEvent.VC_VERSION_INVALID_PRODUCT, true);
      }
      String mVersionString;
      Integer mVersion = 0;
      try {
         mVersionString = info.getApiVersion().split("\\.")[0];
         mVersion = new Integer(mVersionString);
      } catch (NumberFormatException e) {
         initFailure(VcException.UNSUPPORTED_VERSION(e, info.getApiVersion()),
               VcConnectionStatusChangeEvent.VC_VERSION_UNSUPPORTED, true);
      }
      if (mVersion < vcRequiredMajorVersion) {
         initFailure(VcException.UNSUPPORTED_VERSION(null, info.getApiVersion()),
               VcConnectionStatusChangeEvent.VC_VERSION_UNSUPPORTED, true);
      }
   }

   /**
    * Initialize three global vc sessions and save them in the context:
    * - a general purpose vc session used by short lived requests
    * - an event listener session, a long running service
    * - an dedicated session for long running VC calls
    * Also initialize all other auxiliary data structures and state.
    * This function either completes the entire initialization successfully
    * or rolls back partial initialization on any failure. In the latter case,
    * we will attempt to re-initialize again at a later time on VC use.
    */
   public static synchronized void initVcContext() {
      AboutInfo info = null;
      genCount++;
      if (initialized) {
         logger.info("VC already initialized");
         return;
      }
      /*
       * Don't attempt to relogin into VC on fatal errors (will fail again) or
       * when we are in the middle of a shutdown operation (just logged out).
       */
      if (fatalInitError) {
         logger.warn("Fatal VC init error");
         throw VcException.INIT_ERROR();
      }
      if (shutdown) {
         logger.warn("initVcContext during shutdown");
         throw VcException.SHUTDOWN_ERROR();
      }
      /* No partial initialization tolerated. */
      AuAssert.check(vcCleaner == null          &&
             vcService == null          &&
             eventService == null);

      vcCleaner = new VcCleaner();

      try {
         VmodlContext.getContext();
      } catch(IllegalStateException ex) {
         //Context has not been initialized. Continue to initialize
         VmodlContext.initContext(new String[] { "com.vmware.vim.binding.vim" });
      }

      try {
         vcService = new VcService("VcService");
         info = vcService.getServiceInstanceContent().getAbout();
         serverGuid = info.getInstanceUuid();
         vcVersion = info.getVersion();
      } catch (Exception e) {
         logger.error("init vc context failed (vc session)");
         initFailure(VcException.LOGIN_ERROR(e), null, false); // Retry later.
      }
      checkVcVersion(info);

      try {
         eventService = new VcService("VcEventService");
         /*
          * XXX Since Aurora is currently happy with resource pools from different
          * datatcenters, we have to monitor the entire inventory starting with the
          * root folder. Will need to find a way to reduce this to either a set
          * of datacenters or, better, a single datacenter. Also see Bug 677287.
          */
         VcContext.startEventListener(vcService.getServiceInstanceContent().getRootFolder());
      } catch (Exception e) {
         logger.error("init vc context failed (vc event listener)");
         initFailure(VcException.LOGIN_ERROR(e), null, false); // Retry later.
      }

      try {
         /*
          * Set the HTTP timeout for long running VC calls to 5 minutes.
          */
         longCallService = new VcService("VcLongCallService", false, 5 * 60 * 1000);
         VcContext.startVcLongCallHandler();
      } catch (Exception e) {
         logger.error("init vc context failed (vc long call handler)");
         initFailure(VcException.LOGIN_ERROR(e), null, false); // Retry later.
      }

      logger.info("init vc context succeeded");
      initialized = true;
      triggerEvent(VcConnectionStatusChangeEvent.VC_CONNECTED, null);
   }

   public static void initVcContext(Map<VcConnectionStatusChangeEvent, VcConnectionStatusChangeCallback> callback) {
      VcContext.statusChangeEventCallback.putAll(callback);
      initVcContext();
   }

   /**
    * Shut down all VC services on system shutdown.
    */
   public static synchronized void shutdown() {
      logger.info("Shutting down VcContext");
      shutdown = true;
      cleanup(null);
      logger.info("VcContext shut down");
   }

   private static synchronized void cleanup(VcConnectionStatusChangeEvent eventType) {
      VcService svc = vcService;
      VcService eventSvc = eventService;
      VcService longTaskSvc = longCallService;

      if (eventType != null) {
         triggerEvent(eventType, null);
      }
      stopEventListener();
      stopVcLongCallHandler();
      eventService = null;
      longCallService = null;
      vcService = null;
      vcVersion = null;
      serverGuid = null;
      if (svc != null) {
         svc.logout(null);
      }
      if (eventSvc != null) {
         eventSvc.logout(null);
      }
      if (longTaskSvc != null) {
         longTaskSvc.logout(null);
      }
      if (vcCleaner != null) {
         vcCleaner.shutDown();  // Will wait for all logout completions.
         vcCleaner = null;
      }

      initialized = false;
   }

   // public for testing only
   public static synchronized void startSession(VcSession<?> vcSession) {
      if (shutdown) {
         throw VcException.SHUTDOWN_ERROR();
      }
      AuAssert.check(initialized && vcService != null);
      tVcSession.set(vcSession);
      tVcService.set(vcService);
   }

   // public for testing only
   public static void endSession() {
      tVcService.set(null);
      tVcSession.set(null);
   }

   /**
    * A special long running session used by Event Listener thread.
    * Does not need to be protected against shutdowns like the general
    * session because this gets executed only once, initiated by the
    * main thread that holds VcContext monitor.
    */
   public static void startEventSession() {
      AuAssert.check(eventService != null && !shutdown);
      tVcService.set(eventService);
      tVcSession.set(null);
   }

   /**
    * A special long running session used by VcLongCallHandler thread.
    * Does not need to be protected against shutdowns like the general
    * session because this gets executed only once, initiated by the
    * main thread that holds VcContext monitor.
    */
   public static void startLongCallSession() {
      AuAssert.check(longCallService != null && !shutdown);
      tVcService.set(longCallService);
      tVcSession.set(null);
   }

   private static void wrapAndThrow(Throwable e) throws AuroraException {
      if (e instanceof AuroraException) {
         throw (AuroraException)e;
      }
      throw VcException.GENERAL_ERROR(e);
   }

   /**
    * Wrapper function for a VC session.
    *
    * Repeat the VC session in a loop if there is a VC connection
    * runtime fault during execution.
    *
    * Only repeatable VC operations can be executed in this loop.
    * For VC operations that have side effects, use VcSagaStep.
    *
    * @param <T> type of the return value of the VC session
    * @param session defines the VC session in {@link session.body()}
    * @return result of the VC session
    */
   public static <T> T inVcSessionDo(VcSession<T> session) throws AuroraException {
      if (isInSession()) {
         AuAssert.check(initialized);
         // Make sure that we don't reuse a non-task session for a task session
         AuAssert.check(!session.isTaskSession() || isInTaskSession());
         // Already in VC session, simply return.
         try {
            return session.body();
         } catch (Throwable e) {
            wrapAndThrow(e);
         }
      }

      /*
       * If failed to initialize on start-up, try again, but without retrying.
       */
      if (!initialized) {
         initVcContext();
         AuAssert.check(initialized);
      }
      boolean needsRetry = false;
      int retries = 0;
      long waitTime = VcSession.VC_INIT_WAIT_TIME;
      T result = null;
      long genCount = -1L;
      startSession(session);
      // The try block should follow immediately, so that endSession will
      // always be called if startSession succeeded.
      do {
         try {
            AuAssert.check(isInSession());
            // Login VC if we are logged out of VC service.
            genCount = getService().login();
            if (needsRetry) {
               // undo previous invocation of body()
               session.undo();
            }
            result = session.body();
            needsRetry = false;
         } catch (Throwable e) {
            VcService svc = getService();
            Long curGenCount = svc.getServiceGenCount();
            endSession();
            logger.debug(e);

            if(e instanceof RuntimeFault) {
               RuntimeFault rtFault = (RuntimeFault)e;
               if(!ArrayUtils.isEmpty(rtFault.getFaultMessage())) {
                  logger.error("vCenter Fault Message(s)>>>>");
                  for(LocalizableMessage message : rtFault.getFaultMessage()) {
                     logger.error(message.getMessage());
                  }
                  logger.error("<<<< End of Fault Message(s)");
               }
            }

            if (e instanceof RejectedExecutionException ||
                e instanceof IllegalStateException) {
               /*
                * It's likely that the current session was terminated by another thread,
                * thus we receive this exception from java Executor. We should retry.
                */
               if (curGenCount != null && genCount == svc.getServiceGenCount()) {
                  /* Log error if we got HTTP binding or thread executor exceptions
                   * in the same session.
                   */
                  logger.error("genCount=" + genCount, e);
                  svc.dumpServiceStatus();
               }
            } else if (!(e instanceof NotAuthenticated ||
                         e instanceof InternalException ||
                         e instanceof UnmarshallException)) {
               /*
                * For a normal exception, we simply throw it.
                *
                *  XXX need to verify that we caught the right exceptions
                * The following code is by reading MethodInvocationHandlerImpl.java
                * in vlsi code. We potentially didn't catch other interesting
                * RuntimeExceptions.
                */
               wrapAndThrow(e);
            }

            if (curGenCount != null && genCount != curGenCount) {
               /* If another thread has updated the VcService instance,
                * reset the retry counter for using the new service instance.
                * This would slow down the VcSession failure rate
                * if connection to VC experiences frequent hiccups.
                */
               retries = 0;
               genCount = curGenCount;
               logger.info("reset retries with new genCounter=" + genCount);
            } else if (++retries > session.retries) {
               logger.error("failed to reconnect to VC connection");
               // Failed N retries. Log out of VC and terminate with exception.
               vcService.logout(genCount);
               if ((e instanceof InternalException &&
                    MoUtil.isNetworkException(e.getCause())) ||
                   (e instanceof RejectedExecutionException) ||
                   (e instanceof IllegalStateException)) {
                  /* Either a network problem or VC is down. */
                  throw VcException.UNAVAILABLE_ERROR(e);
               }
               wrapAndThrow(e);
            }

            needsRetry = true;

            /* This is likely caused by session timeout or has
             * exceeded the threshold for forcing reconnection.
             * Let's try to clean up, login again and retry.
             * XXX needs to investigate what exceptions should do login.
             */
            if (e instanceof NotAuthenticated ||
                retries > session.reconnectThreshold) {
               logger.info("try to reconnect for " + e);
               genCount = vcService.reconnect(genCount);
               /*
                * If we managed to login and get VC connection,
                * do not need to wait.
                */
            } else {
               try {
                  Thread.sleep(waitTime);
               } catch (InterruptedException e1) {
                  logger.error("reconnect to VC connection interrupted");
                  wrapAndThrow(e);
               }
               // double wait time for next the round
               waitTime = waitTime << 1;
            }
            startSession(session);
         }
      } while (needsRetry);
      endSession();
      return result;
   }

   /**
    * A callback from a vc service executed on service reset. If this vc
    * session was associated with Event Listener, need to re-establish
    * Listener's server side state in the context of a new vc session.
    *
    * @param svc        vc service that was (re)initialized
    * @throws Exception
    */
   public static void serviceReset(VcService svc) throws Exception {
      if (svc == eventService && eventListener != null) {
         eventListener.reset();
      }
   }

   /**
    * Start listening to events rooted in the specified target cluster
    * or datacenter moRef in the context of vc event listener session.
    * Cannot wait on VcTask completion until EventListener is started.
    *
    * @param targetMoRef        either clutser or datacenter
    * @throws Exception
    */
   private static void startEventListener(ManagedObjectReference targetMoRef)
   throws Exception {
      eventListener = new VcEventListener(targetMoRef);
      eventListener.setDaemon(true);
      eventListener.start();
      eventListener.waitUntilStarted();
      /*
       * If something went wrong during initialization of VcEventListener thread,
       * rethrow exception in the parent thread context.
       */
      if (eventListener.getInitException() != null) {
         throw new Exception("VcEventListener failed to start",
               eventListener.getInitException());
      }
   }

   /**
    * Safely stop Event Listener's thread.
    */
   private static void stopEventListener() {
      if (eventListener != null) {
         eventListener.shutDown();
         eventListener = null;
      }
   }

   /**
    * Start a thread polling for VC long calls.
    */
   private static void startVcLongCallHandler() throws Exception {
      longCallHandler = new VcLongCallHandler();
      longCallHandler.setDaemon(true);
      longCallHandler.start();
      longCallHandler.waitUntilStarted();
   }

   /**
    * Safely stop VC long call handler thread.
    */
   private static void stopVcLongCallHandler() {
      if (longCallHandler != null) {
         longCallHandler.shutDown();
         longCallHandler = null;
      }
   }

   /**
    * "Bad" tasks don't trigger recentTask[] property updates and we must poll
    * for their completions. This function must be called by the poller to make
    * sure that the registered TaskFinishedEvent handlers are called for "bad"
    * tasks just like they are called for good ones. Synchronized prevents
    * eventListener.shutDown().
    * @param task
    */
   public static synchronized void badTaskCompleted(VcTask task) throws Exception {
      if (eventListener != null) {
         eventListener.fireBadTaskFinishedEventHandlers(task);
      }
   }
}
