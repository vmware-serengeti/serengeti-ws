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

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.vim.VirtualMachine.FaultToleranceState;
import com.vmware.vim.binding.vim.vm.GuestInfo;

public class VcVmUtil {
   private static final Logger logger = Logger.getLogger(VcVmUtil.class);
   public static String getIpAddress(final VcVirtualMachine vcVm, 
         boolean inSession) {
      try {
         if (inSession) {
            return vcVm.queryGuest().getIpAddress();
         }
         String ip = VcContext.inVcSessionDo(new VcSession<String>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }
            @Override
            public String body() throws Exception {
               GuestInfo guest = vcVm.queryGuest();
               return guest.getIpAddress();
            }
         });
         return ip;
      } catch (Exception e) {
         throw BddException.wrapIfNeeded(e, e.getLocalizedMessage());
      }
   }

   public static String getGuestHostName(final VcVirtualMachine vcVm,
         boolean inSession) {
      try {
         if (inSession) {
            return vcVm.queryGuest().getHostName();
         }
         String hostName = VcContext.inVcSessionDo(new VcSession<String>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }
            @Override
            public String body() throws Exception {
               GuestInfo guest = vcVm.queryGuest();
               return guest.getHostName();
            }
         });
         return hostName;
      } catch (Exception e) {
         throw BddException.wrapIfNeeded(e, e.getLocalizedMessage());
      }
   }

   public static boolean setBaseNodeForVm(BaseNode vNode, 
         VcVirtualMachine vm) {
      boolean success = true;
      String vmName = vm.getName();
      vm = VcCache.getIgnoreMissing(vm.getId()); //reload vm in case vm is changed from vc
      if (vm == null) {
         logger.info("vm " + vmName + "is created, and then removed afterwards.");
      }
      String ip = null;
      if (vm != null) {
         ip = VcVmUtil.getIpAddress(vm, false);
      }
      if (ip != null) {
         vNode.setSuccess(true);
         vNode.setIpAddress(ip);
         vNode.setGuestHostName(VcVmUtil.getGuestHostName(vm, false));
         vNode.setTargetHost(vm.getHost().getName());
         vNode.setTargetRp(vm.getResourcePool().getName());
         vNode.setTargetVcCluster(vm.getResourcePool()
               .getVcCluster().getName());
         vNode.setVmMobId(vm.getId());
         if (vm.isPoweredOff()) {
            vNode.setNodeStatus(NodeStatus.POWERED_OFF);
            vNode.setNodeAction(Constants.NODE_ACTION_CLONING_FAILED);
         } else {
            vNode.setNodeStatus(NodeStatus.VM_READY);
            vNode.setNodeAction(null);
         }
      } else {
         vNode.setSuccess(false);
         if (vm != null) {
            vNode.setVmMobId(vm.getId());
            if (vm.isPoweredOn()) {
               vNode.setNodeStatus(NodeStatus.POWERED_ON);
               vNode.setNodeAction(Constants.NODE_ACTION_GET_IP_FAILED);
            } else {
               vNode.setNodeStatus(NodeStatus.POWERED_OFF);
               vNode.setNodeAction(Constants.NODE_ACTION_CLONING_FAILED);
            }
         }
         success = false;
         logger.error("Failed to get ip address of VM "
               + vNode.getVmName());
      }
      String haFlag = vNode.getNodeGroup().getHaFlag();
      if (haFlag != null && 
            Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
         // ha is enabled, need to check if secondary VM is ready either
         if(vm.getFTState() == null ||
               vm.getFTState() != FaultToleranceState.running) {
            logger.fatal("Failed to power on FT secondary VM for node "
                  + vNode.getVmName() + ", " + "FT state " 
                  + vm.getFTState() + " is unexpected.");
            success = false;
         }
      }

      return success;
   }
}