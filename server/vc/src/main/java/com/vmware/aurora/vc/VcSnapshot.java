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

import java.util.HashMap;
import java.util.Map;

import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.VcVirtualMachineImpl.VmOp;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.vm.ConfigInfo;
import com.vmware.vim.binding.vim.vm.FileLayoutEx;
import com.vmware.vim.binding.vim.vm.FileLayoutEx.DiskLayout;
import com.vmware.vim.binding.vim.vm.FileLayoutEx.DiskUnit;
import com.vmware.vim.binding.vim.vm.FileLayoutEx.FileInfo;
import com.vmware.vim.binding.vim.vm.FileLayoutEx.SnapshotLayout;
import com.vmware.vim.binding.vim.vm.Snapshot;
import com.vmware.vim.binding.vim.vm.device.VirtualDevice;
import com.vmware.vim.binding.vim.vm.device.VirtualDisk;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;

public interface VcSnapshot extends VcVmBase {

   /**
    * Remove a snapshot asynchronously.
    * @throws Exception
    * @return the task
    */
   public VcTask remove(final IVcTaskCallback callback) throws Exception;

   /**
    * Remove a snapshot.
    * @throws Exception
    */
   public void remove() throws Exception;

   /**
    * Return parent Vm
    */
   public VcVirtualMachine getVm();

   /**
    * @return the name of the snapshot
    */
   public String getName();

   /**
    * Gets the size of the snapshot on data storage.
    * @return The size.
    */
   public DiskSize getSizeOnData(DiskSchema disSchema);

   /**
    * Get the size details of each disks in the snapshot.
    * @return The size.
    */
   public Map<DeviceId, DiskSize> getSizeDetail();

   /**
    * Gets the size of the snapshot on all storages.
    * @return The size.
    */
   public DiskSize getSize();

   /**
    * Get the vmsn file size of the snapshot.
    */
   public DiskSize getVmsnFileSize();

   /**
    * Revert to a snapshot.
    * @param callback (optional) call-back function for the task
    * @return the task for revert to the snapshot
    * @throws Exception
    */
   VcTask revert(final IVcTaskCallback callback) throws Exception;

   void revert() throws Exception;

}

@SuppressWarnings("serial")
class VcSnapshotImpl extends VcVmBaseImpl implements VcSnapshot {
   private final VcVirtualMachineImpl vm;
   private final String name;
   private ConfigInfo config;
   private DiskSize size;
   private Map<DeviceId, DiskSize> sizeDetail;
   private DiskSize vmsnFileSize;

   @Override
   protected void update(ManagedObject mo) {
      final Snapshot snap = (Snapshot)mo;
      config = snap.getConfig();
      updateSize();
   }

   /*
    * There is no way to find out the VM from VC's Snapshot moref,
    * so we add it during initialization.
    */
   protected VcSnapshotImpl(Snapshot mo, VcVirtualMachineImpl vm, String name) {
      super(mo);
      AuAssert.check(vm != null);
      this.vm = vm;
      this.name = name;
      update(mo);
   }

   @Override
   protected ConfigInfo getConfig() {
      return config;
   }

   @Override
   public String getName() {
      return MoUtil.fromURLString(name);
   }

   public VcVirtualMachine getVm() {
      return vm;
   }

   @Override
   public VcDatacenter getDatacenter() {
      return vm.getDatacenter();
   }

