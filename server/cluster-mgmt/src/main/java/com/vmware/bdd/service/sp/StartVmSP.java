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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

/**
 * Store Procedure for start a vm
 */

public class StartVmSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(StartVmSP.class);
   private String vmId;
   private final IPrePostPowerOn postPowerOn;
   private final Map<String, String> bootupConfigs;
   private VcHost host;

   public StartVmSP(VcVirtualMachine vcVm, IPrePostPowerOn postPowerOn,
         Map<String, String> bootupConfigs, VcHost host) {
      this.vmId = vcVm.getId();
      this.postPowerOn = postPowerOn;
      this.bootupConfigs = bootupConfigs;
      this.host = host;
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
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            // set the bootup configs
            Map<String, String> guestConfigs;
            if (bootupConfigs == null) {
               guestConfigs = new HashMap<String, String>();
            } else {
               guestConfigs = bootupConfigs;
            }
            VcVmUtil.addBootupUUID(guestConfigs);
            vcVm.setGuestConfigs(guestConfigs);
            vcVm.powerOn(host);
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
      executePostPowerOn(vcVm);
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
}
