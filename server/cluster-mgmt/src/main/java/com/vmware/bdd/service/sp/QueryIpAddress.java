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

import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;
import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

import java.util.Set;

public class QueryIpAddress implements IPrePostPowerOn {
   private static final Logger logger = Logger.getLogger(QueryIpAddress.class);
   private static long checkPeriod = 10 * 1000; // 10 seconds

   private String vmId;
   private VcVirtualMachine vm;
   private long timeout;
   private Set<String> portGroups;

   /**
    * After a VM is powered on, wait for the guest information to be
    * available at most for <tt>timeout</tt> in seconds, and retrieve the
    * guest information.
    *
    * @param portGroups
    * @param timeoutInSeconds
    */
   public QueryIpAddress(Set<String> portGroups, int timeoutInSeconds) {
      this.portGroups = portGroups;
      this.timeout = timeoutInSeconds * 1000L;
   }

   @Override
   public Void call() throws Exception {
      long start = System.currentTimeMillis();
      vm = VcCache.getIgnoreMissing(vmId);
      while (System.currentTimeMillis() - start < timeout) {
         boolean stop = VcContext.inVcSessionDo(new VcSession<Boolean>() {
            @Override
            protected Boolean body() throws Exception {
               if (vm != null && vm.isPoweredOn()) {
                  return false;
               } else {
                  // stop waiting, since vm is not found
                  logger.info("vm is not found or is powered off in VC, " +
                  		"stop waiting for ip address.");
                  return true;
               }
            }
         });

         // check if all ipaddresses for portGroups are valid
         int found = 0;
         for (String pgName : portGroups) {
            String ip = VcVmUtil.getIpAddressOfPortGroup(vm, pgName, false);
            if (!ip.equals(Constants.NULL_IP)) {
               logger.info("got one ip, vm: " + vmId + ", portgroup: " + pgName + ", ip: " + ip);
               found += 1;
            }
         }

         if (found == portGroups.size()) {
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
