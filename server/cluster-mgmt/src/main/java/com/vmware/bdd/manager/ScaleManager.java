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
package com.vmware.bdd.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.ResourceScale;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.service.job.JobConstants;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class ScaleManager {
   private static final Logger logger = Logger.getLogger(ScaleManager.class);

   private ClusterEntityManager clusterEntityMgr;
   private JobManager jobManager;


   public long scaleNodeGroupResource(ResourceScale scale) throws Exception {
      String clusterName = scale.getClusterName();
      List<JobParameters> jobParametersList = buildJobParameters(scale);
      //launch sub job to scale node one by one
      try {
         return jobManager.runSubJobForNodes(JobConstants.NODE_SCALE_JOB_NAME,
               jobParametersList, clusterName);
      } catch (Exception e) {
         logger.error("Failed to start cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.ERROR);
         throw e;
      }
   }

   /**
    * @param scale
    * @return
    */
   @Transactional
   public List<JobParameters> buildJobParameters(ResourceScale scale) {
      String clusterName = scale.getClusterName();
      String nodeGroupName = scale.getNodeGroupName();
      List<NodeEntity> nodes =
            clusterEntityMgr.findAllNodes(clusterName, nodeGroupName);
      List<JobParameters> jobParametersList = new ArrayList<JobParameters>();
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      for (NodeEntity nodeEntity : nodes) {
         if ((nodeEntity.getCpuNum() != scale.getCpuNumber() && scale.getCpuNumber() > 0)
               || (nodeEntity.getMemorySize() != scale.getMemory() && scale.getMemory() > 0)) {
            logger.info("original cpu number :" + nodeEntity.getCpuNum()
                  + ". Expected cpu number: " + scale.getCpuNumber());
            String nodeName = nodeEntity.getVmName();
            JobParameters nodeParameters =
                  parametersBuilder
                        .addString(JobConstants.SUB_JOB_NODE_NAME, nodeName)
                        .addString(JobConstants.TARGET_NAME_JOB_PARAM, nodeName)
                        .addString(JobConstants.CLUSTER_NAME_JOB_PARAM,
                              clusterName)
                        .addString(JobConstants.NODE_SCALE_CPU_NUMBER,
                              String.valueOf(scale.getCpuNumber()))
                        .addString(JobConstants.NODE_SCALE_MEMORY_SIZE,
                              String.valueOf(scale.getMemory()))
                        .toJobParameters();
            jobParametersList.add(nodeParameters);
         } else {
            logger.info("This node does need to be scaled. "
                  + nodeEntity.getVmName());
            continue;
         }
      }
      return jobParametersList;
   }

   /**
    * @return the clusterEntityMgr
    */
   public ClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   /**
    * @param clusterEntityMgr
    *           the clusterEntityMgr to set
    */
   public void setClusterEntityMgr(ClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   /**
    * @return the jobManager
    */
   public JobManager getJobManager() {
      return jobManager;
   }

   /**
    * @param jobManager
    *           the jobManager to set
    */
   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }


}
