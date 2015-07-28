package com.vmware.bdd.service.resmgmt;

/**
 * Created by xiaoliangl on 7/27/15.
 */
public interface IVcInventorySyncService {
   void refreshInventory() throws InterruptedException;

   boolean isRefreshInProgress();

   void waitForCompletion() throws InterruptedException;
}
