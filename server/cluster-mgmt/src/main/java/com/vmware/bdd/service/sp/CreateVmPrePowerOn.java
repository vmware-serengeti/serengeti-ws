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

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority;

public class CreateVmPrePowerOn implements IPrePostPowerOn {
   private VcVirtualMachine vm;
   private boolean ha;
   private boolean ft;
   private Priority ioShares;

   public CreateVmPrePowerOn(boolean ha, boolean ft, Priority ioShares) {
      this.ha = ha;
      this.ft = ft;
      this.ioShares = ioShares;
   }

   @Override
   public Void call() throws Exception {
      if (!ha) {
         disableHa(vm);
      }
      if (ft) {
         enableFt(vm);
      }
      // by default, the share level is NORMAL
      if (!Priority.NORMAL.equals(ioShares)) {
         configIOShares();
      }
      return null;
   }

   private void enableFt(final VcVirtualMachine vm) throws Exception {
      try {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected Void body() throws Exception {
               // cdrom is not supported in FT mode
               vm.detachAllCdroms();
               // do not change disk mode to persistent here,
               // instead set it to persistent before vm cloning
               vm.turnOnFT(null);
               return null;
            }

            protected boolean isTaskSession() {
               return true;
            }
         });
      } catch (Exception e) {
         throw ClusteringServiceException.ENABLE_FT_FAILED(e, vm.getName());
      }
   }

   private void disableHa(final VcVirtualMachine vm) throws Exception {
      VcCluster cluster = vm.getResourcePool().getVcCluster();
      boolean clusterHa = cluster.getConfig().getHAEnabled();
      if (!clusterHa) {
         // cluster is not ha enabled, don't need to disable ha again
         return;
      }
      try {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected Void body() throws Exception {
               vm.modifyHASettings(RestartPriority.disabled, null, null);
               return null;
            }

            protected boolean isTaskSession() {
               return true;
            }
         });
      } catch (Exception e) {
         throw ClusteringServiceException.DISABLE_HA_FAILED(e, vm.getName());
      }
   }

   private void configIOShares() throws Exception {
      VcVmUtil.configIOShares(vm.getId(), ioShares);
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