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
package com.vmware.bdd.software.mgmt.plugin.utils;

import com.google.gson.Gson;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by qjin on 12/3/14.
 */
public final class ValidateRolesUtil {
   private static Logger logger = Logger.getLogger(ValidateRolesUtil.class);

   public static void validateRolesForShrink(String appManagerConfPath, NodeGroupInfo groupInfo) {
      String blacklistJson;
      String filePath = appManagerConfPath + File.separator + Constants.rolesBlacklistForShrink;
      try {
         blacklistJson = SerialUtils.dataFromFile(filePath);
      } catch (IOException e) {
         String errMsg = appManagerConfPath + ": "  + e.getMessage();
         throw SoftwareManagementPluginException.READ_BLACKLIST_FOR_SCALE_IN_FAILED(errMsg);
      }
      Gson gson = new Gson();
      Set<String> blacklist = new HashSet(gson.fromJson(blacklistJson, List.class));
      logger.info("roles in blackList are: " + blacklist.toString());

      List<String> roles = groupInfo.getRoles();
      List<String> invalidRoles = null;
      for (String role : roles) {
         if (blacklist.contains(role)) {
            if (invalidRoles == null) {
               invalidRoles = new ArrayList<String>();
            }
            invalidRoles.add(role);
         }
      }
      if (invalidRoles != null) {
         throw SoftwareManagementPluginException.INVALID_ROLES_TO_SHRINK(invalidRoles.toString());
      }
   }
}
