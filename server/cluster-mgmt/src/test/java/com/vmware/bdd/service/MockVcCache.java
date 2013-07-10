package com.vmware.bdd.service;

import mockit.Mock;
import mockit.MockClass;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;

@MockClass(realClass = VcCache.class)
public class MockVcCache {

   private static boolean getFlag = false;
   @Mock
   static public <T extends VcObject> T getIgnoreMissing(String id) {
      if (getFlag) {
         if (id.equals("create-vm-succ")) {
            VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
            VcHost host = Mockito.mock(VcHost.class);
            Mockito.when(vm.getHost()).thenReturn(host);
            Mockito.when(host.getName()).thenReturn("host1.eng.vmware.com");
            VcResourcePool rp = Mockito.mock(VcResourcePool.class);
            Mockito.when(vm.getResourcePool()).thenReturn(rp);
            VcCluster cluster = Mockito.mock(VcCluster.class);
            Mockito.when(rp.getVcCluster()).thenReturn(cluster);
            Mockito.when(cluster.getName()).thenReturn("cluster-ws");
            return (T)vm;
         }
         return (T) Mockito.mock(VcVirtualMachine.class);
      } else {
         return null;
      }
   }
   public static void setGetFlag(boolean flag) {
      getFlag = flag;
   }

}
