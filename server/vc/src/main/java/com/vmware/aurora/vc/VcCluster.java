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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.vmware.aurora.exception.VcException;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcTask.TaskType;
import com.vmware.aurora.vc.VcTaskMgr.IVcTaskBody;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.ComputeResource;
import com.vmware.vim.binding.vim.Datacenter;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.cluster.ConfigInfoEx;
import com.vmware.vim.binding.vim.cluster.ConfigSpecEx;
import com.vmware.vim.binding.vim.cluster.DasVmConfigInfo;
import com.vmware.vim.binding.vim.host.ConnectInfo.DatastoreInfo;
import com.vmware.vim.binding.vmodl.ManagedObject;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

public interface VcCluster extends VcObject {

   abstract String getName();

   /**
    * @return the datacenter of this cluster
    * @throws Exception
    */
   abstract VcDatacenter getDatacenter();

   /**
    * @return the ESX hosts in this cluster
    * @throws Exception
    */
   abstract List<VcHost> getHosts() throws Exception;

   /**
    * @return a list of networks shared by all hosts in the cluster
    */
   abstract List<VcNetwork> getSharedNetworks();

   /**
    * Return a list of all networks (shared by all hosts and not).
    *
    * @return all networks
    * @throws Exception
    */
   abstract List<VcNetwork> getAllNetworks();

   /**
    * Get the given named network
    *
    * @param name
    *           name of the network
    * @return the network, null if not found
    */
   abstract VcNetwork getNetwork(String name);

   /**
    * Get a network connected by all hosts.
    *
    * @param name
    *           name of the network
    * @return the network, null if not found
    * @throws Exception
    */
   abstract VcNetwork getSharedNetwork(String name);

   /**
    * @return a list of datastores shared by all hosts in the cluster
    */
   abstract List<VcDatastore> getSharedDatastores();

   /**
    * Return a list of all datastores (shared by all hosts and not).
    *
    * @return all datastores
    * @throws Exception
    */
   abstract List<VcDatastore> getAllDatastores();

   /**
    * Get the given named datastore
    *
    * @param name
    *           name of the datastore
    * @return the datastore, null if not found
    */
   abstract VcDatastore getDatastore(String name);

   /**
    * @return the root RP of this cluster
    * @throws Exception
    */
   abstract VcResourcePool getRootRP();

   /**
    * Find the RP corresponding to the path.
    *
    * @param path
    * @return the resource pool
    * @throws Exception
    */
   abstract VcResourcePool searchRP(String path) throws Exception;

   /**
    * @return all resource pools qualified for resource bundle
    * @throws Exception
    */
   abstract List<VcResourcePool> getQualifiedRPs() throws Exception;

   /**
    * @return all leaf resource pools in this cluster
    * @throws Exception
    */
   abstract List<VcResourcePool> getAllRPs() throws Exception;

   /**
    * @return Whether High Availability is enabled for this cluster
    * @throws Exception
    **/
   abstract VcClusterConfig getConfig();

   abstract List<String> getHADRSIncompatReasonsForAlert();

   abstract List<String> getHADRSIncompatReasonsForRBQual();

   abstract DasVmConfigInfo[] getVmConfigInfo();

   /**
    * Reconfigure cluster settings
    *
    * @param spec
    *           specification of change
    * @param callback
    *           (optional) call-back function for the task
    * @return a task object for the reconfiguration operation
    * @throws Exception
    */
   abstract VcTask reconfigure(final ConfigSpecEx spec,
         final IVcTaskCallback callback) throws Exception;

   /**
    * Reconfigure cluster settings. Blocks until change is complete, returns
    * only success/failure.
    *
    * @param spec
    *           specification of the change
    * @return true on success, false on failure
    * @throws Exception
    */
   boolean reconfigure(final ConfigSpecEx spec) throws Exception;

   int getTotalCpu();

   long getTotalMemory();

   int getNumberOfHost();

   /**
    *
    * @return MoRefs for all hosts.
    */
   ManagedObjectReference[] getHostMoRefs();

   /**
    *
    * @return MoRefs for all Network.
    */
   ManagedObjectReference[] getNetworkMoRefs();

   /**
    *
    * @return MoRefs for all Datastore.
    */
   ManagedObjectReference[] getDataStoreMoRefs();
}

