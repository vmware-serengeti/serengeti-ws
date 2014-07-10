/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved
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
package com.vmware.bdd.specpolicy;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.InstanceType;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.spectypes.HadoopDistroMap;
import com.vmware.bdd.spectypes.IronfanStack;

public class CommonClusterExpandPolicy {
   private static final Logger logger = Logger.getLogger(CommonClusterExpandPolicy.class);

   public static void expandGroupInstanceType(NodeGroupEntity ngEntity, NodeGroupCreate group,
         Set<String> sharedPattern, Set<String> localPattern) {
      logger.debug("Expand instance type config for group " + ngEntity.getName());
      InstanceType instanceType = ngEntity.getNodeType();
      int memory = ngEntity.getMemorySize();
      int cpu = ngEntity.getCpuNum();
      if (instanceType == null && (cpu == 0 || memory == 0)) {
         throw ClusterConfigException.INSTANCE_SIZE_NOT_SET(group.getName());
      }
      if (instanceType == null) {
         logger.debug("instance type is not set.");
      } else {
         logger.debug("instance type is " + instanceType.toString());
      }

      if (memory == 0) {
         ngEntity.setMemorySize(instanceType.getMemoryMB());
      }
      if (cpu == 0) {
         ngEntity.setCpuNum(instanceType.getCpuNum());
      }

      // storage
      logger.debug("storage size is set to : " + ngEntity.getStorageSize());
      if (ngEntity.getStorageType() == null) {
         String expectedType = group.getStorage().getExpectedTypeFromRoles();
         DatastoreType storeType = DatastoreType.valueOf(expectedType);
         if ((sharedPattern == null || sharedPattern.isEmpty())
               && DatastoreType.SHARED == storeType) {
            storeType = DatastoreType.LOCAL;
         }
         if ((localPattern == null || localPattern.isEmpty())
               && DatastoreType.LOCAL == storeType) {
            storeType = DatastoreType.SHARED;
         }
         ngEntity.setStorageType(storeType);
      } else {
         if ((sharedPattern == null || sharedPattern.isEmpty()) 
               && (ngEntity.getStorageType().equals(DatastoreType.SHARED))) {
            String msg =
                  "Group "
                        + ngEntity.getName()
                        + " is type SHARED, but there are no shared datastore in Serengeti.";
            logger.error(msg);
            throw ClusterConfigException.CLUSTER_CONFIG_DATASTORE_TYPE_NONEXISTENT(msg);
         }
         if ((localPattern == null || localPattern.isEmpty()) 
               && (ngEntity.getStorageType().equals(DatastoreType.LOCAL))) {
            String msg = "Group " + ngEntity.getName() + "'s  type is LOCAL, but no local datastore in serengeti.";
            logger.error(msg);
            throw ClusterConfigException.CLUSTER_CONFIG_DATASTORE_TYPE_NONEXISTENT(msg);
         }
      }
   }

   public static void expandDistro(ClusterCreate clusterConfig,
         IronfanStack stack) {
      String packagesExistStatus = stack.getPackagesExistStatus();
      clusterConfig.setPackagesExistStatus(packagesExistStatus);
      switch (packagesExistStatus) {
      case "TARBALL":
         HadoopDistroMap map = new HadoopDistroMap();
         Map<String, String> distroMap = stack.getHadoopDistroMap();
         map.setHadoopUrl(distroMap.get("HadoopUrl"));
         map.setHiveUrl(distroMap.get("HiveUrl"));
         map.setPigUrl(distroMap.get("PigUrl"));
         map.setHbaseUrl(distroMap.get("HbaseUrl"));
         map.setZookeeperUrl(distroMap.get("ZookeeperUrl"));
         clusterConfig.setDistroMap(map);
         break;
      case "REPO":
         clusterConfig.setPackageRepos(stack.getPackageRepos());
         break;
      case "NONE":
         throw ClusterConfigException.MANIFEST_CONFIG_TARBALL_REPO_NONE();
      case "BOTH":
         throw ClusterConfigException.MANIFEST_CONFIG_TARBALL_REPO_COEXIST();
      }
   }
}
