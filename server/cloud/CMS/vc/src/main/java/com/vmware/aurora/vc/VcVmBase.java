/************************************************************
 * Copyright (c) 2011-2012 VMware, Inc.  All rights reserved.
 ************************************************************/
package com.vmware.aurora.vc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.vmware.aurora.util.AuAssert;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualController;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.BackingInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vmodl.ManagedObject;

public interface VcVmBase extends VcObject {
   VirtualDevice getVirtualDevice(DeviceId srcDeviceId);

   List<DeviceId> getVirtualDiskIds();

   void dumpDevices();

   VirtualController getVirtualController(DeviceId deviceId);

   String getDiskChangeId(DeviceId deviceId);

   abstract VcDatacenter getDatacenter();
}


/**
 * Abstract class for shared resources and operations
 * on a VM or a VM snapshot.
 */
@SuppressWarnings("serial")
abstract class VcVmBaseImpl extends VcObjectImpl implements VcVmBase {
   protected VcVmBaseImpl(ManagedObject mo) {
      super(mo);
   }

   abstract protected ConfigInfo getConfig();

   public abstract VcDatacenter getDatacenter();

   protected VirtualDevice[] getDevice() {
      return getConfig().getHardware().getDevice();
   }

   /**
    * Get a list of virtual disks attached to the VM.
    *
    * @return a list of disk IDs.
    */
   @Override
   public List<DeviceId> getVirtualDiskIds() {
      VirtualDevice[] devices = getDevice();
      HashMap<Integer, VirtualController> controllers =
         new HashMap<Integer, VirtualController>();
      List<DeviceId> diskIds = new ArrayList<DeviceId>();
      // Find all valid controllers.
      for (VirtualDevice device : devices) {
         if (DeviceId.isSupportedController(device)) {
            controllers.put(device.getKey(), (VirtualController)device);
         }
      }
      // Find all valid disk devices.
      for (VirtualDevice device : devices) {
         if (device instanceof VirtualDisk) {
             VirtualController controller = controllers.get(device.getControllerKey());
             if (controller != null) {
                diskIds.add(new DeviceId(controller, device));
             }
         }
      }
      return diskIds;
   }

   @Override
   public void dumpDevices() {
      for (VirtualDevice dev : getDevice()) {
         logger.info(VmConfigUtil.getVirtualDeviceInfo(dev));
      }
   }

   /*
    * Lookup a virtual controller device in the VM.
    */
   protected static VirtualController
   findVirtualController(VirtualDevice[] devices, DeviceId id) {
      Class<?> controllerType = id.getTypeClass();
      for (VirtualDevice device : devices) {
         if (controllerType.isInstance(device) &&
               ((VirtualController)device).getBusNumber() == id.busNum) {
            return (VirtualController)device;
         }
      }
      return null;
   }

   /*
    * Lookup an end-point virtual device in the VM.
    */
   protected static VirtualDevice
   findVirtualDevice(VirtualDevice[] devices, DeviceId id) {
      AuAssert.check(id.unitNum != null);
      VirtualDevice controller = findVirtualController(devices, id);
      if (controller == null) {
         return null;
      }
      Integer key = controller.getKey();
      for (VirtualDevice device : devices) {
         if (key.equals(device.getControllerKey()) &&
             id.unitNum.equals(device.getUnitNumber())) {
            return device;
         }
      }
      return null;
   }

   /**
    * Get an end-point virtual device.
    * @param deviceId unique virtual device location id
    * @return the end-point virtual disk device
    */
   @Override
   public VirtualDevice getVirtualDevice(DeviceId deviceId) {
      return findVirtualDevice(getDevice(), deviceId);
   }

   /**
    * Get a virtual controller device.
    * @param devicdId unique virtual disk or controller location id
    * @return the virtual controller device
    */
   @Override
   public VirtualController getVirtualController(DeviceId deviceId) {
      return findVirtualController(getDevice(), deviceId);
   }

   /**
    * Get a virtual device by device label.
    * @param label virtual device label
    * @return virtual device, null if not found
    */
   protected VirtualDevice getDeviceByLabel(String label) {
      VirtualDevice target = null;
      for (VirtualDevice device : getDevice()) {
         if (device.getDeviceInfo().getLabel().equalsIgnoreCase(label)) {
            target = device;
            break;
         }
      }
      return target;
   }

   /**
    * Get the change id for the specified disk. This is useful for Changed Block
    * Tracking.
    * @param deviceId id for the disk
    * @return the disk change id string.
    */
   public String getDiskChangeId(DeviceId deviceId) {
      VirtualDevice dev = getVirtualDevice(deviceId);
      BackingInfo backing = dev.getBacking();
      if (backing instanceof VirtualDisk.FlatVer2BackingInfo) {
         return ((VirtualDisk.FlatVer2BackingInfo) backing).getChangeId();
      } else if (backing instanceof VirtualDisk.SparseVer2BackingInfo) {
         return ((VirtualDisk.SparseVer2BackingInfo) backing).getChangeId();
      } else if (backing instanceof VirtualDisk.SeSparseBackingInfo) {
         return ((VirtualDisk.SeSparseBackingInfo) backing).getChangeId();
      } else {
         return null;
      }
   }
}
