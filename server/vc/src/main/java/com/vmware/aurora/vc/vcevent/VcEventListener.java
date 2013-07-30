/* ***************************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/

/**
 * <code>VcEventListener</code> is a thread associated with VC session
 * that is responsible for listening to "interesting" VC events rooted
 * at an inventory subtree. Clients will eventually use VcEventListener
 * to get callbacks when watched objects change in VC.
 *
 * @since   0.6
 * @version 0.6
 * @author Boris Weissman
 */

package com.vmware.aurora.vc.vcevent;

import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.concurrent.Semaphore;

import javax.net.ssl.SSLException;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcTask;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.TaskFinishedEvent;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.TaskUpdateProgressEvent;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcService;
import com.vmware.vim.binding.impl.vim.event.ComputeResourceEventArgumentImpl;
import com.vmware.vim.binding.impl.vim.event.EventFilterSpecImpl;
import com.vmware.vim.binding.impl.vim.event.GeneralUserEventImpl;
import com.vmware.vim.binding.impl.vim.event.ManagedEntityEventArgumentImpl;
import com.vmware.vim.binding.impl.vmodl.TypeNameImpl;
import com.vmware.vim.binding.vim.TaskInfo;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.EventFilterSpec;
import com.vmware.vim.binding.vim.event.EventHistoryCollector;
import com.vmware.vim.binding.vim.event.EventManager;
import com.vmware.vim.binding.vim.fault.InvalidLogin;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.query.InvalidProperty;
import com.vmware.vim.binding.vmodl.query.PropertyCollector;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.Change;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.Change.Op;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.FilterSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.FilterUpdate;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectUpdate;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.ObjectUpdate.Kind;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.PropertySpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.SelectionSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.TraversalSpec;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.UpdateSet;
import com.vmware.vim.binding.vmodl.query.PropertyCollector.WaitOptions;
import com.vmware.vim.vmomi.core.exception.InternalException;

public class VcEventListener extends Thread {

   /* VcEventListener DFA states. */
   public enum VcEventListenerState {
      CREATED,          // Just created, no VC server state.
      RESET,            // VC server state established.
      DRAINED,          // "Old" events drained.
      LISTENING,        // Listening in inner loop.
      STOPPED,          // Stopped on shutdown.
      STOPPED_UNCLEAN,  // Unclean stop on shutdown.
      CONNECTING,       // Trying to reconnect.
      INVALID           // Fatal configuration problems.
   }

   private static Logger logger = Logger.getLogger(VcEventListener.class);

   private Semaphore initSema;                  // Signals when init completes.

   // Exponential back-off parameters for vc connection problems.
   private static final int MIN_RETRY_DELAY_SEC = 1;
   private static final int MAX_RETRY_DELAY_SEC = 60;
   private int currentRetryDelaySec = MIN_RETRY_DELAY_SEC;

   /* Read by other threads. */
   private volatile VcEventListenerState state; // Current state.
   private volatile int totalEventCount;        // Total number of events seen so far.
   private volatile int dispatchedEventCount;   // Interesting events seen so far.
   private volatile int totalTaskCount;         // All task state transitions seen.
   private volatile int dispatchedTaskCount;    // Those that caused callback dispatch.
   private volatile Exception initException;    // Exception encountered during init.

   /* Written by other threads */
   private volatile boolean stopRequested;      // Stop requested?

   private ManagedObjectReference targetMoRef;  // Root object to monitor.
   private String currentVersion;               // From last UpdateSet.

   private WaitOptions waitForUpdateOptions;    // Property Collector parameters.

   private EventFilterSpec eventFilterSpec;     // Events of interest.
   private FilterSpec eventPFS;                 // PC "latestPage" prop filter spec.
   private FilterSpec taskPFS;                  // PC task state prop filter spec.

   private ManagedObjectReference eventManagerMoRef;
   private ManagedObjectReference eventHistoryCollectorMoRef;

   private EventManager eventManager;
   private EventHistoryCollector eventHistoryCollector;

   private ManagedObjectReference taskManagerMoRef;

   final private int eventPageSize = 100;       // Max # of events for single read.
   final private int maxWaitSeconds = 60;       // WaitForUpdatesEx timeout.

