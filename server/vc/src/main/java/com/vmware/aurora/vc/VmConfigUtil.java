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

package com.vmware.aurora.vc;

import java.util.List;

import com.google.gson.Gson;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.util.AuAssert;
import com.vmware.bdd.apitypes.LatencyPriority;
import com.vmware.vim.binding.impl.vim.DescriptionImpl;
import com.vmware.vim.binding.impl.vim.ext.ManagedByInfoImpl;
import com.vmware.vim.binding.impl.vim.option.OptionValueImpl;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.device.ParaVirtualSCSIControllerImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualBusLogicControllerImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualCdromImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDiskImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualE1000Impl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualEthernetCardImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualFloppyImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualLsiLogicControllerImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualLsiLogicSASControllerImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualSCSIControllerImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualVmxnet3Impl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualVmxnetImpl;
import com.vmware.vim.binding.vim.Description;
import com.vmware.vim.binding.vim.ext.ManagedByInfo;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vim.vm.ConfigSpec;
import com.vmware.vim.binding.vim.vm.device.ParaVirtualSCSIController;
import com.vmware.vim.binding.vim.vm.device.VirtualBusLogicController;
import com.vmware.vim.binding.vim.vm.device.VirtualController;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;
import com.vmware.vim.binding.vim.vm.device.VirtualEthernetCard;
import com.vmware.vim.binding.vim.vm.device.VirtualLsiLogicController;
import com.vmware.vim.binding.vim.vm.device.VirtualLsiLogicSASController;
import com.vmware.vim.binding.vim.vm.device.VirtualSCSIController;
import com.vmware.vim.binding.vim.vm.device.VirtualSCSIController.Sharing;


/**
 * Utility functions for help configuring virtual machine.
 */
public class VmConfigUtil {
   public enum ScsiControllerType {
      PVSCSI(ParaVirtualSCSIController.class, ParaVirtualSCSIControllerImpl.class),
      LSILOGIC(VirtualLsiLogicController.class, VirtualLsiLogicControllerImpl.class),
      LSILOGICSAS(VirtualLsiLogicSASController.class, VirtualLsiLogicSASControllerImpl.class),
      BUSLOGIC(VirtualBusLogicController.class, VirtualBusLogicControllerImpl.class);

      public final Class<? extends VirtualSCSIController> infClass;
      public final Class<? extends VirtualSCSIControllerImpl> implClass;

      ScsiControllerType(Class<? extends VirtualSCSIController> infClass,
            Class<? extends VirtualSCSIControllerImpl> implClass) {
         this.infClass = infClass;
         this.implClass = implClass;
      }

      VirtualSCSIController createController() {
         try {
            return implClass.newInstance();
         } catch (Exception e) {
            AuAssert.INTERNAL(e);
            return null;
         }
      }

      public static ScsiControllerType findController(Class<? extends VirtualSCSIController> clazz) {
         for (ScsiControllerType type : ScsiControllerType.values()) {
            if (type.infClass == clazz) {
               return type;
            }
         }
         return null;
      }
   }

   public enum EthernetControllerType {
      E1000(VirtualE1000Impl.class), VMXNET(VirtualVmxnetImpl.class), VMXNET3(
            VirtualVmxnet3Impl.class);

      private final Class<? extends VirtualEthernetCardImpl> clazz;

      EthernetControllerType(Class<? extends VirtualEthernetCardImpl> clazz) {
         this.clazz = clazz;
      }

      VirtualEthernetCard createController() {
         try {
            return clazz.newInstance();
         } catch (Exception e) {
            AuAssert.INTERNAL(e);
            return null;
         }
      }
   }

   private static final String HOT_ADD_CPU_KEY = "vcpu.hotadd";
   private static final String HOT_ADD_MEMORY_KEY = "mem.hotadd";

   public static final String MACHINE_ID = "machine.id";
   public static final String DBVM_CONFIG = "dbvm.config";

   /**
    * Create a scsi controller device for a vm.
    *
    * @param type
    * @param busNum
    * @return virtual device spec of the controller
    */
   public static VirtualDeviceSpec createControllerDevice(
         ScsiControllerType type, int busNum) {
      VirtualSCSIController controller = type.createController();
      controller.setBusNumber(busNum);
      controller.setHotAddRemove(true);
      controller.setKey(-1);
      controller.setSharedBus(Sharing.noSharing);
      return addDeviceSpec(controller);
   }

