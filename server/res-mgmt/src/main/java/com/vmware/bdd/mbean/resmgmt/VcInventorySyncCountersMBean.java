package com.vmware.bdd.mbean.resmgmt;

/**
 * Created by xiaoliangl on 7/28/15.
 */
public interface VcInventorySyncCountersMBean {
   boolean isRefreshInProgress();
   int getInventoryRefresh();
   int getFinishedRefresh();
   int getPendingRefresh();
}