   private ManagedObjectReference eventFilterMoRef;  // PC filter reference.
   private ManagedObjectReference taskFilterMoRef;   // PC filter reference.

   /**
    * Returns the total number of events trapped by our event collector. Ok to
    * be called by other threads concurrently. Not synchronized, might return
    * stale values.
    * @return total events seen so far
    */
   public int getTotalEventCount() {
      return totalEventCount;
   }

   /**
    * A count of dispatched events - events with registered handlers. Might
    * return stale values.
    * @return dispatched events
    */
   public int getDispatchedEventCount() {
      return dispatchedEventCount;
   }

   /**
    * Return exception thrown at VcEventListener initialization time or null
    * if everything went well.
    * @return Exception or null
    */
   public Exception getInitException() {
      return initException;
   }

   /**
    * If the server side event listener state has been established, returns a
    * reference to Event Manager proxy object. Otherwise returns null.
    * @return EventManager
    */
   private EventManager getEventManager() {
      AuAssert.check(eventManager != null);
      return eventManager;
   }

   /**
    * DFA transitions clearing house.
    * @param newState
    */
   private void setState(VcEventListenerState newState) {
      if (newState == VcEventListenerState.LISTENING) {
         AuAssert.check(state == VcEventListenerState.DRAINED ||
                state == VcEventListenerState.CONNECTING);
      } else if (newState == VcEventListenerState.DRAINED) {
         AuAssert.check(state == VcEventListenerState.CREATED ||
                state == VcEventListenerState.RESET);
      } else if (newState == VcEventListenerState.INVALID) {
         AuAssert.check(state == VcEventListenerState.LISTENING);
      }
      state = newState;
   }

   /**
    * Listening to events in the inner event loop?
    * @return true if all is well
    */
   public boolean isListening() {
      return state == VcEventListenerState.LISTENING;
   }

   /**
    * Create a new Event Listener for a specified target. The target
    * is a moRef of either cluster or datacenter. Currently all events
    * for objects rooted under the target are monitored. While desirable,
    * watching for events rooted at Resource Pool does not quite work:
    *   - VC is unwilling to report sub-RP related events for parent RP
    *   - we want all host and network events that are per cluster
    * This function just creates a java object. reset() needs to be called
    * to establish VC server side event collector state.
    *
    * @param targetMoRef        either cluster or datacenter
    * @param taskMgr            internal task events manager
    * @throws Exception
    */
   public VcEventListener(ManagedObjectReference targetMoRef)
   throws Exception {
      this.targetMoRef = targetMoRef;
      waitForUpdateOptions = new WaitOptions();
      waitForUpdateOptions.maxWaitSeconds = maxWaitSeconds;
      initSema = new Semaphore(0);
      setName("VcEventListener");
      setState(VcEventListenerState.CREATED);
      initException = null;
   }

   /**
    * VcEventListener creator thread can wait here until the listener is
    * fully initialized.
    * @throws InterruptedException
    */
   public void waitUntilStarted() throws InterruptedException {
      AuAssert.check(Thread.currentThread() != this);
      initSema.acquire();
   }

   /**
    * Re-initialize EventListener state either on start-up or following
    * a lost VC connections. Sets up all the necessary VC server side state:
    * property collector filters for "lastPage" as well as the event collector
    * itself and its associated filters. Must be called each time a new VC
    * connection is established.
    *
    * Note that reset() does not remove any previously installed event handlers.
    * We don't want the handlers to disappear on re-establishing VC connection.
    *
    * @throws Exception
    */
   public void reset() throws Exception {
      VcService vcService = VcContext.getService();
      AuAssert.check(vcService != null && vcService.isConnected());
      AuAssert.check(Thread.currentThread() == this); // Other threads can't do this.

      logger.info("VcEventListener reset");
      currentVersion = null;

      eventManagerMoRef = vcService.getServiceInstanceContent().getEventManager();
      eventManager = MoUtil.getManagedObject(eventManagerMoRef);

      taskManagerMoRef = vcService.getServiceInstanceContent().getTaskManager();

      /* Listen to all events under targetMoRef. */
      eventFilterSpec = createEventFilterSpec();
      eventHistoryCollectorMoRef = eventManager.createCollector(eventFilterSpec);
      eventHistoryCollector = MoUtil.getManagedObject(eventHistoryCollectorMoRef);

      /*
       * Set up property collector to watch for changes of either:
       * - EventColletor.latestPage property
       * - Task.info.state property in recent tasks
       */
      eventPFS = createEventPFS();
      eventFilterMoRef = vcService.getPropertyCollector().createFilter(
            eventPFS, false);

      taskPFS = createTaskPFS();
      taskFilterMoRef = vcService.getPropertyCollector().createFilter(
            taskPFS, false);

      /*
       * Reset retry delay only if the last generation did any real work in order
       * to avoid login()/logout() in a tight loop when VC is persistently down.
       */
      if (totalEventCount > 0) {
         resetRetryDelay();
      }
      totalEventCount = 0;
      dispatchedEventCount = 0;
      stopRequested = false;

      setState(VcEventListenerState.RESET);

      drainEvents();
   }

