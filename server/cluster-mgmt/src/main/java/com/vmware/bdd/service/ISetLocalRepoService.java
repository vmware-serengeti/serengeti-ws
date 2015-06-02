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

package com.vmware.bdd.service;

import java.util.List;

import com.vmware.bdd.entity.NodeEntity;

public interface ISetLocalRepoService {
   /**
    * Set local repo for nodes in cluster
    *
    * @param clusterName
    * @param nodes
    * @param password
    * @return failed nodes list
    */
   public boolean setLocalRepoForNodes(String clusterName, List<NodeEntity> nodes, String repoId, String localRepoURL);

   /**
    * Set local repo for node in cluster
    *
    * @param clusterName
    * @param fixedNodeIP
    * @param newPassword
    * @return success or not
    * @throws Exception
    */
   public boolean setLocalRepoForNode(String clusterName, NodeEntity node, String repoId, String localRepoURL) throws Exception;

   public void updateNodeData(NodeEntity nodeEntity, boolean b, String errMsg, String currentTimestamp);
}
