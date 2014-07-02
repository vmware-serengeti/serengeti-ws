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
package com.vmware.bdd.dal;

import java.util.List;
import java.util.Set;

import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeEntity;


/**
 * @author Terry Li
 * @since 0.8
 * @version 0.8
 *
 */
public interface IClusterDAO extends IBaseDAO<ClusterEntity> {

   ClusterEntity findByName(String name);

   List<String> findClustersByUsedResourcePool(String rpName);

   List<String> findClustersByUsedDatastores(Set<String> patterns);

   List<String> findClustersByAppManager(String appManagerName);

   boolean isExist(String name);

   void updateStatus(String name, ClusterStatus status);

   List<NodeEntity> getAllNodes(String name);

   ClusterStatus getStatus(String clusterName);

   void updateLastStatus(String clusterName, ClusterStatus status);
}
