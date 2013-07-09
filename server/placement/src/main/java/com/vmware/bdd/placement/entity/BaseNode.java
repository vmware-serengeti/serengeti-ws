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

package com.vmware.bdd.placement.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.vmware.aurora.composition.DiskSchema;
import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.composition.VmSchema;
import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.StorageRead.DiskScsiControllerType;
import com.vmware.bdd.placement.entity.AbstractDatacenter.AbstractHost;
import com.vmware.bdd.placement.util.PlacementUtil;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.vm.device.VirtualDiskOption.DiskMode;

public class BaseNode {

   private String vmName;

   private NodeGroupCreate nodeGroup;

   private ClusterCreate cluster;

   private String targetVcCluster;

   private String targetRp;

   //TODO: TM module should be able to specify target host
   private String targetHost;

   // target datastore for system disk
   private String targetDs;

   // target rack
   private String targetRack;

   private List<DiskSpec> disks;

   private String vmFolder;

   private VmSchema vmSchema;
   private String ipAddress;
   private boolean success = false;
   private boolean finished = false;
   private String vmMobId;
   private String guestHostName;
   private NodeStatus nodeStatus;
   private String nodeAction;
   
   // this class becomes overloaded 
   private VcDatastore targetVcDs;
   private VcResourcePool targetVcRp;
   private VcHost targetVcHost;
   private Folder targetVcFolder;

   public BaseNode() {
      super();
   }
   
   public BaseNode(String vmName) {
      super();
      this.vmName = vmName;
   }

   public BaseNode(String vmName, NodeGroupCreate nodeGroup,
         ClusterCreate cluster) {
      super();
      this.vmName = vmName;
      this.nodeGroup = nodeGroup;
      this.cluster = cluster;

      this.vmSchema = new VmSchema();
   }

   public NodeStatus getNodeStatus() {
      return nodeStatus;
   }

   public void setNodeStatus(NodeStatus nodeStatus) {
      this.nodeStatus = nodeStatus;
   }

   public String getNodeAction() {
      return nodeAction;
   }

   public void setNodeAction(String nodeAction) {
      this.nodeAction = nodeAction;
   }

   public String getGuestHostName() {
      return guestHostName;
   }

   public void setGuestHostName(String guestHostName) {
      this.guestHostName = guestHostName;
   }

   public boolean isFinished() {
      return finished;
   }

   public void setFinished(boolean finished) {
      this.finished = finished;
   }

   public String getVmMobId() {
      return vmMobId;
   }

