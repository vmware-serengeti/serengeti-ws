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
package com.vmware.bdd.plugin.ambari.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponentsRequest;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

public class AmUtils {
   public static List<Map<String, Object>> toAmConfigurations(
         Map<String, Object> configuration) {
      List<Map<String, Object>> configurations =
            new ArrayList<Map<String, Object>>();
      //set service user to configuration
      updateServiceUserConfigInConfiguration(configuration);
      if (configuration != null) {
         for (String configurationType : configuration.keySet()) {
            Map<String, String> properties = (Map<String, String>) configuration.get(configurationType);
            if (!properties.keySet().isEmpty()) {
               configurations = toAmConfigurations(configurations, configurationType, properties);
            }
         }
      }
      return configurations;
   }

   private static void updateServiceUserConfigInConfiguration(Map<String, Object> configuration) {
      if (configuration == null) {
         return;
      }
      Map<String, Map<String, String>> serviceUserConfigs = (Map<String, Map<String, String>>)
            configuration.get(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE);
      if (MapUtils.isEmpty(serviceUserConfigs)) {
         return;
      }
      for (Map.Entry<String, Map<String, String>> serviceUserConfig: serviceUserConfigs.entrySet()) {
         String serviceUser = serviceUserConfig.getValue().get(UserMgmtConstants.SERVICE_USER_NAME);
         if (!StringUtils.isBlank(serviceUser)) {
            String serviceUserParentConfigName = serviceName2EnvName(serviceUserConfig.getKey());
            String serviceUserConfigName = serviceUserConfig.getKey().toLowerCase() + "_user";
            Map<String, String> serviceConfig = (Map<String, String>) configuration.get(serviceUserParentConfigName);
            if (serviceConfig == null) {
               serviceConfig = new HashMap<>();
            }
            serviceConfig.put(serviceUserConfigName, serviceUser);
            configuration.put(serviceUserParentConfigName, serviceConfig);

            //for HDFS/HBASE we also need to change related field in xml files
            //Reference: http://docs.hortonworks.com/HDPDocuments/HDP1/HDP-1.3.3/bk_using_Ambari_book/content/ambari-chap3-7-1_2x.html
            switch (serviceUserConfig.getKey()) {
               case "HDFS":
                  updateConfigInConfiguration(configuration, "hdfs-site", "dfs.permissions.superusergroup", serviceUser);
                  //the administrators config need to have a whitespace in front of the service user
                  updateConfigInConfiguration(configuration, "hdfs-site", "dfs.cluster.administrators", " " + serviceUser);
                  break;
               case "HBASE":
                  updateConfigInConfiguration(configuration, "", "dfs.block.local-path-access.user", serviceUser);
                  break;
            }
         }
      }
      configuration.remove(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE);
   }

   private static String serviceName2EnvName(String serviceName) {
      String envName;
      if (serviceName.equals("HDFS")) {
         envName = "hadoop" + "-env";
      } else if (serviceName.equals("MAPREDUCE2")) {
         envName = "mapred" + "-env";
      } else {
         envName = serviceName.toLowerCase() + "-env";
      }
      return envName;
   }

   private static void updateConfigInConfiguration(Map<String, Object> configuration, String configType, String key, String value) {
      Map<String, String> conf = (Map<String, String>) configuration.get(configType);
      if (conf == null) {
         conf = new HashMap<>();
         configuration.put(configType, conf);
      }
      conf.put(key, value);
   }

   public static List<Map<String, Object>> toAmConfigurations(
         List<Map<String, Object>> configurations, String configurationType,
         Map<String, String> property) {
      if (configurations == null) {
         configurations = new ArrayList<Map<String, Object>>();
      }
      Map<String, Object> configuration = new HashMap<String, Object>();
      configuration.put(configurationType, property);
      if (configurations.isEmpty()) {
         configurations.add(configuration);
      } else {
         boolean isContainsKey = false;
         for (Map<String, Object> nodeConfiguration : configurations) {
            if (nodeConfiguration.containsKey(configurationType)) {
               Map<String, String> properties =
                     (Map<String, String>) nodeConfiguration
                           .get(configurationType);
               properties.putAll(property);
               isContainsKey = true;
            }
         }
         if (!isContainsKey) {
            configurations.add(configuration);
         }
      }
      return configurations;
   }

   public static boolean isValidRack(String rack) {
      Pattern rackPattern = Pattern.compile("(/[a-zA-Z0-9\\.\\-\\_]+)+");
      return rackPattern.matcher(rack).matches();
   }

   public static ApiHostsRequest createInstallComponentsRequest() {
      ApiHostsRequest hostsRequest = new ApiHostsRequest();
      ApiHostComponentsRequest components = new ApiHostComponentsRequest();
      hostsRequest.setBody(components);
      ApiComponentInfo hostRoles = new ApiComponentInfo();
      hostRoles.setState("INSTALLED");
      components.setHostRoles(hostRoles);
      ApiHostsRequestInfo requestInfo = new ApiHostsRequestInfo();
      hostsRequest.setRequestInfo(requestInfo);
      requestInfo.setContext("Installing components");
      requestInfo.setQueryString("HostRoles/state=INIT");
      return hostsRequest;
   }

   public static String getConfDir() {
      return com.vmware.bdd.utils.CommonUtil.getConfDir() + File.separator + Constants.AMBARI_PLUGIN_NAME;
   }
}