   public static VirtualDeviceSpec removeDeviceSpec(int key) {
      VirtualDevice dev = new VirtualDeviceImpl();
      dev.setKey(key);
      VirtualDeviceSpec devSpec = new VirtualDeviceSpecImpl();
      devSpec.setOperation(VirtualDeviceSpec.Operation.remove);
      devSpec.setDevice(dev);
      return devSpec;
   }

   public static VirtualDeviceSpec removeDeviceSpec(VirtualDevice dev) {
      VirtualDeviceSpec devSpec = new VirtualDeviceSpecImpl();
      devSpec.setOperation(VirtualDeviceSpec.Operation.remove);
      devSpec.setDevice(dev);
      return devSpec;
   }

   public static VirtualDeviceSpec createNetworkDevice(
         EthernetControllerType type) {
      VirtualEthernetCard nic = type.createController();
      nic.setKey(-1);
      return addDeviceSpec(nic);
   }

   public static VirtualDeviceSpec createNetworkDevice(
         EthernetControllerType type, String label, VcNetwork vN) {
      VirtualDeviceSpec deviceSpec = createNetworkDevice(type);

      VirtualDevice device = deviceSpec.getDevice();
      Description deviceInfo = new DescriptionImpl();
      deviceInfo.setSummary("XXX");
      deviceInfo.setLabel(label);
      device.setDeviceInfo(deviceInfo);
      setVirtualDeviceBacking(device, vN.getBackingInfo());

      return deviceSpec;
   }

   public static VirtualDeviceSpec addDeviceSpec(VirtualDevice dev) {
      VirtualDeviceSpec devSpec = new VirtualDeviceSpecImpl();
      devSpec.setOperation(VirtualDeviceSpec.Operation.add);
      devSpec.setDevice(dev);
      return devSpec;
   }

   /**
    * Create a default scsi controller at bus 0.
    *
    * @return virtual device spec of the controller
    */
   public static VirtualDeviceSpec createControllerDevice() {
      return createControllerDevice(ScsiControllerType.LSILOGIC, 0);
   }

