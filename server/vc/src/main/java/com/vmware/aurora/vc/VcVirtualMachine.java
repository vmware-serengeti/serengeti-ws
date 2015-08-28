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
import java.util.List;
import java.util.Map;

import com.google.gson.internal.Pair;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.aurora.vc.VcClusterConfig.VmHAConfig;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.VirtualMachine.ConnectionState;
import com.vmware.vim.binding.vim.VirtualMachine.DiskChangeInfo;
import com.vmware.vim.binding.vim.VirtualMachine.FaultToleranceState;
import com.vmware.vim.binding.vim.cluster.DasConfigInfo.VmMonitoringState;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.IsolationResponse;
import com.vmware.vim.binding.vim.cluster.DasVmSettings.RestartPriority;
import com.vmware.vim.binding.vim.ext.ManagedByInfo;
import com.vmware.vim.binding.vim.vApp.VmConfigInfo;
import com.vmware.vim.binding.vim.vm.*;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice.BackingInfo;
import com.vmware.vim.binding.vim.vm.device.VirtualDeviceSpec;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

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

         if(cloneType != VcVmCloneType.INSTANT
               || (cloneType == VcVmCloneType.INSTANT && persisted)) {
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
    * Change the VM disks layout.
    *
    * @param removeDisks disks to be removed
    * @param addDisks    disks to be added
    */
   public void changeDisks(final DeviceId[] removeDisks, final DiskCreateSpec[] addDisks) throws Exception;

   public Folder getParentFolder();
}

