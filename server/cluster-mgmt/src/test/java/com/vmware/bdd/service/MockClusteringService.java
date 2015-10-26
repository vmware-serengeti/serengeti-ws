package com.vmware.bdd.service;

import java.util.List;

import org.mockito.Mockito;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.impl.ClusteringService;

@MockClass(realClass = ClusteringService.class)
public class MockClusteringService {

   private VcVirtualMachine getMockTemplateVM() {
      // mock a VcVm
      VcVirtualMachine vm = Mockito.mock(VcVirtualMachine.class);
      Mockito.when(vm.getName()).thenReturn("template-vm");
      Mockito.when(vm.getDatacenter()).thenReturn(
            Mockito.mock(VcDatacenter.class));
      return vm;
   }


   @Mock
   public VcVirtualMachine getTemplateVM(String templateName) {
      return getMockTemplateVM();
   }

   @Mock
   public VcVirtualMachine prepareTemplateVM(String templateName) {
      return getMockTemplateVM();
   }

   @Mock
   public VcVirtualMachine prepareNodeTemplate(String templateName, List<BaseNode> vNodes) {
      return getMockTemplateVM();
   }

}
