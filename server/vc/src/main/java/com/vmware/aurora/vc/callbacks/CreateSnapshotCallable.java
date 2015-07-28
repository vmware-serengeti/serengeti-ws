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

import com.vmware.aurora.vc.VcObject;
import com.vmware.aurora.vc.VcTask;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

/**
 * Created By xiaoliangl on 6/12/15.
 */
public abstract class CreateSnapshotCallable extends VcTaskCallable {
   private String name;
   private String desc;

   public CreateSnapshotCallable(String snapshotName, String description) throws Exception {
      super(VcTask.TaskType.Snapshot, null);

      name = snapshotName;
      desc = description;
   }

   @Override
   public ManagedObjectReference callVc() throws Exception {
      VirtualMachine vm = getVm().getManagedObject();
      return vm.createSnapshot(name, desc, false, false);
   }

   public VcObject getParent() {
      return getVm();
   }

   public abstract VcVirtualMachine getVm();
}
