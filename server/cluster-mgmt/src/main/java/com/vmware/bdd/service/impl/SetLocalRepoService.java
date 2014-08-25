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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.SetLocalRepoException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.ISetLocalRepoService;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.SetLocalRepoSP;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

public class SetLocalRepoService implements ISetLocalRepoService {
   private static final Logger logger = Logger
         .getLogger(ExecutionService.class);

   private IClusterEntityManager clusterEntityMgr;

   public SetLocalRepoService() {
   }

   @Override
   public boolean setLocalRepoForNodes(String clusterName,
         List<NodeEntity> nodes, String repoId, String localRepoURL) {
      AuAssert.check(!nodes.isEmpty());

      logger.info("Setting password for " + clusterName);
      ArrayList<String> ipsOfNodes =
            VcVmUtil.getNodePrimaryMgtIPV4sFromEntitys(nodes);
      logger.info("Nodes needed to be set password: " + ipsOfNodes.toString());

      boolean succeed = true;
      List<Callable<Void>> storeProcedures = new ArrayList<Callable<Void>>();
      for (NodeEntity node : nodes) {
         SetLocalRepoSP setLocalRepoSP =
               new SetLocalRepoSP(node, repoId, localRepoURL);
         storeProcedures.add(setLocalRepoSP);
      }
      AuAssert.check(!storeProcedures.isEmpty());

      try {
         Callable<Void>[] storeProceduresArray =
               storeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
                     .executeStoredProcedures(
                           com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                           storeProceduresArray, callback);

         for (int i = 0; i < storeProceduresArray.length; i++) {
            SetLocalRepoSP sp = (SetLocalRepoSP) storeProceduresArray[i];
            NodeEntity node = sp.getNodeEntity();
            String vmNameWithIP = node.getVmNameWithIP();
            if (result[i].finished && result[i].throwable == null) {
               updateNodeData(node, true, null, null);
               logger.info("Set local repo: store procedure succeed for "
                     + vmNameWithIP);
            }
            if (!result[i].finished || result[i].throwable != null) {
               succeed = false;
               if (result[i].throwable != null) {
                  String errMsg = result[i].throwable.getMessage();
                  updateNodeData(node, false, errMsg,
                        CommonUtil.getCurrentTimestamp());
                  logger.error("Set local repo: store procedure failed for "
                        + vmNameWithIP + ": " + errMsg);
               }
            }
         }
      } catch (Exception e) {
         //place holder in case of known error, in this case, we just log it and
         //throw cli exception, we don't set node task field
         String errMsg = " : " + e.getMessage();
         logger.error("Unknown error in setting local repo for " + clusterName,
               e);
         throw SetLocalRepoException.FAIL_TO_SET_LOCAL_REPO(" cluster "
               + clusterName, errMsg);
      }

      return succeed;
   }

   @Override
   public boolean setLocalRepoForNode(String clusterName, NodeEntity node,
         String repoId, String localRepoURL) throws Exception {
      AuAssert.check(clusterName != null && node != null);

      SetLocalRepoSP setLocalRepoSP = new SetLocalRepoSP(node, repoId, localRepoURL);
      String vmNameWithIP = node.getVmNameWithIP();
      try {
         if (setLocalRepoSP.setupNodeLocalRepo()) {
            updateNodeData(node, true, null, null);
            logger.info("Set local repo for " + vmNameWithIP + " succeed.");
            return true;
         }
         //we fail by throwing exceptions
         logger.error("Should not reach here");
         return false;
      } catch (Exception e) {
         String errMsg = (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
         updateNodeData(node, false, errMsg, CommonUtil.getCurrentTimestamp());
         logger.error("Set local repo for " + vmNameWithIP + " failed. ", e);
         return false;
      }
   }

   @Transactional
   public void updateNodeData(NodeEntity node, boolean localRepoSetted, String errorMessage, String errorTimestamp) {
      node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());
      String nodeNameWithIP = node.getVmNameWithIP();
      if (localRepoSetted) {
         if (node.canBeUpgrade()) {
            node.setAction(Constants.NODE_ACTION_SET_LOCAL_REPO_SUCCEED);
            node.setActionFailed(false);
            node.setErrMessage(null);
            clusterEntityMgr.update(node);
         }
      } else {
         node.setAction(Constants.NODE_ACTION_SET_LOCAL_REPO_FAILED);
         node.setActionFailed(true);
         String[] messages = errorMessage.split(":");
         if (messages != null && messages.length > 0) {
            node.setErrMessage(errorTimestamp + " " + messages[messages.length-1]);
         } else {
            node.setErrMessage(errorTimestamp + " " + "Setting local repo for " + nodeNameWithIP + " failed.");
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
