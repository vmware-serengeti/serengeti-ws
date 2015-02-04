/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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


import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.internal.Pair;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.aurora.exception.AuroraException;
import com.vmware.aurora.exception.GuestVariableException;
import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.aurora.vc.VcClusterConfig.VmHAConfig;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcPseudoTaskBody;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.VmConfigUtil.ScsiControllerType;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.IVcEventHandler;
import com.vmware.aurora.vc.vcevent.VcEventHandlers.VcEventType;
import com.vmware.aurora.vc.vcevent.VcEventListener;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcLongCallHandler;
import com.vmware.vim.binding.impl.vim.cluster.ConfigSpecExImpl;
import com.vmware.vim.binding.impl.vim.cluster.DasVmConfigInfoImpl;
import com.vmware.vim.binding.impl.vim.cluster.DasVmConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.cluster.DasVmSettingsImpl;
import com.vmware.vim.binding.impl.vim.cluster.DrsVmConfigInfoImpl;
import com.vmware.vim.binding.impl.vim.cluster.DrsVmConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.option.OptionValueImpl;
import com.vmware.vim.binding.impl.vim.vm.CloneSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.ConfigSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.CreateChildSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.RelocateSpecImpl;
import com.vmware.vim.binding.impl.vim.vm.device.VirtualDeviceSpecImpl;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualDiskManager.VirtualDiskSpec;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.VirtualMachine.ConnectionState;
import com.vmware.vim.binding.vim.VirtualMachine.DiskChangeInfo;
import com.vmware.vim.binding.vim.VirtualMachine.FaultToleranceState;
import com.vmware.vim.binding.vim.VirtualMachine.PowerState;
import com.vmware.vim.binding.vim.cluster.DasConfigInfo.VmMonitoringState;
import com.vmware.vim.binding.vim.cluster.DasVmConfigInfo;
import com.vmware.vim.binding.vim.cluster.DasVmConfigSpec;
import com.vmware.vim.binding.vim.cluster.DasVmSettings;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.IsolationResponse;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority;
import com.vmware.vim.binding.vim.cluster.DrsVmConfigInfo;
import com.vmware.vim.binding.vim.cluster.DrsVmConfigSpec;
import com.vmware.vim.binding.vim.cluster.VmToolsMonitoringSettings;
import com.vmware.vim.binding.vim.event.Event;
import com.vmware.vim.binding.vim.event.VmEvent;
import com.vmware.vim.binding.vim.ext.ManagedByInfo;
import com.vmware.vim.binding.vim.fault.FileNotFound;
import com.vmware.vim.binding.vim.fault.InvalidPowerState;
import com.vmware.vim.binding.vim.fault.InvalidState;
import com.vmware.vim.binding.vim.fault.ToolsUnavailable;
import com.vmware.vim.binding.vim.net.IpConfigInfo.IpAddress;
import com.vmware.vim.binding.vim.net.IpConfigInfo.IpAddressStatus;
import com.vmware.vim.binding.vim.option.ArrayUpdateSpec.Operation;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vim.vApp.VmConfigInfo;
import com.vmware.vim.binding.vim.vm.*;
import com.vmware.vim.binding.vim.vm.GuestInfo.NicInfo;
import com.vmware.vim.binding.vim.vm.Summary.StorageSummary;
import com.vmware.vim.binding.vim.vm.device.VirtualCdrom;
import com.vmware.vim.binding.vim.vm.device.VirtualController;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.BackingInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public interface VcVirtualMachine extends VcVmBase {
   /**
    * Default short and long time-outs to help our callers:
    * - short timeout
    *   for use when the database is known to have been previously shut down cleanly
    *   and we will not go into crash recovery, mkfs, etc, upon boot.
    * - long timeout
    *   a conservative timeout when we know nothing about the current state of the
    *   vm. Used as a last resort to prevent permanent leakage of CMS threads on DBVM
    *   hangs of any kind.
    */
   public static final int SHORT_POWER_ON_TIMEOUT_SECS = 10 * 60;
   public static final int LONG_POWER_ON_TIMEOUT_SECS = 2 * 60 * 60;

   /**
    * Sometimes VC's state for VM can be out of sync with
    * what is reported by events and exceptions. As the latter may
    * be generated as a result of hostd state.
    *
    * We use the following timeout to wait for VC to sync its state
    * when we detect such occasion.
    */
   public static final int WAIT_FOR_VC_STATE_TIMEOUT_SECS = 60;

   /**
    * This enum defines the collection of keys that stored as key-value map in the
    * VM's configuration file (vmx file). Use VcVirtualMachine.getDbvmConfig() and
    * VcVirtualMachine.setDbvmConfig() to access the key-value map.
    */
   public enum DbvmConfigKey {
      Dbvm_Description("vm.desc"),
      Import_Error("vm.import_error");

      private String key;
      private DbvmConfigKey(String key) {
         this.key = key;
      }

      public String getKey() {
         return key;
      }
   }

   @SuppressWarnings("serial")
   public class GuestVarReturnCode implements Serializable {
      /* These mirror definitions in agent/src/main/scripts/interface.sh */
      int GUEST_V_DONE = 1; // All is well.
      int GUEST_V_DOING = 0; // In-progress.
      private int code = 0;
      private String statusMsg;
      private String privateIP;
      private Map<String, String> guestVariables = null;

      GuestVarReturnCode(Map<String, String> guestVariables) {
         this.guestVariables = guestVariables;
         String retCode = guestVariables.get("guestinfo.return_code");
         code = Integer.parseInt(retCode);
         if (code == GUEST_V_DONE) {
            statusMsg = "Success";
            privateIP = guestVariables.get("guestinfo.dbvm_private_ip");
         } else if (code == GUEST_V_DOING) {
            statusMsg = "Busy";
         } else {
            AuAssert.check(code < 0);
            statusMsg = guestVariables.get("guestinfo.error_message");
            if (statusMsg == null) {
               statusMsg = "Error"; // Unknown error?
            }
         }
         statusMsg = statusMsg + " (" + code + ")";
      }

      public boolean isSuccess() {
         return code == GUEST_V_DONE;
      }

      public boolean isBusy() {
         return code == GUEST_V_DOING;
      }

      public boolean isError() {
         return code < 0;
      }

      public String getStatusMsg() {
         return statusMsg;
      }

      public String getPrivateIP() {
         return privateIP;
      }

      public Map<String, String> getGuestVariables() {
         return guestVariables;
      }
   }

   /**
    * Configuration specification for creating a new virtual machine.
    */
   static public class CreateSpec {
      final String name;
      /**
       * Configuration of VM snapshot from which to clone.
       */
      final VcSnapshot parentSnap;
      /**
       * Resource pool to deploy the new VM.
       */
      final VcResourcePool rp;
      /**
       * Datastore to deploy the new VM.
       */
      final VcDatastore ds;
      /**
       * True if this is a link clone.
       */
//      final boolean linkClone;

      final VcVmCloneType cloneType;

      final boolean persisted;

      /**
       * More VM configuration changes.
       */
      final ConfigSpec spec;
      /**
       * VM folder to hold the new VM,
       * if null, the VM will be put in the root VM folder for the datacenter,
       * in which the resource pool lives.
       */
      final Folder folder;
      /**
       * the vc host the new vm be placed
       */
      final VcHost host;
      /**
       * Create a clone from a VM snapshot.
       */
      public CreateSpec(String name, VcSnapshot parent, VcResourcePool rp, VcDatastore ds,
                        VcVmCloneType cloneType, boolean persisted, ConfigSpec spec) {
         this(name, parent, rp, ds, null, null, cloneType, persisted, spec);
      }

      /**
       * Create a clone from a VM snapshot.
       */
      public CreateSpec(String name, VcSnapshot parent, VcResourcePool rp, VcDatastore ds, Folder folder,
            VcHost host, VcVmCloneType cloneType, boolean persisted, ConfigSpec spec) {
         AuAssert.check(cloneType != null, "please specify the clone type!");

         if(cloneType != VcVmCloneType.VMFORK
               || (cloneType == VcVmCloneType.VMFORK && persisted)) {
            AuAssert.check(parent != null);
         }

         parentSnap = parent;
         this.name = name;
         this.rp = rp;
         this.ds = ds;
         this.folder = folder;
         this.host = host;

         this.cloneType = cloneType;
         this.persisted = persisted;


         this.spec = spec;
      }

      /**
       * @return the corresponding {@link VcSnapshot} object that contains the VM Config.
       */
      VcSnapshot getParentSnapshot() {
         return parentSnap;
      }

      /**
       * @return the parent {@link VcVirtualMachine} VM object .
       */
      VcVirtualMachine getParentVm() {
            return parentSnap.getVm();
      }
   }

   public class DiskCreateSpec {
      DeviceId deviceId;
      VcDatastore ds = null;
      String diskName;
      DiskMode diskMode;
      DiskSize size = null;
      AllocationType allocationType;

      public DiskCreateSpec(DeviceId deviceId, VcDatastore ds, String diskName,
                 DiskMode diskMode, DiskSize size, AllocationType allocationType) {
         this.deviceId = deviceId;
         this.ds = ds;
         this.diskName = diskName;
         this.diskMode = diskMode;
         this.size = size;
         this.allocationType = allocationType;
         AuAssert.check(deviceId != null);
      }

      public DiskCreateSpec(DeviceId deviceId, VcDatastore ds, String diskName,
            DiskMode diskMode, DiskSize size) {
         this(deviceId, ds, diskName, diskMode, size, AllocationType.THIN);
      }

      VirtualDeviceSpec getVcSpec(VcVirtualMachine vm) throws Exception {
         Boolean thinDisk = null;
         Boolean eagerlyScrub = null;
         if (allocationType != null) {
            switch (allocationType) {
            case THIN:
               thinDisk = Boolean.TRUE;
               break;
            case ZEROEDTHICK:
               eagerlyScrub = Boolean.TRUE;
            case THICK:
               thinDisk = Boolean.FALSE;
               break;
            }
         }
         BackingInfo backing = VmConfigUtil.createVmdkBackingInfo(vm, ds, diskName, diskMode, thinDisk, eagerlyScrub);
         return vm.attachVirtualDiskSpec(deviceId, backing, true, size);
      }

      public String toString() {
         return new ToStringBuilder("DiskCreateSpec")
               .append("name",diskName)
               .append("id",deviceId)
               .append("size",size)
               .append("ds",ds.getName())
               .toString();
      }
   }

   /**
    * @return VM config info
    */
   abstract ConfigInfo getConfig();

   /**
    * @return VM vApp config info
    */
   abstract VmConfigInfo getVAppConfig();

   /**
    * @return display name of the VM
    */
   abstract String getName();

   /**
    * Rename the VM.
    *
    * @param newName
    *           The new name of the VM.
    */
   abstract void rename(String newName) throws Exception;

   /**
    * @return name already in URL format as returned from VLSI
    */
   abstract String getURLName();

   /**
    * @return default pathName of the VM (location of vmx file)
    */
   abstract String getPathName();

   /**
    * @return if this is a template VM
    */
   abstract boolean isTemplate();

   /**
    *
    * @return the current host that the VM is on
    */
   abstract VcHost getHost();

   /**
    * @return the datacenter of the current VM.
    * XXX: needs to have an even better way of getting at the datacenter.
    */
   abstract VcDatacenter getDatacenter();

   /**
    * @return the datastores of the current VM.
    */
   abstract VcDatastore[] getDatastores();

   /**
    * Get the default datastore of the current VM at index 0.
    * @return the default datastore of the current VM.
    */
   abstract VcDatastore getDefaultDatastore();

   abstract String getInfo();

   /**
    * Refresh the parent resource pool of this VM.
    */
   abstract void refreshRP();

   /**
    * Detaches a virtual disk and optionally destroys (deletes) it.
    *
    * @param deviceId
    *           device location identity
    * @param destroyDisk
    *           whether to destroy the disk
    * @throws Exception
    */
   abstract void detachVirtualDisk(DeviceId deviceId, boolean destroyDisk)
         throws Exception;

   /**
    * Detaches a virtual disk if it exists and optionally destroys (deletes) it.
    *
    * @param deviceId
    *           device location identity
    * @param destroyDisk
    *           whether to destroy the disk
    * @throws Exception
    */
   abstract void detachVirtualDiskIfExists(DeviceId deviceId, boolean destroyDisk)
         throws Exception;

   /**
    * Attach a virtual disk
    * @param deviceId device location identity
    * @param backing virtual disk backing
    * @param createDisk whether to create new disk
    * @param size disk size
    * @throws Exception
    */
   abstract void attachVirtualDisk(DeviceId deviceId,
         VirtualDevice.BackingInfo backing, boolean createDisk, DiskSize size)
         throws Exception;

   /**
    * Attach a virtual disk
    * @param deviceId device location identity
    * @param backing virtual disk backing
    * @param createDisk whether to create new disk
    * @param size disk size
    * @exception Exception
    */
   abstract VirtualDeviceSpec attachVirtualDiskSpec(DeviceId deviceId,
         VirtualDevice.BackingInfo backing, boolean createDisk, DiskSize size) throws Exception;


   /**
    * Attach the copy of a virtual disk from a source VM.
    * @param deviceId   virtual disk location identity
    * @param srcVm      source VM (may be the same VM)
    * @param srcDeviceId  source virtual disk location identity
    * @param dstDs         datastore for the new disk (null means default VM datastore)
    * @param diskName   new disk name
    * @param diskMode   attachment mode of the new disk
    * @throws Exception
    */
   abstract void copyAttachVirtualDisk(DeviceId deviceId, VcVmBase srcVm,
         DeviceId srcDeviceId, VcDatastore dstDs, String diskName,
         DiskMode diskMode) throws Exception;

   abstract void copyAttachVirtualDisk(DeviceId deviceId, VcVmBase srcVm,
         DeviceId srcDeviceId, String diskName, DiskMode diskMode)
         throws Exception;

   /**
    * Attach a child disk with its parent disk determined by the
    * <code>srcVm</code> and <code>srcDeviceId</code>.
    *
    * 1. dstDs != null && atRootOfDs == true
    *    The child is created at the root of <code>dstDs</code> with
    *    "<vm-name>-<diskName>" as the unique disk name.
    * 2. dstDs != null && atRootOfDs == false
    *    The child is created at "[dstDs] <destVmDirPath>".
    * 3, dstDs == null
    *    The child is created at "<actual destVmDir>".
    *
    * @param deviceId
    *           destination virtual disk location identity
    * @param srcSnap
    *           source VM (may be the same VM)
    * @param srcDeviceId
    *           source virtual disk location identity
    * @param diskMode
    *           attachment mode of the new disk
    * @throws Exception
    */
   abstract void attachChildDiskPath(DeviceId deviceId, VcSnapshot srcSnap,
         DeviceId srcDeviceId, String diskPath,
         DiskMode diskMode) throws Exception;

   abstract void attachChildDisk(DeviceId deviceId, VcSnapshot srcSnap,
         DeviceId srcDeviceId, VcDatastore dstDs, String diskName,
         DiskMode diskMode) throws Exception;

   abstract void attachChildDisk(DeviceId deviceId, VcSnapshot srcSnap,
         DeviceId srcDeviceId, String diskName, DiskMode diskMode)
         throws Exception;

   /**
    * Unlinks (breaks sharing) of any shared backings in the disk chain, copies
    * the now-unshared backings over and consolidates the links in the chain.
    *
    * @param deviceId
    *           disk to consolidate.
    * @throws Exception
    */
   abstract void promoteDisk(DeviceId deviceId) throws Exception;

   /**
    * Extend the provisioned capacity of a virtual disk.
    * This can be done when the VM is power on or off.
    * @param deviceId
    * @throws Exception
    */
   abstract void extendVirtualDisk(DeviceId deviceId, DiskSize size)
         throws Exception;

   /**
    * Updates the disk mode of the specified disk.
    *
    * @throws Exception
    *            if the task fails for any reason.
    */
   abstract void editVirtualDisk(DeviceId deviceId, DiskMode newMode)
         throws Exception;

   /**
    * Updates the disk mode of the specified disk.
    *
    * @throws Exception
    *            if the task fails for any reason.
    */
   abstract VirtualDeviceSpec editVirtualDiskSpec(DeviceId deviceId, DiskMode newMode)
         throws Exception;

   /**
    * Determines whether a device is a base disk i.e. a disk without any parents
    * in this VM's disk tree.
    *
    * Note that this is an Aurora-specific method. It assumes that a base disk
    * can only be of VMFS-flat disk type. Native snapshots should fall into this
    * type too. In vSphere an RDM or an SeSparse disk are perfectly legal
    * candidates for a base disk; but for Aurora they are not.
    *
    * @param deviceId
    *           id of the device to be checked.
    * @return true if the device is a base disk (i.e. no children). false if it
    *         is not.
    */
   abstract boolean isBaseDisk(DeviceId deviceId);

   /**
    * Returns the capacity of the virtual disk.
    *
    * @param deviceId
    * @return The disk capacity or null if the device is not a virtual disk.
    */
   abstract DiskSize getDiskCapacity(DeviceId deviceId);

   /**
    * Returns the datastore of the virtual disk.
    *
    * @param deviceId
    * @return The datastore object holding the virtual disk
    */
   abstract VcDatastore getDiskDatastore(DeviceId deviceId);

   /**
    * Determines whether the specified disk is attached to this VM.
    * @param deviceId
    * @return
    */
   abstract boolean isDiskAttached(DeviceId deviceId);

   /**
    * Gets a list of areas of a virtual disk belonging to this VM that have
    * been modified since a well-defined point in the past. The beginning of
    * the change interval is identified by <code>diskChangeId</code>, while
    * the end of the change interval is implied by <code>endMarkerSnapshot</code>.
    *
    * @param endMarkerSnapshot
    * @param deviceId
    * @param startOffset
    * @param diskChangeId
    * @return
    */
   abstract DiskChangeInfo queryChangedDiskAreas(VcSnapshot endMarkerSnapshot,
         DeviceId deviceId, long startOffset, String diskChangeId) throws Exception;

   /**
    * Mount an ISO image onto a Cdrom device.
    * @param deviceId location identity of the virtual device
    * @param backing backing of the virtual device (must be an iso image)
    * @return virtual device change spec
    */
   abstract VirtualDeviceSpec mountISO(DeviceId deviceId,
         VirtualDevice.BackingInfo backing) throws Exception;

   /**
    * Change the network of a NIC.
    * @param label NIC location identity
    * @param network virtual network
    * @return virtual device change spec
    * @throws Exception
    */
   abstract VirtualDeviceSpec reconfigNetworkSpec(String label,
         VcNetwork network) throws Exception;

   /**
    * Returns true if VM is currently powered on.
    * If the VM power state is unknown or inconsistent, return false.
    * @return true if VM is in consistent powered on state
    */
   abstract boolean isPoweredOn();

   /**
    * Returns true if the VM is currently powered off.
    * If the VM power state is unknown or inconsistent, return false.
    * @return true if VM is in consistent powered off state.
    */
   abstract boolean isPoweredOff();

   abstract ConnectionState getConnectionState();
   abstract boolean isConnected();

   abstract FaultToleranceState getFTState();

   /**
    * Power on a VM.
    * @param callback (optional) call-back function for the task
    * @return task for the operation
    * @throws Exception
    */
   abstract VcTask powerOn(final IVcTaskCallback callback) throws Exception;

   abstract boolean powerOn() throws Exception;

   /**
    * Power on a VM
    * @param host The host where the virtual machine is to be powered on, could be null.
    * @param callback
    * @return
    * @throws Exception
    */
   VcTask powerOn(final VcHost host, final IVcTaskCallback callback) throws Exception;

   boolean powerOn(final VcHost host) throws Exception;

   /**
    * Power off a VM.
    * @param callback (optional) call-back function for the task
    * @return task for the operation
    * @throws Exception
    */
   abstract VcTask powerOff(final IVcTaskCallback callback) throws Exception;

   abstract boolean powerOff() throws Exception;

   /**
    * Called by a thread wishing to block until vm is powered off via a mechanism
    * outside of CMS (agent). 2 cases:
    * (1) vm is already powered off, return immediately
    * (2) vm is powered on, wait for VmPowredOffEvent
    *
    * Returns true if the vm is in powered off state upon return.
    *
    * @param timeoutMillis
    * @return true if powered off upon return.
    * @throws Exception
    */
   abstract boolean waitForExternalPowerOff(long timeoutMillis)
         throws Exception;

   /**
    * Wait for power-off initiated within cms via any vim api calls including
    * both proper vc tasks and pseudo-tasks such as vm.shutdownGuest().
    * @param timeoutMillis
    * @return
    * @throws Exception
    */
   abstract boolean waitForPowerOff(long timeoutMillis) throws Exception;

   /**
    * Performs a clean guest shutdown (via tools). Isn't guaranteed
    * to succeed for unhealthy guests.
    * @param timeoutMillis
    * @return true if successful
    * @throws Exception
    */
   abstract boolean shutdownGuest(final long timeoutMillis) throws Exception;

   /**
    * Remove this virtual machine and its files from datastore.
    * Take care of the cases that the VM may be powered on or already deleted.
    * @throws Exception
    */
   abstract void destroy() throws Exception;

   /**
    * Remove this virtual machine and its files from datastore.
    * Take care of the cases that the VM may be powered on or already deleted.
    * @param removeSnapShot true to remove all snapshots manually first.
    *                       false to remove VM directly.
    * @throws Exception
    */
   abstract void destroy(boolean removeSnapShot) throws Exception;

   /**
    * Unregister this virtual machine from VC inventory.
    * @throws Exception
    */
   abstract void unregister() throws Exception;

   /**
    * Relocate a disk
    * @param deviceIds Disks to relocate
    * @param ds Datastore to relocate it to
    * @throws Exception
    */
   abstract void relocateDisks(DeviceId[] deviceIds, VcDatastore ds) throws Exception;

   /**
    * Clone a new VM from a VM template
    * We always create a link-clone.
    * @param name new VM's name
    * @param rp resource pool for the new VM
    * @param ds datastore for the new VM
    * @param config (optional) VM configuration change
    * @param callback (optional) call-back function for the task
    * @return a task object for the clone operation
    * @throws Exception
    */
   abstract VcTask cloneTemplate(String name, VcResourcePool rp,
         VcDatastore ds, ConfigSpec config, IVcTaskCallback callback)
         throws Exception;

   abstract VcVirtualMachine cloneTemplate(String name, VcResourcePool rp,
         VcDatastore ds, ConfigSpec config) throws Exception;

   /**
    * Clone a VM
    * @param name new VM's name
    * @param rp resource pool for the new VM
    * @param ds datastore for the new VM
    * @param folder The VM folder to hold the new VM. If null, the new VM
    * will be put into the root VM folder of the datacenter, in which the resource pool lives
    * @param isLinked true for the linked clone, false for the full clone
    * @param callback (optional) call-back function for the task
    * @return a task object for the clone operation
    * @throws Exception
    */
   abstract VcTask cloneVm(String name, VcResourcePool rp, VcDatastore ds, Folder folder,
                           boolean isLinked, IVcTaskCallback callback) throws Exception;

   /**
    * Clone a VM
    * @param name new VM's name
    * @param rp resource pool for the new VM
    * @param ds datastore for the new VM
    * @param folder The VM folder to hold the new VM. If null, the new VM
    * will be put into the root VM folder of the datacenter, in which the resource pool lives
    * @param isLinked true for the linked clone, false for the full clone
    * @return a task object for the clone operation
    * @throws Exception
    */
   abstract VcVirtualMachine cloneVm(String name, VcResourcePool rp,
         VcDatastore ds, Folder folder, boolean isLinked) throws Exception;

   /**
    * Clone a new VM from a snapshot of the VM
    * @param name new VM's name
    * @param rp resource pool for the new VM
    * @param ds datastore for the new VM
    * @param snap snapshot of the VM to create the clone
    * @param folder The VM folder to hold the new VM. If null, the new VM
    * will be put into the root VM folder of the datacenter, in which the resource pool lives
    * @param linked true for the linked clone, false for the full clone
    * @param config (optional) VM configuration change
    * @param callback (optional) call-back function for the task
    * @return a task object for the clone operation
    * @throws Exception
    */
   abstract VcTask cloneSnapshot(String name, VcResourcePool rp, VcDatastore ds,
         VcSnapshot snap, Folder folder, VcHost host, boolean linked, ConfigSpec config,
         IVcTaskCallback callback) throws Exception;

   /**
    * Clone a new VM from a snapshot of the VM, the blocking version.
    * @return the new VM.
    */
   abstract VcVirtualMachine cloneSnapshot(String name, VcResourcePool rp, VcDatastore ds,
         VcSnapshot snap, Folder folder, VcHost host, boolean linked, ConfigSpec config) throws Exception;
   abstract VcVirtualMachine cloneSnapshot(String name, VcResourcePool rp,
         VcSnapshot snap, Folder folder, VcHost host, boolean linked, ConfigSpec config) throws Exception;

   /**
    * Create a snapshot of the VM
    * @param name snapshot name
    * @param description snapshot description
    * @param callback (optional) call-back function for the task
    * @return a task object for the snapshot operation
    * @throws Exception
    */
   abstract VcTask createSnapshot(final String name, final String description,
         final IVcTaskCallback callback) throws Exception;

   abstract VcSnapshot createSnapshot(final String name,
         final String description) throws Exception;

   /**
    * Find the first snapshot that matches the name.
    * @param name identifier of the snapshot
    * @return the snapshot or null if not found
    */
   abstract VcSnapshot getSnapshotByName(final String name);

   /**
    * Find the snapshot that matches the moref id.
    * @param id id of the snapshot
    * @return the snapshot
    * @throws VcException if not found
    */
   abstract VcSnapshot getSnapshotById(final String id);

   /**
    * @return the current snapshot of the virtual machine
    */
   abstract VcSnapshot getCurrentSnapshot();

   /**
    * @return all the snapshots of the virtual machine.
    */
   abstract List<VcSnapshot> getSnapshots();

   /**
    * @return remove all the snapshots of the virtual machine.
    */
   abstract VcTask removeAllSnapshots(final IVcTaskCallback callback) throws Exception;

   abstract void removeAllSnapshots() throws Exception;

   /**
    * Reconfigure VM settings.
    * @param spec specification of the change
    * @param callback (optional) call-back function for the task
    * @return a task object for the reconfiguration operation
    * @throws Exception
    */
   abstract VcTask reconfigure(final ConfigSpec spec,
         final IVcTaskCallback callback) throws Exception;

   /**
    * Reconfigure VM settings. Blocks until change is complete, returns only success/failure.
    * @param spec specification of the change
    * @return true on success, false on failure
    * @throws Exception
    */
   abstract boolean reconfigure(ConfigSpec spec) throws Exception;

   /**
    * If requested, unlinks (breaks sharing) of any shared backings in the
    * disk chain, copies the now-unshared backings over and consolidates
    * the links in the chain.
    *
    * @param disk
    *           Disk to consolidate.
    * @param callback
    *           (optional) call-back function for the task
    * @return a task object for the reconfiguration operation
    * @throws Exception
    */
   abstract VcTask promoteDisk(final VirtualDisk disk, final boolean unlink,
         final IVcTaskCallback callback) throws Exception;

   abstract void promoteDisk(VirtualDisk disk, boolean unlink) throws Exception;

   /**
    * Promote given virtual disks of the VM. The entire disk chain is copied
    * to the VM directory and consolidated into a single disk.
    *
    * @param diskIds device IDs of the disks
    * @param callback
    * @return a task object for this VC operation.
    * @throws Exception
    */
   public VcTask promoteDisks(DeviceId[] diskIds,
         final IVcTaskCallback callback) throws Exception;

   public void promoteDisks(DeviceId[] diskIds) throws Exception;

   /**
    * Mark the VM as a template.
    * @throws Exception
    */
   abstract void markAsTemplate() throws Exception;

   /**
    * Mark the template to be a normal virtual machine
    * @throws Exception
    */
   abstract void markAsVirtualMachine(VcResourcePool rp, String hostName) throws Exception;

   /**
    * Get updated guest variable values from the VM.
    * XXX The caller runs in a loop to poll for GV changes.
    *     We can do better by blocking on getting events for new updates.
    */
   abstract Map<String, String> getGuestVariables();

   /**
    * @param optionKey identifier of the extra config
    * @return extra config values encoded in Map
    */
   abstract Map<String, String> getExtraConfigMap(String optionKey);

   /**
    * Returns a map of <key, value> pairs for variables set in "machine.id".
    * machine.id is encoded as a Json string, which was created from
    * a Map<String, String>.
    *
    * @return Map of <key, value> pairs. Empty map if no well-formed variables
    *         are found.
    */
   abstract Map<String, String> getGuestConfigs();

   abstract Map<String, String> getDbvmConfig() throws Exception;

   /**
    * Returns value for the specified <code>key</code> variable if it is set
    * in "machine.id".
    *
    * @return Value for <code>key</code>. null if the key is not found.
    */
   abstract String getGuestConfig(String key);

   /**
    * Sends guest variables to vm via "machine.id" mechanism.
    * @param guestVariables
    * @throws Exception
    */
   abstract void setGuestConfigs(Map<String, String> guestVariables)
         throws Exception;

   /**
    * An async version of {@link VcVirtualMachine#setGuestConfigs(Map)}
    * @param guestVariables
    * @throws Exception
    */
   abstract VcTask setGuestConfigs(Map<String, String> guestVariables,
         final IVcTaskCallback callback) throws Exception;

   /**
    * Set VMX extra config values.
    *
    * @param configs Array of config key-value pairs.
    * @throws Exception
    */
   void setExtraConfig(Pair<String, String>[] configs) throws Exception;

   abstract void setDbvmConfig(Map<String, String> config) throws Exception;

   /**
    * @return total storage usage of this VM
    */
   abstract DiskSize getStorageUsage();

   /**
    * @return The committed storage used by this VM
    */
   abstract DiskSize getStorageCommitted();

   /**
    * @return The file layout of the vm
    */
   abstract FileLayoutEx getFileLayout();

   /**
    * @param timeoutMs timeout in milliseconds
    * @param expectNum IP address number expected.
    * @return The list of IP addresses in the VC order.
    */
   abstract List<String> queryIpAddresses(long timeoutMs, int expectNum) throws Exception;

   /**
    * @return The list of IP addresses in the VC order.
    * @throws Exception
    */
   abstract List<String> queryIpAddresses() throws Exception;

   /**
    * @return The resource pool in which the vm runs
    */
   abstract VcResourcePool getResourcePool();

   /**
    * @return The VApp resource pool in which the vm runs
    */
   abstract VcResourcePool getParentVApp();

   /**
    * Migrate this vm to a specified rp and call callback on completion.
    * @param rp    dest rp
    * @param callback   called on completion
    * @return VcTask
    * @throws Exception
    */
   abstract VcTask migrate(final VcResourcePool rp, final IVcTaskCallback callback)
         throws Exception;

   /**
    * Migrate this vm to a specified host and call callback on completion.
    * @param host    dest host
    * @param callback   called on completion
    * @return VcTask
    * @throws Exception
    */
   abstract VcTask migrate(final VcHost host, final IVcTaskCallback callback)
         throws Exception;

   /**
    * Migrates this vm to a specified rp.
    * @param rp
    */
   abstract void migrate(VcResourcePool rp) throws Exception;

   /**
    * Migrates this vm to a specified host.
    * @param host
    */
   abstract void migrate(VcHost host) throws Exception;

   abstract Long getCpuReservationHZ();

   abstract Long getMemReservationMB();

   abstract Integer getMemSizeMB();

   /**
    * Either enable or disable changed block tracking on the VM.
    * This unconditionally issues a VC command without checking
    * the current state. Callers are expected to check the
    * current state.
    *
    * @param enabled
    * @return
    * @throws Exception
    */
   abstract void setRequestedChangeTracking(boolean enabled)
         throws Exception;

   abstract ManagedByInfo getManagedBy();

   abstract void setManagedBy(String owner, String type) throws Exception;

   /**
    * Check whether this VM is managed by this instance of Aurora
    * @return True if managed by this CMS; false if not managed.
    */
   abstract boolean isManagedByThisCms();

   /**
    * @return VM HA configuration.
    */
   abstract VmHAConfig getVmHAConfig();

   /**
    * @return VM guest info real time
    */
   abstract GuestInfo queryGuest() throws Exception;

   /**
    * Wait for power-on initiated within cms via any vim api calls
    * @param timeOutSecs
    * @return Power on result info including return code and VM's IP
    * @throws Exception
    */
   abstract GuestVarReturnCode waitForPowerOnResult(Integer timeOutSecs) throws Exception;


   /**
    * Refresh the storage info if the storage refresh flag is dirty.
    */
   abstract void updateStorageInfoIfNeeded() throws Exception;

   /**
    * Reconfig virtual machine's network.
    */
   abstract void updateVmNic(String pubNICLabel, String privNICLabel,
                             String pubNetId, String privNetId) throws Exception;

   /**
    * Reconfig virtual machine, and remove all cdrom.
    */
   abstract void detachAllCdroms() throws Exception;

   /**
    * Modify virtual machine HA settings.
    *
    * @param restartPriority Virtual machine restart priority to resolve
    * resource contention. If null, this property is not modified.
    * See {@link com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority}
    * @param isolationResponse  Whether or not the virtual machine should be
    * powered off if a host determines that it is isolated from
    * the rest of the cluster. If null, this property is not modified.
    * See {@link com.vmware.vim.binding.vim.cluster.DasVmSettings.IsolationResponse}
    * @param vmMonitoringState Set the virtual machine health monitoring state.
    * See {@link com.vmware.vim.binding.vim.cluster.DasConfigInfo.VmMonitoringState}
    * @throws Exception
    */
   void modifyHASettings(RestartPriority restartPriority, IsolationResponse isolationResponse,
         VmMonitoringState vmMonitoringState) throws Exception;

   /**
    * Turn on fault tolerant for this virtual machine, and a secondary virtual machine
    * will be created on the <code>host</code> if specified.
    * @param host The host where the secondary virtual machine is to be
    * created and powered on. If no host is specified, a compatible host will be
    * selected by the system. If a host cannot be found for the secondary or the specified
    * host is not suitable, the secondary will not be created and a fault will be returned.
    * @param callback
    * @return
    * @throws Exception
    */
   VcTask turnOnFT(final VcHost host, final IVcTaskCallback callback) throws Exception;

   void turnOnFT(final VcHost host) throws Exception;

   /**
    * Remove secondary virtual machine associated with this virtual machine and
    * turn off protection for this virtual machine.
    * @param callback
    * @return
    * @throws Exception
    */
   VcTask turnOffFT(final IVcTaskCallback callback) throws Exception;

   void turnOffFT() throws Exception;

   /**
    * Enable secondary virtual machine associated with this virtual machine.
    * @param callback
    * @return
    * @throws Exception
    */
   VcTask enableFT(final IVcTaskCallback callback) throws Exception;

   void enableFT() throws Exception;

   /**
    * Disable DRS service.
    * @return
    * @throws Exception
    */
   VcTask disableDrs() throws Exception;

   /**
    * Disable secondary virtual machine associated with this virtual machine.
    * @param callback
    * @return
    * @throws Exception
    */
   VcTask disableFT(final IVcTaskCallback callback) throws Exception;

   void disableFT() throws Exception;

   /**
    * Creates a new Virtual Machine by cloning an existing VM.
    *
    * @param vmSpec      specification of the new VM's configuration
    * @param removeDisks disks to be removed from the new VM
    * @return the VM created.
    * @throws Exception
    */
   public VcVirtualMachine cloneVm(final CreateSpec vmSpec,
                       final DeviceId[] removeDisks) throws Exception;

   /**
    *
    * @param vmSpec      specification of the new VM's configuration
    * @param removeDisks disks to be removed from the new VM
    * @return VMTask cloning the VM.
    * @throws Exception
    */
   public VcTask cloneVmAsync(final CreateSpec vmSpec, final DeviceId[] removeDisks) throws Exception;


   /**
    *
    * @param name new VM's name
    * @param rp new VM's resource pool
    * @param ds new VM's datastore
    * @param folder new VM's datastore folder.
    * @param persisted
    * @param callback
    * @return
    * @throws Exception
    */
   VcTask createForkChild(String name, VcResourcePool rp, VcDatastore ds, Folder folder,
                             boolean persisted, IVcTaskCallback callback) throws Exception;

   /**
    * if the VM is ready for forking child.
    * @return
    */
   boolean isQuiescedForkParent();

   /**
    * Change the VM disks layout.
    *
    * @param removeDisks disks to be removed
    * @param addDisks    disks to be added
    */
   public void changeDisks(final DeviceId[] removeDisks, final DiskCreateSpec[] addDisks) throws Exception;

   public Folder getParentFolder();

   void enableForkParent();
}

