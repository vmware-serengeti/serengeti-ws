/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiComponentDependency;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiComponentDependencyInfo;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiConfiguration;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiConfigurationInfo;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ComponentCategory;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ComponentName;
import com.vmware.bdd.plugin.ambari.api.model.support.AvailableConfigurationContainer;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.usermgmt.UserMgmtConstants;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class AmClusterValidator {

   private static final Logger logger = Logger
         .getLogger(AmClusterValidator.class);
   private static final Set<String> hdfsRoles = new HashSet<String>(Arrays.asList("NAMENODE", "SECONDARY_NAMENODE", "DATANODE"));
   private List<String> warningMsgList;
   private List<String> errorMsgList;
   private ApiManager apiManager;

   public ApiManager getApiManager() {
      return apiManager;
   }

   public void setApiManager(ApiManager apiManager) {
      this.apiManager = apiManager;
   }

   public AmClusterValidator() {
      this.warningMsgList = new ArrayList<String>();
      this.errorMsgList = new ArrayList<String>();
   }

   public boolean validateBlueprint(ClusterBlueprint blueprint) {
      logger.info("Start to validate blueprint for cluster "
            + blueprint.getName());
      HadoopStack hadoopStack = blueprint.getHadoopStack();
      String distro = hadoopStack.getDistro();
      String stackVendor = hadoopStack.getVendor();
      String stackVersion = hadoopStack.getFullVersion();

      List<String> unRecogConfigTypes = new ArrayList<String>();
      List<String> unRecogConfigKeys = new ArrayList<>();

      validateRoles(blueprint, unRecogConfigTypes, unRecogConfigKeys,
            stackVendor, stackVersion, distro);

      validateRacks(blueprint.getNodeGroups());

      validateConfigs(blueprint.getConfiguration(), unRecogConfigTypes,
            unRecogConfigKeys, stackVendor, stackVersion);

      validateSupportComputeOnly(blueprint);

      if (!unRecogConfigTypes.isEmpty()) {
         errorMsgList.add("Configurations for " + unRecogConfigTypes.toString()
               + " are not available by distro " + distro);
      }

      if (!unRecogConfigKeys.isEmpty()) {
         errorMsgList.add("Configuration items " + unRecogConfigKeys.toString()
               + " are invalid");
      }

      if (!warningMsgList.isEmpty() || !errorMsgList.isEmpty()) {
         throw ValidationException.VALIDATION_FAIL("Blueprint", errorMsgList, warningMsgList);
      }

      return true;
   }

   private void validateRoles(ClusterBlueprint blueprint,
         List<String> unRecogConfigTypes, List<String> unRecogConfigKeys,
         String stackVendor, String stackVersion, String distro) {
      Map<String, Integer> definedRoles = new HashMap<String, Integer>();

      List<String> unRecogRoles = null;

      List<NodeGroupInfo> nodeGroups = blueprint.getNodeGroups();
      if (nodeGroups == null || nodeGroups.isEmpty()) {
         return;
      }

      ApiStackServiceList servicesList =
            apiManager.getStackServiceListWithComponents(stackVendor,
                  stackVersion);

      List<ApiStackComponent> apiStackComponents =
            new ArrayList<ApiStackComponent>();
      for (ApiStackService apiStackService : servicesList.getApiStackServices()) {
         for (ApiStackComponent apiStackComponent : apiStackService
               .getServiceComponents()) {
            apiStackComponents.add(apiStackComponent);
         }
      }

      for (NodeGroupInfo group : nodeGroups) {
         validateConfigs(group.getConfiguration(), unRecogConfigTypes,
               unRecogConfigKeys, stackVendor, stackVersion);

         for (String roleName : group.getRoles()) {
            boolean isSupported = false;
            for (ApiStackComponent apiStackComponent : apiStackComponents) {
               if (roleName.equals(apiStackComponent.getApiComponent()
                     .getComponentName())) {
                  isSupported = true;
                  if (isSupported) {
                     continue;
                  }
               }
            }
            if (!isSupported) {
               if (unRecogRoles == null) {
                  unRecogRoles = new ArrayList<String>();
               }
               unRecogRoles.add(roleName);
               continue;
            } else {
               if (!definedRoles.containsKey(roleName)) {
                  definedRoles.put(roleName, group.getInstanceNum());
               } else {
                  Integer instanceNum =
                        definedRoles.get(roleName) + group.getInstanceNum();
                  definedRoles.put(roleName, instanceNum);
               }
            }
         }
      }

      if (unRecogRoles != null && !unRecogRoles.isEmpty()) {
         errorMsgList.add("Roles " + unRecogRoles.toString()
               + " are not available by distro " + distro);
      }

      validateRoleDependencies(nodeGroups, apiStackComponents, unRecogRoles, blueprint.getExternalNamenode(), blueprint.getExternalDatanodes());

   }

   private void validateRoleDependencies(List<NodeGroupInfo> nodeGroups,
         List<ApiStackComponent> apiStackComponents, List<String> unRecogRoles,
         String externalNamenode, Set<String> externalDatanodes) {
      if (nodeGroups == null || nodeGroups.isEmpty()) {
         return;
      }

      Set<String> allRoles = new HashSet<String>();
      for (NodeGroupInfo group : nodeGroups) {
         allRoles.addAll(group.getRoles());
      }
      for (String role : allRoles) {
         List<String> NotExistDenpendencyNames = new ArrayList<String>();
         for (ApiStackComponent apiStackComponent : apiStackComponents) {

            List<ApiComponentDependency> apiComponentDependencies =
                  apiStackComponent.getApiComponentDependencies();
            if (apiComponentDependencies != null
                  && !apiComponentDependencies.isEmpty()) {
               for (ApiComponentDependency dependency : apiComponentDependencies) {
                  ApiComponentDependencyInfo dependencyInfo =
                        dependency.getApiComponentDependencyInfo();
                  if (role.equals(dependencyInfo.getDependentComponentName())) {
                     String denpendencyName = dependencyInfo.getComponentName();

                     if (!allRoles.contains(denpendencyName)) {
                        NotExistDenpendencyNames.add(denpendencyName);
                     }
                  }
               }
            }

            ApiComponentInfo apiComponentInfo =
                  apiStackComponent.getApiComponent();
            if (role.equals(apiComponentInfo.getComponentName())) {
               Set<String> roleCategoryDependencies =
                     validateRoleCategoryDependencies(apiComponentInfo,
                           allRoles, unRecogRoles, externalNamenode,
                           externalDatanodes);
               if (roleCategoryDependencies != null
                     && !roleCategoryDependencies.isEmpty()) {
                  NotExistDenpendencyNames.addAll(roleCategoryDependencies);
               }
            }

         }
         if (!NotExistDenpendencyNames.isEmpty()) {
            warningMsgList.add("Missing dependency: Component " + role
                  + " depends on " + NotExistDenpendencyNames.toString());
         }
      }
   }

   private Set<String> validateRoleCategoryDependencies(
         ApiComponentInfo apiOriginComponentInfo, Set<String> allRoles,
         List<String> unRecogRoles, String externalNamenode,
         Set<String> externalDatanodes) {
      List<String> masterRoles = new ArrayList<String>();
      List<String> slaveRoles = new ArrayList<String>();
      Set<String> NotExistDenpendencies = new HashSet<String>();
      ComponentCategory componentCategory =
            ComponentCategory.valueOf(apiOriginComponentInfo
                  .getComponentCategory());
      if (componentCategory.isMaster()) {
         return NotExistDenpendencies;
      }

      ApiStackService apiTargetService =
            apiManager.getStackServiceWithComponents(
                  apiOriginComponentInfo.getStackName(),
                  apiOriginComponentInfo.getStackVersion(),
                  apiOriginComponentInfo.getServiceName());
      for (ApiStackComponent apiTargetComponent : apiTargetService
            .getServiceComponents()) {
         ApiComponentInfo apiTargetComponentInfo =
               apiTargetComponent.getApiComponent();
         ComponentCategory targetComponentCategory =
               ComponentCategory.valueOf(apiTargetComponentInfo
                     .getComponentCategory());
         String componentName = apiTargetComponentInfo.getComponentName();
         if (isNamenodeHa(allRoles, unRecogRoles)) {
            if (ComponentName.isSecondaryNamenode(componentName)) {
               continue;
            }
         } else {
            if (ComponentName.isJournalnode(componentName) || ComponentName.isZkfc(componentName)) {
               continue;
            }
         }
         if (targetComponentCategory.isMaster()) {
            masterRoles.add(componentName.toString());
         }
         if (targetComponentCategory.isSlave()) {
            slaveRoles.add(componentName.toString());
         }
      }
      if (componentCategory.isSlave()) {
         for (String masterRole : masterRoles) {
            if (!allRoles.contains(masterRole)) {
               if (!(isComputeOnly(externalNamenode, externalDatanodes) && hdfsRoles.contains(masterRole))) {
                  NotExistDenpendencies.add(masterRole);
               }
            }
         }
      }
      if (componentCategory.isClient()) {
         for (String masterRole : masterRoles) {
            if (!allRoles.contains(masterRole)) {
               if (!(isComputeOnly(externalNamenode, externalDatanodes) && hdfsRoles.contains(masterRole))) {
                  NotExistDenpendencies.add(masterRole);
               }
            }
         }
         for (String slaveRole : slaveRoles) {
            if (!allRoles.contains(slaveRole)) {
               if (!(isComputeOnly(externalNamenode, externalDatanodes) && hdfsRoles.contains(slaveRole))) {
                  NotExistDenpendencies.add(slaveRole);
               }
            }
         }
      }
      return NotExistDenpendencies;
   }

   private boolean isNamenodeHa(Set<String> allRoles, List<String> unRecogRoles) {
      boolean isNamenodeHa = false;
      int nameNodesCount = 0;
      for (String role : allRoles) {
         if (unRecogRoles != null && unRecogRoles.contains(role)) {
            continue;
         }
         if (ComponentName.isNamenode(role)) {
            nameNodesCount++;
         }
      }
      if (nameNodesCount > 1) {
         isNamenodeHa = true;
      }
      return isNamenodeHa;
   }

   private void validateRacks(List<NodeGroupInfo> nodeGroups) {
      Set<String> invalidRacks = null;

      for (NodeGroupInfo group : nodeGroups) {
         if (group.getNodes() != null) {
            for (NodeInfo node : group.getNodes()) {
               if (node.getRack() != null
                     && !AmUtils.isValidRack(node.getRack())) {
                  if (invalidRacks == null) {
                     invalidRacks = new HashSet<String>();
                  }
                  invalidRacks.add(node.getRack());
               }
            }
         }
      }

      if (invalidRacks != null && !invalidRacks.isEmpty()) {
         errorMsgList
               .add("Racks "
                     + invalidRacks.toString()
                     + " are invalid,"
                     + " rack names must be slash-separated, like Unix paths. For example, \"/rack1\" and \"/cabinet3/rack4\"");
      }
   }

   @SuppressWarnings("unchecked")
   private void validateConfigs(Map<String, Object> config,
         List<String> unRecogConfigTypes, List<String> unRecogConfigKeys,
         String stackVendor, String stackVersion) {
      if (config == null || config.isEmpty()) {
         return;
      }

      ApiStackServiceList servicesList =
            apiManager.getStackServiceListWithConfigurations(stackVendor,
                  stackVersion);
      Map<String, Object> supportedConfigs = new HashMap<String, Object>();
      for (ApiStackService apiStackService : servicesList.getApiStackServices()) {
         for (ApiConfiguration apiConfiguration : apiStackService
               .getApiConfigurations()) {
            ApiConfigurationInfo apiConfigurationInfo =
                  apiConfiguration.getApiConfigurationInfo();
            String configType = apiConfigurationInfo.getType();
            String propertyName = apiConfigurationInfo.getPropertyName();
            Set<String> propertyNames;

            if (supportedConfigs.containsKey(configType)) {
               propertyNames = (Set<String>) supportedConfigs.get(configType);
            } else {
               propertyNames = new HashSet<String>();
            }
            propertyNames.add(propertyName);
            supportedConfigs.put(configType, propertyNames);
         }
      }

      // customized configurations
      try {
         Map<String, Object> customizedConfigs = AvailableConfigurationContainer.getSupportedConfigs(stackVersion);
         for (String configType : customizedConfigs.keySet()) {
            if (supportedConfigs.keySet().contains(configType)) {
               Set<String> propertyNames = (Set<String>) supportedConfigs.get(configType);
               propertyNames.addAll((Set<String>) customizedConfigs.get(configType));
            } else {
               supportedConfigs.put(configType, customizedConfigs.get(configType));
            }
         }
      } catch (IOException erorr) {
         errorMsgList.add("Read customized configurations failed. Please check it on conf directory of ambari. ERROR: " + erorr.getMessage());
      }

      Map<String, Object> notAvailableConfig = new HashMap<String, Object>();
      for (String key : config.keySet()) {
         //bypass the service_user config as we already validated it
         if (key.equals(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE)) {
            Map<String, Map<String, String>> configs = (Map<String, Map<String, String>>)config.get(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE);
            List<Map<String, String>> configList = new ArrayList<>(configs.values());
            validateServiceUserConfigs(configList, errorMsgList);
            continue;
         }

         boolean isSupportedType = false;
         for (String configType : supportedConfigs.keySet()) {
        	if (configType.equals(key + ".xml")) {
               isSupportedType = true;
               if (isSupportedType) {
                  continue;
               }
            }
         }
         if (!isSupportedType) {
        	unRecogConfigTypes.add(key);
         }

         try {
            Map<String, String> items = (Map<String, String>) config.get(key);
            for (String subKey : items.keySet()) {
               boolean isSupportedPropety = false;
               for (String propertyName : (Set<String>) supportedConfigs
                     .get(key + ".xml")) {
                  if (propertyName.equals(subKey)) {
                     isSupportedPropety = true;
                     if (isSupportedPropety) {
                        continue;
                     }
                  }
               }
               if (!isSupportedPropety) {
                  unRecogConfigKeys.add(subKey);
               }
            }
         } catch (Exception e) {
            notAvailableConfig.put(key, config.get(key));
         }
      }
   }

   private void validateServiceUserConfigs(List<Map<String, String>>configs, List<String> errorMsgList) {
      for (Map<String, String> config: configs) {
         for (String key: config.keySet()) {
            if (!key.equalsIgnoreCase(UserMgmtConstants.SERVICE_USER_NAME) &&
                  !key.equalsIgnoreCase(UserMgmtConstants.SERVICE_USER_TYPE)) {
               errorMsgList.add("Service user config doesn't support key: " + key);
            }
            if (key.equalsIgnoreCase(UserMgmtConstants.SERVICE_USER_TYPE) &&
                  !config.get(key).equalsIgnoreCase("LOCAL")) {
               errorMsgList.add("You must use local user to customize service user in Ambari deployed cluster");
            }
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

   private boolean isComputeOnly(String externalNamenode, Set<String> externalDatanodes) {
      boolean isComputeOnly = false;
      if (externalNamenode != null && externalDatanodes != null) {
         isComputeOnly = true;
      }
      return isComputeOnly;
   }

   private void validateSupportComputeOnly(ClusterBlueprint blueprint) {
      if (isComputeOnly(blueprint.getExternalNamenode(), blueprint.getExternalDatanodes())) {
         String hdpVersion = blueprint.getHadoopStack().getFullVersion();
         DefaultArtifactVersion hdpVersionInfo = new DefaultArtifactVersion(hdpVersion);
         if (!((hdpVersionInfo.getMajorVersion() >= 2 && hdpVersionInfo.getMinorVersion() >= 1) || hdpVersionInfo.getMajorVersion() > 2)) {
            warningMsgList.add("Compute only cluster does not support in HDP " + hdpVersion +" of Ambari yet");
         }
      }
   }

}
