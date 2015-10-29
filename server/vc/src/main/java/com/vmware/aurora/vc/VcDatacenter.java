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
package com.vmware.aurora.vc;

import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

import java.util.List;

public interface VcDatacenter extends VcObject {

   abstract String getName();

   /**
    * @return name already in URL format as returned from VLSI
    */
   abstract String getURLName();

   abstract Folder getVmFolder() throws Exception;

   abstract ManagedObjectReference getVmFolderMoRef();

   /**
    * @return all clusters in this data center. Clusters can live at any level
    * in any folder, so find all descendants.
    */
   List<VcCluster> getVcClusters();

   List<ManagedObjectReference> getClusterMoRefs();

   /**
    * Get the VM in the datacenter.
    * @param name VM's name
    * @return the VM, or null if not found
    * @throws Exception
    */
   abstract VcVirtualMachine getVirtualMachine(String name) throws Exception;

}