@SuppressWarnings("serial")
class VcVirtualMachineImpl extends VcVmBaseImpl implements VcVirtualMachine {
   /**
    * Inner class that allows threads to wait until the arrival of any
    * requested VmEvent. Clients can wait only for "future" events - the
    * ones arriving after a new instance of this class was created. Upon
    * the event arrival all waiting threads are released.
    */
   private abstract class WaitForVmEventHandler implements IVcEventHandler {
      // Threshold for single wait.
      private final long waitThresholdNanos = TimeUnit.SECONDS.toNanos(20);
      private final CountDownLatch eventLatch;  // Drops to 0 when event arrives.
      private final VcEventType eventType;      // VcEventType to wait for.
      private final boolean external;           // External event to wait for?

      /**
       * Create a new handler and register it with VcEventListener.
       * @param eventType
       */
      WaitForVmEventHandler(VcEventType eventType, boolean external) {
         /* Make sure we are waiting for VmEvent subtypes only. */
         AuAssert.check(VmEvent.class.isAssignableFrom(eventType.getEventClass()));
         this.eventType = eventType;
         this.external = external;
         eventLatch = new CountDownLatch(1); // Will wait for 1 event.
         if (external) {
            VcEventListener.installExtEventHandler(eventType, this);
         } else {
            VcEventListener.installEventHandler(eventType, this);
         }
      }

