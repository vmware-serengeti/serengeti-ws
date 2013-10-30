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
package com.vmware.aurora.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsEntry;
import com.vmware.aurora.stats.StatsType;

/**
 * CmsWorker manages a pool of threads, each accepting requests with different execution
 * requirements and execute them in order of priority and schedule.
 *
 * As each collection of queues is backed by a single thread,
 * all requests are executed sequentially.
 */
public class CmsWorker {
   protected static Logger logger = Logger.getLogger(CmsWorker.class);

   @SuppressWarnings("serial")
   public static class DelayedReqQueue extends LinkedBlockingQueue<Request> {
      // Delay between each scan in nanoseconds.
      private final long scanInterval;
      // Number of requests to scan in an interval.
      private final int numReqToScan;
      // Number of request left to be scanned during the current interval.
      private int numLeft;
      private Long nextScanTime = null;

      public DelayedReqQueue(int scanIntervalSec, int numReqToScan, int capacity) {
         super(capacity);
         scanInterval = TimeUnit.SECONDS.toNanos(scanIntervalSec);
         this.numReqToScan = numReqToScan;
      }

      /**
       * Make a number of requests ready to be taken if
       * the next interval has arrived.
       * @param curTime time to determine whether an interval has happened.
       * @return time for the next interval
       */
      public long refreshScanDelay(long curTime) {
         if (nextScanTime == null || nextScanTime <= curTime) {
            numLeft = size();
            // cap the number of items to scan in an interval
            if (numLeft > numReqToScan) {
               numLeft = numReqToScan;
            }
            nextScanTime = curTime + scanInterval;
         } else if (nextScanTime > curTime + scanInterval) {
            /* It's possible that curTime has drifted.
             * If this happens, reset nextScanTime.
             */
            nextScanTime = curTime + scanInterval;
         }
         return nextScanTime;
      }

      /*
       * This queue has no delay.
       */
      public boolean noDelay() {
         return scanInterval == 0;
      }

      /**
       * Return a request if it's ready.
       * @param waitForDelay true if only return requests that are due
       * @return the request.
       */
      public Request pollRequest(boolean waitForDelay) {
         if (!waitForDelay) {
            return poll();
         } else if (noDelay()) {
            return poll();
         } else if (numLeft > 0) {
            numLeft--;
            return poll();
         } else {
            return null;
         }
      }
   }

   /**
    * An enumeration of all queues of requests to be executed by {\link WorkerThread}s.
    */
   public enum WorkQueue {
      // No delay VC query queue.
      VC_QUERY_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 10 seconds, up to 1000 requests per interval.
      VC_QUERY_TEN_SEC_DELAY(new DelayedReqQueue(10, 1000, Integer.MAX_VALUE)),

      // Highest priority queue, execution without delay & threshold (limit capacity).
      VC_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 10 seconds, up to 1000 requests per interval.
      VC_SYNC_TEN_SEC_DELAY(new DelayedReqQueue(10, 1000, Integer.MAX_VALUE)),
      // Execute every 1 minute, up to 10 requests per interval.
      VC_SYNC_ONE_MIN_DELAY(new DelayedReqQueue(60, 10, Integer.MAX_VALUE)),


      // Highest priority queue, execution without delay & threshold (limit capacity).
      VC_CACHE_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 2 minutes, up to 100 requests per interval.
      VC_CACHE_TWO_MIN_DELAY(new DelayedReqQueue(2 * 60, 100, Integer.MAX_VALUE)),
      // Execute every 5 minutes, up to 1000 requests per interval.
      VC_CACHE_FIVE_MIN_DELAY(new DelayedReqQueue(5 * 60, 1000, Integer.MAX_VALUE)),

      // No delay VC task queue.
      VC_TASK_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 10 seconds, up to 100 requests per interval.
      VC_TASK_TEN_SEC_DELAY(new DelayedReqQueue(10, 100, Integer.MAX_VALUE)),
      // Execute every 5 minute, up to 100 requests per interval.
      VC_TASK_FIVE_MIN_DELAY(new DelayedReqQueue(5 * 60, 100, Integer.MAX_VALUE)),
      // Execute every 1 hour, up to 10 requests per interval.
      VC_TASK_ONE_HOUR_DELAY(new DelayedReqQueue(60 * 60, 10, Integer.MAX_VALUE)),

      // Highest priority queue, execution without delay & threshold (limit capacity).
      VCD_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 10 minutes, up to 1000 requests per interval.
      VCD_SYNC_TEN_MIN_DELAY(new DelayedReqQueue(10 * 60, 1000, Integer.MAX_VALUE)),

      // Highest priority queue, execution without delay & threshold (limit capacity).
      BASE_VM_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 5 minutes, up to 1000 requests per interval.
      BASE_VM_FIVE_MIN_DELAY(new DelayedReqQueue(5 * 60, 1000, Integer.MAX_VALUE));

      private DelayedReqQueue queue;

      WorkQueue(DelayedReqQueue queue) {
         this.queue = queue;
      }

      public DelayedReqQueue getQ() {
         return queue;
      }
   }

