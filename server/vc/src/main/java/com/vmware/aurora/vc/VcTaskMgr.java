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
 * <code>VcTaskMgr</code> manages pending VC tasks. Pending tasks are waiting
 * on their respective monitors. VcTaskMgr is responsible for the communication
 * of task initiations to VC and task completion notifications from VC (via
 * EventListener).
 *
 * The locking discipline for pending tasks is somewhat complicated by the
 * fact that task moRef is not available on task start. It is sent to the
 * caller by VC and, in theory, could arrive after short tasks already
 * completed including a completion notification.
 *
 * To avoid losing a task completion notification from Event Listener in this
 * race window, we use this locking strategy:
 *
 * Task initiation:
 * - acquire a read lock before initiating any VC task
 * - hold this lock until we get task moRef from VC
 * - concurrent task initiations are ok
 *
 * Task completion:
 * - first check if a task with a given moRef is pending
 * - if not, acquire a write lock and repeat the check to address the race
 * - if task not found, the notification does not apply to "our" tasks
 * - write lock is never held across network operations
 *
 * Pending tasks are kept in ConcurrentHashMap to protect concurrent task
 * initiations.
 *
 * @since   0.7
 * @version 0.7
 * @author Boris Weissman
 */
package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.TaskFinishedEvent;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.TaskUpdateProgressEvent;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.ResourcePoolEvent;
import com.vmware.vim.binding.vim.event.VmEvent;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public class VcTaskMgr {
   /**
    * <code>IVcTaskBody</code> must be implemented by all vc tasks - this is
    * the only way to safely specify a task action.
    */
   public static abstract class IVcTaskBody {
      public IVcTaskBody() {
      }
      abstract public VcTask body() throws Exception;
   }

   /**
    * <code>IVcPseudoTaskBody</code> must be implemented by operations that
    * should have been vc tasks, but unfortunately aren't. For now, don't
    * expose VcPseudoTask objects outside this module.
    * @return moref of the target object, if any
    */
   public static abstract class IVcPseudoTaskBody {
      public IVcPseudoTaskBody() {
      }
      abstract public ManagedObjectReference body() throws Exception;
   }

   /**
    * <code> VcPseudoTask names vc mutation operations that should have been
    * proper vc tasks, but aren't. It also captures the information about the
    * expected task "completion" event and its target moref. This serves the
    * purpose of event chains of real vc tasks to distinguish internally generated
    * events from external ones.
    */
   protected class VcPseudoTask {
      String name;                      // Pseudo-task name.
      VcEventType eventType;            // Event to expect.
      ManagedObjectReference moRef;     // Event target moref.

      protected VcPseudoTask(String name, VcEventType eventType,
            ManagedObjectReference moRef) {
         this.name = name;
         this.eventType = eventType;
         this.moRef = moRef;
      }

      protected String getName() {
         return name;
      }

      protected VcEventType getEventType() {
         return eventType;
      }

      protected ManagedObjectReference getMoRef() {
         return moRef;
      }
   }

   /**
    * A handler for TaskFinished pseudo-event.
    */
   public class VcTaskFinishedHandler implements IVcEventHandler {
      /**
       * Called by VcEventListener on observing a task termination state transition
       * (success and error alike). Returns true if the task was one of ours, false
       * otherwise. For our tasks, wakes up the waiting thread.
       *
       * @param  type      event type
       * @param  taskEvent instance of TaskFinishedEvent
       * @return boolean   true for aurora tasks
       * @throws Exception
       */
      public boolean eventHandler(VcEventType type, Event taskEvent) throws Exception {
         TaskFinishedEvent event = (TaskFinishedEvent)taskEvent;
         ManagedObjectReference taskMoRef = event.getTaskMoRef();
         VcTask task = taskFinished(taskMoRef);

         if (task == null) {
            /* Repeat the check with the write lock. */
            rwLock.writeLock().lock();
            task = taskFinished(taskMoRef);
            rwLock.writeLock().unlock();
         }
         if (task != null) {
            logger.info("Task completed: " + task.getType() + " " + task.getMoRef().getValue());
            task.taskNotify(event.getTaskState());
         } else {
            logger.debug("Foreign task: " + taskMoRef);
         }
         return task != null;
      }
   }

   /**
    * A handler for TaskUpdateProgress pseudo-event.
    */
   public class VcTaskUpdateProgressHandler implements IVcEventHandler {
      public boolean eventHandler(VcEventType type, Event e) throws Exception {
         TaskUpdateProgressEvent taskEvent = (TaskUpdateProgressEvent)e;
         ManagedObjectReference taskMoRef = taskEvent.getTaskMoRef();
         synchronized(VcTaskMgr.this) {
            VcTask task = pendingTasks.get(taskMoRef);
            if (task != null) {
               logger.info("task: " + task.getMoRef() + " progress: " + taskEvent.getProgress());
               task.setProgress(taskEvent.getProgress());
               return true;
            }
            return false;
         }
      }
   }

   private static Logger logger = Logger.getLogger(VcTaskMgr.class);
   /* Fair RW lock policy: younger readers cannot bypass an older writer. */
   private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
   private static final long trimFinishedTasksNanos = TimeUnit.MINUTES.toNanos(10); // 10min.

   /*
    * These four are protected by VcTaskMgr monitor. These maps are used to
    * look up VcTask by different keys depending on the code path:
    *   moRef:                  on task completion (via PC) for pending tasks
    *   eventChainId(int):      on event arrival for pending and finished tasks
    *   time order:             when trimming finishedTasks
    */
   private HashMap<ManagedObjectReference, VcTask> pendingTasks;
   private HashMap<Integer, VcTask> pendingEventChains;
   private HashMap<Integer, VcTask> finishedEventChains;
   private LinkedList<VcTask> finishedTasks; // In order of completions.
   private VcTaskFinishedHandler taskFinishedHandler;
   private VcTaskUpdateProgressHandler taskUpdateProgressHandler;

   /*
    * expectedEvents for pseudo-tasks. Maps expected events to pseudo-tasks
    * that will trigger them.
    */
   private HashMap<VcEventType, List<VcPseudoTask>> expectedEvents;

   public VcTaskMgr() {
      pendingTasks = new HashMap<ManagedObjectReference, VcTask>();
      pendingEventChains = new HashMap<Integer, VcTask>();
      finishedEventChains = new HashMap<Integer, VcTask>();
      finishedTasks = new LinkedList<VcTask>();
      taskFinishedHandler = new VcTaskFinishedHandler();
      taskUpdateProgressHandler = new VcTaskUpdateProgressHandler();
      expectedEvents = new HashMap<VcEventType, List<VcPseudoTask>>();
   }

   public IVcEventHandler getTaskFinishedHandler() {
      return taskFinishedHandler;
   }

   public IVcEventHandler getTaskUpdateProgressHandler() {
      return taskUpdateProgressHandler;
   }

   /**
    * Safely execute the task specified by IVcTaskBody object. This is the
    * only safe way to execute tasks to avoid lost completion notifications.
    * A read lock is held until a task moRef is received from VC.
    *
    * @param  taskObj   task object
    * @return VcTask    task handle
    * @throws Exception
    */
   public VcTask execute(IVcTaskBody taskObj) throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      VcTask task = null;
      rwLock.readLock().lock();
      try {
         task = taskObj.body();  // VC access (slow).
         Profiler.inc(StatsType.VC_TASK_EXEC, task.getType());
         taskStarted(task);
      } finally {
         rwLock.readLock().unlock();
      }
      return task;
   }

   /**
    * Update various maps to record a pending task.
    * @param task
    */
   private synchronized void taskStarted(VcTask task) {
      pendingTasks.put(task.getMoRef(), task);
      pendingEventChains.put(task.getEventChainId(), task);
      logger.info("Task started: " + task.getType() + " " + task.getMoRef().getValue() +
            " eventChainId: " + task.getEventChainId());
   }

   /**
    * Update maps to record a finished task. A finished task is moved from
    * pendingTasks to finishedTasks where it stays a few minutes until the
    * data structure is trimmed.
    * @param taskMoRef  task handle
    * @return VcTask    null if we don't know about this task
    */
   private synchronized VcTask taskFinished(ManagedObjectReference taskMoRef) {
      long now = System.nanoTime();
      VcTask task = pendingTasks.remove(taskMoRef);
      if (task != null) {
         VcTask t = pendingEventChains.remove(task.getEventChainId());
         AuAssert.check(task == t);
         t.setCompletionTimeNanos(now);
         finishedEventChains.put(task.getEventChainId(), t);
         finishedTasks.add(t);
      }
      trimFinishedTasks(now);
      return task;
   }

   /**
    * Delete finished tasks older than trimFinishedTasksMillis.
    * @param now millis
    */
   private void trimFinishedTasks(long now) {
      while (true) {
         VcTask oldestTask = finishedTasks.getFirst();
         if (oldestTask != null &&
             (now - oldestTask.getCompletionTimeNanos()) > trimFinishedTasksNanos) {
            finishedTasks.removeFirst();
            finishedEventChains.remove(oldestTask.getEventChainId());
         } else {
            break;
         }
      }
   }

   /**
    * Callback executed by the low-level event dispatch mechanism before
    * the handler is fired for a newly arrived vim event to give VcTaskMgr a
    * chance to clean up its data structures. Returns true if a passed event
    * was caused by any activity external to cms and false for a cms induced
    * event. 2 cases for cms events:
    * (1) event caused by a proper vc task
    *     check event chains for all pending tasks and tasks finished within
    *     trimFinishedTasksMillis in the past (many minutes).
    * (2) event caused by a pseudo-task
    *     check expected events triggered by pseudo-tasks
    * @param event      vim event
    * @return true      if non-cms event
    */
   public synchronized boolean eventFireCallback(Event event) {
      VcTask task = pendingEventChains.get(event.getChainId());
      VcPseudoTask pTask = null;
      /* First, check real vc tasks. */
      if (task == null) {
         task = finishedEventChains.get(event.getChainId());
      }
      /* Now check pseudo-tasks. */
      if (task == null) {
         VcEventType vcEventType = VcEventType.getInstance(event);
         ManagedObjectReference moRef = null;
         if (event instanceof VmEvent && event.getVm() != null) {
            moRef = event.getVm().getVm();
            pTask = removePseudoTask(vcEventType, moRef);
         } else if (event instanceof ResourcePoolEvent) {
            ResourcePoolEvent rpEvent = (ResourcePoolEvent)event;
            moRef = VcResourcePoolImpl.getEventTargetMoRef(rpEvent);
            pTask = removePseudoTask(vcEventType, moRef);
         }
      }
      return task == null && pTask == null;
   }

   /**
    * Returns a pending task corresponding to passed moRef, or null if
    * no such task is pending.
    * @param taskMoRef
    * @return VcTask
    */
   public synchronized VcTask getPendingTask(ManagedObjectReference taskMoRef) {
      return pendingTasks.get(taskMoRef);
   }

   /**
    * A weakly consistent dump that may or may not include tasks added during
    * the dump.
    */
   public void dumpTasks() {
      logger.info("Pending tasks:");
      for (VcTask task : pendingTasks.values()) {
         logger.info(task);
      }
      logger.info("Finished tasks:");
      for (VcTask task : finishedTasks) {
         logger.info(task);
      }
   }

   /**
    * A way to call "VC pseudo-tasks". Pseudo-tasks should have been tasks, but
    * aren't for some strange reasons. Example: ResourcePool.updateConfig().
    * @param name       pseudo-task name
    * @param eventType  expected completion event
    * @param refId      target for expected event
    * @param obj        code to execute
    * @return moref returned by the task
    * @throws Exception
    */
   public ManagedObjectReference execPseudoTask(String name,
         VcEventType eventType, ManagedObjectReference moRef,
         IVcPseudoTaskBody obj) throws Exception {
      AuAssert.check(eventType != null);
      AuAssert.check(VcContext.isInTaskSession());
      VcPseudoTask task = pseudoTaskStarted(name, eventType, moRef);
      Profiler.inc(StatsType.VC_TASK_EXEC, task.getName());
      ManagedObjectReference res = null;
      try {
         res = obj.body();
      } catch (Exception e) {
         /**
          * Assume that if task execution triggered an exception, we will
          * not receive a completion event from vc, so remove it. On normal
          * completion, the event is removed from an expected queue upon arrival.
          */
         pseudoTaskFailed(task);
         /*
          * Asynchronously refresh the target state
          * if an exception has been generated as task
          * may already have changed the target state.
          */
         VcCache.refreshAll(moRef);
         throw e;
      }
      return res;
   }

   /**
    * Record starting of a pseudo-task: make a note of the expected event.
    */
   private synchronized VcPseudoTask pseudoTaskStarted(String name,
         VcEventType eventType, ManagedObjectReference moRef) {
      AuAssert.check(eventType != null);
      VcPseudoTask task = new VcPseudoTask(name, eventType, moRef);
      List<VcPseudoTask> tasks = expectedEvents.get(eventType);
      if (tasks == null) {
         tasks = new ArrayList<VcPseudoTask>();
         expectedEvents.put(eventType, tasks);
      }
      tasks.add(task);
      StringBuilder buf = new StringBuilder("Pseudo-task started:")
         .append(name)
         .append(" moref: ")
         .append(moRef)
         .append(" expectedEvent: ")
         .append(eventType);
      logger.info(buf);
      return task;
   }

   /**
    * Remove an expected event upon pseudo-task failure.
    * @param task
    */
   private synchronized void pseudoTaskFailed(VcPseudoTask task) {
      List<VcPseudoTask> tasks = expectedEvents.get(task.getEventType());
      boolean res = tasks.remove(task);
      if (!res) {
         logger.warn("missing pseudoTask: " + task.getName());
      }
   }

   /**
    * Removes and returns the first pending pseudo-task with matching eventType
    * and moRef.
    * @param eventType
    * @param moRef
    * @return VcPseudoTask
    */
   private synchronized VcPseudoTask removePseudoTask(VcEventType eventType,
         ManagedObjectReference moRef) {
      List<VcPseudoTask> tasks = expectedEvents.get(eventType);
      if (tasks != null) {
         for (VcPseudoTask task : tasks) {
            if (task.getMoRef().equals(moRef)) {
               boolean res = tasks.remove(task);
               AuAssert.check(res);
               return task;
            }
         }
      }
      return null;
   }
}
