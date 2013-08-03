/* Copyright (c) 2012 VMware, Inc.  All rights reserved. */

package com.vmware.aurora.composition;

import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcVirtualMachine;

public class TestDeleteVmSP extends AbstractTmTest {

   @Test
   public void testDeleteVm() throws Exception {
      String vmName = "clonedVM1";
      // Need to import the VM first, since TM layer doesn't persist data.
      VcVirtualMachine vm = util.testImportVM(vmName, rp).getResult();

      DeleteVmSP sp = new DeleteVmSP(vm);
      sp.call();
   }
}
