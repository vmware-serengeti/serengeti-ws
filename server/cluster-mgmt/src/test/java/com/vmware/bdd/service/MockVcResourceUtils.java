package com.vmware.bdd.service;

import org.mockito.Mockito;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.service.utils.VcResourceUtils;

@MockClass(realClass = VcResourceUtils.class)
public class MockVcResourceUtils {
   private static boolean flag = true;
   @Mock
   public static VcResourcePool findRPInVCCluster(final String clusterName,
         final String vcRPName) {
      if (flag) {
         return Mockito.mock(VcResourcePool.class);
      } else {
         return null;
      }
   }

   @Mock
   public static VcDatastore findDSInVcByName(String dsName) {
      if (flag) {
         return Mockito.mock(VcDatastore.class);
      } else {
         return null;
      }
   }

   public static void setFlag(boolean flag) {
      MockVcResourceUtils.flag = flag;
   }

   @Mock
   public static VcHost findHost(final String hostName) { 
      return null;
   }
}
