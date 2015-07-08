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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import com.vmware.aurora.composition.ImportVmSP;
import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.vc.VcVirtualMachine.DiskCreateSpec;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.clone.spec.VmCreateSpec;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

/**
 * @author shuang
 */
public class CloneTest extends AbstractTmTest {
   static final Logger logger = Logger.getLogger(CloneTest.class);

   /**
    * Test case :
    * <p/>
    * 1.Import the test vm.
    * <p/>
    * 2.Take a snapshot for the imported vm -- snap0.
    * <p/>
    * 3.Do two linked clones of snap0. One is "ClonedVM1",the other one is
    * "ClonedVM2".
    * <p/>
    * 4.The imported vm has two hard disks -- SCSI(0:0)Hard disk1 and
    * SCSI(0:1)Hard disk2.
    * <p/>
    * During the clone procedure of "ClonedVM1",I removed the SCSI(0:1)Hard
    * disk2 and added a new disk -- SCSI(0:2)Hard disk2.
    * <p/>
    * During the clone procedure of "ClonedVM2",I removed the SCSI(0:1)Hard
    * disk2 and added a new disk -- SCSI(0:3)Hard disk2.
    * <p/>
    * 5.Take a snapshot of "ClonedVM1" -- snap1.
    * <p/>
    * 6.Do a linked clone of snap1 and call the cloned vm "ClonedVM3".
    * "ClonedVM3" should have the same disk layout as "ClonedVM1".
    */
   @Test
   public void testTransaction() throws Exception {
      DeviceId slot1 = new DeviceId("VirtualLsiLogicController", 0, 1);
      DeviceId slot2 = new DeviceId("VirtualLsiLogicController", 0, 2);
      DeviceId slot3 = new DeviceId("VirtualLsiLogicController", 0, 3);

      DeviceId[] removeDisks = {slot1};

      DiskCreateSpec[] addDisks =
            {new DiskCreateSpec(slot2, ds, "data",
                  DiskMode.persistent, DiskSize.sizeFromGB(10))};

      DiskCreateSpec[] addDisks1 =
            {new DiskCreateSpec(slot3, ds, "data",
                  DiskMode.persistent, DiskSize.sizeFromGB(20))};


      //Import vm -- "PlatformTestVM" as the target test vm.
      ImportVmSP sp0 = new TestUtil().testImportVM(vmName, rp);

      VcVirtualMachineImpl vcVirtualMachine = (VcVirtualMachineImpl)sp0.getResult();

      logger.info("import VM: " + vcVirtualMachine);

      VirtualDevice[] devices = vcVirtualMachine.getDevice();
      for(VirtualDevice device : devices) {
         logger.info(device);
      }

      //Take a snapshot for test vm -- snap0
      final String snapshotName = "snap";
      TestSP.TakeSnapshotSP sp1 =
            new TestUtil().testTakeSnapshot(sp0.getResult().getId(), snapshotName, "snapshot of PlatformTestVM");

      //Clone from imported vm's snapshot -- "snap0".
      String newVmName1 = "clonedVM1";
      TestSP.CloneVmSP sp2 =
            new TestUtil().testCloneVm(newVmName1, sp0.getResult().getId(),
                  snapshotName, rp, ds, null, addDisks);
      logger.info("Cloned VM: " + sp2.getResult());

      //Clone from "clonedVM1",first take a snapshot,then clone.
      String newVmName2 = "clonedVM2";
      TestSP.CloneVmSP sp3 =
            new TestUtil().testCloneVm(newVmName2, sp0.getResult().getId(),
                  snapshotName, rp, ds, null, addDisks1);
      logger.info("Cloned VM: " + sp3.getResult());

      //Take a snapshot for "clonedVM1" -- snap1
      TestSP.TakeSnapshotSP sp4 =
            new TestUtil().testTakeSnapshot(sp2.getResult(), snapshotName, "snapshot of clonedVM1");

      //Do a lined clone of "clonedVM1" using snapshot snap1.
      String newVmName3 = "clonedVM3";
      final TestSP.CloneVmSP sp5 =
            new TestUtil().testCloneVm(newVmName3, sp2.getResult(),
                  snapshotName, rp, ds, null, null);
      logger.info("Cloned VM: " + sp5.getResult());

      VcContext.inVcSessionDo(new VcSession<Object>() {
         @Override
         protected boolean isTaskSession() {
            return true;
         }
         @Override
         protected Object body() throws Exception {
            return sp5.getVM().powerOn();
         }
      });

      System.in.read();
      /*
      //Mark "ClonedVM1" for delete,but it won't be deleted until clonedVM3 is deleted.
      new TestUtil().testCleanupVm(sp2.getResult());
      logger.info("Deleted VM: " + sp2.getResult());

      //Mark "ClonedVM3" for delete,it should be deleted immediately.
      new TestUtil().testCleanupVm(sp5.getResult());
      logger.info("Deleted VM: " + sp5.getResult());

      new TestUtil().testCleanupVm(sp3.getResult());
      logger.info("Deleted VM: " + sp3.getResult());
      */
   }


