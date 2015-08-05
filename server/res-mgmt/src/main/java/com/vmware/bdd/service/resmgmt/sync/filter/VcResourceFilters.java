package com.vmware.bdd.service.resmgmt.sync.filter;

import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.service.resmgmt.VC_RESOURCE_TYPE;
import com.vmware.bdd.utils.AuAssert;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by xiaoliangl on 7/30/15.
 */
public class VcResourceFilters {
   private final static Logger LOGGER = Logger.getLogger(VcResourceFilters.class);

   private Map<VC_RESOURCE_TYPE, List<IVcResourceFilter>> filterMap = new HashMap<>();

   public <T extends VcObject> VcResourceFilters addNameFilter(VC_RESOURCE_TYPE resourceClass, String[] resourceNames, boolean isReg) {
      if(ArrayUtils.isNotEmpty(resourceNames)) {
         List<IVcResourceFilter> vcResourceFilterList = filterMap.get(resourceClass);
         if(vcResourceFilterList == null) {
            vcResourceFilterList = new ArrayList<>();
            filterMap.put(resourceClass, vcResourceFilterList);
         }


         if(isReg) {
            vcResourceFilterList.add(new VcResourceNameRegFilter<T>(resourceNames));
         } else {
            vcResourceFilterList.add(new VcResourceNameFilter<T>(resourceNames));
         }
      } else {
         LOGGER.warn("can't create an empty vc resource filter!");
      }

      return this;
   }

   public VcResourceFilters addHostFilterByDatastore(String[] vcDsNameRegxs) {
      if(ArrayUtils.isNotEmpty(vcDsNameRegxs)) {
         List<IVcResourceFilter> vcResourceFilterList = filterMap.get(VC_RESOURCE_TYPE.HOST);
         if (vcResourceFilterList == null) {
            vcResourceFilterList = new ArrayList<>();
            filterMap.put(VC_RESOURCE_TYPE.HOST, vcResourceFilterList);
         }

         vcResourceFilterList.add(new HostFilterByDatastore(vcDsNameRegxs));
      } else {
         LOGGER.warn("can't create an empty host by datastore filter!");
      }


      return this;
   }


   public VcResourceFilters addHostFilterByNetwork(String[] vcNetworkNames) {
      if(ArrayUtils.isNotEmpty(vcNetworkNames)) {
         List<IVcResourceFilter> vcResourceFilterList = filterMap.get(VC_RESOURCE_TYPE.HOST);
         if (vcResourceFilterList == null) {
            vcResourceFilterList = new ArrayList<>();
            filterMap.put(VC_RESOURCE_TYPE.HOST, vcResourceFilterList);
         }

         vcResourceFilterList.add(new HostFilterByNetwork(vcNetworkNames));
      } else {
         LOGGER.warn("can't create an empty host by datastore filter!");
      }


      return this;
   }

   public <T extends VcObject> List<T> filter(List<T> vcObjects) {
      if(CollectionUtils.isEmpty(vcObjects)) {
         return vcObjects;
      } else {
         List<T> filteredList = new ArrayList<>();
         for(T vcObject : vcObjects) {
            if(!isFiltered(vcObject)) {
               filteredList.add(vcObject);
            }
         }

         return filteredList;
      }
   }

   public <T extends VcObject> boolean isFiltered(T vcObject) {
      AuAssert.check(vcObject != null, "can't filter a null vc resource!");

      List<IVcResourceFilter> filterList = filterMap.get(VC_RESOURCE_TYPE.fromType(vcObject));

      boolean isFiltered = false;
      if(CollectionUtils.isNotEmpty(filterList)) {
         for (IVcResourceFilter filter : filterList) {
            isFiltered = filter.isFiltered(vcObject);

            if(LOGGER.isDebugEnabled()) {
               LOGGER.debug(vcObject.getName() + " is filtered by " + filter + ": " + isFiltered);
            }

            if(isFiltered) {
               break;
            }
         }
      }

      return isFiltered;
   }
}
