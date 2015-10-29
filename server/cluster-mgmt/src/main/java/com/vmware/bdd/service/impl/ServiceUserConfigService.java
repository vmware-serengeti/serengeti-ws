/******************************************************************************
 *   Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.service.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.usermgmt.UserMgmtConstants;

/**
 * Created By xiaoliangl on 2/6/15.
 */
@Component
public class ServiceUserConfigService {
   private final static Logger LOGGER = Logger.getLogger(ClusterLdapUserMgmtCfgService.class);

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   public Map<String, Map<String, String>> getServiceUserConfigs(String clusterName) {
      Map<String, Object> hadoopConfig =
            (new Gson()).fromJson(clusterEntityManager.findByName(clusterName).getHadoopConfig(), Map.class);

      Map<String, Map<String, String>> serviceUserConfigs = hadoopConfig != null ? (Map<String, Map<String, String>>)
            hadoopConfig.get(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE) : null;

      if(MapUtils.isEmpty(serviceUserConfigs)) {
         LOGGER.info("no service user configuration, no need to configure service users for cluster");
      }

      return serviceUserConfigs;
   }

   public Set<String> getServiceUserGroups(Map<String, Map<String, String>> serviceUserConfigs) {
      Set<String> serviceUserGroups = null;

      if (!MapUtils.isEmpty(serviceUserConfigs)) {
         serviceUserGroups = new HashSet<>();
         for (Map<String, String> serviceUserConfig : serviceUserConfigs.values()) {
            serviceUserGroups.add(serviceUserConfig.get(UserMgmtConstants.SERVICE_USER_GROUP));
         }
         LOGGER.info("Service user groups are " + new Gson().toJson(serviceUserGroups));
      }
      return serviceUserGroups;
   }
}
