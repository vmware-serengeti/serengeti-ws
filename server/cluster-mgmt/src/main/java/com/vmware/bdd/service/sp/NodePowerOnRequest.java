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
package com.vmware.bdd.service.sp;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.util.CmsWorker.SimpleRequest;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.service.job.software.SoftwareManagementTaskFactory;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.SyncHostsUtils;

public class NodePowerOnRequest extends SimpleRequest {
   private static final Logger logger = Logger
         .getLogger(NodePowerOnRequest.class);
   private IConcurrentLockedClusterEntityManager lockClusterEntityMgr;
   private String vmId;
   private ClusterManager clusterManager;
   private SoftwareManagerCollector softwareManagerCollector;

   public IConcurrentLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IConcurrentLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

   public NodePowerOnRequest(
         IConcurrentLockedClusterEntityManager lockClusterEntityMgr,
         String vmId, ClusterManager clusterManager,
         SoftwareManagerCollector softwareManagerCollector) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
      this.vmId = vmId;
      this.clusterManager = clusterManager;
      this.softwareManagerCollector = softwareManagerCollector;
   }

   @Override
   protected boolean execute() {
      logger.info("Start to waiting for VM " + vmId + " post power on status");
      IClusterEntityManager clusterEntityMgr =
            lockClusterEntityMgr.getClusterEntityMgr();
      String serverVersion = clusterEntityMgr.getServerVersion();
      NodeEntity nodeEntity = clusterEntityMgr.getNodeWithNicsByMobId(vmId);
      if (nodeEntity == null) {
         logger.info("Node " + nodeEntity.getVmName() + " is deleted.");
      }
      boolean needBootstrap = nodeEntity.needUpgrade(serverVersion);
      StartVmPostPowerOn query =
            new StartVmPostPowerOn(nodeEntity.fetchAllPortGroups(),
                  Constants.VM_POWER_ON_WAITING_SEC, clusterEntityMgr);
      query.setVmId(vmId);
      try {
         query.call();
      } catch (Exception e) {
         logger.error("Failed to query ip address of vm: " + vmId, e);
      }
      String clusterName = CommonUtil.getClusterName(nodeEntity.getVmName());
      lockClusterEntityMgr.refreshNodeByMobId(clusterName, vmId, false);

      nodeEntity = clusterEntityMgr.getNodeWithNicsByMobId(vmId);
      if (nodeEntity.isVmReady() && needBootstrap) {
         try {
            bootstrapNode(nodeEntity, clusterName);
         } catch (Exception e) {
            logger.error("Bootstrapping node " + nodeEntity.getVmName()
                  + " failed", e);
            nodeEntity.setStatus(NodeStatus.BOOTSTRAP_FAILED);
            nodeEntity.setActionFailed(true);
            nodeEntity
                  .setErrMessage("Bootstrapping node "
                        + nodeEntity.getVmName()
                        + " failed. Please ssh to this node and run 'sudo chef-client' to get error details.");
            lockClusterEntityMgr.getClusterEntityMgr().update(nodeEntity);
         }
      }

      return true;
   }

   public void bootstrapNode(NodeEntity node, String clusterName) {
      String targetName = node.getVmName();

      logger.info("Start to check host time.");
      ClusterCreate clusterSpec = clusterManager.getClusterSpec(clusterName);

      Set<String> hostnames = new HashSet<String>();
      hostnames.add(node.getHostName());
      SoftwareManager softManager =
            softwareManagerCollector
                  .getSoftwareManagerByClusterName(clusterName);
      SyncHostsUtils.SyncHosts(clusterSpec, hostnames, softManager);

      ISoftwareManagementTask task = null;
      if (!Constants.IRONFAN.equals(softManager.getName())) {
         task =
               createExternalTask(targetName, clusterName);
      } else {
         task = createThriftCommandTask(targetName);
      }

      try {
         Map<String, Object> ret = task.call();

         if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            throw BddException.UPGRADE(null, errorMessage);
         }
      } catch (Exception e) {
         throw BddException.UPGRADE(e, e.getMessage());
      }
   }

   private ISoftwareManagementTask createExternalTask(String targetName,
         String clusterName) {
      SoftwareManager softwareMgr =
            softwareManagerCollector
                  .getSoftwareManagerByClusterName(clusterName);
      ClusterBlueprint clusterBlueprint =
            lockClusterEntityMgr.getClusterEntityMgr().toClusterBluePrint(
                  clusterName);
      return SoftwareManagementTaskFactory.createExternalMgtTask(targetName,
            ManagementOperation.START_NODES, clusterBlueprint, null,
            lockClusterEntityMgr, softwareMgr, null);
   }

   private ISoftwareManagementTask createThriftCommandTask(String targetName) {
      // get command work directory
      File workDir = CommandUtil.createWorkDir((int) Math.random() * 1000);

      // write cluster spec file
      String specFilePath = null;
      File specFile =
            clusterManager.writeClusterSpecFile(targetName, workDir, true);
      specFilePath = specFile.getAbsolutePath();

      ISoftwareManagementTask task =
            createCommandTask(targetName, specFilePath);
      return task;
   }

   // TODO: CM and Ambari
   public ISoftwareManagementTask createCommandTask(String clusterName,
         String specFileName) {
      return SoftwareManagementTaskFactory.createThriftTask(clusterName,
            specFileName, null, ManagementOperation.CONFIGURE,
            lockClusterEntityMgr);
   }

}
