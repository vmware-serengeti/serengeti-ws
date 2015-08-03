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

import com.vmware.aurora.util.AuAssert;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;

import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeGroupEntity;

import java.util.ArrayList;
import java.util.List;

public class ResizeClusterJobExecutionListener extends
      ClusterJobExecutionListener {
   private static final Logger logger = Logger
         .getLogger(ResizeClusterJobExecutionListener.class);

   public void afterJob(JobExecution je) {
      super.afterJob(je);
      Boolean success =
            TrackableTasklet.getFromJobExecutionContext(
                  je.getExecutionContext(),
                  JobConstants.CLUSTER_OPERATION_SUCCESS, Boolean.class);
      final String clusterName =
            getJobParameters(je).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      final String groupName =
            getJobParameters(je).getString(JobConstants.GROUP_NAME_JOB_PARAM);
      if (success == null || success) {
         success = (je.getExitStatus().equals(ExitStatus.COMPLETED));
      }

      final Long oldInstanceNum = getJobParameters(je).getLong(
                  JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM, 0);
      if (!success || hasNotReadyNodes(je)) {
          // TODO: need to discuss whether to check the software provision status
          // || hasSoftwareBootstapFailedNodes(clusterName, groupName, oldInstanceNum)) {
         logger.warn("resize cluster failed, revert to the original defined instance number " + oldInstanceNum.intValue());
         updateDefinedInstanceNum(clusterName, groupName, oldInstanceNum);
      }
   }

   //verify if there is not vm ready nodes by 2 steps, first step is to verify vm creation
   //second step is verify vm status == VM_READY. Those two parameters are put into job context in
   //CreateClusterVMStep and ClusterUpdateDataStep. As the vm status depends on vm creation, so you have
   //to verify VMs' creation, then verify VMs status
   private boolean hasNotReadyNodes(JobExecution je) {
      boolean createVMSuccess = TrackableTasklet.getFromJobExecutionContext(
            je.getExecutionContext(),
            JobConstants.CLUSTER_CREATE_VM_OPERATION_SUCCESS, Boolean.class);
      if (!createVMSuccess) {
         logger.info("Some VMs are not created successfully in cluster scale out");
         return true;
      }
      boolean allNewVMsAreVMReady = TrackableTasklet.getFromJobExecutionContext(
            je.getExecutionContext(),
            JobConstants.VERIFY_NODE_STATUS_RESULT_PARAM, Boolean.class);
      if (!allNewVMsAreVMReady) {
         logger.info("Some VMs are not VM_READY in cluster scale out");
         return true;
      }
      return false;
   }

   private boolean hasSoftwareBootstapFailedNodes(String cluster, String nodegroup, Long oldInstanceNum) {
      List<NodeEntity> nodes = getClusterEntityMgr().findAllNodes(cluster, nodegroup);
      ArrayList<String> serviceFailedNodeNames = new ArrayList<>();
      for (NodeEntity node: nodes) {
         long index = CommonUtil.getVmIndex(node.getVmName());
         if (index < oldInstanceNum) {
            // do not verify existing nodes from last successful deployment
            continue;
         }
         //for cloudera/ambari cluster scale out operation, the status is correct
         //for ironfan deployed cluster, the NodeStatus might be not accurate sometimes
         //But in this case, as we judge whether to rollback db by VM creation/VM_READY/SERVICE_READY,
         //there will be no problem here
         if (node.getStatus().ordinal() < NodeStatus.SERVICE_READY.ordinal()) {
            serviceFailedNodeNames.add(node.getVmName());
         }
      }
      if (serviceFailedNodeNames.isEmpty()) {
         return false;
      } else {
         logger.info(" The following VMs " + serviceFailedNodeNames.toString() + " are not SERVICE_READY");
         return true;
      }
   }

   private void updateDefinedInstanceNum(String clusterName, String groupName,
         Long instanceNum) {
      int intNum = instanceNum.intValue();
      AuAssert.check(intNum > 0, String.format("The instance number %s should be larger than 0", intNum));

      ClusterEntity cluster = getClusterEntityMgr().findByName(clusterName);
      NodeGroupEntity groupEntity = getClusterEntityMgr().findByName(cluster, groupName);

      logger.info("Set cluster " + clusterName + " group " + groupName
            + " instance number from " + groupEntity.getDefineInstanceNum() + " to " + intNum);
      groupEntity.setDefineInstanceNum(intNum);
      getClusterEntityMgr().update(groupEntity);
   }

}
