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
package com.vmware.bdd.manager.job;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class ProvisionTasklet extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(ProvisionTasklet.class);

   private void randomlyFail(double p) throws Exception {
      if (RandomUtils.nextDouble() > 1 - p) {
         throw BddException.INTERNAL(new Exception(),
               "Undefined random failure.");
      }
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      getFromJobExecutionContext(chunkContext, "clusterCreate", ClusterCreate.class);

      Thread.sleep(500);

      logger.info("provision cluster vms: "
            + getJobParameters(chunkContext).getString("cluster.name"));
      Integer processed = getFromJobExecutionContext(chunkContext, "processed.count", Integer.class); 

      for (int i = processed == null ? 0 : processed; i < 100; ++i) {
         logger.info("provision vm #" + i);
         putIntoJobExecutionContext(chunkContext, "processed.count", i); 
         jobExecutionStatusHolder.setCurrentStepProgress(
               getJobExecutionId(chunkContext), i / 100.0);
         Thread.sleep(10);
         randomlyFail(0.02);
      }

      return RepeatStatus.FINISHED;
   }
}