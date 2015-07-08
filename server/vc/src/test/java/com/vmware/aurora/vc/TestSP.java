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

import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcVirtualMachine.CreateSpec;
import com.vmware.aurora.vc.VcVirtualMachine.DiskCreateSpec;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

/**
 * Class for all the store procedures used in junit test.
 */
public class TestSP {

   /**
    * Import a vm,clone a vm,delete a vm,changed the disk of a vm.
    */
   class ChangeDiskSP implements Callable<Void> {
      final String vmId;
      final DeviceId[] removeDisks;
      final DiskCreateSpec[] addDisks;

      public ChangeDiskSP(String vmId, DeviceId[] removeDisks,
            DiskCreateSpec[] addDisks) {
         this.vmId = vmId;
         this.removeDisks = removeDisks;
         this.addDisks = addDisks;
      }

      @Override
      public Void call() throws Exception {
         VcVirtualMachine vm = VcCache.get(vmId);
         if (!(removeDisks == null && addDisks == null)) {
            vm.changeDisks(removeDisks, addDisks);
         }
         return null;
      }
   }

   /**
    * Copy vm store procedure,override the run()method. This procedure doesn't
    * remove any disks.
    */
   class CloneVmSP implements Callable<Void> {
      final String newVmName;
      final String srcVmId;
      final String snapName;
      final VcResourcePool targetRp;
      final VcDatastore targetDs;
      final DeviceId[] removeDisks;
      final DiskCreateSpec[] addDisks;

      transient VcVirtualMachine vcVm = null;
      transient String dstVmId;

      public CloneVmSP(String newVmName, String srcVmId, String snapName,
            VcResourcePool targetRp, VcDatastore targetDs,
            DeviceId[] removeDisks, DiskCreateSpec[] addDisks) {
         this.newVmName = newVmName;
         this.srcVmId = srcVmId;
         this.snapName = snapName;
         this.targetRp = targetRp;
         this.targetDs = targetDs;
         this.removeDisks = removeDisks;
         this.addDisks = addDisks;
      }

      @Override
      public Void call() throws Exception {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               VcVirtualMachine template = VcCache.get(srcVmId);
               VcSnapshot snap = template.getSnapshotByName(snapName);

               CreateSpec vmSpec =
                     new CreateSpec(newVmName, snap, targetRp, targetDs, VcVmCloneType.LINKED,
                           true,
                           null);

               DeviceId[] removeSet = removeDisks;
               VcVirtualMachine newVm = template.cloneVm(vmSpec, removeSet);
               newVm.changeDisks(null, addDisks);
               //newVm.powerOn();

               // retrieve the resulting VcObject instance
               dstVmId = newVm.getId();
               vcVm = newVm;
               return null;
            }
         });
         return null;
      }

      public String getResult() {
         return dstVmId;
      }

      public VcVirtualMachine getVM() {
         return this.vcVm;
      }
   }

   /**
    * Store procedure for taking snap shot of vm.
    */
   class TakeSnapshotSP implements Callable<Void> {
      final String vmId;
      final String name;
      final String description;
      transient long snapId;

      public TakeSnapshotSP(String vmId, String name, String description) {
         this.vmId = vmId;
         this.name = name;
         this.description = description;
      }

      @Override
      public Void call() throws Exception {
         final VcVirtualMachine vm = VcCache.get(vmId);

         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               vm.createSnapshot(name, description);
               return null;
            }
         });
         return null;
      }
   }

   /**
    * Store procedure for removing snap shot of vm.
    */
   class RemoveSnapshotSP implements Callable<Void> {
      final String vmId;
      final String snapName;

      public RemoveSnapshotSP(String vmId, String snapName) {
         this.vmId = vmId;
         this.snapName = snapName;
      }

      @Override
      public Void call() throws Exception {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               VcVirtualMachine vm = VcCache.get(vmId);
               VcSnapshot snap = vm.getSnapshotByName(snapName);
               snap.remove();
               return null;
            }
         });
         return null;
      }
   }

   /**
    * Store procedure for cleaning up.
    */
   class CleanUpSP implements Callable<Void> {
      final String vmId;

      public CleanUpSP(String vmId) {
         this.vmId = vmId;
      }

      @Override
      public Void call() throws Exception {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               VcVirtualMachine vm = VcCache.get(vmId);
               vm.destroy();
               return null;
            }
         });
         return null;
      }
   }
}
