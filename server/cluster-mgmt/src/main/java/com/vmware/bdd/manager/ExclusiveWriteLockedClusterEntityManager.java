/***************************************************************************
 * Copyright (c) 2013-2014 VMware, Inc. All Rights Reserved.
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

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.vmware.bdd.manager.concurrent.AsyncExecutors;
import com.vmware.bdd.service.impl.ClusterSyncService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.vmware.bdd.aop.annotation.ClusterEntityExclusiveWriteLock;
import com.vmware.bdd.aop.lock.LockFactory;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;

/**
 * This class will lock exclusively to call cluster entity manager. The lock
 * will be released after the method is finished. Be careful to use this class
 * inside of Spring Batch step thread, as Spring Batch step does not commit txn
 * until its own txn is committed.
 *
 * @author line
 *
 */
@Component
public class ExclusiveWriteLockedClusterEntityManager implements
      IExclusiveLockedClusterEntityManager {
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
   @ClusterEntityExclusiveWriteLock
   public boolean handleOperationStatus(String clusterName,
         OperationStatusWithDetail status, boolean lastUpdate) {
      return clusterEntityMgr.handleOperationStatus(clusterName, status, lastUpdate);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void syncUp(String clusterName, boolean updateClusterStatus) {
      clusterSyncService.syncUp(clusterName, updateClusterStatus);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   @Async(AsyncExecutors.CLUSTER_SYNC_EXEC)
   public void asyncSyncUp(String clusterName, boolean updateClusterStatus) {
      clusterSyncService.syncUp(clusterName, updateClusterStatus);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void removeVmReference(String clusterName, String vmId) {
      clusterEntityMgr.removeVmReference(vmId);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void syncUpNode(String clusterName, String nodeName) {
      clusterEntityMgr.syncUpNode(clusterName, nodeName);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void refreshNodeByMobId(String clusterName, String vmId,
         boolean inSession) {
      clusterEntityMgr.refreshNodeByMobId(vmId, inSession);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void setNodeConnectionState(String clusterName, String vmName) {
      clusterEntityMgr.setNodeConnectionState(vmName);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void refreshNodeByMobId(String clusterName, String vmId,
         String action, boolean inSession) {
      clusterEntityMgr.refreshNodeByMobId(vmId, action, inSession);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void refreshNodeByVmName(String clusterName, String vmId,
         String vmName, boolean inSession) {
      clusterEntityMgr.refreshNodeByVmName(vmId, vmName, inSession);
   }

   @Override
   @ClusterEntityExclusiveWriteLock
   public void refreshNodeByVmName(String clusterName, String vmId,
         String vmName, String nodeAction, boolean inSession) {
      clusterEntityMgr.refreshNodeByVmName(vmId, vmName, nodeAction, inSession);
   }

   public ReentrantReadWriteLock.WriteLock getLock(String clusterName) {
      return LockFactory.getClusterLock(clusterName).writeLock();
   }

   @Override
   public boolean handleOperationStatus(String clusterName,
         ClusterReport report, boolean lastUpdate) {
      return clusterEntityMgr.handleOperationStatus(clusterName, report, lastUpdate);
   }
}
