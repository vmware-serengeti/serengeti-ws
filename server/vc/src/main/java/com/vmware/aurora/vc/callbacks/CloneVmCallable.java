/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.aurora.vc.callbacks;

import com.vmware.aurora.vc.VcTask;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Created By xiaoliangl on 6/12/15.
 */
public abstract class CloneVmCallable extends VcTaskCallable {
   public CloneVmCallable() throws Exception {
      super(VcTask.TaskType.CloneVm, null);
   }

   @Override
   public ManagedObjectReference callVc() throws Exception {
      VcVirtualMachine src = getSrcVm();

      /*VcCache.refresh(virtualMachine.getMoRef());
      VcCache.sync(virtualMachine.getMoRef());
      VcSnapshot snap = virtualMachine.getSnapshotByName("snapshot");

      VcVirtualMachine.CreateSpec vmSpec =
            new VcVirtualMachine.CreateSpec("ClonedVm", snap, rp, ds, VcVmCloneType.LINKED,
                  true,
                  null);*/

      return src.getCloneAsyncCallable(getCloneSpec(), null).callVc();
   }

   public abstract VcVirtualMachine getSrcVm();

   public abstract VcVirtualMachine.CreateSpec getCloneSpec();

}
