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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   private static final int SYNCUP_INTERVAL_MILLISECONDS = 300000; // every 5 minutes
   private static final Logger logger = Logger
         .getLogger(SoftwareSyncupService.class);
   private static final int MAX_QUEUE_SIZE = 1000;
   private static final String SERVICE_SYNCUP_THREAD_NAME = "ServiceSyncup";
   private SoftwareManagerCollector softwareManagerCollector;
   private IExclusiveLockedClusterEntityManager lockedEntityManager;
   private BlockingQueue<String> requestQueue = new ArrayBlockingQueue<String>(
         MAX_QUEUE_SIZE);
   private Timer syncupTimer;

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
      List<ClusterEntity> clusters =
            lockedEntityManager.getClusterEntityMgr().findAllClusters();
      for (ClusterEntity cluster : clusters) {
         requestQueue.add(cluster.getName());
         logger.info("Start service sync up for cluster " + cluster.getName());
      }
      syncupTimer = new Timer(SERVICE_SYNCUP_THREAD_NAME, true);
      StatusSyncUpTask task =
            new StatusSyncUpTask(lockedEntityManager, softwareManagerCollector,
                  requestQueue);
      syncupTimer.schedule(task, SYNCUP_INTERVAL_MILLISECONDS);
   }

   @Override
   public void syncUp(String clusterName) {
      requestQueue.add(clusterName);
   }

   @Override
   public void syncUpOnce(String clusterName) {
      // TODO Auto-generated method stub

   }

   private static class StatusSyncUpTask extends TimerTask {
      private ILockedClusterEntityManager lockedEntityManager;
      private SoftwareManagerCollector softwareManagerCollector;
      private BlockingQueue<String> requestQueue;

      public StatusSyncUpTask(ILockedClusterEntityManager lockedEntityManager,
            SoftwareManagerCollector softwareManagerCollector,
            BlockingQueue<String> requestQueue) {
         this.lockedEntityManager = lockedEntityManager;
         this.softwareManagerCollector = softwareManagerCollector;
         this.requestQueue = requestQueue;
      }

      @Override
      public void run() {
         // using set to remove duplicate
         Set<String> clusterList = new HashSet<String>();
         requestQueue.drainTo(clusterList);
         if (clusterList.isEmpty()) {
            logger.debug("No cluster need to be sync up for service status.");
            return;
         }

         try {
            Iterator<String> ite = clusterList.iterator();
            for (String clusterName = ite.next(); ite.hasNext();) {
               ClusterEntity cluster =
                     lockedEntityManager.getClusterEntityMgr().findByName(
                           clusterName);
               if (cluster == null) {
                  logger.info("Cluster " + clusterName
                        + " does not exist, stop sync up for it.");
                  ite.remove();
                  continue;
               }

               if (!cluster.inStableStatus()) {
                  logger.debug("Cluster " + clusterName + " is in status "
                        + cluster.inStableStatus());
                  logger.debug("Do not sync up this time.");
                  continue;
               }
               ClusterBlueprint blueprint =
                     lockedEntityManager.getClusterEntityMgr()
                           .toClusterBluePrint(clusterName);
               SoftwareManager softMgr =
                     softwareManagerCollector
                           .getSoftwareManagerByClusterName(clusterName);
               if (softMgr == null) {
                  logger.error("No software manager for cluster " + clusterName
                        + " available.");
                  continue;
               }
               ClusterReport report = softMgr.queryClusterStatus(blueprint);
               if (report == null) {
                  logger.debug("No service status got from software manager, ignore it.");
                  continue;
               }
               setClusterStatus(clusterName, report);
            }
         } catch (Exception e) {
            logger.error(
                  "Failed to syncup cluster status for " + e.getMessage(), e);
         }
         //add back all clusters for next time sync up.
         requestQueue.addAll(clusterList);
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
   }
}
