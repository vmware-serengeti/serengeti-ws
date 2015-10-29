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
package com.vmware.aurora.util.worker;

import com.vmware.aurora.util.AuAssert;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CmsWorker manages a pool of threads, each accepting requests with different execution
 * requirements and execute them in order of priority and schedule.
 *
 * As each collection of queues is backed by a single thread,
 * all requests are executed sequentially.
 */
@Component
@Scope("singleton")
public class CmsWorker {
   static Logger logger = Logger.getLogger(CmsWorker.class);
   /**
    * An enumeration of all queues of requests to be executed by {\link WorkerThread}s.
    */
   public enum WorkQueue {
      // No delay VC query queue.
      VC_QUERY_NO_DELAY("vcQueryExec"),
      // Highest priority queue, execution without delay & threshold (limit capacity).
      VC_CACHE_NO_DELAY("vcCacheExec"),
      // No delay VC task queue.
      VC_TASK_NO_DELAY("vcTaskExec"),

      // Execute every 10 seconds, up to 1000 requests per interval.
      //VC_QUERY_TEN_SEC_DELAY(new DelayedReqQueue(10, 1000, Integer.MAX_VALUE)),
      // Highest priority queue, execution without delay & threshold (limit capacity).
      //VC_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 10 seconds, up to 1000 requests per interval.
      //VC_SYNC_TEN_SEC_DELAY(new DelayedReqQueue(10, 1000, Integer.MAX_VALUE)),
      // Execute every 1 minute, up to 10 requests per interval.
      //VC_SYNC_ONE_MIN_DELAY(new DelayedReqQueue(60, 10, Integer.MAX_VALUE)),
      // Execute every 5 minutes, up to 1000 requests per interval.
      //VC_CACHE_FIVE_MIN_DELAY(new DelayedReqQueue(5 * 60, 1000, Integer.MAX_VALUE)),
      // Execute every 10 seconds, up to 100 requests per interval.
      //VC_TASK_TEN_SEC_DELAY(new DelayedReqQueue(10, 100, Integer.MAX_VALUE)),

      // Execute every 2 minutes, up to 100 requests per interval.
      VC_CACHE_TWO_MIN_DELAY("vcCacheScheduler"),
      // Execute every 5 minute, up to 100 requests per interval.
      VC_TASK_FIVE_MIN_DELAY("vcCacheScheduler");

      // Execute every 1 hour, up to 10 requests per interval.
      //VC_TASK_ONE_HOUR_DELAY(new DelayedReqQueue(60 * 60, 10, Integer.MAX_VALUE)),
      // Highest priority queue, execution without delay & threshold (limit capacity).
      //VCD_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 10 minutes, up to 1000 requests per interval.
      //VCD_SYNC_TEN_MIN_DELAY(new DelayedReqQueue(10 * 60, 1000, Integer.MAX_VALUE)),
      // Highest priority queue, execution without delay & threshold (limit capacity).
      //BASE_VM_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, Integer.MAX_VALUE)),
      // Execute every 5 minutes, up to 1000 requests per interval.
      //BASE_VM_FIVE_MIN_DELAY(new DelayedReqQueue(5 * 60, 1000, Integer.MAX_VALUE)),
      // Execute every 5 minute, up to 1000 requests per interval.
      //CUSTOM_FIVE_MIN_SYNC_DELAY(new DelayedReqQueue(5 * 60, 1000, 1000)),
      //CUSTOM_SYNC_NO_DELAY(new DelayedReqQueue(0, 1000, 1000));

      private String executorName;
      WorkQueue(String executorName1) {
         executorName = executorName1;
      }
      public String getExecutorName() {
         return executorName;
      }
   }

   @Autowired
   private Map<String, Executor> executors;
   @Autowired
   private Map<String, ScheduledExecutorService> schedulers;

   private volatile static CmsWorker SINGLETON;

   @PostConstruct
   public void init() {
      SINGLETON = this;
   }

   @Bean
   public static CmsWorker getInstance() {
      return SINGLETON;
   }

   private Executor findExecutor(WorkQueue queue) {
      return find(executors, queue);
   }

   private ScheduledExecutorService findScheduler(WorkQueue queue) {
      return find(schedulers, queue);
   }

   private static<T> T find(Map<String, T> items, WorkQueue name) {
      T item = items.get(name.getExecutorName());

      AuAssert.check(item != null, "can't find executor/scheduler for: " + name);

      return item;
   }

   public void execRequest(WorkQueue queue, Request req) {
      findExecutor(queue).execute(req);
   }

   public void scheduleRequest(WorkQueue queue, PeriodicRequest req) {
      findScheduler(queue).scheduleWithFixedDelay(req, 0, req.getDelay(), TimeUnit.MILLISECONDS);
   }

   /**
    * Add a request to the specified queue.
    * @param queue
    * @param req
    */
   public static void addRequest(WorkQueue queue, Request req) {
      SINGLETON.execRequest(queue, req);
   }

   /**
    * Add a periodic request to the specified queue.
    * @param req
    */
   static public void addPeriodic(PeriodicRequest req) {
      SINGLETON.scheduleRequest(req.getQueue(), req);
   }

}