   /**
    * We don't care about the differences between truncated and non-truncated
    * UpdatSet versions. We use a property collector to watch for *any* changes
    * to kick off a round of real event reads. Truncated UpdateSet might cause
    * occasional spurious event reads which is ok.
    *
    * @param version    new version
    * @param UpdateSet  latest changes
    */
   private void setCurrentVersion(String version, UpdateSet updates) {
      if(logger.isInfoEnabled()) {
         if (version.equals("")) {
            logger.debug("Listener: version reset");
         } else {
            logger.debug("Listener: version <- " + version +
                  " truncated " + updates.truncated);
         }
      }
      currentVersion = version;
   }

   /**
    * Create an event filter to listen to events of interest under targetMoRef
    * recursively. We subscribe only to events that are of interest to cms.
    * These events are explicitly listed in VcEventType.
    * @return EventFilterSpec
    */
   private EventFilterSpec createEventFilterSpec() {
      AuAssert.check(targetMoRef != null);
      EventFilterSpec efs = new EventFilterSpecImpl();
      String[] eventTypeIds = VcEventType.getEventTypeIds();
      logger.info("Subscribing to vc events:");
      for (String eventId : eventTypeIds) {
         logger.info("\t" + eventId);
      }
      efs.setEntity(new EventFilterSpecImpl.ByEntityImpl(
            targetMoRef,
            EventFilterSpec.RecursionOption.all));
      efs.setEventTypeId(eventTypeIds);
      return efs;
   }

   /**
    * Create a property filter spec for PropertyCollector with these parameters:
    *   PropertySpec:  "latestPage" property of EventHistoryCollector
    *   ObjectSpec:    eventHistoryCollector
    *   SelectionSpec: none
    * The idea is to wait for any changes to "latestPage" in the eventCollector
    * which designates an arrival of a new event.
    *
    * @return FilterSpec
    */
   private FilterSpec createEventPFS() {
      PropertySpec propSpec = new PropertySpec();
      propSpec.setAll(false);
      propSpec.setPathSet(new String[] { "latestPage" });
      propSpec.setType(MoUtil.getTypeName(eventHistoryCollectorMoRef));

      ObjectSpec objSpec = new ObjectSpec();
      objSpec.setObj(eventHistoryCollectorMoRef);
      objSpec.setSkip(false);
      objSpec.setSelectSet(new SelectionSpec[] { });

      FilterSpec filterSpec = new FilterSpec();
      filterSpec.setPropSet(new PropertySpec[] { propSpec });
      filterSpec.setObjectSet(new ObjectSpec[] { objSpec });
      return filterSpec;
   }

