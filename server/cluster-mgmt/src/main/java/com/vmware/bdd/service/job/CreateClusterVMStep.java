/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.resmgmt.IVcInventorySyncService;
import com.vmware.bdd.service.resmgmt.VC_RESOURCE_TYPE;
import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.Constants;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.*;

public class CreateClusterVMStep extends TrackableTasklet {
   IClusteringService clusteringService;

   IVcInventorySyncService syncService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      StatusUpdater statusUpdator = new DefaultStatusUpdater(jobExecutionStatusHolder,
            getJobExecutionId(chunkContext));
      List<BaseNode> nodes = getFromJobExecutionContext(chunkContext, JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM,
            new TypeToken<List<BaseNode>>() {}.getType());
      ClusterCreate clusterSpec = getFromJobExecutionContext(chunkContext,JobConstants.CLUSTER_SPEC_JOB_PARAM, ClusterCreate.class);
      Map<String, Set<String>> usedIpSets = getFromJobExecutionContext(chunkContext,
            JobConstants.CLUSTER_USED_IP_JOB_PARAM,
            new TypeToken<Map<String, Set<String>>>() {}.getType());
      if (usedIpSets == null) {
         usedIpSets = new HashMap<>();
      }
      String clusterCloneType = clusterSpec.getClusterCloneType();
      boolean reserveRawDisks = clusterSpec.getDistroVendor().equalsIgnoreCase(Constants.MAPR_VENDOR);
      boolean success = clusteringService.createVcVms(clusterSpec, nodes, usedIpSets,
            reserveRawDisks, statusUpdator);
      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_CREATE_VM_OPERATION_SUCCESS, success);
      putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_ADDED_NODES_JOB_PARAM, nodes);
      asyncRefreshUsedResources(nodes);
      UUID reservationId = getFromJobExecutionContext(chunkContext, JobConstants.CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM, UUID.class);
      if (reservationId != null) {
         // release the resource reservation since vm is created
         clusteringService.commitReservation(reservationId);
         putIntoJobExecutionContext(chunkContext, JobConstants.CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM, null);
      }
      return RepeatStatus.FINISHED;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public void setSyncService(IVcInventorySyncService syncService) {
      this.syncService = syncService;
   }

   public IVcInventorySyncService getSyncService() {
      return syncService;
   }

   private void asyncRefreshUsedResources(List<BaseNode> addedNodes) {
      Set<String> dsNameSet = new HashSet<>();
      for (BaseNode node: addedNodes) {
         for(DiskSpec disk : node.getDisks()) {
            dsNameSet.add(disk.getTargetDs());
         }
      }

      String[] dsNames = new String[dsNameSet.size()];
      dsNameSet.toArray(dsNames);

      syncService.asyncRefreshUsedDatastores(dsNames);
   }

}
