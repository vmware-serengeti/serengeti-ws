package com.vmware.bdd.service.resmgmt.impl;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Created by xiaoliangl on 7/28/15.
 */

@Component
@ManagedResource(objectName= "bean:name=VcInventorySyncCountersMBean" , description= "My First Bean" )
public class VcInventorySyncCounters {
   private AtomicBoolean refreshInProgress = new AtomicBoolean(false);
   private AtomicIntegerArray counters = new AtomicIntegerArray(3);

   @ManagedAttribute(description="The RefreshInProgress Attribute")
   public boolean isRefreshInProgress() {
      return refreshInProgress.get();
   }

   protected void setRefreshInProgress(boolean flag) {
      refreshInProgress.set(flag);
   }

   @ManagedAttribute(description="The InventoryRefresh Attribute")
   public int getInventoryRefresh() {
      return counters.get(0);
   }

   protected void increaseInvRefresh() {
      counters.incrementAndGet(0);
   }

   @ManagedAttribute
   public int getFinishedRefresh() {
      return counters.get(1);
   }

   protected void increaseFinishedRefresh() {
      counters.incrementAndGet(1);
   }

   @ManagedAttribute
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
