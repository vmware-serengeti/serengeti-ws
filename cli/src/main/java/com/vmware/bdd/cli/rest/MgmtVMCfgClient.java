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
package com.vmware.bdd.cli.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.usermgmt.UserMgmtMode;

/**
 * Created By xiaoliangl on 12/15/14.
 */
@Component
public class MgmtVMCfgClient {
   private final static String mgmtVMCfgURL = "vmconfig/mgmtvm";

   @Autowired
   private RestClient restClient;

   public void enableLdapOnMgmtVM() {
      Map<String, Object> mgmtVMcfg = new HashMap<>();
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE, UserMgmtMode.MIXED);
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME, UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

      restClient.update(mgmtVMcfg, mgmtVMCfgURL, HttpMethod.PUT);
   }

   public void disableLocalAccountOnMgmtVM() {
      Map<String, Object> mgmtVMcfg = new HashMap<>();
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE, UserMgmtMode.LDAP);
      mgmtVMcfg.put(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME, UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

      restClient.update(mgmtVMcfg, mgmtVMCfgURL, HttpMethod.PUT);
   }

   public Map<String, String> get() {
      return restClient.getObject("", Map.class, mgmtVMCfgURL, HttpMethod.GET, false);
   }
}