   VcVirtualMachine forkParentVm = null;
   VcVirtualMachine forkChildVm = null;
   @Test
   public void testVMFork() throws Exception {

      //Import vm -- "PlatformTestVM" as the target test vm.
      final ImportVmSP sp0 = new TestUtil().testImportVM(vmName, rp);

      //Take a snapshot for test vm -- snap0
      final String snapshotName = "snap";
      TestSP.TakeSnapshotSP sp1 =
            new TestUtil().testTakeSnapshot(sp0.getResult().getId(), snapshotName, "snapshot of PlatformTestVM");


      VmCreateSpec vmCreateSpec = new VmCreateSpec();
      vmCreateSpec.setTargetRp(rp);
      vmCreateSpec.setTargetDs(ds);
      vmCreateSpec.setCloneType(VcVmCloneType.FULL);
      vmCreateSpec.setPersisted(true);
      vmCreateSpec.setVmName("forkParentVm");


      final VcVirtualMachine.CreateSpec vcVmCreateSpec =
            vmCreateSpec.toCreateSpec(sp0.getResult().getSnapshotByName("snap"),
                  new ConfigSpecImpl());

      try {
         forkParentVm = VcContext.inVcSessionDo(new VcSession<VcVirtualMachine>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected VcVirtualMachine body() throws Exception {
               forkParentVm = sp0.getResult().cloneVm(vcVmCreateSpec, null);
               forkParentVm.enableForkParent();

               Map<String, String> bootupConfigs = new HashMap<String, String>();
               bootupConfigs.put("vmfork", "yes");
               forkParentVm.setGuestConfigs(bootupConfigs);

               forkParentVm.powerOn();

               int i = 0;
               while(i < 120){
                  boolean flag = forkParentVm.isQuiescedForkParent();
                  System.out.println("Vm Quiesced: " + flag);
                  if(flag) {
                     break;
                  }

                  Thread.sleep(2000);
               }
               return forkParentVm;
            }
         });


         VmCreateSpec forkCreateSpec = new VmCreateSpec();
         forkCreateSpec.setTargetRp(rp);
         forkCreateSpec.setTargetDs(ds);
         forkCreateSpec.setCloneType(VcVmCloneType.VMFORK);
         forkCreateSpec.setPersisted(false);
         forkCreateSpec.setVmName("fork01");

         final VcVirtualMachine.CreateSpec vcForkCreateSpec =
               forkCreateSpec.toCreateSpec(null, new ConfigSpecImpl());


          forkChildVm = VcContext.inVcSessionDo(new VcSession<VcVirtualMachine>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected VcVirtualMachine body() throws Exception {
               forkChildVm = forkParentVm.cloneVm(vcForkCreateSpec, null);
               forkChildVm.powerOn();
               return forkChildVm;
            }
         });

      } finally {

         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               if(forkChildVm != null) {
                  forkChildVm.powerOff();
                  forkChildVm.destroy(false);
               }

               if (forkParentVm != null) {
                  forkParentVm.powerOff();
                  forkParentVm.destroy();
               }
               sp0.getResult().removeAllSnapshots();
               return null;
            }
         });
      }



   }
}
