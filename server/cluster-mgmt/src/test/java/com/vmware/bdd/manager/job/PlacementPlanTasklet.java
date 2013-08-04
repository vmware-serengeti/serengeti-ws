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

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;

public class PlacementPlanTasklet extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(PlacementPlanTasklet.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      Map<String, String> hostToRackMap = new TreeMap<String, String>();
      hostToRackMap.put("host1", "rack1");
      hostToRackMap.put("host2", "rack1");
      hostToRackMap.put("host3", "rack1");

      ClusterCreate cluster = new ClusterCreate();
      cluster.setHostToRackMap(hostToRackMap);

      logger.info("generating placement plan: "
            + getJobParameters(chunkContext).getString("cluster.name"));
      putIntoJobExecutionContext(chunkContext, "some-variable-which-need-be-saved", 10);
      putIntoJobExecutionContext(chunkContext, "clusterCreate", cluster);
      jobExecutionStatusHolder.setCurrentStepProgress(getJobExecutionId(chunkContext),
            0.8);

      Thread.sleep(500);
      return RepeatStatus.FINISHED;
   }
}