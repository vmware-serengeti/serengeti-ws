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
import com.vmware.aurora.vc.VcUtil;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vim.VirtualMachine.ConnectionState;
import com.vmware.vim.binding.vim.vm.FaultToleranceConfigInfo;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

/**
 * Stored Procedure to delete a VM
 * 
 * @author Xin Li (xinli)
 * 
 */

public class DeleteVmByIdSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(DeleteVmByIdSP.class);
   private String vmId;

   public DeleteVmByIdSP(String vmId) {
      this.vmId = vmId;
   }

   @Override
   public Void call() throws Exception {
      final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
      if (vcVm == null) {
         logger.info("vm " + vmId + " is not found in VC, ignore this delete.");
         return null;
      }

      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            try {
               FaultToleranceConfigInfo info = vcVm.getConfig().getFtInfo();
               if (info != null && info.getRole() == 1) {
                  logger.info("VM " + vcVm.getName()
                        + " is FT primary VM, disable FT before delete it.");
                  vcVm.turnOffFT();
               }
               if (vcVm.isPoweredOn()) {
                  vcVm.powerOff();
               }
               vcVm.destroy();
               return null;
            } catch (ManagedObjectNotFound e) {
               VcUtil.processNotFoundException(e, vmId, logger);
               return null;
            } catch (Exception e) {
               //if vm is in inaccessible state, unregister VM directly
               if (vcVm.getConnectionState() == ConnectionState.inaccessible) {
                  logger.error("Failed to delete VM " + vcVm.getName(), e);
                  logger.info("Unregister VM." + vcVm.getName());
                  vcVm.unregister();
                  return null;
               } else {
                  throw e;
               }
            }
         }

         protected boolean isTaskSession() {
            return true;
         }
      });

      return null;
   }
}
