/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.aurora.util.worker;

import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsEntry;
import com.vmware.aurora.stats.StatsType;

/**
 * Wrap a request with scheduled periodic execution.
 */
public abstract class PeriodicRequest extends SimpleRequest {
   private CmsWorker.WorkQueue queue;

   private long delay;

   protected PeriodicRequest(CmsWorker.WorkQueue queue, long delayInMillisecond1) {
      this(Profiler.getStatsEntry(StatsType.CMSWORKER_PERIOD), queue, delayInMillisecond1);
   }

   protected PeriodicRequest(StatsEntry entry, CmsWorker.WorkQueue queue, long delayInMillisecond1) {
      super(entry);
      this.queue = queue;
      delay = delayInMillisecond1;
   }

   abstract protected boolean executeOnce();

   @Override
   final protected boolean execute() {
      boolean done;
      try {
         done = executeOnce();
      } catch (Throwable e) {
         CmsWorker.logger.error("error executing periodic req " + e);
         done = true;
      }
      return done;
   }

   /**
    * Queue a request in the same queue as the periodic request.
    * The new request will be executed before the next scheduled
    * execution of the periodic request.
    */
   protected void queueRequest(Request request) {
//      queue.getQ().add(request);
   }

   public void removeRequest() {
//      queue.getQ().remove(this);
   }

   public CmsWorker.WorkQueue getQueue() {
      return queue;
   }


   public long getDelay() {
      return delay;
   }
}
