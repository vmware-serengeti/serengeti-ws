/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.util.Map;
import java.util.concurrent.Callable;

import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.VcVmUtil;

/**
 * Store Procedure for start a vm
 */

public class StartVmSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(StartVmSP.class);
   private String vmId;
   private final IPrePostPowerOn prePowerOn;
   private final IPrePostPowerOn postPowerOn;
   private VcHost host;
   private String vmName;

   public StartVmSP(VcVirtualMachine vcVm, IPrePostPowerOn prePowerOn,
         IPrePostPowerOn postPowerOn, VcHost host) {
      this.vmId = vcVm.getId();
      this.prePowerOn = prePowerOn;
      this.postPowerOn = postPowerOn;
      this.host = host;
      this.vmName = vcVm.getName();
   }

   public static class StartVmPrePowerOn implements IPrePostPowerOn {
      private VcVirtualMachine vm;
      private boolean reserveRawDisks;
      private String volumes;

      public StartVmPrePowerOn(boolean reserveRawDisks, String volumes) {
         this.reserveRawDisks = reserveRawDisks;
         this.volumes = volumes;
      }

      @Override
      public void setVm(VcVirtualMachine vm) {
         this.vm = vm;
      }

      @Override
      public VcVirtualMachine getVm() {
         return vm;
      }

      @Override
      public Void call() throws Exception {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected Void body() throws Exception {
               Map<String, String> bootupConfigs = vm.getGuestConfigs();
               AuAssert.check(bootupConfigs != null);
               /* serengeti operation flag - a random generated uuid
               * a script inside the vm compares this uuid with its stored value, if they are
               * different, this VM decides it's started by Serengeti, otherwise, it's started
               * by third parties.
               */
               VcVmUtil.addBootupUUID(bootupConfigs);
               bootupConfigs.put(Constants.GUEST_VARIABLE_RESERVE_RAW_DISKS, String.valueOf(reserveRawDisks));
               bootupConfigs.put(Constants.GUEST_VARIABLE_VOLUMES, volumes);
               vm.setGuestConfigs(bootupConfigs);
               return null;
            }

            protected boolean isTaskSession() {
               return true;
            }
         });
         return null;
      }
   }

   @Override
   public Void call() throws Exception {
      executeOnce();
      return null;
   }

   private void executeOnce() throws Exception {
      AuAssert.check(vmId != null);
      final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
      if (vcVm == null) {
         logger.info("vm: " + vmId + " is not found.");
         return;
      }
      if (vcVm.isPoweredOn()) {
         logger.info("vm " + vcVm.getName() + " is already powered on.");
         executePostPowerOn(vcVm);
         return;
      }

      // scale up/down its memory will replace its swap disk, in this case, we still
      // need to add volumes info into machine id to re-mount this new swap disk.
      executePrePowerOn(vcVm);
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            vcVm.powerOn(host);
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
      executePostPowerOn(vcVm);
   }

   private void executePrePowerOn(VcVirtualMachine vm) throws Exception {
      if (prePowerOn != null) {
         prePowerOn.setVm(vm);
         prePowerOn.call();
      }
   }

   private void executePostPowerOn(VcVirtualMachine vm) throws Exception {
      if (postPowerOn != null) {
         postPowerOn.setVm(vm);
         postPowerOn.call();
      }
   }

   public VcVirtualMachine getVcVm() {
      return VcCache.getIgnoreMissing(vmId);
   }

   public String getVmName() {
      return vmName;
   }
}
