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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vmware.aurora.util.AuAssert;

/**
 * <code>DiskSchema</code> defines the specification of all disks in the
 * template DBVM.
 */
@SuppressWarnings("serial")
public class DiskSchema implements Serializable {

   private List<DiskSpec> diskSpecs;

   public DiskSchema(List<DiskSpec> diskSpecs) {
      this.diskSpecs = diskSpecs == null ? new ArrayList<DiskSpec>(0) : diskSpecs;
   }

   public DiskSchema createAlikeSchema(List<DiskSpec> disksToAdd, List<DiskType> disksToRemove) {
      List<DiskSpec> newSpecs = new ArrayList<DiskSpec>(
            diskSpecs.size() + (disksToAdd == null ? 0 : disksToAdd.size()));

      // Remove all disks of the given type
      if (disksToRemove != null && disksToRemove.size() > 0) {
         for (DiskSpec spec : diskSpecs) {
            boolean keepIt = true;
            for (DiskType type : disksToRemove) {
               if (spec.getDiskType().equals(type)) {
                  keepIt = false;
                  break;
               }
            }
            if (keepIt) {
               newSpecs.add(spec);
            }
         }
      } else {
         for (DiskSpec spec : diskSpecs) {
            newSpecs.add(spec);
         }
      }

      // Append the given new disks
      if (disksToAdd != null) {
         for (DiskSpec spec : disksToAdd) {
            newSpecs.add(spec);
         }
      }
      return new DiskSchema(newSpecs);
   }

   public List<DiskSpec> getDiskSpecs() {
      return Collections.unmodifiableList(diskSpecs);
   }

   /**
    * There could be multiple disks of same type. But for Borealis we will
    * support at most 1 disk for each type.
    */
   public List<DiskSpec> getDiskSpecsByType(DiskType diskType) {
      List<DiskSpec> ret = new ArrayList<DiskSpec>(diskSpecs.size());
      for (DiskSpec spec : diskSpecs) {
         if (spec.getDiskType().equals(diskType)) {
            ret.add(spec);
         }
      }
      return ret;
   }

   /*
    * TODO: Bug 837790: We need to examine all the callers to this method and make sure
    * whether they should support multiple disks of the same disk type.
    */
   public DiskSpec getSingleDiskSpecByType(DiskType diskType) {
      AuAssert.check(diskType != null);
      DiskSpec ret = null;
      for (DiskSpec spec : diskSpecs) {
         if (spec.getDiskType().equals(diskType)) {
            if (ret == null) {
               ret = spec;
            } else {
               throw new AssertionError("More than one disks found for the specified type: " + diskType.name());
            }
         }
      }
      if (ret == null) {
         throw new AssertionError("No disk found for the specified type: " + diskType.name());
      }
      return ret;
   }
}
