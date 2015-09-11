/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.util.List;

import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.service.ClusterNodeUpdator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClusterNodeUpdatorImpl implements ClusterNodeUpdator {
   private final static Logger LOGGER = Logger.getLogger(ClusterNodeUpdatorImpl.class);
   private final static long FIVE_MINS_IN_MILLI_SEC = 5 * 60 * 1000;

   @Autowired
   private IClusterEntityManager entityMgr;
   @Autowired
   private IConcurrentLockedClusterEntityManager lockMgr;

   @Scheduled(initialDelay = FIVE_MINS_IN_MILLI_SEC, fixedDelay = FIVE_MINS_IN_MILLI_SEC)
   public void syncAllClusters() {
      List<ClusterEntity> clusters = entityMgr.findAllClusters();
      for (ClusterEntity cluster : clusters) {
         if (cluster.getStatus().isStableStatus()) {
            lockMgr.asyncSyncUp(cluster.getName(), true);

            if(LOGGER.isDebugEnabled()) {
               LOGGER.info("submit sync cluster task: " + cluster.getName());
            }
         }
      }

      LOGGER.info("submit sync all cluster tasks.");
   }
}
