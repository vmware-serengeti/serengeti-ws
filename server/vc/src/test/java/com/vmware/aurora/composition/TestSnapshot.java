/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

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