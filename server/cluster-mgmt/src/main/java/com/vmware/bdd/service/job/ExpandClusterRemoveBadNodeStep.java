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
package com.vmware.bdd.service.job;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.VcVmUtil;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class ExpandClusterRemoveBadNodeStep extends TrackableTasklet {
   private IClusteringService clusteringService;
   private ClusterConfigManager configMgr;
   private SoftwareManagerCollector softwareMgrs;

   @Autowired
   public void setSoftwareMgrs(SoftwareManagerCollector softwareMgrs) {
      this.softwareMgrs = softwareMgrs;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String clusterName =
            getJobParameters(chunkContext).getString(
                    JobConstants.CLUSTER_NAME_JOB_PARAM);
      String nodeGroupNameList =
              TrackableTasklet.getJobParameters(chunkContext).getString(
                      JobConstants.NEW_NODE_GROUP_LIST_JOB_PARAM);

      List<String> nodeGroupNames = new ArrayList<String>();
      Map<String, Set<String>> occupiedIps = new HashMap<String, Set<String>>();

      for (String nodeGroupName : nodeGroupNameList.split(",")){
         nodeGroupNames.add(nodeGroupName);
      }

      ClusterCreate clusterSpec = configMgr.getClusterConfig(clusterName);
      List<BaseNode> existingNodes = JobUtils.getExistingNodes(
              clusterSpec, getClusterEntityMgr());
      List<BaseNode> deletedNodes = new ArrayList<BaseNode>();

      for (String groupName: nodeGroupNames) {
         long newInstanceNum = clusterSpec.getNodeGroup(groupName).getInstanceNum();
         removeExcessiveOrWrongStatusNodes(existingNodes,
                 deletedNodes, groupName, newInstanceNum);
         JobUtils.removeNonExistNodes(existingNodes, occupiedIps);
      }

      StatusUpdater statusUpdator = new DefaultStatusUpdater(
              jobExecutionStatusHolder, getJobExecutionId(chunkContext));
      deleteServices(getClusterEntityMgr(),
              softwareMgrs.getSoftwareManagerByClusterName(clusterName),
              deletedNodes);
      boolean deleted = false;
      try {
         deleted = clusteringService.syncDeleteVMs(deletedNodes,
               statusUpdator, false);
      } catch (BddException e) {
         String errMsg = "Failed to remove bad nodes for expanding cluster " + clusterName + ": " + e.getMessage();
         JobUtils.recordErrorInClusterOperation(chunkContext, errMsg);
         if (!JobUtils.getJobParameterForceClusterOperation(chunkContext)) {
            throw e;
         }
      }
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_EXISTING_NODES_JOB_PARAM, existingNodes);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_SPEC_JOB_PARAM, clusterSpec);
      putIntoJobExecutionContext(chunkContext,
      JobConstants.CLUSTER_USED_IP_JOB_PARAM, occupiedIps);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_DELETED_NODES_JOB_PARAM, deletedNodes);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_DELETE_VM_OPERATION_SUCCESS, deleted);
      return RepeatStatus.FINISHED;
   }

   private void removeExcessiveOrWrongStatusNodes(List<BaseNode> existingNodes,
                                                  List<BaseNode> deletedNodes, String groupName,
                                                  long newInstanceNum) {
      for(BaseNode node : existingNodes) {
         if (node.getGroupName().equals(groupName)) {
            long index = CommonUtil.getVmIndex(node.getVmName());
            if (index >= newInstanceNum) {
               deletedNodes.add(node);
               continue;
            }
            if (node.getVmMobId() == null) {
               deletedNodes.add(node);
               continue;
            }
            VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getVmMobId());
            Set<String> ips = VcVmUtil.getAllIpAddresses(vm, node.getNics().keySet(), false);
            if (vm == null
                    || (!vm.isPoweredOn())
                    || ips.contains(Constants.NULL_IPV4_ADDRESS)) {
               deletedNodes.add(node);
               continue;
            }
         }
      }
      existingNodes.removeAll(deletedNodes);
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public ClusterConfigManager getConfigMgr() {
      return configMgr;
   }

   public void setConfigMgr(ClusterConfigManager configMgr) {
      this.configMgr = configMgr;
   }


   public static void deleteServices(IClusterEntityManager clusterEntityMgr,
                                     SoftwareManager softMgr, List<BaseNode> toBeDeleted) {
      if (toBeDeleted.isEmpty()) {
         return;
      }

      ClusterBlueprint blueprint =
              clusterEntityMgr.toClusterBluePrint(toBeDeleted.get(0)
                      .getClusterName());
      ClusterReportQueue queue = new ClusterReportQueue();
      List<String> nodeNames = new ArrayList<>();
      for (BaseNode node : toBeDeleted) {
         if (node.getVmMobId() != null) {
            nodeNames.add(node.getVmName());
         }
      }
      try {
         softMgr.onDeleteNodes(blueprint, nodeNames);
      } catch (Exception e) {
         logger.error("Failed to delete services on bad nodes: " + nodeNames);
      }
   }
}