   /**
    * Create a property filter spec to get Task state updates with these
    * parameters:
    *   PropertySpec: "info.state" & "info.progress" properties of Task
    *   ObjectSpec:    taskManager
    *   SelectionSpec: "recentTask"
    *
    * TODO This filter triggers a bunch of "enter" updates on startup as
    * all recentTask[] objects come into view. Could this be addressed as
    * we should not care about initial recentTasks?
    *
    * @return FilterSpec
    */
   private FilterSpec createTaskPFS() {
      PropertySpec propSpec = new PropertySpec();
      propSpec.setAll(false);
      propSpec.setPathSet(new String[] { "info.state", "info.progress" });
      propSpec.setType(new TypeNameImpl("Task"));

      ObjectSpec objSpec = new ObjectSpec();
      objSpec.setObj(taskManagerMoRef);
      objSpec.setSkip(false);

      TraversalSpec tSpec = new TraversalSpec();
      tSpec.setType(new TypeNameImpl("TaskManager"));
      tSpec.setPath("recentTask");
      tSpec.setSkip(false);

      objSpec.setSelectSet(new SelectionSpec[] {tSpec});

      FilterSpec filterSpec = new FilterSpec();
      filterSpec.setPropSet(new PropertySpec[] { propSpec });
      filterSpec.setObjectSet(new ObjectSpec[] { objSpec });
      return filterSpec;
   }

   /**
    * Block on waitForUpdatesEx() until a new event arrives.
    * @return UpdateSet
    * @throws Exception
    */
   private UpdateSet waitForUpdates() throws Exception {
      UpdateSet updateSet;
      PropertyCollector pc = VcContext.getService().getPropertyCollector();

      logger.debug("waitForUpdates");
      updateSet = pc.waitForUpdatesEx(currentVersion, waitForUpdateOptions);
      return updateSet;
   }

   /**
    * Returns true if the exception received in innerWaitForEventsLoop() could
    * be recovered from via retry *without* re-initialization of vc session:
    * transient network failures, etc. List borrowed from redwood.
    *
    *    Hierarchy:
    *    Exception
    *    -- IOException
    *    ---- SocketException
    *    ------ ConnectException
    *    -------- HttpHostConnectException
    *    ---- InterruptedIOException
    *    ------ SocketTimeoutException
    *
    * Socket exception involves many recoverable cases, but also includes vc
    * shutdown, unfortunately. Upon vc restart, we'll get NotAuthenticated
    * breaking us out of the inner loop. Until then, it is ok to retry in
    * the inner loop.
    *
    * @param  exception to check
    * @return true if retry could help
    */
   private boolean isRecoverableInnerException(Throwable e) {
      return MoUtil.isNetworkException(e) ||      /* Transient network problem.  */
         e instanceof InterruptedException;       /* Will recheck stopRequested. */
   }

   /**
    * Inner wait for events loop. Waits for "lastPage" property updates from VC
    * via property collector (new event received). Then reads all new events from
    * EventCollector. Runs continuously until shutDown() or a fatal VC error
    * that invalidates the current VC session.
    *
    * Tries hard not to break out of the loop on recoverable errors. Returning
    * on a fatal VC error will likely mean VcEventListener reset *and* full
    * inventory reload/verification.
    *
    * Returns when Event Listener was shut down. Throws an exception upon
    * encountering an irrecoverable VC session problems.
    *
    * @throws Exception when a VC session cannot be recovered
    */
   private void innerWaitForEventsLoop() throws Exception {
      logger.debug("innerWaitForEventsLoop");
      setState(VcEventListenerState.LISTENING);

      while (!stopRequested) {
         UpdateSet updateSet = null;

         try {
            updateSet = waitForUpdates();
            dispatchUpdates(updateSet);
            resetRetryDelay();
         } catch (InternalException e) {
            /*
             * VLSI fun: deal with unchecked internal exceptions. Here we try
             * to extract potentially recoverable exceptions to preserve the
             * current session with its established Event Collector state.
             */
            Throwable cause = e.getCause();
            if (isRecoverableInnerException(cause)) {
               onInnerLoopContinue(e);
               continue;
            } else {
               /* For now, terminate on all unknown internal exceptions. */
               onInnerLoopTerminate(e);
               throw e;
            }
         } catch (AuroraException e) {
            /*
             * Something went wrong during event handler execution (not related to this
             * VC session). Also might land here if CMS DB is down - breaks some event
             * handlers (Bug 733665). No need to break out of inner loop - logging out
             * of VC will not fix this problem.
             */
            onInnerLoopContinue(e);
            continue;
         } catch (Exception e) {
            /* For now, terminate on all unknown exceptions. */
            onInnerLoopTerminate(e);
            throw e;
         }
      }
      onInnerLoopTerminate(null);
   }

