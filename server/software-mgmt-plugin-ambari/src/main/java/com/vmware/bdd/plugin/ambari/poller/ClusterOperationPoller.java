/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTask;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTaskInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ClusterRequestStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.TaskStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequest;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

public class ClusterOperationPoller extends StatusPoller {

   private static final Logger logger = Logger
         .getLogger(ClusterOperationPoller.class);

   private final ApiManager apiManager;
   private final List<ApiRequest> apiRequestsSummary;
   private final String clusterName;
   private final ClusterReport currentReport;
   private final ClusterReportQueue reportQueue;
   private final int beginProgress;
   private final int endProgress;

   public ClusterOperationPoller(final ApiManager apiManager,
         final ApiRequest apiRequestSummary, final String clusterName,
         final ClusterReport currentReport,
         final ClusterReportQueue reportQueue, int endProgress) {
      this(apiManager, new ArrayList<ApiRequest>(Arrays.asList(apiRequestSummary)), clusterName, currentReport, reportQueue, endProgress);
   }

   public ClusterOperationPoller(final ApiManager apiManager,
         final List<ApiRequest> apiRequestsSummary, final String clusterName,
         final ClusterReport currentReport,
         final ClusterReportQueue reportQueue, int endProgress) {
      this.apiManager = apiManager;
      this.apiRequestsSummary = apiRequestsSummary;
      this.clusterName = clusterName;
      this.currentReport = currentReport;
      this.reportQueue = reportQueue;
      this.endProgress = endProgress;
      this.beginProgress = currentReport.getProgress();
   }

   @Override
   public boolean poll() {
      if (apiRequestsSummary == null || apiRequestsSummary.isEmpty()) {
         return true;
      }

      boolean isCompleted = true;

      for (ApiRequest apiRequestSummary : apiRequestsSummary) {
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

         boolean isCompletedState = clusterRequestStatus.isCompletedState();

         // Fix bug the request_status is COMPLETED when running cluster creation REST API on Ambari server >= 2.2
         if (apiRequest.getApiRequestInfo().getTaskCount() == 0 && isCompletedState) {
            isCompleted = false;
            break;
         }

         int provisionPercent =
               (int) apiRequest.getApiRequestInfo().getProgressPercent();
         if (provisionPercent != 0) {
            int currentProgress = currentReport.getProgress();
            int toProgress = beginProgress + provisionPercent / 2;
            if (toProgress >= endProgress) {
               toProgress = endProgress;
            }
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


         if (!isCompletedState) {
            isCompleted = false;
            break;
         }
      }

      return isCompleted;
   }

}
