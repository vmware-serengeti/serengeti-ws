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
package com.vmware.bdd.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vmware.bdd.apitypes.StorageRead.DiskType;
import com.vmware.bdd.spectypes.DiskSpec;

/**
 * Disk Entity class: disk infos for a node entity
 */
@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "disk_seq", allocationSize = 1)
@Table(name = "disk")
public class DiskEntity extends EntityBase {
   private static final String SYSTEM_DISK_ADDRESS =
         "VirtualLsiLogicController:0:0";

   @Column(name = "name")
   private String name;

   @Column(name = "size")
   private int sizeInMB;

   // OS/SWAP/DATA
   @Column(name = "disk_type")
   private String diskType;

   // thick/thin/lazy_zero etc.
   @Column(name = "alloc_type")
   private String allocType;

   // independent/persistent etc.
   @Column(name = "disk_mode")
   private String diskMode;

   // path to find vmdk
   @Column(name = "external_addr")
   private String externalAddress;

   @Column(name = "ds_name")
   private String datastoreName;

   @Column(name = "ds_moid")
   private String datastoreMoId;

   // uuid to identify this disk
   @Column(name = "hardware_uuid")
   private String hardwareUUID;

   @Column(name = "vmdk_path")
   private String vmdkPath;

   @ManyToOne
   @JoinColumn(name = "node_id")
   private NodeEntity nodeEntity;

   public DiskEntity() {

   }

   public DiskEntity(String name) {
      super();
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getSizeInMB() {
      return sizeInMB;
   }

   public void setSizeInMB(int sizeInMB) {
      this.sizeInMB = sizeInMB;
   }

   public String getDiskType() {
      return diskType;
   }

   public void setDiskType(String diskType) {
      this.diskType = diskType;
   }

   public String getAllocType() {
      return allocType;
   }

   public void setAllocType(String allocType) {
      this.allocType = allocType;
   }

   public String getDiskMode() {
      return diskMode;
   }

   public void setDiskMode(String diskMode) {
      this.diskMode = diskMode;
   }

   public String getExternalAddress() {
      return externalAddress;
   }

   public void setExternalAddress(String externalAddress) {
      this.externalAddress = externalAddress;
   }

   public String getDatastoreName() {
      return datastoreName;
   }

   public void setDatastoreName(String datastoreName) {
      this.datastoreName = datastoreName;
   }

   public String getDatastoreMoId() {
      return datastoreMoId;
   }

   public void setDatastoreMoId(String datastoreMoId) {
      this.datastoreMoId = datastoreMoId;
   }

   public String getHardwareUUID() {
      return hardwareUUID;
   }

   public void setHardwareUUID(String hardwareUUID) {
      this.hardwareUUID = hardwareUUID;
   }

   public String getVmdkPath() {
      return vmdkPath;
   }

   public void setVmdkPath(String vmkdPath) {
      this.vmdkPath = vmkdPath;
   }

   public NodeEntity getNodeEntity() {
      return nodeEntity;
   }

   public void setNodeEntity(NodeEntity nodeEntity) {
      this.nodeEntity = nodeEntity;
   }

   public DiskEntity copy(DiskEntity other) {
      DiskEntity disk = new DiskEntity();
      disk.name = other.name;
      disk.sizeInMB = other.sizeInMB;
      disk.allocType = other.allocType;
      disk.datastoreMoId = other.datastoreMoId;
      disk.datastoreName = other.datastoreName;
      disk.hardwareUUID = other.getHardwareUUID();
      disk.diskType = other.diskType;
      disk.diskMode = other.diskMode;
      disk.externalAddress = other.externalAddress;
      disk.vmdkPath = other.vmdkPath;

      return disk;
   }

   public DiskSpec toDiskSpec() {
      DiskSpec spec = new DiskSpec();
      spec.setName(this.name);
      spec.setAllocType(this.allocType);
      spec.setDiskMode(this.diskMode);
      spec.setDiskType(DiskType.getDiskType(this.diskType));
      spec.setExternalAddress(this.externalAddress);
      spec.setSize(this.sizeInMB / 1024);
      spec.setSeparable(false);
      spec.setTargetDs(this.getDatastoreName());
      spec.setVmdkPath(this.vmdkPath);

      spec.setId(getId());
      return spec;
   }

   public static String getSystemDiskExternalAddress() {
      return SYSTEM_DISK_ADDRESS;
   }

   // inside a vm, the disk name is unique
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      DiskEntity other = (DiskEntity) obj;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "DiskEntity [name=" + name + ", sizeInMB=" + sizeInMB
            + ", diskType=" + diskType + ", allocType=" + allocType
            + ", diskMode=" + diskMode + ", externalAddress=" + externalAddress
            + ", datastoreName=" + datastoreName + ", datastoreMoId="
            + datastoreMoId + ", hardwareUUID=" + hardwareUUID + ", vmkdPath="
            + vmdkPath + ", nodeEntity=" + nodeEntity + "]";
   }
}
