package com.vmware.bdd.service;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcVirtualMachine;

@MockClass(realClass = VcCache.class)
public class MockVcCache {

   private static boolean getFlag = false;
   @Mock
   static public <T extends VcObject> T getIgnoreMissing(String id) {
      if (getFlag) {
         return (T) Mockito.mock(VcVirtualMachine.class);
      } else {
         return null;
      }
   }
   public static void setGetFlag(boolean flag) {
      getFlag = flag;
   }
}
