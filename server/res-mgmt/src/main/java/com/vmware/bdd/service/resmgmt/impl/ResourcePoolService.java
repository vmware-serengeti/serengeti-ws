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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.dal.IClusterDAO;
import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.spectypes.VcCluster;

@Service
public class ResourcePoolService implements IResourcePoolService {
   private static final Logger logger = Logger
         .getLogger(ResourcePoolService.class);

   private IResourcePoolDAO rpDao;

   private IClusterDAO clusterDao;

   private IResourceService resService;

   public IResourcePoolDAO getRpDao() {
      return rpDao;
   }

   @Autowired
   public void setRpDao(IResourcePoolDAO rpDao) {
      this.rpDao = rpDao;
   }

   public IClusterDAO getClusterDao() {
      return clusterDao;
   }

   @Autowired
   public void setClusterDao(IClusterDAO clusterDao) {
      this.clusterDao = clusterDao;
   }

   public IResourceService getResService() {
      return resService;
   }

   @Autowired
   public void setResService(IResourceService resSvc) {
      this.resService = resSvc;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#getAllVcResourcePool()
    */
   @Override
   public List<VcCluster> getAllVcResourcePool() {
      // load vc resource pools
      List<VcResourcePoolEntity> rps = rpDao.findAllOrderByClusterName();
      List<VcCluster> result = convertToVcCluster(rps);
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#addResourcePool(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public synchronized void addResourcePool(String rpName,
         String vcClusterName, String vcResourcePool) {
      boolean existed = rpDao.isRPAdded(vcClusterName, vcResourcePool);
      if (existed) {
         logger.debug("resource pool " + vcResourcePool + " in cluster "
               + vcClusterName + " is already existed.");
         throw VcProviderException
               .VC_RESOURCE_POOL_ALREADY_ADDED(vcResourcePool);
      }
      resService.refreshResourcePool();
      if (!resService.isRPExistInVc(vcClusterName, vcResourcePool)) {
         throw VcProviderException.RESOURCE_POOL_NOT_FOUND(vcResourcePool);
      }
      rpDao.addResourcePoolEntity(rpName, vcClusterName, vcResourcePool);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#getVcResourcePoolByName(java.lang.String)
    */
   @Override
   public List<VcCluster> getVcResourcePoolByName(String name) {
      logger.info("get resource pool by name " + name);
      List<VcResourcePoolEntity> rps = new ArrayList<VcResourcePoolEntity>();
      VcResourcePoolEntity rp = rpDao.findByName(name);
      if (rp != null) {
         rps.add(rp);
      } else {
         logger.info("got null resource pool entity for name:" + name);
      }
      List<VcCluster> result = convertToVcCluster(rps);
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#getVcResourcePoolByNameList(java.lang.String[])
    */
   @Override
   public List<VcCluster> getVcResourcePoolByNameList(String[] names) {
      logger.debug("get resource pools by name list " + Arrays.toString(names));
      if (names == null || names.length == 0) {
         return null;
      }
      Map<String, VcCluster> clusterMap = new HashMap<String, VcCluster>(); // key: cluster name
      for (String name : names) {
         VcResourcePoolEntity rpForName = rpDao.findByName(name);
         if (rpForName == null) {
            logger.warn("Specified resource pool "
                  + name
                  + ", but the resource pool is removed by others. Continue to use other resource pools.");
            continue;
         }
         combineCluster(clusterMap, rpForName);
      }
      if (clusterMap.values().isEmpty()) {
         throw VcProviderException.NO_RESOURCE_POOL_FOUND(names);
      }
      List<VcCluster> result = new ArrayList<VcCluster>();
      result.addAll(clusterMap.values());
      return result;
   }

   private void combineCluster(Map<String, VcCluster> clusterMap,
         VcResourcePoolEntity rpEntity) {
      List<VcResourcePoolEntity> rpEntities =
            new ArrayList<VcResourcePoolEntity>();
      rpEntities.add(rpEntity);
      List<VcCluster> vcClusters = convertToVcCluster(rpEntities);
      if (vcClusters == null) {
         return;
      }
      for (VcCluster vcCluster : vcClusters) {
         if (!clusterMap.containsKey(vcCluster.getName())) {
            clusterMap.put(vcCluster.getName(), vcCluster);
            continue;
         }
         List<String> rps = vcCluster.getVcRps();
         VcCluster storedCluster = clusterMap.get(vcCluster.getName());
         if (rps == null)
            continue;
         for (String rp : rps) {
            if (storedCluster.getVcRps().contains(rp)) {
               continue;
            }
            storedCluster.addVCResourcePool(rp);
         }
      }
   }

   private List<VcCluster> convertToVcCluster(List<VcResourcePoolEntity> rps) {
      if (rps == null || rps.isEmpty()) {
         return null;
      }
      List<VcCluster> vcClusters = new ArrayList<VcCluster>();
      VcCluster prevCluster = null;
      for (VcResourcePoolEntity rp : rps) {
         if (prevCluster == null) {
            prevCluster =
                  new VcCluster(rp.getVcCluster(), new ArrayList<String>());
            vcClusters.add(prevCluster);
         }

         if (rp.getVcCluster().equals(prevCluster.getName())) {
            prevCluster.addVCResourcePool(rp.getVcResourcePool());
         } else {
            prevCluster =
                  new VcCluster(rp.getVcCluster(), new ArrayList<String>());
            vcClusters.add(prevCluster);
            prevCluster.addVCResourcePool(rp.getVcResourcePool());
         }
      }
      return vcClusters;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#getAllRPNames()
    */
   @Override
   public Set<String> getAllRPNames() {
      logger.debug("get all resource pool names.");
      List<VcResourcePoolEntity> rps = rpDao.findAllOrderByClusterName();
      Set<String> result = new HashSet<String>();
      for (VcResourcePoolEntity rp : rps) {
         result.add(rp.getName());
      }
      logger.debug("got resource pool names: " + result);
      return result;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#getAllResourcePoolForRest()
    */
   @Override
   @Transactional
   public List<ResourcePoolRead> getAllResourcePoolForRest() {
      logger.debug("get all resource pools.");

      List<ResourcePoolRead> rps = new ArrayList<ResourcePoolRead>();
      List<VcResourcePoolEntity> entities = rpDao.findAllOrderByClusterName();

      for (VcResourcePoolEntity entity : entities) {
         ResourcePoolRead read = entity.toRest();
         rps.add(read);
      }
      return rps;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#getResourcePoolForRest(java.lang.String)
    */
   @Override
   @Transactional
   public ResourcePoolRead getResourcePoolForRest(final String rpName) {
      logger.debug("get resource pool " + rpName);

      VcResourcePoolEntity entity = rpDao.findByName(rpName);
      if (entity == null) {
         return null;
      }
      return entity.toRest();
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#deleteResourcePool(java.lang.String)
    */
   @Override
   @Transactional
   public synchronized void deleteResourcePool(final String rpName) {
      logger.debug("delete resource pool " + rpName);
      VcResourcePoolEntity entity = rpDao.findByName(rpName);
      if (entity == null) {
         throw VcProviderException.RESOURCE_POOL_NOT_FOUND(rpName);
      }

      List<String> clusterNames =
            getClusterDao().findClustersByUsedResourcePool(rpName);

      if (!clusterNames.isEmpty()) {
         logger.error("cannot remove resource pool, since following clusters referenced it: "
               + clusterNames);
         throw VcProviderException
               .RESOURCE_POOL_IS_REFERENCED_BY_CLUSTER(clusterNames);
      }
      rpDao.delete(entity);
      logger.debug("successfully deleted resource pool " + rpName);
   }

   @Override
   public boolean isDeployedUnderCluster(final String clusterName,
         final String vcRPName) {
      if (VcResourceUtils.findRPInVCCluster(clusterName, vcRPName) != null) {
         return false;
      } else if ((VcResourceUtils.findRPInVCCluster(clusterName, "") != null)
            && ("[" + clusterName + "]").equals(vcRPName)) {
         return true;
      } else {
         return false;
      }
   }

   private String getAutoRandomName() {
      long ran = new Random().nextLong();
      String name = "auto-" + ran;
      boolean found = false;
      for (int i = 0; i < 10; i++) {
         VcResourcePoolEntity rpEntity = rpDao.findByName(name);
         if (rpEntity != null) {
            logger.debug("rp name " + name + " exists, get new one");
         } else {
            found = true;
            break;
         }
      }
      if (found) {
         return name;
      } else {
         return null;
      }
   }

   public synchronized List<String> addAutoResourcePools(
         List<VcCluster> vcClusters, boolean ignoreDuplicate) {
      List<String> rpNames = new ArrayList<String>();
      for (VcCluster vcCluster : vcClusters) {
         List<String> vcRps = vcCluster.getVcRps();
         if (vcRps == null) {
            vcRps = new ArrayList<String>();
         }
         if (vcRps.isEmpty()) {
            // add empty string to represent root rp of vc cluster
            vcRps.add("");
         }
         for (String vcRp : vcRps) {
            if (ignoreDuplicate && rpDao.isRPAdded(vcCluster.getName(), vcRp)) {
               rpNames.add(rpDao.getNameByClusterAndRp(vcCluster.getName(), vcRp));
               continue;
            }
            addAutoResourcePool(rpNames, vcCluster, vcRp);
         }
      }
      return rpNames;
   }

   private void addAutoResourcePool(List<String> rpNames, VcCluster vcCluster,
         String vcRp) {
      String name = getAutoRandomName();
      if (name == null) {
         logger.error("Could not find a random rp name.");
         throw VcProviderException.AUTO_RP_NAME_NOT_FOUND();
      }
      addResourcePool(name, vcCluster.getName(), vcRp);
      rpNames.add(name);
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.service.impl.IResourcePoolService#addResourcePool(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public synchronized void updateResourcePool(String rpName,
         String vcClusterName, String vcResourcePool) {
      VcResourcePoolEntity rp = rpDao.findByName(rpName);
      if (null == rp) {
         logger.debug("resource pool " + vcResourcePool + " in cluster "
               + vcClusterName + " is not existed.");
         throw VcProviderException.RESOURCE_POOL_NOT_FOUND(rpName);
      }
      resService.refreshResourcePool();
      if (!resService.isRPExistInVc(vcClusterName, vcResourcePool)) {
         throw VcProviderException.RESOURCE_POOL_NOT_FOUND(vcResourcePool);
      }

      if ( null != vcClusterName ) {
         rp.setVcCluster(vcClusterName);
      }
      if ( null != vcResourcePool ) {
         rp.setVcResourcePool(vcResourcePool);
      }
      rpDao.update(rp);
   }
}
