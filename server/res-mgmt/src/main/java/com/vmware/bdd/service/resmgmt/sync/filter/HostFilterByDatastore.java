package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.bdd.utils.AuAssert;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xiaoliangl on 8/3/15.
 */
public class HostFilterByDatastore implements IVcResourceFilter<VcHost> {
   private List<Pattern> regxList = new ArrayList<>();

   public HostFilterByDatastore(String[] vcDsNameRegxs) {
      AuAssert.check(ArrayUtils.isNotEmpty(vcDsNameRegxs), "can't build an empty name regx filter.");

      HashSet<String> nameRegSet = new HashSet<>();
      for (String name : vcDsNameRegxs) {
         nameRegSet.add(name);
      }

      for (String nameReg : nameRegSet) {
         Pattern pattern = Pattern.compile(nameReg);
         regxList.add(pattern);
      }
   }

   @Override
   public boolean isFiltered(VcHost vcHost) {
      boolean hasMatchedDs = false;

      if(CollectionUtils.isNotEmpty(vcHost.getDatastores())) {
         for(VcDatastore vcDatastore : vcHost.getDatastores()) {
            hasMatchedDs = isDatastoreMatched(vcDatastore);
            if(hasMatchedDs) {
               break;
            }
         }
      }


      return !hasMatchedDs;
   }

   public boolean isDatastoreMatched(VcDatastore vcDatastore) {
      boolean isMatched = false;
      for (Pattern regx : regxList) {
         isMatched = regx.matcher(vcDatastore.getName()).matches();
         if (isMatched) {
            break;
         }
      }

      return isMatched;
   }

   public String toString() {
      return new ToStringBuilder(this).append("datastore_regx", regxList).toString();
   }
}
