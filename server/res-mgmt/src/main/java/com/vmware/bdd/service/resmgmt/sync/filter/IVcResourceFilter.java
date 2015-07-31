package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcObject;

import java.util.List;

/**
 * Created by xiaoliangl on 7/30/15.
 */
public interface IVcResourceFilter<T extends VcObject> {
   boolean isFiltered(T vcObject);
}
