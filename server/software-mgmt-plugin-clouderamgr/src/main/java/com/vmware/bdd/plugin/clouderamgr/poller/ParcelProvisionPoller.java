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
package com.vmware.bdd.plugin.clouderamgr.poller;

import com.cloudera.api.model.ApiParcelState;
import com.cloudera.api.v3.ParcelResource;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableParcelStage;
import com.vmware.bdd.plugin.clouderamgr.service.ClouderaManagerImpl;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

/**
 * Author: Xiaoding Bian
 * Date: 7/1/14
 * Time: 10:24 AM
 */
public class ParcelProvisionPoller extends StatusPoller {

   private ParcelResource apiParcelResource;
   private AvailableParcelStage toStage;
   private AvailableParcelStage formerStage;
   private boolean enteredFormerStage;
   private ClusterReport currentReport;
   private ClusterReportQueue reportQueue;
   private int beginProgress;
   private int endProgress;

   public ParcelProvisionPoller(ParcelResource parcelResource, AvailableParcelStage toStage,
         final ClusterReport currentReport,
         final ClusterReportQueue reportQueue, int endProgress) {
      this.apiParcelResource = parcelResource;
      this.toStage = toStage;
      this.currentReport = currentReport;
      this.reportQueue = reportQueue;
      this.endProgress = endProgress;
      this.beginProgress = currentReport.getProgress();
      this.formerStage = AvailableParcelStage.values()[toStage.ordinal() - 1];
      this.enteredFormerStage = false;
   }

   @Override
   public boolean poll() {

      /*
      The ApiCommand instance for parcel operations are not exposed by Cloudera Manager, if user cancelled the
      operation from UI, we have no way to be aware of this. Here, we check the parcel status for judge if the
      operation is success or failed.
       */
      String stage = apiParcelResource.readParcel().getStage();
      if (stage.equals(toStage.toString())) {
         return true;
      }
      enteredFormerStage |= stage.equals(formerStage.toString());
      // to avoid hang if cancelled by another session
      if (enteredFormerStage && !stage.equals(toStage.toString()) && !stage.equals(formerStage.toString())) {
         return true;
      }
      ApiParcelState parcelState = apiParcelResource.readParcel().getState();
      if (parcelState.getTotalProgress() != 0) {
         float percent = ((float)parcelState.getProgress() / parcelState.getTotalProgress());
         int currentProgress = currentReport.getProgress();
         int toProgress = (int) (beginProgress + (endProgress - beginProgress) * percent);
         // sometimes parcelState.getProgress() > parcel.getTotalProgress()
         if (toProgress > endProgress) {
            toProgress = endProgress;
         }
         if (toProgress > currentProgress) {
            currentReport.setProgress(toProgress);
            reportQueue.addClusterReport(currentReport.clone());
         }
      }
      return false;
   }
}
