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
package com.vmware.aurora.stats;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * A thread that dumps stats & runtime info periodically.
 */
public class StatsLogger extends Thread {
   // Use this logger to do non-stats log.
   // The statsLogger is configured to write log into aurora-cms-stats.log.
   private static Logger logger = Logger.getLogger(StatsLogger.class);


   private static long REPORT_INTERVAL = TimeUnit.MINUTES.toNanos(2); // every 2 min
   private static StatsLogger loggerThread;
   private static boolean shutdown = false;


   static public void init(int intervalMinutes) {
      loggerThread = new StatsLogger();
      loggerThread.start();

      REPORT_INTERVAL = TimeUnit.MINUTES.toNanos(intervalMinutes);
   }

   static public void shutdown() {
      shutdown = true;
   }

   final static void logStats(Object obj) {
      logger.info(obj);
   }

   private StatsLogger() {
      setName("StatsLogger");
      setDaemon(true);
   }

   private void checkThreadLimit() {

   }

   private void reportRuntimeInfo() {
      // assume a single thread group
      logStats(String.format("RUNTIME THREADS active %d.",
               Thread.activeCount()));
      logStats(String.format("RUNTIME MEMORY free %d total %d max %d.",
               Runtime.getRuntime().freeMemory(),
               Runtime.getRuntime().totalMemory(),
               Runtime.getRuntime().maxMemory()));
      checkThreadLimit();
   }

   private void reportInterval(long interval) {
      logStats("INTERVAL " + interval + " START");
      Profiler.logInterval(interval);
      reportRuntimeInfo();
      logStats("INTERVAL " + interval + " END");
   }

   @Override
   public void run() {
      long interval = 0;
      long lastReportTime = System.nanoTime();
      while (!shutdown) {
         long nextReportTime = lastReportTime + REPORT_INTERVAL;
         try {
            reportInterval(++interval);
            long curTime = System.nanoTime();
            if (nextReportTime > curTime) {
               sleep(TimeUnit.NANOSECONDS.toMillis(nextReportTime - curTime));
            }
            lastReportTime = nextReportTime;
         } catch (Throwable e) {
            // Take note of any exceptions and continue.
            logger.info("caught exception ", e);
         }
      }
   }
}
