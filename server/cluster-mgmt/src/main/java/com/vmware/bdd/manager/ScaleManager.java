/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.exception.VcProviderException;

import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.ResourceScale;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ScaleServiceException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.utils.ValidationUtils;
import com.vmware.bdd.utils.VcVmUtil;

/**
 * @author Jarred Li
 * @version 0.9
 * @since 0.9
 * 
 */
public class ScaleManager {
   private static final Logger logger = Logger.getLogger(ScaleManager.class);

   private IClusterEntityManager clusterEntityMgr;
   private JobManager jobManager;

   public long scaleNodeGroupResource(ResourceScale scale) throws Exception {
      String clusterName = scale.getClusterName();
      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);
      ClusterStatus originalStatus =
            clusterEntityMgr.findByName(clusterName).getStatus();
      logger.info("before scaling, cluster status is:" + originalStatus);
      if (scale.getMemory() > 0) {
         //VM's memory must be divisible by 4, otherwise VM can not be started
         long converted = VcVmUtil.makeVmMemoryDivisibleBy4(scale.getMemory());
         logger.info("user's setting for memory: " + scale.getMemory()
               + " converted to : " + converted);
         scale.setMemory(converted);
      }
      List<JobParameters> jobParametersList = buildJobParameters(scale);
      if (jobParametersList.size() == 0) {
         throw ScaleServiceException.NOT_NEEDED(clusterName);
      }
      String nodeGroupName = scale.getNodeGroupName();
      List<NodeEntity> nodes =
            clusterEntityMgr.findAllNodes(clusterName, nodeGroupName);
      if (nodes.size() > 0) {
         // vm max configuration check
         VcResourceUtils.checkVmMaxConfiguration(nodes.get(0).getMoId(),
               scale.getCpuNumber(), scale.getMemory());
         if (scale.getCpuNumber() > 1) {
            //cpu number check for vm with FT enabled
            for (NodeEntity nodeEntity : nodes) {
               VcResourceUtils.checkVmFTAndCpuNumber(nodeEntity.getMoId(), nodeEntity.getVmName(),
                     scale.getCpuNumber());
               if (!VcVmUtil.validateCPU(nodeEntity.getMoId(), scale.getCpuNumber())) {
                  throw VcProviderException.CPU_NUM_NOT_MULTIPLE_OF_CORES_PER_SOCKET(scale.getNodeGroupName(),
                        nodeEntity.getVmName());
               }
            }
         }
      }
      updateNodeGroupResource(scale);
      //launch sub job to scale node one by one
      try {
         logger.info("set cluster to maintenace");
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.MAINTENANCE);
         logger.info("current cluster status: "
               + clusterEntityMgr.findByName(clusterName).getStatus());
         return jobManager.runSubJobForNodes(JobConstants.NODE_SCALE_JOB_NAME,
               jobParametersList, clusterName, originalStatus,
               ClusterStatus.ERROR);
      } catch (Throwable t) {
         logger.error("Failed to start cluster " + clusterName, t);
         clusterEntityMgr.updateClusterStatus(clusterName, originalStatus);
         throw ScaleServiceException.JOB_LAUNCH_FAILURE(clusterName, t,
               t.getMessage());
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
         if (nodeEntity.isObsoleteNode()) {
            logger.info("Ingore node "
                  + nodeEntity.getVmName()
                  + ", for it violate VM name convention " 
                  + "or exceed defined group instance number. ");
            continue;
         }

         if ((nodeEntity.getCpuNum() != scale.getCpuNumber() && scale
               .getCpuNumber() > 0)
               || (nodeEntity.getMemorySize() != scale.getMemory() && scale
                     .getMemory() > 0)) {
            logger.info("original cpu number :" + nodeEntity.getCpuNum()
                  + ". Expected cpu number: " + scale.getCpuNumber());
            String nodeName = nodeEntity.getVmName();
            boolean vmPowerOn =
                  (nodeEntity.getStatus().ordinal() != NodeStatus.POWERED_OFF
                        .ordinal());
            logger.debug("orginal vm power on? " + vmPowerOn);
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
                        .addString(JobConstants.IS_VM_POWER_ON,
                              String.valueOf(vmPowerOn)).toJobParameters();
            jobParametersList.add(nodeParameters);
         } else {
            logger.info("This node does need to be scaled. "
                  + nodeEntity.getVmName());
            continue;
         }
      }
      return jobParametersList;
   }

   public void updateNodeGroupResource(ResourceScale scale) {
      NodeGroupEntity nodeGroup =
            clusterEntityMgr.findByName(scale.getClusterName(),
                  scale.getNodeGroupName());
      if (scale.getCpuNumber() > 0) {
         nodeGroup.setCpuNum(scale.getCpuNumber());
      }
      if (scale.getMemory() > 0) {
         nodeGroup.setMemorySize((int) scale.getMemory());
      }
      clusterEntityMgr.update(nodeGroup);
   }

   /**
    * @return the clusterEntityMgr
    */
   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   /**
    * @param clusterEntityMgr
    *           the clusterEntityMgr to set
    */
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
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
