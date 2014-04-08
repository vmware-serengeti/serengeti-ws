/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.util.CmsWorker.SimpleRequest;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IConcurrentLockedClusterEntityManager;
import com.vmware.bdd.service.impl.ClusterUpgradeService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

public class NodePowerOnRequest extends SimpleRequest {
   private static final Logger logger = Logger
         .getLogger(NodePowerOnRequest.class);
   private IConcurrentLockedClusterEntityManager lockClusterEntityMgr;
   private String vmId;

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
         String vmId) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
      this.vmId = vmId;
   }

   @Override
   protected boolean execute() {
      logger.info("Start to waiting for VM " + vmId + " post power on status");
      NodeEntity nodeEntity =
            lockClusterEntityMgr.getClusterEntityMgr().getNodeWithNicsByMobId(vmId);
      if (nodeEntity == null) {
         logger.info("Node " + nodeEntity.getVmName() + " is deleted.");
      }
      QueryIpAddress query =
            new QueryIpAddress(nodeEntity.fetchAllPortGroups(),
                  Constants.VM_POWER_ON_WAITING_SEC);
      query.setVmId(vmId);
      try {
         query.call();
      } catch (Exception e) {
         logger.error("Failed to query ip address of vm: " + vmId, e);
      }
      String clusterName = CommonUtil.getClusterName(nodeEntity.getVmName());
      lockClusterEntityMgr
            .refreshNodeByMobId(clusterName, vmId, false);

      //upgrade node if needed
      ClusterUpgradeService upgradeService = new ClusterUpgradeService();
      IClusterEntityManager clusterEntityMgr = lockClusterEntityMgr.getClusterEntityMgr();
      upgradeService.setClusterEntityMgr(clusterEntityMgr);
      logger.debug("vm " + nodeEntity.getVmName() + "is going to upgrade");
      clusterEntityMgr.updateNodeAction(nodeEntity, Constants.NODE_ACTION_UPGRADING);
      upgradeService.upgradeNode(nodeEntity);
      return true;
   }
}
