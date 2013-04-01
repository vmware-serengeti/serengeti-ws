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

import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.IClusteringService;

public class ClusterJobExecutionListener extends SimpleJobExecutionListener {
   private static final Logger logger = Logger
         .getLogger(ClusterJobExecutionListener.class);
   private IClusteringService clusteringService;

   private ClusterEntityManager clusterEntityMgr;

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   @Override
   public void beforeJob(JobExecution je) {
      String clusterName =
            getJobParameters(je).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      clusterEntityMgr.updateClusterTaskId(clusterName, je.getId());
      super.beforeJob(je);
   }

   public void afterJob(JobExecution je) {
      releaseResource(je);
      setClusterStatus(je);
      super.afterJob(je);
   }

   private void releaseResource(JobExecution je) {
      UUID reservationId =
            TrackableTasklet.getFromJobExecutionContext(
                  je.getExecutionContext(),
                  JobConstants.CLUSTER_RESOURCE_RESERVATION_ID_JOB_PARAM,
                  UUID.class);
      if (reservationId != null) {
         // release the resource reservation if some step failed, and the resource is not released yet.
         clusteringService.commitReservation(reservationId);
      }
   }

   private void setClusterStatus(JobExecution je) {
      String successStatus =
            getJobParameters(je).getString(
                  JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM);
      String failureStatus =
            getJobParameters(je).getString(
                  JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM);
      String clusterName =
            getJobParameters(je).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      Boolean success =
            TrackableTasklet.getFromJobExecutionContext(
                  je.getExecutionContext(),
                  JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);

      if (success == null || success) {
         success = (je.getExitStatus().equals(ExitStatus.COMPLETED));
      }

      ClusterStatus status = null;
      if (success & successStatus != null) {
         status = ClusterStatus.valueOf(successStatus);
      } else if (!success && failureStatus != null) {
         status = ClusterStatus.valueOf(failureStatus);
      }

      logger.info("set cluster " + clusterName + " status to " + status);
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster != null && status != null) {
         clusterEntityMgr.updateClusterStatus(clusterName, status);
      }
   }

   JobParameters getJobParameters(JobExecution je) {
      return je.getJobInstance().getJobParameters();
   }

}
