package com.vmware.bdd.service.resmgmt.impl;

import com.vmware.bdd.mbean.resmgmt.VcInventorySyncCountersMBean;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Created by xiaoliangl on 7/28/15.
 */
@ManagedResource
public class VcInventorySyncCounters implements VcInventorySyncCountersMBean {
   private AtomicBoolean refreshInProgress = new AtomicBoolean(false);

   private AtomicIntegerArray counters = new AtomicIntegerArray(3);

   @Override
   public boolean isRefreshInProgress() {
      return refreshInProgress.get();
   }

   protected void setRefreshInProgress(boolean flag) {
      refreshInProgress.set(flag);
   }

   @Override
   public int getInventoryRefresh() {
      return counters.get(0);
   }

   protected void increaseInvRefresh() {
      counters.incrementAndGet(0);
   }

   @Override
   public int getFinishedRefresh() {
      return counters.get(1);
   }

   protected void increaseFinishedRefresh() {
      counters.incrementAndGet(1);
   }

   @Override
   public int getPendingRefresh() {
      return counters.get(2);
   }

   protected void increasePendingRefresh() {
      counters.incrementAndGet(2);
   }

   public void decreasePendingRefresh() {
      counters.decrementAndGet(2);
   }
}
