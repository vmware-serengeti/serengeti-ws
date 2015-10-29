/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.aurora.composition.ResourceSchemaUtil;
import com.vmware.bdd.apitypes.LatencyPriority;
import com.vmware.vim.binding.vim.option.OptionValue;
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

               //Check the cpu reservation ratio, and set vm's cpu reservation accord to it
               checkAndSetReservedCpu(vcVm, newConfigSpec, cpuNumber);
            }
            if (memory > 0) {
               VmConfigUtil.setMemoryAndBalloon(newConfigSpec, memory);

               if (targetDs != null) {
                  logger.info("current ds swap disk placed: "
                        + swapDisk.getDatastoreName());
                  logger.info("target ds to place swap disk: "
                        + targetDs.getName());
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

               //check latencySensitivity &memory reservation ratio, then set the reserved memory to new VM's memory
               checkAndSetReservedMem(vcVm, newConfigSpec, memory);
            }

            vcVm.reconfigure(newConfigSpec);
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
   }

   //Get original vm reserved CPU and caculate the ratio, then set the new reservedCpu according to the new vcpu number
   private void checkAndSetReservedCpu(VcVirtualMachine vcVm, ConfigSpecImpl newConfigSpec, int cpuNumber){
      float originalCpuNumber = vcVm.getConfig().getHardware().getNumCPU();;
      float originalReservedCpu = vcVm.getConfig().getCpuAllocation().getReservation();
      float hostCpuMhz = vcVm.getHost().getCpuHz()/(1024*1024);
      float reservedCpu_Ration = originalReservedCpu/(hostCpuMhz*originalCpuNumber);
      if(reservedCpu_Ration > 0 && reservedCpu_Ration <= 1) {
         long newReservedCpu =
               (long) Math.ceil(vcVm.getHost().getCpuHz() / (1024 * 1024)
                     * cpuNumber * reservedCpu_Ration);
         ResourceSchemaUtil.setCpuAllocationSize(newConfigSpec,
               vcVm.getConfig().getCpuAllocation(), newReservedCpu);
      }
   }

   //Check latencySensitivity & cpu reservation ratio, and set vm's cpu reservation accord to it
   private void checkAndSetReservedMem(VcVirtualMachine vcVm, ConfigSpecImpl newConfigSpec, long memory){
      OptionValue[] extraConfig = vcVm.getConfig().getExtraConfig();
      Boolean highLatency = false;

      //If vm's latencySensitivity is High, set the reserved memory to new VM's memory
      for(OptionValue tmp: extraConfig){
         if (tmp.getKey().equalsIgnoreCase("sched.cpu.latencySensitivity")) {
            String value = (String)tmp.getValue();
            if(value.equalsIgnoreCase(LatencyPriority.HIGH.toString())) {
               ResourceSchemaUtil.setMemReservationSize(newConfigSpec,
                     vcVm.getConfig().getMemoryAllocation(), memory);
               highLatency = true;
               break;
            }
         }
      }
      //If vm's latencySensitivity is not high & reservedMemory_ratio is not 0, reserved memory = new VM's memory*reservedMem_Ration
      if(highLatency == false){
         float originalMem = vcVm.getConfig().getHardware().getMemoryMB();;
         float originalReservedMem = vcVm.getConfig().getMemoryAllocation().getReservation();
         float reservedMem_Ration = originalReservedMem/originalMem;
         if(reservedMem_Ration > 0 && reservedMem_Ration <= 1) {
            ResourceSchemaUtil.setMemReservationSize(newConfigSpec,
                  vcVm.getConfig().getMemoryAllocation(), (long)Math.ceil(memory*reservedMem_Ration));
         }
      }

   }
}
