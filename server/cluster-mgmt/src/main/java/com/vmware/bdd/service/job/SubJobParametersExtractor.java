/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.job.JobParametersExtractor;

import com.vmware.bdd.utils.JobUtils;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class SubJobParametersExtractor implements JobParametersExtractor {

   /* (non-Javadoc)
    * @see org.springframework.batch.core.step.job.JobParametersExtractor#getJobParameters(org.springframework.batch.core.Job, org.springframework.batch.core.StepExecution)
    */
   @Override
   public JobParameters getJobParameters(Job job, StepExecution stepExecution) {
      String stepName = stepExecution.getStepName();
      int index = stepName.lastIndexOf("-");
      int stepNumber = Integer.parseInt(stepName.substring(index + 1));
      JobParameters jobParameters = stepExecution.getJobParameters();
      Map<String, JobParameter> jobParametersMap =
            jobParameters.getParameters();
      Map<String, JobParameter> subJobParameters =
            new TreeMap<String, JobParameter>();
      for (String key : jobParametersMap.keySet()) {
         subJobParameters.put(key, jobParametersMap.get(key));
      }
      if (jobParametersMap
            .containsKey(JobConstants.SUB_JOB_PARAMETERS_NUMBER
                  + stepNumber)) {
         int jobParametersNumber =
               (int) jobParameters
                     .getLong(JobConstants.SUB_JOB_PARAMETERS_NUMBER
                           + stepNumber);
         for (int count = 0; count < jobParametersNumber; count++) {
            String key =
                  jobParameters.getString(JobUtils.getSubJobParameterPrefixKey(
                        stepNumber, count));
            JobParameter parameterValue =
                  jobParametersMap.get(JobUtils.getSubJobParameterPrefixValue(
                        stepNumber, count));
            subJobParameters.put(key, parameterValue);
         }
      }
      subJobParameters.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(
            new Date()));
      return new JobParameters(subJobParameters);
   }

}
