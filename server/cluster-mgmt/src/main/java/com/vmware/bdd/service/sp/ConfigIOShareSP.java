/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.vim.SharesInfo;
import com.vmware.vim.binding.vim.SharesInfo.Level;
import com.vmware.vim.binding.vim.StorageResourceManager.IOAllocationInfo;
import com.vmware.vim.binding.impl.vim.StorageResourceManager_Impl.IOAllocationInfoImpl;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;

public class ConfigIOShareSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(ConfigIOShareSP.class);
   private String vmId;
   private Priority ioShares;

   public ConfigIOShareSP(String vmId, Priority ioShares) {
      super();
      this.vmId = vmId;
      this.ioShares = ioShares;
   }

   public String getVmId() {
      return vmId;
   }

   public void setVmId(String vmId) {
      this.vmId = vmId;
   }

   public Priority getIoShares() {
      return ioShares;
   }

   public void setIoShares(Priority ioShares) {
      this.ioShares = ioShares;
   }

   @Override
   public Void call() throws Exception {
      final VcVirtualMachine vcVm = getVcVm();

      if (vcVm == null) {
         logger.info("vm " + vmId + " is not found.");
         return null;
      }
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            List<VirtualDeviceSpec> deviceSpecs = new ArrayList<VirtualDeviceSpec>();
            for (DeviceId slot :vcVm.getVirtualDiskIds()) {
               SharesInfo shares = new SharesInfoImpl();
               shares.setLevel(Level.valueOf(ioShares.toString().toLowerCase()));
               IOAllocationInfo allocationInfo = new IOAllocationInfoImpl();
               allocationInfo.setShares(shares);
               VirtualDisk vmdk = (VirtualDisk) vcVm.getVirtualDevice(slot);
               vmdk.setStorageIOAllocation(allocationInfo);
               VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
               spec.setOperation(VirtualDeviceSpec.Operation.edit);
               spec.setDevice(vmdk);
               deviceSpecs.add(spec);
            }
            logger.info("reconfiguring disks in vm " + vmId + " io share level to "
                  + ioShares);
            vcVm.reconfigure(VmConfigUtil.createConfigSpec(deviceSpecs));
            logger.info("reconfigured disks in vm " + vmId + " io share level to "
                  + ioShares);
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
