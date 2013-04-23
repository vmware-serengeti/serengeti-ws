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
package com.vmware.bdd.utils;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;

public class TestResourceCleanupUtils {
   private static final Logger logger = Logger
         .getLogger(TestResourceCleanupUtils.class);
   private IResourcePoolService resPoolSvc;
   private IDatastoreService dsSvc;
   private INetworkService netSvc;

   public void setResPoolSvc(IResourcePoolService resPoolSvc) {
      this.resPoolSvc = resPoolSvc;
   }

   public void setDsSvc(IDatastoreService dsSvc) {
      this.dsSvc = dsSvc;
   }

   public void setNetSvc(INetworkService netSvc) {
      this.netSvc = netSvc;
   }

   public void removeCluster(ClusterEntityManager entityMgr, String clusterName) {
      List<ClusterEntity> clusters = entityMgr.findAllClusters();
      for (ClusterEntity cluster : clusters) {
         if (cluster.getName().equals(clusterName)) {
            releaseIp(cluster);
            entityMgr.delete(cluster);
         }
      }
   }

   public void removeClusters(ClusterEntityManager entityMgr) {
      List<ClusterEntity> clusters = entityMgr.findAllClusters();
      for (ClusterEntity cluster : clusters) {
         releaseIp(cluster);
         entityMgr.delete(cluster);
      }
   }

   public void releaseIp(ClusterEntity cluster) {
      logger.info("Free ip adderss of cluster: " + cluster.getName());
      try {
         if (cluster.getNetwork().getAllocType() == NetworkEntity.AllocType.IP_POOL) {
            netSvc.free(cluster.getNetwork(), cluster.getId());
         }
      } catch (Exception e) {
         logger.error("Ignore failure of free ip address for cluster "
               + cluster.getName(), e);
      }
   }

   public void removeNetwork(String networkName) {
      try {
         netSvc.removeNetwork(networkName);
      } catch (Exception e) {
         logger.info("failed to delete network: " + networkName, e);
      }
   }

   public void removeDatastore(String datastoreName) {
      try {
         dsSvc.deleteDatastore(datastoreName);
      } catch (Exception e) {
         logger.info("failed to delete datastore: " + datastoreName, e);
      }
   }

   public void removeRP(String rpName) {
      try {
         resPoolSvc.deleteResourcePool(rpName);
      } catch (Exception e) {
         logger.info("failed to delete resource pool: " + rpName, e);
      }
   }

   public void removeRPs() {
      Set<String> rpNames = resPoolSvc.getAllRPNames();
      for (String rpName : rpNames) {
         removeRP(rpName);
      }
   }
}
