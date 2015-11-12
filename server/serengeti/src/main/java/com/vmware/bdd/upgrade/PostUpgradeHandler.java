/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.upgrade;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.usermgmt.MgmtVmCfgService;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.usermgmt.UserMgmtMode;
import com.vmware.bdd.usermgmt.UserMgmtServerService;
import com.vmware.bdd.usermgmt.job.MgmtVmConfigJobService;

public class PostUpgradeHandler {
   private static final Logger logger =
         Logger.getLogger(PostUpgradeHandler.class);

   @Autowired
   private MgmtVmCfgService mgmtVmCfgService;
   @Autowired
   private UserMgmtServerService userMgmtServerService;
   @Autowired
   private MgmtVmConfigJobService mgmtVmConfigJobService;

   public void handlePostUpgrade() {
      logger.info("start to process the post upgrade");
      handlePostUpgradeForUsrMgmt();
   }

   public void handlePostUpgradeForUsrMgmt() {
      logger.info("start to process the post upgrade for user management");
      Map<String, String> mvcMap = mgmtVmCfgService.get();
      if ( mvcMap != null ) {
         String mgmtMode = mvcMap.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE);
         String userMgmtServerName = mvcMap.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME);
         logger.info("management server mode: " + mgmtMode);
         logger.info("management server name: " + userMgmtServerName);
         if ( mgmtMode.equals(UserMgmtMode.LOCAL.name()) ) {
            // the LDAP is not enabled on the source bde server
            return;
         }

         // the mode is MIXED or LDAP_ONLY, so enable the LDAP now
         logger.info("For MIXED or LDAP_ONLY mode, we need to enable LDAP on the upgraded bde server");
         UserMgmtServer userMgmtServer = userMgmtServerService.getByName(userMgmtServerName, false);
         if (userMgmtServer != null) {
            mgmtVmConfigJobService.enableLdap(userMgmtServer);
         }

         // for mode LDAP_ONLY, we need disable local accounts
         if ( mgmtMode.equals(UserMgmtMode.LDAP.name()) ) {
            logger.info("For LDAP_ONLY mode, we need to disable the local accounts");
            mgmtVmConfigJobService.changeLocalAccountState(false);
         }
      }
   }
}
