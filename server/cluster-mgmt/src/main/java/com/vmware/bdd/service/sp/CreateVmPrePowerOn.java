package com.vmware.bdd.service.sp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.vim.binding.impl.vim.SharesInfoImpl;
import com.vmware.vim.binding.impl.vim.StorageResourceManager_Impl.IOAllocationInfoImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.vim.SharesInfo;
import com.vmware.vim.binding.vim.SharesInfo.Level;
import com.vmware.vim.binding.vim.StorageResourceManager.IOAllocationInfo;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;

public class CreateVmPrePowerOn implements IPrePostPowerOn {
   private static final Logger logger = Logger
         .getLogger(CreateVmPrePowerOn.class);
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
      List<VirtualDeviceSpec> deviceSpecs = new ArrayList<VirtualDeviceSpec>();
      for (DeviceId slot : vm.getVirtualDiskIds()) {
         SharesInfo shares = new SharesInfoImpl();
         shares.setLevel(Level.valueOf(ioShares.toString().toLowerCase()));
         IOAllocationInfo allocationInfo = new IOAllocationInfoImpl();
         allocationInfo.setShares(shares);
         VirtualDisk vmdk = (VirtualDisk) vm.getVirtualDevice(slot);
         vmdk.setStorageIOAllocation(allocationInfo);
         VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
         spec.setOperation(VirtualDeviceSpec.Operation.edit);
         spec.setDevice(vmdk);
         deviceSpecs.add(spec);
      }
      logger.info("reconfiguring disks in vm " + vm.getId()
            + " io share level to " + ioShares);
      vm.reconfigure(VmConfigUtil.createConfigSpec(deviceSpecs));
      logger.info("reconfigured disks in vm " + vm.getId()
            + " io share level to " + ioShares);
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