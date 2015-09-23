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
package com.vmware.bdd.service.job;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.ISetLocalRepoService;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class ExpandClusterConfigLocalRepoStep extends ConfigLocalRepoStep {
   private static final Logger logger = Logger
         .getLogger(ExpandClusterConfigLocalRepoStep.class);

   @Override
   protected List<NodeEntity> getNodesToBeSetLocalRepo(ChunkContext chunkContext,
         String clusterName) throws TaskException {
        List<NodeEntity> toBeSetLocalRepo = null;
        List<NodeEntity> nodesInGroup = null;
        List<String> nodeGroupNames = new ArrayList<String>();

        String nodeGroupNameList =
               TrackableTasklet.getJobParameters(chunkContext).getString(
                       JobConstants.NEW_NODE_GROUP_LIST_JOB_PARAM);

        logger.info("ExpandClusterConfigLocalRepoStep===getNodesToBeSetPassword");
        for (String nodeGroupName : nodeGroupNameList.split(",")){
           nodeGroupNames.add(nodeGroupName);
        }

        for (String nodeGroupName: nodeGroupNames) {
           nodesInGroup = clusterEntityMgr.findAllNodes(clusterName, nodeGroupName);
        }

        for (NodeEntity node : nodesInGroup) {
           if (node.getStatus().ordinal() >= NodeStatus.VM_READY.ordinal()) {
               if (toBeSetLocalRepo == null) {
                   toBeSetLocalRepo = new ArrayList<NodeEntity>();
               }
               toBeSetLocalRepo.add(node);
           }
        }
        return toBeSetLocalRepo;
   }


}
