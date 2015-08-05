package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.utils.AuAssert;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xiaoliangl on 8/3/15.
 */
class VcResourceNameRegFilter<T extends VcObject> implements IVcResourceFilter<T> {
   private List<Pattern> regxList = new ArrayList<>();

   public VcResourceNameRegFilter(String[] nameRegs) {
      AuAssert.check(ArrayUtils.isNotEmpty(nameRegs), "can't build an empty name regx filter.");

      HashSet<String> nameRegSet = new HashSet<>();
      for (String name : nameRegs) {
         nameRegSet.add(name);
      }

      for (String nameReg : nameRegSet) {
         Pattern pattern = Pattern.compile(nameReg);
         regxList.add(pattern);
      }

   }

   @Override
   public boolean isFiltered(T vcObject) {
      boolean isMatched = false;
      for (Pattern regx : regxList) {
         isMatched = regx.matcher(vcObject.getName()).matches();
         if (isMatched) {
            break;
         }
      }

      return !isMatched;
   }

   public String toString() {
      return new ToStringBuilder(this).append("regxList", regxList).toString();
   }
}
