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

package com.vmware.aurora.vc;

import com.vmware.aurora.composition.ImportVmSP;
import com.vmware.aurora.vc.TestSP.CleanUpSP;
import com.vmware.aurora.vc.TestSP.CloneVmSP;
import com.vmware.aurora.vc.TestSP.RemoveSnapshotSP;
import com.vmware.aurora.vc.TestSP.TakeSnapshotSP;
import com.vmware.aurora.vc.VcVirtualMachine.DiskCreateSpec;

/**
 * Test utility functions.
 *
 */
public class TestUtil {

   TestSP testSP;

   public TestUtil() {
      this.testSP = new TestSP();
   }

   /**
    * Test method for importing target vm.
    *
    * @param vmName the name of the import vm
    * @param rp the resource pool the import vm is in
    * @return the import vm store procedure
    */
   public ImportVmSP testImportVM(String vmName, VcResourcePool rp)
         throws Exception {

      ImportVmSP sp = new ImportVmSP(rp, vmName);
      sp.call();
      return sp;
   }


   /**
    * Test method for taking snapshot
    *
    * @param id The id of the VM
    * @param description The description of the snapshot
    * @return : the take snapshot store procedure
    */
   public TakeSnapshotSP testTakeSnapshot(String vmId, String name, String description)
         throws Exception {
      TakeSnapshotSP sp = testSP.new TakeSnapshotSP(vmId, name, description);
      sp.call();
      return sp;
   }

   /**
    * Test method for removing snapshot
    *
    * @param id The id of the TmObject
    * @return : the take snapshot store procedure
    */
   public RemoveSnapshotSP testRemoveSnapshot(String vmId, String snapName) throws Exception {
      RemoveSnapshotSP sp = testSP.new RemoveSnapshotSP(vmId, snapName);
      sp.call();
      return sp;
   }

   /**
    * Test method for cloning a vm
    *
    * @param newVmName new name for the cloned vm
    * @param srcVmId id of the vm that we are cloning from
    * @param snapName name of the snapshot we are cloning from
    * @param targetRp the target resource pool
    * @param targetDs the target datastore
    * @param removeDisks disks that we want to remove during the clone
    *        procedure
    * @param addDisks disks that we want to add during the clone procedure
    * @return the clone store procedure
    */
   public CloneVmSP testCloneVm(String newVmName, String srcVmId,
         String snapName, VcResourcePool targetRp, VcDatastore targetDs,
         DeviceId[] removeDisks, DiskCreateSpec[] addDisks)
         throws Exception {
      CloneVmSP sp =
            testSP.new CloneVmSP(newVmName, srcVmId, snapName, targetRp,
                  targetDs, removeDisks, addDisks);
      sp.call();
      return sp;
   }

   /**
    * Test method for deleting a vm
    *
    * @param vmId id of the vm that is going to be deleted
    * @return the cleanup store procedure
    */

   public CleanUpSP testCleanupVm(String vmId) throws Exception {
      CleanUpSP sp = testSP.new CleanUpSP(vmId);
      sp.call();
      return sp;
   }
}
