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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.util.CmsWorker.SimpleRequest;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.service.job.software.SoftwareManagementTaskFactory;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class NodePowerOnRequest extends SimpleRequest {
   private static final Logger logger = Logger
         .getLogger(NodePowerOnRequest.class);
   private IConcurrentLockedClusterEntityManager lockClusterEntityMgr;
   private String vmId;
   private ClusterManager clusterManager;

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
         String vmId, ClusterManager clusterManager) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
      this.vmId = vmId;
      this.clusterManager = clusterManager;
   }

   @Override
   protected boolean execute() {
      logger.info("Start to waiting for VM " + vmId + " post power on status");
      IClusterEntityManager clusterEntityMgr = lockClusterEntityMgr.getClusterEntityMgr();
      String serverVersion = clusterEntityMgr.getServerVersion();
      NodeEntity nodeEntity = clusterEntityMgr.getNodeWithNicsByMobId(vmId);
      if (nodeEntity == null) {
         logger.info("Node " + nodeEntity.getVmName() + " is deleted.");
      }
      boolean needBootstrap = nodeEntity.needUpgrade(serverVersion);
      StartVmPostPowerOn query = new StartVmPostPowerOn(nodeEntity.fetchAllPortGroups(), Constants.VM_POWER_ON_WAITING_SEC, clusterEntityMgr);
      query.setVmId(vmId);
      try {
         query.call();
      } catch (Exception e) {
         logger.error("Failed to query ip address of vm: " + vmId, e);
      }
      String clusterName = CommonUtil.getClusterName(nodeEntity.getVmName());
      lockClusterEntityMgr.refreshNodeByMobId(clusterName, vmId, false);

      nodeEntity = clusterEntityMgr.getNodeWithNicsByMobId(vmId);
      if (needBootstrap) {
         bootstrapNode(nodeEntity, clusterName);
      }

      return true;
   }

   public void bootstrapNode(NodeEntity node, String clusterName) {
      String targetName = node.getVmName();

      logger.info("Start to check host time.");
      ClusterCreate clusterSpec = clusterManager.getClusterSpec(clusterName);

      String hostname = node.getHostName();
      int maxTimeDiffInSec = Constants.MAX_TIME_DIFF_IN_SEC;
      if (clusterSpec.checkHBase())
         maxTimeDiffInSec = Constants.MAX_TIME_DIFF_IN_SEC_HBASE;
      List<String> outOfSyncHosts = new ArrayList<String>();
      int hostTimeDiffInSec = VcResourceUtils.getHostTimeDiffInSec(hostname);
      if (Math.abs(hostTimeDiffInSec) > maxTimeDiffInSec) {
         logger.info("Host " + hostname + " has a time difference of "
               + hostTimeDiffInSec + " seconds and is dropped from placement.");
         outOfSyncHosts.add(hostname);
      }
      if (!outOfSyncHosts.isEmpty()) {
         String managementServerHost = VcResourceUtils.getManagementServerHost();
         logger.error("Time on host " + outOfSyncHosts
               + "is out of sync which will lead to failure, "
               + "synchronize the time on these hosts with "
               + "Serengeti management server and try again.");
         throw TaskException.HOST_TIME_OUT_OF_SYNC(outOfSyncHosts,
               managementServerHost);
      }

      // get command work directory
      File workDir = CommandUtil.createWorkDir((int)Math.random()*1000);

      // write cluster spec file
      String specFilePath = null;
      File specFile = clusterManager.writeClusterSpecFile(targetName, workDir, true);
      specFilePath = specFile.getAbsolutePath();

      ISoftwareManagementTask task =  createCommandTask(targetName, specFilePath);
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

   public ISoftwareManagementTask createCommandTask(String clusterName, String specFileName) {
      return SoftwareManagementTaskFactory.createCommandTask(clusterName, specFileName, null, ManagementOperation.CONFIGURE, lockClusterEntityMgr);
   }

}
