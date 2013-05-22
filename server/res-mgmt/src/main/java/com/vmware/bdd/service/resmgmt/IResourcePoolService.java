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
package com.vmware.bdd.service.resmgmt;

import java.util.List;
import java.util.Set;

import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.spectypes.VcCluster;


/**
 * @author Jarred Li
 * @since 0.8
 * @version 0.8
 * 
 */
public interface IResourcePoolService {

   List<VcCluster> getAllVcResourcePool();

   void addResourcePool(String rpName, String vcClusterName,
         String vcResourcePool);

   List<VcCluster> getVcResourcePoolByName(String name);

   List<VcCluster> getVcResourcePoolByNameList(String[] names);

   Set<String> getAllRPNames();

   List<ResourcePoolRead> getAllResourcePoolForRest();

   ResourcePoolRead getResourcePoolForRest(String rpName);

   void deleteResourcePool(String rpName);

   /**
    * <p>
    * This method is used for verifying whether current vApp is deployed under
    * the cluster direct or not. More specifically, returns <code>true</code> if
    * current vApp is deployed under cluster. Otherwise, returns
    * <code>false</code>.
    * 
    * @param clusterName
    * @param vcRPName
    * @return
    */
   boolean isDeployedUnderCluster(final String clusterName,
         final String vcRPName);

   List<String> addAutoResourcePools(List<VcCluster> vcClusters,
         boolean ignoreDuplicate);
}