   public void setVmMobId(String vmMobId) {
      this.vmMobId = vmMobId;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   public String getVmName() {
      return vmName;
   }

   public void setVmName(String vmName) {
      this.vmName = vmName;
   }

   public NodeGroupCreate getNodeGroup() {
      return nodeGroup;
   }

   public void setNodeGroup(NodeGroupCreate nodeGroup) {
      this.nodeGroup = nodeGroup;
   }

   public ClusterCreate getCluster() {
      return cluster;
   }

   public void setCluster(ClusterCreate cluster) {
      this.cluster = cluster;
   }

   public String getGroupName() {
      return nodeGroup.getName();
   }

   public String getClusterName() {
      return cluster.getName();
   }

   public int getCpu() {
      return nodeGroup.getCpuNum();
   }

   public int getMem() {
      return nodeGroup.getMemCapacityMB();
   }

   public String getTargetVcCluster() {
      return targetVcCluster;
   }

   public void setTargetVcCluster(String targetVcCluster) {
      this.targetVcCluster = targetVcCluster;
   }

   public String getTargetRp() {
      return targetRp;
   }

   public void setTargetRp(String rpName) {
      this.targetRp = rpName;
   }

   public String getTargetHost() {
      return targetHost;
   }

   public void setTargetHost(String targetHost) {
      this.targetHost = targetHost;
   }

   public String getTargetRack() {
      return targetRack;
   }

   public void setTargetRack(String targetRack) {
      this.targetRack = targetRack;
   }

   public String getTargetDs() {
      return targetDs;
   }

   public void setTargetDs(String targetDs) {
      this.targetDs = targetDs;
   }

   public List<DiskSpec> getDisks() {
      return disks;
   }

   public void setDisks(List<DiskSpec> disks) {
      this.disks = disks;
   }

   public String getVmFolder() {
      return vmFolder;
   }

   public void setVmFolder(String vmFolder) {
      this.vmFolder = vmFolder;
   }

   public VmSchema getVmSchema() {
      return vmSchema;
   }

   public void setVmSchema(VmSchema vmSchema) {
      this.vmSchema = vmSchema;
   }

   // GB
   public int getStorageSize() {
      AuAssert.check(disks != null && disks.size() != 0);
      int size = 0;
      for (DiskSpec disk : disks) {
         size += disk.getSize();
      }
      return size;
   }

   public String[] getDiskstoreNamePattern() {
      AuAssert.check(this.nodeGroup != null
            && this.nodeGroup.getStorage() != null);

      return NodeGroupCreate.getDiskstoreNamePattern(this.cluster,
            this.nodeGroup);
   }
   
   public String[] getImagestoreNamePattern() {
      AuAssert.check(this.nodeGroup != null
            && this.nodeGroup.getStorage() != null);

      return NodeGroupCreate.getImagestoreNamePattern(this.cluster,
            this.nodeGroup);
   }

   // GB
   public int getSystemDiskSize() {
      AuAssert.check(this.disks != null && this.disks.size() != 0);

      int size = -1;
      boolean found = false;
      for (DiskSpec disk : this.disks) {
         if (disk.isSystemDisk()) {
            size = disk.getSize();
            found = true;
         }
      }

      AuAssert.check(found);
      return size;
   }

   /**
    * populate the base node with placement attributes,
    * cluster/rp/host/datastore, etc.
    * 
    * make sure there are enough cpu/mem/storage inside the rp and vc host
    * 
    * @param vcClusterName
    * @param rpName
    * @param host
    */
   public void place(String rack, String vcClusterName, String rpName,
         AbstractHost host) {
      AuAssert.check(getDisks() != null && getDisks().size() != 0);

      setTargetVcCluster(vcClusterName);
      setTargetHost(host.getName());
      setTargetRp(rpName);
      setTargetRack(rack);

      // generate disk schema
      ArrayList<Disk> tmDisks = new ArrayList<Disk>();

      // transform DiskSpec to TM.VmSchema.DiskSchema
      int lsiScsiIndex = 1;
      int paraVirtualScsiIndex = 0;
      for (DiskSpec disk : this.disks) {
         if (disk.isSystemDisk()) {
            setTargetDs(disk.getTargetDs());
         } else {
            Disk tmDisk = new Disk();
            tmDisk.name = disk.getName();
            tmDisk.initialSizeMB = disk.getSize() * 1024;
            tmDisk.datastore = disk.getTargetDs();
            tmDisk.mode = DiskMode.independent_persistent;
            if (DiskScsiControllerType.LSI_CONTROLLER.equals(disk
                  .getController())) {
               if (lsiScsiIndex == PlacementUtil.CONTROLLER_RESERVED_CHANNEL) {
                  // controller reserved channel, *:7, cannot be used by custom disk
                  lsiScsiIndex++;
               }
               tmDisk.externalAddress =
                     PlacementUtil.LSI_CONTROLLER_EXTERNAL_ADDRESS_PREFIX
                           + lsiScsiIndex;
               lsiScsiIndex++;
            } else {
               tmDisk.externalAddress =
                     PlacementUtil.getParaVirtualAddress(paraVirtualScsiIndex);
               paraVirtualScsiIndex =
                     PlacementUtil
                           .getNextValidParaVirtualScsiIndex(paraVirtualScsiIndex);
            }
            tmDisk.allocationType =
                  AllocationType.valueOf(disk.getAllocType());
            tmDisk.type = disk.getDiskType().getType();
            tmDisks.add(tmDisk);
         }
      }

      DiskSchema diskSchema = new DiskSchema();
      diskSchema.setName("Disk Schema");
      diskSchema.setDisks(tmDisks);
      this.vmSchema.diskSchema = diskSchema;
   }

   public VcDatastore getTargetVcDs() {
      return targetVcDs;
   }

   public void setTargetVcDs(VcDatastore targetVcDs) {
      this.targetVcDs = targetVcDs;
   }

   public VcResourcePool getTargetVcRp() {
      return targetVcRp;
   }

   public void setTargetVcRp(VcResourcePool targetVcRp) {
      this.targetVcRp = targetVcRp;
   }

   public VcHost getTargetVcHost() {
      return targetVcHost;
   }

   public void setTargetVcHost(VcHost targetVcHost) {
      this.targetVcHost = targetVcHost;
   }

   public Folder getTargetVcFolder() {
      return targetVcFolder;
   }

   public void setTargetVcFoler(Folder targetVcFolder) {
      this.targetVcFolder = targetVcFolder;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      String newLine = System.getProperty("line.separator");

      result.append(this.getClass().getName());
      result.append(" Object {");
      result.append(newLine);

      //determine fields declared in this class only (no fields of superclass)
      Field[] fields = this.getClass().getDeclaredFields();

      //print field names paired with their values
      for (Field field : fields) {
         if ("vmSchema".equals(field.getName()))
            continue;

         result.append("  ");
         try {
            result.append(field.getName());
            result.append(": ");
            // requires access to private field:
            result.append(field.get(this));
         } catch (IllegalAccessException ex) {
            System.out.println(ex);
         }
         result.append(newLine);
      }
      result.append("}");

      return result.toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((vmName == null) ? 0 : vmName.hashCode());
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
      BaseNode other = (BaseNode) obj;
      if (vmName == null) {
         if (other.vmName != null)
            return false;
      } else if (!vmName.equals(other.vmName))
         return false;
      return true;
   }
}
