/***************************************************************************
 * Copyright (c) 2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.impl;

import com.vmware.bdd.service.IClusterInitializerService;
import org.apache.log4j.Logger;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.ClusterEntityManager;

import java.util.List;

public class ClusterInitializerService implements IClusterInitializerService {

   private static final Logger logger = Logger.getLogger(ClusterInitializerService.class);

   private ClusterEntityManager clusterEntityManager;

   public ClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(ClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }

   @Override
   public void transformClusterStatus(ClusterStatus from, ClusterStatus to) {
      List<ClusterEntity> allClusters = clusterEntityManager.findAllClusters();
      for (ClusterEntity clusterEntity : allClusters) {
         if (clusterEntity.getStatus().equals(from)) {
            clusterEntity.setStatus(to);
            clusterEntityManager.update(clusterEntity);
            logger.info("update status from " + from.toString() + " to " + to.toString()
                  + " for cluster: " + clusterEntity.getName());
         }
      }
   }
}
