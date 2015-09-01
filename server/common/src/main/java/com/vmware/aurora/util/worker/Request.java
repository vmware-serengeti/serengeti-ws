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
 * Abstraction of all requests served by CmsWorker
 */
public abstract class Request implements Runnable{
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
         CmsWorker.logger.debug("execute " + this);
         Profiler.pushInc(statsEntry);
         return execute();
      } catch (Throwable e) {
         CmsWorker.logger.error("unexpected error in executing req " + this, e);
         try {
            cleanup();
         } catch (Throwable e1) {
            CmsWorker.logger.error("unexpected error in executing req " + this, e1);
         }
         return true;
      } finally {
         Profiler.pop(srcStats);
      }
   }

   public void run() {
      safeExecute();
   }
}
