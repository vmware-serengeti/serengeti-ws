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
package com.vmware.bdd.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.SetPasswordException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.ISetPasswordService;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.SetVMPasswordSP;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

public class SetPasswordService implements ISetPasswordService {

   private static final Logger logger = Logger.getLogger(SetPasswordService.class);
   private IClusterEntityManager clusterEntityMgr;

   public ArrayList<String> setPasswordForNodes(String clusterName, List<NodeEntity> nodes, String password) {
      AuAssert.check(!nodes.isEmpty());

      logger.info("Setting password for " + clusterName);
      ArrayList<String> ipsOfNodes = VcVmUtil.getNodePrimaryMgtIPV4sFromEntitys(nodes);
      logger.info("Nodes needed to be set password: " + ipsOfNodes.toString());

      ArrayList<String> failedIPs = null;
      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      for (NodeEntity node : nodes) {
         SetVMPasswordSP setVMPasswordSP = new SetVMPasswordSP(node, password);
         storeProcedures.add(setVMPasswordSP);
      }
      AuAssert.check(!storeProcedures.isEmpty());

      try {
         Callable<Void>[] storeProceduresArray = storeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler.executeStoredProcedures(com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                     storeProceduresArray, callback);

         for (int i = 0; i < storeProceduresArray.length; i++) {
            SetVMPasswordSP sp = (SetVMPasswordSP) storeProceduresArray[i];
            NodeEntity node = sp.getNodeEntity();
            if (result[i].finished && result[i].throwable == null) {
               updateNodeData(node, true, null, null);
            }
            if (!result[i].finished || result[i].throwable != null) {
               String failedNodeIP = sp.getNodeIP();
               if (result[i].throwable != null) {
                  updateNodeData(node, false, result[i].throwable.getMessage(), CommonUtil.getCurrentTimestamp());
                  failedNodeIP = failedNodeIP + ": " + result[i].throwable.getMessage();
               }
               if (failedIPs == null) {
                  failedIPs = new ArrayList<String>();
               }
               failedIPs.add(failedNodeIP);
            }
         }
      } catch (Exception e) {
         String errMsg = " : " + e.getMessage();
         logger.error("Error in setting password for " + clusterName + errMsg);
         throw SetPasswordException.FAIL_TO_SET_PASSWORD(" cluster " + clusterName, errMsg);
      }
      return failedIPs;
   }

   @Override
   public boolean setPasswordForNode(String clusterName, NodeEntity node, String password) throws Exception {
      AuAssert.check(clusterName != null && node != null);

      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      SetVMPasswordSP setVMPasswordSP = new SetVMPasswordSP(node, password);
      try {
         if (setVMPasswordSP.setPasswordForNode()) {
            updateNodeData(node, true, null, null);
            return true;
         }
         logger.error("In SetPasswordService, should not reach here");
         return false;
      } catch (Exception e) {
         String nodeIP = node.getPrimaryMgtIpV4();
         String errMsg = (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
         logger.error("Set password for " + nodeIP + " failed. " + errMsg);
         updateNodeData(node, false, errMsg, CommonUtil.getCurrentTimestamp());
         throw SetPasswordException.FAIL_TO_SET_PASSWORD(nodeIP, errMsg);
      }
   }

   @Transactional
   private void updateNodeData(NodeEntity node, boolean passwordSetted, String errorMessage, String errorTimestamp) {
      node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());
      String nodeVmName = node.getVmName();
      if (passwordSetted) {
         if (node.canBeUpgrade()) {
            logger.info("Successfully upgrade cluster node " + nodeVmName);
            node.setAction(Constants.NODE_ACTION_SET_PASSWORD_SUCCEED);
            node.setActionFailed(false);
            node.setErrMessage(null);
            clusterEntityMgr.update(node);
         }
      } else {
         node.setAction(Constants.NODE_ACTION_SET_PASSWORD_FAILED);
         node.setActionFailed(true);
         String[] messages = errorMessage.split(":");
         if (messages != null && messages.length > 0) {
            node.setErrMessage(errorTimestamp + " " + messages[messages.length-1]);
         } else {
            node.setErrMessage(errorTimestamp + " " + "Setting password for node " + nodeVmName + " failed.");
         }
         clusterEntityMgr.update(node);
      }
   }

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }
}