   /**
    * Clean up on recoverable exception in waitForEventLoop().
    * @param recoverable exception
    */
   private void onInnerLoopContinue(Throwable e) {
      if(!stopRequested) {
         logger.warn("Recovering from inner exception: " + e.getMessage());
      }
      /* Don't delay for client timeout or stop request. */
      if (!(e instanceof SocketTimeoutException || stopRequested)) {
         retryDelay();
      }
   }

   /**
    * Executed before breaking out of the inner event loop. Since the
    * exception will be re-thrown to the outer loop that will perform
    * additional parsing, we log the stack trace at the outer level.
    * @param irrecoverable exception
    */
   private void onInnerLoopTerminate(Throwable e) {
      if (stopRequested) {
         logger.debug("Inner stopRequested");
      } else {
         logger.warn("Inner irrecoverable exception: " +
               ((e == null) ? "null" : e.getMessage()));
      }
   }

   /**
    * Sleep between successive attempts to contact vc that fail because of
    * recoverable errors. Uses an exponential back-off algorithm.
    */
   private void retryDelay() {
      logger.debug("retryDelay secs: " + currentRetryDelaySec);
      try {
         Thread.sleep(currentRetryDelaySec * 1000L);
      } catch (Exception e) {
      }
      updateRetryDelay();
   }

   /**
    *  Apply exponential back-off to the retry interval.
    */
   private void updateRetryDelay() {
      currentRetryDelaySec *= 2;
      if (currentRetryDelaySec > MAX_RETRY_DELAY_SEC) {
         currentRetryDelaySec = MAX_RETRY_DELAY_SEC;
      }
      logger.debug("currentRetryDelaySec <- " + currentRetryDelaySec);
   }

   /**
    * Reset vc retry delay following a successful round of vc communication.
    */
   private void resetRetryDelay() {
      currentRetryDelaySec = MIN_RETRY_DELAY_SEC;
      logger.debug("Resetting currenRetryDelaySec <- " + currentRetryDelaySec);
   }

   /**
    * "Drain" events on VC server. In effect, moves the current event
    * pointer past all events received so far. Our inventory snapshot is
    * not longer trustworthy after this operation.
    * @throws InterruptedException
    */
   private void drainEvents() throws InterruptedException {
      int drained = 0;
      Event[] events;

      /* Move the cursor to the last "viewable page" */
      eventHistoryCollector.reset();
      /* Read and discard everything in the last page. */
      while ((events = readEvents(false)) != null) {
         drained += events.length;
      }
      setState(VcEventListenerState.DRAINED);
      logger.debug("EventListener drained: " + drained);
   }

   /**
    * A placeholder for event callback dispatch. Currently just logs
    * event info.
    * @param events
    * @throws Exception
    */
   private void dispatchEvents(Event[] events) throws Exception {
      if (logger.isDebugEnabled()) {
         VcEventUtil.dumpEvents(events);
      }
      for (Event e : events) {
         VcEventHandlers.getInstance().fire(e);
      }
   }

   /**
    * Returns true for exceptions triggered by invalid configuration
    * parameters: username/password, bad URL, etc. We cannot do anything
    * to recover from exceptions of this kind.
    * @param e
    * @return true for 'invalid' exceptions
    */
   boolean isInvalidConfigOuterException(Throwable e) {
      return
         e instanceof InvalidLogin              ||
         e instanceof URISyntaxException        ||
         e instanceof InvalidProperty           ||
         e instanceof SSLException;
   }

   /**
    * The outer event processing loop. The inner loop breaks out on shutdown
    * request or when vc session is no longer available. The outer loop deals
    * with vc session re-initialization. Full inventory re-validation will
    * also belong here.
    *
    * Returns in two cases:
    * - shut down request
    * - invalid configuration: bad authentication, url, etc.
    *
    * On all other failures, tries to reconnect to vc.
    */
   private void outerWaitForEventsLoop() {
      while (true) {
         logger.debug("outerWaitForEventsLoop");
         try {
            if (stopRequested) {
               onOuterLoopTerminate(VcEventListenerState.STOPPED, null);
               break;
            }

            innerWaitForEventsLoop();

         } catch (Exception e) {
            if (isInvalidConfigOuterException(e)) {
               onOuterLoopTerminate(VcEventListenerState.INVALID, e);
               break;
            } else {
               onOuterLoopContinue(VcEventListenerState.CONNECTING, e);
            }
         }
         /*
          * VC session went bad. Logout to force automatic relogin that will
          * trigger the establishment of the new VC session. On stop, logout
          * happens along with other cleanup.
          */
         if (!stopRequested) {
            VcService vcService = VcContext.getService();
            if (vcService == null) {
               /*
                * Tomcat likes to clean out ThreadLocal on shutdown. We might land here
                * if something went wrong with the thread executing WebServiceContextListener
                * callbacks before it had a chance to cleanly shut down VcEventListener.
                * Terminate the thread - Bug 733665.
                */
               onOuterLoopTerminate(VcEventListenerState.STOPPED_UNCLEAN, null);
               break;
            } else {
               vcService.logout();
            }
         }
      }
   }

