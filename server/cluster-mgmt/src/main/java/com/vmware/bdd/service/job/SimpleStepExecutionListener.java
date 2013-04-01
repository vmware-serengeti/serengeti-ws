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

import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.item.ExecutionContext;

import com.vmware.bdd.exception.BddException;

public class SimpleStepExecutionListener implements StepExecutionListener {
   static final Logger logger = Logger.getLogger(SimpleStepExecutionListener.class);
   JobRegistry jobRegistry;
   JobExecutionStatusHolder jobExecutionStatusHolder;

   @Override
   public ExitStatus afterStep(StepExecution se) {
      logger.info("step finished: " + se.getStepName());
      ExecutionContext jec = se.getJobExecution().getExecutionContext();
      if (se.getStatus().equals(BatchStatus.COMPLETED)) {
         jec.put(se.getStepName() + ".COMPLETED", true);
         jobExecutionStatusHolder.setCurrentStepProgress(se.getJobExecution().getId(), 1);
      } else {
         for (Throwable t : se.getFailureExceptions()) {
            String msg = t.getMessage();
            if (msg != null && !msg.isEmpty()) {
               TrackableTasklet.putIntoJobExecutionContext(jec,
                     JobConstants.CURRENT_ERROR_MESSAGE, msg);
               break;
            }
         }
      }

      return se.getExitStatus();
   }

   @Override
   public void beforeStep(StepExecution se) {
      logger.info("step started: " + se.getStepName());
      JobExecution je = se.getJobExecution();
      AbstractJob job = null;
      try {
         job = (AbstractJob) jobRegistry.getJob(je.getJobInstance().getJobName());
      } catch (NoSuchJobException ex) {
         throw BddException.INTERNAL(ex, "illegal state");
      }

      Collection<String> stepNames = job.getStepNames();

      int steps = stepNames.size();
      double done = 0;
      for (String stepName : stepNames) {
         ExecutionContext jec = se.getJobExecution().getExecutionContext();
         Boolean completed = (Boolean) jec.get(stepName + ".COMPLETED");

         if (completed != null && completed) {
            ++done;
         }
      }
      double progress = done/steps;
      jobExecutionStatusHolder.setCurrentStepWeight(je.getId(), 1.0/steps);
      jobExecutionStatusHolder.setCurrentProgressBeforeStepStart(je.getId(), progress);
   }

   public JobExecutionStatusHolder getJobExecutionStatusHolder() {
      return jobExecutionStatusHolder;
   }

   public void setJobExecutionStatusHolder(
         JobExecutionStatusHolder jobExecutionStatusHolder) {
      this.jobExecutionStatusHolder = jobExecutionStatusHolder;
   }

   public JobRegistry getJobRegistry() {
      return jobRegistry;
   }

   public void setJobRegistry(JobRegistry jobRegistry) {
      this.jobRegistry = jobRegistry;
   }
}
