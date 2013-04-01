/************************************************************
 * Copyright (c) 2011 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc.test;

import org.testng.annotations.Test;

import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority;

public class VmHaTest extends AbstractVcTest {

   @Test
   public void changeVmHaSettingTest() throws Exception {
      String vmId = properties.getProperty("testVmReference");
      VcVirtualMachine vm = VcCache.get(vmId);
      AuAssert.check(vm != null);

      vm.modifyHASettings(RestartPriority.disabled, null, null);
   }
}
