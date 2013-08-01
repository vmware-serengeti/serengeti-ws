package com.vmware.bdd.service.sp;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.composition.NetworkSchema;
import com.vmware.aurora.composition.NetworkSchemaUtil;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vim.vm.FaultToleranceConfigInfo;

public class ReplaceVmPrePowerOn implements IPrePostPowerOn {
   private static final Logger logger = Logger
         .getLogger(ReplaceVmPrePowerOn.class);
   private String oldVmId;
   private String newName;
   private Priority ioShares;
   private VcVirtualMachine vm;
   private List<DiskSpec> fullDiskSet;
   private NetworkSchema networkSchema;

   public ReplaceVmPrePowerOn(String vmId, String newName, Priority ioShares,
         List<DiskSpec> fullDiskSet, NetworkSchema networkSchema) {
      this.oldVmId = vmId;
      this.newName = newName;
      this.ioShares = ioShares;
      this.fullDiskSet = fullDiskSet;
      this.networkSchema = networkSchema;
   }

   private void destroyVm(VcVirtualMachine oldVm) throws Exception {
      FaultToleranceConfigInfo info = oldVm.getConfig().getFtInfo();
      if (info != null && info.getRole() == 1) {
         logger.info("VM " + oldVm.getName()
               + " is FT primary VM, disable FT before delete it.");
         oldVm.turnOffFT();
      }
      // try guest shut down first, wait for 3 minutes, power it off after time out
      if (oldVm.isPoweredOn()
            && !oldVm
                  .shutdownGuest(Constants.VM_FAST_SHUTDOWN_WAITING_SEC * 1000)) {
         oldVm.powerOff();
      }
      
      /*
       * TRICK: destroy vm with unaccessible disks will throw exceptions, ignore 
       * it and destroy it again.
       */
      try {
         // detach existed vmdks on the old vm
         for (DiskSpec disk : fullDiskSet) {
            if (disk.getVmdkPath() != null && !disk.getVmdkPath().isEmpty()) {
               oldVm.detachVirtualDisk(new DeviceId(disk.getExternalAddress()),
                     false);
            }
         }

         oldVm.destroy(false);
      } catch (Exception e) {
         logger.warn("failed to delete vm " + oldVm.getName() + " as "
               + e.getMessage());
         logger.info("try to unregister it again");
         oldVm.unregister();
      }
      logger.info("VM " + oldVm.getName() + " deleted");
   }

   private OptionValue[] getVhmExtraConfigs(VcVirtualMachine oldVm) {
      List<OptionValue> options = new ArrayList<OptionValue>();
      for (OptionValue option : oldVm.getConfig().getExtraConfig()) {
         if (option.getKey().startsWith("vhmInfo")) {
            options.add(option);
         }
      }
      return options.toArray(new OptionValue[options.size()]);
   }
   
   private void copyNicSettings(VcVirtualMachine oldVm) throws Exception {
      ConfigSpec configSpec = new ConfigSpecImpl();
      NetworkSchemaUtil.copyMacAddresses(configSpec, oldVm, vm, networkSchema);
      vm.reconfigure(configSpec);
   }

   @Override
   public Void call() throws Exception {
      final VcVirtualMachine oldVm = VcCache.getIgnoreMissing(oldVmId);
      if (oldVm == null) {
         logger.info("vm " + oldVmId
               + " is not found in VC, ignore this delete.");
         return null;
      }

      // delete old vm and rename the replacement VM to original name
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            // copy parent vm's mac addresses
            logger.info("copy parent vm's mac addresses");
            copyNicSettings(oldVm);
            
            // copy vhm related extra configures
            logger.info("copy vhm related extra configs from parent vm");
            OptionValue[] optionValues = getVhmExtraConfigs(oldVm);
            if (optionValues.length != 0) {
               ConfigSpec spec = new ConfigSpecImpl();
               spec.setExtraConfig(optionValues);
               vm.reconfigure(spec);
            }

            // destroy vm
            logger.info("destroy parent vm");
            destroyVm(oldVm);

            // rename vm
            logger.info("VM " + vm.getName() + " renamed to " + newName);
            vm.rename(newName);

            // copy the io share level from the original vm
            logger.info("set io share level same with parent vm");
            if (!Priority.NORMAL.equals(ioShares)) {
               VcVmUtil.configIOShares(oldVmId, ioShares);
            }
            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
      return null;
   }

   @Override
   public void setVm(VcVirtualMachine vm) {
      this.vm = vm;
   }

   @Override
   public VcVirtualMachine getVm() {
      return this.vm;
   }
}