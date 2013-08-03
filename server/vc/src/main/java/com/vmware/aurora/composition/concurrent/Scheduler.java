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

package com.vmware.aurora.composition.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.google.gson.internal.Pair;
import com.vmware.aurora.util.AuAssert;

/**
 * A helper class that provides methods to execute multiple stored procedures in
 * parallel by pooled threads.
 * 
 * @author Xin Li (xinli)
 * 
 */

@ThreadSafe
public class Scheduler {

   public interface ProgressCallback {
      /**
       * Execution progress update callback interface. When a stored procedure
       * is finished (either successfully or unsuccessfully), this callback
       * method will be called with three arguments. This method should return
       * as soon as possible.
       * 
       * @param sp
       *           The stored procedure finished
       * @param result
       *           Execution result
       * @param compensate
       *           Whether the stored procedure is a compensation stored
       *           procedure
       * @param total
       *           If <code>compensate</code> is false, <code>total</code> is
       *           the number of stored procedures submitted with the stored
       *           procedure just finished; otherwise, <code>total</code> is the
       *           number of compensation stored procedures to execute, and
       *           might be less than the number of compensation stored
       *           procedures submitted with the compensation stored procedure
       *           just finished.
       */
      public void progressUpdate(Callable<Void> sp, ExecutionResult result,
            boolean compensate, int total);
   }

   private static PriorityThreadPoolExecutor executor = null;

   private static class StoredProcedureCallable implements Callable<Void> {
      private final Callable<Void> sp;
      private final Semaphore semaphore;
      private final ConcurrentLinkedQueue<Integer> queue;
      private final int idx;

      private StoredProcedureCallable(Callable<Void> sp, Semaphore semaphore,
            ConcurrentLinkedQueue<Integer> queue, int idx) {
         this.sp = sp;
         this.semaphore = semaphore;
         this.queue = queue;
         this.idx = idx;
      }

      @Override
      public Void call() throws Exception {
         try {
            sp.call();
         } finally {
            queue.add(Integer.valueOf(idx));
            semaphore.release();
         }
         return null;
      }
   }

   /**
    * Initilize the scheduler and its thread pool. Must be called before
    * submitting first task.
    * 
    * @param poolSize
    */
   @GuardedBy("this")
   synchronized public static void init(int... poolSize) {
      if (executor == null) {
         executor = new PriorityThreadPoolExecutor(poolSize);
      }
   }

   /**
    * Attempts to terminate all threads in the pool.
    * 
    * @param immediate
    *           if true, all executing threads will be interrupted; otherwise
    *           wait them to finish
    * 
    */
   @GuardedBy("this")
   synchronized public static void shutdown(boolean immediate) {
      if (executor == null)
         return;

      if (immediate) {
         executor.shutdownNow();
      } else {
         executor.shutdown();
      }
      executor = null;
   }

   private static ExecutionResult[] executeStoredProcedures(Priority priority,
         Callable<Void>[] storedProcedures, ProgressCallback callback,
         boolean compensate) throws InterruptedException {
      AuAssert.check(storedProcedures != null && storedProcedures.length > 0);
      int len = storedProcedures.length;
      ExecutionResult[] ret = new ExecutionResult[len];
      @SuppressWarnings("unchecked")
      Future<Void>[] futures = new Future[len];
      Semaphore semaphore = new Semaphore(0);
      ConcurrentLinkedQueue<Integer> queue =
            new ConcurrentLinkedQueue<Integer>();
      int total = 0;

      for (int i = 0; i < len; ++i) {
         if (storedProcedures[i] != null) {
            futures[i] =
                  executor.submit(priority, new StoredProcedureCallable(
                        storedProcedures[i], semaphore, queue, i));
            ++total;
         }
      }

      int finished = 0;
      while (finished < total) {
         semaphore.acquire();
         ++finished;
         int idx = queue.remove().intValue();
         Throwable throwable = null;
         try {
            futures[idx].get();
         } catch (ExecutionException ex) {
            throwable = ex.getCause();
         }
         ret[idx] = new ExecutionResult(true, throwable);
         if (callback != null) {
            callback.progressUpdate(storedProcedures[idx], ret[idx],
                  compensate, total);
         }
      }

      for (int i = 0; i < len; ++i) {
         if (futures[i] == null) {
            ret[i] = new ExecutionResult(false, null);
         }
      }

      return ret;
   }

