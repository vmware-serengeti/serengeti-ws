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

package com.vmware.bdd.service.resmgmt.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.aurora.vc.VcCluster;
import com.vmware.aurora.vc.VcDatastore;
import com.vmware.aurora.vc.VcHost;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcNetwork;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.dal.IDatastoreDAO;
import com.vmware.bdd.dal.INetworkDAO;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.VcDatastoreEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.entity.resmgmt.ResourceReservation;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;

/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 * 
 */
@Service
public class ResourceService implements IResourceService {

   private static final Logger logger = Logger.getLogger(ResourceService.class);

   private IResourcePoolDAO rpDao;
   private IDatastoreDAO dsDao;
   private INetworkDAO networkDao;

   private final Map<UUID, ResourceReservation> reservedResource =
         new HashMap<UUID, ResourceReservation>();


   /**
    * @return the rpDao
    */
   public IResourcePoolDAO getRpDao() {
      return rpDao;
   }


   /**
    * @param rpDao
    *           the rpDao to set
    */
   @Autowired
   public void setRpDao(IResourcePoolDAO rpDao) {
      this.rpDao = rpDao;
   }


   /**
    * @return the dsDao
    */
   public IDatastoreDAO getDsDao() {
      return dsDao;
   }


   /**
    * @param dsDao
    *           the dsDao to set
    */
   @Autowired
   public void setDsDao(IDatastoreDAO dsDao) {
      this.dsDao = dsDao;
   }


   /**
    * @return the networkDao
    */
   public INetworkDAO getNetworkDao() {
      return networkDao;
   }


