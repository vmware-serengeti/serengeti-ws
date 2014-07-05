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

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.ClusterRequestStatus;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

public class ClusterOperationPoller extends StatusPoller {

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
      ApiRequest apiRequest =
            apiManager.request(clusterName, apiRequestSummary
                  .getApiRequestInfo().getRequestId());

      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());

      if (clusterRequestStatus.isCompletedState()) {
         return true;
      }

      int provisionPercent =
            (int) apiRequest.getApiRequestInfo().getProgressPercent();
      if (provisionPercent != 0) {
         int currentProgress = currentReport.getProgress();
         int toProgress = beginProgress + provisionPercent / 2;
         if (toProgress >= endProgress) {
            toProgress = endProgress;
         }
         if (toProgress != currentProgress) {
            currentReport.setProgress(toProgress);
            reportQueue.addClusterReport(currentReport.clone());
         }
      }

      return false;
   }

}