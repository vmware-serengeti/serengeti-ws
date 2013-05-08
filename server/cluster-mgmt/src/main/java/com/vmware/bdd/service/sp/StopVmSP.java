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
package com.vmware.bdd.service.sp;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.Constants;

/**
 * Store Procedure for stop a vm
 */

public class StopVmSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(StopVmSP.class);
   private final String vmId;
   private final boolean vmPoweroff;

   public StopVmSP(VcVirtualMachine vcVm) {
      this.vmId = vcVm.getId();
      this.vmPoweroff = false;
   }

   public StopVmSP(VcVirtualMachine vcVm, boolean vmShutdown) {
      this.vmId = vcVm.getId();
      this.vmPoweroff = vmShutdown;
   }

   @Override
   public Void call() throws Exception {
      final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
      if (vcVm == null) {
         logger.info("vm " + vmId
               + " is deleted from vc. Ignore the power off request.");
      }
      if (vcVm.isPoweredOff()) {
         logger.info("vm " + vcVm.getName() + " is already powered off.");
         return null;
      }
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            if (vmPoweroff) {
               vcVm.powerOff();
            } else {
               vcVm.shutdownGuest(Constants.VM_SHUTDOWN_WAITING_SEC * 1000);
            }
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
      return null;
   }

   public VcVirtualMachine getVcVm() {
      return VcCache.getIgnoreMissing(vmId);
   }
}