   /**
    * Submit stored procedures to execute. Each stored procedure will be
    * executed in a separate transaction ({@link Transaction}) by a thread in
    * the thread pool.
    * 
    * @param priority
    *           Task priority
    * @param storedProcedures
    *           Tasks to execute.
    * @param callback
    *           Progress update callback, might be null.
    * @return An array of <tt>ExecutionResult</tt> that encapsulates execution
    *         result. The length of the array is the same as input parameter
    *         <tt>storedProcedures</tt>, and the execution result of the stored
    *         procedure in the array <tt>storeProcedures</tt> can be fetched in
    *         the return array using the same subscript.
    * @throws InterruptedException
    *            if execution is interrupted by another thread
    */
   @CheckReturnValue
   public static ExecutionResult[] executeStoredProcedures(Priority priority,
         Callable<Void>[] storedProcedures, ProgressCallback callback)
         throws InterruptedException {
      return executeStoredProcedures(priority, storedProcedures, callback,
            false);
   }

   /**
    * Submit stored procedures to execute. Each stored procedure will be
    * executed in a separate transaction ({@link Transaction}) by a thread in
    * the thread pool.
    * 
    * If the total number of failed stored procedures exceeds
    * <tt>numberOfFailuresAllowed</tt>, the corresponding compensation stored
    * procedures will be executed in a new transaction.
    * 
    * @param priority
    *           Task priority
    * @param storedProcedures
    *           Stored procedures to execute and their corresponding
    *           compensation stored procedures. The stored procedures are the
    *           first element of the array element, and the second element of
    *           each array element is the corresponding compensation stored
    *           procedure for the first element. The compensation stored
    *           procedures will be executed if the total number of failed store
    *           procedures exceeds <tt>numberOfFailuresAllowed</tt>, even for
    *           failed stored procedures.
    * @param numberOfFailuresAllowed
    *           Number of failed stored procedures that are allowed, otherwise,
    *           the compensation stored procedures will be executed, even for
    *           failed stored procedures.
    * @param callback
    *           Progress update callback, might be null.
    * @return An array of pair of two <tt>ExecutionResult</tt>, the first is for
    *         stored procedure, and the other is for the corresponding
    *         compensation stored procedure, both are not null, but the
    *         <tt>finished</tt> part of second might be false, if the
    *         compensation stored procedure is not executed.
    * @throws InterruptedException
    *            if execution is interrupted by another thread
    */

   @SuppressWarnings("unchecked")
   @CheckReturnValue
   public static Pair<ExecutionResult, ExecutionResult>[] executeStoredProcedures(
         Priority priority,
         Pair<? extends Callable<Void>, ? extends Callable<Void>>[] storedProcedures,
         int numberOfFailuresAllowed, ProgressCallback callback)
         throws InterruptedException {
      AuAssert.check(storedProcedures != null && storedProcedures.length > 0
            && numberOfFailuresAllowed >= 0
            && numberOfFailuresAllowed <= storedProcedures.length);

      int len = storedProcedures.length;
      Pair<ExecutionResult, ExecutionResult>[] ret = new Pair[len];
      Callable<Void>[] forwardExecution = new Callable[len], rollbackExecution =
            null;
      ExecutionResult[] forwardExecutionResult = null, rollbackExecutionResult =
            null;

      for (int i = 0; i < len; ++i) {
         forwardExecution[i] = storedProcedures[i].first;
      }
      forwardExecutionResult =
            executeStoredProcedures(priority, forwardExecution, callback, false);

      boolean compensateAll =
            ((len - Util.getNumberOfSuccessfulExecution(forwardExecutionResult)) > numberOfFailuresAllowed);
      rollbackExecution = new Callable[len];

      for (int i = 0; i < len; ++i) {
         // We should compensate only when <code>compensateAll</tt> is true
         // but since transaction layer now won't rollback a failed transaction automatically,
         // so we have to compensate failed transactions by ourselves.
         if (compensateAll || forwardExecutionResult[i].throwable != null) {
            rollbackExecution[i] = storedProcedures[i].second;
         }
      }

      // Call compensation stored procedures
      rollbackExecutionResult =
            executeStoredProcedures(priority, rollbackExecution, callback, true);

      for (int i = 0; i < len; ++i) {
         ret[i] =
               new Pair<ExecutionResult, ExecutionResult>(
                     forwardExecutionResult[i], rollbackExecutionResult[i]);
      }

      return ret;
   }
}
