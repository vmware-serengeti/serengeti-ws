/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.dal.IResourcePoolDAO;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.service.IClusterUpdateDataService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class ClusterUpdateDataStep extends TrackableTasklet {
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;

   public IExclusiveLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

   private static final Logger logger = Logger
         .getLogger(ClusterUpdateDataStep.class);

   private INetworkService networkMgr;

   private IResourcePoolDAO rpDao;

   @Autowired
   private IClusterUpdateDataService clusterUpdateDataService;

   public INetworkService getNetworkMgr() {
      return networkMgr;
   }

   @Autowired
   public void setNetworkMgr(INetworkService networkMgr) {
      this.networkMgr = networkMgr;
   }

   /**
    * @return the rpDao
    */
   public IResourcePoolDAO getRpDao() {
      return rpDao;
   }

   /**
    * @param rpDao
    *           the rpDao to set
    */
   @Autowired
   public void setRpDao(IResourcePoolDAO rpDao) {
      this.rpDao = rpDao;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      clusterUpdateDataService.updateAndValidateNodes(chunkContext);

      logger.info("finish update+validate nodes and persist in DB.");
      /*
       * If Tomcat crashes before IPs retrieved when creating cluster, ipconfigs field would
       * be "0.0.0.0", then in resume we should refresh it initiative.
       */
      if (chunkContext.getStepContext().getJobName()
            .equals(JobConstants.RESUME_CLUSTER_JOB_NAME)) {
         lockClusterEntityMgr.syncUp(clusterName, false);
      }
      return RepeatStatus.FINISHED;
   }

}
