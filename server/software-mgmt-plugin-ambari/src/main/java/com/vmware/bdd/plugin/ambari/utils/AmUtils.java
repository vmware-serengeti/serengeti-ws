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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponents;

public class AmUtils {

   public static List<Map<String, Object>> toAmConfigurations(
         Map<String, Object> configuration) {
      List<Map<String, Object>> configurations =
            new ArrayList<Map<String, Object>>();
      if (configuration != null) {
         for (String configurationType : configuration.keySet()) {
            configurations =
                  toAmConfigurations(configurations, configurationType,
                        (Map<String, String>) configuration
                              .get(configurationType));
         }
      }
      return configurations;
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
      ApiHostComponents components = new ApiHostComponents();
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
}