   private void onOuterLoopContinue(VcEventListenerState newState, Throwable e) {
      if (!stopRequested) {
         logger.info("onOuterLoopContinue ", e);
         retryDelay();
      }
      setState(newState);
   }

   private void onOuterLoopTerminate(VcEventListenerState newState, Throwable e) {
      StringBuilder msg = new StringBuilder("onOuterLoopTerminate ").append(newState);
      if (stopRequested) {
         logger.info(msg);  // No stack trace ok.
      } else if (newState == VcEventListenerState.INVALID){
         logger.error(msg.toString(), e);
      } else if (newState == VcEventListenerState.STOPPED_UNCLEAN){
         logger.warn(msg);
      } else {
         logger.warn(msg.toString(), e);
      }
      setState(newState);
   }

   /**
    * Activate Event Listener thread.
    */
   public void run() {
      VcContext.startEventSession();
      try {
         try {
            reset();
         } catch (Exception e) {
            logger.error("EventListener reset failure", e);
            initException = e;     // Runs before initSema.release().
            return;
         } finally {
            initSema.release();    // Wake up the creator.
         }
         AuAssert.check(state == VcEventListenerState.DRAINED);
         outerWaitForEventsLoop();
      } finally {
         VcContext.endSession();
      }
   }

   /**
    * Request Event Listener shutdown and wait until it completes.
    * @throws InterruptedException
    */
   public void shutDown() {
      if (Thread.currentThread() == this) {
         return;
      }
      /* Wait until the Event Listener breaks out of event loops. */
      stopRequested = true;
      interrupt();

      while (true) {
         try {
            join();
            logger.info("VcEventListener joined");
            break;
         } catch (InterruptedException e) {
            logger.debug("Interrupted while waiting for VcEventListener shutdown");
         }
      }
   }

   /**
    * Read at most eventPageSize events from VC and advance the cursor.
    * Hint says if we should expect to find some events. Every now and
    * then we get a VC hiccup when "lastPage" indicates a new event
    * arrival, but readNext() still returns null for a while. So retry.
    *
    * @param hint       do we expect to find new events?
    * @return events    new events, if any
    * @throws InterruptedException
    */
   private Event[] readEvents(boolean hint) throws InterruptedException {
      final int retryCount = 5;
      int iter = 0;
      Event[] events = null;

      do {
         events = eventHistoryCollector.readNext(eventPageSize);
         if (hint && events == null) {
            Thread.sleep(100);
         }
      } while (hint && events == null && iter++ < retryCount);

      if (iter >= retryCount) {
         logger.debug("lastPage & readNext() mismatch");
      }
      return events;
   }

   private void fireTaskFinishedEventHandlers(ManagedObjectReference taskMoRef,
                                              TaskInfo.State state)
   throws Exception {
      VcEventHandlers handlers = VcEventHandlers.getInstance();
      /* Create and fire TaskFinished pseudo-event. */
      TaskFinishedEvent taskEvent = handlers.new TaskFinishedEvent(taskMoRef, state);
      handlers.fire(taskEvent);
      dispatchedTaskCount++;    // stats only, unlocked ok.
   }

