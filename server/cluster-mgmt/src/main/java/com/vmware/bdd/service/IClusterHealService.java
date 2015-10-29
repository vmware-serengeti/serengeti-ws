/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service;

import java.util.List;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.entity.DiskEntity;
import com.vmware.bdd.spectypes.DiskSpec;

/**
 * 
 * @author tli
 * 
 */
public interface IClusterHealService {
   /**
    * does this node have bad disks that locate on unaccessible datastores
    * 
    * @param nodeName
    * @return
    */
   public boolean hasBadDisks(String nodeName);

   /**
    * return bad disks that locate on unaccessible datastores
    * 
    * @param nodeName
    * @return
    */
   public List<DiskSpec> getBadDisks(String nodeName);

   /**
    * does this node have bad disks except system disk that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public boolean hasBadDisksExceptSystem(String nodeName);

   /**
    * does this node have bad data disks that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public boolean hasBadDataDisks(String nodeName);

   /**
    * return bad data disk entities that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public List<DiskEntity> getBadDataDiskEntities(String nodeName);

   /**
    * return bad data disks that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public List<DiskSpec> getBadDataDisks(String nodeName);

   /**
    * does this node have bad system disks that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public boolean hasBadSystemDisks(String nodeName);

   /**
    * return bad system disk entities that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public List<DiskEntity> getBadSystemDiskEntities(String nodeName);

   /**
    * return bad system disks that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public List<DiskSpec> getBadSystemDisks(String nodeName);

   /**
    * does this node have bad swap disk that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public boolean hasBadSwapDisks(String nodeName);

   /**
    * return bad swap disk entity that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public List<DiskEntity> getBadSwapDiskEntities(String nodeName);

   /**
    * return bad swap disk that locate on unaccessible datastores
    *
    * @param nodeName
    * @return
    */
   public List<DiskSpec> getBadSwapDisks(String nodeName);

   /**
    * for the specified bad disks, find their replacements in healthy datastores
    * 
    * @param clusterName
    * @param nodeName
    * @param badDisks
    *           bad disks that locate on unaccessible datastores
    * @return replacement disks for the input bad disks
    */
   public List<DiskSpec> getReplacementDisks(String clusterName,
         String groupName, String nodeName, List<DiskSpec> badDisks);

   /**
    * fix disk failures for the specified node, say node A. Steps: 1). power off
    * VM A, 2). create a empty VM (named A.vm_name + "recovery") or clone one
    * from the template VM, depends on whether the system disk is corrupted or
    * not, with the exact same vccluster/host/rp/vc folder/network/cpu/mem
    * settings, 3). attach existing or create non-existing vmdks in fullDiskSet,
    * 4). remove VM A, set node A's mobId to null, 5). rename the recovered VM
    * to A.vm_name and power on
    * 
    * @param clusterName
    * @param groupName
    * @param nodeName
    * @param replacementDisks
    * @return
    */
   public boolean fixDiskFailures(String clusterName, String groupName,
         String nodeName, List<DiskSpec> replacementDisks);

   /**
    * create a replacement vm with the exact same settings with the origin one
    * for the input node. attach good disks from the old vm and create
    * replacement disks for bad ones
    * 
    * @param clusterSpec
    * @param groupName
    * @param node
    * @param replacementDisks
    *           disks to be created as their original one is not accessible
    * @return
    */
   public VcVirtualMachine createReplacementVm(String clusterName,
         String groupName, String nodeName, List<DiskSpec> replacementDisks);

   /**
    * power on the specified vm
    *
    * @param nodeName
    * @param vmId
    * @param clusterName
    */
   public void startVm(String nodeName, String vmId, String clusterName);

   /**
    * update vm and disk info from the new vm
    * 
    * @param clusterName
    * @param groupName
    * @param nodeName
    * @param newVmId
    */
   public void updateData(String clusterName, String groupName,
         String nodeName, String newVmId);

   /**
    * verify node has correct status after vm fix, before software bootstrap
    * 
    * @param nodeName
    */
   public void verifyNodeStatus(String vmId, String nodeName);

   /**
    * check the status of vm and recovery vm, do something to make sure cluster fix
    * is able to reentrant
    * 
    * @param clusterName
    * @param groupName
    * @param nodeName
    * @return
    */
   public VcVirtualMachine getFixingVm(String clusterName, String groupName,
         String nodeName);


   /**
    *
    * Replace bad Disks except system for VM
    *
    * @param clusterSpec
    * @param groupName
    * @param node
    * @param replacementDisks
    *           disks to be created as their original one is not accessible
    * @return
    */
   public VcVirtualMachine replaceBadDisksExceptSystem(String clusterName, String groupName, String nodeName, List<DiskSpec> replacementDisks);
}
