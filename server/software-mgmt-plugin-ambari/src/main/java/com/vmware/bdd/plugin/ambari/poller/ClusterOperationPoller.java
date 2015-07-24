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
package com.vmware.bdd.plugin.ambari.poller;

import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTask;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTaskInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ClusterRequestStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.TaskStatus;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

public class ClusterOperationPoller extends StatusPoller {

   private static final Logger logger = Logger
         .getLogger(ClusterOperationPoller.class);

   private ApiManager apiManager;
   private ApiRequest apiRequestSummary;
   private String clusterName;
   private ClusterReport currentReport;
   private ClusterReportQueue reportQueue;
   private int beginProgress;
   private int endProgress;

   public ClusterOperationPoller(final ApiManager apiManager,
         final ApiRequest apiRequestSummary, final String clusterName,
         final ClusterReport currentReport,
         final ClusterReportQueue reportQueue, int endProgress) {
      this.apiManager = apiManager;
      this.apiRequestSummary = apiRequestSummary;
      this.clusterName = clusterName;
      this.currentReport = currentReport;
      this.reportQueue = reportQueue;
      this.endProgress = endProgress;
      this.beginProgress = currentReport.getProgress();
   }

   @Override
   public boolean poll() {
      if (apiRequestSummary == null) {
         return true;
      }
      Long requestId = apiRequestSummary.getApiRequestInfo().getRequestId();
      ApiRequest apiRequest = apiManager.getRequestWithTasks(clusterName, requestId);

      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());

      Map<String, NodeReport> nodeReports = currentReport.getNodeReports();
      for (String nodeReportKey : nodeReports.keySet()) {
         for (ApiTask apiTask : apiRequest.getApiTasks()) {
            NodeReport nodeReport = nodeReports.get(nodeReportKey);
            nodeReport.setUseClusterMsg(false);
            ApiTaskInfo apiTaskInfo = apiTask.getApiTaskInfo();
            if (nodeReport.getHostname() != null && nodeReport.getHostname().equals(apiTaskInfo.getHostName())) {
               TaskStatus taskStatus =
                     TaskStatus.valueOf(apiTask.getApiTaskInfo().getStatus());
               if (taskStatus.isRunningState()) {
                  if (clusterRequestStatus.isFailedState() &&
                        apiTaskInfo.getStderr() != null &&
                        !apiTaskInfo.getStderr().isEmpty()) {
                     nodeReport.setAction(apiTaskInfo.getCommandDetail() + ": "
                           + apiTaskInfo.getStderr());
                  } else {
                     nodeReport.setAction(apiTaskInfo.getCommandDetail());
                  }
                  nodeReports.put(nodeReportKey, nodeReport);
               }
            }
         }
      }
      currentReport.setNodeReports(nodeReports);

      int provisionPercent =
            (int) apiRequest.getApiRequestInfo().getProgressPercent();
      if (provisionPercent != 0) {
         int currentProgress = currentReport.getProgress();
         int toProgress = beginProgress + provisionPercent / 2;
         if (toProgress >= endProgress) {
            toProgress = endProgress;
         }
         boolean isCompletedState = clusterRequestStatus.isCompletedState();
         if ((toProgress != currentProgress) && (provisionPercent % 10 == 0)
               || isCompletedState) {
            if (isCompletedState) {
               logger.info("Cluster request " + requestId + " is completed.");
            } else {
               logger.info("Waiting for cluster request " + requestId
                     + " to complete.");
            }
            currentReport.setProgress(toProgress);
            if (reportQueue != null) {
               reportQueue.addClusterReport(currentReport.clone());
            }
         }
      }

      if (clusterRequestStatus.isCompletedState()) {
         return true;
      }

      return false;
   }

}
