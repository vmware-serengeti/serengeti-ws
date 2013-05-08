package com.vmware.bdd.service.sp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.apitypes.Priority;
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

   public ReplaceVmPrePowerOn(String vmId, String newName, Priority ioShares) {
      this.oldVmId = vmId;
      this.newName = newName;
      this.ioShares = ioShares;
   }

   private void destroyVm(VcVirtualMachine oldVm) throws Exception {
      FaultToleranceConfigInfo info = oldVm.getConfig().getFtInfo();
      if (info != null && info.getRole() == 1) {
         logger.info("VM " + oldVm.getName()
               + " is FT primary VM, disable FT before delete it.");
         oldVm.turnOffFT();
      }
      if (oldVm.isPoweredOn()) {
         oldVm.powerOff();
      }
      /*
       * TRICK: destroy vm with unaccessible disks will throw exceptions, ignore 
       * it and destroy it again.
       */
      try {
         oldVm.destroy(false);
      } catch (Exception e) {
         logger.warn("failed to delete vm " + oldVm.getName() + " as "
               + e.getMessage());
         logger.info("try to destroy it again");
         oldVm.destroy(false);
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
            if (!Priority.Normal.equals(ioShares)) {
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