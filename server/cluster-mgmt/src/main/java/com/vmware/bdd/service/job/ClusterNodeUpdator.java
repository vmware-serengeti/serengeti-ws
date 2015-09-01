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
package com.vmware.bdd.service.job;

import java.util.List;

import com.vmware.aurora.util.worker.PeriodicRequest;
import com.vmware.aurora.util.worker.CmsWorker.WorkQueue;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import org.apache.log4j.Logger;

public class ClusterNodeUpdator extends PeriodicRequest {
   private final static Logger LOGGER = Logger.getLogger(ClusterNodeUpdator.class);
   
   private IClusterEntityManager entityMgr;
   private IConcurrentLockedClusterEntityManager lockMgr;
   private final static long FIVE_MINS_IN_MILLI_SEC = 5 * 60 * 1000;
   
   public ClusterNodeUpdator(IConcurrentLockedClusterEntityManager lockMgr) {
      super(WorkQueue.VC_TASK_FIVE_MIN_DELAY, FIVE_MINS_IN_MILLI_SEC);
      this.lockMgr = lockMgr;
      this.entityMgr = lockMgr.getClusterEntityMgr();
   }

   public boolean executeOnce() {
      LOGGER.info("start sync all clusters' nodes");
      List<ClusterEntity> clusters = entityMgr.findAllClusters();
      for (ClusterEntity cluster : clusters) {
         if (cluster.getStatus().isStableStatus()) {
            long timeMillis = 0l;
            if(LOGGER.isDebugEnabled()) {
               timeMillis = System.currentTimeMillis();
            }

            syncUp(cluster.getName());

            if(LOGGER.isDebugEnabled()) {
               LOGGER.debug("syncing cluster " + cluster.getName() + " finished in milliseconds: " + (System.currentTimeMillis() - timeMillis));
            } else {
               LOGGER.info("syncing cluster " + cluster.getName() + " finished");
            }
         }
      }

      LOGGER.info("finish sync all clusters' nodes");
      return true;
   }

   public void syncUp(String clusterName) {
      lockMgr.syncUp(clusterName, true);
   }
}
