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

import com.google.gson.Gson;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRoleContainer;
import com.vmware.bdd.plugin.clouderamgr.utils.CmUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      logger.info("Start to validate bludprint for cluster " + blueprint.getName());

      String distro = blueprint.getHadoopStack().getDistro();
      int majorVersion = CmUtils.majorVersionOfHadoopStack(blueprint.getHadoopStack());

      try {
         List<String> unRecogConfigTypes = new ArrayList<String>();
         List<String> unRecogConfigKeys = new ArrayList<>();
         validateConfigs(blueprint.getConfiguration(), unRecogConfigTypes, unRecogConfigKeys, majorVersion);

         Set<String> availableRoles = AvailableServiceRoleContainer.allRoles(majorVersion);

         Set<String> definedServices = new HashSet<String>();
         Map<String, Integer> definedRoles = new HashMap<String, Integer>();

         List<String> unRecogRoles = null;
         Set<String> invalidRacks = null;

         if (blueprint.getNodeGroups() == null || blueprint.getNodeGroups().isEmpty()) {
            return false;
         }

         for (NodeGroupInfo group : blueprint.getNodeGroups()) {
            validateConfigs(group.getConfiguration(), unRecogConfigTypes, unRecogConfigKeys, majorVersion);

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

            if (group.getNodes() != null) {
               for (NodeInfo node : group.getNodes()) {
                  if (node.getRack() != null && !CmUtils.isValidRack(node.getRack())) {
                     if (invalidRacks == null) {
                        invalidRacks = new HashSet<String>();
                     }
                     invalidRacks.add(node.getRack());
                  }
               }
            }
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
            Set<String> requiredRoles = new HashSet<String>();
            switch (serviceName) {
               case "HDFS":
                  requiredRoles.add("HDFS_NAMENODE");
                  requiredRoles.add("HDFS_DATANODE");
                  if (checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList)) {
                     if (definedRoles.get("HDFS_NAMENODE") < 2 && !definedRoles.containsKey("HDFS_SECONDARY_NAMENODE")) { // TODO: how to check HA
                        errorMsgList.add("HDFS service not configured for High Availability must have a SecondaryNameNode");
                     }
                  }
                  break;
               case "YARN":
                  requiredRoles.add("YARN_RESOURCE_MANAGER");
                  requiredRoles.add("YARN_NODE_MANAGER");
                  requiredRoles.add("YARN_JOB_HISTORY");
                  checkRequiredRoles(serviceName, requiredRoles, definedRoles.keySet(), errorMsgList);
                  if (!definedServices.contains("HDFS")) {
                     errorMsgList.add("YARN service depends on HDFS service");
                  }
                  break;
               case "HBASE":
                  if (!definedServices.contains("HDFS")) {
                     errorMsgList.add("HBASE service depends on HDFS service");
                  }
                  if (!definedServices.contains("ZOOKEEPER")) {
                     errorMsgList.add("HBASE service depends on ZOOKEEPER service");
                  }
                  break;
               default:
                  break;
            }

         }

      } catch (IOException e) {
         // IO exception ignored
      }

      if (!warningMsgList.isEmpty() || !errorMsgList.isEmpty()) {
         ValidationException e = new ValidationException(null, null);
         e.getFailedMsgList().addAll(errorMsgList);
         e.getWarningMsgList().addAll(warningMsgList);
         throw e;
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
         List<String> unRecogConfigKeys, int majorVersion) {
      if (config == null || config.isEmpty()) {
         return;
      }

      for (String key : config.keySet()) {
         try {
            AvailableServiceRole def = AvailableServiceRoleContainer.load(key);
            if (!AvailableServiceRoleContainer.isSupported(majorVersion, def)) {
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
}
