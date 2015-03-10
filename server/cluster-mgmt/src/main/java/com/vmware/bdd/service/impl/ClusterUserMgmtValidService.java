/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.usermgmt.SssdConfigurationGenerator;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.usermgmt.UserMgmtServerService;
import com.vmware.bdd.usermgmt.UserMgmtServerValidService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

/**
 * Created By xiaoliangl on 12/30/14.
 */
@Component
public class ClusterUserMgmtValidService {
   private final static Logger LOGGER = Logger.getLogger(ClusterUserMgmtValidService.class);

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   @Autowired
   private UserMgmtServerService userMgmtServerService;

   @Autowired
   private UserMgmtServerValidService userMgmtServerValidService;

   @Autowired
   private SssdConfigurationGenerator sssdConfigurationGenerator;

   public void validateGroups(String[] groupNames) {
      UserMgmtServer userMgmtServer = userMgmtServerService.getByName(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME, false);

      if(userMgmtServer == null) {
         throw new BddException(null, "CLUSTER_LDAP_USER_MGMT", "LDAP_NOT_ENABLED");
      }
      userMgmtServerValidService.searchGroup(userMgmtServer, groupNames);
   }

   public void validateGroupUsers(String userMgmtServerName, Map<String, Set<String>> groupUsers) {
      UserMgmtServer userMgmtServer = userMgmtServerService.getByName(userMgmtServerName, false);
      if(userMgmtServer == null) {
         throw new BddException(null, "CLUSTER_LDAP_USER_MGMT", "LDAP_NOT_ENABLED");
      }
      userMgmtServerValidService.validateGroupUsers(userMgmtServer, groupUsers);
   }

   protected String[] getGroupNames(Map<String, String> userMgmtCfg) {
      if (MapUtils.isEmpty(userMgmtCfg)) {
         return null;
      }
      Set<String> validGroupNameSet = new HashSet<>();

      String adminGroupName = userMgmtCfg.get(UserMgmtConstants.ADMIN_GROUP_NAME);
      if (!CommonUtil.isBlank(adminGroupName)) {
         validGroupNameSet.add(adminGroupName);
      }

      String userGroupName = userMgmtCfg.get(UserMgmtConstants.USER_GROUP_NAME);
      if (!CommonUtil.isBlank(userGroupName)) {
         validGroupNameSet.add(userGroupName);
      }

      String[] groupNames = new String[validGroupNameSet.size()];
      validGroupNameSet.toArray(groupNames);
      return groupNames;
   }

   public void validateUserMgmtConfig(Map<String, String> userMgmtCfg) {
      String[] groupNames = getGroupNames(userMgmtCfg);
      if ((groupNames != null) && (groupNames.length > 0)) {
         LOGGER.info("validate groups: " + Arrays.toString(groupNames));
         validateGroups(groupNames);
         LOGGER.info("groups are validated successfully!");
      }
   }

}
