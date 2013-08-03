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

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Stored Procedure to set machine id.
 *
 * @author Xin Li (xinli)
 *
 */

import com.vmware.aurora.vc.VcVirtualMachine;

public class SetVmMachineIdSP implements Callable<Void> {
   private VcVirtualMachine vm;
   private Map<String, String> machineIds;
   private boolean overwrite;

   public SetVmMachineIdSP(VcVirtualMachine vm, Map<String, String> machineIds, boolean overwrite) {
      this.vm = vm;
      this.machineIds = machineIds;
      this.overwrite = overwrite;
   }

   @Override
   public Void call() throws Exception {
      if (overwrite) {
         vm.setGuestConfigs(machineIds);
      } else {
         Map<String, String> current = vm.getGuestConfigs();
         current.putAll(machineIds);
         vm.setGuestConfigs(current);
      }
      return null;
   }
}
