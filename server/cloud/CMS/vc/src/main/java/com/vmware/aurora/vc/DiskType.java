/* ***************************************************************************
 * Copyright (c) 2012 VMware, Inc.  All rights reserved.
 * -- VMware Confidential
 * ***************************************************************************/
package com.vmware.aurora.vc;

import java.util.Date;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.global.DiskSize;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

/**
 * <code>DiskType</code> defines the content type that the disk is designed for.
 */
public enum DiskType {

   OS(false, "root.vmdk",                                                        // Operating system
         DiskMode.independent_persistent, false, false,
         null, Configuration.getDouble("dbvm.rootDiskAlpha", 0.4), "OS"),
   Binary(false, "binary.vmdk",                                                  // Database binaries
         DiskMode.independent_persistent, false, false,
         null, Configuration.getDouble("dbvm.dbserverDiskAlpha", 0.4), "Binary"),
   Agent(false, "agent.vmdk",                                                    // TODO: This will be removed in Borealis
         DiskMode.independent_persistent, false, false,
         null, 1.0, "Agent"),
   MemorySwap(false, Configuration.getString("dbvm.swapDiskName", "swap.vmdk"),  // Linux swap or Windows virtual memory
         DiskMode.independent_persistent, false, true,
         null, Configuration.getDouble("dbvm.swapDiskAlpha", 1.0), "MemorySwap"),
   Data(false, Configuration.getString("dbvm.dataDiskName", "data.vmdk"),        // Database data
         DiskMode.persistent, Configuration.getBoolean("dbvm.dataDiskThinMode", true), true,
         null, Configuration.getDouble("dbvm.dataDiskAlpha", 1.0), "Data"),
   Archive(false, Configuration.getString("dbvm.archiveDiskName", "archive.vmdk"), // Database archive for transaction log
         DiskMode.persistent, Configuration.getBoolean("dbvm.archiveDiskThinMode", true), true,
         null, Configuration.getDouble("dbvm.archiveDiskAlpha", 1.0), "Archive"),
   Log(false, Configuration.getString("dbvm.logDiskName", "log.vmdk"),           // System logs
         DiskMode.independent_persistent, false, false /*TODO: /var cannot simply remount*/,
         DiskSize.sizeFromMB(400), Configuration.getDouble("dbvm.logDiskAlpha", 1.0), "Log"),
   Diagnostics(false, Configuration.getString("dbvm.diagDiskName", "diagnostics.vmdk"), // System diagnostics (for example, dumps)
         DiskMode.independent_persistent, true, true,
         DiskSize.sizeFromMB(3072), Configuration.getDouble("dbvm.diagDiskAlpha", 0.05), "Diagnostics"),
   BackupSrc(true, Configuration.getString("backupVm.backupSrcDiskName", "backupSrc.vmdk"), // Incremental backup disk
         DiskMode.independent_persistent, false, false,
         DiskSize.sizeFromMB(1), Configuration.getDouble("backupVm.backupSrcDiskAlpha", 1.0), "BackupSrc"),
   OldBinary(false, Configuration.getString("dbvm.oldBinaryDiskName", "oldbinary.vmdk"),  // Old database binaries
         DiskMode.independent_persistent, false, false,
         null, Configuration.getDouble("dbvm.dbserverDiskAlpha", 0.0), "OldBinary"),
   OldData(false, Configuration.getString("dbvm.oldDataDiskName", "olddata.vmdk"),          // Old database data
         DiskMode.independent_persistent, Configuration.getBoolean("dbvm.dataDiskThinMode", true), false,
         null, Configuration.getDouble("dbvm.dataDiskAlpha", 1.0), "OldData");

   private final boolean internalOnly;
   private final String diskName;
   private final DiskMode diskMode;
   private final boolean thinDisk;
   private final boolean needToCreate; // Create, if not a part of the DBVM template.
   private final DiskSize defaultCapacity;
   /*
    * alpha is the storage accounting factor. Currently unused, but this can be used
    * once we finalize on how to do storage accounting for various disks. Perhaps
    * separate defaultAlpha and currentAlpha fields may be needed depending on
    * the user-visible operations for setting/resetting alpha. It can also be changed
    * to "float" from "Float" and use 1 for disks where it's not applicable.
    */
   private final double defaultAlpha;
   private final String typeName;

   private DiskType(boolean internalOnly, String diskName,
         DiskMode diskMode, boolean thinDisk, boolean needToCreate,
         DiskSize defaultCapacity, double defaultAlpha, String typeName) {
      this.internalOnly = internalOnly;
      this.diskName = diskName;
      this.diskMode = diskMode;
      this.thinDisk = thinDisk;
      this.needToCreate = needToCreate;
      this.defaultCapacity = defaultCapacity;
      this.defaultAlpha = defaultAlpha;
      this.typeName = typeName;
   }

   /**
    * @return whether a disk is for CMS internal use only. Return true if the disk
    * won't show up when building DBVM template and shouldn't be defined by user in
    * disk schema.
    */
   public boolean isInternalOnly() {
      return internalOnly;
   }

   public String getDiskName() {
      return diskName;
   }
  
   /**
    * @return the prefix of the vmdk file name, i.e. data.vmdk => data.
    */
   public String getDiskNamePrefix() {
      int index = getDiskName().lastIndexOf('.');
      if (index > 0) {
         return getDiskName().substring(0, index);
      } else {
         return getDiskName();
      }
   }

   public DiskMode getDiskMode() {
      return diskMode;
   }

   public boolean isThinDisk() {
      return thinDisk;
   }

   public boolean isNeedToCreate() {
      return needToCreate;
   }

   public DiskSize getDefaultCapacity() {
      return defaultCapacity;
   }

   public double getDefaultAlpha() {
      return defaultAlpha;
   }

   // There is many disk operations that require a temporary snapshot.
   // This method return a valid snapshot name with time information.
   public String getTmpSnapName () {
      return "tmp" + getDiskNamePrefix() + "Snap-" + new Date();
   }

   public String getTypeName() {
      return this.typeName;
   }

   public static DiskType getDiskType(String diskType) {
      for (DiskType type : getDiskTypes()) {
         if (type.getTypeName().equals(diskType)) {
            return type;
         }
      }
      return null;
   }

   private static DiskType[] getDiskTypes() {
      return new DiskType[]{OS, Binary, Agent, MemorySwap, Data, Archive, Log, Diagnostics, BackupSrc, OldBinary, OldData};
   }
}
