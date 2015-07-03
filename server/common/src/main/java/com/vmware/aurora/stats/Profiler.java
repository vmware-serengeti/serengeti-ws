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
package com.vmware.aurora.stats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

/**
 * This is a lightweight runtime performance profiler.
 *
 * The profiler keeps track of STAT entries in the system.
 * Each STAT entry is defined by user and records accumulative
 * counts of invocations of events, requests and code execution.
 *
 * The profiler periodically dumps the STAT entries and
 * reports threads and memory usage.
 *
 */
public final class Profiler {
   protected static Logger logger = Logger.getLogger(Profiler.class);

   /**
    * The thread local variable that saves the current STATS source.
    * The STATS source typically indicates the triggering event or
    * request that causes or invokes the code that follows.
    */
   private static ThreadLocal<StatsType> tStatsSrc = new ThreadLocal<StatsType>();

   private static final ConcurrentMap<String, StatsEntry> countMap =
      new ConcurrentHashMap<>();

   /**
    * Find or create a matching STATS entry.
    *
    * Note that the STATS source is retrieved from the thread local variable
    * that was previous set by StatsEntry.start() or StatsEntry.push().
    *
    * @param type current STATS type
    * @param objs objects for identifying the STATS entry
    * @return the STATS entry
    */
   public static StatsEntry getStatsEntry(StatsType type, Object... objs) {
      String key = StatsEntry.getKey(getStatsSrc(), type, objs);
      StatsEntry val = countMap.get(key);
      if (val == null) {
         val = new StatsEntry(getStatsSrc(), type, objs);
         countMap.put(key, val);
      }
      return val;
   }

   /**
    * Start a STATS source on the current execution thread.
    * @param srcType
    */
   public static void start(StatsType srcType) {
      tStatsSrc.set(srcType);
   }

   /**
    * Push the Stats type to be used in combination with pop().
    *
    * void methodA() {
    *    try {
    *       StatsType oldType = StatsEntry.push(MY_STAT);
    *
    *       // Any StatsEntry.inc() will have MY_STAT as the "source".
    *
    *    } finally {
    *       StatsEntry.pop(oldType);
    *    }
    * }
    *
    * @param srcType the new Stats Type
    * @return the old Stats type
    */
   public static StatsType push(StatsType srcType) {
      StatsType ret = tStatsSrc.get();
      tStatsSrc.set(srcType);
      return ret;
   }

   /**
    * Reverse StatsEntry.push().
    * @param oldSrc
    */
   public static void pop(StatsType oldSrc) {
      tStatsSrc.set(oldSrc);
   }

   /**
    * This can be used for passing STATS source from a requesting
    * thread to a handler thread.
    * @return STATS source of the current thread
    */
   public static StatsType getStatsSrc() {
      return tStatsSrc.get();
   }

   /**
    * Increment the Stats counter and push stats type.
    * @return the old Stats type
    */
   public static StatsType pushInc(StatsEntry entry) {
      entry.inc();
      return push(entry.getDest());
   }

   public static StatsType pushInc(StatsType type, Object... objs) {
      return pushInc(getStatsEntry(type, objs));
   }

   /**
    * Increment the Stats counter.
    * @param objs
    */
   public static void inc(StatsType type, Object... objs) {
      getStatsEntry(type, objs).inc();
   }

   /**
    * Log all STATS during the interval.
    */
   public static void logInterval(long interval) {
      for (StatsEntry e : countMap.values()) {
         long count = e.getCount();
         long diff = e.updateLastCount(count);
         // Only output the items that change.
         if (count > 0) {
            StatsLogger.logStats("STATS " + interval + " | " + e + ":" +
                  count + "," + diff);
         }
      }
   }
}
