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

package com.vmware.aurora.composition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import com.vmware.aurora.global.DiskSize;
import com.vmware.aurora.composition.DiskSchema.Disk;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.DeviceId;
import com.vmware.aurora.vc.DiskType;
import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine.DiskCreateSpec;

/**
 * Utility Class for the DiskSchema
 * 
 * @author sridharr
 * 
 */
public class DiskSchemaUtil {

   public static DiskSchema getSchema(String xmlSchema) throws JAXBException {
      return SchemaUtil.getSchema(xmlSchema, DiskSchema.class);
   }

   public static DiskSchema getSchema(File file) throws JAXBException {
      return SchemaUtil.getSchema(file, DiskSchema.class);
   }

   /**
    * 
    * @param template
    * @param diskMap
    */
   public static void getTemplateDiskMap(VmSchema vmSchema,
         HashMap<String, Disk.Operation> diskMap) {
      for (DiskSchema.Disk disk : vmSchema.diskSchema.getDisks()) {
         diskMap.put(disk.externalAddress, Disk.Operation.CLONE);
      }
   }

   /**
    * Get the set of disks to add to the newly cloned Vm (from the DiskSchema
    * information)
    * 
    * @param hostList
    *           the list of hosts that have access to the datastores on which
    *           the disks are to be added
    * @param rp
    *           the resource pool in which the VM is
    * @param datastore
    *           the default datastore to add the disk to, if not specified in
    *           diskSchema
    * @param diskSchema
    *           the VM's diskSchema
    * @return ArrayList of CreateSpec for the new disks to add hostList is
    *         updated
    */
   public static List<DiskCreateSpec> getDisksToAdd(List<VcHost> hostList,
         VcResourcePool rp, VcDatastore ds, DiskSchema diskSchema,
         HashMap<String, Disk.Operation> diskMap) {
      List<DiskCreateSpec> result = new ArrayList<DiskCreateSpec>();
      /* XXX : TODO Go through the hierarchy of DiskSchemas to ensure that these
       * disks are to be added, not modified. However, it appears that changing 
       * disk mode to independent_persistent is not supported in vSphere 5.1.
       * That means we can no longer mark the OS and BIN disks to independent_persistent 
       * so that they are not snapshoted
       */
      VcCluster cluster = rp.getVcCluster();

      HashMap<VcHost, Integer> hostCount = new HashMap<VcHost, Integer>();

      int numDisks = 0;
      for (DiskSchema.Disk disk : diskSchema.getDisks()) {
         if (disk.vmdkPath != null && !disk.vmdkPath.isEmpty()) {
            // existed virtual disk, no need to create, need to attach.
            continue;
         }
         if (DiskType.OS.getTypeName().equals(disk.type)) {
            // system disk is either be cloned or attached, it will never be added.
            continue;
         }
         numDisks++;
         VcDatastore diskDs = null;
         if (!disk.datastore.equals("")) {
            // Find the right datastore from the list of cluster datastores
            diskDs = cluster.getDatastore(disk.datastore);
            AuAssert.check(diskDs != null);
            for (VcHost h : diskDs.getHosts()) {
               if (hostCount.containsKey(h)) {
                  hostCount.put(h, hostCount.get(h) + 1);
               } else {
                  hostCount.put(h, 1);
               }
            }
         }

         if (disk.attributes != null
               && disk.attributes.contains(DiskSchema.DiskAttribute.PROMOTE)) {
            diskMap.put(disk.externalAddress, Disk.Operation.PROMOTE);
         } else {
            // Make sure we don't already have an existing disk of the same address
            AuAssert.check(diskMap.get(disk.externalAddress) == null);
            DiskCreateSpec createSpec =
                  new DiskCreateSpec(new DeviceId(disk.externalAddress),
                        diskDs, disk.name, disk.mode,
                        DiskSize.sizeFromMB(disk.initialSizeMB),
                        disk.allocationType);
            result.add(createSpec);
         }
      }

      for (Entry<VcHost, Integer> entry : hostCount.entrySet()) {
         if (entry.getValue().intValue() == numDisks) {
            hostList.add(entry.getKey());
         }
      }
      return result;
   }
}
