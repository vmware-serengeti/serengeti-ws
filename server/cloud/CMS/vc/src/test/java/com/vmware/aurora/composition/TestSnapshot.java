/* Copyright (c) 2012 VMware, Inc.  All rights reserved. */

package com.vmware.aurora.composition;

import org.testng.annotations.Test;

import com.vmware.aurora.vc.VcSnapshot;
import com.vmware.aurora.vc.VcVirtualMachine;

public class TestSnapshot extends AbstractTmTest {
   private VcSnapshot snapshot;

   @Test
   public void takeSnapshot() throws Exception {
      VcVirtualMachine vm = util.testImportVM(vmName, rp).getResult();
      CreateSnapshotSP sp = new CreateSnapshotSP(vm, "name", "TestSnpshot");
      sp.call();
      snapshot = sp.getResult();
   }

   @Test(dependsOnMethods = {"takeSnapshot"})
   public void revertSnapshot() throws Exception {
      Thread.sleep(1000L * 60 * 10); // Power on the VM, make some changes, and power off
      RestoreVmSp sp = new RestoreVmSp(snapshot);
      sp.call();
      Thread.sleep(1000L * 60 * 10); // Power on VM again, check the changes, the changes should be gone
   }
}