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

package com.vmware.aurora.vc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcLongCallHandler.VcLongCall;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.impl.vim.PerformanceManager_Impl.MetricIdImpl;
import com.vmware.vim.binding.impl.vim.PerformanceManager_Impl.QuerySpecImpl;
import com.vmware.vim.binding.vim.ElementDescription;
import com.vmware.vim.binding.vim.HistoricalInterval;
import com.vmware.vim.binding.vim.PerformanceManager;
import com.vmware.vim.binding.vim.PerformanceManager.CounterInfo;
import com.vmware.vim.binding.vim.PerformanceManager.EntityMetric;
import com.vmware.vim.binding.vim.PerformanceManager.EntityMetricBase;
import com.vmware.vim.binding.vim.PerformanceManager.IntSeries;
import com.vmware.vim.binding.vim.PerformanceManager.MetricId;
import com.vmware.vim.binding.vim.PerformanceManager.MetricSeries;
import com.vmware.vim.binding.vim.PerformanceManager.ProviderSummary;
import com.vmware.vim.binding.vim.PerformanceManager.QuerySpec;
import com.vmware.vim.binding.vim.ResourcePool;
import com.vmware.vim.binding.vim.VirtualApp;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/*
 * Class to query VC for performance stats from a virtual machine (VM) or resource pool (RP),
 * measuring use of memory/cpu/network.
 *
 * TO DO:
 * - units: obtain scale, find way of getting relative units for all queries we want to run,
 *   or else upper layers need to do this (which thye can probably do more efficiently)
 * - any historical/average performance gathering needed by dashboard and not supported innately by VC.
 *
 * BUGS (inherited from VC):
 * - the limitRealtimeSamples parameter to queryPerformance() is ignored (bug 658620).
 * - relative stats claim to be in % (1/100) but are actually in %/100 (1/10000) (bug 699834).
 * - RPs won't answer queries entirely inside the last half hour (bug 699830).
 *
 * Some notes on how VC exposes statistics for RP objects:
 * - RP stats are not available realtime, only for historical intervals (5 minutes and above).
 * - newest RP stat is always > 5 minutes old.
 * - RP stats are not accurate when VMs move in/out of RP (VC implements by aggregating VM
 *   stats from all VMs in RP at time of query)
 * - RP doesn't support % based counters like mem::usage and cpu::usage, only the underlying
 *   physical-unit-based counters, because in its view, there's no obvious maximum to use for
 *   the denominator (our use, where limit==reservation, is easier).
 * - RP doesn't support mem::active (until VC 5.0, where it's named mem::capacity.usage but
 *   means the same thing).
 * All of this taken together means that we can aggregate VM stats into RP stats ourselves,
 * and potentially do a better job than VC does for our purposes.
 *
 * We should consider adding 2 new areas of functionality to this module:
 * - scale: per object, know what the limits (vm host core speed, vm mem assigned, rp cpu assigned,
 *   rp mem assigned) are, cache these per VC connection, be able to report it
 * - rp aggregation: separate method to query rp stats, which queries individual vm stats and aggregates
 * Unless the client wants to just do those things itself. But the client does want those things.
 */


/*
 * Class VcPerformance: use this class to issue queries for performance statistics
 * from VirtualCenter's PerformanceManager.
 */
/**
 * @author mginzton
 *
 */
public class VcPerformance {
   private static final Logger logger = Logger.getLogger(VcVmBase.class);

   /*
    * VC session cache: try to avoid repeated queries to VC for data that doesn't change;
    * query once every 5 min.
    */
   private long nextRefreshTime;
   private static final long PERFCOUNTER_REFRESH_PERIOD = TimeUnit.MINUTES.toNanos(5);
   private CounterInfo[] sessionPerfCounters; // access only via getCachedPerfCounterList()

