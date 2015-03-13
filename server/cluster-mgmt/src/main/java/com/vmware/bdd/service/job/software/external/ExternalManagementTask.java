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
package com.vmware.bdd.service.job.software.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.bdd.utils.JobUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;

import com.vmware.aurora.util.AuAssert;
import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.utils.CommonUtil;
/**
 * Author: Xiaoding Bian
 * Date: 6/11/14
 * Time: 2:58 PM
 */
public class ExternalManagementTask implements ISoftwareManagementTask {

   private static final Logger logger = Logger.getLogger(ExternalManagementTask.class);
   private String targetName;
   private ManagementOperation managementOperation;
   private ClusterBlueprint clusterBlueprint;
   private StatusUpdater statusUpdater;
   private ILockedClusterEntityManager lockedClusterEntityManager;
   private SoftwareManager softwareManager;
   private ChunkContext chunkContext;

   public ExternalManagementTask(String targetName, ManagementOperation managementOperation,
         ClusterBlueprint clusterBlueprint, StatusUpdater statusUpdater,
         ILockedClusterEntityManager lockedClusterEntityManager, SoftwareManager softwareManager,
         ChunkContext chunkContext) {
      this.targetName = targetName;
      this.managementOperation = managementOperation;
      this.clusterBlueprint = clusterBlueprint;
      this.statusUpdater = statusUpdater;
      this.lockedClusterEntityManager = lockedClusterEntityManager;
      this.softwareManager = softwareManager;
      this.chunkContext = chunkContext;
   }

   @Override
   public Map<String, Object> call() throws Exception {

      Map<String, Object> result = new HashMap<String, Object>();

      ClusterReportQueue queue = new ClusterReportQueue();
      Thread progressThread = null;
      ExternalProgressMonitor monitor =
            new ExternalProgressMonitor(targetName, queue, statusUpdater,
                  lockedClusterEntityManager);
      progressThread = new Thread(monitor, "ProgressMonitor-" + targetName);
      progressThread.setDaemon(true);
      progressThread.start();

      boolean success = false;

      try {
         switch(managementOperation) {
            case CREATE:
               success = softwareManager.createCluster(clusterBlueprint, queue);
               break;
            case CONFIGURE:
               success = softwareManager.reconfigCluster(clusterBlueprint, queue);
               break;
            case PRE_DESTROY:
               if (softwareManager == null) {
                  logger.warn("Software manager was unavailable when deleting cluster " + clusterBlueprint.getName() + ", will skip it and delete vms forcely");
                  logger.warn("You may need to delete related resource on software manager server manually.");
                  success = true;
               } else {
                  success = softwareManager.onDeleteCluster(clusterBlueprint, queue);
               }
               break;
            case DESTROY:
               success = softwareManager.deleteCluster(clusterBlueprint, queue);
               break;
            case START:
               boolean forceStart = JobUtils.getJobParameterForceClusterOperation(chunkContext);
               success = softwareManager.startCluster(clusterBlueprint, queue, forceStart);
               break;
            case STOP:
               success = softwareManager.onStopCluster(clusterBlueprint,queue);
               break;
            case START_NODES:
               List<NodeInfo> nodes = new ArrayList<NodeInfo>();
               for (NodeGroupInfo group : clusterBlueprint.getNodeGroups()) {
                  if (group != null) {
                     for (NodeInfo node : group.getNodes()) {
                        if (node.getName().equals(targetName)) {
                           nodes.add(node);
                           break;
                        }
                     }
                     if (!nodes.isEmpty()) {
                        break;
                     }
                  }
               }
               success = softwareManager.startNodes(clusterBlueprint.getName(), nodes, queue);
               break;
            case QUERY:
               ClusterReport report = softwareManager.queryClusterStatus(clusterBlueprint);
               queue.addClusterReport(report);
               success = true;
               break;
            case RESIZE:
               AuAssert.check(chunkContext != null);
               List<String> addedNodes = getResizedVmNames(chunkContext, clusterBlueprint);
               success = softwareManager.scaleOutCluster(clusterBlueprint, addedNodes, queue);
               break;
            default:
               success = true;
         }
      } catch (Throwable t) {
         logger.error(" operation : " + managementOperation.name()
               + " failed on cluster: " + targetName, t);
         result.put("errorMessage", t.getMessage());
      } finally {
         if (progressThread != null) {
               monitor.setStop(true); // tell monitor to stop monitoring then the thread will exit
               progressThread.interrupt(); // wake it up to stop immediately if it's sleeping
               progressThread.join();
            }
         }

      result.put("succeed", success);

      if (!success) {
         logger.error("command execution failed. " + result.get("errorMessage"));
      }

      return result;
   }

   private List<String> getResizedVmNames(ChunkContext chunkContext, 
         ClusterBlueprint clusterBlueprint) {
      String groupName =
            TrackableTasklet.getJobParameters(chunkContext).getString(
                  JobConstants.GROUP_NAME_JOB_PARAM);
      long oldInstanceNum =
            TrackableTasklet.getJobParameters(chunkContext).getLong(
               JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM);

      List<String> addedNodeNames = new ArrayList<String>();
      for (NodeGroupInfo group : clusterBlueprint.getNodeGroups()) {
         if (group.getName().equals(groupName)) {
            for (NodeInfo node: group.getNodes()) {
               long index = CommonUtil.getVmIndex(node.getName());
               if (index < oldInstanceNum) {
                  continue;
               }
               addedNodeNames.add(node.getName());
            }
         }
      }
      return addedNodeNames;
   }
}
