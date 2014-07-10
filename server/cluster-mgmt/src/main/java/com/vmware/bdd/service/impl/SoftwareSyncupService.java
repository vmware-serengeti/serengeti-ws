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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.util.CmsWorker;
import com.vmware.aurora.util.CmsWorker.PeriodicRequest;
import com.vmware.aurora.util.CmsWorker.WorkQueue;
import com.vmware.bdd.aop.annotation.RetryTransaction;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.ISoftwareSyncUpService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;

@Service
public class SoftwareSyncupService implements ISoftwareSyncUpService,
      InitializingBean {
   private static final Logger logger = Logger
         .getLogger(SoftwareSyncupService.class);
   private SoftwareManagerCollector softwareManagerCollector;
   private IExclusiveLockedClusterEntityManager lockedEntityManager;
   private Set<String> inQueueCluster = new HashSet<String>();

   public SoftwareManagerCollector getSoftwareManagerCollector() {
      return softwareManagerCollector;
   }

   @Autowired
   public void setSoftwareManagerCollector(
         SoftwareManagerCollector softwareManagerCollector) {
      this.softwareManagerCollector = softwareManagerCollector;
   }

   public IExclusiveLockedClusterEntityManager getLockedEntityManager() {
      return lockedEntityManager;
   }

   @Autowired
   public void setLockedEntityManager(
         IExclusiveLockedClusterEntityManager lockedEntityManager) {
      this.lockedEntityManager = lockedEntityManager;
   }

   @Override
   public void afterPropertiesSet() throws Exception {
      List<ClusterEntity> clusters = lockedEntityManager.getClusterEntityMgr().findAllClusters();
      for (ClusterEntity cluster : clusters) {
         syncUp(cluster.getName());
         logger.info("Start service sync up for cluster " + cluster.getName());
      }
   }

   @Override
   public void syncUp(String clusterName) {
      StatusSyncUpRequest request =
            new StatusSyncUpRequest(clusterName, lockedEntityManager,
                  softwareManagerCollector, this,
                  WorkQueue.CUSTOM_FIVE_MIN_SYNC_DELAY);
      CmsWorker.addPeriodic(request);
      inQueueCluster.add(clusterName);
   }

   @Override
   public void syncUpOnce(String clusterName) {
      // TODO Auto-generated method stub

   }

   private void removeClusterFromQueue(String clusterName) {
      inQueueCluster.remove(clusterName);
   }

   @Override
   public boolean isClusterInQueue(String clusterName) {
      return inQueueCluster.contains(clusterName);
   }

   private static class StatusSyncUpRequest extends PeriodicRequest {
      private ILockedClusterEntityManager lockedEntityManager;
      private SoftwareManagerCollector softwareManagerCollector;
      private SoftwareSyncupService syncupService;
      private String clusterName;
      private boolean isContinue = true;

      private void setContinue(boolean isContinue) {
         this.isContinue = isContinue;
         if (!isContinue) {
            syncupService.removeClusterFromQueue(clusterName);
            logger.info("Stop service sync up for cluster " + clusterName);
         }
      }

      public StatusSyncUpRequest(String clusterName,
            ILockedClusterEntityManager lockedEntityManager,
            SoftwareManagerCollector softwareManagerCollector,
            SoftwareSyncupService syncupService, WorkQueue queue) {
         super(queue);
         this.clusterName = clusterName;
         this.lockedEntityManager = lockedEntityManager;
         this.softwareManagerCollector = softwareManagerCollector;
         this.syncupService = syncupService;
      }

      @Override
      protected boolean executeOnce() {
         ClusterEntity cluster =
               lockedEntityManager.getClusterEntityMgr()
                     .findByName(clusterName);
         if (cluster == null) {
            setContinue(false);
            return true;
         }

         if (!cluster.inStableStatus()) {
            logger.debug("Cluster " + clusterName + " is in status "
                  + cluster.inStableStatus());
            logger.debug("Do not sync up this time.");
            return true;
         }
         ClusterBlueprint blueprint =
               lockedEntityManager.getClusterEntityMgr().toClusterBluePrint(
                     clusterName);
         SoftwareManager softMgr =
               softwareManagerCollector
                     .getSoftwareManagerByClusterName(clusterName);
         if (softMgr == null) {
            logger.error("No software manager for cluster " + clusterName
                  + " available.");
            return true;
         }
         ClusterReport report = softMgr.queryClusterStatus(blueprint);
         if (report == null) {
            logger.debug("No service status got from software manager, ignore it.");
            return true;
         }
         setClusterStatus(clusterName, report);
         return true;
      }

      @Transactional
      @RetryTransaction
      private void setClusterStatus(String clusterName, ClusterReport report) {
         ClusterEntity cluster =
               lockedEntityManager.getClusterEntityMgr()
                     .findByName(clusterName);
         switch (cluster.getStatus()) {
         case RUNNING:
            if (report.getStatus() != ServiceStatus.RUNNING) {
               cluster.setStatus(ClusterStatus.SERVICE_ERROR);
               logger.info("Got status " + report.getStatus()
                     + ", change cluster status from RUNNING to SERVICE_ERROR.");
            }
            break;
         case PROVISION_ERROR:
         case ERROR:
         case CONFIGURE_ERROR:
         case UPGRADE_ERROR:
         case SERVICE_ERROR:
            if (report.getStatus() == ServiceStatus.RUNNING) {
               cluster.setStatus(ClusterStatus.RUNNING);
               logger.info("Got status " + report.getStatus()
                     + ", change cluster status from " + cluster.getStatus()
                     + " to RUNNING.");
            }
            break;
         default:
            break;
         }

         Map<String, NodeReport> nodeReports = report.getNodeReports();
         List<NodeEntity> nodes =
               lockedEntityManager.getClusterEntityMgr().findAllNodes(
                     clusterName);
         if (nodeReports != null && !nodeReports.isEmpty()) {
            for (NodeEntity node : nodes) {
               if (node.getStatus().ordinal() > NodeStatus.VM_READY.ordinal()) {
                  NodeReport nodeReport = nodeReports.get(node.getVmName());
                  if (nodeReport != null) {
                     NodeStatus oldState = node.getStatus();
                     if (nodeReport.getStatus() == ServiceStatus.RUNNING) {
                        node.setStatus(NodeStatus.SERVICE_READY);
                     } else {
                        node.setStatus(NodeStatus.BOOTSTRAP_FAILED);
                     }
                     if (oldState != node.getStatus()) {
                        logger.info("Got status " + nodeReport.getStatus()
                              + " for node " + node.getVmName()
                              + ", change VM status from " + node.getStatus()
                              + " to " + node.getStatus());
                     }
                  }
               }
            }
         }
      }

      @Override
      protected boolean isContinue() {
         return isContinue;
      }
   }
}
