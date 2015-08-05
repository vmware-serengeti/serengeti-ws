package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.utils.AuAssert;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashSet;

/**
 * Created by xiaoliangl on 8/3/15.
 */
class VcResourceNameFilter<T extends VcObject> implements IVcResourceFilter<T> {
   private HashSet<String> nameSet = new HashSet<>();


   public VcResourceNameFilter(String[] names) {
      AuAssert.check(ArrayUtils.isNotEmpty(names), "can't build an empty name regx filter.");
      for (String name : names) {
         nameSet.add(name);
      }
   }

   @Override
   public boolean isFiltered(T vcObject) {
      return !nameSet.contains(vcObject.getName());
   }

   public String toString() {
      return new ToStringBuilder(this).append("nameSet", nameSet).toString();
   }
}
