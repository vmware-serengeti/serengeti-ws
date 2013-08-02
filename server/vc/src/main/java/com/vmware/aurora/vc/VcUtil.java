/************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.CommonUtil;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.ResourceAllocationInfo;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

/**
 * VC related static functions.
 */
public class VcUtil {
   /**
    * Checks if the allocation info is valid for a resource bundle RP.
    * 
    * @param cpu
    *           The cpu alloc info.
    * @param mem
    *           The mem alloc info.
    * @return reasons for incompatible.
    */
   protected static List<String> getCPUMemAllocIncompatReasons(
         ResourceAllocationInfo cpu, ResourceAllocationInfo mem) {
      List<String> reasons = new ArrayList<String>();
      if (!Configuration.getBoolean("vc.skipRpCheck", false)) {
         CommonUtil.checkCond((cpu.getLimit().equals(cpu.getReservation())),
               reasons, "CPU limit is not equal to reservation.");
         CommonUtil.checkCond((mem.getLimit().equals(mem.getReservation())),
               reasons, "Memory limit is not equal to reservation.");
         CommonUtil.checkCond((cpu.getReservation() > 0), reasons,
               "CPU reservation is equal to zero.");
         CommonUtil.checkCond((mem.getReservation() > 0), reasons,
               "Memory reservation is equal to zero.");
      } // else return empty reasons list
      return reasons;
   }

   public static List<String> getIncompatReasonsForSysRp(VcResourcePool sysRp) {
      List<String> reasons = new ArrayList<String>();
      ResourceAllocationInfo cpu = sysRp.getCpuAllocationInfo();
      ResourceAllocationInfo mem = sysRp.getMemAllocationInfo();
      if (sysRp.isRootRP()) {
         CommonUtil.checkCond((cpu.getReservation() > 0), reasons,
               "No available CPU resource.");
         CommonUtil.checkCond((mem.getReservation() > 0), reasons,
               "No available Memory resource.");
      } else {
         CommonUtil
               .checkCond((cpu.getReservation() > 0 || (cpu
                     .getExpandableReservation() && cpu.getLimit() > 0)),
                     reasons, "No available CPU resource.");
         CommonUtil
               .checkCond((mem.getReservation() > 0 || (mem
                     .getExpandableReservation() && mem.getLimit() > 0)),
                     reasons, "No available Memory resource.");
      }
      return reasons;
   }

   public static List<String> getIncompatReasonsForDatastore(
         VcDatastore datastore) {
      List<String> reasons = new ArrayList<String>();
      CommonUtil.checkCond(!datastore.isInStoragePod(), reasons,
            "The datastore can't be in storage pod.");
      CommonUtil
            .checkCond(
                  !datastore.isVmfs() || datastore.isSupportedVmfsVersion(),
                  reasons,
                  "The datastore file system is not supported. Data Director requires VMFS 5 or greater.");
      return reasons;
   }

   public static boolean isValidAllocationForResourceBundleRP(
         ResourceAllocationInfo cpu, ResourceAllocationInfo mem) {
      return getCPUMemAllocIncompatReasons(cpu, mem).isEmpty();
   }

   public static boolean isValidAllocationForResourceBundleRP(
         VcResourcePool rp, boolean forSysRb) {
      ResourceAllocationInfo cpu = rp.getCpuAllocationInfo();
      ResourceAllocationInfo mem = rp.getMemAllocationInfo();
      if (forSysRb) {
         return getIncompatReasonsForSysRp(rp).isEmpty();
      } else {
         return getCPUMemAllocIncompatReasons(cpu, mem).isEmpty();
      }
   }

   public static VcVirtualMachine createVm(final Folder parentFolder,
         final ConfigSpec spec, final VcResourcePool rp, final HostSystem host,
         final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            ManagedObjectReference hostRef =
                  host != null ? host._getRef() : null;
            ManagedObjectReference taskRef =
                  parentFolder.createVm(spec, rp.getMoRef(), hostRef);
            return new VcTask(TaskType.CreateVm, taskRef, callback);
         }
      });
      task.waitForCompletion();
      return (VcVirtualMachine) task.getResult();
   }

   public static void processNotFoundException(ManagedObjectNotFound e,
         String moId, Logger logger) throws Exception {
      ManagedObjectReference moRef = e.getObj();
      if (MoUtil.morefToString(moRef).equals(moId)) {
         logger.error("VC object " + MoUtil.morefToString(moRef)
               + " is already deleted from VC. Purge from vc cache");
         // in case the event is lost
         VcCache.purge(moRef);
         ManagedObjectReference rpMoRef = VcCache.removeVmRpPair(moRef);
         if (rpMoRef != null) {
            VcCache.refresh(rpMoRef);
         }
      } else {
         throw e;
      }
   }
}