   /**
    * An enumeration of worker threads that execute request {\link WorkQueue}s.
    */
   public enum WorkerThread {
      VC_QUERY_THREAD(EnumSet.of(WorkQueue.VC_QUERY_NO_DELAY,
                                 WorkQueue.VC_QUERY_TEN_SEC_DELAY),
                      WorkQueue.VC_QUERY_NO_DELAY, WorkQueue.VC_QUERY_TEN_SEC_DELAY),
      VC_SYNC_THREAD(EnumSet.of(WorkQueue.VC_SYNC_NO_DELAY,
                                WorkQueue.VC_SYNC_TEN_SEC_DELAY,
                                WorkQueue.VC_SYNC_ONE_MIN_DELAY),
                     WorkQueue.VC_SYNC_NO_DELAY, WorkQueue.VC_SYNC_TEN_SEC_DELAY),
      VC_CACHE_THREAD(EnumSet.of(WorkQueue.VC_CACHE_NO_DELAY,
                                 WorkQueue.VC_CACHE_TWO_MIN_DELAY,
                                 WorkQueue.VC_CACHE_FIVE_MIN_DELAY),
                      WorkQueue.VC_CACHE_NO_DELAY, WorkQueue.VC_CACHE_TWO_MIN_DELAY),
      VC_TASK_THREAD(EnumSet.of(WorkQueue.VC_TASK_NO_DELAY,
                                WorkQueue.VC_TASK_TEN_SEC_DELAY,
                                WorkQueue.VC_TASK_FIVE_MIN_DELAY,
                                WorkQueue.VC_TASK_ONE_HOUR_DELAY),
                      WorkQueue.VC_TASK_NO_DELAY, WorkQueue.VC_TASK_TEN_SEC_DELAY),
      VCD_SYNC_THREAD(EnumSet.of(WorkQueue.VCD_SYNC_NO_DELAY,
                                 WorkQueue.VCD_SYNC_TEN_MIN_DELAY),
                      WorkQueue.VCD_SYNC_NO_DELAY, null),
      BASE_VM_SYNC_THREAD(EnumSet.of(WorkQueue.BASE_VM_SYNC_NO_DELAY,
                                     WorkQueue.BASE_VM_FIVE_MIN_DELAY),
                          WorkQueue.BASE_VM_SYNC_NO_DELAY, null);

      private CmsWorkerLoop thread;

      private WorkerThread(EnumSet<WorkQueue> queues, WorkQueue noDelay, WorkQueue resched) {
         thread = new CmsWorkerLoop(queues, noDelay, resched);
         thread.setName(this.name());
         thread.setDaemon(true);
      }

      protected void start() {
         thread.start();
      }

      protected void shutdown() {
         thread.shutdown();
      }

      public boolean isCurrentThread() {
         return Thread.currentThread() == thread;
      }
   }


   /**
    * Abstraction of all requests served by CmsWorker
    */
   public static abstract class Request {
      // STATS source of the requesting thread
      private final StatsType srcStats;
      // STATS entry that identifies the current request.
      private final StatsEntry statsEntry;

      public Request(final StatsEntry entry) {
         srcStats = Profiler.getStatsSrc();
         statsEntry = entry;
      }

      /**
       * By default, all requests are profiled as CMSWORKER_REQ.
       */
      public Request() {
         this(Profiler.getStatsEntry(StatsType.CMSWORKER_REQ));
      }

      /**
       * Callback to execute the request.
       * @return false if the request should be rescheduled.
       */
      abstract protected boolean execute();

      /**
       * Callback to clean up if execute() throws an exception.
       */
      abstract protected void cleanup();

      /**
       * Callback to abort the request without execution.
       * This function cannot block.
       */
      abstract protected void abort();

      /**
       * Safely execute a request without throwing exceptions.
       */
      public boolean safeExecute() {
         Profiler.start(srcStats);
         try {
            logger.debug("execute " + this);
            Profiler.pushInc(statsEntry);
            return execute();
         } catch (Throwable e) {
            logger.error("unexpected error in executing req " + this, e);
            try {
               cleanup();
            } catch (Throwable e1) {
               logger.error("unexpected error in executing req " + this, e1);
            }
            return true;
         } finally {
            Profiler.pop(srcStats);
         }
      }
   }

   /**
    * Abstract request with no clean or abort implementation.
    */
   public abstract static class SimpleRequest extends Request {
      protected SimpleRequest() {
         super();
      }

      protected SimpleRequest(StatsEntry entry) {
         super(entry);
      }

      @Override
      protected void cleanup() {
      }

      @Override
      protected void abort() {
      }

   }

   /**
    * Wrap a request with scheduled periodic execution.
    */
   public abstract static class PeriodicRequest extends SimpleRequest {
      private WorkQueue queue;

      protected PeriodicRequest(WorkQueue queue) {
         super(Profiler.getStatsEntry(StatsType.CMSWORKER_PERIOD));
         this.queue = queue;
      }

      protected PeriodicRequest(StatsEntry entry, WorkQueue queue) {
         super(entry);
         this.queue = queue;
      }

      abstract protected boolean executeOnce();

