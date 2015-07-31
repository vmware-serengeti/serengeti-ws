package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.bdd.utils.AuAssert;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Created by xiaoliangl on 8/3/15.
 */
public class HostFilterByNetwork implements IVcResourceFilter<VcHost> {
   private HashSet<String> networkNameSet = new HashSet<>();

   public HostFilterByNetwork(String[] vcNetworkNames) {
      AuAssert.check(ArrayUtils.isNotEmpty(vcNetworkNames), "can't build an empty name regx filter.");

      for (String name : vcNetworkNames) {
         networkNameSet.add(name);
      }
   }

   @Override
   public boolean isFiltered(VcHost vcHost) {
      int matchedNetworkCount = 0;
      if(CollectionUtils.isNotEmpty(vcHost.getNetworks())) {
         for(VcNetwork vcNetwork : vcHost.getNetworks()) {
            if(networkNameSet.contains(vcNetwork.getName())) {
               matchedNetworkCount ++;
            }
         }
      }


      return matchedNetworkCount == networkNameSet.size();
   }

}
