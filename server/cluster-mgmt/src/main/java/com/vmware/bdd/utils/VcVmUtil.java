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
package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.MoUtil;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
import com.vmware.vim.binding.impl.vim.StorageResourceManager_Impl.IOAllocationInfoImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.SharesInfo;
import com.vmware.vim.binding.vim.SharesInfo.Level;
import com.vmware.vim.binding.vim.StorageResourceManager.IOAllocationInfo;
import com.vmware.vim.binding.vim.VirtualMachine.FaultToleranceState;
import com.vmware.vim.binding.vim.vm.GuestInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;

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

   public static boolean setBaseNodeForVm(BaseNode vNode, VcVirtualMachine vm) {
      boolean success = true;
      String vmName = vm.getName();
      vm = VcCache.getIgnoreMissing(vm.getId()); //reload vm in case vm is changed from vc
      if (vm == null) {
         logger.info("vm " + vmName
               + "is created, and then removed afterwards.");
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
         logger.error("Failed to get ip address of VM " + vNode.getVmName());
      }
      String haFlag = vNode.getNodeGroup().getHaFlag();
      if (haFlag != null && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
         // ha is enabled, need to check if secondary VM is ready either
         if (vm.getFTState() == null
               || vm.getFTState() != FaultToleranceState.running) {
            logger.fatal("Failed to power on FT secondary VM for node "
                  + vNode.getVmName() + ", " + "FT state " + vm.getFTState()
                  + " is unexpected.");
            success = false;
         }
      }

      return success;
   }

   public static VirtualDisk findVirtualDisk(String vmMobId, String externalAddr) {
      VcVirtualMachine vm = VcCache.getIgnoreMissing(vmMobId);

      DeviceId diskId = new DeviceId(externalAddr);
      VirtualDevice device = vm.getVirtualDevice(diskId);
      if (device == null)
         return null;

      AuAssert.check(device instanceof VirtualDisk);
      return (VirtualDisk) device;
   }

   public static void populateDiskInfo(final DiskEntity diskEntity,
         final String vmMobId) {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected boolean isTaskSession() {
            return true;
         }

         @Override
         protected Void body() throws Exception {
            VirtualDisk vDisk =
                  findVirtualDisk(vmMobId, diskEntity.getExternalAddress());
            if (vDisk == null)
               return null;

            VirtualDisk.FlatVer2BackingInfo backing =
                  (VirtualDisk.FlatVer2BackingInfo) vDisk.getBacking();
            Datastore ds = MoUtil.getManagedObject(backing.getDatastore());
            diskEntity.setDatastoreName(ds.getName());
            diskEntity.setVmkdPath(backing.getFileName());
            diskEntity.setDatastoreMoId(MoUtil.morefToString(ds._getRef()));

            return null;
         }
      });
   }

   public static boolean isDatastoreAccessible(String dsMobId) {
      final VcDatastore ds = VcCache.getIgnoreMissing(dsMobId);
      try {
         VcContext.inVcSessionDo(new VcSession<Void>() {
            @Override
            protected boolean isTaskSession() {
               return true;
            }

            @Override
            protected Void body() throws Exception {
               ds.update();
               return null;
            }
         });
      } catch (Exception e) {
         logger.info("failed to update datastore " + ds.getName()
               + ", ignore this error.");
      }
      if (ds != null && ds.isAccessible())
         return true;
      return false;
   }

   public static boolean configIOShares(final String vmId,
         final Priority ioShares) {
      final VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);

      if (vcVm == null) {
         logger.info("vm " + vmId + " is not found.");
         return false;
      }
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            List<VirtualDeviceSpec> deviceSpecs =
                  new ArrayList<VirtualDeviceSpec>();
            for (DeviceId slot : vcVm.getVirtualDiskIds()) {
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
            logger.info("reconfiguring disks in vm " + vmId
                  + " io share level to " + ioShares);
            vcVm.reconfigure(VmConfigUtil.createConfigSpec(deviceSpecs));
            logger.info("reconfigured disks in vm " + vmId
                  + " io share level to " + ioShares);
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
      return true;
   }

   public static boolean runSPOnSingleVM(NodeEntity node, Callable<Void> call) {
      boolean operationResult = true;
      if (node == null || node.getMoId() == null) {
         logger.info("VC vm does not exist for node: " + node.getVmName());
         return false;
      }
      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(node.getMoId());
      if (vcVm == null) {
         // cannot find VM
         logger.info("VC vm does not exist for node: " + node.getVmName());
         return false;
      }
      @SuppressWarnings("unchecked")
      Callable<Void>[] storeProceduresArray = new Callable[1];
      storeProceduresArray[0] = call;
      NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
      try {
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProceduresArray, callback);
         if (result == null) {
            logger.error("No result from composition layer");
            return false;
         } else {
            if (result[0].finished && result[0].throwable == null) {
               operationResult = true;
               logger.info("successfully run operation on vm for node: "
                     + node.getVmName());
            } else {
               operationResult = false;
               logger.error("failed in run operation on vm for node: "
                     + node.getVmName());
            }
         }
      } catch (Exception e) {
         operationResult = false;
         logger.error("error in run operation on vm.", e);
      }
      return operationResult;
   }
}