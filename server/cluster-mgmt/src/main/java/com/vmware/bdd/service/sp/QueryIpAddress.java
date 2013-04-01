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

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.vim.binding.vim.vm.GuestInfo;
import com.vmware.vim.binding.vim.vm.GuestInfo.ToolsRunningStatus;

public class QueryIpAddress implements IPrePostPowerOn {
   private static final Logger logger = Logger.getLogger(QueryIpAddress.class);
   private static long checkPeriod = 10 * 1000; // 10 seconds

   private String vmId;
   private VcVirtualMachine vm;
   private GuestInfo guestInfo;
   private long timeout;

   /**
    * After a VM is powered on, wait for the guest information to be
    * available at most for <tt>timeout</tt> in seconds, and retrieve the
    * guest information.
    * @param timeout
    */
   public QueryIpAddress(int timeoutInSeconds) {
      this.timeout = timeoutInSeconds * 1000L;
   }

   public String getIpAddress() {
      return guestInfo.getIpAddress();
   }

   @Override
   public Void call() throws Exception {
      long start = System.currentTimeMillis();
      while (System.currentTimeMillis() - start < timeout) {
         boolean stop = VcContext.inVcSessionDo(new VcSession<Boolean>() {
            @Override
            protected Boolean body() throws Exception {
               VcVirtualMachine vm = VcCache.getIgnoreMissing(vmId);
               if (vm != null && vm.isPoweredOn()) {
                  guestInfo = vm.queryGuest();
                  return false;
               } else {
                  // stop waiting, since vm is not found
                  logger.info("vm is not found or is powered off in VC, " +
                  		"stop waiting for ip address.");
                  return true;
               }
            }
         });

         if (guestInfo != null && guestInfo.getToolsRunningStatus()
               .equals(ToolsRunningStatus.guestToolsRunning.toString())
               && guestInfo.getIpAddress() != null) {
            break;
         }
         if (stop) {
            break;
         }
         Thread.sleep(checkPeriod);
      }
      return null;
   }

   @Override
   public void setVm(VcVirtualMachine vm) {
      vmId = vm.getId();
      this.vm = vm;
   }

   @Override
   public VcVirtualMachine getVm() {
      return vm;
   }

   public String getVmId() {
      return vmId;
   }

   public void setVmId(String vmId) {
      this.vmId = vmId;
   }
}
