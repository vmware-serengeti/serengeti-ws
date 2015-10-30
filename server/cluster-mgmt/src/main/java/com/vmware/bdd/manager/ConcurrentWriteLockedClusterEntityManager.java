/***************************************************************************
 * Copyright (c) 2013-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.manager.concurrent.AsyncExecutors;
import com.vmware.bdd.service.impl.ClusterSyncService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.vmware.bdd.aop.annotation.ClusterEntityConcurrentWriteLock;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;

@Component
public class ConcurrentWriteLockedClusterEntityManager implements
      IConcurrentLockedClusterEntityManager {
   private static final Logger logger = Logger.getLogger(ConcurrentWriteLockedClusterEntityManager.class);

   private IClusterEntityManager clusterEntityMgr;

   @Autowired
   private ClusterSyncService clusterSyncService;

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public boolean handleOperationStatus(String clusterName,
         OperationStatusWithDetail status, boolean lastUpdate) {
      return clusterEntityMgr.handleOperationStatus(clusterName, status, lastUpdate);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void syncUp(String clusterName, boolean updateClusterStatus) {
      logger.info("syncup cluster: " + clusterName);
      clusterSyncService.syncUp(clusterName, updateClusterStatus);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   @Async(AsyncExecutors.CLUSTER_SYNC_EXEC)
   public void asyncSyncUp(String clusterName, boolean updateClusterStatus) {
      logger.info("async syncup cluster: " + clusterName);
      clusterSyncService.syncUp(clusterName, updateClusterStatus);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void removeVmReference(String clusterName, String vmId) {
      clusterEntityMgr.removeVmReference(vmId);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void syncUpNode(String clusterName, String nodeName) {
      clusterEntityMgr.syncUpNode(clusterName, nodeName);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void refreshNodeByMobId(String clusterName, String vmId,
         boolean inSession) {
      clusterEntityMgr.refreshNodeByMobId(vmId, inSession);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void setNodeConnectionState(String clusterName, String vmName) {
      clusterEntityMgr.setNodeConnectionState(vmName);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void refreshNodeByMobId(String clusterName, String vmId,
         String action, boolean inSession) {
      clusterEntityMgr.refreshNodeByMobId(vmId, action, inSession);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void refreshNodeByVmName(String clusterName, String vmId,
         String vmName, boolean inSession) {
      clusterEntityMgr.refreshNodeByVmName(vmId, vmName, inSession);
   }

   @Override
   @ClusterEntityConcurrentWriteLock
   public void refreshNodeByVmName(String clusterName, String vmId,
         String vmName, String nodeAction, boolean inSession) {
      clusterEntityMgr.refreshNodeByVmName(vmId, vmName, nodeAction, inSession);
   }

   @Override
   public boolean handleOperationStatus(String clusterName,
         ClusterReport report, boolean lastUpdate) {
      return clusterEntityMgr.handleOperationStatus(clusterName, report, lastUpdate);
   }
}
