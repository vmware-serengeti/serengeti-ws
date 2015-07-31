/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.vc;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;

import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcevent.VcEventUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.Task;
import com.vmware.vim.binding.vim.TaskInfo;
import com.vmware.vim.binding.vim.TaskInfo.State;
import com.vmware.vim.binding.vim.fault.TaskInProgress;
import com.vmware.vim.binding.vim.host.DatastoreBrowser.SearchResults;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@SuppressWarnings("serial")
public class VcTask extends VcObjectImpl {
   /**
    * All VC tasks. The "targetClass" is the {@link VcObject} class
    * corresponding to the (new) {@link ManagedObject} as the result of
    * the task. If there is no result, set it to "Void" and
    * {@link VcTask.getResult()} will always return null.
    */
   static public enum TaskType {
      PowerOn   (Void.class),
      PowerOff  (Void.class),
      ReconfigVm (Void.class),
      CreateVm  (VcVirtualMachine.class),
      CloneVm   (VcVirtualMachine.class),
      RelocateVm (Void.class),
      MigrateVm (Void.class),
      Snapshot  (VcSnapshot.class),
      RemoveSnap (Void.class),
      RevertSanp (Void.class),
      DestroyVm (Void.class),
      DestroyRp (Void.class),
      CopyFile (Void.class),
      CopyVmdk (Void.class),
      DeleteVmdk (Void.class),
      DeleteFile (Void.class),
      MoveFile (Void.class),
      SearchFile (SearchResults.class),
      PromoteDisks (Void.class),
      ReconfigCluster (Void.class),
      DeleteFolder (Void.class),
      TurnOnFT (Void.class),
      TurnOffFT (Void.class),
      EnableFT (Void.class),
      DisableFT (Void.class),
      CreateForkChild(VcVirtualMachine.class);

      private Class<?> targetClass;
      private TaskType(Class<?> clazz) {
         targetClass = clazz;
      }
      public Class<?> getTargetClass() {
         return targetClass;
      }
   }

   // Enforce runtime check for task return types
   private TaskType type;

   private VcObject parent;
   private IVcTaskCallback callback;
   private boolean callbackInvoked = false;

   private TaskInfo.State state;
   private VcObject result = null;
   private Object taskResult = null;
   private int eventChainId;            // To match tasks with events.

   /* Keep an eye on VC PC notifications - wake up periodically. */
   private static final long defaultWaitIntervalNanos = TimeUnit.SECONDS.toNanos(20);
   private static final long badTaskWaitIntervalNanos = TimeUnit.SECONDS.toNanos(1);
   private static final int maxRetryNum = 5;

   private long lastWaitTimeNanos;      // Time taken by last iteration of wait.
   private long totalWaitTimeNanos;     // Total task wait time.
   private long startTimeNanos;         // Task initiation time.
   private long completionTimeNanos;    // Completion time.
   private boolean isWaiting;           // Stale if read without synchronized.
   private boolean notified;            // Did VcEventListener notify us?
   private boolean needsPoll;           // If need to poll for completions.
   private int progress;                // Current progress.
   private String name;
   /**
    * @return if the task has been completed successfully
    */
   public boolean taskCompleted() {
      return state.equals(State.success);
   }

   /**
    * @return if the task has failed
    */
   public boolean taskFailed() {
      return state.equals(State.error);
   }

   public TaskInfo.State getState() {
      return state;
   }

   /**
    * @return the result of the task if available
    */
   public VcObject getResult() {
      return result;
   }

   /**
    * @return VC's task result
    */
   protected Object getTaskResult() {
      return taskResult;
   }

   /**
    * @return VcTask type
    */
   public TaskType getType() {
      return type;
   }

   /**
    * @return current progress
    */
   public int getProgress() {
      return progress;
   }

   protected void setProgress(int progress) {
      this.progress = progress;
   }

   /**
    * @return VcTask eventChainId
    */
   public int getEventChainId() {
      return eventChainId;
   }

   /**
    * @return startTimeNanos
    */
   public long getStartTimeNanos() {
      return startTimeNanos;
   }

   /**
    * @return completionTimeNanos;
    */
   public long getCompletionTimeNanos() {
      return completionTimeNanos;
   }

   /**
    * Called by VcTaskMgr upon observing task completion.
    */
   protected void setCompletionTimeNanos(long timeNanos) {
      completionTimeNanos = timeNanos;
   }

