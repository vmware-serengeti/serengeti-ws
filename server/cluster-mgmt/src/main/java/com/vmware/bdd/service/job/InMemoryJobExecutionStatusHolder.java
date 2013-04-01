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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

class StatusMap extends LinkedHashMap<Long, Map<String, Object>> 
{
   private static final long serialVersionUID = 1L;
   private static final int MAX_JOB_EXECUTION_COUNT = 1000;

   protected boolean removeEldestEntry(Map.Entry<Long, Map<String, Object>> eldest) {
      Map<String, Object> data = eldest.getValue();
      boolean unregistered = (Boolean) data
            .get(InMemoryJobExecutionStatusHolder.JOB_UNREGISTERED);
      return unregistered && size() > MAX_JOB_EXECUTION_COUNT;
   }
}
public class InMemoryJobExecutionStatusHolder implements JobExecutionStatusHolder {
   private static final Logger logger = Logger
         .getLogger(InMemoryJobExecutionStatusHolder.class);
   private Map<Long, Map<String, Object>> statusHolder;

   protected final static String JOB_UNREGISTERED = "job.unregistered";
   private final static String JOB_PROGRESS = "job.progress";
   private final static String BASE_STEP_PROGRESS = "base.step.progress";
   private final static String STEP_WEIGHT = "step.weight";

   public InMemoryJobExecutionStatusHolder() {
      statusHolder = Collections.synchronizedMap(new LinkedHashMap<Long, Map<String, Object>>());
   }

   public void registerJobExecution(long jobExecutionId) {
      logger.info("registering job execution: " + jobExecutionId);
      Map<String, Object> data = new ConcurrentHashMap<String, Object>();
      data.put(JOB_UNREGISTERED, false);
      statusHolder.put(jobExecutionId, data);
   }

   public void unregisterJobExecution(long jobExecutionId) {
      logger.info("unregistering job execution: " + jobExecutionId);
      Map<String, Object> data = new ConcurrentHashMap<String, Object>();
      data.put(JOB_UNREGISTERED, true);
   }

   @Override
   public void setCurrentStepWeight(long jobExecutionId, double weight) {
      Map<String, Object> data = statusHolder.get(jobExecutionId);
      data.put(STEP_WEIGHT, weight);
   }

   @Override
   public void setCurrentStepProgress(long jobExecutionId, double progress) {
      Map<String, Object> data = statusHolder.get(jobExecutionId);
      double weight = (Double) data.get(STEP_WEIGHT);
      double baseProgress = (Double) data.get(BASE_STEP_PROGRESS);

      double jobProgress = baseProgress + weight * progress;
      data.put(JOB_PROGRESS, jobProgress);
   }

   @Override
   public void setCurrentProgressBeforeStepStart(long jobExecutionId, double progress) {
      Map<String, Object> data = statusHolder.get(jobExecutionId);
      data.put(BASE_STEP_PROGRESS, progress);

      setCurrentStepProgress(jobExecutionId, 0);
   }

   @Override
   public double getCurrentProgress(long jobExecutionId) {
      Map<String, Object> data = statusHolder.get(jobExecutionId);
      if (data == null) {
         logger.info("no status found for job execution: " + jobExecutionId);
         return 0.0;
      }

      Double progress = (Double) data.get(JOB_PROGRESS);
      if (progress == null) {
         logger.info("no progress found for job execution: " + jobExecutionId);
         return 0.0;
      }

      return progress;
   }
}