      /**
       * Deactivate the handler by removing it from VcEventListener.
       */
      void disable() {
         if (external) {
            VcEventListener.removeExtEventHandler(eventType, this);
         } else {
            VcEventListener.removeEventHandler(eventType, this);
         }
      }

      private String msg(String text) {
         StringBuilder buf = new StringBuilder(text);
         buf = buf.append(" ").append(eventType).append(" ").
               append(VcVirtualMachineImpl.this);
         return buf.toString();
      }

      /**
       * VmEvent handler: if event is for this VM, release all waiters and
       * return true. Otherwise, return false.
       * @param type    VcEventType
       * @param e   VmEvent
       * @return true, if event for this vm
       */
      @Override
      public boolean eventHandler(VcEventType type, Event e) throws Exception {
         AuAssert.check(eventLatch.getCount() <= 1);
         VmEvent event = (VmEvent)e;
         AuAssert.check(event.getVm() != null);
         /* If VmEvent is for this VM, release waiters. */
         if (VcVirtualMachineImpl.this.getMoRef().equals(event.getVm().getVm())) {
            eventCallback();
            eventLatch.countDown();
            logger.debug(msg("VmEventHandler: match"));
            return true;
         }
         return false;
      }

      /**
       * Called by threads to wait until event arrival. Waits until either:
       * - a latch is dropped
       * - a timeout expires
       * - resumeWaiting condition turns to false
       * Returns false on timeout, true in other cases. Does not break out
       * of wait on InterruptedException. Rechecks vc condition every
       * waitThresholdNanos to make sure vc does not drop events permanently.
       * @param timeout
       * @param unit
       * @return true if event arrived
       * @throws Exception
       */
      boolean await(long timeout, TimeUnit unit) throws Exception {
         long timeoutNanos = unit.toNanos(timeout);
         boolean res = false;
         long finishNanos = System.nanoTime() + timeoutNanos;

         if (!resumeWaiting()) {
            logger.warn(msg("Unnecessary wait for event skipped:"));
            return true;
         }
         logger.info(msg("Waiting for event:"));
         while (!res && timeoutNanos > 0) {
            /* Wake up periodically because we don't quite trust VC. */
            if (timeoutNanos > waitThresholdNanos) {
               timeoutNanos = waitThresholdNanos;
            }
            try {
               res = eventLatch.await(timeoutNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
               /* Ok to swallow. */
            } finally {
               timeoutNanos = finishNanos - System.nanoTime();
            }
            /*
             * If we did not get an event we were waiting for, check with vc.
             * Until vc convinces us that it does not lose events, do periodic
             * checks to make sure that the "event condition" is still false.
             */
            if (!res && !resumeWaiting()) {
               logger.warn(msg("Dropped event?"));
               res = true;
            }
         }
         if (res) {
            logger.info(msg("Success: wait for event"));
         } else {
            logger.info(msg("Failure: wait for event"));
         }
         return res;
      }

      /**
       * Since we don't fully trust VC, this function must be supplied by
       * subtypes to periodically check the server condition matching
       * waiting for the expected event. For example,
       *    VmPoweredOffEvent -> vm.runtimeInfo.powerState == poweredOn
       * This function would normally return true while we are still
       * waiting for an event and false after event's arrival.
       * @return true, if we need to continue waiting for event
       */
      protected abstract boolean resumeWaiting() throws Exception;

      /**
       * A callback when the expected event has arrived.
       * The callback typically refreshes the target object's state
       * as a result of the task. Even if the task caller fails after
       * receiving an exception, the target object's state will still
       * be updated by the event handler.
       */
      protected abstract void eventCallback();
   }

   /**
    * A handler that waits until this VM is powered off.
    */
   private class WaitForPowerOffHandler extends WaitForVmEventHandler {
      WaitForPowerOffHandler(boolean external) {
         super(VcEventType.VmPoweredOff, external);
      }

      /**
       * Continue to wait as long as vm stays powered on.
       */
      @Override
      protected boolean resumeWaiting() throws Exception {
         updateRuntime();
         return !isPoweredOff();
      }

      /**
       * Refresh vm state after receiving the event.
       */
      @Override
      protected void eventCallback() {
         VcCache.refreshRuntime(getMoRef());
      }
   }

   public interface VmOp<T> {
      public T exec() throws Exception;
   }

   /*
    *  Retry VM operation if we detect that an invalid state
    *  exception has been thrown.
    */
   <T> T safeExecVmOp(VmOp<T> vmOp) throws Exception {
      int retries = 5;
      while (true) {
         try {
            return vmOp.exec();
         } catch (InvalidState e) {
            if (retries <= 0) {
               throw e;
            }
            logger.info("got invalid state exception on vm " + this);
            if (e instanceof InvalidPowerState) {
               InvalidPowerState e1 = (InvalidPowerState)e;
               if (e1.getRequestedState().equals(PowerState.poweredOff)) {
                  logger.info("power off and retry operation on " + this);
                  powerOff();
               } else {
                  logger.warn("expecting power state: " + e1.getRequestedState());
               }
            }
            // VC might be in an inconsistent state, retry the operation.
            Thread.sleep(20 * 1000);
            retries--;
         }
      }
   }

