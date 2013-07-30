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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.annotation.concurrent.GuardedBy;

import com.vmware.aurora.util.AuAssert;

/**
 * An {@link ExecutorService} that executes each submitted task using
 * one of possibly several pooled threads. Each task is assigned a priority,
 * so in general, tasks of high priority will be processed before tasks of low priority.
 *
 * <p>
 * For each priority, the maximal number of tasks that are allowed to execute at a time
 * can be specified in the constructor of <tt>PriorityThreadPoolExecutor</tt>, as an array
 * of integers, in the order of descending priority.
 * <tt>PriorityThreadPoolExecutor</tt> keeps track of how many tasks are being
 * executed for each priority. And {@link PriorityBlockingQueue} maintains a waiting queue
 * for each priority.
 *
 * <p>
 * When a task with priority information is submitted, it's put in to the waiting queue of that priority.
 * And when a worker thread is available, it will try to get a queued task. It checks
 * which priority has free capacity, i.e., the number of active tasks of that priority is
 * less than the maximal number of simultaneous
 * tasks, in the order of descending priority. There must be at least one worker thread, since
 * the thread that performs the check is free. The worker thread then tries to pick up one queued
 * task from the queue of that priority, if the queue is empty, then it will try other queues,
 * in the order of descending priority.
 *
 * <p>
 * When a queued task is retrieved, the number of active tasks for that priority is increased, and
 * the work thread begins to execute it. On completion, it decreases the number of active tasks
 * for that priority, and tries to pick up a new task.
 *
 * @author Xin Li (xinli)
 *
 */

class PriorityThreadPoolExecutor {

   // Number of queues of different priority.
   private final int queueCount;

   // Size of the thread pool for each priority.
   private final int[] poolSize;

   // Number of active tasks for each priority.
   // Must use AtomicIntegerArray, the keyword "volatile" cannot protect elements of an array
   private final AtomicIntegerArray activeTaskCount;

   // Not declared as private for the sake of test purpose
   final ThreadPoolExecutor executor;

   /*
    * Decrease active task count.
    */
   void afterExecute(int idx) {
      activeTaskCount.decrementAndGet(idx);
   }

   /*
    * Increase active task count.
    */
   @GuardedBy ("PriorityBlockingQueue.lock")
   void beforeExecute(int idx) {
      activeTaskCount.incrementAndGet(idx);
   }

   /**
    * Create a new <tt>PriorityThreadPoolExecutor</tt> with a given array of
    * max pool sizes for each priority.
    * @param poolSize
    */
   @SuppressWarnings("unchecked")
   public PriorityThreadPoolExecutor(int... poolSize) {
      AuAssert.check(poolSize.length == Priority.values().length);
      this.poolSize = poolSize;
      queueCount = poolSize.length;
      activeTaskCount = new AtomicIntegerArray(queueCount);

      PriorityBlockingQueue<?> workQueue = new PriorityBlockingQueue<PriorityFutureTask<?>>(poolSize.length);

      int totalPoolSize = 0;
      for (int size : poolSize) {
         if (size <= 0) {
            throw new IllegalArgumentException("Pool size cannot be less than or equal to zero");
         }
         totalPoolSize += size;
      }
      // The first and second argument to constructor of ThreadPoolExecutor, corePoolSize and
      // maximumPoolSize here are identical, but this is not required.
      executor = new ThreadPoolExecutor(totalPoolSize, totalPoolSize, 0, TimeUnit.SECONDS,
            (BlockingQueue<Runnable>)(BlockingQueue<?>)workQueue) {
         @Override
         protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            PriorityCallable<T> priorityCallable = (PriorityCallable<T>) callable;
            return new PriorityFutureTask<T>(priorityCallable, priorityCallable.getPriority());
         };

         @Override
         protected void afterExecute(Runnable r, Throwable t) {
            /*
             * Decrease active task count after a task is executed.
             * We cannot override beforeExecute to increase active task count,
             * because the worker thread first gets queued task, this is protected by a lock,
             * and then calls #beforeExecute. If we increase the active task count in #beforeExecute,
             * there will be short time window that multiple tasks are retrieved and removed from
             * queue but the active task count is not updated. So we protect task retrieval and
             * active task count update by the same lock.
             */
            PriorityThreadPoolExecutor.this.afterExecute(getTaskPriority(r).ordinal());
         }
      };

      workQueue.setPriorityThreadPoolExecutor(this);

      // Create and start all core threads, it MUST be called before first task is submitted,
      // otherwise some tasks will sent to worker threads directly, not through work queue,
      // so the active task counter in <tt>activeTaskCount</tt> will be wrong.
      executor.prestartAllCoreThreads();
   }

   private Priority getTaskPriority(Runnable r) {
      AuAssert.check(r instanceof PriorityFutureTask);
      @SuppressWarnings("rawtypes")
      PriorityFutureTask task = (PriorityFutureTask)r;
      return task.getPriority();
   }

   // Return the highest priority whose # of active execution threads is
   // less than the size of corresponding thread pool.
   @GuardedBy ("PriorityBlockingQueue.lock")
   int getNextPriorityToProcess() {
      for (int i = 0; i < queueCount; ++i) {
         if (activeTaskCount.get(i) < poolSize[i]) {
            return i;
         }
      }
      AuAssert.check(false);
      return 0;
   }

   /**
    * Submits a value-returning task for execution and returns a Future representing
    * the pending results of the task. The Future's get method will return the task's
    * result upon successful completion.
    * @param priority the priority of task
    * @param task the task to submit
    * @return a Future representing pending completion of the task
    * @see ExecutorService#submit(Callable)
    */
   public <T> Future<T> submit(final Priority priority, final Callable<T> task) {
      return executor.submit(new PriorityCallable<T>() {
         @Override
         public Priority getPriority() {
            return priority;
         }
         @Override
         public
         T call() throws Exception {
            return task.call();
         }
      });
   }

   /**
    * Attempts to stop all actively executing tasks, halts the
    * processing of waiting tasks, and returns a list of the tasks
    * that were awaiting execution. These tasks are drained (removed)
    * from the task queue upon return from this method.
    * @see ThreadPoolExecutor#shutdownNow()
    */
   public List<Runnable> shutdownNow() {
      return executor.shutdownNow();
   }

   /**
    * Initiates an orderly shutdown in which previously submitted tasks
    * are executed, but no new tasks will be accepted. Invocation has
    * no additional effect if already shut down.
    * @see ThreadPoolExecutor#shutdown()
    */
   public void shutdown() {
      executor.shutdown();
   }
}
