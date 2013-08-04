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

/**
 * <code>VmAppHealthThread</code> maintains a queue of VmAppHealthChanged events
 * and attempts to fetch 'latest' guest vars for the source VM. Once the latest
 * guest vars are obtained, it dispatches the eventHandler to actually process
 * the event (e.g. using guest vars to extract per-app events).
 * @since   1.0
 * @version 1.0
 * @author Nikhil Deshpande
 */

package com.vmware.aurora.vc.vcevent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.util.CommandExec;
import com.vmware.aurora.vc.CmsVApp;
import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.EventEx;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public class VmAppHealthThread extends Thread {

   /**
    * A handler interface for VmAppHealthChanged event.
    */
   public interface IVmAppHealthEventHandler {
      /**
       * A handler for a specific type of VcEvent VmAppHealthChanged event.
       * The VmAppHealthThread object invokes this interface on fetching latest
       * state (e.g. guest vars for the VM) for the VM once the VmAppHealthChanged
       * trigger event is received.
       * @param vmMoRef moref string for VM on which event occurs
       * @param vmName name of VM on which event occurs
       * @param eventType vc event type
       * @param newState VmAppHealthChanged event (e.g. Green/Yellow/Red etc.)
       * @param guestVars map of HA app event vars (e.g. VM's HA status summary, per-app status etc.)
       */
      void appEventHandler(String vmMoRef, String vmName, VcEventType eventType,
            VmAppHealthState newState, Map<String, String> guestVars);
   }

   //--------------------------------------------------------------------------
   /**
    * A set of application monitoring states inferred from vc events. Our states
    * are slightly different from vSphere application monitoring states.
    * vSphere: Green all is well Red vm is in trouble Gray app monitoring
    * disabled
    *
    * Our Yellow state includes Gray, but it might also mean a non-critical or
    * recoverable failure that does not require VM reset by HA. The agent can
    * trigger this state by briefly disabling VMGuestAppMonitor as in:
    * VMGuestAppMonitor_Disable(); // Triggers Gray event. sleep(1);
    * VMGuestAppMonitor_Enable(); // Triggers Green event. The idea is to
    * communicate Yellow correctable condition without ever triggering Red which
    * might cause VM reset by HA.
    *
    * The Yellow state might be transient state, aurora_mon running in the source
    * VM might attempt to correct the problem.
    */
   static public enum VmAppHealthState {
      /* State ("pattern") */
      Green("Green"),
      Yellow("Gray"),
      Red("Red"),
      Unknown("Unknown");

      private String pattern; // Pattern in vc event message.
      final private static Pattern msgPattern = Pattern.compile("[Aa]pplication heartbeat status changed to (\\S+) ");

      private VmAppHealthState(String pattern) {
         this.pattern = pattern;
      }

      private String getPattern() {
         return pattern;
      }

      /**
       * Returns VmAppHealthState for event with dynamic type set to
       * "VmAppHealthMonitoringStateChangedEvent".
       *
       * @param eventMsg
       * @return VmAppHealthState
       */
      public static VmAppHealthState getInstance(Event event) {
         AuAssert.check(event instanceof EventEx);
         String eventMsg = event.getFullFormattedMessage();
         if (eventMsg != null) {
            Matcher matcher = msgPattern.matcher(eventMsg);
            if (matcher.find()) {
               for (VmAppHealthState state : VmAppHealthState.values()) {
                  if (matcher.group(1).equals(state.getPattern())) {
                     return state;
                  }
               }
            }
         }
         String msg = ("Unable to parse VmAppHealthState event message: " +
               (eventMsg == null ? "null" : eventMsg));
         logger.warn(msg);
         return Unknown;
      }
   }
   //--------------------------------------------------------------------------

   private static class QEntry {
      static enum QEntryState {
         FETCH_GV,
         GOT_GV,
         ERRORED;
      }
      Event event;
      IVmAppHealthEventHandler evtHandler;
      // Transient info
      QEntryState state;
      final long stopTime;
      final long waitTimeHint;
      Map<String, String> guestVars;
      VcVirtualMachine vcVm;
      int errorCount;
      int objNotFoundErrorCount;
      long backoffStepSize;
      long backoffTime;

      private static final String HA_GVAR_REFRESH_TS = "guestinfo.ha_app_mon_refresh_ts";
      private static final long GUEST_VAR_CHECK_TIMEOUT  = TimeUnit.SECONDS.toNanos(11);
      private static final long BACKOFF_START_STEP = TimeUnit.SECONDS.toNanos(2);
      private static final int MAX_ERR_COUNT = 11; // With 2sec as start, ~51min total backoff
      private static final int MAX_OBJ_NOT_FOUND_ERR_COUNT = 5;

      QEntry(Event event, IVmAppHealthEventHandler evtHandler) {
         this.event = event;
         this.evtHandler = evtHandler;
         this.stopTime = System.nanoTime() + GUEST_VAR_CHECK_TIMEOUT;
         this.waitTimeHint = event.getCreatedTime().getTimeInMillis();
         this.guestVars = null;
         this.errorCount = 0;
         this.backoffStepSize = BACKOFF_START_STEP;
         this.backoffTime = 0;
         String refId = MoUtil.morefToString(event.getVm().getVm());
         try {
            this.vcVm = VcCache.get(refId);
         } catch (Exception ex) {
            logger.error("Caught exception from VcObject.get():"
                  + ex.getClass().getName() + ":" + ex.getMessage());
            this.vcVm = null;
         }
         if (this.vcVm == null) {
            logger.warn("Unable to get VcVirtualMachine for event "
               + VcEventType.getInstance(event) + " with moref" + refId);
            this.state = QEntryState.ERRORED;
         } else {
            this.state = QEntryState.FETCH_GV;
         }
      }

      boolean shouldRefresh() {
         return (state == QEntryState.FETCH_GV);
      }

      boolean gotGV() {
         return (state == QEntryState.GOT_GV);
      }

      boolean isDone() {
         return (state == QEntryState.GOT_GV || state == QEntryState.ERRORED);

      }

      void refreshGuestVars() {
         if (isDone()) {
            return;
         }
         long curtime = System.nanoTime();
         if (errorCount > 0 && curtime < backoffTime) {
            // Throttle-down refreshing guest vars if error encountered on previous attempts
            return;
         }
         boolean returnGV = false;
         try {
            VcContext.inVcSessionDo(new VcSession<Void>(){
               @Override
               protected Void body() throws Exception {
                  guestVars = vcVm.getGuestVariables();
                  return null;
               }
            });
            // We keep the error count as it is even on a successful guest var refresh,
            // so that the entry can't be stuck infnitely in a queue and does have a limit
            // on how long it can stay in the queue.
         } catch (Exception ex) {
            if (errorCount < MAX_ERR_COUNT && objNotFoundErrorCount < MAX_OBJ_NOT_FOUND_ERR_COUNT) {
               // E.g. if VC connection is down temporarily, retry later again.
               logger.info("Caught exception while fetching guest vars for VM " + vcVm.getId() + ", will retry. "
                     + "Got exception " + ex.getClass().getName() + " description:" + ex.getMessage());
               errorCount++;
               if (ex instanceof ManagedObjectNotFound) {
                  // Very less chance of this being recoverable error.
                  objNotFoundErrorCount++;
               }
               backoffTime = System.nanoTime() + backoffStepSize;
               // Double the next wait interval
               backoffStepSize += backoffStepSize;
            } else {
               logger.warn("Stopping guest var refresh for VM " + vcVm.getId() + ", max error count reached.");
               state = QEntryState.ERRORED;
            }
            return;
         }

         // If current batch of GVs contain the VM's refreshTimestamp
         // near the waitTimeHint, we've found latest GVs.
         long refreshTs = getHaAppMonRefreshTs(guestVars);
         if (refreshTs >= waitTimeHint) {
            returnGV = true;
         }
         // If waited more than wait timeout (stopTime), return current
         // state of GVs.
         curtime = System.nanoTime();
         if (curtime > stopTime) {
            returnGV = true;
         }
         if (returnGV) {
            long totalWait =
               TimeUnit.NANOSECONDS.toMillis(GUEST_VAR_CHECK_TIMEOUT - (stopTime - curtime));
            logger.info("Waited " + totalWait + "(ms) to get guest vars for vm " +
               vcVm.getName() + " refreshTs(ms)=" + refreshTs);
            if (guestVars == null || guestVars.isEmpty()) {
               state = QEntryState.ERRORED;
               logger.error("Got null/empty guest vars for vm " + vcVm.getName());
            } else {
               state = QEntryState.GOT_GV;
            }
         }
      }

      static long getHaAppMonRefreshTs(Map<String, String> guestVars) {
         try {
            if (guestVars != null && guestVars.get(HA_GVAR_REFRESH_TS) != null) {
               return Long.parseLong(guestVars.get(HA_GVAR_REFRESH_TS));
            }
         } catch (NumberFormatException ex) {
            logger.error("Error parsing value of guest var " + HA_GVAR_REFRESH_TS
                  + ":" + ex.getClass().getName() + ":" + ex.getMessage());
         }
         return -1;
      }
   }

   //--------------------------------------------------------------------------
   /* Utility class to invoke local command to get HA app status from local aurora_mon,
    * parse the verbose status string and split into a map similar to guest vars.
    */
   static class CmsAppHealthEventHelper {
      private String prevHAStatus; // Previous HA status summary string, if different than current,
                                   // need to get all guest vars from CMS.
      private long checkTime;      // Wait at least till this time to perform next HA status summary diff.

      private static long STATUS_CHECK_INTERVAL_MS = 30 * 1000; // default 30 sec
      private static final long CMD_TIMEOUT = 2 * 1000; // 2 sec
      private static final String VAR_KEY_PREFIX = "guestinfo.";
      private static String getHAStatusCmd = null;
      private static String getHAVerboseStatusCmd = null;
      static {
         STATUS_CHECK_INTERVAL_MS = Configuration.getLong("cms.local_ha_status_check_interval", STATUS_CHECK_INTERVAL_MS);
         getHAStatusCmd = Configuration.getNonEmptyString("cms.ctl_get_ha_status");
         getHAVerboseStatusCmd = Configuration.getNonEmptyString("cms.ctl_get_ha_status_verbose");
      }

      CmsAppHealthEventHelper() {
         prevHAStatus = "";
         checkTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STATUS_CHECK_INTERVAL_MS);
      }

      public boolean timeToCheckStatus() {
         return (System.nanoTime() > checkTime);
      }

      public String getPrevHAStatus() {
         return prevHAStatus;
      }

      public void setPrevHAStatus(String curHAStatus) {
         prevHAStatus = curHAStatus;
      }

      public String getCurHAStatus() {
         try {
            String curHAStatus = CommandExec.exec(getHAStatusCmd.split(" "), null, CMD_TIMEOUT);
            checkTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STATUS_CHECK_INTERVAL_MS);
            return curHAStatus;
         } catch (Exception ex) {
            logger.warn("Error while getting local HA app verbose status, retrying." +
                  ex.getClass().getName() + ":" + ex.getMessage());
         }
         return null;
      }

      public Map<String, String> getVars() {
         String verboseStatus = null;
         try {
            verboseStatus = CommandExec.exec(getHAVerboseStatusCmd.split(" "), null, CMD_TIMEOUT);
            if (verboseStatus != null && !verboseStatus.isEmpty()) {
               return parseVars(verboseStatus);
            }
         } catch (Exception ex) {
            logger.warn("Error while getting local HA app verbose status, retrying." +
                  ex.getClass().getName() + ":" + ex.getMessage());
         }
         logger.warn("Failed to get local HA app verbose status");
         return null;
      }

      private Map<String, String> parseVars(String verboseStatus) {
         Map<String, String> vars = new HashMap<String, String>();
         String[] varTokens = verboseStatus.split("#");
         for (String varToken : varTokens) {
            String[] kvTokens = varToken.split("=", 2);
            if (kvTokens.length == 2 && !kvTokens[0].isEmpty() && !kvTokens[1].isEmpty()) {
               vars.put(VAR_KEY_PREFIX + kvTokens[0], kvTokens[1]);
            } else {
               logger.warn("Skipping malformed local var string while reading local HA app health var: "
                     + varToken);
            }
         }
         return vars;
      }
   }

   //--------------------------------------------------------------------------
   private static Logger logger = Logger.getLogger(VmAppHealthThread.class);
   private static final int GV_REFRESH_WAIT_PER_VM = 100; // ms
   private static final int GV_REFRESH_WAIT_PER_VM_BATCH = 1000; // ms

   private LinkedBlockingQueue<QEntry> inputQ;
   private ArrayList<QEntry> workQ;
   private volatile boolean stopRequested;
   private volatile IVmAppHealthEventHandler cmsAppEventHandler;
   private CmsAppHealthEventHelper cmsAppEventHelper;

   /**
    * Starts VM HA app events handler which is able to queue-up VmAppHealthChanged events,
    * fetch guest vars from the VM to get HA app event info, parse them and publish CMS events.
    * @param localAppEventHandler Special handler for local CMS HA events which are directly
    *        fetched from local HA app monitor instead of through guest vars.
    */
   public VmAppHealthThread(IVmAppHealthEventHandler cmsAppEventHandler) {
      setName("VmAppHealthThread");
      this.inputQ = new LinkedBlockingQueue<QEntry>();
      this.workQ = new ArrayList<QEntry>(); // TODO: Currently unbounded.
      this.stopRequested = false;
      this.cmsAppEventHelper = new CmsAppHealthEventHelper();
      this.cmsAppEventHandler = cmsAppEventHandler;
      setDaemon(true);
      start();
   }

   /*
    * Invokes the event handler method inside a VC session.
    */
   private void dispatchEvent(final IVmAppHealthEventHandler handlerObj, final String moRefStr,
         final String vmName, final VcEventType evtType, final VmAppHealthState evtState,
         final Map<String, String> vars) {
      VcContext.inVcSessionDo(new VcSession<Void>(){
         @Override
         protected Void body() throws Exception {
            handlerObj.appEventHandler(moRefStr, vmName, evtType, evtState, vars);
            return null;
         }
      });
   }

   private void dispatchVmEvent(QEntry entry) {
      // We add VmDasBeingReset event here
      VmAppHealthState state = entry.event instanceof EventEx ? VmAppHealthState.getInstance(entry.event) : VmAppHealthState.Unknown;
      dispatchEvent(entry.evtHandler, MoUtil.morefToString(entry.event.getVm().getVm()),
            entry.event.getVm().getName(), VcEventType.getInstance(entry.event),
            state, entry.guestVars);
   }

   /*
    * Fakes a VmAppHealthChanged event to feed in local CMS event into the VM app event handler.
    */
   private void dispatchCmsEvent(Map<String, String> guestVars) {
      String cmsMoRefStr = null;
      String cmsVmName = null;
      try {
         cmsMoRefStr = CmsVApp.getInstance().getCMS().getId();
         cmsVmName = CmsVApp.getInstance().getCMS().getName();
      } catch (Exception ex) {
         logger.error("Error retrieving Management Server VM info while dispatching local event" +
               "Got exception:" + ex.getClass().getName() + ":" + ex.getMessage());
         return;
      }
      dispatchEvent(cmsAppEventHandler, cmsMoRefStr, cmsVmName,
         VcEventType.VmAppHealthChanged, VmAppHealthState.Yellow, guestVars);
   }

   /*
    * If enough time has passed since last check, checks current local HA app monitor status
    * against previous status, if they are different, fetches verbose status from HA app monitor
    * and dispatches an event to VM event handler.
    */
   private void checkAndDispatchCmsEvent() {
      if (cmsAppEventHandler != null && cmsAppEventHelper.timeToCheckStatus()) {
         String curHAStatus = cmsAppEventHelper.getCurHAStatus();
         logger.debug("Time to check HA status, cur=" + curHAStatus + " prev=" + cmsAppEventHelper.getPrevHAStatus());
         if (curHAStatus != null && !curHAStatus.isEmpty()
               && !cmsAppEventHelper.getPrevHAStatus().equals(curHAStatus)) {
            Map<String, String> vars = cmsAppEventHelper.getVars();
            logger.debug("Fetch vars:" + (vars == null ? "null" : "" + vars.size()));
            if (vars != null && !vars.isEmpty()) {
               dispatchCmsEvent(vars);
               cmsAppEventHelper.setPrevHAStatus(curHAStatus);
            }
         }
      }
   }

   /**
    * Request VmAppHealthThread shutdown and wait until it completes.
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
            break;
         } catch (InterruptedException e) {
            logger.debug("Interrupted while waiting for VmAppHealthThread shutdown");
         }
      }
   }

   /**
    * Adds the pair of event and it's already identified eventHandler to the
    * processing queue.
    * @returns true if the pair was added to queue successfully.
    */
   public boolean enqEvent(Event event, IVmAppHealthEventHandler evtHandler) {
      AuAssert.check(event != null);
      AuAssert.check(evtHandler != null);
      return inputQ.offer(new QEntry(event, evtHandler));
   }

   /**
    * Main processing loop.
    */
   public void run() {
      logger.info("VmAppHealthThread loop starting");

      while (!stopRequested) {
         if (workQ.size() > 0) {
            // Refresh guest vars, build a list of cooked/done entries
            List<QEntry> cookedEntries = new ArrayList<QEntry>();
            for (QEntry entry : workQ) {
               if (entry.shouldRefresh()) {
                  entry.refreshGuestVars();
                  try {
                     Thread.sleep(GV_REFRESH_WAIT_PER_VM);
                  } catch (InterruptedException e) {
                     logger.debug("VmAppHealthThread interrupted");
                  }
               }
               if (entry.isDone()) {
                  cookedEntries.add(entry);
               }
            }
            // Process each cooked/done entry, remove it from workQ.
            for (QEntry entry : cookedEntries) {
               if (entry.gotGV()) {
                  try {
                     dispatchVmEvent(entry);
                  } catch (Throwable t) {
                     logger.error("Error processing event. " + entry.event.getClass().getName() +
                           "Got exception:" + t.getClass().getName() + ":" + t.getMessage());
                  }
               }
               workQ.remove(entry);
            }
            cookedEntries.clear();
         }
         try {
            checkAndDispatchCmsEvent();
         } catch (Throwable t) {
            logger.error("Error dispatching local Management Server event. " +
                  "Got exception:" + t.getClass().getName() + ":" + t.getMessage());
         }
         try {
            if (inputQ.size() > 0) {
               inputQ.drainTo(workQ);
            }
            if (workQ.size() > 0) {
               // There's work to do, but avoid stressing VC, ok to delay a bit refreshing
               // guest vars.

               // Blocking call, but throws InterrupedException (e.g. when thread is
               // interrupted for shutdown etc.).
               Thread.sleep(GV_REFRESH_WAIT_PER_VM_BATCH);
            }
            if (inputQ.size() == 0 && workQ.size() == 0) {
               // Block on no work, waiting at most for local HA status check interval.
               // Blocking call, but throws InterrupedException (e.g. when thread is
               // interrupted for shutdown etc.).
               QEntry entry = inputQ.poll(CmsAppHealthEventHelper.STATUS_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
               // Drain all entries to workQ from inputQ.
               if (entry != null) {
                  workQ.add(entry);
                  // Also drain any remaining entries from inputQ.
                  inputQ.drainTo(workQ);
               }
            }
         } catch (InterruptedException e1) {
            logger.debug("VmAppHealthThread interrupted");
         }
      }
      if (workQ.size() > 0) {
         logger.warn("Abandoning " + workQ.size() + " pending items in workQ.");
         workQ.clear();
      }
      if (inputQ.size() > 0) {
         logger.warn("Abandoning " + inputQ.size() + " pending items in inputQ.");
         inputQ.clear();
      }
      logger.info("VmAppHealthThread loop shutting down.");
   }
}