   /**
    * @param networkDao
    *           the networkDao to set
    */
   @Autowired
   public void setNetworkDao(INetworkDAO networkDao) {
      this.networkDao = networkDao;
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getResourcePoolByName(java.lang.String)
    */
   @Override
   public VcResourcePool getResourcePoolByName(String rpName)
         throws VcProviderException {
      if (StringUtils.isEmpty(rpName)) {
         throw VcProviderException.RESOURCE_POOL_NAME_INVALID(rpName);
      }
      VcResourcePoolEntity rp = rpDao.findByName(rpName);
      final String clusterName = rp.getVcCluster();
      final String vcRPName = rp.getVcResourcePool();

      VcResourcePool vcRP =
            VcResourceUtils.findRPInVCCluster(clusterName, vcRPName);
      if (vcRP == null) {
         throw VcProviderException.RESOURCE_POOL_NOT_FOUND(rpName);
      }
      return vcRP;
   }


   @Override
   public boolean isRPExistInVc(String clusterName, String rpName)
         throws VcProviderException {
      boolean result = false;
      VcResourcePool vcRP =
            VcResourceUtils.findRPInVCCluster(clusterName, rpName);
      if (vcRP != null) {
         result = true;
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getAvailableRPs()
    */
   @Override
   public List<VcResourcePool> getAvailableRPs() throws VcProviderException {
      List<VcResourcePool> result = new ArrayList<VcResourcePool>();

      List<VcResourcePoolEntity> allRPEntities =
            rpDao.findAllOrderByClusterName();

      for (VcResourcePoolEntity rpEntity : allRPEntities) {
         VcResourcePool vcRP =
               VcResourceUtils.findRPInVCCluster(rpEntity.getVcCluster(),
                     rpEntity.getVcResourcePool());
         if (vcRP != null) {
            result.add(vcRP);
         } else {
            logger.warn("Could not find the resource pool in the vCenter. RP name:"
                  + rpEntity.getName());
         }
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getDatastoreByName(java.lang.String)
    */
   @Override
   public List<VcDatastore> getDatastoreByName(String dsName)
         throws VcProviderException {
      logger.info("get datastore by name: " + dsName);
      List<VcDatastoreEntity> datastores = dsDao.findByName(dsName);
      return findDSInVC(datastores);
   }

   @Override
   public void refreshDatastore() {
      List<VcCluster> vcClusters = VcInventory.getClusters();
      AuAssert.check(vcClusters != null && vcClusters.size() != 0);
      for (VcCluster vcCluster : vcClusters) {
         VcResourceUtils.refreshDatastore(vcCluster);
      }
   }

   @Override
   public boolean isDatastoreExistInVC(String dsName)
         throws VcProviderException {
      boolean result = false;
      Collection<VcDatastore> vcDS =
            VcResourceUtils.findDSInVCByPattern(dsName);
      if (vcDS != null && vcDS.size() > 0) {
         result = true;
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getAvailableDSs()
    */
   @Override
   public List<VcDatastore> getAvailableDSs() throws VcProviderException {
      List<VcDatastoreEntity> datastores = dsDao.findAllSortByName();
      return findDSInVC(datastores);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.IResourceManager#isDatastoreAccessibleByCluster(java.lang.String, java.lang.String)
    */
   @Override
   public boolean isDatastoreAccessibleByCluster(String datastoreSpec,
         String cluster) throws VcProviderException {
      boolean result = false;
      List<VcDatastore> availableDatastores = getDatastoreByName(datastoreSpec);
      List<VcHost> hosts = getHostsByClusterName(cluster);
      for (VcHost host : hosts) {
         List<VcDatastore> hostDSs = host.getDatastores();
         boolean found = false;
         for (VcDatastore availableDS : availableDatastores) {
            for (VcDatastore hostDS : hostDSs) {
               if (availableDS.getName().equals(hostDS.getName())) {
                  found = true;
                  break;
               }
            }
            if (found) {
               break;
            }
         }
         if (found) {
            result = true;
            break;
         }
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getNetworkByName(java.lang.String)
    */
   @Override
   public VcNetwork getNetworkByName(String networkName)
         throws VcProviderException {
      NetworkEntity networkEntity = networkDao.findNetworkByName(networkName);
      return VcResourceUtils.findNetworkInVC(networkEntity.getPortGroup());
   }


   @Override
   public boolean isNetworkExistInVc(String networkName)
         throws VcProviderException {
      boolean result = false;
      VcNetwork network = VcResourceUtils.findNetworkInVC(networkName);
      if (network != null) {
         result = true;
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getAvailableNetworks()
    */
   @Override
   public List<VcNetwork> getAvailableNetworks() throws VcProviderException {
      List<NetworkEntity> networkList = networkDao.findAllNetworks();
      List<VcNetwork> result = new ArrayList<VcNetwork>();
      for (NetworkEntity networkEntity : networkList) {
         VcNetwork network =
               VcResourceUtils.findNetworkInVC(networkEntity.getPortGroup());
         if (network != null) {
            result.add(network);
         } else {
            logger.warn("Could not find the network in the vCenter. Network name: "
                  + networkEntity.getName());
         }
      }
      return result;
   }

   @Override
   public boolean isNetworkSharedInCluster(String networkName,
         String clusterName) throws VcProviderException {
      boolean result = true;
      VcNetwork vcNetwork = getNetworkByName(networkName);
      if (vcNetwork == null) {
         return false;
      } else {
         String portGroupName = vcNetwork.getName();
         List<VcHost> hosts = getHostsByClusterName(clusterName);
         for (VcHost vcHost : hosts) {
            List<VcNetwork> networks = vcHost.getNetworks();
            boolean found = false;
            for (VcNetwork network : networks) {
               if (network.getName().equals(portGroupName)) {
                  found = true;
                  break;
               }
            }
            if (!found) {
               result = false;
               break;
            }
         }
      }
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getHostsByRpName(java.lang.String)
    */
   @Override
   public List<VcHost> getHostsByRpName(String rpName)
         throws VcProviderException {
      VcResourcePoolEntity rp = rpDao.findByName(rpName);
      final String clusterName = rp.getVcCluster();
      return VcResourceUtils.findAllHostsInVCCluster(clusterName);
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#reserveResoruce(com.vmware.bdd.bo.ResourceReservation)
    */
   @Override
   public synchronized UUID reserveResoruce(ResourceReservation resReservation)
         throws VcProviderException {
      if (reservedResource.keySet().size() == 0) {
         UUID result = UUID.randomUUID();
         reservedResource.put(result, resReservation);
         return result;
      } else {
         ResourceReservation[] reservations =
               reservedResource.values().toArray(new ResourceReservation[0]);
         String clusterName = reservations[0].getClusterName();
         logger.error("concurrent cluster create.");
         throw VcProviderException
               .CONCURRENT_CLUSTER_CREATING(clusterName != null ? clusterName
                     : "unkown cluster");
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#cancleReservation(long)
    */
   @Override
   public synchronized void cancleReservation(UUID reservationId)
         throws VcProviderException {
      reservedResource.remove(reservationId);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#commitReservation(long)
    */
   @Override
   public synchronized void commitReservation(UUID reservationId)
         throws VcProviderException {
      reservedResource.remove(reservationId);
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getHostsByClusterName(java.lang.String)
    */
   @Override
   public List<VcHost> getHostsByClusterName(String clusterName)
         throws VcProviderException {
      return VcResourceUtils.findAllHostsInVCCluster(clusterName);
   }


   /* (non-Javadoc)
    * @see com.vmware.bdd.service.resmgmt.ResourceManager#getAvailableClusters()
    */
   @Override
   public List<VcCluster> getAvailableClusters() throws VcProviderException {
      final List<VcResourcePoolEntity> allRPEntities =
            rpDao.findAllOrderByClusterName();
      List<VcCluster> clusters =
            VcContext.inVcSessionDo(new VcSession<List<VcCluster>>() {
               List<VcCluster> result = new ArrayList<VcCluster>();

               @Override
               protected List<VcCluster> body() throws Exception {
                  List<VcCluster> vcClusters = VcInventory.getClusters();
                  for (VcResourcePoolEntity rpEntity : allRPEntities) {
                     boolean added = false;
                     for (VcCluster vcCluster : result) {
                        if (rpEntity.getVcCluster().equals(vcCluster.getName())) {
                           added = true;
                           break;
                        }
                     }
                     if (added) {
                        continue;
                     }
                     for (VcCluster vcCluster : vcClusters) {
                        if (rpEntity.getVcCluster().equals(vcCluster.getName())) {
                           result.add(vcCluster);
                           break;
                        }
                     }
                  }
                  return result;
               }

            });
      return clusters;
   }


   /**
    * @param datastores
    * @return
    */
   protected List<VcDatastore> findDSInVC(List<VcDatastoreEntity> datastores) {
      List<VcDatastore> result = new ArrayList<VcDatastore>();
      if (datastores == null) {
         return result;
      }
      for (VcDatastoreEntity dsEntity : datastores) {
         Collection<VcDatastore> vcDS =
               VcResourceUtils.findDSInVCByPattern(dsEntity.getVcDatastore());
         if (vcDS != null) {
            result.addAll(vcDS);
         } else {
            logger.warn("Could not find the data store in the vCenter. Name:"
                  + dsEntity.getVcDatastore());
         }
      }
      return result;
   }


   @Override
   public Map<String, String> getCloudProviderAttributes() {
      Map<String, String> attr = new HashMap<String, String>();
      attr.put(Constants.CLOUD_PROVIDER_NAME_FIELD,
            Constants.VSPHERE_PROVIDER_NAME);
      attr.put(Constants.CLOUD_PROVIDER_TYPE_FIELD,
            Constants.VSPHERE_PROVIDER_TYPE);
      return attr;
   }

}
