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

import org.apache.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;

import com.vmware.bdd.exception.BddException;

public class SimpleJobExecutionListener implements JobExecutionListener {
   private static final Logger logger = Logger
         .getLogger(SimpleJobExecutionListener.class);
   private JobExecutionStatusHolder jobExecutionStatusHolder;
   private JobRegistry jobRegistry;

   public JobRegistry getJobRegistry() {
      return jobRegistry;
   }

   public void setJobRegistry(JobRegistry jobRegistry) {
      this.jobRegistry = jobRegistry;
   }

   @Override
   public void afterJob(JobExecution je) {
      jobExecutionStatusHolder.unregisterJobExecution(je.getId());
      unRegisterJob(je);
   }

   @Override
   public void beforeJob(JobExecution je) {
      jobExecutionStatusHolder.registerJobExecution(je.getId());
   }

   public JobExecutionStatusHolder getJobExecutionStatusHolder() {
      return jobExecutionStatusHolder;
   }

   public void setJobExecutionStatusHolder(
         JobExecutionStatusHolder jobExecutionStatusHolder) {
      this.jobExecutionStatusHolder = jobExecutionStatusHolder;
   }

   private void unRegisterJob(JobExecution je) {
      String jobName = je.getJobInstance().getJobName();
      JobParameters jobParameters = je.getJobInstance().getJobParameters();
      long subJobEnabled = jobParameters.getLong(JobConstants.SUB_JOB_ENABLED);
      // sub job launcher
      if (subJobEnabled == 1) {
         if (jobRegistry != null) {
            try {
               Job job = jobRegistry.getJob(jobName);
               if (job != null) {
                  jobRegistry.unregister(jobName);
                  logger.info("unregistered sub job launcher. " + jobName);
               }
            } catch (NoSuchJobException e) {
               throw BddException.INTERNAL(e,
                     "No spring batch job is registered. " + jobName);
            }
         }
      }
   }
}
