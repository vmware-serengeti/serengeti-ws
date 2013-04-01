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

import java.util.concurrent.Callable;

import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;

/**
 * Store Procedure for import a vm
 */

public class ImportVmSP implements Callable<Void> {
   final VcVirtualMachine vcVm;

   public ImportVmSP(VcResourcePool rp, String vmName) {
      for (VcVirtualMachine vm : rp.getChildVMs()) {
         if (vm.getName().equals(vmName)) {
            this.vcVm = vm;
            return;
         }
      }
      this.vcVm = null;
   }

   @Override
   public Void call() throws Exception {
      return null;
   }

   public VcVirtualMachine getResult() {
      return vcVm;
   }
}