   /**
    * Dispatch qualified task state transitions to registered handlers.
    *
    * @param taskMoRef  affected task
    * @param taskState  new state
    * @throws Exception
    */
   private void taskUpdateStateHandler(ManagedObjectReference taskMoRef,
         TaskInfo.State taskState) throws Exception {
      logger.debug("\ttaskMoRef: " + taskMoRef.getValue());
      logger.debug("\tstate:     " + taskState);
      if (taskState == TaskInfo.State.success ||
          taskState == TaskInfo.State.error) {
         fireTaskFinishedEventHandlers(taskMoRef, taskState);
      }
   }

   /**
    * Dispatch task progress updates to registered handlers.
    *
    * @param taskMoRef  affected task
    * @param progress   current progress
    * @throws Exception
    */
   private void taskUpdateProgressHandler(ManagedObjectReference taskMoRef,
         Integer progress) throws Exception {
      VcEventHandlers handlers = VcEventHandlers.getInstance();
      logger.debug("\ttaskMoRef: " + taskMoRef.getValue());
      logger.debug("\tprogress:     " + progress);
      if (progress != null) {
         /* Create and fire TaskUpdateProgress pseudo-event. */
         TaskUpdateProgressEvent taskEvent =
            handlers.new TaskUpdateProgressEvent(taskMoRef, progress);
         handlers.fire(taskEvent);
      }
   }

   /**
    * Called to trigger fake TaskFinishedEvent for "bad" tasks that don't naturally
    * update VC recentTask[] objects on their own...
    * @param task
    */
   public void fireBadTaskFinishedEventHandlers(VcTask task) throws Exception {
      AuAssert.check(task.isBadTask());
      fireTaskFinishedEventHandlers(task.getMoRef(), task.getState());
   }

   /**
    * Dispatch all newly found events to the handler.
    * @throws Exception
    */
   private void eventUpdateHandler() throws Exception {
      Event[] events;

      while ((events = readEvents(true)) != null) {
         totalEventCount += events.length;
         dispatchEvents(events);
      }
   }

