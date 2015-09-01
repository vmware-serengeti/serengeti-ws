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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.stats.Profiler;
import com.vmware.aurora.stats.StatsType;
import com.vmware.aurora.util.worker.CmsWorker;
import com.vmware.aurora.util.worker.CmsWorker.WorkQueue;
import com.vmware.aurora.util.worker.PeriodicRequest;
import com.vmware.aurora.util.worker.SimpleRequest;
import com.vmware.aurora.vc.VcObject.VcObjectType;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import org.apache.log4j.Logger;

public class VcInventory {
   private final static Logger LOGGER = Logger.getLogger(VcInventory.class);

   static List<ManagedObjectReference> dcList = new ArrayList<ManagedObjectReference>();

   /**
    * Request to sync VC inventory object.
    */
   protected abstract static class SyncRequest extends SimpleRequest {
      // MoRef of the target VC object.
      final protected ManagedObjectReference moRef;
      // Specification of types of VC objects to sync.
      final protected EnumSet<VcObjectType> syncSet;
      // Work queue for adding sync requests.
      final private WorkQueue queue;
      // true if the target object should be added to VcCache if it doesn't exist
      final private boolean forceLoad;

      SyncRequest(ManagedObjectReference moRef, WorkQueue queue, boolean forceLoad,
                  EnumSet<VcObjectType> syncSet) {
         super(Profiler.getStatsEntry(StatsType.VCSYNC_INVENTORY, moRef));
         this.moRef = moRef;
         this.queue = queue;
         this.forceLoad = forceLoad;
         this.syncSet = syncSet;
      }

      SyncRequest(ManagedObjectReference moRef, SyncRequest parent) {
         this(moRef, parent.queue, parent.forceLoad, parent.syncSet);
      }

      protected void add(SyncRequest request) {
         CmsWorker.addRequest(queue, request);
      }

      /**
       * Sync child objects contained in this object.
       * @param obj
       */
      abstract void syncChildObjects(VcObject obj);

      @Override
      protected boolean execute() {
         VcObject obj;
         if (forceLoad) {
            obj = VcCache.load(moRef);
         } else {
            obj = VcCache.lookup(moRef);
         }
         if (obj instanceof VcObject) {
            syncChildObjects(obj);
            if (!forceLoad && syncSet.contains(obj.getVcObjectType())) {
               VcCache.loadAsync(moRef);
            }
         }
         return true;
      }
   }

   /**
    * Sync the root of VC inventory: all datacenters.
    */
   private static class SyncRootRequest extends SimpleRequest {
      private WorkQueue queue;
      private boolean forceLoad;
      private EnumSet<VcObjectType> syncSet;
      SyncRootRequest(WorkQueue queue, boolean forceLoad, EnumSet<VcObjectType> syncSet) {
         super(Profiler.getStatsEntry(StatsType.VCSYNC_INVENTORY));
         this.queue = queue;
         this.forceLoad = forceLoad;
         this.syncSet = syncSet;
      }

      @Override
      protected boolean execute() {
         if(LOGGER.isInfoEnabled()) {
            LOGGER.info("execute all datacenters sync.");
         }
         VcContext.inVcSessionDo(new VcSession<Void>() {
            public Void body() throws Exception {
               VcInventory.update();
               return null;
            }
         });
         for (ManagedObjectReference dc : dcList) {
            CmsWorker.addRequest(queue, new VcDatacenterImpl.SyncRequest(dc, queue, forceLoad, syncSet));
         }
         return true;
      }
   }

   /**
    * Periodically update existing inventory objects in VcCache.
    * We don't need to sync RPs as they are updated when we receive RP VcEvents.
    */
   public static class SyncInventoryRequest extends PeriodicRequest {
      private final static WorkQueue SYNC_INVENTORY_WORK_QUEUE = WorkQueue.VC_CACHE_TWO_MIN_DELAY;

      private final static long SYNC_INTERVAL_IN_MILLISEC=Configuration.getInt(Constants.VC_INVENTORY_SYNC_INTERVAL_IN_SECOND, 120) * 1000;

      public SyncInventoryRequest() {
         super(Profiler.getStatsEntry(StatsType.VCSYNC_INVENTORY_PERIOD),
               SYNC_INVENTORY_WORK_QUEUE, SYNC_INTERVAL_IN_MILLISEC);

            if(LOGGER.isInfoEnabled()) {
               LOGGER.info("set vc inventory sync's interval to " + SYNC_INTERVAL_IN_MILLISEC);
            }
      }

      @Override
      protected boolean executeOnce() {
         if(LOGGER.isInfoEnabled()) {
            LOGGER.info("execute vc inventory sync.");
         }
         EnumSet<VcObjectType> syncSet = EnumSet.of(
               VcObjectType.VC_DATACENTER, VcObjectType.VC_CLUSTER,
               VcObjectType.VC_DATASTORE, VcObjectType.VC_NETWORK,
               VcObjectType.VC_DVPORTGROUP);
         queueRequest(new SyncRootRequest(SYNC_INVENTORY_WORK_QUEUE, false, syncSet));
         return true;
      }
   }

   protected static void update() throws Exception {
      Folder rootFolder = MoUtil.getRootFolder();
      dcList = MoUtil.getDescendantsMoRef(rootFolder, Datacenter.class);
   }

   /**
    * Load all inventory objects into VcCache.
    */
   public static void loadInventory() {
      CmsWorker.addRequest(WorkQueue.VC_CACHE_NO_DELAY,
            new SyncRootRequest(WorkQueue.VC_CACHE_NO_DELAY, true,
                  EnumSet.allOf(VcObjectType.class)));
   }

   /**
    * @return all datacenters in the Virtual Center.
    */
   static public List<VcDatacenter> getDatacenters() {
      return VcCache.<VcDatacenter>getList(dcList);
   }

   /**
    * @return all clusters in the Virtual Center.
    */
   static public List<VcCluster> getClusters() {
      ArrayList<VcCluster> results = new ArrayList<VcCluster>();
      List<VcDatacenter> dcList = getDatacenters();
      for (VcDatacenter dc : dcList) {
         results.addAll(dc.getVcClusters());
      }
      return results;
   }

   /**
    * @return all clusters under the specified DataCenter in the Virtual Center.
    */
   static public List<VcCluster> getClustersInDatacenter(String datacenterName) {
      ArrayList<VcCluster> results = new ArrayList<VcCluster>();
      List<VcDatacenter> dcList = getDatacenters();
      for (VcDatacenter dc : dcList) {
         if (datacenterName == null || datacenterName.equalsIgnoreCase(dc.getName())) {
            results.addAll(dc.getVcClusters());
         }
      }
      return results;
   }
}