      @Override
      final protected boolean execute() {
         boolean done;
         try {
            done = executeOnce();
         } catch (Throwable e) {
            logger.error("error executing periodic req " + e);
            done = true;
         }
         if (done) {
            // Schedule the next execution of this request.
            DelayedReqQueue q = queue.getQ();
            q.add(this);
            return true;
         }
         // will be rescheduled
         return false;
      }

      /**
       * Queue a request in the same queue as the periodic request.
       * The new request will be executed before the next scheduled
       * execution of the periodic request.
       */
      protected void queueRequest(Request request) {
         queue.getQ().add(request);
      }
   }

   /**
    * A worker thread that executes requests in a set of queues.
    */
   private static class CmsWorkerLoop extends Thread {
      static final long MAX_LATENCY = TimeUnit.SECONDS.toNanos(20);     // 20 seconds
      private volatile boolean terminate = false;
      // The queue for requests to be executed without delay.
      private WorkQueue noDelay = null;
      // The queue for requests to be rescheduled for execution.
      private WorkQueue resched = null;
      // Work queues handled by this thread.
      private EnumSet<WorkQueue> queues;

      private CmsWorkerLoop(EnumSet<WorkQueue> queues,
                            WorkQueue noDelay, WorkQueue resched) {
         AuAssert.check(noDelay != null);
         AuAssert.check(queues.contains(noDelay));
         AuAssert.check(resched == null ||
                        queues.contains(resched));
         this.queues = queues;
         this.noDelay = noDelay;
         this.resched = resched;
      }

      /*
       * Get a request with the highest priority.
       */
      private Request pollRequest(boolean waitForDelay) {
         for (WorkQueue queueType : queues) {
            DelayedReqQueue queue = queueType.getQ();
            Request req = queue.pollRequest(waitForDelay);
            if (req != null) {
               return req;
            }
         }
         return null;
      }

      /**
       * Update all queues with current time to make eligible requests due for
       * execution.
       *
       * @return next sync time (in nanoSec).
       */
      private long refreshSyncQueues(long curTime) {
         long nextSyncTime = curTime + MAX_LATENCY;

         for (WorkQueue queueType : queues) {
            DelayedReqQueue queue = queueType.getQ();
            long time = queue.refreshScanDelay(curTime);
            if (!queue.noDelay() && queue.size() > 0 && nextSyncTime > time) {
               nextSyncTime = time;
            }
         }
         return nextSyncTime;
      }

      public void run() {
         Request req;
         logger.info(getName() + " is running");
         while (!terminate) {
            try {
               long curTime = System.nanoTime();
               long nextSyncTime = refreshSyncQueues(curTime);
               long maxWait = nextSyncTime - curTime;
               AuAssert.check(maxWait >= 0);
               List<Request> failedReqs = new ArrayList<Request>();

               // Scan all queues for requests that are due for execution.
               req = pollRequest(true);
               while (req != null) {
                  if (!req.safeExecute()) {
                     AuAssert.check(resched != null);
                     failedReqs.add(req);
                  }
                  req = pollRequest(true);
               }

               // Block to get the next request or timeout.
               curTime = System.nanoTime();
               while (curTime < nextSyncTime) {
                  // If curTime goes backwards, restart the outer loop.
                  if (nextSyncTime > curTime + maxWait) {
                     break;
                  }
                  req = noDelay.getQ().poll(nextSyncTime - curTime,
                                            TimeUnit.NANOSECONDS);
                  if (req != null) {
                     if (!req.safeExecute()) {
                        failedReqs.add(req);
                     }
                  }
                  curTime = System.nanoTime();
               }
               // reschedule failed requests
               if (resched != null) {
                  resched.getQ().addAll(failedReqs);
               }
            } catch (InterruptedException e) {
               // interrupted
            }
         }

         // Scan all queued requests.
         while ((req = pollRequest(false)) != null) {
            try {
               req.abort();
            } catch (Throwable e) {
               logger.warn("tried to abort: ", e);
            }
         }
      }

      private void shutdown() {
         terminate = true;
         interrupt();
         try {
            join();
         } catch (InterruptedException e) {
            // interrupted
         }
      }
   }

   static private boolean initialized = false;
   static {
      /* Actual initialization.
       * XXX Should be moved to a power on table.
       */
      init();
   }

   /**
    * Initialize and starts all daemon threads.
    */
   static synchronized public void init() {
      if (initialized) {
         return;
      }
      for (WorkerThread worker : WorkerThread.values()) {
         worker.start();
      }
      initialized = true;
   }

   /**
    * Shutdown all worker threads.
    */
   static synchronized public void shutdown() {
      for (WorkerThread worker : WorkerThread.values()) {
         worker.shutdown();
      }
      initialized = false;
   }

   /**
    * Add a request to the specified queue.
    * @param queue
    * @param req
    */
   static public void addRequest(WorkQueue queue, Request req) {
      if (!queue.getQ().offer(req)) {
         logger.warn("Queue " + queue.name() + " is full with a size of " + queue.getQ().size());
      }
   }

   /**
    * Add a periodic request to the specified queue.
    * @param queue
    * @param req
    */
   static public void addPeriodic(PeriodicRequest req) {
      req.queue.getQ().add(req);
   }
}
