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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.spectypes.VcCluster;

public class VcResourcePoolManager {
   private static final Logger logger = Logger
         .getLogger(VcResourcePoolManager.class);

   public List<VcCluster> getAllVcResourcePool() {
      // load vc resource pools
      List<VcResourcePoolEntity> rps =
            VcResourcePoolEntity.findAllOrderByClusterName();
      List<VcCluster> result = convertToVcCluster(rps);
      logger.debug("got resource pools: " + result);
      return result;
   }

   public void addResourcePool(String rpName, String vcClusterName,
         String vcResourcePool) {
      boolean existed =
            VcResourcePoolEntity.isRPAdded(vcClusterName, vcResourcePool);
      if (existed) {
         logger.debug("resource pool " + vcResourcePool + " in cluster "
               + vcClusterName + " is already existed.");
         throw VcProviderException.VC_RESOURCE_POOL_ALREADY_ADDED(vcResourcePool);
      }

      addResourcePoolEntity(rpName, vcClusterName, vcResourcePool);
   }

   private void addResourcePoolEntity(final String rpName,
         final String vcClusterName, final String vcResourcePool) {
      DAL.inTransactionDo(new Saveable<Void>() {
         public Void body() {
            if (VcResourcePoolEntity.findByName(rpName) != null) {
               logger.info("resource pool name " + rpName + " is already existed.");
               throw BddException.ALREADY_EXISTS("resource pool", rpName);
            }
            VcResourcePoolEntity entity = new VcResourcePoolEntity();
            entity.setName(rpName);
            entity.setVcCluster(vcClusterName);
            entity.setVcResourcePool(vcResourcePool);
            entity.insert();
            logger.info("add resource pool " + rpName);
            return null;
         }
      });
   }

   public List<VcCluster> getVcResourcePoolByName(String name) {
      logger.debug("get resource pools by name " + name);
      VcResourcePoolEntity rp = VcResourcePoolEntity.findByName(name);
      List<VcResourcePoolEntity> rps = new ArrayList<VcResourcePoolEntity>();
      rps.add(rp);
      List<VcCluster> result =  convertToVcCluster(rps);
      logger.debug("got resource pools: " + result);
      return result;
   }

   public List<VcCluster> getVcResourcePoolByNameList(String[] names) {
      logger.debug("get resource pools by name list " + Arrays.toString(names));
      if (names == null || names.length == 0) {
         return null;
      }
      Map<String, VcCluster> clusterMap = new HashMap<String, VcCluster>(); // key: cluster name
      for (String name : names) {
         VcResourcePoolEntity rpForName =
               VcResourcePoolEntity.findByName(name);
         if (rpForName == null) {
            logger.warn("Specified resource pool " + name + ", but the resource pool is removed by others. Continue to use other resource pools.");
            continue;
         }
         combineCluster(clusterMap, rpForName);
      }
      if (clusterMap.values().isEmpty()) {
         throw VcProviderException.NO_RESOURCE_POOL_FOUND(names);
      }
      List<VcCluster> result = new ArrayList<VcCluster>();
      result.addAll(clusterMap.values());
      logger.debug("got resource pools: " + result);
      return result;
   }

   private void combineCluster(Map<String, VcCluster> clusterMap,
         VcResourcePoolEntity rpEntity) {
      List<VcResourcePoolEntity> rpEntities = new ArrayList<VcResourcePoolEntity>();
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
         List<String> rps = vcCluster.getVc_rps();
         VcCluster storedCluster = clusterMap.get(vcCluster.getName());
         if (rps == null) continue;
         for(String rp : rps) {
            if (storedCluster.getVc_rps().contains(rp)) {
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

   public Set<String> getAllRPNames() {
      logger.debug("get all resource pool names.");
      List<VcResourcePoolEntity> rps = VcResourcePoolEntity.findAllOrderByClusterName();
      Set<String> result = new HashSet<String>();
      for (VcResourcePoolEntity rp : rps) {
         result.add(rp.getName());
      }
      logger.debug("got resource pool names: " + result);
      return result;
   }

   public List<ResourcePoolRead> getAllResourcePoolForRest() {
      logger.debug("get all resource pools.");
      return DAL.inRoTransactionDo(new Saveable<List<ResourcePoolRead>>() {
         @Override
         public List<ResourcePoolRead> body() {
            List<ResourcePoolRead> rps = new ArrayList<ResourcePoolRead>();
            List<VcResourcePoolEntity> entities = VcResourcePoolEntity.findAllOrderByClusterName();

            for (VcResourcePoolEntity entity : entities) {
               ResourcePoolRead read = entity.toRest();
               rps.add(read);
            }

            return rps;
         }
      });
   }

   public ResourcePoolRead getResourcePoolForRest(final String rpName) {
      logger.debug("get resource pool " + rpName);
      return DAL.inRoTransactionDo(new Saveable<ResourcePoolRead>() {
         @Override
         public ResourcePoolRead body() {
            VcResourcePoolEntity entity = VcResourcePoolEntity.findByName(rpName);
            if (entity == null) {
               return null;
            }
            return entity.toRest();
         }
      });
   }

   public void deleteResourcePool(final String rpName) {
      logger.debug("delete resource pool " + rpName);
      VcResourcePoolEntity entity = VcResourcePoolEntity.findByName(rpName);
      if (entity == null) {
         throw VcProviderException.RESOURCE_POOL_NOT_FOUND(rpName);
      }

      List<String> clusterNames = ClusterEntity.findClusterNamesByUsedResourcePool(rpName);

      if (!clusterNames.isEmpty()) {
         logger.error("cannot remove resource pool, since following clusters referenced it: " + clusterNames);
         throw VcProviderException.RESOURCE_POOL_IS_REFERENCED_BY_CLUSTER(clusterNames);
      }

      DAL.inTransactionDelete(entity);
      logger.debug("successfully deleted resource pool " + rpName);
   }


}
