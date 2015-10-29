/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.job.vm;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.ShrinkException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.service.sp.DeleteVmByIdSP;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

/**
 * Created by qjin on 11/15/14.
 */
public class DeleteSingleVMStep extends TrackableTasklet {

   private static final Logger logger = Logger
         .getLogger(DeleteSingleVMStep.class);

   private IClusterEntityManager clusterEntityManager;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
                                   JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      String nodeName =
            getJobParameters(chunkContext).getString(
                  JobConstants.SUB_JOB_NODE_NAME);
      String nodeGroupName = getJobParameters(chunkContext).getString(JobConstants.GROUP_NAME_JOB_PARAM);
      NodeEntity nodeEntity = clusterEntityManager.findNodeByName(nodeName);
      if ((nodeEntity != null) && (nodeEntity.getMoId() != null)) {
         String moId = nodeEntity.getMoId();
         DeleteVmByIdSP deleteVmSP = new DeleteVmByIdSP(moId);
         nodeEntity.setAction("Destroying VM");
         clusterEntityManager.update(nodeEntity);
         logger.info("Destroying VM " + nodeName);
         try {
            deleteVmSP.call();
         } catch (Exception e) {
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.NODE_OPERATION_SUCCESS, false);
            throw ShrinkException.DELETE_VM_FAILED(e, clusterName, nodeName);
         }
      }
      NodeGroupEntity nodeGroupEntity = clusterEntityManager.findByName(clusterName, nodeGroupName);
      logger.info("before set defined instance, instanceNum is: " + nodeGroupEntity.getDefineInstanceNum());
      nodeGroupEntity.setDefineInstanceNum(nodeGroupEntity.getDefineInstanceNum() - 1);
      clusterEntityManager.update(nodeGroupEntity);
      clusterEntityManager.delete(nodeEntity);

      logger.info("VM " + nodeName + " has been deleted ? " + (clusterEntityManager.getNodeByVmName(nodeName) == null ? "YES" : "NO" ));
      putIntoJobExecutionContext(chunkContext,
            JobConstants.NODE_OPERATION_SUCCESS, true);
      return RepeatStatus.FINISHED;
   }

   public IClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }
}
