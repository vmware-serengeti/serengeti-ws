/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VcVirtualMachine.DiskCreateSpec;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class ScaleVMSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(ScaleVMSP.class);
   private String vmId;
   private int cpuNumber;
   private long memory;
   private VcDatastore targetDs;
   private DiskEntity swapDisk;
   private long newSwapSizeInMB;


   public ScaleVMSP(String vmId, int cpuNumber, long memory,
         VcDatastore targetDs, DiskEntity swapDisk, long newSwapSizeInMB) {
      this.vmId = vmId;
      this.cpuNumber = cpuNumber;
      this.memory = memory;
      this.targetDs = targetDs;
      this.swapDisk = swapDisk;
      this.newSwapSizeInMB = newSwapSizeInMB;
   }

   /* (non-Javadoc)
    * @see java.util.concurrent.Callable#call()
    */
   @Override
   public Void call() throws Exception {
      final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
      if (vcVm == null) {
         logger.info("vm: " + vmId + " is not found.");
         return null;
      }
      if (vcVm.isPoweredOn()) {
         logger.info("vm " + vcVm.getName()
               + " must be powered off before scaling");
         return null;
      }
      logger.info("scale vm,vmId:" + vmId + ",cpuNumber:" + cpuNumber
            + ",memory:" + memory);
      return VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {            
            //start config vm if max configuration check is passed
            ConfigSpecImpl newConfigSpec = new ConfigSpecImpl();
            if (cpuNumber > 0) {
               newConfigSpec.setNumCPUs(cpuNumber);
            }
            if (memory > 0) {
               VmConfigUtil.setMemoryAndBalloon(newConfigSpec, memory);

               if (targetDs != null) {
                  VirtualDisk vmSwapDisk =
                        VcVmUtil.findVirtualDisk(vmId,
                              swapDisk.getExternalAddress());
                  logger.info("current ds swap disk placed: "
                        + swapDisk.getDatastoreName());
                  logger.info("target ds to place swap disk: "
                        + targetDs.getName());
                  if (swapDisk.getDatastoreMoId() == targetDs.getId()) {
                     VirtualDeviceSpec devSpec = new VirtualDeviceSpecImpl();
                     devSpec.setOperation(VirtualDeviceSpec.Operation.edit);
                     vmSwapDisk.setCapacityInKB(newSwapSizeInMB * 1024);
                     devSpec.setDevice(vmSwapDisk);
                     VirtualDeviceSpec[] changes = { devSpec };
                     newConfigSpec.setDeviceChange(changes);
                     logger.info("finished resize swap disk size");
                  } else {
                     vcVm.detachVirtualDisk(
                           new DeviceId(swapDisk.getExternalAddress()), true);
                     AllocationType allocType =
                           swapDisk.getAllocType() == null ? null : AllocationType
                                 .valueOf(swapDisk.getAllocType());
                     DiskCreateSpec[] addDisks =
                           { new DiskCreateSpec(new DeviceId(swapDisk
                                 .getExternalAddress()), targetDs, swapDisk
                                 .getName(), DiskMode.independent_persistent,
                                 DiskSize.sizeFromMB(newSwapSizeInMB), allocType) };
                     // changeDisks() will run vcVm.reconfigure() itself
                     vcVm.changeDisks(null, addDisks);
                  }
               }
            }

            vcVm.reconfigure(newConfigSpec);
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
   }
}
