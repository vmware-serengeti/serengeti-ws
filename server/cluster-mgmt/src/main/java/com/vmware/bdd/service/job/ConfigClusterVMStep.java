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
package com.vmware.bdd.service.job;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.google.gson.reflect.TypeToken;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

public class ConfigClusterVMStep extends TrackableTasklet {
   IClusteringService clusteringService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      StatusUpdater statusUpdator =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));
      List<BaseNode> nodes =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM,
                  new TypeToken<List<BaseNode>>() {
                  }.getType());
      ClusterCreate clusterSpec =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_SPEC_JOB_PARAM, ClusterCreate.class);
      List<BaseNode> existingNodes =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_EXISTING_NODES_JOB_PARAM,
                  new TypeToken<List<BaseNode>>() {
                  }.getType());
      Set<String> occupiedIps = new HashSet<String>();
      addExistingNodes(existingNodes, nodes, occupiedIps);
      boolean success =
            clusteringService.reconfigVms(clusterSpec.getNetworking().get(0),
                  nodes, statusUpdator, occupiedIps);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_CREATE_VM_OPERATION_SUCCESS, success);
      putIntoJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM, nodes);
      UUID reservationId =
            getFromJobExecutionContext(chunkContext,
                  JobConstants.CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM,
                  UUID.class);
      if (reservationId != null) {
         // release the resource reservation since vm is created
         clusteringService.commitReservation(reservationId);
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM, null);
      }
      return RepeatStatus.FINISHED;
   }

   private void addExistingNodes(List<BaseNode> existingNodes,
         List<BaseNode> nodes, Set<String> occupiedIps) {
      if (existingNodes != null) {
         for (BaseNode existNode : existingNodes) {
            if (existNode.getVmMobId() == null) {
               // this will cause node re-cloned.
               logger.info("Node " + existNode.getVmName() + " does not exist.");
               nodes.add(existNode);
               continue;
            }
            VcVirtualMachine vm =
                  VcCache.getIgnoreMissing(existNode.getVmMobId());
            if (vm == null) {
               logger.info("Node " + existNode.getVmName()
                     + " has mob id, but was deleted.");
               nodes.add(existNode);
               continue;
            }
            String ipAddress = VcVmUtil.getIpAddress(vm, false);
            if (!vm.isPoweredOn() || (ipAddress == null)) {
               logger.info("Node " + existNode.getVmName()
                     + " is not ready, reconfig it.");
               nodes.add(existNode);
               continue;
            }
            String haFlag = existNode.getNodeGroup().getHaFlag();
            if (haFlag != null
                  && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
               if (!VcVmUtil.verifyFTState(vm)) {
                  logger.info("FT secondary VM state incorrect for node "
                        + vm.getName() + ", " + "FT state " + vm.getFTState()
                        + " is unexpected.");
                  nodes.add(existNode);
                  continue;
               }
            }
            occupiedIps.add(ipAddress);
            // for vm ready node, we'll not touch it
         }
      }
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

}
