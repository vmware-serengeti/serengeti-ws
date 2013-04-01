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

import java.lang.reflect.Type;

import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.vmware.bdd.manager.ClusterEntityManager;

public abstract class TrackableTasklet implements Tasklet {
   static final Logger logger = Logger.getLogger(TrackableTasklet.class);
   JobExecutionStatusHolder jobExecutionStatusHolder;
   
   ClusterEntityManager clusterEntityMgr;
   
   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   final public RepeatStatus execute(StepContribution contribution,
         ChunkContext chunkContext) throws Exception {
      return executeStep(chunkContext, jobExecutionStatusHolder);
   }

   public abstract RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception;

   public static long getJobExecutionId(ChunkContext chunkContext) {
      return chunkContext.getStepContext().getStepExecution().getJobExecution().getId();
   }

   private static ExecutionContext getJobExecutionContext(ChunkContext chunkContext) {
      return chunkContext.getStepContext().getStepExecution().getJobExecution()
            .getExecutionContext();
   }
   public static void putIntoJobExecutionContext(ChunkContext chunkContext, String key,
         Object value) {
      putIntoJobExecutionContext(getJobExecutionContext(chunkContext), key, value);
   }
   public static void putIntoJobExecutionContext(ExecutionContext context, String key,
         Object value) {
      Gson gson = new Gson();
      String valueString = gson.toJson(value);
      context.put(key, valueString);
   }

   public static <T> T getFromJobExecutionContext(ChunkContext chunkContext, String key,
         Class<T> klass) {
      return getFromJobExecutionContext(getJobExecutionContext(chunkContext), key, klass);
   }

   public static <T> T getFromJobExecutionContext(ExecutionContext context, String key,
         Class<T> klass) {
      Object valueString = context.get(key);
      if (valueString != null) {
         if (valueString instanceof String) {
            Gson gson = new Gson();
            return gson.fromJson((String) valueString, klass);
         } else {
            logger.error("invalid data type saved into execution context: "
                  + valueString.getClass() + ", " + valueString);
         }
      }

      return null;
   }

   public static <T> T getFromJobExecutionContext(ChunkContext chunkContext, String key,
         Type typeOfT) {
      return (T)getFromJobExecutionContext(getJobExecutionContext(chunkContext), key, typeOfT);
   }

   public static <T> T getFromJobExecutionContext(ExecutionContext context, String key,
         Type typeOfT) {
      Object valueString = context.get(key);
      if (valueString != null) {
         if (valueString instanceof String) {
            Gson gson = new Gson();
            return (T)gson.fromJson((String) valueString, typeOfT);
         } else {
            logger.error("invalid data type saved into execution context: "
                  + valueString.getClass() + ", " + valueString);
         }
      }

      return null;
   }

   public static JobParameters getJobParameters(ChunkContext chunkContext) {
      return chunkContext.getStepContext().getStepExecution().getJobParameters();
   }

   public JobExecutionStatusHolder getJobExecutionStatusHolder() {
      return jobExecutionStatusHolder;
   }

   public void setJobExecutionStatusHolder(
         JobExecutionStatusHolder jobExecutionStatusHolder) {
      this.jobExecutionStatusHolder = jobExecutionStatusHolder;
   }
}
