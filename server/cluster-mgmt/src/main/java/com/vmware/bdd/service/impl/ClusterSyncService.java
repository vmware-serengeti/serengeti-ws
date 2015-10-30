/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.impl;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.aop.annotation.ClusterEntityConcurrentWriteLock;
import com.vmware.bdd.aop.annotation.RetryTransaction;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.concurrent.AsyncExecutors;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.utils.Constants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by xiaoliangl on 9/8/15.
 */
@Component
public class ClusterSyncService {
   private static final Logger logger = Logger.getLogger(ClusterSyncService.class);

   private final long MAX_WAIT = Configuration.getLong(Constants.CLUSTER_SYNC_MAX_WAIT_SEC, 5*60*1000);
   private final long TIME_SLICE = 100l;

   @Autowired
   private IClusterEntityManager clusterEntityMgr;

   @Autowired
   private NodeSyncService nodeSyncService;

   public void syncUp(String clusterName, boolean updateClusterStatus) {
      if (logger.isDebugEnabled()) {
         logger.debug("start to sync cluster: " + clusterName);
      }

      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);

      boolean allNodesDown = true;

      List<Future<NodeRead>> refreshedNodeList = new ArrayList<>();
      for (NodeEntity node : nodes) {
         refreshedNodeList.add(nodeSyncService.asyncRefreshNodeStatus(node.getVmName()));
      }

      //wait all node refresh is done
      long elapsed = 0l;
      while (CollectionUtils.isNotEmpty(refreshedNodeList)) {
         for (Iterator<Future<NodeRead>> futureItr = refreshedNodeList.iterator(); futureItr.hasNext(); ) {
            Future<NodeRead> refreshedNodeFuture = futureItr.next();
            if(refreshedNodeFuture.isDone()) {
               try {
                  NodeRead refreshedNode = refreshedNodeFuture.get();
                  if (logger.isDebugEnabled()) {
                     logger.debug("got sync node result: " + refreshedNode.getName());
                  }

                  if (NodeStatus.fromString(refreshedNode.getStatus()).ordinal() >= NodeStatus.POWERED_ON.ordinal()) {
                     allNodesDown = false;
                  }
               } catch (InterruptedException e) {
                  logger.error("failed to get async refresh node result", e);
               } catch (ExecutionException e) {
                  logger.error("failed to get async refresh node result", e);
               } finally {
                  futureItr.remove();
               }
            }
         }

         try {
            Thread.sleep(TIME_SLICE);
            elapsed += TIME_SLICE;

            if(elapsed >= MAX_WAIT) {
               break;
            }
         } catch (InterruptedException e) {
            //nothing to do
         }

         if(logger.isDebugEnabled()) {
            logger.debug("sync cluster: " + clusterName);
         }
      }

      if(CollectionUtils.isNotEmpty(refreshedNodeList)) {
         logger.warn("failed to sync all nodes status in given time interval: " + clusterName);
      } else {
         logger.info(String.format("sync all node status of cluster: %1s in %2s milliseconds", clusterName, elapsed));
      }

      if (updateClusterStatus && allNodesDown) {
         ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
         if (cluster.getStatus() == ClusterStatus.RUNNING) {
            logger.info("All nodes are powered off, switch cluster status to stopped.");
            cluster.setStatus(ClusterStatus.STOPPED);
         }
      }
   }
}
