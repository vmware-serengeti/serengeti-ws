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
package com.vmware.bdd.manager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.bdd.entity.resmgmt.ResourceReservation;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.resmgmt.impl.ResourceService;

@MockClass(realClass = ResourceService.class)
public class MockResourceManager implements IResourceService {
   private static boolean reserved = false;
   @Mock
   public boolean isRPExistInVc(String clusterName, String rpName)
   throws VcProviderException {
      // TODO Auto-generated method stub
      return true;
   }

   @Mock
   public boolean isDatastoreExistInVC(String dsName)
   throws VcProviderException {
      // TODO Auto-generated method stub
      return true;
   }

   @Mock
   public boolean isNetworkExistInVc(String networkName)
   throws VcProviderException {
      // TODO Auto-generated method stub
      return true;
   }

   @Mock
   public synchronized UUID reserveResoruce(ResourceReservation resReservation)
   throws VcProviderException {
      if (reserved) {
         throw VcProviderException.CONCURRENT_CLUSTER_CREATING(resReservation.getClusterName());
      }
      reserved = true;
      return UUID.randomUUID();
   }

   @Mock
   public synchronized void commitReservation(UUID reservationId) throws VcProviderException {
      if (reserved) {
         reserved = false;
      }
   }

   @Override
   public VcResourcePool getResourcePoolByName(String rpName)
         throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<VcResourcePool> getAvailableRPs() throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<VcDatastore> getDatastoreByName(String dsName)
         throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<VcDatastore> getAvailableDSs() throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isDatastoreAccessibleByCluster(String datastoreSpec,
         String cluster) throws VcProviderException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public VcNetwork getNetworkByName(String networkName)
         throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isNetworkSharedInCluster(String networkName,
         String clusterName) throws VcProviderException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public List<VcNetwork> getAvailableNetworks() throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<VcHost> getHostsByRpName(String rpName)
         throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<VcHost> getHostsByClusterName(String clusterName)
         throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<VcCluster> getAvailableClusters() throws VcProviderException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void cancleReservation(UUID reservationId) throws VcProviderException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public Map<String, String> getCloudProviderAttributes() {
      // TODO Auto-generated method stub
      return null;
   }

   @Mock
   public void refreshDatastore() {
      
   }
}
