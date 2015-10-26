package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;

import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.service.resmgmt.impl.NodeTemplateService;
import com.vmware.bdd.service.utils.VcResourceUtils;

import mockit.Mock;
import mockit.MockClass;


@MockClass(realClass = VcResourceUtils.class)
public class MockVcResourceUtils {

   private static VcVirtualMachine vm;

   static {
      vm = Mockito.mock(VcVirtualMachine.class);
      Mockito.when(vm.getName()).thenReturn("node-template-1");
      Mockito.when(vm.getId()).thenReturn("vm-01");
   }

   @Mock
   public static List<VcVirtualMachine> findAllNodeTemplates() {
      List<VcVirtualMachine> vms = new ArrayList<VcVirtualMachine>();
      vms.add(vm);
      return vms;
   }

   @Mock
   public static VcVirtualMachine findVM(final String moid) {
      if (moid.equalsIgnoreCase("vm-01"))
         return vm;
      else
         return null;
   }
}
