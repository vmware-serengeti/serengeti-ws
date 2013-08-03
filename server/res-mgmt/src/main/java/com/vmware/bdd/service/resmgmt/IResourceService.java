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

package com.vmware.bdd.service.resmgmt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.entity.resmgmt.ResourceReservation;
import com.vmware.bdd.exception.VcProviderException;


/**
 * APIs to interact with resource pool, network, data store etc.
 *
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 */

public interface IResourceService {

   /**
    * Get vCenter resource pool by the name that user added by using
    * add resource pool API, addResourcePool
    *
    * @param rpName
    *           resource pool name in the DB not VC
    * @return VC resource pool object
    * @throws VcProviderException
    *
    * @see com.vmware.bdd.service.resmgmt.IResourcePoolService
    */
   VcResourcePool getResourcePoolByName(String rpName)
         throws VcProviderException;

   /**
    * Verify whether the specified vCenter resource pool exist in the cluster
    *
    * @param clusterName
    *           vCenter cluster name
    * @param rpName
    *           vCenter resource pool name
    * @return true - the resource pool exist in the cluster,
    *         false - the resource pool does not exist in the cluster
    * @throws VcProviderException
    */
   boolean isRPExistInVc(String clusterName, String rpName)
         throws VcProviderException;

   /**
    * Get all the resource pools that user added
    * @return List of vCenter resource pool
    * @throws VcProviderException
    */
   List<VcResourcePool> getAvailableRPs() throws VcProviderException;

   /**
    * Get the vCenter datastore by the name that user added by using add datastore API
    * @param dsName datastore name
    * @return List of vCenter datastore
    * @throws VcProviderException
    *
    * @see com.vmware.bdd.service.resmgmt.IDatastoreService
    */
   List<VcDatastore> getDatastoreByName(String dsName)
         throws VcProviderException;

   /**
    * refresh datastores of available clusters
    */
   void refreshDatastore();

   /**
    * Whether the datastore exist in the vCenter
    * @param dsName datastore name
    * @return true - the datastore exist in the vCenter,
    *         false - otherwise
    * @throws VcProviderException
    */
   boolean isDatastoreExistInVC(String dsName)
         throws VcProviderException;

   /**
    * Get all available datastore added by user
    * @return
    * @throws VcProviderException
    */
   List<VcDatastore> getAvailableDSs() throws VcProviderException;

   /**
    * Whether all the datastores in the spec can be accessed by the hosts in the cluster.
    * @param datastoreSpec Datastore spec name
    * @param cluster Cluster name
    * @return  True - if any datastores can be accessed by the hosts in the cluster
    *          False - Othwerwise
    * @throws VcProviderException
    */
   boolean isDatastoreAccessibleByCluster(String datastoreSpec, String cluster) throws VcProviderException;

   /**
    * Get the vCenter network by the name added by using add network API.
    * @param networkName
    * @return
    * @throws VcProviderException
    *
    * @see @see com.vmware.bdd.service.INetworkService
    */
   VcNetwork getNetworkByName(String networkName) throws VcProviderException;

   /**
    * Whether the network exist in the vCenter
    * @param networkName
    * @return
    * @throws VcProviderException
    */
   boolean isNetworkExistInVc(String networkName)
         throws VcProviderException;

   /**
    * Whether the network is shared network, which mean all the host in the
    * cluster have the specified network.
    * @param networkName
    * @param clusterName
    * @return
    * @throws VcProviderException
    */
   boolean isNetworkSharedInCluster(String networkName,
         String clusterName) throws VcProviderException;

   /**
    * Get all available networks added by user
    * @return
    * @throws VcProviderException
    */
   List<VcNetwork> getAvailableNetworks() throws VcProviderException;

   /**
    * Get all the host in the cluster where resource pool exist.
    * @param rpName
    * @return
    * @throws VcProviderException
    */
   List<VcHost> getHostsByRpName(String rpName) throws VcProviderException;

   /**
    * Get all the host in the cluster
    * @param clusterName
    * @return
    * @throws VcProviderException
    */
   List<VcHost> getHostsByClusterName(String clusterName)
         throws VcProviderException;

   /**
    * Get all the available clusters added by using
    * add resource pool API, addResourcePool
    * @return
    * @throws VcProviderException
    * @see com.vmware.bdd.service.resmgmt.IResourcePoolService
    */
   List<VcCluster> getAvailableClusters() throws VcProviderException;

   /**
    * Reserver resource
    * @param resReservation
    * @return
    * @throws VcProviderException
    */
   UUID reserveResoruce(ResourceReservation resReservation)
         throws VcProviderException;

   /**
    * Commit resource reservation
    * @param reservationId
    * @throws VcProviderException
    */
   void commitReservation(UUID reservationId) throws VcProviderException;

   /**
    * Cancel resource reservation
    * @param reservationId
    * @throws VcProviderException
    */
   void cancleReservation(UUID reservationId) throws VcProviderException;
   
   /**
    * get the attributes of the underlying cloud provider, in map
    * @return
    */
   Map<String, String> getCloudProviderAttributes();

}
