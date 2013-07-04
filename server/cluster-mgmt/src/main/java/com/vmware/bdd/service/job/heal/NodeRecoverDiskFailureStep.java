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
package com.vmware.bdd.service.job.heal;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.service.IClusterHealService;
import com.vmware.bdd.service.job.ClusterUpdateDataStep;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.spectypes.DiskSpec;
import com.vmware.bdd.utils.AuAssert;

public class NodeRecoverDiskFailureStep extends TrackableTasklet {

   private static final Logger logger = Logger
         .getLogger(ClusterUpdateDataStep.class);

   @Autowired
   private IClusterHealService healService;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      String groupName =
            getJobParameters(chunkContext).getString(
                  JobConstants.GROUP_NAME_JOB_PARAM);
      String targetNode =
            getJobParameters(chunkContext).getString(
                  JobConstants.SUB_JOB_NODE_NAME);
      // find bad disks
      List<DiskSpec> badDisks = healService.getBadDisks(targetNode);
      AuAssert.check(!badDisks.isEmpty());

      // find replacements for bad disks
      logger.debug("get replacements for bad disks");
      List<DiskSpec> replacements;
      try {
         replacements =
               healService.getReplacementDisks(clusterName, groupName,
                     targetNode, badDisks);
         AuAssert.check(badDisks.size() == replacements.size());
      } catch (Exception e) {
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CURRENT_ERROR_MESSAGE, e.getMessage());
         throw e;
      }

      logger.debug("get replacement disk set for recovery "
            + replacements.toString());
      jobExecutionStatusHolder.setCurrentStepProgress(
            getJobExecutionId(chunkContext), 0.3);

      // clone and recover
      logger.debug("start recovering bad VMs");
      try {
         VcVirtualMachine newVm =
               healService.createReplacementVm(clusterName, groupName,
                     targetNode, replacements);
         if (newVm != null) { 
            logger.info("created replacement vm " + newVm.getId());
            putIntoJobExecutionContext(chunkContext, JobConstants.REPLACE_VM_ID,
               newVm.getId());
         } else {
            logger.error("failed creating replacement vm " + targetNode);
         }
      } catch (Exception e) {
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CURRENT_ERROR_MESSAGE, e.getMessage());
         throw e;
      }

      jobExecutionStatusHolder.setCurrentStepProgress(
            getJobExecutionId(chunkContext), 1.0);
      return RepeatStatus.FINISHED;
   }
}
