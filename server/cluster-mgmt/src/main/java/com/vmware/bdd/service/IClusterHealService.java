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
package com.vmware.bdd.service;

import java.util.List;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.entity.DiskEntity;

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
   public List<DiskEntity> getBadDisks(String nodeName);

   /**
    * for the specified bad disks, find their replacements in healthy datastores
    * 
    * @param clusterName
    * @param nodeName
    * @param badDisks
    *           bad disks that locate on unaccessible datastores
    * @return replacement disks for the input bad disks
    */
   public List<DiskEntity> getReplacementDisks(String clusterName,
         String groupName, String nodeName, List<DiskEntity> badDisks);

   /**
    * fix disk failures for the specified node, say node A. Steps: 
    * 1. power off VM A 
    * 2. create a empty VM (named A.vm_name + "recovery") or clone one from
    *    the template VM, depends on whether the system disk is corrupted or not,
    *    with the exact same vccluster/host/rp/vc folder/network/cpu/mem settings
    * 3. attach existing or create non-existing vmdks in fullDiskSet 
    * 4. remove VM A, set node A's mobId to null
    * 5. rename the recovered VM to A.vm_name and power on
    *  
    * @param clusterName
    * @param groupName
    * @param diskSet
    * @return
    */
   public boolean fixDiskFailures(String clusterName, String groupName,
         String nodeName, List<DiskEntity> fullDiskSet);
   
   /**
    * create a replacement vm with the exact same settings with the origin one for 
    * the input node. attach good disks from the old vm and create replacement disks 
    * for bad ones
    * 
    * @param clusterSpec
    * @param groupName
    * @param node
    * @param fullDiskSet
    * @return
    */
   public VcVirtualMachine createReplacementVm(String clusterName,
         String groupName, String nodeName, List<DiskEntity> fullDiskSet);
   
   /**
    * update disk info with the input disk entity set
    * @param vmId
    * @param nodeName
    * @param fullDiskSet
    */
   public void updateDiskData(String vmId, String nodeName);
}