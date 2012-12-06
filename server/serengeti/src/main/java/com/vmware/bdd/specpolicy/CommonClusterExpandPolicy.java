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
package com.vmware.bdd.specpolicy;

import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConversionException;
import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.InstanceType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.manager.DistroManager;
import com.vmware.bdd.manager.DistroManager.PackagesExistStatus;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.HadoopDistroMap;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.AppConfigValidationUtils;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;
import com.vmware.bdd.utils.Configuration;
import com.vmware.bdd.utils.ValidateResult;

public class CommonClusterExpandPolicy {
   private static final Logger logger = Logger.getLogger(CommonClusterExpandPolicy.class);
   private static int[][] templateStorage;
   static {
      initTemplateValues();
   }

   private static void initTemplateValues() {
      templateStorage = new int[GroupType.values().length][InstanceType.values().length];
      
      int value = setTemplateStorage("storage.mastergroup.extralarge",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 200);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.EXTRA_LARGE.ordinal()] = value;
      logger.debug("extra large storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.large",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 100);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.LARGE.ordinal()] = value;
      logger.debug("large storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.medium",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 50);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.MEDIUM.ordinal()] = value;
      logger.debug("medium storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.mastergroup.small",
            GroupType.MASTER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 25);
      templateStorage[GroupType.MASTER_JOBTRACKER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] =  value;
      templateStorage[GroupType.HBASE_MASTER_GROUP.ordinal()][InstanceType.SMALL.ordinal()] = value;
      logger.debug("small storage of master group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.extralarge",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 400);
      logger.debug("extra large storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.large",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 200);
      logger.debug("large storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.medium",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 100);
      logger.debug("medium storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.workergroup.small",
            GroupType.WORKER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 50);
      logger.debug("small storage of worker group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.extralarge",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 400);
      logger.debug("extral large storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.large",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 200);
      logger.debug("large storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.medium",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 100);
      logger.debug("medium storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.clientgroup.small",
            GroupType.CLIENT_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 50);
      logger.debug("small storage of client group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.extralarge",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.EXTRA_LARGE.ordinal(), 200);
      logger.debug("extra large storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.large",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.LARGE.ordinal(), 100);
      logger.debug("large storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.medium",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.MEDIUM.ordinal(), 50);
      logger.debug("medium storage of zookeeper group  is " + value + "GB.");

      value = setTemplateStorage("storage.zookeepergroup.small",
            GroupType.ZOOKEEPER_GROUP.ordinal(), InstanceType.SMALL.ordinal(), 25);
      logger.debug("small storage of zookeeper group  is " + value + "GB.");
   }

   private static int setTemplateStorage(String propertyName, int groupType, int instanceType, int defaultVal) {
      int value = 0;
      try {
         value = Configuration.getInt(propertyName, defaultVal);
      } catch (ConversionException e) {
         value = defaultVal;
      }
      templateStorage[groupType][instanceType] = value;
      return value;
   }

   public static void expandGroupInstanceType(NodeGroupEntity ngEntity, GroupType groupType,
         Set<String> sharedPattern, Set<String> localPattern) {
      logger.debug("Expand instance type config for group " + ngEntity.getName());
      InstanceType instanceType = ngEntity.getNodeType();
      if (instanceType == null) {
         // replace with default instanceType
         if (groupType == GroupType.MASTER_GROUP
               || groupType == GroupType.MASTER_JOBTRACKER_GROUP
               || groupType == GroupType.HBASE_MASTER_GROUP
               || groupType == GroupType.ZOOKEEPER_GROUP) {
            instanceType = InstanceType.MEDIUM;
         } else {
            instanceType = InstanceType.SMALL;
         }
      }
      logger.debug("instance type is " + instanceType.toString());

      int memory = ngEntity.getMemorySize();
      if (memory <= 0) {
         ngEntity.setMemorySize(instanceType.getMemoryMB());
      }
      int cpu = ngEntity.getCpuNum();
      if (cpu <= 0) {
         ngEntity.setCpuNum(instanceType.getCpuNum());
      }

      // storage
      if (ngEntity.getStorageSize() <= 0) {
         ngEntity.setStorageSize(getStorage(instanceType, groupType));
         logger.debug("storage size is setting to default value: " + ngEntity.getStorageSize());
      } else {
         logger.debug("storage size is set to : " + ngEntity.getStorageSize());
      }
      if (ngEntity.getStorageType() == null) {
         DatastoreType storeType = groupType.getStorageEnumType();
         if ((sharedPattern == null || sharedPattern.isEmpty()) && storeType == DatastoreType.SHARED) {
            storeType = DatastoreType.LOCAL;
         }
         if ((localPattern == null || localPattern.isEmpty()) && storeType == DatastoreType.LOCAL) {
            storeType = DatastoreType.SHARED;
         }
         ngEntity.setStorageType(storeType);
      }
      if (groupType == GroupType.ZOOKEEPER_GROUP) {
         ngEntity.setDiskBisect(true);         
      } else {
         ngEntity.setDiskBisect(false);
      }
   }

   public static void expandDistro(ClusterEntity clusterEntity, ClusterCreate clusterConfig, DistroManager distroMgr) {
      String distro = clusterEntity.getDistro();
      clusterConfig.setDistro(distro);
      PackagesExistStatus status = distroMgr.checkPackagesExistStatus(distro);
      switch (status) {
      case TARBALL:
         HadoopDistroMap map = new HadoopDistroMap();
         map.setHadoopUrl(distroMgr.getPackageUrlByDistroRole(distro, HadoopRole.HADOOP_NAMENODE_ROLE.toString()));
         map.setHiveUrl(distroMgr.getPackageUrlByDistroRole(distro, HadoopRole.HIVE_ROLE.toString()));
         map.setPigUrl(distroMgr.getPackageUrlByDistroRole(distro, HadoopRole.PIG_ROLE.toString()));
         map.setHbaseUrl(distroMgr.getPackageUrlByDistroRole(distro, HadoopRole.HBASE_MASTER_ROLE.toString()));
         map.setZookeeperUrl(distroMgr.getPackageUrlByDistroRole(distro, HadoopRole.ZOOKEEPER_ROLE.toString()));
         clusterConfig.setDistroMap(map);
         break;
      case REPO:
         clusterConfig.setPackageRepos(distroMgr.getPackageRepos(distro));
         break;
      case NONE:
         throw ClusterConfigException.MANIFEST_CONFIG_TARBALL_REPO_NONE();
      case BOTH:
         throw ClusterConfigException.MANIFEST_CONFIG_TARBALL_REPO_COEXIST();
      }
   }

   private static int getStorage(InstanceType instance, GroupType groupType) {
      return templateStorage[groupType.ordinal()][instance.ordinal()];
   }

   public static void validateAppConfig(Map<String, Object> appConfigs, boolean checkWhiteList) {
      // validate hadoop config
      if (checkWhiteList) {
         logger.debug("Validate hadoop configuration in white list.");
         ValidateResult valid = AppConfigValidationUtils.validateConfig(ValidationType.WHITE_LIST, appConfigs);
         switch (valid.getType()) {
         case WHITE_LIST_INVALID_VALUE:
            throw ClusterConfigException.INVALID_APP_CONFIG_VALUE(valid.getFailureNames());
         case WHITE_LIST_INVALID_NAME:
            logger.warn("Hadoop configurations " + valid.getFailureNames() + " not in white list.");
            break;
         default:
            logger.debug("Passed white list validation.");
            break;
         }
      }
      logger.debug("Validate hadoop configuration in black list.");
      ValidateResult valid = AppConfigValidationUtils.validateConfig(ValidationType.BLACK_LIST, appConfigs);
      switch (valid.getType()) {
      case NAME_IN_BLACK_LIST:
         logger.warn("Hadoop configurations " + valid.getFailureNames() + " in black list. The configuration for these parameters do not take effect.");
      default:
         logger.debug("Passed black list validation.");
         break;
      }
   }
}
