package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcObject;

/**
 * Created by xiaoliangl on 8/7/15.
 */
public class VcResourceTypeFilter<T extends VcObject> implements IVcResourceFilter<T>  {
   @Override
   public boolean isFiltered(T vcObject) {
      return true;
   }
}