   /**
    * Create a virtual disk backing object.
    *
    * @param vmdkPath
    *           full vmdk path in the datastore
    * @param diskMode
    *           virtual disk access mode
    * @return backing object
    */
   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         String vmdkPath, DiskMode diskMode) {
      return createVmdkBackingInfo(vmdkPath, diskMode, null, null, null);
   }

   /**
    * Create a virtual disk backing object.
    *
    * @param vmdkPath
    *           full vmdk path in the datastore
    * @param diskMode
    *           virtual disk access mode
    * @param parentBacking
    *           indicates creation of a child delta disk attached to this parent
    *           backing.
    * @param thinDisk
    *           requests creation of a thin disk, null if the field should not
    *           be set
    * @param eagerlyScrub Flag to indicate to the underlying filesystem whether the
    *  virtual disk backing file should be scrubbed completely at this time.
    * @see com.vmware.vim.binding.vim.vm.device.VirtualDisk.FlatVer2BackingInfo#setEagerlyScrub
    * @return backing object
    */
   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         String vmdkPath, DiskMode diskMode,
         VirtualDisk.FlatVer2BackingInfo parentBacking, Boolean thinDisk, Boolean eagerlyScrub) {
      VirtualDisk.FlatVer2BackingInfo vmdkBacking =
            new VirtualDiskImpl.FlatVer2BackingInfoImpl();
      vmdkBacking.setFileName(vmdkPath);
      vmdkBacking.setDiskMode(diskMode.toString());
      if (parentBacking != null) {
         vmdkBacking.setParent(parentBacking);
      }
      if (thinDisk != null) {
         vmdkBacking.setThinProvisioned(thinDisk);
      }
      if (eagerlyScrub != null) {
         vmdkBacking.setEagerlyScrub(eagerlyScrub);
      }
      return vmdkBacking;
   }

   /**
    * Create a virtual disk backing object. The disk will be under the VM
    * directory on the specified datastore.
    *
    * @param vm
    *           virtual machine
    * @param ds
    *           datastore (null if using the default VM datastore.)
    * @param diskName
    *           virtual disk file name
    * @param diskMode
    *           virtual disk access mode
    * @param parentBacking
    *           indicates creation of a child delta disk attached to this parent
    *           backing.
    * @return backing object
    */
   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         VcVirtualMachine vm, VcDatastore ds, String diskName,
         DiskMode diskMode, VirtualDevice.BackingInfo parentBacking) {
      String vmdkPath = VcFileManager.getDsPath(vm, ds, diskName);
      return createVmdkBackingInfo(vmdkPath, diskMode,
            (VirtualDisk.FlatVer2BackingInfo) parentBacking, null, null);
   }

   /**
    * Create a virtual disk backing object. The disk will be under the VM
    * directory on the specified datastore.
    *
    * @param vm
    *           virtual machine
    * @param ds
    *           datastore (null if using the default VM datastore.)
    * @param diskName
    *           virtual disk file name
    * @param diskMode
    *           virtual disk access mode
    * @return backing object
    */
   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         VcVirtualMachine vm, VcDatastore ds, String diskName, DiskMode diskMode, Boolean thin, Boolean eagerlyScrub) {
      String vmdkPath = VcFileManager.getDsPath(vm, ds, diskName);
      return createVmdkBackingInfo(vmdkPath, diskMode, null, thin, eagerlyScrub);
   }

   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         VcVirtualMachine vm, VcDatastore ds, DiskType diskType) {
      String vmdkPath = VcFileManager.getDsPath(vm, ds, diskType.getDiskName());
      return createVmdkBackingInfo(vmdkPath, diskType.getDiskMode(), null,
            diskType.isThinDisk(), null);
   }

   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         VcVirtualMachine vm, VcDatastore ds, String diskName,
         DiskMode diskMode, Boolean thinDisk) {
      String vmdkPath = VcFileManager.getDsPath(vm, ds, diskName);
      return createVmdkBackingInfo(vmdkPath, diskMode, null, thinDisk, null);
   }

   public static VirtualDevice.BackingInfo createVmdkBackingInfo(
         VcVirtualMachine vm, String diskName, DiskMode diskMode) {
      return createVmdkBackingInfo(vm, null, diskName, diskMode, false);
   }

   /**
    * @return the full Datastore path of a virtual disk.
    */
   public static String getVmdkPath(VirtualDisk vmdk) {
      VirtualDisk.FileBackingInfo backing =
            (VirtualDisk.FileBackingInfo) vmdk.getBacking();
      return backing.getFileName();
   }

   /**
    * Create a ISO backing object.
    *
    * @param isoPath
    *           pathname to the ISO image file.
    * @return backing object
    */
   public static VirtualDevice.BackingInfo createCdromBackingInfo(String isoPath) {
      return new VirtualCdromImpl.IsoBackingInfoImpl(isoPath, null, null);
   }

   /**
    * Set a new backing object for the virtual device. The state of the device
    * will be "startConnected" and "connected".
    *
    * @param device
    *           the virtual device device
    * @param backing
    *           the backing object
    */
   public static void setVirtualDeviceBacking(VirtualDevice device,
         VirtualDevice.BackingInfo backing) {
      VirtualDevice.ConnectInfo connectInfo =
            new VirtualDeviceImpl.ConnectInfoImpl();
      connectInfo.setConnected(true);
      connectInfo.setStartConnected(true);
      device.setBacking(backing);
      device.setConnectable(connectInfo);
   }

   /**
    * Create a virtual disk object.
    *
    * @param controller
    *           virtual adapter to add the virtual disk
    * @param unitNum
    *           slot number under the adapter
    * @param backing
    *           backing of the virtual device
    * @param size
    *           size of the disk
    * @return virtual disk object
    */
   public static VirtualDisk createVirtualDisk(VirtualDevice controller,
         int unitNum, VirtualDevice.BackingInfo backing, DiskSize size) {
      AuAssert.check(controller instanceof VirtualController);

      VirtualDisk vmdk = new VirtualDiskImpl();
      vmdk.setUnitNumber(unitNum);
      vmdk.setControllerKey(controller.getKey());
      if (size != null) {
         vmdk.setCapacityInKB(size.getKiB());
      }
      setVirtualDeviceBacking(vmdk, backing);
      return vmdk;
   }

   /**
    * Create a VM configuration specification. The result {\link ConfigSpec} can
    * be passed to {\link VcVirtualMachine.clone()} to clone a VM, or {\link
    * VcVirtualMachine.reconfigure()} to reconfigure the VM.
    *
    * @param deviceChange
    *           a list of change descriptions
    * @return configuration specification
    * @throws Exception
    */
   public static ConfigSpec createConfigSpec(
         List<VirtualDeviceSpec> deviceChange) throws Exception {
      ConfigSpec config = new ConfigSpecImpl();
      config.setDeviceChange(deviceChange
            .toArray(new VirtualDeviceSpec[deviceChange.size()]));
      return config;
   }

   public static ConfigSpec createConfigSpec(VirtualDeviceSpec... deviceChanges)
         throws Exception {
      ConfigSpec config = new ConfigSpecImpl();
      config.setDeviceChange(deviceChanges);
      return config;
   }

   /**
    * Wrapper around ConfigSpecImpl.setMemoryMB that also adjusts the maximum
    * allowed balloon size, to keep it proportional to the memory size
    *
    * @param spec
    *           -- vm spec
    * @param memSize
    *           -- memory size in MB
    */
   public static void setMemoryAndBalloon(ConfigSpec spec, Long memSize) {
      spec.setMemoryMB(memSize);
      Long maxBalloon = memSize * 75 / 100;
      OptionValue balloonOption =
            new OptionValueImpl("sched.mem.maxmemctl", maxBalloon.toString());
      spec.setExtraConfig(new OptionValue[] { balloonOption });
   }

   /**
    *
    * @param spec
    *           -- vm spec
    * @param level
    *           -- latency sensitivity's level
    */
   public static void setLatencySensitivity(ConfigSpec spec, LatencyPriority level) {
      OptionValue balloonOption =
            new OptionValueImpl("sched.cpu.latencySensitivity", level.name());
      spec.setExtraConfig(new OptionValue[] { balloonOption });
   }

   /**
    * Get debug information of a virtual device
    *
    * @param device
    * @return info
    */
   public static String getVirtualDeviceInfo(VirtualDevice device) {
      String type = device.getClass().getSimpleName();
      if (device instanceof VirtualController) {
         VirtualController controller = (VirtualController) device;
         return device.getDeviceInfo().getLabel() + ":" + type + " bus="
               + controller.getBusNumber() + ",key=" + device.getKey();
      } else {
         String backingInfo = null;
         VirtualDevice.BackingInfo backing = device.getBacking();
         if (backing instanceof VirtualDisk.FileBackingInfo) {
            if (device instanceof VirtualDisk) {
               backingInfo =
                     ((VirtualDisk.FileBackingInfo) backing).getFileName();
               backingInfo +=
                     "(" + ((VirtualDisk) device).getCapacityInKB() + "KB)";
            }
         } else if (backing instanceof VirtualEthernetCard.NetworkBackingInfo) {
            backingInfo =
                  ((VirtualEthernetCard.NetworkBackingInfo) backing)
                        .getDeviceName();
         } else if (backing != null) {
            backingInfo = backing.toString();
         }
         return device.getDeviceInfo().getLabel() + ":" + type + " unit="
               + device.getUnitNumber() + ",key=" + device.getKey()
               + ",controllerKey=" + device.getControllerKey() + ",backing="
               + backingInfo;
      }
   }

   public static void addManagedByToConfigSpec(ConfigSpec spec, String owner,
         String type) {
      ManagedByInfo managedBy = new ManagedByInfoImpl();
      managedBy.setExtensionKey(owner);
      managedBy.setType(type);

      spec.setManagedBy(managedBy);
   }

   /**
    * Set VMX extra config value encoded in JSON.
    *
    * @param optionKey
    *           unique name of the config value
    * @param value
    *           object representing the config value
    */
   public static void setExtraConfigInSpec(ConfigSpec spec, String optionKey,
         Object value) {
      String jsonString = (new Gson()).toJson(value);
      OptionValue[] extraConfig = new OptionValueImpl[1];
      extraConfig[0] = new OptionValueImpl(optionKey, jsonString);
      spec.setExtraConfig(extraConfig);
   }

   public static void copyAttachDisk(VcVirtualMachine dstVcVm,
         VcVirtualMachine srcVcVm, VcSnapshot srcVcSnap, DeviceId srcDiskId,
         DeviceId dstDiskId, String diskName, DiskMode diskMode,
         VcDatastore ds, String tmpSnapName, String tmpSnapDescription,
         boolean isChangeTrackingEnabled, boolean fullCopy) throws Exception {
      VcSnapshot tmpSnap = null;

      if (srcVcSnap == null) {
         /*
          * Testing shows that a child delta disk can only be attached to a
          * disk that belongs to a snapshot, so create a temporary snapshot.
          */
         // XXX assert srcVcVm.isPoweredOff(); This assert sometimes fails due to a race.
         srcVcSnap = srcVcVm.createSnapshot(tmpSnapName, tmpSnapDescription);
         tmpSnap = srcVcSnap;
      }

      dstVcVm.attachChildDisk(dstDiskId, srcVcSnap, srcDiskId, ds, diskName,
            diskMode);
      if (fullCopy) {
         dstVcVm.promoteDisk(dstDiskId);
         /*
          * See PR 823579 for an explanation of the following hack.
          * To work-around an ESX-side bug, we need to delete the tracker file
          * per disk link used by ESX for changed block tracking (CBT) and then
          * re-create it.
          *
          * For a VM which already has CBT enabled (eg. a DBVM), disabling and
          * then re-enabling CBT achieves the above. However this does not work
          * for a VM that does not have CBT enabled (eg. an external backup VM).
          * For such a VM, an operation like changing the disk mode to any
          * other mode and back, achieves the above.
          */
         if (isChangeTrackingEnabled) {
            dstVcVm.setRequestedChangeTracking(false);
         }
         AuAssert.check(!DiskMode.independent_persistent.equals(diskMode));
         dstVcVm.editVirtualDisk(dstDiskId, DiskMode.independent_persistent);
         if (isChangeTrackingEnabled) {
            dstVcVm.setRequestedChangeTracking(true);
         }
         dstVcVm.editVirtualDisk(dstDiskId, diskMode);
      }
      if (tmpSnap != null) {
         tmpSnap.remove();
      }
   }

   public static ConfigSpec createEnableDisableHotAddConfigSpec(
         boolean enableHotAddCpu, boolean enableHotAddMemory) {
      ConfigSpec cfgSpec = new ConfigSpecImpl();
      OptionValue hotAddMemory =
            new OptionValueImpl(HOT_ADD_MEMORY_KEY, String.valueOf(
                  enableHotAddMemory).toUpperCase());
      OptionValue hotAddCpu =
            new OptionValueImpl(HOT_ADD_CPU_KEY, String
                  .valueOf(enableHotAddCpu).toUpperCase());
      cfgSpec.setExtraConfig(new OptionValue[] { hotAddMemory, hotAddCpu });
      return cfgSpec;
   }

   /**
    * Create virtual device object for a CD-ROM/DVD-ROM device.
    *
    * @param backing
    *           the backing info for the new device
    * @return cdrom the device spec
    */
   public static VirtualDevice createCdromDevice(
         VirtualDevice.BackingInfo backing) {
      VirtualCdromImpl cdrom = new VirtualCdromImpl();
      setVirtualDeviceBacking(cdrom, backing);
      return cdrom;
   }

   public static VirtualDevice.BackingInfo createFloppyBackingInfo(
         String flpPath) {
      return new VirtualFloppyImpl.ImageBackingInfoImpl(flpPath, null, null);
   }

   public static VirtualDevice createFloppyDevice(
         VirtualDevice.BackingInfo backing) {
      VirtualFloppyImpl floppy = new VirtualFloppyImpl();
      setVirtualDeviceBacking(floppy, backing);
      return floppy;
   }

   /**
    * Support workaround for PR 904771. Detach disk (FileOperation.destroy not
    * set) is not enabled in vSphere 5.1 if snapshot exists. If this detach disk
    * is disabled, we will use destroy disk to replace detach disk operation.
    */
   private static final boolean DETACH_DISK_IS_ENABLED = false;
   private static final String DETACH_DISK_IS_ENABLED_KEY =
         "vc.detachDiskEnabled";

   public static boolean isDetachDiskEnabled() {
      return Configuration.getBoolean(DETACH_DISK_IS_ENABLED_KEY,
            DETACH_DISK_IS_ENABLED);
   }

   /**
    * Define whether use single VM operation solution in the Sagas such as
    * repair/upgrade. Should change this setting according to work flow
    * requirement. In vSphere 5.1, use this flag as a workaround. As destroy
    * independent disks is not enabled if snapshot exist. Single VM operation
    * solutions cannot work. So do not use single VM operation by default.
    */
   private static final boolean SINGLE_VM_OPS_IS_ENABLED = false;
   private static final String SINGLE_VM_OPS_IS_ENABLED_KEY =
         "cms.singleVMOpsEnabled";

   public static boolean isSingleVMOperationEnabled() {
      return Configuration.getBoolean(SINGLE_VM_OPS_IS_ENABLED_KEY,
            SINGLE_VM_OPS_IS_ENABLED);
   }
}
