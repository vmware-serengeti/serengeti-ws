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

import com.vmware.aurora.composition.DiskSchema;
import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.StorageRead;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateVmPrePowerOn implements IPrePostPowerOn {
   private VcVirtualMachine vm;
   private boolean reserveRawDisks;
   private List<DiskSchema.Disk> disks;
   private boolean ha;
   private boolean ft;
   private Priority ioShares;

   public CreateVmPrePowerOn(boolean reserveRawDisks, List<DiskSchema.Disk> disks, boolean ha, boolean ft, Priority ioShares) {
      this.reserveRawDisks = reserveRawDisks;
      this.disks = disks;
      this.ha = ha;
      this.ft = ft;
      this.ioShares = ioShares;
   }

   @Override
   public Void call() throws Exception {
      if (!ha) {
         VcVmUtil.disableHa(vm);
      }
      if (ft) {
         VcVmUtil.enableFt(vm);
      }

      // enable disk UUID
      VcVmUtil.enableDiskUUID(vm);

      addVolumesToBootupConfigs(VcVmUtil.getVolumes(vm.getId(), disks));

      // by default, the share level is NORMAL
      if (!Priority.NORMAL.equals(ioShares)) {
         configIOShares();
      }
      return null;
   }

   private void configIOShares() throws Exception {
      VcVmUtil.configIOShares(vm.getId(), ioShares);
   }

   private void addVolumesToBootupConfigs(final String volumes) throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            Map<String, String> bootupConfigs = vm.getGuestConfigs();
            AuAssert.check(bootupConfigs != null);

            bootupConfigs.put(Constants.GUEST_VARIABLE_RESERVE_RAW_DISKS, String.valueOf(reserveRawDisks));
            bootupConfigs.put(Constants.GUEST_VARIABLE_VOLUMES, volumes);
            vm.setGuestConfigs(bootupConfigs);
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
   }

   @Override
   public void setVm(VcVirtualMachine vm) {
      this.vm = vm;
   }

   @Override
   public VcVirtualMachine getVm() {
      return vm;
   }
}
