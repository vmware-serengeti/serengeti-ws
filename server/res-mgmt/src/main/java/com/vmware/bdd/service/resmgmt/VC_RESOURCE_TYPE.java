package com.vmware.bdd.service.resmgmt;

import com.vmware.aurora.vc.*;

/**
 * Created by xiaoliangl on 8/3/15.
 */
public enum VC_RESOURCE_TYPE {
   DATA_CENTER, CLUSTER, DATA_STORE, HOST, RESOURCE_POOL;

   public static VC_RESOURCE_TYPE fromType(VcObject vcObject) {
      if(vcObject instanceof VcDatacenter) {
         return DATA_CENTER;
      } else if(vcObject instanceof VcCluster) {
         return CLUSTER;
      } else if(vcObject instanceof VcDatastore) {
         return DATA_STORE;
      } else if(vcObject instanceof VcHost) {
         return HOST;
      }  else if(vcObject instanceof VcResourcePool) {
         return RESOURCE_POOL;
      }

      return null;
   }
}