   @Override
   public VcTask remove(final IVcTaskCallback callback) throws Exception {
      // remove child snaps=false, consolidate=true
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            Snapshot snap = getManagedObject();
            return new VcTask(TaskType.RemoveSnap, snap.remove(false, true), callback);
         }
      });
      return task;
   }

   void removeInt() throws Exception {
      try {
         VcTask task = remove(VcCache.getRefreshVcTaskCB(vm));
         task.waitForCompletion();
      } catch (ManagedObjectNotFound e) {
         // The object was gone. Nothing to do.
         logger.info("cannot destroy " + this + ", not found.");
      }
   }

   @Override
   public void remove() throws Exception {
      vm.safeExecVmOp(new VmOp<Void>() {
         public Void exec() throws Exception {
            removeInt();
            return null;
         }
      });
   }

   private VirtualDevice findDevice(VirtualDevice[] devices, int key) {
      for (VirtualDevice device : devices) {
         if (device.getKey() == key) {
            return device;
         }
      }
      return null;
   }

   /**
    * Calculate size of snapshot files.
    *
    * Note: This is done at configuration time,
    *       so it won't get updated if file size ever changes.
    */
   private void updateSize() {
      FileLayoutEx fileLayout = vm.getFileLayout();

      // create file map to avoid unnecessary loops
      Map<Integer, FileInfo> fileMap = new HashMap<Integer, FileInfo>();
      if (fileLayout.getFile() != null) {
         for (FileInfo fileInfo : fileLayout.getFile()) {
            fileMap.put(fileInfo.getKey(), fileInfo);
         }
      }

      sizeDetail = new HashMap<DeviceId, DiskSize>();
      size = new DiskSize(0);

      if (fileLayout.getSnapshot() == null) {
         logger.info("missing snapshot layout in " + vm);
         return;
      }
      // search for the snapshot's files
      for (SnapshotLayout snapshotLayout : fileLayout.getSnapshot()) {
         if (getMoRef().equals(snapshotLayout.getKey())) {
            // loop through each disk and count the last diskunit in the chain only
            // because the previous ones are shared with the other snapshots so don't double counting
            VirtualDevice[] devices = getConfig().getHardware().getDevice();
            for(DiskLayout disk : snapshotLayout.getDisk()) {
               VirtualDisk vdisk = (VirtualDisk)findDevice(devices, disk.getKey());
               String diskMode = ((VirtualDisk.FlatVer2BackingInfo)vdisk.getBacking()).getDiskMode();
               // independent disks are not included in snapshot
               if (!diskMode.equals(DiskMode.independent_nonpersistent.name()) &&
                   !diskMode.equals(DiskMode.independent_persistent.name())) {
                  DiskUnit[] diskUnits = disk.getChain();
                  int[] fileKeys = diskUnits[diskUnits.length - 1].getFileKey();

                  // count the total of each file's size
                  long total = 0;
                  for (int fileKey : fileKeys) {
                     FileInfo fileInfo = fileMap.get(fileKey);
                     if (fileInfo != null) {
                        total += fileInfo.getSize();
                     }
                  }

                  // add up
                  size.add(total);
                  DeviceId deviceId =
                        new DeviceId(findDevice(devices, vdisk.getControllerKey()), vdisk);
                  sizeDetail.put(deviceId, new DiskSize(total));
               }
            }

            // add vmsn file size which is always on data storage
            FileInfo fileInfo = fileMap.get(snapshotLayout.getDataKey());
            if (fileInfo != null) {
               size.add(fileInfo.getSize());
               vmsnFileSize = new DiskSize(fileInfo.getSize());
            }
            return;
         }
      }
      // no matching file layout for this snapshot?
   }

   @Override
   public DiskSize getSize() {
      return size;
   }

   @Override
   public DiskSize getSizeOnData(DiskSchema diskSchema) {
      DiskSize dataSize = new DiskSize(0);
      Map<DeviceId, DiskSize> sizeDetail = getSizeDetail();
      for (DiskSpec diskSpec : diskSchema.getDiskSpecs()) {
         // only the snapshot of the archive disk is on backup storage since V1.0
         if (!diskSpec.getDiskType().equals(DiskType.Archive)) {
            DiskSize size = sizeDetail.get(diskSpec.getDeviceId());
            if (size != null) {
               dataSize.add(size);
            }
         }
      }
      dataSize.add(getVmsnFileSize());
      return dataSize;
   }

   @Override
   public Map<DeviceId, DiskSize> getSizeDetail() {
      return sizeDetail;
   }

   @Override
   public DiskSize getVmsnFileSize() {
      return vmsnFileSize;
   }

   @Override
   public VcTask revert(final IVcTaskCallback callback) throws Exception {
      // revert to snapshot: host=null, suppressPowerOn=false
      VcTask task = VcContext.getTaskMgr().execute(new IVcTaskBody() {
         @Override
         public VcTask body() throws Exception {
            final Snapshot snap = getManagedObject();
            return new VcTask(TaskType.RevertSanp, snap.revert(null, false), callback);
         }
      });
      return task;
   }

   @Override
   public void revert() throws Exception {
      try {
         VcTask task = revert(VcCache.getRefreshVcTaskCB(vm));
         task.waitForCompletion();
      } catch (ManagedObjectNotFound e) {
         // The object was gone. Nothing to do.
         logger.info("cannot revert to " + this + ", not found.");
      }
   }

   /**
    * Extends VcObject serialization proxy with extra parameters.
    */
   protected static class SerializationProxy extends VcObjectImpl.SerializationProxy
   {
       private final ManagedObjectReference vmMoref;

       private SerializationProxy(VcSnapshotImpl snapshot) {
          super(snapshot);
          vmMoref = snapshot.vm.getMoRef();
       }

       @Override
       protected final VcObject getCachedObject() {
          VcVirtualMachineImpl vm = VcCache.get(vmMoref);
          return vm.getSnapshot(moRef);
       }
   }

   @Override
   protected Object writeReplace() {
       return new SerializationProxy(this);
   }

}