   /*
    * Class PerformanceType: used to specify which object you want performance
    * statistics for.
    */
   public enum PerformanceType {
      cpuAbs,         // absolute: in MHz; works for VMs and RPs
      cpuRel,         // relative: in %x100 of underlying core(s); works for VMs but not RPs
      memOccupiedAbs, // absolute: in KB; based on consumption: all memory occupying physical pages; works for VMs and RPs
      memActiveAbs,   // absolute: in KB; based on active: memory recently touched; works for VMs, and also RPs starting with vSphere 5.0
      memActiveRel,   // relative: in %x100 of assigned size; based on active: memory recently touched; works for VMs but not RPs
      memOverheadAbs, // absolute: in KB; based on overhead: memory overhead consumed by vm kernel; works for VMs and RPs
      vmSize,    // absolute: in KB; vm size
      netInAbs,       // absolute: in kbps;
      netOutAbs,      // absolute: in kbps;
      netAbs          // absolute: in kbps; works for VMs but not RPs
   }

   /*
    * Class PerformanceSample: used to encapsulate one performance sample from VC.
    */
   public static class PerformanceSample {
      private long sample; // could be ratio, or absolute
      private Calendar timestamp;
      private String unit;

      /**
       * @return the sample
       */
      public long getSample() {
         return sample;
      }

      /**
       * @return the timestamp
       */
      public Calendar getTimestamp() {
         return timestamp;
      }

      /**
       * @return the unit
       */
      public String getUnit() {
         return unit;
      }

      /**
       * Format as string (intended for human readability)
       */
      public String toString() {
         return String.format("[%s %s@%s]", sample, unit, new SimpleDateFormat("yyyy/mm/dd HH:mm:ss").format(timestamp.getTime()));
      }
   }

   /**
    * getRealtimeInterval(): ask VC for the interval between "real-time" updates
    * from a performance provider.
    *
    * @param targetId: RefId of VC entity that is a performance provider (normally a VM).
    * @return Realtime sampling interval (aka refresh rate) for target entity.
    */
   public Integer getRealtimeInterval(final String targetId) {
      return VcContext.inVcSessionDo(new VcSession<Integer>() {
         public Integer body() {
            ManagedObjectReference target = MoUtil.stringToMoref(targetId);
            ProviderSummary summary = getCachedPerfMgr().queryProviderSummary(target);
            Integer refresh = summary.getRefreshRate();
            if (summary.isCurrentSupported()) {
               return refresh;
            }

            return null;
         }
      });
   }

   /**
    * getHistoricalIntervals(): ask VC for the intervals between historical
    * statistics collection.
    *
    * @return List of sampling intervals.
    */
   public Integer[] getHistoricalIntervals() {
      return VcContext.inVcSessionDo(new VcSession<Integer[]>() {
         public Integer[] body() {
            ArrayList<Integer> intervals = new ArrayList<Integer>();
            for (HistoricalInterval interval: getCachedPerfMgr().getHistoricalInterval()) {
               intervals.add(interval.getSamplingPeriod());
            }

            return intervals.toArray(new Integer[intervals.size()]);
         }
      });
   }

   /**
    * getAllSamplingIntervals(): convenience method to determine all supported
    * sampling intervals for a given performance provider; combines the results
    * of getHistoricalIntervals() and getRealtimeInterval().
    *
    * @param targetId: RefId of VC entity that is a performance provider (normally a VM).
    * @return List of sampling intervals.
    */
   public Integer[] getAllSamplingIntervals(final String targetId) {
      ArrayList<Integer> intervals = new ArrayList<Integer>();

      Integer interval = getRealtimeInterval(targetId);
      if (interval != null) {
         intervals.add(interval);
      }
      intervals.addAll(Arrays.asList(getHistoricalIntervals()));
      return intervals.toArray(new Integer[intervals.size()]);
   }

   /**
    * queryVcCurrentTime(): get the current time according to the VC server.
    *
    * @return Time, as java.util.Calendar.
    */
   public Calendar queryVcCurrentTime() {
      return VcContext.inVcSessionDo(new VcSession<Calendar>() {
         public Calendar body() {
            return VcContext.getService().getServiceInstance().currentTime();
         }
      });
   }

