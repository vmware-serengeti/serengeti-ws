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
package com.vmware.bdd.service.job.vm;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeStatus;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */

public class StopSingleVMStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(StopSingleVMStep.class);

   private IClusteringService clusteringService;
   private boolean vmPoweroff = false;
   private boolean checkVMStatus = false;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      if (clusterName == null) {
         clusterName =
               getJobParameters(chunkContext).getString(
                     JobConstants.TARGET_NAME_JOB_PARAM).split("-")[0];
      }
      String nodeName =
            getJobParameters(chunkContext).getString(
                  JobConstants.SUB_JOB_NODE_NAME);
      String vmPowerOnStr =
            getJobParameters(chunkContext).getString(
                  JobConstants.IS_VM_POWER_ON);
      logger.debug("nodename: " + nodeName + "vm original status is power on? "
            + vmPowerOnStr);
      boolean vmPowerOn = Boolean.parseBoolean(vmPowerOnStr);
      if (!checkVMStatus || (checkVMStatus && !vmPowerOn)) {
         logger.debug("check vm status: " + checkVMStatus
               + ", vm original status is poweron? " + vmPowerOn);
         StatusUpdater statusUpdator =
               new DefaultStatusUpdater(jobExecutionStatusHolder,
                     getJobExecutionId(chunkContext));
         boolean success =
               clusteringService.stopSingleVM(clusterName, nodeName,
                     statusUpdator, vmPoweroff);

         putIntoJobExecutionContext(chunkContext,
               JobConstants.NODE_OPERATION_SUCCESS, success);
         putIntoJobExecutionContext(chunkContext,
               JobConstants.EXPECTED_NODE_STATUS, NodeStatus.POWERED_OFF);
         if (!success) {
            throw VcProviderException.STOP_VM_ERROR(nodeName);
         }
      }
      return RepeatStatus.FINISHED;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   /**
    * @return the vmPoweroff
    */
   public boolean isVmPoweroff() {
      return vmPoweroff;
   }

   /**
    * @param vmPoweroff
    *           the vmPoweroff to set
    */
   public void setVmPoweroff(boolean vmPoweroff) {
      this.vmPoweroff = vmPoweroff;
   }

   /**
    * @return the checkVMStatus
    */
   public boolean isCheckVMStatus() {
      return checkVMStatus;
   }

   /**
    * @param checkVMStatus
    *           the checkVMStatus to set
    */
   public void setCheckVMStatus(boolean checkVMStatus) {
      this.checkVMStatus = checkVMStatus;
   }


}