   private int key;
   private ConfigInfo config;
   private RuntimeInfo runtime;
   private PowerState cachedPowerState = null;
   private DiskSize storageUsage;
   private DiskSize storageCommitted;
   private ManagedObjectReference resourcePool;
   private ManagedObjectReference parentVApp;
   private List<ManagedObjectReference> datastores; // datastores accessed by this VM
   private FileLayoutEx layoutEx;
   private Map<ManagedObjectReference, VcSnapshotImpl> snapshots =
      new HashMap<ManagedObjectReference, VcSnapshotImpl>();
   private ManagedObjectReference currentSnapshot;
   private volatile boolean needsStorageInfoRefresh = true;

   static final String MACHINE_ID = "machine.id";
   static final String DBVM_CONFIG = "dbvm.config";
   static final long GUEST_VAR_CHECK_INTERVAL = 3000; // in milliseconds

   @Override
   protected void update(ManagedObject mo) throws Exception {
      VirtualMachine vm = (VirtualMachine)mo;
      config = checkReady(vm.getConfig());
      resourcePool = vm.getResourcePool();
      if (!isTemplate()) {
         checkReady(resourcePool);
      }
      parentVApp = vm.getParentVApp();
      datastores = Arrays.asList(checkReady(vm.getDatastore()));
      layoutEx = checkReady(vm.getLayoutEx());
      updateSnapshots(vm);
   }

   @Override
   protected synchronized void updateRuntime(ManagedObject mo) throws Exception {
      VirtualMachine vm = (VirtualMachine)mo;
      this.runtime = checkReady(vm.getRuntime());
      Summary summary = checkReady(vm.getSummary());
      StorageSummary storageSummary = checkReady(summary.getStorage());
      storageUsage = new DiskSize(storageSummary.getUnshared());
      storageCommitted = new DiskSize(storageSummary.getCommitted());
      /* XXX Layout needs to be updated in both update() and updateRuntime()
       * as it contains both configuration & runtime data.
       */
      this.layoutEx = checkReady(vm.getLayoutEx());
      /**
       * VC sometime can get out of sync with hostd on VM's power state.
       * This cachedPowerState is computed base on return values of events
       * and/or VC calls, which may be different from VC's runtime state.
       * We trust cachedPowerState more than runtime state if they are out of sync.
       */
      if (cachedPowerState != null) {
         if (cachedPowerState == runtime.getPowerState()) {
            /* At some point, runtime state becomes in sync.
             * We set cachedPowerState to null to indicate that VC contains
             * the consistent state.
             */
            cachedPowerState = null;
         }
      }

   }

   @Override
   protected void processNotFoundException() throws Exception {
      logger.error("vm " + MoUtil.morefToString(moRef)
            + " is already deleted in VC. Purge from vc cache");
      VcCache.purge(moRef);
      VcCache.removeVmRpPair(moRef);
   }

   /**
    * Refresh the storage info if the storage refresh flag is dirty.
    */
   @Override
   public void updateStorageInfoIfNeeded() throws Exception {
      boolean needsRefresh = needsStorageInfoRefresh();
      if (needsRefresh) {
         try {
            // clear dirty flag
            setNeedsStorageInfoRefresh(false);

            VcContext.getVcLongCallHandler().execute(
                  new VcLongCallHandler.VcLongCall<Void>() {
                     @Override
                     public Void callVc() throws Exception {
                        VirtualMachine vm = getManagedObject();
                        vm.refreshStorageInfo();
                        return null;
                     }
                  });
         } catch (Exception e) {
            setNeedsStorageInfoRefresh(true);
            throw e;
         }

         VirtualMachine vm = getManagedObject();
         Summary summary = checkReady(vm.getSummary());
         StorageSummary storageSummary = checkReady(summary.getStorage());
         storageUsage = new DiskSize(storageSummary.getUnshared());
         storageCommitted = new DiskSize(storageSummary.getCommitted());
         layoutEx = checkReady(vm.getLayoutEx());
      }
   }

