/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.software.external.ExternalProgressMonitor;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IClusterUpgradeService;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.NodeUpgradeSP;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class ClusterUpgradeService implements IClusterUpgradeService {
   private static final Logger logger = Logger
         .getLogger(ClusterUpgradeService.class);

   private String serverVersion;
   @Autowired
   private IClusterEntityManager clusterEntityMgr;
   @Autowired
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   @Autowired
   private SoftwareManagerCollector softwareManagerCollector;
   @Autowired
   private SoftwareManagementService softwareManagementService;

   @Override
   public boolean upgrade(final String clusterName, StatusUpdater statusUpdator) {
      logger.info("Upgrading cluster " + clusterName + ".");

      this.serverVersion = clusterEntityMgr.getServerVersion();
      List<NodeEntity> nodes = getNodes(clusterName);

      // do upgrade
      List<Callable<Void>> storeNodeProcedures = new ArrayList<Callable<Void>>();
      try {
         preUpgradeNode(clusterName);

         for (NodeEntity node : nodes) {
            if (node.needUpgrade(serverVersion)) {
               NodeUpgradeSP nodeUpgradeSP = new NodeUpgradeSP(node, serverVersion);
               storeNodeProcedures.add(nodeUpgradeSP);
            }
         }

         if (storeNodeProcedures.isEmpty()) {
            logger.info("no VM is available. Return directly.");
            return true;
         }

         @SuppressWarnings("unchecked")
         Callable<Void>[] storeNodeProceduresArray = storeNodeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeNodeProceduresArray, callback);

         if (result == null || result.length == 0) {
            logger.error("No node is upgraded.");
            return false;
         }

         boolean success = true;
         for (int i = 0; i < storeNodeProceduresArray.length; i++) {
            Throwable nodeUpgradeSPException = result[i].throwable;
            if (nodeUpgradeSPException != null) {
               success = false;
               break;
            }
         }
         if (success) {
            // this call must be placed before the updateNodeData(node) below,
            // otherwise a @Transactional deadlock will occur.
            postUpgradeNode(clusterName);
         }

         success = true;
         int total = 0;
         for (int i = 0; i < storeNodeProceduresArray.length; i++) {
            Throwable nodeUpgradeSPException = result[i].throwable;
            NodeUpgradeSP sp = (NodeUpgradeSP) storeNodeProceduresArray[i];
            NodeEntity node = sp.getNode();
            if (result[i].finished && nodeUpgradeSPException == null) {
               updateNodeData(node);
               ++total;
            } else if (nodeUpgradeSPException != null) {
               updateNodeData(node, false, nodeUpgradeSPException.getMessage(), CommonUtil.getCurrentTimestamp());
               logger.error("Failed to Upgrade cluster Node " + node.getVmName(), nodeUpgradeSPException);
               success = false;
            }
         }
         logger.info(total + " Nodes are upgraded.");

         return success;
      } catch (InterruptedException e) {
         logger.error("error in upgrading cluster nodes", e);
         throw BddException.UPGRADE(e, e.getMessage());
      }
   }

   private void postUpgradeNode(String clusterName) {
      if (serverVersion.equalsIgnoreCase(Constants.BDE_SERVER_VERSION_2_2)) {
         SoftwareManager softwareManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
         ClusterBlueprint blueprint = clusterEntityMgr.toClusterBluePrint(clusterName);
         if (needToRestartCluster(softwareManager, blueprint)) {
            this.softwareManagementService.configCluster(clusterName);
         }
      }
   }

   private void preUpgradeNode(String clusterName) {
      if (serverVersion.equalsIgnoreCase(Constants.BDE_SERVER_VERSION_2_2)) {
         SoftwareManager softwareManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
         ClusterBlueprint blueprint = clusterEntityMgr.toClusterBluePrint(clusterName);
         if (needToRestartCluster(softwareManager, blueprint)) {
            this.softwareManagementService.stopCluster(clusterName);
         }
      }
   }

   @Override
   public boolean upgradeFailed(final String clusterName, StatusUpdater statusUpdator) {
      boolean upgradeFailed = false;
      List<NodeEntity> nodes = getNodes(clusterName);
      for (NodeEntity node : nodes) {
         if (Constants.NODE_ACTION_UPGRADE_FAILED.equals(node.getAction())) {
            upgradeFailed = true;
            break;
         }
      }
      return upgradeFailed;
   }

   private List<NodeEntity> getNodes(String clusterName) {
      return clusterEntityMgr.findAllNodes(clusterName);
   }

   private void updateNodeData(NodeEntity node) {
      updateNodeData(node, true, null, null);
   }

   @Transactional
   private void updateNodeData(NodeEntity node, boolean upgraded, String errorMessage, String errorTimestamp) {
      node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());
      String nodeVmName = node.getVmName();
      if (upgraded) {
         if (node.canBeUpgrade()) {
            logger.info("Successfully upgrade cluster node " + nodeVmName);
            node.setVersion(serverVersion);
            node.setAction(Constants.NODE_ACTION_UPGRADE_SUCCEED);
            node.setActionFailed(false);
            node.setErrMessage(null);
            clusterEntityMgr.update(node);
         }
      } else {
         logger.error("Failed to upgrade cluster node " + nodeVmName);
         node.setAction(Constants.NODE_ACTION_UPGRADE_FAILED);
         node.setActionFailed(true);
         String[] messages = errorMessage.split(":");
         if (messages != null && messages.length > 0) {
            node.setErrMessage(errorTimestamp + " " + messages[messages.length-1]);
         } else {
            node.setErrMessage(errorTimestamp + " " + "Upgrading node " + nodeVmName + " failed.");
         }
         clusterEntityMgr.update(node);
      }
   }

   private boolean needToRestartCluster(SoftwareManager softwareManager, ClusterBlueprint blueprint) {
      if (softwareManager.hasMountPointStartwithDatax(blueprint.getName())) {
         return false;
      } else {
         return true;
      }
   }
}
