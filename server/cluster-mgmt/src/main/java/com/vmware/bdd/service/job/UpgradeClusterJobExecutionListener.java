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
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;

public class UpgradeClusterJobExecutionListener extends ClusterJobExecutionListener {
   private static final Logger logger = Logger.getLogger(UpgradeClusterJobExecutionListener.class);

   @Transactional
   public void afterJob(JobExecution je) {
      super.afterJob(je);
      Boolean success =
            TrackableTasklet.getFromJobExecutionContext(
                  je.getExecutionContext(),
                  JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);
      final String clusterName = getJobParameters(je).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      if (success == null || success) {
         success = (je.getExitStatus().equals(ExitStatus.COMPLETED));
      }
      if (success) {
         IClusterEntityManager clusterEntityMgr = getClusterEntityMgr();
         ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

         cluster.setVersion(clusterEntityMgr.getServerVersion());
         cluster.setStatus(cluster.getLastStatus());
         cluster.setLastStatus(null);

         logger.info("cluster " + clusterName + "upgrade job succeeds");
      }
   }
}