   protected VcVirtualMachineImpl(final VirtualMachine vm) throws Exception {
      super(vm);
      safeExecVmOp(new VmOp<Void>() {
         public Void exec() throws Exception {
            update(vm);
            updateRuntime(vm);
            return null;
         }
      });
      // key is used in ConfigSpec when updating multiple aspects, such as devices
      // The key we specify does not matter, and will get reassigned, so we start with -1
      // and go lower (the system assigned keys are positive).
      key = -1;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getConfig()
    */
   @Override
   public ConfigInfo getConfig() {
      return config;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getVAppConfig()
    */
   @Override
   public VmConfigInfo getVAppConfig() {
      return config.getVAppConfig();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getName()
    */
   @Override
   public String getName() {
      return MoUtil.fromURLString(config.getName());
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#rename(java.lang.String)
    */
   @Override
   public void rename(String newName) throws Exception {
      ConfigSpec spec = new ConfigSpecImpl();
      spec.setName(newName);
      reconfigure(spec);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getURLName()
    */
   @Override
   public String getURLName() {
      return config.getName();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getPathName()
    */
   @Override
   public String getPathName() {
      String vmxPath = config.getFiles().getVmPathName();
      // get directory name of VMX file, excluding the last '/'
      return vmxPath.substring(0, vmxPath.lastIndexOf('/'));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#isTemplate()
    */
   @Override
   public boolean isTemplate() {
      return config.isTemplate();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getHost()
    */
   @Override
   public VcHost getHost() {
      return VcCache.get(runtime.getHost());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getDatacenter()
    */
   @Override
   public VcDatacenter getDatacenter() {
      if (datastores.size() > 0) {
         VcDatastore ds = VcCache.get(datastores.get(0));
         return ds.getDatacenter();
      } else {
         return null;
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getDatastores()
    */
   @Override
   public VcDatastore[] getDatastores() {
      List<VcDatastore> dsList = VcCache.getPartialList(datastores, getMoRef());
      return dsList.toArray(new VcDatastore[dsList.size()]);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getDefaultDatastore()
    */
   @Override
   public VcDatastore getDefaultDatastore() {
      if (datastores.size() > 0) {
         return VcCache.get(datastores.get(0));
      } else {
         return null;
      }
   }

   public String toString() {
      return String.format("%s[%s](%s)", isTemplate()? "VM_T":"VM", getName(),
                           isCached() ? "c":"");
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getInfo()
    */
   @Override
   public String getInfo() {
      Long cpuLimit=config.getCpuAllocation().getLimit();
      Long cpuReservation=config.getCpuAllocation().getReservation();
      Long memLimit=config.getMemoryAllocation().getLimit();
      Long memReservation=config.getMemoryAllocation().getReservation();
      return String.format("%s[%s](cpu:R=%d,L=%d,mem:R=%d,L=%d,%d cpus,CS:%s,PS:%s)",
            isTemplate()? "VM_T": "VM", getName(),
            cpuReservation, cpuLimit, memReservation, memLimit,
            config.getHardware().getNumCPU(),
            runtime.getConnectionState(), runtime.getPowerState());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#refreshRP()
    */
   public void refreshRP() {
      if (!isTemplate()) {
         VcCache.refresh(resourcePool);
      }
   }

   private VirtualDeviceSpec
   detachVirtualDiskSpec(DeviceId deviceId, boolean destroyDisk)
   throws Exception {
      VirtualDevice device = getVirtualDevice(deviceId);
      if (device == null || !(device instanceof VirtualDisk)) {
         String deviceInfo = device != null ? VmConfigUtil
               .getVirtualDeviceInfo(device) : "device not found";
         logger.info("cannot detach disk " + deviceId + ": " + deviceInfo);
         throw VcException.INTERNAL_DISK_DETACHMENT_ERROR();
      }
      VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
      spec.setDevice(device);
      spec.setOperation(VirtualDeviceSpec.Operation.remove);
      if (destroyDisk) {
         spec.setFileOperation(VirtualDeviceSpec.FileOperation.destroy);
      }
      return spec;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#detachVirtualDisk(com.vmware.aurora.vc.DeviceId, boolean)
    */
   @Override
   public void detachVirtualDisk(DeviceId deviceId, boolean destroyDisk)
         throws Exception {
      String dsPath = null;
      if (destroyDisk) {
         VirtualDisk vmdk = (VirtualDisk) getVirtualDevice(deviceId);
         if (vmdk == null) {
            throw new ManagedObjectNotFound();
         }
         dsPath = VmConfigUtil.getVmdkPath(vmdk);
      }

      /*
       *  Workaround for PR 904771.
       *  Detach disk (FileOperation.destroy not set) is not enabled in vSphere 5.1 if snapshot exists.
       *  Use destroy disk to replace detach disk operation.
       *  Disks that are still needed should be protected by snapshot.
       */
      VirtualDeviceSpec change =
            detachVirtualDiskSpec(deviceId, !VmConfigUtil.isDetachDiskEnabled() || destroyDisk /* "destroyDisk" Comment below. */);
      reconfigure(VmConfigUtil.createConfigSpec(change));

      /*
       *  A work-around for PR 742324.
       *  The previous call may not have succeeded for yet unknown reasons. Try
       *  deletion one more time through a direct disk-level call if the disk
       *  still exists.
       *
       *  Another variation is that reconfigure() above can only detach
       *  the disk, and the following is always used as the primary deletion
       *  mechanism instead of a fall-back mechanism, but I am scared
       *  to do it so close to the dead-line.
       */
      if (destroyDisk) {
         try {
            String uuid = VcFileManager.queryVirtualDiskUuid(dsPath, getDatacenter());
            if (uuid != null) {
               VcFileManager.deleteVirtualDisk(dsPath, getDatacenter());
               logger.info("The disk at " + deviceId + " was deleted on second try.");
            }
         } catch (FileNotFound exc) {
             logger.info("The disk at " + deviceId + " is probably already deleted. OK.");
         } catch (Exception exc) {
            logger.warn(
                  "The disk at " + deviceId + " could not be deleted through the direct method.",
                  exc);
         }
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#detachVirtualDisk(com.vmware.aurora.vc.DeviceId, boolean)
    */
   @Override
   public void detachVirtualDiskIfExists(DeviceId deviceId, boolean destroyDisk)
         throws Exception {
      if (isDiskAttached(deviceId)) {
         detachVirtualDisk(deviceId, destroyDisk);
      }
   }

   private VirtualController attachVirtualController(DeviceId deviceId) throws Exception {
      ScsiControllerType scsiType = VmConfigUtil.ScsiControllerType.findController(deviceId.getTypeClass());
      if (scsiType != null) {
         logger.info("Adding " + deviceId.controllerType + " SCSI controller to VM " + this.getName()
               + " at bus " + deviceId.busNum);
      } else {
         logger.error("Unsupported SCSI type creation: " + deviceId.controllerType);
         throw VcException.INTERNAL();
      }

      if (reconfigure(VmConfigUtil.createConfigSpec(VmConfigUtil.createControllerDevice(scsiType, deviceId.busNum)))) {
         return getVirtualController(deviceId);
      } else {
         return null;
      }
   }

   public VirtualDeviceSpec
   attachVirtualDiskSpec(DeviceId deviceId,
                         VirtualDevice.BackingInfo backing,
                         boolean createDisk, DiskSize size) throws Exception {
      VirtualController controller = getVirtualController(deviceId);
      if (controller == null) {
         // Add the controller to the VM if it does not exist
         controller = attachVirtualController(deviceId);
         if (controller == null) {
            throw VcException.CONTROLLER_NOT_FOUND(deviceId.toString());
         }
      }

      VirtualDisk vmdk = VmConfigUtil.createVirtualDisk(controller, deviceId.unitNum,
                                                        backing, size);
      // key is used in ConfigSpec when updating multiple aspects, such as devices
      // The key we specify does not matter, and will get reassigned, so we start with -1
      // and go lower (the system assigned keys are positive). Without specifying the keys,
      // multiple updates in a single call will not work.
      vmdk.setKey(key);
      key--;
      VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
      spec.setOperation(VirtualDeviceSpec.Operation.add);
      if (createDisk) {
         spec.setFileOperation(VirtualDeviceSpec.FileOperation.create);
      }
      spec.setDevice(vmdk);
      return spec;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#attachVirtualDisk(com.vmware.aurora.vc.DeviceId, com.vmware.vim.binding.vim.vm.device.VirtualDevice.BackingInfo, boolean, com.vmware.aurora.global.DiskSize)
    */
   @Override
   public void
   attachVirtualDisk(DeviceId deviceId, VirtualDevice.BackingInfo backing,
                     boolean createDisk, DiskSize size) throws Exception {


      // Here we attach a disk once at a time.
      // VC can attach multiple disks in one change set - if you want that, use attachVirtualDiskSpec
      // to build up an array of VirtualDeviceSpecs and call reconfigure directly on the created
      // config spec.
      VirtualDeviceSpec change = attachVirtualDiskSpec(deviceId, backing,
                                                       createDisk, size);
      reconfigure(VmConfigUtil.createConfigSpec(change));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#copyAttachVirtualDisk(com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcVmBase, com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcDatastore, java.lang.String, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode)
    */
   @Override
   public void
   copyAttachVirtualDisk(DeviceId deviceId, VcVmBase srcVm,
         DeviceId srcDeviceId, VcDatastore dstDs, String diskName, DiskMode diskMode)
   throws Exception {
      VirtualDisk vmdk = (VirtualDisk)srcVm.getVirtualDevice(srcDeviceId);
      String srcPath = VmConfigUtil.getVmdkPath(vmdk);
      String dstPath = VcFileManager.getDsPath(this, dstDs, diskName);
      logger.info("Copying disk '" + srcPath + "' to '" + dstPath + "'");
      // By default it would use settings from the parent disk,
      // verified for sparse & thin provisioned disks.
      VirtualDiskSpec spec = null;
      VcFileManager.copyVirtualDisk(srcPath, srcVm.getDatacenter(),
                                    dstPath, getDatacenter(), spec);
      attachVirtualDisk(deviceId,
            VmConfigUtil.createVmdkBackingInfo(this, dstDs, diskName, diskMode, null, null),
            false, null);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#copyAttachVirtualDisk(com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcVmBase, com.vmware.aurora.vc.DeviceId, java.lang.String, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode)
    */
   @Override
   public void
   copyAttachVirtualDisk(DeviceId deviceId, VcVmBase srcVm,
         DeviceId srcDeviceId, String diskName, DiskMode diskMode)
   throws Exception {
      copyAttachVirtualDisk(deviceId, srcVm, srcDeviceId, null, diskName, diskMode);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#attachChildDisk(com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcSnapshot, com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcDatastore, java.lang.String, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode, java.lang.Boolean)
    */
   @Override
   public void attachChildDiskPath(DeviceId deviceId, VcSnapshot srcSnap,
         DeviceId srcDeviceId, String diskPath,
         DiskMode diskMode) throws Exception {
      VirtualDisk vmdk = (VirtualDisk) srcSnap.getVirtualDevice(srcDeviceId);
      BackingInfo parentBacking = vmdk.getBacking();
      BackingInfo backing =
            VmConfigUtil.createVmdkBackingInfo(diskPath,
                  diskMode, (VirtualDisk.FlatVer2BackingInfo)parentBacking, null, null);
      attachVirtualDisk(deviceId, backing, true, null);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#attachChildDisk(com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcSnapshot, com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcDatastore, java.lang.String, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode)
    */
   @Override
   public void attachChildDisk(DeviceId deviceId, VcSnapshot srcSnap,
         DeviceId srcDeviceId, VcDatastore dstDs, String diskName,
         DiskMode diskMode) throws Exception {
      VirtualDisk vmdk = (VirtualDisk) srcSnap.getVirtualDevice(srcDeviceId);
      BackingInfo parentBacking = vmdk.getBacking();
      BackingInfo backing =
            VmConfigUtil.createVmdkBackingInfo(this, dstDs, diskName,
                  diskMode, parentBacking);
      attachVirtualDisk(deviceId, backing, true, null);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#attachChildDisk(com.vmware.aurora.vc.DeviceId, com.vmware.aurora.vc.VcSnapshot, com.vmware.aurora.vc.DeviceId, java.lang.String, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode)
    */
   @Override
   public void attachChildDisk(DeviceId deviceId, VcSnapshot srcSnap,
         DeviceId srcDeviceId, String diskName, DiskMode diskMode)
         throws Exception {
      attachChildDisk(deviceId, srcSnap, srcDeviceId, null, diskName, diskMode);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#promoteDisk(com.vmware.aurora.vc.DeviceId)
    */
   @Override
   public void promoteDisk(DeviceId deviceId) throws Exception {
      VirtualDisk vmdk = (VirtualDisk) this.getVirtualDevice(deviceId);
      this.promoteDisk(vmdk, true);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#extendVirtualDisk(com.vmware.aurora.vc.DeviceId, com.vmware.aurora.global.DiskSize)
    */
   @Override
   public void
   extendVirtualDisk(DeviceId deviceId, DiskSize size) throws Exception {
      VirtualDisk vmdk = (VirtualDisk)getVirtualDevice(deviceId);
      VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
      vmdk.setCapacityInKB(size.getKiB());
      spec.setOperation(VirtualDeviceSpec.Operation.edit);
      spec.setDevice(vmdk);
      reconfigure(VmConfigUtil.createConfigSpec(spec));
   }


   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#editVirtualDisk(com.vmware.aurora.vc.DeviceId, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode)
    */
   @Override
   public VirtualDeviceSpec editVirtualDiskSpec(DeviceId deviceId, DiskMode newMode)
         throws Exception {
      VirtualDisk vmdk = (VirtualDisk) getVirtualDevice(deviceId);

      BackingInfo backing = vmdk.getBacking();
      if (backing instanceof VirtualDisk.FlatVer2BackingInfo) {
         ((VirtualDisk.FlatVer2BackingInfo) backing).setDiskMode(newMode
               .toString());
      } else if (backing instanceof VirtualDisk.SparseVer2BackingInfo) {
         ((VirtualDisk.SparseVer2BackingInfo) backing).setDiskMode(newMode
               .toString());
      } else {
         AuAssert.check(backing instanceof VirtualDisk.SeSparseBackingInfo);
         ((VirtualDisk.SeSparseBackingInfo) backing).setDiskMode(newMode
               .toString());
      }
      vmdk.setBacking(backing);

      VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
      spec.setOperation(VirtualDeviceSpec.Operation.edit);
      spec.setDevice(vmdk);
      return spec;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#editVirtualDisk(com.vmware.aurora.vc.DeviceId, com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode)
    */
   @Override
   public void editVirtualDisk(DeviceId deviceId, DiskMode newMode)
         throws Exception {
      VirtualDeviceSpec spec = editVirtualDiskSpec(deviceId, newMode);
      boolean success = reconfigure(VmConfigUtil.createConfigSpec(spec));
      if (!success) {
         throw new Exception("Failed to change the disk mode of " + deviceId
               + " to " + newMode);
      }
   }


   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#isBaseDisk(com.vmware.aurora.vc.DeviceId)
    */
   @Override
   public boolean isBaseDisk(DeviceId deviceId) {
      VirtualDevice device = getVirtualDevice(deviceId);
      BackingInfo backing = device.getBacking();
      if (backing != null && backing instanceof VirtualDisk.FlatVer2BackingInfo
            && ((VirtualDisk.FlatVer2BackingInfo) backing).getParent() == null) {
         return true;
      }
      return false;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getDiskCapacity(com.vmware.aurora.vc.DeviceId)
    */
   @Override
   public DiskSize getDiskCapacity(DeviceId deviceId) {
      VirtualDevice device = getVirtualDevice(deviceId);
      if (device instanceof VirtualDisk) {
         return DiskSize.sizeFromKiB(((VirtualDisk) device).getCapacityInKB());
      } else {
         return new DiskSize(0);
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getDiskDatastore(com.vmware.aurora.vc.DeviceId)
    */
   @Override
   public VcDatastore getDiskDatastore(DeviceId deviceId) {
      VirtualDisk vmdk = (VirtualDisk) getVirtualDevice(deviceId);
      if (vmdk == null) {
         throw VcException.DISK_NOT_FOUND(deviceId.toString());
      }
      VirtualDisk.FileBackingInfo diskBacking = (VirtualDisk.FileBackingInfo) vmdk.getBacking();
      return (VcDatastore) VcCache.get(diskBacking.getDatastore());
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#isDiskAttached(com.vmware.aurora.vc.saga.DbvmConfig.DiskId)
    */
   @Override
   public boolean isDiskAttached(DeviceId deviceId) {
      VirtualDevice device = getVirtualDevice(deviceId);
      return device != null && device instanceof VirtualDisk;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#QueryChangedDiskAreas
    */
   @Override
   public DiskChangeInfo queryChangedDiskAreas(VcSnapshot endMarkerSnapshot,
         DeviceId deviceId, long startOffset, String diskChangeId) throws Exception {
      VirtualDisk vmdk = (VirtualDisk) getVirtualDevice(deviceId);
      VirtualMachine vm = getManagedObject();
      return vm.queryChangedDiskAreas(endMarkerSnapshot.getMoRef(), vmdk.getKey(),
            startOffset, diskChangeId);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#mountISO(com.vmware.aurora.vc.DeviceId, com.vmware.vim.binding.vim.vm.device.VirtualDevice.BackingInfo)
    */
   @Override
   public VirtualDeviceSpec
   mountISO(DeviceId deviceId, VirtualDevice.BackingInfo backing)
   throws Exception {
      VirtualCdrom cdrom = (VirtualCdrom)getVirtualDevice(deviceId);
      VmConfigUtil.setVirtualDeviceBacking(cdrom, backing);
      VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
      spec.setOperation(VirtualDeviceSpec.Operation.edit);
      spec.setDevice(cdrom);
      return spec;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#reconfigNetworkSpec(java.lang.String, com.vmware.aurora.vc.VcNetwork)
    */
   @Override
   public VirtualDeviceSpec
   reconfigNetworkSpec(String label, VcNetwork network)
   throws Exception {
      VirtualDevice nic = getDeviceByLabel(label);
      VmConfigUtil.setVirtualDeviceBacking(nic, network.getBackingInfo());
      VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
      spec.setOperation(VirtualDeviceSpec.Operation.edit);
      spec.setDevice(nic);
      return spec;
   }

   /*
    * Wait in a loop for VC's view of the VM's power state
    * to be back in sync.
    */
   private void waitForPowerStateToSync(PowerState state, int timeout) throws Exception {
      AuAssert.check(Thread.holdsLock(this));
      VirtualMachine vm = getManagedObject();
      try {
         while (timeout > 0) {
            runtime = checkReady(vm.getRuntime());
            // If runtime state matches the claimed power state, done.
            if (runtime.getPowerState() == state) {
               return;
            }
            logger.info("syncing power state " + state + " on " + this);
            timeout -= 10;
            wait(10 * 1000);
         }
      } catch (InterruptedException e) {
         // break out if the thread is interrupted
      }
   }

   /*
    * Set requested power state of the VM if the known VM state retrieved from
    * task or exception is inconsistent with VC value. This could happen if
    * we get an external power event from VC before VC updates the VM's
    * power state. When this happens, we wait for VC to become in sync.
    * If that doesn't work, mark the inconsistent state.
    */
   private synchronized void
   setRequestedPowerState(PowerState state) throws Exception {
      if (runtime.getPowerState() != state) {
         waitForPowerStateToSync(state, WAIT_FOR_VC_STATE_TIMEOUT_SECS);
      }
      // If still not in sync, warn and record inconsistent state.
      if (runtime.getPowerState() != state) {
         logger.warn("inconsistent requested power state " + state + " on " + this);
         cachedPowerState = state;
      }
   }

   @Override
   public void
   setRequestedChangeTracking(boolean enabled) throws Exception {
      ConfigSpec spec = new ConfigSpecImpl();
      spec.setChangeTrackingEnabled(enabled);
      reconfigure(spec);
   }

   /*
    * Get power state. If power state may be inconsistent, return null.
    */
   private PowerState getPowerState() {
      if (cachedPowerState != null) {
         return null;
      }
      return runtime.getPowerState();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#isPoweredOn()
    */
   @Override
   public boolean isPoweredOn() {
      return getPowerState() == PowerState.poweredOn;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#isPoweredOff()
    */
   @Override
   public boolean isPoweredOff() {
      return getPowerState() == PowerState.poweredOff;
   }

   public ConnectionState getConnectionState() {
      return runtime.getConnectionState();
   }

   public boolean isConnected() {
      return (runtime.getConnectionState() == ConnectionState.connected);
   }

   public FaultToleranceState getFTState() {
      return runtime.getFaultToleranceState();
   }

   @Override
   public VcTask powerOn(final IVcTaskCallback callback) throws Exception {
      return powerOn(null, callback);
   }

   private boolean powerOnInt(final VcHost host) throws Exception {
      try {
         VcTask task = powerOn(host, VcCache.getRefreshRuntimeVcTaskCB(this));
         task.waitForCompletion();
         if (task.taskCompleted()) {
            setRequestedPowerState(PowerState.poweredOn);
         }
         return task.taskCompleted();
      } catch (InvalidPowerState e) {
         if (e.getExistingState().equals(PowerState.poweredOn)) {
            setRequestedPowerState(PowerState.poweredOn);
            return true;
         } else {
            throw e;
         }
      }
   }

   @Override
   public boolean powerOn() throws Exception {
      return powerOn((VcHost)null);
   }

   @Override
   public VcTask powerOn(final VcHost host, final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.PowerOn, vm.powerOn(host == null ? null : host.getMoRef()), callback);
         }
      });

      logger.debug("power_on " + this + " task created");
      return task;
   }

   @Override
   public boolean powerOn(final VcHost host) throws Exception {
      try {
         return safeExecVmOp(new VmOp<Boolean>() {
            public Boolean exec() throws Exception {
               return powerOnInt(host);
            }
         });
      } catch (Exception e) {
         throw VcException.POWER_ON_VM_FAILED(e, getName(), e.getMessage());
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#powerOff(com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask powerOff(final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.PowerOff, vm.powerOff(), callback);
         }
      });

      logger.debug("power_off " + this + " task created");
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#powerOff()
    */
   @Override
   public boolean powerOff() throws Exception {
      try {
         VcTask task = powerOff(VcCache.getRefreshRuntimeVcTaskCB(this));
         task.waitForCompletion();
         if (task.taskCompleted()) {
            setRequestedPowerState(PowerState.poweredOff);
         }
         return task.taskCompleted();
      } catch (InvalidPowerState e) {
         if (e.getExistingState().equals(PowerState.poweredOff)) {
            setRequestedPowerState(PowerState.poweredOff);
            return true;
         } else {
            throw VcException.POWER_OFF_VM_FAILED(e, getName(), e.getMessage());
         }
      }
   }

   private boolean waitForPowerOff(long timeoutMillis, boolean external) throws Exception {
      WaitForPowerOffHandler eventHandler = new WaitForPowerOffHandler(external);
      boolean res;
      try {
         res = eventHandler.await(timeoutMillis, TimeUnit.MILLISECONDS);
         if (res) {
            setRequestedPowerState(PowerState.poweredOff);
         }
      } finally {
         eventHandler.disable();
      }
      return res;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#waitForExternalPowerOff(long)
    */
   @Override
   public boolean waitForExternalPowerOff(long timeoutMillis) throws Exception {
      return waitForPowerOff(timeoutMillis, true);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#waitForPowerOff(long)
    */
   @Override
   public boolean waitForPowerOff(long timeoutMillis) throws Exception {
      return waitForPowerOff(timeoutMillis, false);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#shutdownGuest(long)
    */
   @Override
   public boolean shutdownGuest(final long timeoutMillis) throws Exception {
      try {
         VcContext.getTaskMgr().execPseudoTask("VirtualMachine.shutDownGuest",
               VcEventType.VmPoweredOff, getMoRef(),
               new IVcPseudoTaskBody () {
            @Override
            public ManagedObjectReference body() throws Exception {
               VirtualMachine vm = getManagedObject();
               vm.shutdownGuest();      // Initiates shutdown.
               if (!waitForPowerOff(timeoutMillis)) {
                  throw VcException.GUEST_TIMEOUT();
               }
               return getMoRef();
            }
         });
      } catch (InvalidPowerState e) {
         if (e.getExistingState().equals(PowerState.poweredOff)) {
            setRequestedPowerState(PowerState.poweredOff);
            return true;
         } else {
            return false;
         }
      } catch (Exception e) {
         if (e instanceof ToolsUnavailable ||
             e instanceof VcException) {
            logger.info("shutdownGuest got ", e);
         } else {
            logger.warn("shutdownGuest got unexpected ", e);
         }
         return false;
      }
      return true;
   }

   private VcTask destroy(final IVcTaskCallback callback) throws Exception {
      return VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.DestroyVm, vm.destroy(), callback);
         }
      });
   }

   void destroyInt() throws Exception {
      try {
         final ManagedObjectReference oldRp = resourcePool;
         final ManagedObjectReference oldVm = getMoRef();
         VcTask task = destroy(new IVcTaskCallback() {
            @Override
            public final void completeCB(VcTask task) {
               VcCache.purge(oldVm);
               if (oldRp != null) {
               VcCache.refresh(oldRp);
               }
               VcCache.removeVmRpPair(oldVm);
            }
            @Override
            public final void syncCB() {
               if (oldRp != null) {
               VcCache.sync(oldRp);
            }
            }
         });
         task.waitForCompletion();
      } catch (ManagedObjectNotFound e) {
         // The object is gone. Nothing to do.
         logger.info("cannot destroy " + this + ", not found.");
      }
   }

   @Override
   public void destroy() throws Exception {
      destroy(true);
   }

   @Override
   public void destroy(final boolean removeSnapShot) throws Exception {
      try {
         safeExecVmOp(new VmOp<Void>() {
            public Void exec() throws Exception {
               if (removeSnapShot) {
                  removeAllSnapshots(); // PR 878822
               }
               destroyInt();
               return null;
            }
         });
      } catch(Exception e) {
         throw VcException.DELETE_VM_FAILED(e, getName(), e.getMessage());
      }
   }

   @Override
   public void unregister() throws Exception {
      final ManagedObjectReference oldRp = resourcePool;
      final ManagedObjectReference oldVm = getMoRef();
      VirtualMachine vm = getManagedObject();
      vm.unregister();
      VcCache.purge(oldVm);
      VcCache.removeVmRpPair(oldVm);
      if (oldRp != null) {
         VcCache.refresh(oldRp);
         VcCache.sync(oldRp);
      }
   }

   private VcTask relocateDisksWork(DeviceId[] deviceIds,
            ManagedObjectReference dsMoRef, final IVcTaskCallback callback) throws Exception {
      final RelocateSpec relocSpec = new RelocateSpecImpl();
      List<RelocateSpec.DiskLocator> diskList = new ArrayList<RelocateSpec.DiskLocator>();
      for(DeviceId deviceId : deviceIds) {
         VirtualDevice device = getVirtualDevice(deviceId);
         RelocateSpec.DiskLocator disk = new RelocateSpecImpl.DiskLocatorImpl();
         disk.setDatastore(dsMoRef);
         disk.setDiskId(device.getKey());
         diskList.add(disk);
      }

      relocSpec.setDisk(diskList.toArray(new RelocateSpec.DiskLocator[diskList.size()]));
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.RelocateVm,
                  vm.relocate(relocSpec, VirtualMachine.MovePriority.defaultPriority), callback);
         }
      });
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#relocateDisks()
    */
   @Override
   public void relocateDisks(DeviceId[] deviceIds, VcDatastore ds) throws Exception {
      VcTask task = relocateDisksWork(deviceIds, ds.getMoRef(),
            VcCache.getRefreshAllVcTaskCB(this));
      task.waitForCompletion();
      setNeedsStorageInfoRefresh(true);
   }

   /*
    * Clone a new VM (low level code).
    */
   private VcTask cloneWork(final VcDatacenter dc,
         ManagedObjectReference rpMoRef, ManagedObjectReference dsMoRef,
         ManagedObjectReference snapMoRef, final ManagedObjectReference folderMoRef,
         ManagedObjectReference hostMoRef, boolean isLinked, final String name,
         ConfigSpec config, final IVcTaskCallback callback) throws Exception {
      final CloneSpec spec = new CloneSpecImpl();
      RelocateSpec relocSpec = new RelocateSpecImpl();
      relocSpec.setPool(rpMoRef);
      relocSpec.setDatastore(dsMoRef);
      if (hostMoRef != null) {
         relocSpec.setHost(hostMoRef);
      }
      if (isLinked) {
         relocSpec.setDiskMoveType("createNewChildDiskBacking");
      }
      spec.setLocation(relocSpec);
      spec.setSnapshot(snapMoRef);
      spec.setTemplate(false);
      spec.setConfig(config);

      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.CloneVm,
                  vm.clone(folderMoRef == null ? dc.getVmFolderMoRef() : folderMoRef, name, spec), callback);
         }
      });
      logger.debug("clone_vm task on " + this + " to VM[" + name + "] created");
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneTemplate(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcDatastore, com.vmware.vim.binding.vim.vm.ConfigSpec, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask cloneTemplate(String name, VcResourcePool rp, VcDatastore ds,
         ConfigSpec config, IVcTaskCallback callback) throws Exception {
      AuAssert.check(isTemplate());
      AuAssert.check(rp != null && ds != null);
      // All disks of a template VM must reside on the same datastore.
      AuAssert.check(datastores.size() == 1);
      /*
       * XXX
       * Currently an incremental backup VM is cloned from the same template as
       * the one used for DBVMs. After that issue is resolved, this assert will either
       * be removed or uncommented.
       */
      //AuAssert.check(ds.getMoRef().equals(datastore[0]));
      VcDatacenter dc = rp.getVcCluster().getDatacenter();
      /*
       * To support link-clone, a snapshot of the VM must have be taken
       * before being marked as a template. We then use the snapshot to clone.
       */
      return cloneWork(dc, rp.getMoRef(), ds.getMoRef(), currentSnapshot, null, null, true,
                       name, config, callback);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneTemplate(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcDatastore, com.vmware.vim.binding.vim.vm.ConfigSpec)
    */
   @Override
   public VcVirtualMachine
   cloneTemplate(String name, VcResourcePool rp, VcDatastore ds,
         ConfigSpec config) throws Exception {
      VcTask task = cloneTemplate(name, rp, ds, config, VcCache.getRefreshVcTaskCB(rp));
      return (VcVirtualMachine)task.waitForCompletion();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneVm(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcDatastore, com.vmware.vim.binding.vim.Folder, boolean, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask cloneVm(String name, VcResourcePool rp, VcDatastore ds, Folder folder,
         boolean isLinked, IVcTaskCallback callback) throws Exception {
      VcDatacenter dc = rp.getVcCluster().getDatacenter();
      ManagedObjectReference snapMoRef = null;
      if (isLinked) {
         // To support link-clone, a snapshot of the VM must have been taken.
         // We use the current snapshot to clone.
         snapMoRef = currentSnapshot;
      }
      return cloneWork(dc, rp.getMoRef(), ds.getMoRef(), snapMoRef,
            folder == null ? null : folder._getRef(), null, isLinked, name, null, callback);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneVm(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcDatastore, com.vmware.vim.binding.vim.Folder, boolean)
    */
   @Override
   public VcVirtualMachine cloneVm(String name, VcResourcePool rp,
         VcDatastore ds, Folder folder, boolean isLinked) throws Exception {
      VcTask task = cloneVm(name, rp, ds, folder, isLinked, VcCache.getRefreshVcTaskCB(rp));
      task.waitForCompletion();
      return (VcVirtualMachine)task.getResult();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneSnapshot(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcSnapshot, com.vmware.vim.binding.vim.Folder, boolean, com.vmware.vim.binding.vim.vm.ConfigSpec, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask cloneSnapshot(String name, VcResourcePool rp, VcDatastore ds, VcSnapshot snap, Folder folder,
         VcHost host, boolean isLinked, ConfigSpec config, IVcTaskCallback callback) throws Exception {
      // no change to ds
      AuAssert.check(!isTemplate());
      AuAssert.check(snap != null);
      final VcDatacenter dc = getDatacenter();
      ManagedObjectReference dsMoRef = null;
      if (ds != null) {
         dsMoRef = ds.getMoRef();
      }
      return cloneWork(dc, rp.getMoRef(), dsMoRef, snap.getMoRef(),
            folder == null ? null : folder._getRef(), host == null ? null : host.getMoRef(), isLinked, name, config, callback);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneSnapshot(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcSnapshot, com.vmware.vim.binding.vim.Folder, boolean, com.vmware.vim.binding.vim.vm.ConfigSpec)
    */
   @Override
   public VcVirtualMachine cloneSnapshot(String name, VcResourcePool rp, VcDatastore ds, VcSnapshot snap,
         Folder folder, VcHost host, boolean isLinked, ConfigSpec config) throws Exception {
      VcTask task = cloneSnapshot(name, rp, ds, snap, folder, host, isLinked, config,
                                  VcCache.getRefreshVcTaskCB(rp));
      return (VcVirtualMachine)task.waitForCompletion();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#cloneSnapshot(java.lang.String, com.vmware.aurora.vc.VcResourcePool, com.vmware.aurora.vc.VcSnapshot, com.vmware.vim.binding.vim.Folder, boolean, com.vmware.vim.binding.vim.vm.ConfigSpec)
    */
   @Override
   public VcVirtualMachine cloneSnapshot(String name, VcResourcePool rp, VcSnapshot snap, Folder folder,
         VcHost host, boolean isLinked, ConfigSpec config) throws Exception {
      return cloneSnapshot(name, rp, null, snap, folder, host, isLinked, config);
   }


   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#createSnapshot(java.lang.String, java.lang.String, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask createSnapshot(final String name, final String description,
         final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.Snapshot,
                  vm.createSnapshot(name, description, false, false),
                  VcVirtualMachineImpl.this, callback);
         }
      });
      logger.debug("snap_vm task on " + this + ":" + name + " created");
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#createSnapshot(java.lang.String, java.lang.String)
    */
   @Override
   public VcSnapshot createSnapshot(final String name, final String description)
   throws Exception {
      VcTask task = createSnapshot(name, description, VcCache.getRefreshVcTaskCB(this));
      return (VcSnapshot)task.waitForCompletion();
   }


   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#createSnapshot(com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask removeAllSnapshots(final IVcTaskCallback callback) throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.RemoveSnap,
                  vm.removeAllSnapshots(true),
                  callback);
         }
      });
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#removeAllSnapshots()
    */
   @Override
   public void removeAllSnapshots()
   throws Exception {
      VcTask task = removeAllSnapshots(VcCache.getRefreshVcTaskCB(this));
      task.waitForCompletion();
   }

   /*
    * Insert and update all snapshots into a newMap from the oldMap.
    */
   private void updateSnapshotTree(Map<ManagedObjectReference, VcSnapshotImpl> newMap,
                                   Map<ManagedObjectReference, VcSnapshotImpl> oldMap,
                                   final SnapshotTree[] list) throws Exception {
      if (list != null) {
         for (SnapshotTree snap : list) {
            ManagedObjectReference mo = snap.getSnapshot();
            String name = snap.getName();
            VcSnapshotImpl snapObj = oldMap.get(mo);
            if (snapObj == null) {
               snapObj = VcObjectImpl.loadSnapshotFromMoRef(mo, this, name);
            } else {
               snapObj.update();
            }
            newMap.put(mo, snapObj);
            updateSnapshotTree(newMap, oldMap, snap.getChildSnapshotList());
         }
      }
   }

   /*
    * Updates all snapshots of this VM.
    */
   private synchronized void updateSnapshots(VirtualMachine vm) throws Exception {
      SnapshotInfo snapInfo = vm.getSnapshot();
      if (snapInfo != null) {
         Map<ManagedObjectReference, VcSnapshotImpl> newMap =
            new HashMap<ManagedObjectReference, VcSnapshotImpl>();
         updateSnapshotTree(newMap, snapshots, snapInfo.getRootSnapshotList());
         snapshots = newMap;
         currentSnapshot = snapInfo.getCurrentSnapshot();
      } else {
         snapshots.clear();
         currentSnapshot = null;
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getSnapshotByName(java.lang.String)
    */
   @Override
   public synchronized VcSnapshot getSnapshotByName(final String name) {
      for (VcSnapshotImpl snap : snapshots.values()) {
         if (snap.getName().equals(name)) {
            return snap;
         }
      }
      return null;
   }

   protected synchronized VcSnapshot getSnapshot(ManagedObjectReference moref) {
      VcSnapshot snap = snapshots.get(moref);
      if (snap == null) {
         throw VcException.INVALID_MOREF(MoUtil.morefToString(moref));
      }
      return snap;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getSnapshotById(String)
    */
   @Override
   public VcSnapshot getSnapshotById(String id) {
      return getSnapshot(MoUtil.stringToMoref(id));
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getCurrentSnapshot()
    */
   @Override
   public VcSnapshot getCurrentSnapshot() {
      return getSnapshot(currentSnapshot);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#reconfigure(com.vmware.vim.binding.vim.vm.ConfigSpec, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask reconfigure(final ConfigSpec spec, final IVcTaskCallback callback)
   throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            spec.setName(MoUtil.toURLString(spec.getName()));
            return new VcTask(TaskType.ReconfigVm, vm.reconfigure(spec), callback);
         }
      });
      return task;
   }

   private boolean reconfigureInt(final ConfigSpec spec) throws Exception {
      VcTask task = reconfigure(spec, VcCache.getRefreshAllVcTaskCB(this));
      task.waitForCompletion();
      setNeedsStorageInfoRefresh(true);
      return task.taskCompleted();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#reconfigure(com.vmware.vim.binding.vim.vm.ConfigSpec)
    */
   @Override
   public boolean reconfigure(final ConfigSpec spec) throws Exception {
      return safeExecVmOp(new VmOp<Boolean>() {
         public Boolean exec() throws Exception {
            return reconfigureInt(spec);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#promoteDisk(com.vmware.vim.binding.vim.vm.device.VirtualDisk, boolean, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask promoteDisk(final VirtualDisk disk, final boolean unlink,
         final IVcTaskCallback callback) throws Exception {
      final VirtualDisk[] diskArray = { disk };
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.PromoteDisks, vm.promoteDisks(unlink,
                  diskArray), callback);
         }
      });
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#promoteDisk(com.vmware.vim.binding.vim.vm.device.VirtualDisk, boolean)
    */
   @Override
   public void promoteDisk(VirtualDisk disk, boolean unlink) throws Exception {
      VcTask task = promoteDisk(disk, unlink, VcCache.getRefreshAllVcTaskCB(this));
      task.waitForCompletion();
      setNeedsStorageInfoRefresh(true);
   }

   /**
    * @see VcVirtualMachine#promoteDisks(DeviceId[], IVcTaskCallback)
    */
   @Override
   public VcTask promoteDisks(DeviceId[] diskIds,
                              final IVcTaskCallback callback) throws Exception {
      final VirtualDisk[] diskArray = new VirtualDisk[diskIds.length];
      for (int i = 0; i < diskIds.length; i++) {
         diskArray[i] = (VirtualDisk)getVirtualDevice(diskIds[i]);
         AuAssert.check(diskArray[i] != null);
      }
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.PromoteDisks, vm.promoteDisks(true,
                              diskArray), callback);
         }
      });
      return task;
   }

   @Override
   public void promoteDisks(DeviceId[] diskIds) throws Exception {
      promoteDisks(diskIds, VcCache.getRefreshAllVcTaskCB(this)).waitForCompletion();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#markAsTemplate()
    */
   @Override
   public void markAsTemplate() throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      VirtualMachine vm = getManagedObject();
      vm.markAsTemplate();
      update();
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#markAsVirtualMachine()
    */
   @Override
   public void markAsVirtualMachine(VcResourcePool rp, String hostName) throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      VirtualMachine vm = getManagedObject();
      List<VcHost> hosts = rp.getVcCluster().getHosts();
      VcHost targetHost = null;
      if (hostName != null) {
         for (VcHost host : hosts) {
            if (host.getName().equals(hostName)) {
               targetHost = host;
               break;
            }
         }
         if (targetHost == null) {
            // TODO: throw Exception
         }
      } else {
         targetHost = hosts.get(0);
      }

      vm.markAsVirtualMachine(rp.getMoRef(), targetHost.getMoRef());
      update();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getGuestVariables()
    */
   @Override
   public Map<String, String> getGuestVariables() {
      // force update to get new guest variables
      VcVirtualMachine vm = VcCache.load(getMoRef());
      // XXX We should be able to assert that vm == this,
      //     but let's delay it to post 2.0.
      Map<String, String> guestVariables = new HashMap<String, String>();
      for (OptionValue val : vm.getConfig().getExtraConfig()) {
         if (val.getKey().contains("guestinfo")) {
            if (val.getValue() != null) {
               guestVariables.put(val.getKey(), val.getValue().toString());
            } else {
               // XXX This logging should be turned off if we are no longer curious.
               logger.info("got null val on " + val.getKey());
            }
         }
      }
      return guestVariables;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getExtraConfigMap(java.lang.String)
    */
   @Override
   public Map<String, String> getExtraConfigMap(String optionKey) {
      /*
       * update() is not needed, because only the CMS server should initiate
       * machine.id updates.
       */
      for (OptionValue val : config.getExtraConfig()) {
         if (val.getKey().equals(optionKey)) {
            String value = (String) val.getValue();
            if (value == null) {
               return null;
            }
            try {
               Gson gson = new Gson();
               Type type = new TypeToken<Map<String, String>>(){}.getType();
               return gson.fromJson(value, type);
            } catch (Throwable t) {
               logger.warn("Failed to parse " + optionKey + "=" + value);
               throw AuroraException.wrapIfNeeded(t);
            }
         }
      }
      return null;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getGuestConfigs()
    */
   @Override
   public Map<String, String> getGuestConfigs() {
      Map<String, String> map = getExtraConfigMap(MACHINE_ID);
      if (map == null) {
         return new HashMap<String, String>();
      }
      return map;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getDbvmConfig()
    */
   @Override
   public Map<String, String> getDbvmConfig() throws Exception {
      Map<String, String> map = getExtraConfigMap(DBVM_CONFIG);
      if (map == null) {
         return new HashMap<String, String>();
      }
      return map;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getGuestConfig(java.lang.String)
    */
   @Override
   public String getGuestConfig(String key) {
      return getGuestConfigs().get(key);
   }

   /**
    * Set VMX extra config value encoded in JSON.
    * @param optionKey unique name of the config value
    * @param value object representing the config value
    */
   private void setExtraConfig(String optionKey, Object value)
   throws Exception {
      VcTask task = setExtraConfig(optionKey, value, VcCache.getRefreshAllVcTaskCB(this));
      task.waitForCompletion();
   }

   /**
    * The async version of setExtraConfig.
    * @param callback The callback function.
    */
   private VcTask setExtraConfig(String optionKey, Object value, final IVcTaskCallback callback)
   throws Exception {
      String jsonString = (new Gson()).toJson(value);
      ConfigSpec spec = new ConfigSpecImpl();
      OptionValue[] extraConfig = new OptionValueImpl[1];
      extraConfig[0] = new OptionValueImpl(optionKey, jsonString);
      spec.setExtraConfig(extraConfig);
      return reconfigure(spec, callback);
   }


   /**
    * Sends variables to guest via "machine.id" mechanism. Format: a JSON
    * encoded string created from Map<String, String>.
    * @param guestVariables
    */
   private void setMachineIdVariables(Map<String, String> guestVariables)
   throws Exception {
      setExtraConfig(MACHINE_ID, guestVariables);
   }

   /**
    * The async version of setMachineIdVariables.
    * @param callback The callback function.
    */
   private VcTask setMachineIdVariables(Map<String, String> guestVariables, final IVcTaskCallback callback)
   throws Exception {
      return setExtraConfig(MACHINE_ID, guestVariables, callback);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#setGuestConfigs(java.util.Map)
    */
   @Override
   public void setGuestConfigs(Map<String, String> guestVariables)
   throws Exception {
      setMachineIdVariables(guestVariables);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#setGuestConfigs(java.util.Map)
    */
   @Override
   public VcTask setGuestConfigs(Map<String, String> guestVariables, final IVcTaskCallback callback)
   throws Exception {
      return setMachineIdVariables(guestVariables, callback);
   }

   @Override
   public void setExtraConfig(Pair<String, String>[] configs) throws Exception {
      OptionValue[] extraConfigs = new OptionValueImpl[configs.length];
      for (int i = configs.length - 1; i >= 0; --i) {
         extraConfigs[i] = new OptionValueImpl(configs[i].first, configs[i].second);
      }
      ConfigSpec spec = new ConfigSpecImpl();
      spec.setExtraConfig(extraConfigs);
      reconfigure(spec);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#setDbvmConfig(java.util.Map)
    */
   @Override
   public void setDbvmConfig(Map<String, String> config)
   throws Exception {
      setExtraConfig(DBVM_CONFIG, config);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getStorageUsage()
    */
   @Override
   public DiskSize getStorageUsage() {
      return storageUsage;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getStorageCommitted()
    */
   @Override
   public DiskSize getStorageCommitted() {
      return storageCommitted;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getFileLayout()
    */
   @Override
   public FileLayoutEx getFileLayout() {
      return layoutEx;
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getIpAddresses()
    */
   @Override
   public List<String> queryIpAddresses(long timeoutMs, int expectNum) throws Exception {
      long expTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
      while (true) {
         List<String> ipAddrList = queryIpAddresses();
         /*
          * only when the size of IP address list equals to expected number,
          * stop retry.
          */
         if (ipAddrList.size() != expectNum) {
            if (System.nanoTime() > expTime) {
               throw VcException.GUEST_TIMEOUT();
            }
            Thread.sleep(2000);
         } else {
            return ipAddrList;
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#getIpAddresses()
    */
   @Override
   public List<String> queryIpAddresses() throws Exception {
      List<String> ipAddrList = new ArrayList<String>();
      VirtualMachine vm = getManagedObject();
      if (vm.getGuest() != null) {
         NicInfo[] nicInfoArray = vm.getGuest().getNet();
         if (nicInfoArray != null) {
            for (NicInfo nicInfo : nicInfoArray) {
               if (nicInfo != null && nicInfo.getIpConfig() != null &&
                     nicInfo.getIpConfig().getIpAddress() != null) {
                  for (IpAddress ip : nicInfo.getIpConfig().getIpAddress()) {
                     if (IpAddressStatus.preferred.toString().equals(ip.getState())) {
                        ipAddrList.add(ip.getIpAddress());
                     }
                  }
               }
            }
         }
      }
      return ipAddrList;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getResourcePool()
    */
   @Override
   public VcResourcePool getResourcePool() {
      if (isTemplate()) {
         // template doesn't have resource pool
         throw VcException.INVALID_ARGUMENT();
      } else {
         return VcCache.get(resourcePool);
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getParentVApp()
    */
   @Override
   public VcResourcePool getParentVApp() {
      return VcCache.get(parentVApp);
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#migrate(java.lang.String, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask migrate(final VcResourcePool rp, final IVcTaskCallback callback)
   throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.MigrateVm,
                  vm.migrate(rp.getMoRef(), null, VirtualMachine.MovePriority.defaultPriority, null),
                  callback);
         }
      });
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#migrate(java.lang.String)
    */
   @Override
   public void migrate(final VcResourcePool rp) throws Exception {
      final ManagedObjectReference oldRp = resourcePool;
      final ManagedObjectReference newRp = rp.getMoRef();
      VcTask task = migrate(rp, new IVcTaskCallback() {
         @Override
         public void completeCB(VcTask task) {
            VcCache.refresh(moRef);
            VcCache.refresh(oldRp);
            VcCache.refresh(newRp);
         }
         @Override
         public void syncCB() {
            VcCache.sync(moRef);
            VcCache.sync(oldRp);
            VcCache.sync(newRp);
         }
      });
      task.waitForCompletion();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#migrate(java.lang.String, com.vmware.aurora.vc.IVcTaskCallback)
    */
   @Override
   public VcTask migrate(final VcHost host, final IVcTaskCallback callback)
   throws Exception {
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.MigrateVm,
                  vm.migrate(null, host.getMoRef(), VirtualMachine.MovePriority.defaultPriority, null),
                  callback);
         }
      });
      return task;
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#migrate(java.lang.String)
    */
   @Override
   public void migrate(final VcHost host) throws Exception {
      VcTask task = migrate(host, new IVcTaskCallback() {
         @Override
         public void completeCB(VcTask task) {
            VcCache.refresh(moRef);
         }

         @Override
         public void syncCB() {
            VcCache.sync(moRef);
         }
      });
      task.waitForCompletion();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getCpuReservationHZ()
    */
   @Override
   public Long getCpuReservationHZ() {
      return config.getCpuAllocation().getReservation();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getMemReservationMB()
    */
   @Override
   public Long getMemReservationMB() {
      return config.getMemoryAllocation().getReservation();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getMemSizeMB()
    */
   @Override
   public Integer getMemSizeMB() {
      return config.getHardware().getMemoryMB();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getManagedBy()
    */
   @Override
   public ManagedByInfo getManagedBy() {
      return config.getManagedBy();
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#setManagedBy(java.lang.String, java.lang.String)
    */
   @Override
   public void setManagedBy(String owner, String type) throws Exception {
      VirtualMachine vm = getManagedObject();
      ConfigSpec spec = new ConfigSpecImpl();
      VmConfigUtil.addManagedByToConfigSpec(spec, owner, type);
      reconfigure(spec);
      update(vm); // propagate VC changes back to this object
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#isManagedByThisCms()
    */
   @Override
   public boolean isManagedByThisCms() {
      ManagedByInfo manager = getManagedBy();
      return manager != null
         && manager.getExtensionKey().equals(VcContext.getService().getExtensionKey())
         && manager.getType().equals("dbvm");
   }

   /* (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualmachine#getVmHAConfig()
    */
   @Override
   public VmHAConfig getVmHAConfig() {
      VmHAConfig vmHAConfig = null;
      // Since the vm cluster information is stored with the cluster, we have to retrieve it
      VcCluster cluster = getResourcePool().getVcCluster();
      vmHAConfig = cluster.getConfig().getDefaultVmHAConfig();
      DasVmConfigInfo[] dasInfo = cluster.getVmConfigInfo();
      if (dasInfo != null) {
         for (DasVmConfigInfo vmConfig : dasInfo) {
            if (vmConfig.getKey().equals(getMoRef())) {
               vmHAConfig = new VmHAConfig(vmConfig, vmHAConfig);
            }
         }
      }
      return vmHAConfig;
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#queryGuest()
    */
   @Override
   public GuestInfo queryGuest() throws Exception {
      VirtualMachine vm = getManagedObject();
      return vm.getGuest();
   }

   /*
    * (non-Javadoc)
    * @see com.vmware.aurora.vc.VcVirtualMachine#getSnapshots()
    */
   @Override
   public synchronized List<VcSnapshot> getSnapshots() {
      return new ArrayList<VcSnapshot>(snapshots.values());
   }

   @Override
   public GuestVarReturnCode waitForPowerOnResult(Integer timeOutSecs) throws Exception {
      long finishNanos = 0;
      Map<String, String> guestVariables;
      GuestVarReturnCode returnCode;
      if (timeOutSecs != null) {
         finishNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeOutSecs);
      }
      while (true) {
         try {
            guestVariables = this.getGuestVariables();
         } catch (Exception ex) {
            logger.error("Failed to get guest variables on VM " + this.getName(), ex);
            throw GuestVariableException.COMMUNICATION_ERROR(ex);
         }

         if (guestVariables != null && guestVariables.get("guestinfo.return_code") != null) {
            returnCode = new GuestVarReturnCode(guestVariables);
            if (!returnCode.isBusy()) {
               break;
            }
         }

         /*
          * Abort wait in two cases:
          * - target vm is powered-off
          *   Unexpected, we generally wait for results from a VM that was just powered-on.
          *   The fix is likely possible only via "Repair" and might take a while, so don't
          *   tie up this thread.
          * - target vm is powered-on, but is truly stuck, so time-out.
          */

         /* getGuestVariable() gets external vc event and will refresh the power state.
          * It doesn't update runtime state. */
         if (isPoweredOff()) {
            logger.warn("waitForResult() aborted for powered-off vm " + this.getName());
            throw GuestVariableException.POWERED_OFF();
         }
         if (timeOutSecs != null && System.nanoTime() >= finishNanos) {
            logger.warn("waitForStartupResult() aborted due to time-out for vm" + this.getName());
            throw GuestVariableException.TIMEOUT();
         }
         Thread.sleep(GUEST_VAR_CHECK_INTERVAL);
      }

      logger.info(returnCode.getGuestVariables() + " guest variables are returned from vm " + this.getName() +
            " return code: " + returnCode.getStatusMsg());
      if (returnCode.isError()) {
         throw GuestVariableException.RETURN_CODE_ERROR(returnCode.getStatusMsg());
      }
      return returnCode;
   }

   /**
    * Sets the needsStorageInfoRefresh flag.
    * @param value True if the storage info needs to be refreshed.
    */
   private void setNeedsStorageInfoRefresh(boolean value) {
      this.needsStorageInfoRefresh = value;
   }

   private boolean needsStorageInfoRefresh() {
      return this.needsStorageInfoRefresh;
   }

   @Override
   public void updateVmNic(String pubNICLabel, String privNICLabel,
                           String pubNetId, String privNetId) throws Exception {
      List<VirtualDeviceSpec> changes = new ArrayList<VirtualDeviceSpec>();
      if (pubNetId != null) {
         VcNetwork pubNet = VcCache.get(pubNetId);
         if (pubNet != null) {
            changes.add(this.reconfigNetworkSpec(pubNICLabel, pubNet));
         }
      }
      VcNetwork privNet = VcCache.get(privNetId);
      changes.add(this.reconfigNetworkSpec(privNICLabel, privNet));
      this.reconfigure(VmConfigUtil.createConfigSpec(changes));
   }

   @Override
   public void detachAllCdroms() throws Exception {
      AuAssert.check(VcContext.isInTaskSession());
      List<VirtualDeviceSpec> changes = new ArrayList<VirtualDeviceSpec>();
      for (VirtualDevice device : getDevice()) {
         if (device instanceof VirtualCdrom) {
            VirtualDeviceSpec spec = new VirtualDeviceSpecImpl();
            spec.setDevice(device);
            spec.setOperation(VirtualDeviceSpec.Operation.remove);
            changes.add(spec);
         }
      }
      if (!changes.isEmpty()) {
         ConfigSpec config = new ConfigSpecImpl();
         config.setDeviceChange(changes.toArray(new VirtualDeviceSpec[changes.size()]));
         reconfigure(config);
      }
   }

   @SuppressWarnings("deprecation")
   @Override
   public void modifyHASettings(RestartPriority restartPriority, IsolationResponse isolationResponse,
         VmMonitoringState vmMonitoringState) throws Exception {
      AuAssert.check(VcContext.isInTaskSession());

      ClusterComputeResource cluster = MoUtil.getManagedObject(getResourcePool().getVcCluster().getMoRef());
      DasVmSettings dasVmSettings = null;
      boolean found = false;
      DasVmConfigInfo[] dasVmConfig = cluster.getConfiguration().getDasVmConfig();
      if (dasVmConfig != null) {
         for (DasVmConfigInfo iter : dasVmConfig) {
            if (iter.getKey().equals(getMoRef())) {
               found = true;
               dasVmSettings = iter.getDasSettings();
               break;
            }
         }
      }

      if (dasVmSettings == null) {
         dasVmSettings = new DasVmSettingsImpl();
      }
      if (restartPriority != null) {
         dasVmSettings.setRestartPriority(restartPriority.name());
      }
      if (isolationResponse != null) {
         dasVmSettings.setIsolationResponse(isolationResponse.name());
      }

      VmToolsMonitoringSettings vmToolsMonitoringSettings = dasVmSettings.getVmToolsMonitoringSettings();
      if (vmToolsMonitoringSettings == null) {
         // Use the default settings for VmToolsMonitoringSettings
         vmToolsMonitoringSettings = cluster.getConfiguration().getDasConfig().getDefaultVmSettings().getVmToolsMonitoringSettings();
         dasVmSettings.setVmToolsMonitoringSettings(vmToolsMonitoringSettings);
      }

      if (vmMonitoringState != null) {
         vmToolsMonitoringSettings.setVmMonitoring(vmMonitoringState.name());
      }

      DasVmConfigInfo dasVmConfigInfo = new DasVmConfigInfoImpl();
      dasVmConfigInfo.setKey(getMoRef());
      dasVmConfigInfo.setDasSettings(dasVmSettings);
      ConfigSpecExImpl configSpec = new ConfigSpecExImpl();
      configSpec.setDasVmConfigSpec(new DasVmConfigSpec[] {new DasVmConfigSpecImpl(found? Operation.edit : Operation.add, null, dasVmConfigInfo)});
      getResourcePool().getVcCluster().reconfigure(configSpec);
   }

   @Override
   public VcTask turnOnFT(final VcHost host, final IVcTaskCallback callback) throws Exception {
      return VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.TurnOnFT,
                  vm.createSecondary(host == null ? null : host.getMoRef()),
                  callback);
         }
      });
   }

   @Override
   public void turnOnFT(VcHost host) throws Exception {
      VcTask task = turnOnFT(host, new IVcTaskCallback () {
         @Override
         public void completeCB(VcTask task) {
            VcCache.refresh(moRef);
         }
         @Override
         public void syncCB() {
            VcCache.sync(moRef);
         }
      });

      task.waitForCompletion();
   }

   @Override
   public VcTask turnOffFT(final IVcTaskCallback callback) throws Exception {
      return VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            VirtualMachine vm = getManagedObject();
            return new VcTask(TaskType.TurnOffFT,
                  vm.turnOffFaultTolerance(),
                  callback);
         }
      });
   }

   @Override
   public void turnOffFT() throws Exception {
      VcTask task = turnOffFT(new IVcTaskCallback () {
         @Override
         public void completeCB(VcTask task) {
            VcCache.refresh(moRef);
         }
         @Override
         public void syncCB() {
            VcCache.sync(moRef);
         }
      });

      task.waitForCompletion();
   }

   private VcTask toggleFT(final boolean enable, final IVcTaskCallback callback,
         final VirtualMachine primaryVm, final ManagedObjectReference secondaryVMRef) throws Exception {
      return VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            return new VcTask(enable ? TaskType.EnableFT : TaskType.DisableFT,
                  enable ? primaryVm.enableSecondary(secondaryVMRef, null) : primaryVm.disableSecondary(secondaryVMRef),
                  callback);
         }
      });
   }

   private void toggleFT(final boolean enable) throws Exception {
      VcTask task = toggleFT(enable, new IVcTaskCallback () {
         @Override
         public void completeCB(VcTask task) {
            VcCache.refresh(moRef);
         }
         @Override
         public void syncCB() {
            VcCache.sync(moRef);
         }
      });
      task.waitForCompletion();
   }

   private VcTask toggleFT(final boolean enable, final IVcTaskCallback callback) throws Exception {
      ManagedObjectReference secondaryVMRef = null;
      VirtualMachine vm = getManagedObject();
      FaultToleranceConfigInfo ftConfigInfo = vm.getConfig().getFtInfo();
      if (ftConfigInfo instanceof FaultTolerancePrimaryConfigInfo) {
         FaultTolerancePrimaryConfigInfo primaryFtConfigInfo = (FaultTolerancePrimaryConfigInfo)ftConfigInfo;
         AuAssert.check(primaryFtConfigInfo.getSecondaries().length == 1);
         secondaryVMRef = primaryFtConfigInfo.getSecondaries()[0];
      } else {
         AuAssert.check(false, "Should not reach here");
      }

      return toggleFT(enable, callback, vm, secondaryVMRef);
   }

   @Override
   public VcTask enableFT(IVcTaskCallback callback) throws Exception {
      return toggleFT(true, callback);
   }

   @Override
   public void enableFT() throws Exception {
      toggleFT(true);
   }

   @Override
   public VcTask disableFT(final IVcTaskCallback callback) throws Exception {
      return toggleFT(false, callback);
   }

   @Override
   public void disableFT() throws Exception {
      toggleFT(false);
   }

   @Override
   public VcTask disableDrs() throws Exception {
      AuAssert.check(VcContext.isInTaskSession());

      ClusterComputeResource cluster = MoUtil.getManagedObject(getResourcePool().getVcCluster().getMoRef());
      boolean found = false;
      DrsVmConfigInfo[] drsVmConfig = cluster.getConfiguration().getDrsVmConfig();
      if (drsVmConfig != null) {
         for (DrsVmConfigInfo iter : drsVmConfig) {
            if (iter.getKey().equals(getMoRef())) {
               found = true;
               break;
            }
         }
      }

      DrsVmConfigInfo drsVmConfigInfo = new DrsVmConfigInfoImpl();
      drsVmConfigInfo.setKey(getMoRef());
      drsVmConfigInfo.setEnabled(false);

      ConfigSpecExImpl configSpec = new ConfigSpecExImpl();
      configSpec.setDrsVmConfigSpec(new DrsVmConfigSpec[] {new DrsVmConfigSpecImpl(found? Operation.edit : Operation.add, null, drsVmConfigInfo)});
      return getResourcePool().getVcCluster().reconfigure(configSpec, VcCache.getRefreshRuntimeVcTaskCB(this));
   }

   @Override
   public VcVirtualMachine cloneVm(final CreateSpec vmSpec,
                       final DeviceId[] removeDisks) throws Exception {
      VcTask task = cloneVmAsync(vmSpec, removeDisks);
      return ((VcVirtualMachine)task.waitForCompletion());
   }

   @Override
   public VcTask cloneVmAsync(final CreateSpec vmSpec,
                                    final DeviceId[] removeDisks) throws Exception {
      final VcSnapshot parentVcSnap = vmSpec.getParentSnapshot();
      final ConfigSpec configSpec =
         (vmSpec.spec != null ? vmSpec.spec : new ConfigSpecImpl());
      List<VirtualDeviceSpec> devChanges = new ArrayList<VirtualDeviceSpec>();

      /*
       * No device changes should be set already.
       */
      if (configSpec.getDeviceChange() != null &&
           configSpec.getDeviceChange().length > 0) {
         throw AuAssert.INTERNAL();
      }
      /*
       * Append config for removing disks.
       */
      if (removeDisks != null) {
         for (DeviceId deviceId : removeDisks) {
            VirtualDevice dev = parentVcSnap.getVirtualDevice(deviceId);
            if (dev != null) {
               devChanges.add(VmConfigUtil.removeDeviceSpec(dev));
            }
         }
      }
      if (!devChanges.isEmpty()) {
         configSpec.setDeviceChange(devChanges.toArray(new VirtualDeviceSpec[devChanges.size()]));
      }

      switch (vmSpec.cloneType) {
         case VMFORK:
            AuAssert.check(ArrayUtils.isEmpty(configSpec.getDeviceChange()), "Vmfork doesn't allow change disks.");
            if(vmSpec.persisted) {
               AuAssert.check(vmSpec.parentSnap != null, "Cannot create persistent fork child from a virtual machine which does not have a disk snapshot.");
            }
            AuAssert.check(isQuiescedForkParent(), "VM is not quiesced.");

            return createForkChild(vmSpec.name, vmSpec.rp, vmSpec.ds,
                  vmSpec.folder, vmSpec.persisted, VcCache.getRefreshVcTaskCB(vmSpec.rp));
         case FULL:
            return vmSpec.getParentVm().cloneSnapshot(vmSpec.name, vmSpec.rp, vmSpec.ds,
                  parentVcSnap, vmSpec.folder, vmSpec.host, false/*not linked*/, configSpec, VcCache.getRefreshVcTaskCB(vmSpec.rp));
         case LINKED:
            return vmSpec.getParentVm().cloneSnapshot(vmSpec.name, vmSpec.rp, vmSpec.ds,
                  parentVcSnap, vmSpec.folder, vmSpec.host, true/*linked*/, configSpec, VcCache.getRefreshVcTaskCB(vmSpec.rp));
         default:
            throw AuAssert.INTERNAL(new RuntimeException("Unsupported Clone Type: " + vmSpec.cloneType));
      }
   }

   @Override
   public VcTask createForkChild(final String name, VcResourcePool rp, VcDatastore ds, final Folder folder,
                                    boolean persisted, final IVcTaskCallback callback) throws Exception {
      // no change to ds
      AuAssert.check(!isTemplate());

      RelocateSpec relocSpec = new RelocateSpecImpl();
      relocSpec.setPool(rp.getMoRef());
      relocSpec.setDatastore(ds != null ? ds.getMoRef() : null);
      relocSpec.setFolder(folder == null ? getDatacenter().getVmFolderMoRef() : folder._getRef());

      final CreateChildSpec spec = new CreateChildSpecImpl();
      spec.setLocation(relocSpec);
      spec.setPersistent(persisted);

      final VirtualMachine vm = getManagedObject();
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         public VcTask body() throws Exception {
            return new VcTask(TaskType.CreateForkChild, vm.createForkChild(name, spec), callback);
         }
      });

      return task;
   }



   @Override
   public void enableForkParent() {
      VirtualMachine vm = this.getManagedObject();
      vm.enableForkParent();
   }

   private void waitReady(int maxWaitSeconds) {
      if(this.isQuiescedForkParent()) {
         logger.debug("VM is quiesced.");
         return;
      }

      long remainWaitMilSecs = maxWaitSeconds * 1000;
      boolean quiesced = false;
      do{
         logger.debug("Parent is not quiesced, wait 5 seconds to check.");
         try {
            Thread.sleep(5000);
         } catch (InterruptedException e) {
            logger.error(e);
         }
         remainWaitMilSecs -= 5000;

         quiesced = this.isQuiescedForkParent();
         logger.debug("Is VM quiesced? " + quiesced);
      }  while (!quiesced && remainWaitMilSecs > 0);




      AuAssert.check(quiesced, "VM is not quiesced.");
   }

   /* (non-Javadoc)
 * @see com.vmware.aurora.vc.VcVirtualMachine#isQuiescedForkParent()
 */
   @Override
   public boolean isQuiescedForkParent() {
      VirtualMachine vm = this.getManagedObject();
      RuntimeInfo runtime = vm.getRuntime();
      Boolean quiesced = runtime.getQuiescedForkParent();

      return (quiesced == null ? false : quiesced);
   }



   /**
    * Change the VM disks layout.
    *
    * @param removeDisks disks to be removed
    * @param addDisks    disks to be added
    */
   @Override
   public void changeDisks(final DeviceId[] removeDisks, final DiskCreateSpec[] addDisks) throws Exception {
      final ConfigSpec configSpec = new ConfigSpecImpl();
      final List<VirtualDeviceSpec> devChanges = new ArrayList<VirtualDeviceSpec>();
      if (removeDisks != null) {
         for (DeviceId deviceId : removeDisks) {
            VirtualDevice dev = getVirtualDevice(deviceId);
            if (dev != null) {
               devChanges.add(VmConfigUtil.removeDeviceSpec(dev));
            }
         }
      }

      if (addDisks != null) {
         for (DiskCreateSpec spec : addDisks) {
            devChanges.add(spec.getVcSpec(VcVirtualMachineImpl.this));
         }
      }

      configSpec.setDeviceChange(devChanges.toArray(new VirtualDeviceSpec[devChanges.size()]));
      reconfigure(configSpec);
   }

   public Folder getParentFolder() {
      VirtualMachine vm = this.getManagedObject();
      ManagedObjectReference mo = vm.getParent();
      if (mo != null) {
         return MoUtil.getManagedObject(mo);
      } else {
         return null;
      }
   }
}
