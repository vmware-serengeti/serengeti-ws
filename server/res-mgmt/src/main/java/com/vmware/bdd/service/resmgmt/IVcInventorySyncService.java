package com.vmware.bdd.service.resmgmt;

import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;

/**
 * Created by xiaoliangl on 7/27/15.
 */
public interface IVcInventorySyncService {
   void refreshInventory() throws InterruptedException;

   void refreshInventory(VcResourceFilters vcResourceFilters) throws InterruptedException;

   boolean isRefreshInProgress();

   void waitForCompletion() throws InterruptedException;

   void asyncRefreshInventory(VcResourceFilters filters);
}
