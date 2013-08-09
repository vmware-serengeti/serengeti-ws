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
package com.vmware.aurora.vc.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcPerformance;
import com.vmware.aurora.vc.VcPerformance.PerformanceSample;
import com.vmware.aurora.vc.VcPerformance.PerformanceType;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcService;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.ElementDescription;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.PerformanceManager;
import com.vmware.vim.binding.vim.PerformanceManager.CounterInfo;
import com.vmware.vim.binding.vim.VirtualApp;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Test code to develop VcPerformance class.
 */
public class VcPerformanceTest extends AbstractVcTest {

   /**
    * Returns all datacenters in the root folder.
    */
   public static List<Datacenter> getDatacenters() throws Exception {
      Folder rootFolder = MoUtil.getRootFolder();
      List<Datacenter> dcList = MoUtil.getChildEntity(rootFolder, Datacenter.class);
      return dcList;
   }

   /**
    * Returns all resource pools in the data center.
    */
   public static List<VcResourcePool> getResourcePools(VcCluster cluster) throws Exception {
      return cluster.getAllRPs();
   }

   /**
    * Returns all virtual machines in the data center.
    */
   public static List<VirtualMachine> getVirtualMachines(Datacenter dc) throws Exception {
      Folder vmFolder = MoUtil.getManagedObject(dc.getVmFolder());
      List<VirtualMachine> list = MoUtil.getChildEntity(vmFolder, VirtualMachine.class);
      return list;
   }

   private VcPerformance vcp = new VcPerformance();

   /**
    * Tests VcPerformance.queryPerformance for both VMs and RPs
    * @throws Exception
    */
   @Test
   public void testGetPerformance() throws Exception {
      //enumClusterPerformance();
      //dumpCounters();
      //dumpFailingRPs();
      //watchSingleRP("null:ResourcePool:resgroup-4506", 120, 10000); // every 10 seconds for 20 minutes
      enumClusterLatestPerformance();
   }

