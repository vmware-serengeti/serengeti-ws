package com.vmware.bdd.service.resmgmt.impl;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.service.resmgmt.IVcInventorySyncService;
import com.vmware.bdd.service.resmgmt.sync.AbstractSyncVcResSP;
import com.vmware.bdd.service.resmgmt.sync.SyncVcResourceSp;
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

   private VcInventorySyncCounters counters = new VcInventorySyncCounters();

   public void setCounters(VcInventorySyncCounters counters) {
      this.counters = counters;
   }

   /**
    * refresh the whole vc inventory
    */
   public void refreshInventory() throws InterruptedException {
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
               refresh(dcList);
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
         Thread.sleep(50);
      }
   }

   /**
    * refresh vc resources concurrently.
    *
    * @param vcObjects vc resource objects to refresh
    */
   protected <T extends VcObject> void refresh(List<T> vcObjects) {
      AuAssert.check(CollectionUtils.isNotEmpty(vcObjects), "no vc resources to refresh!");


      List<AbstractSyncVcResSP> syncSps = new ArrayList<>();

      for (VcObject vcObject : vcObjects) {
         SyncVcResourceSp syncVcResourceSp = new SyncVcResourceSp(vcObject.getMoRef());
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
      List<Future<List<AbstractSyncVcResSP>>> refreshTaskList = submit(es, syncSps);

      while (refreshTaskList.size() > 0) {
         List<Future<List<AbstractSyncVcResSP>>> newRefreshTaskList = new ArrayList<>();
         for (Iterator<Future<List<AbstractSyncVcResSP>>> iterator = refreshTaskList.iterator(); iterator.hasNext(); ) {
            Future<List<AbstractSyncVcResSP>> result = iterator.next();
            if (result.isDone()) {
               counters.decreasePendingRefresh();
               counters.increaseFinishedRefresh();
               newRefreshTaskList.addAll(submit(es, result.get()));
               iterator.remove();
            }
         }

         Thread.sleep(50);

         refreshTaskList.addAll(newRefreshTaskList);

         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("remain refresh task count: " + refreshTaskList.size());
         }
      }
   }
}