   public String getName() {return  name; }
   @Override
   protected void update(ManagedObject mo) {
      Task task = (Task)mo;
      TaskInfo info = task.getInfo();
      state = info.getState();
      eventChainId = info.getEventChainId();
      name = task.getInfo().getKey();
   }

   /**
    * Delegation of a VC task object. New tasks could only be created
    * by VcTaskMgr.execute().
    *
    * @param type type of the task
    * @param moRef managed object reference to the task
    * @param parent VC object of this task
    * @param callback (optional) task call back
    */
   protected VcTask(TaskType type, ManagedObjectReference moRef,
         VcObject parent, IVcTaskCallback callback) throws Exception {
      super(MoUtil.getManagedObject(moRef));
      this.type = type;
      this.parent = parent;
      this.callback = callback;
      startTimeNanos = System.nanoTime();
      completionTimeNanos = 0;
      isWaiting = false;
      notified = false;
      needsPoll = isBadTask();
      AuAssert.check(parent == null || type == TaskType.Snapshot);
      update(getManagedObject());
   }

   public VcTask(TaskType type, ManagedObjectReference moRef,
         IVcTaskCallback callback) throws Exception {
      this(type, moRef, null, callback);
   }

   /**
    * Used by junit test code to force polling.
    */
   public void setNeedsPoll() {
      needsPoll = true;
   }

   /**
    * Attempts to perform periodic wait completion verification to keep
    * VC in check. normalCompletion=true means wait() was not interrupted.
    * However, java makes it impossible to tell if wait() was notified or
    * finished because of timeout expiration. Use notified flag for that.
    *
    * @param normalCompletion
    * @throws Exception
    */
   private void verifyWaitCompletion(boolean normalCompletion) throws Exception {
      if (normalCompletion) {
         if (!notified          &&
             !needsPoll         &&
             (state == State.success || state == State.error)) {
            /*
             * Waited for a long time, did not get a Property Collector notification,
             * but the task finished. Did VC drop a property collector update? Since
             * the update might still be in progress, this is just a warning.
             */
            logger.warn("Dropped VC taskInfo.state update? " + type + " : " +
                  getId() + " wait: " +
                  TimeUnit.NANOSECONDS.toSeconds(totalWaitTimeNanos) + "s");
            if (logger.isEnabledFor(Level.TRACE)) {
               VcEventUtil.dumpRecentTasks();
            }
         }
      }
   }

   /**
    * Not all tasks are created equal. Contrary to VC spec, not all tasks have
    * a lifetime independent of sessions. Some read-only tasks don't follow the
    * standard global naming convention, have session specific names, don't get
    * accumulated in TaskManager.recentTask[] and likely are destroyed on session
    * termination. Can't use normal PC "recentTask" monitoring for such tasks.
    * At this time, badness is limited to SearchFile which is used only during
    * VC registration so we just poll. If it spreads, we might need to build a
    * PC + ContainerView monitoring mechanism to watch sets of bad tasks.
    * @return
    */
   public boolean isBadTask() {
      if (getMoRef().getValue().startsWith("task-")) {
         return false;
      }
      AuAssert.check(getMoRef().getValue().startsWith("session"));
      if (type != TaskType.SearchFile) {
         logger.warn("Unknown bad task: " + type);
      }
      return true;
   }

   /**
    * For normal task, we rely on VcEventListener completion notifications
    * and use a long wait interval. For bad tasks, we must poll with a
    * shorter interval.
    *
    * @return wait timeout
    */
   long getWaitIntervalNanos() {
      return needsPoll ? badTaskWaitIntervalNanos : defaultWaitIntervalNanos;
   }

   /**
    * Try to parse and log common VC task errors.
    * @param task
    * @throws Exception
    */
   private void logTaskError(Task task) throws Exception {
      logger.warn("task " + type + " got error");
      if (task.getInfo().getError() instanceof TaskInProgress) {
         TaskInProgress e = (TaskInProgress)task.getInfo().getError();
         Task inProgressTask = MoUtil.getManagedObject(e.getTask());
         logger.warn("Task already in progress: ");
         VcEventUtil.dumpTask(inProgressTask);
      } else {
         logger.warn(task.getInfo().getError());
      }
   }

   /**
    * "Bad" task completions are detected via poll. Tell VcContext to fire
    * task completion events, if any, upon detection of such a completion.
    * @throws Exception
    */
   private void assistBadTaskCompletion() throws Exception {
      if (isBadTask()) {
         VcContext.badTaskCompleted(this);
      }
   }

