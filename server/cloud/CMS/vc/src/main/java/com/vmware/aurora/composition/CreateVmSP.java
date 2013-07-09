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

package com.vmware.aurora.composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.DiskType;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcSnapshot;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VcVirtualMachine.DiskCreateSpec;
import com.vmware.aurora.vc.VmConfigUtil;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.utils.Constants;
import com.vmware.vim.binding.impl.vim.vApp.ProductSpecImpl;
import com.vmware.vim.binding.impl.vim.vApp.VmConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDiskImpl;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.option.ArrayUpdateSpec.Operation;
import com.vmware.vim.binding.vim.vApp.ProductInfo;
import com.vmware.vim.binding.vim.vApp.ProductSpec;
import com.vmware.vim.binding.vim.vApp.VmConfigInfo;
import com.vmware.vim.binding.vim.vApp.VmConfigSpec;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;

/**
 * Stored Procedure for Creating a VM from a template
 * 
 * @author sridharr
 * 
 */
public class CreateVmSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(CreateVmSP.class);
   final String newVmName;
   final VmSchema vmSchema;
   final VcResourcePool targetRp;
   final VcDatastore targetDs;
   final IPrePostPowerOn prePowerOn;
   final IPrePostPowerOn postPowerOn;
   final Map<String, String> bootupConfigs;
   final boolean linkedClone;
   final Folder vmFolder; /* optional */
   final VcHost host; /* optinal */

   private VcVirtualMachine vcVm = null;

   public CreateVmSP(String newVmName, VmSchema vmSchema,
         VcResourcePool targetRp, VcDatastore targetDs,
         IPrePostPowerOn prePowerOn, IPrePostPowerOn postPowerOn,
         Map<String, String> bootupConfigs, boolean linkedClone, Folder vmFolder) {
      this(newVmName, vmSchema, targetRp, targetDs, prePowerOn, postPowerOn,
            bootupConfigs, linkedClone, vmFolder, null);
   }

   public CreateVmSP(String newVmName, VmSchema vmSchema,
         VcResourcePool targetRp, VcDatastore targetDs,
         IPrePostPowerOn prePowerOn, IPrePostPowerOn postPowerOn,
         Map<String, String> bootupConfigs, boolean linkedClone,
         Folder vmFolder, VcHost host) {
      this.newVmName = newVmName;
      this.vmSchema = vmSchema;
      this.targetRp = targetRp;
      this.targetDs = targetDs;
      this.prePowerOn = prePowerOn;
      this.postPowerOn = postPowerOn;
      this.bootupConfigs = bootupConfigs;
      this.linkedClone = linkedClone;
      this.vmFolder = vmFolder;
      this.host = host;
   }

   public CreateVmSP(VcVirtualMachine vcVm, VmSchema vmSchema,
         VcResourcePool targetRp, VcDatastore targetDs,
         IPrePostPowerOn prePowerOn, IPrePostPowerOn postPowerOn,
         Map<String, String> bootupConfigs, boolean linkedClone,
         Folder vmFolder, VcHost host) {
      this.vcVm = vcVm;
      this.newVmName = vcVm.getName();
      this.vmSchema = vmSchema;
      this.targetRp = targetRp;
      this.targetDs = targetDs;
      this.prePowerOn = prePowerOn;
      this.postPowerOn = postPowerOn;
      this.bootupConfigs = bootupConfigs;
      this.linkedClone = linkedClone;
      this.vmFolder = vmFolder;
      this.host = host;
   }

   @Override
   public Void call() throws Exception {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected boolean isTaskSession() {
            return true;
         }

         @Override
         protected Void body() throws Exception {
            callInternal();
            return null;
         }
      });
      return null;
   }

   private boolean requireClone() {
      for (Disk disk : vmSchema.diskSchema.getDisks()) {
         // if the system disk is already exist, skip clone
         if (DiskType.OS.getTypeName().equals(disk.type)
               && disk.vmdkPath != null && !disk.vmdkPath.isEmpty()) {
            return false;
         }
      }
      return true;
   }

   private boolean vmExists() {
      if (vcVm != null) {
         // here get VM instance once again, to make sure the VM is not deleted by other client by mistake
         vcVm = VcCache.getIgnoreMissing(vcVm.getId());
         if (vcVm != null) {
            return true;
         }
      }
      return false;
   }

   /**
    * copy parent vm's configurations, includes vApp configs, hardware version
    * info
    */
   private void copyParentVmSettings(VcVirtualMachine template,
         ConfigSpec configSpec) {
      configSpec.setName(newVmName);

      // copy guest OS info
      configSpec.setGuestId(template.getConfig().getGuestId());

      // copy hardware version
      configSpec.setVersion(template.getConfig().getVersion());

      // copy vApp config info
      VmConfigInfo configInfo = template.getConfig().getVAppConfig();

      if (configInfo == null) {
         // the parent vm does not have vApp option enabled. This is possible when user
         // used customized template.
         return;
      }

      VmConfigSpec vAppSpec = new VmConfigSpecImpl();
      vAppSpec.setOvfEnvironmentTransport(configInfo
            .getOvfEnvironmentTransport());

      // product info
      List<ProductSpec> productSpecs = new ArrayList<ProductSpec>();
      for (ProductInfo info : configInfo.getProduct()) {
         ProductSpec spec = new ProductSpecImpl();
         spec.setInfo(info);
         spec.setOperation(Operation.add);
         productSpecs.add(spec);
      }
      vAppSpec.setProduct(productSpecs.toArray(new ProductSpec[productSpecs
            .size()]));

      configSpec.setVAppConfig(vAppSpec);
   }

   public void callInternal() throws Exception {
      // Find the template and template snapshot to clone from
      final VcVirtualMachine template =
            VcCache.get(vmSchema.diskSchema.getParent());
      VcSnapshot snap =
            template.getSnapshotByName(vmSchema.diskSchema.getParentSnap());

      ConfigSpecImpl configSpec = new ConfigSpecImpl();

      HashMap<String, Disk.Operation> diskMap =
            new HashMap<String, Disk.Operation>();
      if (vmExists()) {
         // try to revert the VM to snapshot, to rollback wrong configuration
         VcSnapshot snapshot = vcVm.getSnapshotByName(Constants.ROOT_SNAPSTHOT_NAME);
         if (snapshot == null) {
            logger.info("Invalide VM for Serengeti root snapshot does not exist, remove it.");
            vcVm.destroy();
            vcVm = null;
         } else {
            logger.info("Revert snapshot");
            snapshot.revert();
         }
      }
      if (!vmExists()) {
         if (requireClone()) {
            VcVirtualMachine.CreateSpec vmSpec =
                  new VcVirtualMachine.CreateSpec(newVmName, snap, targetRp,
                        targetDs, vmFolder, linkedClone, configSpec);
            // Clone from the template
            vcVm = template.cloneVm(vmSpec, null);
         } else {
            copyParentVmSettings(template, configSpec);
            vcVm = targetRp.createVm(configSpec, targetDs, vmFolder);
         }
      }

      if (host != null) {
         vcVm.disableDrs();
      }

      // Get list of disks to add
      List<VcHost> hostList = new ArrayList<VcHost>();
      List<DiskCreateSpec> addDisks =
            DiskSchemaUtil.getDisksToAdd(hostList, targetRp, targetDs,
                  vmSchema.diskSchema, diskMap);

      DiskCreateSpec[] tmpAddDisks =
            addDisks.toArray(new DiskCreateSpec[addDisks.size()]);

      // If current host of VM is not in the list of hosts with access to the
      // datastore(s) for the new disk(s), then migrate the VM first
      if (hostList.size() > 0 && !hostList.contains(vcVm.getHost())) {
         vcVm.migrate(hostList.get(0));
      }
      // add the new disks
      vcVm.changeDisks(null, tmpAddDisks);

      // attach existed disks
      List<VirtualDeviceSpec> deviceChange = new ArrayList<VirtualDeviceSpec>();
      for (Disk disk : vmSchema.diskSchema.getDisks()) {
         if (disk.vmdkPath == null || disk.vmdkPath.isEmpty())
            continue;

         VirtualDisk.FlatVer2BackingInfo backing =
               new VirtualDiskImpl.FlatVer2BackingInfoImpl();
         backing.setFileName(disk.vmdkPath);
         backing.setDiskMode(disk.mode.toString());

         deviceChange.add(vcVm.attachVirtualDiskSpec(new DeviceId(
               disk.externalAddress), backing, false, DiskSize
               .sizeFromMB(disk.initialSizeMB)));
      }

      if (!deviceChange.isEmpty()) {
         vcVm.reconfigure(VmConfigUtil.createConfigSpec(deviceChange));
      }

      if (linkedClone) {
         // Promote necessary disks
         ArrayList<DeviceId> disksToPromote = new ArrayList<DeviceId>();
         for (Entry<String, Disk.Operation> entry : diskMap.entrySet()) {
            if (entry.getValue() == Disk.Operation.PROMOTE) {
               disksToPromote.add(new DeviceId(entry.getKey()));
            }
         }
         if (disksToPromote.size() >= 1) {
            vcVm.promoteDisks(disksToPromote.toArray(new DeviceId[0]));
         }
      }

      // XXX : TODO - can we set these in the ConfigSpec BEFORE clone? Current assert does not allow that.
      ConfigSpecImpl newConfigSpec = new ConfigSpecImpl();
      // Network changes
      NetworkSchemaUtil.setNetworkSchema(newConfigSpec,
            targetRp.getVcCluster(), vmSchema.networkSchema, vcVm);

      // Resource schema
      ResourceSchemaUtil.setResourceSchema(newConfigSpec,
            vmSchema.resourceSchema);

      // Add managed-by information
      // VmConfigUtil.addManagedByToConfigSpec(
      //      newConfigSpec, VcContext.getService().getExtensionKey(), "dbvm");

      vcVm.reconfigure(newConfigSpec);

      // set the bootup configs
      if (bootupConfigs != null) {
         vcVm.setGuestConfigs(bootupConfigs);
      }

      if (prePowerOn != null) {
         prePowerOn.setVm(vcVm);
         prePowerOn.call();
      }

      vcVm.powerOn(host);

      if (postPowerOn != null) {
         postPowerOn.setVm(vcVm);
         postPowerOn.call();
      }
   }

   public VcVirtualMachine getVM() {
      return vcVm;
   }
}