   public void enumClusterPerformance() throws Exception {
      System.out.println("Service url is: " + vcService.getServiceUrl());
      System.out.println("Supported aggregated sampling intervals: " + listToString(vcp.getHistoricalIntervals()));

      // iterate clusters to iterate resource pools
      for (VcCluster cluster : VcInventory.getClusters()) {
         System.out.println("CL " + cluster);
         for (VcResourcePool rp: getResourcePools(cluster)) {
            System.out.println("  RP " + rp + ":");
            String refId = rp.getId();
            try {
               String perf;
               Integer[] intervals = vcp.getAllSamplingIntervals(rp.getId());
               int resolution = intervals[0]; // fastest sampling supported
               //System.out.println("     supports sampling intervals: " + listToString(intervals));

               Calendar start = vcp.queryVcCurrentTime();
               start.add(Calendar.MINUTE, -60); // XXX: queries for < 30 minutes are returning nothing for RPs (but work for VMs)
               Calendar end = null;

               System.out.println(String.format("     %s from %s to %s", refId, new SimpleDateFormat().format(start.getTime()), new SimpleDateFormat().format(vcp.queryVcCurrentTime().getTime())));

               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.cpuAbs, resolution, 0, start, end));
               System.out.println("     cpu performance samples: " + perf);
               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.memOccupiedAbs, resolution, 0, start, end));
               System.out.println("     mem-occupy performance samples: " + perf);
               // vApps are enumerated as RPs, but don't support mem.cap.usage, so don't try this on them
               if (!MoUtil.isOfType(refId, VirtualApp.class)) {
                  perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.memActiveAbs, resolution, 0, start, end));
                  System.out.println("     mem-active performance samples: " + perf);
               }
               // Note: RP doesn't implement relative stats (cpu::usage or mem::usage), or mem::active, or net::anything
               List<String> ids = new ArrayList<String>();
               ids.add(refId);
               Map<String, PerformanceSample> result = vcp.queryLatestPerformance(ids, PerformanceType.cpuAbs);
               PerformanceSample sample= result.get(refId);
               perf = (sample == null ? "unavailable" : sample.toString());
               System.out.println("     latest cpu sample: " + perf);
            } catch (Exception e) {
               System.out.println("     perf query failed: " + e.getMessage());
            }
         }
      }

      // iterate datacenters to iterate virtual machines
      for (Datacenter dc : getDatacenters()) {
         System.out.println("DC " + dc);
         for (VirtualMachine vm: getVirtualMachines(dc)) {
            // Skip templates, which can't be powered on and have no associated performance data
            if (vm.getConfig().isTemplate()) {
               continue;
            }

            System.out.println("VM " + vm.getName() + ":");
            String refId = MoUtil.morefToString(vm._getRef());
            try {
               String perf;
               Integer[] intervals = vcp.getAllSamplingIntervals(refId);
               int resolution = intervals[0]; // fastest sampling supported
               //System.out.println("   supports sampling intervals: " + listToString(intervals));

               // NB: if you pass times (relative to present, minutes ago) 0,0 (all recent samples): get lots, some are very recent
               // if you pass 60,0 (all samples in last hour): tend to get 11 samples, missing recent one, newest sample can be 9 minutes old
               // if you pass 30,0 (all samples in last half hour): tend to get 5 samples, missing recent one, newest sample can be 9 minutes old; optimization not kicking in
               // if you pass 25,0 (all samples in last 25 min): get 5 samples, including most recent; the "optimization" apparently kicks in
               // best realtime query might be for 5,0? But that involves knowing the interval; our API should have a way to get just newest sample without knowing interval.
               // Anyway, note that 0,0, <30,0 and >30,0 are really different queries.
               Calendar start = vcp.queryVcCurrentTime();
               start.add(Calendar.MINUTE, -20);
               Calendar end = null;

               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.cpuAbs, resolution, 0, start, end));
               System.out.println("   cpu performance samples: " + perf);
               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.cpuRel, resolution, 0, start, end));
               System.out.println("   cpu performance samples: " + perf);
               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.memOccupiedAbs, resolution, 0, start, end));
               System.out.println("   mem-occupy performance samples: " + perf);
               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.memActiveAbs, resolution, 0, start, end));
               System.out.println("   mem-active performance samples: " + perf);
               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.memActiveRel, resolution, 0, start, end));
               System.out.println("   mem-active performance samples: " + perf);
               perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.netAbs, resolution, 0, start, end));
               System.out.println("   net performance samples: " + perf);
               List<String> ids = new ArrayList<String>();
               ids.add(refId);
               Map<String, PerformanceSample> result = vcp.queryLatestPerformance(ids, PerformanceType.cpuAbs);
               PerformanceSample sample= result.get(refId);
               perf = (sample == null ? "unavailable" : sample.toString());
               System.out.println("   latest cpu sample: " + perf);
            } catch (Exception e) {
               System.out.println("   perf query failed: " + e.getMessage());
            }
         }
      }
   }

   private void evalLatestSamples(String refId, int resolution, Calendar start, Calendar end) {
      try {
         String perf;
         System.out.println(String.format("     %s from %s to %s", refId, new SimpleDateFormat().format(start.getTime()), new SimpleDateFormat().format(vcp.queryVcCurrentTime().getTime())));
         PerformanceSample[] samples = vcp.queryPerformance(refId, PerformanceType.cpuAbs, resolution, 0, start, end);
         if (samples.length < 2) {
            System.out.println("    no samples in window");
         } else {
            PerformanceSample[] slice = new PerformanceSample[2];
            slice[0] = samples[samples.length - 2];
            slice[1] = samples[samples.length - 1];
            perf = samplesToString(slice);
            System.out.println("   last 2 cpu performance samples: " + perf);

            List<String> ids = new ArrayList<String>();
            ids.add(refId);
            Map<String, PerformanceSample> result = vcp.queryLatestPerformance(ids, PerformanceType.cpuAbs);
            PerformanceSample sample= result.get(refId);
            perf = (sample == null ? "unavailable" : sample.toString());
            System.out.println("   'latest' cpu sample: " + perf);

            if (sample.getTimestamp().equals(slice[1].getTimestamp())) {
               // we kept the newest sample
               System.out.println("tie! kept newest sample");
            } else if (slice[1].getSample() == 0 && sample.getSample() != 0) {
               // newest sample is 0, but we didn't use it
               System.out.println("win! avoided invalid 0 sample");
            } else if (slice[1].getSample() != 0 && sample.getTimestamp().equals(slice[0].getTimestamp())) {
               // latest sample is nonzero, and we didn't use it
               System.out.println("fail! had to skip a valid sample");
            } else {
               // logic error
               System.out.println("inconceivable! not sure how to judge that one");
            }
         }
      } catch (Exception e) {
         System.out.println("   perf query failed: " + e.getMessage());
      }
   }

   public void enumClusterLatestPerformance() throws Exception {
      System.out.println("Service url is: " + vcService.getServiceUrl());
      System.out.println("Supported aggregated sampling intervals: " + listToString(vcp.getHistoricalIntervals()));

      // iterate clusters to iterate resource pools
      for (VcCluster cluster : VcInventory.getClusters()) {
         System.out.println("CL " + cluster);
         for (VcResourcePool rp: getResourcePools(cluster)) {
            System.out.println("  RP " + rp + ":");
            String refId = rp.getId();
            Integer[] intervals = vcp.getAllSamplingIntervals(rp.getId());
            int resolution = intervals[0]; // fastest sampling supported

            Calendar start = vcp.queryVcCurrentTime();
            start.add(Calendar.MINUTE, -60); // XXX: queries for < 30 minutes are returning nothing for RPs (but work for VMs)
            Calendar end = null;

            evalLatestSamples(refId, resolution, start, end);
         }
      }

      // iterate datacenters to iterate virtual machines
      for (Datacenter dc : getDatacenters()) {
         System.out.println("DC " + dc);
         for (VirtualMachine vm: getVirtualMachines(dc)) {
            // Skip templates, which can't be powered on and have no associated performance data
            if (vm.getConfig().isTemplate()) {
               continue;
            }

            System.out.println("VM " + vm.getName() + ":");
            String refId = MoUtil.morefToString(vm._getRef());
            Integer[] intervals = vcp.getAllSamplingIntervals(refId);
            int resolution = intervals[0]; // fastest sampling supported

            Calendar start = vcp.queryVcCurrentTime();
            start.add(Calendar.MINUTE, -20);
            Calendar end = null;

            evalLatestSamples(refId, resolution, start, end);
         }
      }
   }

   public void dumpCounters() throws Exception {
      VcService vcService = VcContext.getService();
      ManagedObjectReference pmRef= vcService.getServiceInstanceContent().getPerfManager();
      PerformanceManager perfMgr = MoUtil.getManagedObject(pmRef);
      CounterInfo[] allCounters = perfMgr.getPerfCounter();
      for (CounterInfo c: allCounters) {
         int key = c.getKey();
         String group = formatElementInfo(c.getGroupInfo());
         String name = formatElementInfo(c.getNameInfo());
         String unit = formatElementInfo(c.getUnitInfo());
         String type = c.getStatsType().toString();

         System.out.println(String.format("Counter %d: %s: group=%s, name=%s, unit=%s", key, type, group, name, unit));
      }
   }

   public void dumpFailingRPs() throws Exception {
      /*
       * Print a list of RPs for which queryLatestPerformance() fails (which could mean queryPerformance() threw
       * an exception for any of the reasons it could do so, but most likely means it returned 0 samples, in
       * which case queryLatestPerformance will throw its own exception).
       *
       * In practice, this seems to happen for RPs with no powered-on VMs (as direct children or descendants).
       */
      System.out.println("Service url is: " + vcService.getServiceUrl());
      System.out.println("Supported aggregated sampling intervals: " + listToString(vcp.getHistoricalIntervals()));
      for (Datacenter dc : getDatacenters()) {
         System.out.println("DC " + dc);
         for (VcCluster cluster : VcInventory.getClusters()) {
            System.out.println("CL " + cluster);
            for (VcResourcePool rp: getResourcePools(cluster)) {
               String refId = rp.getId();
               List<String> ids = new ArrayList<String>();
               ids.add(refId);
               Map<String, PerformanceSample> result = vcp.queryLatestPerformance(ids, PerformanceType.cpuAbs);
               PerformanceSample sample= result.get(refId);
               if (null == sample) {
                  System.out.println("     FAILED: " + rp);
               }
            }
         }
      }
   }

   public void watchSingleRP(String refId, int repeatCount, int delayMs) throws Exception {
      VcResourcePool rp = VcCache.get(refId);

      int resolution = vcp.getAllSamplingIntervals(rp.getId())[0];

      System.out.println(String.format("Watching %s every %d seconds for %d repetitions", refId, delayMs / 1000, repeatCount));
      for (int i = 0; i < repeatCount; i++) {
         Calendar start = vcp.queryVcCurrentTime();
         start.add(Calendar.MINUTE, -60); // XXX: queries for < 30 minutes return nothing for RPs
         Calendar end = vcp.queryVcCurrentTime();

         System.out.println(String.format("     %s from %s to %s", refId, new SimpleDateFormat().format(start.getTime()), new SimpleDateFormat().format(end.getTime())));
         String perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.cpuAbs, resolution, 0, start, end));
         System.out.println("     cpu performance samples: " + perf);
         perf = samplesToString(vcp.queryPerformance(refId, PerformanceType.memOccupiedAbs, resolution, 0, start, end));
         System.out.println("     mem performance samples: " + perf);

         Thread.sleep(delayMs);
      }
   }

   String formatElementInfo(ElementDescription e) {
      return String.format("(%s, %s, %s)", e.getKey(), e.getLabel(), e.getSummary());
   }

   private static String samplesToString(PerformanceSample[] samples) {
      return samples.length + " samples: " + listToString(samples);
   }

   private static String listToString(Object[] list) {
      String s = "";
      for (Object item: list) {
         if (s.length() > 0) {
            s += ", ";
         }
         s += item.toString();
      }
      return s;
   }
}
