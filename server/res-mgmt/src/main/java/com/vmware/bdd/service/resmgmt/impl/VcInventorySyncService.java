package com.vmware.bdd.service.resmgmt.impl;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.service.resmgmt.IVcInventorySyncService;
import com.vmware.bdd.service.resmgmt.sync.AbstractSyncVcResSP;
import com.vmware.bdd.service.resmgmt.sync.SyncVcResourceSp;
import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xiaoliangl on 7/22/15.
 */
@Service
public class VcInventorySyncService implements IVcInventorySyncService {
   private static final Logger LOGGER = Logger.getLogger(VcInventorySyncService.class);

   private ExecutorService es = Executors.newFixedThreadPool(10);

   private AtomicBoolean inProgress = new AtomicBoolean(false);

   @Autowired
   private VcInventorySyncCounters counters;

   private long waitMilliSecs = 100l;

   public void setCounters(VcInventorySyncCounters counters) {
      this.counters = counters;
   }

   /**
    * refresh the vc inventory, some resource will be filtered from refreshing.
    */
   @Override
   public void refreshInventory() throws InterruptedException {
      refreshInventory(null);
   }

   /**
    * @param vcResourceFilters
    * refresh the vc inventory, some resource will be filtered from refreshing.
    */
   public void refreshInventory(VcResourceFilters vcResourceFilters) throws InterruptedException {
      if (inProgress.get()) {
         if(LOGGER.isInfoEnabled()) {
            LOGGER.info("a inventory refresh is already in progress. wait for its finishing.");
         }
         waitForCompletion();
         if(LOGGER.isInfoEnabled()) {
            LOGGER.info("wait for refreshing is done.");
         }
      } else {
         if (inProgress.compareAndSet(false, true)) {
            if(LOGGER.isInfoEnabled()) {
               LOGGER.info("start a inventory refresh.");
            }
            counters.setRefreshInProgress(true);
            counters.increaseInvRefresh();
            try {
               List<VcDatacenter> dcList = VcInventory.getDatacenters();
               refresh(vcResourceFilters == null ? dcList: vcResourceFilters.filter(dcList), vcResourceFilters);
            } finally {
               counters.setRefreshInProgress(false);
               inProgress.set(false);
               if(LOGGER.isInfoEnabled()) {
                  LOGGER.info("the inventory refresh is done.");
               }
            }
         } else {
            if(LOGGER.isInfoEnabled()) {
               LOGGER.info("another thread has initiated a inventory refresh.  wait it finishing.");
            }
            waitForCompletion();
            if(LOGGER.isInfoEnabled()) {
               LOGGER.info("wait for refreshing is done.");
            }
         }
      }
   }


   @Override
   public void asyncRefreshInventory(final VcResourceFilters filters) {
      if(LOGGER.isInfoEnabled()) {
         LOGGER.info("trigger asyncRefreshInventory.");
      }
      es.submit(new Runnable() {
         @Override
         public void run() {
            try {
               if(LOGGER.isInfoEnabled()) {
                  LOGGER.info("asyncRefreshInventory started.");
               }
               refreshInventory(filters);
               if(LOGGER.isInfoEnabled()) {
                  LOGGER.info("asyncRefreshInventory end.");
               }
            } catch (InterruptedException e) {
               LOGGER.error("asyncRefreshInventory failed", e);
            }
         }
      });
   }

   @Override
   public boolean isRefreshInProgress() {
      return inProgress.get();
   }

   /**
    * wait for in progress refresh's completion
    *
    * @throws InterruptedException if the wait is interrupted
    */
   public void waitForCompletion() throws InterruptedException {
      while (inProgress.get()) {
         Thread.sleep(waitMilliSecs);
      }
   }

   /**
    * refresh vc resources concurrently.
    * @param vcResourceFilters vc resource filters.
    * @param vcObjects vc resource objects to refresh
    */
   protected <T extends VcObject> void refresh(List<T> vcObjects, VcResourceFilters vcResourceFilters) {
      AuAssert.check(CollectionUtils.isNotEmpty(vcObjects), "no vc resources to refresh!");


      List<AbstractSyncVcResSP> syncSps = new ArrayList<>();

      for (T vcObject : vcObjects) {
         SyncVcResourceSp syncVcResourceSp = new SyncVcResourceSp(vcObject.getMoRef());
         syncVcResourceSp.setVcResourceFilters(vcResourceFilters);
         syncSps.add(syncVcResourceSp);
      }

      try {
         work(es, syncSps);
      } catch (Exception ex) {
         LOGGER.error("refresh Vc Resources failed.", ex);
      }
   }

   private List<Future<List<AbstractSyncVcResSP>>> submit(ExecutorService es, List<AbstractSyncVcResSP> syncSps) {

      if (CollectionUtils.isNotEmpty(syncSps)) {
         List<Future<List<AbstractSyncVcResSP>>> newRefreshTaskList = new ArrayList();
         for (AbstractSyncVcResSP sp : syncSps) {
            counters.increasePendingRefresh();
            newRefreshTaskList.add(es.submit(sp));
         }
         return newRefreshTaskList;
      } else {
         return Collections.EMPTY_LIST;
      }
   }

   private void work(ExecutorService es, List<AbstractSyncVcResSP> syncSps) throws ExecutionException, InterruptedException {
      List<Future<List<AbstractSyncVcResSP>>> resultList = submit(es, syncSps);

      while (resultList.size() > 0) {
         Thread.sleep(waitMilliSecs);

         List<AbstractSyncVcResSP> newTaskList = new ArrayList<>();
         for (Iterator<Future<List<AbstractSyncVcResSP>>> resultIterator = resultList.iterator(); resultIterator.hasNext(); ) {
            Future<List<AbstractSyncVcResSP>> result = resultIterator.next();
            if (result.isDone()) {
               counters.decreasePendingRefresh();
               counters.increaseFinishedRefresh();

               newTaskList.addAll(result.get());
               resultIterator.remove();
            }
         }

         resultList.addAll(submit(es, newTaskList));

         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("remain refresh task count: " + resultList.size());
         }
      }
   }
}
