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
package com.vmware.bdd.plugin.ironfan.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.bdd.utils.*;
import org.apache.log4j.Logger;

import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.plugin.ironfan.utils.ChefServerUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.NodeGroupRole;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;

public class ClusterValidator {
   private static final Logger logger = Logger.getLogger(ClusterValidator.class);

   public boolean validateBlueprint(ClusterBlueprint blueprint, List<String> distroRoles)
         throws ValidationException {
      logger.info("Start to validate bludprint for cluster " + blueprint.getName());
      return validateDistros(blueprint, distroRoles);
   }

   private boolean validateDistros(ClusterBlueprint blueprint,
         List<String> distroRoles) throws ValidationException {
      validateClusterConfig(blueprint);
      return validateRoles(blueprint, distroRoles);
   }

   private void validateClusterConfig(ClusterBlueprint blueprint) {
      validateHadoopConfig(blueprint.getConfiguration(), blueprint.isNeedToValidateConfig());
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         validateHadoopConfig(group.getConfiguration(), blueprint.isNeedToValidateConfig());
      }
   }

   /**
    * Validate role's existence
    *
    * @param blueprint
    * @param distroRoles
    * @return
    * @throws SoftwareManagementPluginException
    */
   private boolean validateRoles(ClusterBlueprint blueprint,
         List<String> distroRoles) throws ValidationException {
      assert (blueprint != null && distroRoles != null);
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      // only check roles validity in server side, but not in CLI and GUI, because roles info exist in server side.
      checkUnsupportedRoles(blueprint, distroRoles, failedMsgList);
      boolean result = validateRoleDependency(failedMsgList, blueprint);
      // only validate group config for non-mapr distros
      if (!Constants.MAPR_VENDOR.equalsIgnoreCase(blueprint.getHadoopStack().getVendor())) {
         validateGroupConfig(blueprint, failedMsgList, warningMsgList);
      }
      if (!failedMsgList.isEmpty() || !warningMsgList.isEmpty()) {
         throw ValidationException.VALIDATION_FAIL("Roles", failedMsgList, warningMsgList);
      }
      return result;
   }

   /**
    * Check whether the roles used in the cluster exist in distro manifest and
    * Chef Server.
    *
    */
   private void checkUnsupportedRoles(ClusterBlueprint blueprint,
         List<String> distroRoles, List<String> failedMsgList) {
      List<NodeGroupInfo> nodeGroupInfos = blueprint.getNodeGroups();
      assert (nodeGroupInfos != null && !nodeGroupInfos.isEmpty());

      List<String> invalidRoleList = new ArrayList<String>();
      List<String> unspportedRoleList = new ArrayList<String>();

      for (NodeGroupInfo nodeGroup : nodeGroupInfos) {
         List<String> roles = nodeGroup.getRoles();
         if (roles != null) {
            for (String role : roles) {
               if (!ChefServerUtils.isValidRole(role)) {
                  invalidRoleList.add(role);
               } else if (!distroRoles.contains(role)
                     && !HadoopRole.isCustomizedRole(role)) {
                  unspportedRoleList.add(role);
                  /*      .append(" is not supported by distro ")
                        .append(blueprint.getHadoopStack().getDistro());*/
               }
            }
         }
      }

      if(invalidRoleList.size() > 0) {
         String msgFormat = invalidRoleList.size() > 1 ? "roles: %1s are invalid." : "role: %1s is invalid.";
         failedMsgList.add(String.format(
               msgFormat, new ListToStringConverter(invalidRoleList, ',')
         ));
      }

      if(unspportedRoleList.size() > 0) {
         String msgFormat = unspportedRoleList.size() > 1 ? "roles: %1s are not supported by %2s." : "role: %1s is not supported by %2s.";
         failedMsgList.add(String.format(
               msgFormat, new ListToStringConverter(unspportedRoleList, ','), blueprint.getHadoopStack().getDistro()
         ));
      }
   }

   /*
   * Validate role dependency:
   * Case 1: compute node group with external hdfs node group.
   * Case 2: The dependency check of HDFS, MapReduce, HBase, Zookeeper,
   * Hadoop Client(Pig, Hive, Hadoop Client), and HBase Client Combinations. The rules are below:
   * - HDFS includes roles of "haddop_namenode" and "hadoop_datanode";
   * - MapReduce includes roles of "haddop_jobtracker" and "hadoop_takstracker";
   * - HBase includes roles of "hbase_master" and "hbase_regionserver;
   * - Zookeeper includes a single role of "zookeeper";
   * - Hadoop Client includes roles of "hadoop_client";
   * - HBase client includes roles of "hbase_client";
   * - Pig includes roles of "pig";
   * - Hive includes roles of "hive";
   * - Hive Server includes roles of "hive_server";
   * - MapReduce depends on HDFS, HBase depends on HDFS and Zookeeper;
   * - Pig, Hive, Hive Server depends on MapReduce, HBase Client depends on HBase;
   * - Hadoop Client depends on HDFS.
   */
   public boolean validateRoleDependency(List<String> failedMsgList,
         ClusterBlueprint blueprint) {
      boolean valid = true;
      Set<String> roles = new HashSet<String>();
      List<NodeGroupInfo> nodeGroups = blueprint.getNodeGroups();
      if (nodeGroups == null) {
//         failedMsgList.add("Missing JobTracker or TaskTracker role.");
         return false;
      }

      for (NodeGroupInfo nodeGroupCreate : nodeGroups) {
         List<String> nodeGroupRoles = nodeGroupCreate.getRoles();
         if (nodeGroupRoles == null || nodeGroupRoles.isEmpty()) {
            valid = false;
            failedMsgList.add("Missing role attribute for node group "
                  + nodeGroupCreate.getName() + ".");
         } else {
            roles.addAll(nodeGroupCreate.getRoles());
         }
      }

      if (validateHDFSUrl(blueprint)) {
         if(!roles.contains("hbase_master")){
            // TODO: consider Ambari need specify hadoop_namenode role when use of external HDFS
            if (roles.contains("hadoop_namenode")
                  || roles.contains("hadoop_datanode")) {
               valid = false;
               failedMsgList.add("Duplicate NameNode or DataNode role.");
            }
            if (!hasMapreduceConfigured(blueprint)) {
               if (!roles.contains("hadoop_jobtracker")
                     && !roles.contains("hadoop_resourcemanager")) {
                  valid = false;
                  failedMsgList.add("Missing JobTracker or ResourceManager role.");
               }
               if (!roles.contains("hadoop_tasktracker")
                     && !roles.contains("hadoop_nodemanager")) {
                  valid = false;
                  failedMsgList.add("Missing TaskTracker or NodeManager role.");
               }
            }
         }
      } else if (!hasMapreduceConfigured(blueprint)){ //case 2
         // get involved service types of the spec file
         EnumSet<ServiceType> serviceTypes = EnumSet.noneOf(ServiceType.class);
         for (ServiceType service : ServiceType.values()) {
            //identify partially match
            List<HadoopRole> missingRoles = new ArrayList<>();
            for (HadoopRole role : service.getRoles()) {
               if (!roles.contains(role.toString())) {
                  missingRoles.add(role);
               }
            }

            //no missing roles, meaning this service is added in the spec
            if (missingRoles.size() == 0) {
               serviceTypes.add(service);
            }
            //if the roles for this service is not enough, we have to set it as failure.
            else if(missingRoles.size() < service.getRoles().size()){
               failedMsgList.add(
                     String.format("Missing role(s): %1s for service: %2s.",
                           new ListToStringConverter(missingRoles, ','), service)
               );
               valid = false;
            }
            //if all roles are missing, meaning this service is not added in the spec
            //then later check service dependencies.
            //lixl.
         }

         boolean isYarn = serviceTypes.contains(ServiceType.YARN);
         if (isYarn && serviceTypes.contains(ServiceType.MAPRED)) {
            failedMsgList.add("You cannot set " + ServiceType.MAPRED + " "
                  + ServiceType.MAPRED.getRoles() + " and " + ServiceType.YARN
                  + " " + ServiceType.YARN.getRoles() + " \nat the same time.");
            valid = false;
         }
         //validate the relationships of services
         if (valid == true && !serviceTypes.isEmpty()) {
            for (ServiceType service : serviceTypes) {
               EnumSet<ServiceType> dependency = service.depend(isYarn);
               if (dependency != null && !serviceTypes.containsAll(dependency)) {
                  failedMsgList.add("Some dependent services " + dependency
                        + " " + service
                        + " relies on cannot be found in the spec file.");
                  valid = false;
               }
            }
         }
      }

      return valid;
   }

   public void validateGroupConfig(ClusterBlueprint blueprint,
         List<String> failedMsgList, List<String> warningMsgList) {
      List<NodeGroupInfo> nodeGroups = blueprint.getNodeGroups();
      // if hadoop2 namenode ha is enabled
      boolean namenodeHACheck = false;
      //role count
      int masterCount = 0, jobtrackerCount = 0, resourcemanagerCount = 0, hbasemasterCount =
            0, zookeeperCount = 0, workerCount = 0, numOfJournalNode = 0;
      for (NodeGroupInfo nodeGroup : nodeGroups) {
            // get node group role.
            List<NodeGroupRole> groupRoles = getNodeGroupRoles(nodeGroup);
            if (groupRoles != null) {
               for (NodeGroupRole role : groupRoles) {
                  switch (role) {
                  case MASTER:
                     masterCount++;
                     int numOfInstance = nodeGroup.getInstanceNum();
                     if (numOfInstance >= 0 && numOfInstance != 1) {
                        if (numOfInstance != 2) { //namenode ha only support 2 nodes currently
                           collectInstanceNumInvalidateMsg(nodeGroup,
                                 failedMsgList);
                        } else {
                           namenodeHACheck = true;
                        }
                     }
                     break;
                  case JOB_TRACKER:
                     jobtrackerCount++;
                     if (nodeGroup.getInstanceNum() >= 0
                           && nodeGroup.getInstanceNum() != 1) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_JOBTRACKER);
                     }
                     break;
                  case RESOURCEMANAGER:
                     resourcemanagerCount++;
                     if (nodeGroup.getInstanceNum() >= 0
                           && nodeGroup.getInstanceNum() != 1) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_RESOURCEMANAGER);
                     }
                     break;
                  case HBASE_MASTER:
                     hbasemasterCount++;
                     if (nodeGroup.getInstanceNum() == 0) {
                        collectInstanceNumInvalidateMsg(nodeGroup,
                              failedMsgList);
                     }
                     break;
                  case ZOOKEEPER:
                     zookeeperCount++;
                     if (nodeGroup.getInstanceNum() > 0
                           && nodeGroup.getInstanceNum() < 3) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_ZOOKEEPER);
                     } else if (nodeGroup.getInstanceNum() > 0
                           && nodeGroup.getInstanceNum() % 2 == 0) {
                        warningMsgList.add(Constants.ODD_NUM_OF_ZOOKEEPER);
                     }
                     break;
                  case JOURNAL_NODE:
                     numOfJournalNode += nodeGroup.getInstanceNum();
                     if (nodeGroup.getRoles().contains(
                           HadoopRole.HADOOP_DATANODE.toString())
                           || nodeGroup.getRoles().contains(
                                 HadoopRole.HADOOP_CLIENT_ROLE.toString())) {
                        failedMsgList
                        .add(Constants.DATA_CLIENT_NODE_JOURNALNODE_COEXIST);
                     }
                     break;
                  case WORKER:
                     workerCount++;
                     if (nodeGroup.getInstanceNum() == 0) {
                        collectInstanceNumInvalidateMsg(nodeGroup,
                              failedMsgList);
                     } else if (nodeGroup.isHaEnabled()) {
                        warningMsgList.add(Constants.WORKER_CLIENT_HA_FLAG);
                     }

                     //check if datanode and region server are seperate
                     List<String> roles = nodeGroup.getRoles();
                     // TODO: After refactor the error message handle,  need uncomment it.
                     /* if (roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE
                           .toString())
                           && !roles.contains(HadoopRole.HADOOP_DATANODE
                                 .toString())) {
                        warningMsgList
                        .add(Constants.REGISONSERVER_DATANODE_SEPERATION);
                     } */
                     break;
                  case CLIENT:
                     if (nodeGroup.isHaEnabled()) {
                        warningMsgList.add(Constants.WORKER_CLIENT_HA_FLAG);
                     }
                     break;
                  case NONE:
                     // server side will validate whether the roles of this group exist
                     break;
                  default:
                     break;
                  }
               }
            }
      }
      if (!supportedWithHdfs2(blueprint)) {
         if (namenodeHACheck || masterCount > 1) {
            failedMsgList.add(Constants.CURRENT_DISTRO_CAN_NOT_SUPPORT_HDFS2);
         }
      } else if (namenodeHACheck) {
         if (numOfJournalNode >= 0 && numOfJournalNode < 3) {
            failedMsgList.add(Constants.WRONG_NUM_OF_JOURNALNODE);
         } else if (numOfJournalNode > 0 && numOfJournalNode % 2 == 0) {
            warningMsgList.add(Constants.ODD_NUM_OF_JOURNALNODE);
         }
         //check if zookeeper exists for automatic namenode ha failover
         if (zookeeperCount == 0) {
            failedMsgList.add(Constants.NAMENODE_AUTO_FAILOVER_ZOOKEEPER);
         }
      }
      if ((jobtrackerCount > 1) || (resourcemanagerCount > 1)
            || (zookeeperCount > 1) || (hbasemasterCount > 1)) {
         failedMsgList.add(Constants.WRONG_NUM_OF_NODEGROUPS);
      }
      if (numOfJournalNode > 0 && !namenodeHACheck) {
         failedMsgList.add(Constants.NO_NAMENODE_HA);
      }
      if (!warningMsgList.isEmpty() && !warningMsgList.get(0).startsWith("Warning: ")) {
         warningMsgList.set(0, "Warning: " + warningMsgList.get(0));
      }
   }

   private void validateHadoopConfig(Map<String, Object> appConfigs, boolean checkWhiteList) {
      if (appConfigs == null || appConfigs.isEmpty())
         return;
      // validate hadoop config
      if (checkWhiteList) {
         logger.debug("Validate hadoop configuration in white list.");
         ValidateResult valid = AppConfigValidationUtils.validateConfig(ValidationType.WHITE_LIST, appConfigs);
         switch (valid.getType()) {
         case WHITE_LIST_INVALID_VALUE:
            throw ClusterConfigException.INVALID_APP_CONFIG_VALUE(valid.getFailureValues());
         case WHITE_LIST_INVALID_NAME:
            logger.warn("Hadoop configurations " + valid.getNoExistFileNames() + " " +valid.getFailureNames() + " not in white list.");
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

   public boolean validateHDFSUrl(ClusterBlueprint blueprint) {
      if (blueprint.getExternalHDFS() != null) {
         try {
            URI uri = new URI(blueprint.getExternalHDFS());
            if (!"hdfs".equalsIgnoreCase(uri.getScheme())
                  || uri.getHost() == null) {
               return false;
            }
            return true;
         } catch (Exception ex) {
            ex.printStackTrace();
            return false;
         }
      }
      return false;
   }

   private void collectInstanceNumInvalidateMsg(NodeGroupInfo nodeGroup,
         List<String> failedMsgList) {
      failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
            .append(".").append("instanceNum=")
            .append(nodeGroup.getInstanceNum()).append(".").toString());
   }


   private List<NodeGroupRole> getNodeGroupRoles(NodeGroupInfo nodeGroup) {
      List<NodeGroupRole> groupRoles = new ArrayList<NodeGroupRole>();
      //Find roles list from current  NodeGroupCreate instance.
      List<String> roles = nodeGroup.getRoles();
      for (NodeGroupRole role : NodeGroupRole.values()) {
         if (roles != null && matchRole(role, roles)) {
            groupRoles.add(role);
         }
      }
      if (groupRoles.size() == 0) {
         groupRoles.add(NodeGroupRole.NONE);
      }
      return groupRoles;
   }

   /**
    * Check the roles was introduced, whether matching with system's specialize
    * role.
    */
   private boolean matchRole(NodeGroupRole role, List<String> roles) {
      switch (role) {
         case MASTER:
            if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case JOB_TRACKER:
            if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case RESOURCEMANAGER:
            if (roles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case HBASE_MASTER:
            if (roles.contains(HadoopRole.HBASE_MASTER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case ZOOKEEPER:
            if (roles.contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case JOURNAL_NODE:
            if (roles.contains(HadoopRole.HADOOP_JOURNALNODE_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case WORKER:
            if (roles.contains(HadoopRole.HADOOP_DATANODE.toString())
                  || roles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())
                  || roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE.toString())
                  || roles.contains(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case CLIENT:
            if (roles.contains(HadoopRole.HADOOP_CLIENT_ROLE.toString())
                  || roles.contains(HadoopRole.HIVE_ROLE.toString())
                  || roles.contains(HadoopRole.HIVE_SERVER_ROLE.toString())
                  || roles.contains(HadoopRole.PIG_ROLE.toString())
                  || roles.contains(HadoopRole.HBASE_CLIENT_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case NONE:
            break;
         default:
            break;
      }
      return false;
   }

   // For HDFS2, apache, mapr, and gphd distros do not have hdfs2 features.
   private boolean supportedWithHdfs2(ClusterBlueprint blueprint) {
      String vendor = blueprint.getHadoopStack().getVendor();
      if (vendor != null && (vendor.equalsIgnoreCase(Constants.APACHE_VENDOR)
            || vendor.equalsIgnoreCase(Constants.MAPR_VENDOR)
            || vendor.equalsIgnoreCase(Constants.GPHD_VENDOR))){
         return false;
      }
      return true;
   }

   public boolean hasMapreduceConfigured(ClusterBlueprint cluster) {
      return !CommonUtil.isBlank(cluster.getExternalMapReduce());
   }
}