   /**
    * queryLatestPerformance(): query VC for most recent performance statistics
    * available for the given entity, at the highest resolution (shortest sampling
    * interval) supported. Convenience wrapper around queryPerformance().
    *
    * @param targetIds: RefIds of VC entities that are performance providers (VM or RP), all entities must be the same type.
    * @param type: type of performance statistics to retrieve.
    * @return Map of String and PerformanceSample, with reference id of String as key.
    */
   public Map<String, PerformanceSample> queryLatestPerformance(
                                                   List<String> targetIds,
                                                   PerformanceType type) {
      AuAssert.check(targetIds != null && targetIds.size() > 0);
      Integer[] intervals = getAllSamplingIntervals(targetIds.get(0));

      // Ugly: for VM, we need to use value inside last half hour to get newest results;
      // for RP, we need to use value outside last half hour to get any results at all
      // (VC bug 699830; see comment preceding the main queryPerformance method).
      // So, we behave differently depending on whether this target is a VM or RP.
      boolean isRP = MoUtil.isOfType(targetIds.get(0), ResourcePool.class) || MoUtil.isOfType(targetIds.get(0), VirtualApp.class);
      int startMinutesAgo = isRP ? 30 : 5;

      Calendar startTime = queryVcCurrentTime();
      refreshPerfCounters();
      startTime.add(Calendar.MINUTE, -startMinutesAgo);
      Map<String, PerformanceSample[]> samples = queryPerformance(targetIds, type, intervals[0], 0, startTime, null);
      Map<String, PerformanceSample> result = new HashMap<String, PerformanceSample>(samples.size());
      for (String refId : samples.keySet()) {
         PerformanceSample[] sample = samples.get(refId);
         if (sample != null && sample.length > 0) {
            try {
               if (isRP) {
                  // for RP, we can't trust the newest sample (PR 703835, PR 723127). So ignore the
                  // newest sample and return 2nd newest. Because of the PR 699830 workaround above,
                  // we should always have multiple (usually 5) samples anyway.
                  result.put(refId, sample[sample.length - 2]);
               } else {
                  // for VM, return last sample (in practice the only sample, the list should have
                  // only one item)
                  result.put(refId, sample[sample.length - 1]);
               }
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
         }
      }
      return result;
   }

   /*
    * Note on the various ways of limiting samples returned: ESX and VC sample various counters only as defined
    * for each performance provider (such as VM or RP), and only at certain intervals which vary by counter.
    *
    * VC makes a distinction between "realtime" and "historical" stats, which is determined by the sampling
    * interval: a performance provider may or may not implement a realtime sampling interval (also known as
    * "refresh rate" in VC docs); VMs do and RPs do not. VC aggregates the data from more frequent intervals
    * into the less frequent intervals, and stores the coarser-grained data going back farther into the past
    * in its database.
    *
    * Queries for performance data may be answered by VC from its database, or may be forwarded directly to
    * ESX servers. VC has a documented optimization where it treats requests inside the last half hour
    * specially. (This means the begin time must be less than half an hour ago, and the end time after that,
    * or null.) I note that when querying VM statistics, if I ask for samples outside this range, the newest
    * sample is always 1 interval stale (so for the 5-minute interval, best case is the newest sample is 6
    * minutes old, and it may well be 9 minutes old). However, when querying RP statistics, the optimization
    * appears to break things; it returns no data at all for the optimized window (but a window starting >=
    * 30 minutes ago does return data, including the range at the in-optimization-window query returns no
    * results for). I even confirmed this in vSphere Client, interactively: a chart for the last 25 minutes is
    * empty, a chart for the last 30 minutes has data.
    * (See http://www.vmware.com/support/developer/vc-sdk/visdk41pubs/vsp41_vsdk_prog_guide.pdf, page 156,
    * "Optimizing query response time".) Filed as VC bug 699830.
    *
    * Thus, not only the interval between samples, but also the time range over which data is available, and
    * whether data was even collected in the first place, will depend on the sampling interval. The interval
    * is the primary factor; it must be specified for all queries, and must be one of the values returned
    * from getSamplingIntervals().
    *
    * For a given interval, you can narrow the amount of data returned by specifying the optional start & end
    * time parameters. Additionally, the API offers a way to further narrow the data returned to the N newest
    * samples; this is documented to work only for the realtime interval, and is apparently broken entirely
    * in ESX 5.0.
    */

   /**
    * queryPerformance():
    * @param targetId: RefId of VC entity that is a performance provider (VM or RP).
    * @param type: type of performance statistics to retrieve.
    * @param sampleInterval: interval between samples, in seconds
    * @param limitRealtimeSamples: maximum number of realtime samples to retrieve (BUG: apparently ignored by VC).
    * @param historicalStart: retrieve historical stats only after this time, if not null
    * @param historicalEnd: retrieve historical stats only before this time, if not null
    * @return List of results, as PerformanceSamples.
    */
   public PerformanceSample[] queryPerformance(final String targetId,              // RefId to VM or RP
                                               final PerformanceType type,         // mem/cpu/net
                                               final int sampleInterval,           // time between samples in seconds
                                               final int limitRealtimeSamples,     // realtime samples: limit number of samples to return, 0 for unlimited
                                               final Calendar historicalStart,     // historical samples: null for oldest sample available
                                               final Calendar historicalEnd)       // historical samples: null (or now) for up-to-current
   {
      if (targetId == null || targetId.equals("")) {
         return null;
      } else {
         List<String> id = new ArrayList<String>(1);
         id.add(targetId);
         return queryPerformance(id, type, sampleInterval, limitRealtimeSamples,
               historicalStart, historicalEnd).get(targetId);
      }
   }

   /**
    * queryPerformance():
    * @param targetIds: RefIds of VC entities that are performance providers (VM or RP). All entities must be the same type.
    * @param type: type of performance statistics to retrieve.
    * @param sampleInterval: interval between samples, in seconds
    * @param limitRealtimeSamples: maximum number of realtime samples to retrieve (BUG: apparently ignored by VC).
    * @param historicalStart: retrieve historical stats only after this time, if not null
    * @param historicalEnd: retrieve historical stats only before this time, if not null
    * @return Map of (String and PerformanceSample[]), with Reference Id of String as key.
    */
   public Map<String, PerformanceSample[]> queryPerformance(
                                               final List<String> targetIds,       // RefId to VM or RP
                                               final PerformanceType type,         // mem/cpu/net
                                               final int sampleInterval,           // time between samples in seconds
                                               final int limitRealtimeSamples,     // realtime samples: limit number of samples to return, 0 for unlimited
                                               final Calendar historicalStart,     // historical samples: null for oldest sample available
                                               final Calendar historicalEnd)       // historical samples: null (or now) for up-to-current
   {
      // Check for possible bad things as assertions.
      AuAssert.check(historicalStart == null || historicalEnd == null ||
                     historicalStart.before(historicalEnd));
      AuAssert.check(targetIds != null && targetIds.size() > 0);

      return VcContext.getVcLongCallHandler().execute(
            new VcLongCall<Map<String, PerformanceSample[]>>() {
         public Map<String, PerformanceSample[]> callVc() {
            List<ManagedObjectReference> targetMOB = new ArrayList<ManagedObjectReference>(targetIds.size());
            String firstType = null;
            for (String refId : targetIds) {
               ManagedObjectReference moRef = MoUtil.stringToMoref(refId);
               if (firstType == null) {
                  firstType = moRef.getType();
               } else if (!moRef.getType().equals(firstType)) {
                  throw VcException.INVALID_ARGUMENT();
               }
               targetMOB.add(moRef);
            }

            // counters: get info on all performance counters; build map of those relevant to this query type
            CountersByType counters = getCountersOfType(type, targetIds.get(0));
            MetricId[] metrics = getMatchingMetrics(targetMOB.get(0), counters, sampleInterval);
            // construct query spec
            QuerySpec[] specs = new QuerySpecImpl[targetMOB.size()];
            for (int i = 0; i < targetMOB.size(); i++) {
               specs[i] = new QuerySpecImpl();
               specs[i].setEntity(targetMOB.get(i));
               specs[i].setIntervalId(sampleInterval);
               specs[i].setFormat("normal");
               specs[i].setMetricId(metrics);
               // selection of samples (historical)
               if (historicalStart != null) {
                  specs[i].setStartTime(historicalStart);
               }
               if (historicalEnd != null) {
                  specs[i].setEndTime(historicalEnd);
               }
               if (limitRealtimeSamples > 0) {
                  specs[i].setMaxSample(limitRealtimeSamples);
               }
            }

            /*
             * Submit query request
             *
             * NB 1: impedance matching: we have one querySpec and want one result, but API takes a list and returns
             *       a list, I assume 1:1, so we stick our single querySpec in a list, call function, and extract
             *       the first member of the returned list.
             * NB 2: we can ask for information in default or csv format; objects returned will be EntityMetric or EntityMetricCSV
             * NB 3: for default format: EntityMetric stats = ((EntityMetric) metricBase[0]);
             *       - stats.getValue()[0].getValue() is a list of ints: N samples
             *       - stats.getSampleInfo() is a list of strings: N descriptions
             * NB 4: for csv format: EntityMetricCSV csv = ((EntityMetricCSV) metricBase[0]);
             *       - csv.getValue()[0].getValue() is a string, splits by "," into N strings which are int samples
             *       - csv.getSampleInfo.CSV() is a string, splits by "," into 2N strings which are (interval, date) tuples
             */
            //submit query request
            //VC will filter all query request, and only return existing performance data
            //so we should construct performance result base on return value
            //and use reference id as key.
            EntityMetricBase[] metricBase = getCachedPerfMgr().queryStats(specs);
            Map<String, PerformanceSample[]> result = new HashMap<String, PerformanceSample[]>();
            if (metricBase != null && metricBase.length > 0) {
               for (EntityMetricBase perf : metricBase) {
                  EntityMetric stats = ((EntityMetric) perf);
                  if (stats.getValue() != null && stats.getSampleInfo() != null) {
                     IntSeries samples = (IntSeries) stats.getValue()[0];
                     if (samples != null) {
                        int numSamples = samples.getValue().length;
                        List<PerformanceSample> vals = new ArrayList<PerformanceSample>();
                        for (int i = 0; i < numSamples; i++) {
                           if (samples.getValue()[i] >= 0) {
                              PerformanceSample output = new PerformanceSample();
                              output.sample = samples.getValue()[i];
                              output.timestamp = stats.getSampleInfo()[i].getTimestamp();
                              ElementDescription desc = counters.map.get(samples.getId().getCounterId()).getUnitInfo();
                              output.unit = desc.getLabel();
                              vals.add(output);
                           }
                        }
                        if (vals.size() > 0) {
                           result.put(MoUtil.morefToString(perf.getEntity()), vals.toArray(new PerformanceSample[vals.size()]));
                        }
                     }
                  }
               }
            }
            return result;
         }
      });
   }

   /**
    * query performance data of children locates on specified entity;
    * currently only used for vm size on each datastore.
    * @param childrenMap Map of [entity reference Id] : [List of children identifier].
    *                    child identifier could be 12345 if child vm reference id is vm-12345;
    * @param type performance type, currently only support PerformanceType.dataStorage
    * @return Map of (String and PerformanceSample[]), with children identifier of String as key.
    */
   public  Map<String, PerformanceSample> queryPerformance(
         final Map<String, List<String>> childrenMap,  //map of Datastore refId and List of VirtualMachine identifier
         final PerformanceType type)         // disk used
   {
      //use first level historical sample interval, should be 300s as default.
      final Integer sampleInterval = getHistoricalIntervals()[0];

      return VcContext.getVcLongCallHandler().execute(
      new VcLongCall<Map<String, PerformanceSample>>() {
         @Override
         public Map<String, PerformanceSample> callVc() {
            Map<String, PerformanceSample> result = new HashMap<String, PerformanceSample>();
            if (childrenMap.size() == 0) {
               return result;
            }
            CountersByType counters = getCountersOfType(type, null);
            //use first matched counter
            Integer counterKey = (Integer) counters.map.keySet().toArray()[0];
            /*
             * use half hour as default query time range,
             * it is the least time range for the first level sample interval.
             */
            Calendar historicalEnd = VcContext.getService().getServiceInstance().currentTime();
            Calendar historicalStart = (Calendar) historicalEnd.clone();
            historicalStart.add(Calendar.MINUTE, (0 - 30));
            /*
             * construct query spec according to Map of entity:children list
             */
            QuerySpec[] specs = new QuerySpecImpl[childrenMap.size()];
            int index = 0;
            for (String refId : childrenMap.keySet()) {
               List<String> childrenList = childrenMap.get(refId);
               if (childrenList != null && childrenList.size() > 0) {
                  ManagedObjectReference moRef = MoUtil.stringToMoref(refId);
                  specs[index] = new QuerySpecImpl();
                  specs[index].setEntity(moRef);
                  specs[index].setIntervalId(sampleInterval);
                  specs[index].setFormat("normal");
                  MetricId[] metrics = new MetricIdImpl[childrenList.size()];
                  for (int j = 0; j < childrenList.size(); j++) {
                     metrics[j] = new MetricIdImpl();
                     metrics[j].setCounterId(counterKey);
                     metrics[j].setInstance(childrenList.get(j));
                  }
                  specs[index].setMetricId(metrics);
                  specs[index].setStartTime(historicalStart);
                  specs[index].setEndTime(historicalEnd);
                  index++;
               }
            }

            /*
             * submit query request
             * VC will filter the request,
             * and return performance data for each child entity on each parent entity;
             * if child entity has no relationship with parent entity,
             * the return value should be null.
             * we parse all return result, and take the last performance data;
             * and construct a map of [child identifier]:[performance data].
             * if a child entity has relationship with multi-parent,
             * we should aggregate all performance data for this child entity.
             */
            EntityMetricBase[] metricBase = getCachedPerfMgr().queryStats(specs);
            if (metricBase != null && metricBase.length > 0) {
               for (EntityMetricBase perf : metricBase) {
                  EntityMetric stats = ((EntityMetric) perf);
                  if (stats.getValue() != null && stats.getSampleInfo() != null) {
                     MetricSeries[] samples = (MetricSeries[]) stats.getValue();
                     if (samples != null) {
                        for (int i = 0; i < samples.length; i++) {
                           IntSeries sample = (IntSeries) samples[i];
                           if (sample.getValue() != null) {
                              String id = sample.getId().getInstance();
                              PerformanceSample val = result.get(id);
                              if (val == null) {
                                 val = new PerformanceSample();
                                 val.sample = sample.getValue()[sample.getValue().length - 1];
                                 val.timestamp = stats.getSampleInfo()[stats.getSampleInfo().length - 1].getTimestamp();
                                 ElementDescription desc = counters.map.get(samples[i].getId().getCounterId()).getUnitInfo();
                                 val.unit = desc.getLabel();
                                 result.put(id, val);
                              } else {
                                 val.sample = val.sample + sample.getValue()[sample.getValue().length - 1];
                              }
                           }
                        }
                     }
                  }
               }
            }
            return result;
         }
      });
   }
   /**
    * getCountersOfType(): return set of performance counters supported by VC, limited to those
    * relevant to a certain PerformanceType and expressed as a map indexed by counter ID.
    *
    * @param type: PerformanceType to filter on
    * @param targetType: VIM type of the entity to be queried
    * @return Map from counter ID to counter details, holding relevant counters.
    */
   private CountersByType getCountersOfType(PerformanceType type, String targetId) {
      CountersByType counters = new CountersByType();

      switch (type) {
      case cpuRel:
         AuAssert.check(!MoUtil.isOfType(targetId, ResourcePool.class));
         counters.group = "cpu";
         counters.name = "usage";
         break;
      case cpuAbs:
         counters.group = "cpu";
         counters.name = "usagemhz";
         break;
      case memOccupiedAbs:
         counters.group = "mem";
         counters.name = "consumed";
         break;
      case memActiveAbs:
         counters.group = "mem";
         counters.name = (MoUtil.isOfType(targetId, ResourcePool.class)) ? "capacity.usage" : "active";
         break;
      case memActiveRel:
         AuAssert.check(!MoUtil.isOfType(targetId, ResourcePool.class));
         counters.group = "mem";
         counters.name = "usage";
         break;
      case memOverheadAbs:
         counters.group = "mem";
         counters.name = "overhead";
         break;
      case vmSize:
         counters.group = "disk";
         counters.name = "used";
         break;
      case netInAbs:
         AuAssert.check(!MoUtil.isOfType(targetId, ResourcePool.class));
         counters.group = "net";
         counters.name = "received";
         break;
      case netOutAbs:
         AuAssert.check(!MoUtil.isOfType(targetId, ResourcePool.class));
         counters.group = "net";
         counters.name = "transmitted";
         break;
      case netAbs:
         AuAssert.check(!MoUtil.isOfType(targetId, ResourcePool.class));
         counters.group = "net";
         counters.name = "usage";
         break;
      default:
         logger.debug("Performance type " + type + " is not supported");
         throw VcException.INVALID_ARGUMENT();
      }
      CounterInfo[] allCounters = getCachedPerfCounterList();
      if (allCounters == null) {
         logger.debug("No available performance counters in specified vCenter");
         throw VcException.PERFORMANCE_ERROR();
      }
      for (CounterInfo counter: allCounters) {
         if (counter.getGroupInfo().getKey().equals(counters.group) &&
             counter.getNameInfo().getKey().equals(counters.name)) {
            counters.map.put(counter.getKey(), counter);
         }
      }

      return counters;
   }

   /**
    * getMatchingMetrics(): query VC for metrics supported by a given target and relevant to a given query
    * (expressed by the provided counterMap).
    *
    * @param target: RefId of VC entity that is a performance provider.
    * @param counterMap: map populated with counters caller is interested in.
    * @param interval: sampling interval, used to restrict metrics.
    * @return List of metric IDs.
    */
   private MetricId[] getMatchingMetrics(ManagedObjectReference target,
                                         CountersByType counters,
                                         Integer interval) {
      AuAssert.check(VcContext.isInSession());

      /*
       * Metrics: get metrics available for the target, then filter those for ones specifying
       * a counter of the right type.
       *
       * Note on interval to query: This must be a legal interval; i.e. one of the historical
       * intervals or, for entities supporting realtime stats, the refresh rate; otherwise
       * queryAvailableMetric throws an exception. The realtime or historical intervals do work,
       * with one caveat: for powered off VMs, asking for interval 20 (realtime refresh rate)
       * yields null, whereas asking for interval 300 (shortest historical interval) yields an
       * empty list; so we have to check for both cases.
       *
       * XXX: is it useful to populate start/end times here?
       */
      MetricId[] allMetrics = getCachedPerfMgr().queryAvailableMetric(target, null, null, interval);
      ArrayList<MetricId> metrics = new ArrayList<MetricId>();
      // filter metrics and get the specified one
      if (allMetrics != null) {
         for (MetricId metric: allMetrics) {
            if (counters.map.containsKey(metric.getCounterId())) {
               if (metric.getInstance().equals("")) { // only add one instance per counter
                  // XXX: does this get all cores for cpus? Should we not just construct a metric with (counter id, *)?
                  // See http://www.vmware.com/support/developer/vc-sdk/visdk41pubs/ApiReference/vim.PerformanceManager.MetricId.html
                  metrics.add(metric);
               }
            }
         }
      }
      if (metrics.isEmpty()) {
         logger.debug("No available metrics for counter " + counters.group + "::" + counters.name);
         throw VcException.PERFORMANCE_ERROR();
      }

      return metrics.toArray(new MetricId[metrics.size()]);
   }

   private PerformanceManager getCachedPerfMgr() {
      AuAssert.check(VcContext.isInSession());
      return VcContext.getService().getPerfManager();
   }

   private void refreshPerfCounters() {
      long curTime =  System.nanoTime();
      if (nextRefreshTime <= curTime) {
         sessionPerfCounters = null;
         nextRefreshTime = curTime + PERFCOUNTER_REFRESH_PERIOD;
      }
   }

   private CounterInfo[] getCachedPerfCounterList() {
      AuAssert.check(VcContext.isInSession());
      if (sessionPerfCounters == null) {
         sessionPerfCounters = getCachedPerfMgr().getPerfCounter();
      }
      return sessionPerfCounters;
   }

   /*
    * Helpers used internally.
    */

   private static class CountersByType {
      Map<Integer, CounterInfo> map;
      String name;
      String group;

      CountersByType() {
         map = new HashMap<Integer, CounterInfo>();
      }
   }

   private boolean isValidSamplingInterval(String targetId, int requested) {
      Integer[] intervals = getAllSamplingIntervals(targetId);
      for (Integer interval: intervals) {
         if (interval == requested) {
            return true;
         }
      }
      return false;
   }
}
