package com.vmware.bdd.service.resmgmt.impl;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Created by xiaoliangl on 7/28/15.
 */
public interface VcInventorySyncCountersMBean {
   boolean isRefreshInProgress();
   int getInventoryRefresh();
   int getFinishedRefresh();
   int getPendingRefresh();
}
