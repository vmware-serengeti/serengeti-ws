package com.vmware.bdd.service;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.utils.VcVmUtil;

@MockClass(realClass = VcVmUtil.class)
public class MockVcVmUtil {
   private static boolean flag = true;
   private static int i = 0;

   @Mock
   public static String getIpAddress(final VcVirtualMachine vcVm,
         boolean inSession) {
      if (flag) {
         i++;
         return "10.1.1." + i;
      } else {
         return null;
      }
   }

   @Mock
   public static String getGuestHostName(final VcVirtualMachine vcVm,
         boolean inSession) {
      return "localhost";
   }

   public static void setFlag(boolean flag) {
      MockVcVmUtil.flag = flag;
   }

   @Mock
   public static boolean setBaseNodeForVm(BaseNode vNode, VcVirtualMachine vm) {
      return true;
   }

   @Mock
   public static boolean isDatastoreAccessible(String dsMobId) {
      // e.g., datastore:1000 return false
      if (dsMobId.endsWith("0"))
         return false;

      return true;
   }
   
   @Mock
   public static void updateVm(String vmId) {
	   
   }
}
