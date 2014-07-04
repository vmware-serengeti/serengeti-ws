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
package com.vmware.bdd.aop.software;

import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.software.mgmt.exception.SoftwareManagementException;
import com.vmware.bdd.utils.Constants;

public class WaitVMStatusTask implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(WaitVMStatusTask.class);
   private static final int QUERY_GUEST_VARIABLE_INTERVAL = 5000;
   private static final String DISK_FORMAT_SUCCESS = "0";
   private static final String DISK_FORMAT_INPROGRESS = "1";

   private String vmId;
   private int maxWaitingSeconds;
   public WaitVMStatusTask(String vmId, int maxWaitingSeconds) {
      this.vmId = vmId;
      this.maxWaitingSeconds = maxWaitingSeconds;
   }

   @Override
   public Void call() throws Exception {
      return VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            return callInternal();
         }
      });
   }

   private Void callInternal() {
      if (vmId == null) {
         return null;
      }
      VcVirtualMachine vm = VcCache.getIgnoreMissing(vmId);
      if (vm == null) {
         throw BddException.NOT_FOUND("Virtual Machine", vmId);
      }
      Map<String, String> variables = vm.getGuestVariables();
      String status = variables.get(Constants.VM_DISK_FORMAT_STATUS_KEY);
      try {
         long start = System.currentTimeMillis();
         long timeout = maxWaitingSeconds * 1000;
         while ((status == null 
               || status.equalsIgnoreCase(DISK_FORMAT_INPROGRESS))
               && System.currentTimeMillis() - start < timeout) {
            Thread.sleep(QUERY_GUEST_VARIABLE_INTERVAL);
            variables = vm.getGuestVariables();
            status = variables.get(Constants.VM_DISK_FORMAT_STATUS_KEY);
         }
      } catch (InterruptedException e) {
         logger.info("Waiting disk format thread is interrupted.", e);
      }
      if (status == null 
            || status.equalsIgnoreCase(DISK_FORMAT_INPROGRESS)) {
         logger.error("Didn't get disk format finished signal for vm "
               + vm.getName() + ".");
         throw SoftwareManagementException.GET_DISK_FORMAT_STATUS_ERROR(vm.getName());
      }
      if (!status.equalsIgnoreCase(DISK_FORMAT_SUCCESS)) {
         variables = vm.getGuestVariables();
         String error = variables.get(Constants.VM_DISK_FORMAT_ERROR_KEY);
         logger.error("Failed to format disk for vm " + vm.getName() + ", for " + error);
         throw SoftwareManagementException.FAILED_TO_FORMAT_DISK(vm.getName(), error);
      }
      logger.info("Disk format finished for vm " + vm.getName());
      return null;
   }
}
