/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRoleContainer;
import com.vmware.bdd.plugin.clouderamgr.utils.CmUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.utils.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Author: Xiaoding Bian
 * Date: 7/11/14
 * Time: 1:37 PM
 */
public class CmClusterValidator {

   private static final Logger logger = Logger.getLogger(CmClusterValidator.class);
   private List<String> warningMsgList;
   private List<String> errorMsgList;

   public CmClusterValidator() {
      this.warningMsgList = new ArrayList<String>();
      this.errorMsgList = new ArrayList<String>();
   }

   public boolean validateBlueprint(ClusterBlueprint blueprint) {
      logger.info("Start to validate blueprint for cluster " + blueprint.getName());

      String distro = blueprint.getHadoopStack().getDistro();
      String distroVersion = CmUtils.distroVersionOfHadoopStack(blueprint.getHadoopStack());

      try {
         List<String> unRecogConfigTypes = new ArrayList<String>();
         List<String> unRecogConfigKeys = new ArrayList<>();
         validateConfigs(blueprint.getConfiguration(), unRecogConfigTypes, unRecogConfigKeys, distroVersion);

         Set<String> availableRoles = AvailableServiceRoleContainer.allRoles(distroVersion);

         Set<String> definedServices = new HashSet<String>();
         Map<String, Integer> definedRoles = new HashMap<String, Integer>();

         List<String> unRecogRoles = null;
         Set<String> invalidRacks = null;

         if (blueprint.getNodeGroups() == null || blueprint.getNodeGroups().isEmpty()) {
            return false;
         }

         int nnGroupsNum = 0;
         for (NodeGroupInfo group : blueprint.getNodeGroups()) {
            validateConfigs(group.getConfiguration(), unRecogConfigTypes, unRecogConfigKeys, distroVersion);
            if (group.getRoles().contains("HDFS_NAMENODE")) {
               nnGroupsNum++;
            }

            for (String roleName: group.getRoles()) {
               if (!availableRoles.contains(roleName)) {
                  if (unRecogRoles == null) {
                     unRecogRoles = new ArrayList<String>();
                  }
                  unRecogRoles.add(roleName);
               } else {
                  if (!definedRoles.containsKey(roleName)) {
                     definedRoles.put(roleName, group.getInstanceNum());
                  } else {
                     Integer instanceNum = definedRoles.get(roleName) + group.getInstanceNum();
                     definedRoles.put(roleName, instanceNum);
                  }
                  definedServices.add(AvailableServiceRoleContainer.load(roleName).getParent().getDisplayName());
               }
            }
         }

         if (nnGroupsNum > 1) {
            errorMsgList.add("Namenode federation is not supported currently");
         }

         if (unRecogRoles != null && !unRecogRoles.isEmpty()) {      // point 1: unrecognized roles
            errorMsgList.add("Roles " + unRecogRoles.toString() + " are not available by distro " + distro);
         }

         if (!unRecogConfigTypes.isEmpty()) {                        // point 2: add to warning list as will be ignored by creating logic
            warningMsgList.add("Configurations for " + unRecogConfigTypes.toString() + " are not available by distro " + distro);
         }

         if (!unRecogConfigKeys.isEmpty()) {                         // point 3
            errorMsgList.add("Configuration items " + unRecogConfigKeys.toString() + " are invalid");
         }

         if (invalidRacks != null && !invalidRacks.isEmpty()) {
            errorMsgList.add("Racks " + invalidRacks.toString() + " are invalid," +
                  " rack names must be slash-separated, like Unix paths. For example, \"/rack1\" and \"/cabinet3/rack4\"");
         }

         for (String serviceName : definedServices) {
            // service dependency check
            for (AvailableServiceRole.Dependency dependency : AvailableServiceRoleContainer.load(serviceName).getDependencies()) {
               if (!dependency.isRequired()) {
                  continue;
               }
               if (dependency.getServices().size() == 1 && !definedServices.contains(dependency.getServices().get(0))) {
                  if (serviceName.equals("YARN") && isComputeOnly(definedServices)) {
                     continue;
                  }
                  warningMsgList.add(serviceName + " depends on " + dependency.getServices().get(0) + " service");
               } else {
                  boolean found = false;
                  for (String dependService : dependency.getServices()) {
                     if (definedServices.contains(dependService)) {
                        found = true;
                     }
                  }
                  if (!found) {
                     warningMsgList.add(serviceName + " depends on one service of " + dependency.getServices().toString());
                  }
               }
            }

            Set<String> requiredRoles = new HashSet<String>();
            switch (serviceName) {
               case "HDFS":
                  if (!isComputeOnly(definedServices)) {
                     requiredRoles.add("HDFS_NAMENODE");
                     requiredRoles.add("HDFS_DATANODE");
                  }
                  if (checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList)) {
                     if (nnGroupsNum == 1) {
                        if (definedRoles.get("HDFS_NAMENODE") < 2 && !definedRoles.containsKey("HDFS_SECONDARY_NAMENODE")) {
                           errorMsgList.add("HDFS service not configured for High Availability must have a SecondaryNameNode");
                        }
                        if (definedRoles.get("HDFS_NAMENODE") >= 2 && !definedRoles.containsKey("HDFS_JOURNALNODE")) {
                           errorMsgList.add("HDFS service configured for High Availability must have journal nodes");
                        }
                     }
                  }
                  if (definedRoles.containsKey("HDFS_JOURNALNODE")) {
                     if (definedRoles.get("HDFS_JOURNALNODE") > 1 && definedRoles.get("HDFS_JOURNALNODE") < 3) {
                        errorMsgList.add(Constants.WRONG_NUM_OF_JOURNALNODE);
                     } else if (definedRoles.get("HDFS_JOURNALNODE") % 2 == 0) {
                        warningMsgList.add(Constants.ODD_NUM_OF_JOURNALNODE);
                     }
                  }
                  break;
               case "YARN":
                  requiredRoles.add("YARN_RESOURCE_MANAGER");
                  requiredRoles.add("YARN_NODE_MANAGER");
                  requiredRoles.add("YARN_JOB_HISTORY");
                  if (checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList)) {
                     if (definedRoles.get("YARN_RESOURCE_MANAGER") > 1) {
                        errorMsgList.add(Constants.WRONG_NUM_OF_RESOURCEMANAGER);
                     }
                  }
                  break;
               case "MAPREDUCE":
                  requiredRoles.add("MAPREDUCE_JOBTRACKER");
                  requiredRoles.add("MAPREDUCE_TASKTRACKER");
                  if (checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList)) {
                     if (definedRoles.get("MAPREDUCE_JOBTRACKER") > 1) {
                        errorMsgList.add(Constants.WRONG_NUM_OF_JOBTRACKER);
                     }
                  }
                  break;
               case "HBASE":
                  requiredRoles.add("HBASE_MASTER");
                  requiredRoles.add("HBASE_REGION_SERVER");
                  checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList);
                  break;
               case "ZOOKEEPER":
                  requiredRoles.add("ZOOKEEPER_SERVER");
                  if (checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList)) {
                     if (definedRoles.get("ZOOKEEPER_SERVER") > 0 && definedRoles.get("ZOOKEEPER_SERVER") < 3) {
                        errorMsgList.add(Constants.WRONG_NUM_OF_ZOOKEEPER);
                     } else if (definedRoles.get("ZOOKEEPER_SERVER") % 2 == 0)  {
                        warningMsgList.add(Constants.ODD_NUM_OF_ZOOKEEPER);
                     }
                  }
                  break;
               case "HIVE":
                  requiredRoles.add("HIVE_METASTORE");
                  requiredRoles.add("HIVE_SERVER2");
                  checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList);
                  String[] requiredConfigs = {
                        "hive_metastore_database_host",
                        "hive_metastore_database_name",
                        "hive_metastore_database_password",
                        "hive_metastore_database_port",
                        "hive_metastore_database_type",
                        "hive_metastore_database_user"
                  };

                  boolean configured = true;
                  if (blueprint.getConfiguration().containsKey("HIVE")) {
                     Map<String, String> configuredItems = (Map<String, String>) blueprint.getConfiguration().get("HIVE");
                     for (String item : requiredConfigs) {
                        if (!configuredItems.containsKey(item)) {
                           configured = false;
                           break;
                        }
                     }
                  } else {
                     configured = false;
                  }

                  if (!configured) {
                     errorMsgList.add("HIVE service depends on an external database, please setup one and provide configuration properties ["
                           + StringUtils.join(requiredConfigs, ",") + "] for HIVE service");
                  }

                  break;
               case "OOZIE":
                  if (definedRoles.get("OOZIE_SERVER") > 1) {
                     errorMsgList.add("only one OOZIE_SERVER is allowed for OOZIE service");
                  }
                  break;
               case "SENTRY":
                  if (definedRoles.get("SENTRY_SERVER") > 1) {
                     errorMsgList.add("only one SENTRY_SERVER is allowed for SENTRY service");
                  }
                  break;
               case "SQOOP":
                  if (definedRoles.get("SQOOP_SERVER") > 1) {
                     errorMsgList.add("only one SQOOP_SERVER is allowed for SQOOP service");
                  }
                  break;
               case "ISILON":
                  requiredRoles.add("YARN_RESOURCE_MANAGER");
                  requiredRoles.add("YARN_JOB_HISTORY");
                  requiredRoles.add("YARN_NODE_MANAGER");
                  requiredRoles.add("GATEWAY");
                  break;
               default:
                  break;
            }

         }

      } catch (IOException e) {
         // IO exception ignored
      }

      if (!warningMsgList.isEmpty() || !errorMsgList.isEmpty()) {
         throw ValidationException.VALIDATION_FAIL("Blueprint", errorMsgList, warningMsgList);
      }

      return true;
   }


   private boolean checkRequiredRoles(String serviceName, Set<String> requiredRoles, Set<String> definedRoles,
         List<String> errorMsgList) {
      requiredRoles.removeAll(definedRoles);
      if (!requiredRoles.isEmpty()) {
         errorMsgList.add("Service " + serviceName + " requires roles " + requiredRoles.toString());
         return false;
      }
      return true;
   }

   private void validateConfigs(Map<String, Object> config, List<String> unRecogConfigTypes,
         List<String> unRecogConfigKeys, String distroVersion) {
      if (config == null || config.isEmpty()) {
         return;
      }

      for (String key : config.keySet()) {
         try {
            if (key.equals(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE)) {
               continue;
            }
            AvailableServiceRole def = AvailableServiceRoleContainer.load(key);
            if (!AvailableServiceRoleContainer.isSupported(distroVersion, def)) {
               unRecogConfigTypes.add(key);
               continue;
            }

            Map<String, String> items = (Map<String, String>) config.get(key);
            for (String subKey : items.keySet()) {
               if (!def.getAvailableConfigurations().containsKey(subKey)) {
                  unRecogConfigKeys.add(subKey);
               }
            }
         } catch (IOException e) {
            unRecogConfigTypes.add(key);
         }
      }
   }

   @Override
   public String toString() {
      Map<String, List<String>> message = new HashMap<String, List<String>>();
      message.put("WarningMsgList", this.warningMsgList);
      message.put("ErrorMsgList", this.errorMsgList);
      return (new Gson()).toJson(message);
   }

   private boolean isComputeOnly(Set<String> definedServices) {
      boolean isComputeOnly = false;
      if (definedServices.contains("ISILON")) {
         isComputeOnly = true;
      }
      return isComputeOnly;
   }
}