   /**
    * Invoke the callback functions by the task event listener or
    * the task caller.
    * @sync invoke sync callback, only called by the task caller.
    * @throws Exception
    */
   private void invokeCallbacks(boolean sync) throws Exception {
      AuAssert.check(Thread.holdsLock(this));
      if (callback == null) {
         return;
      }
      // callback should be invoked successfully exactly once
      if (!callbackInvoked) {
         callback.completeCB(this);
         callbackInvoked = true;
      }
      if (sync) {
         callback.syncCB();
      }
   }

   /**
    * Blocking wait for task completion. Wait on task's monitor until
    * a notification of a task completion by VcEventListener thread.
    *
    * @return the VC object as a result of this task.
    * @exception Exception failure
    */
   public synchronized VcObject waitForCompletion() throws Exception {
      totalWaitTimeNanos = 0;
      Exception catchedException = null;
      for (int i = 0; i < maxRetryNum; i++) {
         try {
            return waitForCompletionIntenal();
         } catch (Exception e) {
            catchedException = e;
            if (VcUtil.isRecoverableException(e)) {
               wait(TimeUnit.NANOSECONDS.toMillis(badTaskWaitIntervalNanos));
               continue;
            }
            throw e;
         }
      }
      throw catchedException;
   }

   private VcObject waitForCompletionIntenal() throws Exception {
      Task task = getManagedObject();
      StatsType oldSrc = Profiler.pushInc(StatsType.VC_TASK_WAIT, getType());
      long lastWaitStartedNanos ;
      long waitFinishedNanos = System.nanoTime();

      state = task.getInfo().getState();
      while (state != State.success) {
         boolean normalWaitCompletion = false; // wait() not interrupted.

         switch (state) {
         case success:
            break;
         case error:
            completionTimeNanos = waitFinishedNanos;
            logTaskError(task);
            assistBadTaskCompletion();
            throw task.getInfo().getError();
         case queued:
         case running:
            lastWaitStartedNanos = System.nanoTime();
            try {
               /* Wait for a completion notification from VcEventListener. */
               isWaiting = true;
               wait(TimeUnit.NANOSECONDS.toMillis(getWaitIntervalNanos()));
               normalWaitCompletion = true;
            } catch (InterruptedException e) {
               /* Continue after checks. */
            } finally {
               isWaiting = false;
               waitFinishedNanos = System.nanoTime();
               lastWaitTimeNanos = waitFinishedNanos - lastWaitStartedNanos;
               totalWaitTimeNanos += lastWaitTimeNanos;
            }
            break;
         default:
            AuAssert.check(false);
         }
         /*
          * TODO: taskInfo reload might not be always necessary. If the task has
          * no result, callers likely care only about success/error state which
          * could be obtained from Event Listener without talking to VC.
          */
         state = task.getInfo().getState();
         verifyWaitCompletion(normalWaitCompletion);
      }

      AuAssert.check(taskCompleted());
      logger.debug("task " + type + "completed");
      completionTimeNanos = waitFinishedNanos;
      assistBadTaskCompletion();

      if (!(type.getTargetClass() == Void.class)) {
         taskResult = task.getInfo().getResult();
         if (taskResult instanceof ManagedObjectReference) {
            if (type == TaskType.Snapshot) {
               VcVirtualMachineImpl vm = (VcVirtualMachineImpl)parent;
               vm.update();
               result = vm.getSnapshot((ManagedObjectReference)taskResult);
            } else {
               result = VcCache.load((ManagedObjectReference)taskResult);
               // Temp solution to refresh parent resource pool for a new VM.
               // We should refresh during event processing, but current event processing code
               // lives in CMS project, which won't be used by Serengeti.
               if (type == TaskType.CloneVm || type == TaskType.CreateVm) {
                  VcVirtualMachineImpl vm = (VcVirtualMachineImpl)result;
                  vm.refreshRP();
               }
            }
            AuAssert.check(type.getTargetClass().isInstance(result));
         } else if (taskResult != null) {
            AuAssert.check(type.getTargetClass().isInstance(taskResult));
         }
      }

      invokeCallbacks(true);
      Profiler.pop(oldSrc);
      return result;
   }

   /**
    * Called by TaskMgr in the context of VcEventListener thread to notify of
    * a task termination (either success or error). We currently reload
    * taskInfo to get final task state information.
    *
    * @throws Exception
    */
   public synchronized void taskNotify(TaskInfo.State state) throws Exception {
      if (!isWaiting) {
         /* waitForCompletion() never called? */
         logger.debug("Spurious notify");
      }
      this.state = state;
      notified = true;
      notifyAll();
      invokeCallbacks(false);
   }
}
