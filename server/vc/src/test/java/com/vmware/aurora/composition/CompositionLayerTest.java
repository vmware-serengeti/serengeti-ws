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

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import com.vmware.aurora.composition.TestSP.TakeSnapshotSP;
import com.vmware.aurora.vc.VcVmCloneType;
import com.vmware.vim.binding.vim.Folder;

/**
 * @author sridharr
 *
 */
public class CompositionLayerTest extends AbstractTmTest {
   static final Logger logger = Logger.getLogger(CompositionLayerTest.class);
   private Folder vmFolder;

   @Test
   public void createVMFolder() throws Exception {
      CreateVMFolderSP sp = new CreateVMFolderSP(dc, null, Arrays.asList(VcTestConfig.testVmFolderName));
      sp.call();
      vmFolder = sp.getResult().get(0);
   }

   /**
    * Test case :
    *
    * 1.Import the template vm.
    *
    * 2.Attach the diskSchema to it
    *
    * 3.Specify diskSchema for new VM. It specifies a new disk for it.
    *
    * 4.The imported template vm has three hard disks -- SCSI(0:0)Hard disk1,
    * SCSI(0:1)Hard disk2, and SCSI(0:2)Hard disk3. Disk3 is marked as FINAL in
    * the template diskSchema
    *
    * 5. Call the CreateVmSP. Newly created VM is cloned from the template, has
    * two disks from it, and has its own newly added disk
    *
    */
   @Test (dependsOnMethods = {"createVMFolder"})
   public void testTransaction() throws Exception {
      VmSchema templateVmSchema =
            SchemaUtil.getSchema(new File(
                  "./src/test/resources/templateVmSchema.xml"), VmSchema.class);
      //Import vm -- as the template vm.
      ImportVmSP vm0SP = util.testImportVM(vmName, rp);

      String templateId = vm0SP.getResult().getId();

      final String snapshotName = "snapshotname";
      TakeSnapshotSP vm0SnapSP =
            util.testTakeSnapshot(vm0SP.getResult().getId(), snapshotName, "template Snapshot");

      VmSchema vmSchema =
            SchemaUtil.getSchema(new File("./src/test/resources/vmSchema.xml"),
                  VmSchema.class);

      logger.info("Template vm schema: " + templateVmSchema);
      logger.info("vm schema: " + vmSchema);

      SchemaUtil.putXml(vmSchema, new File("vmSchema.xml"));
      SchemaUtil.putXml(templateVmSchema, new File("templateVmSchema.xml"));

      // Update the template information in the new VM diskSchema
      // XXX : TODO need to figure out the best way to represent this
      // By name or by TmObjectId. find in tx layer is by objectId
      vmSchema.diskSchema.setParent(templateId.toString());
      vmSchema.diskSchema.setParentSnap(snapshotName);

      CreateVmSP vm1SP =
            new CreateVmSP("testCompLayer", vmSchema, rp, ds, null, new QueryGuestInfo(3 * 60),
                  null, VcVmCloneType.LINKED, vmFolder);
      vm1SP.call();
      logger.info("Created VM: " + vm1SP.getVM().getName());

      TakeSnapshotSP vm1SnapSP =
            util.testTakeSnapshot(vm1SP.getVM().getId(), snapshotName, "snap0");

      VmSchema vmCloneSchema =
            SchemaUtil.getSchema(new File(
                  "./src/test/resources/vmCloneSchema.xml"), VmSchema.class);
      vmCloneSchema.diskSchema.setParent(vm1SP.getVM().getId());
      vmCloneSchema.diskSchema.setParentSnap(snapshotName);

      CreateVmSP vm2SP =
            new CreateVmSP("testCloneCompLayer", vmCloneSchema, rp, ds, null,
                  null, null, VcVmCloneType.FULL, vmFolder);
      vm2SP.call();

      VmSchema vmFullCloneSchema =
            SchemaUtil
                  .getSchema(new File(
                        "./src/test/resources/vmFullCloneSchema.xml"),
                        VmSchema.class);
      vmFullCloneSchema.diskSchema.setParent(vm1SP.getVM().getId());
      vmFullCloneSchema.diskSchema.setParentSnap(snapshotName);

      CreateVmSP fullCloneVmSP =
            new CreateVmSP("testFullCloneCompLayer", vmFullCloneSchema, rp, ds,
                  null, null, null, VcVmCloneType.LINKED, vmFolder);
      fullCloneVmSP.call();

      util.testRemoveSnapshot(templateId, snapshotName);

      util.testCleanupVm(vm1SP.getVM().getId());
      logger.info("Deleted VM: " + vm1SP.getVM().getName());

      util.testCleanupVm(vm2SP.getVM().getId());
      logger.info("Deleted VM: " + vm2SP.getVM().getName());

      util.testCleanupVm(fullCloneVmSP.getVM().getId());
      logger.info("Deleted VM: " + fullCloneVmSP.getVM().getName());
   }
}