   /**
    * Parse an update set and dispatch individual changes to their
    * respective high level handlers:
    * - if at least one real VcEvent detected, invoke generic vc Event handler
    * - for each individual task completion, invoke a task completion handler
    *
    * @param updates
    * @throws Exception
    */
   private void dispatchUpdates(final UpdateSet updates) throws Exception {
      boolean invokeEventHandler = false;

      logger.debug("dispatchUpdates: curVersion: " + currentVersion);
      if (updates == null) {
         logger.debug("UpdateSet is null, curVersion: " + currentVersion);
         return;
      }
      FilterUpdate[] filterUpdates = updates.getFilterSet();
      if (filterUpdates == null) {
         logger.debug("PropertyFilterUpdate is null, curversion: " + currentVersion);
         return;
      }
      VcEventUtil.dumpUpdateSet(updates, currentVersion);

      for (FilterUpdate pfu : filterUpdates) {
         ManagedObjectReference filterMoRef = pfu.getFilter();
         if (filterMoRef.equals(eventFilterMoRef)) {
            /* EventCollector.latestPage update. */
            invokeEventHandler = true;
         } else {
            /* Task.info.state updates: extract individual moRefs & changes. */
            AuAssert.check(filterMoRef.equals(taskFilterMoRef));
            if (pfu.getObjectSet() != null) {
               for (ObjectUpdate oUpdate : pfu.getObjectSet()) {
                  AuAssert.check(oUpdate.getObj().getType().equalsIgnoreCase("Task"));
                  totalTaskCount++;
                  if ((oUpdate.getKind() == Kind.modify ||
                       oUpdate.getKind() == Kind.enter) &&
                      oUpdate.getChangeSet() != null) {
                     for (Change c : oUpdate.getChangeSet()) {
                        if (c.getName().equals("info.state")) {
                           if (c.getOp() == Op.assign || c.getOp() == Op.add) {
                              TaskInfo.State taskState = (TaskInfo.State) c.getVal();
                              taskUpdateStateHandler(oUpdate.getObj(), taskState);
                           }
                        } else {
                           AuAssert.check(c.getName().equals("info.progress"));
                           if (c.getOp() == Op.assign || c.getOp() == Op.add) {
                              Integer progress = (Integer) c.getVal();
                              taskUpdateProgressHandler(oUpdate.getObj(), progress);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (invokeEventHandler) {
         eventUpdateHandler();
      }

      /* Get ready for next PC cycle. */
      String newVersion = updates.getVersion();
      setCurrentVersion(newVersion, updates);
   }

   /**
    * Post GeneralUserEvent to a target cluster. Event Collector must have
    * been initialized. Used by junit.
    *
    * @param cluster    target cluster
    * @param msg        short event message
    * @param fullMsg    fully formatted event message
    * @param userName   user to post event on behalf of
    * @throws Exception
    */
   public void postGeneralUserEvent(VcCluster cluster, String msg, String fullMsg,
            String userName)
   throws Exception {
      GeneralUserEventImpl event = new GeneralUserEventImpl();
      event.setMessage(msg);
      event.setFullFormattedMessage(fullMsg);
      event.setEntity(new ManagedEntityEventArgumentImpl(cluster.getName(), cluster.getMoRef()));
      event.setComputeResource(new ComputeResourceEventArgumentImpl(cluster.getName(), cluster.getMoRef()));
      event.setCreatedTime(new GregorianCalendar());
      event.setUserName(userName);
      getEventManager().postEvent(event, null);
   }

   /**
    * Install an event handler for a cms event of a specified event type. An event
    * is classified as cms event if it is triggered as a result of any vc task
    * performed by cms. Otherwise it is an external event. Multiple handlers may be
    * installed for the same type. The firing order is unspecified. Installed
    * handlers survive VC connection re-initialization.
    *
    * @param eventType  target event kind
    * @param handler    supplied event handler
    */
   public static void installEventHandler(VcEventType eventType,
         IVcEventHandler handler) {
      VcEventHandlers.getInstance().installEventHandler(eventType, handler, false);
   }

   /**
    * Installs a given handler for a set of events specified via EnumSet.
    * @param eventTypes target event kinds
    * @param handler    supplied event handler
    */
   public static void installEventHandler(EnumSet<VcEventType> eventTypes,
         IVcEventHandler handler) {
      for (VcEventType type : eventTypes) {
         installEventHandler(type, handler);
      }
   }

   /**
    * Similar to installEventHandler(), but is fired for events that are triggered
    * by actions that originate outside CMS: hardware failure, VC admin actions, etc.
    * @param eventType  target event kind
    * @param handler    supplied event handler
    */
   public static void installExtEventHandler(VcEventType eventType,
                                             IVcEventHandler handler) {
      VcEventHandlers.getInstance().installEventHandler(eventType, handler, true);
   }

   /**
    * Installs a given handler for a set of external events specified via EnumSet.
    * @param eventTypes target event kinds
    * @param handler    supplied event handler
    */
   public static void installExtEventHandler(EnumSet<VcEventType> eventTypes,
                                             IVcEventHandler handler) {
      for (VcEventType type : eventTypes) {
         installExtEventHandler(type, handler);
      }
   }

   /**
    * Remove a handler for a specified event type. Returns true if the handler was
    * previously installed and was, in fact, removed. False otherwise. If multiple
    * handlers are installed, this removes only a single handler instance.
    *
    * @param eventType  target event kind
    * @param handler    handler to be removed
    * @return true if hanlder removed
    */
   public static boolean removeEventHandler(VcEventType eventType,
                                            IVcEventHandler handler) {
      return VcEventHandlers.getInstance().removeEventHandler(eventType, handler, false);
   }

   public static boolean removeEventHandler(EnumSet<VcEventType> eventTypes,
         IVcEventHandler handler) {
      boolean res = true;
      for (VcEventType type : eventTypes) {
         res = res && removeEventHandler(type, handler);
      }
      return res;
   }

   /**
    * Removes an external event handler. Returns true if the handler was removed,
    * false if no such handler was found.
    *
    * @param eventType  target event kind
    * @param handler    handler to be removed
    */
   public static boolean removeExtEventHandler(VcEventType eventType,
                                               IVcEventHandler handler) {
      return VcEventHandlers.getInstance().removeEventHandler(eventType, handler, true);
   }

   public static boolean removeExtEventHandler(EnumSet<VcEventType> eventTypes,
                                               IVcEventHandler handler) {
      boolean res = true;
      for (VcEventType type : eventTypes) {
         res = res && removeExtEventHandler(type, handler);
      }
      return res;
   }
}
