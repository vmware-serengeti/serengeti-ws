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
package com.vmware.bdd.usermgmt.job;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.usermgmt.SssdConfigurationGenerator;

/**
 * Created By xiaoliangl on 11/28/14.
 */
@Component
public class MgmtVmConfigJobService {
   private final static Logger LOGGER = Logger.getLogger(MgmtVmConfigJobService.class);

   @Autowired
   private SssdConfigurationGenerator sssdConfigurationGenerator;

   public void enableLdap(UserMgmtServer usrMgmtServer) {
      CfgUserMgmtOnMgmtVMExecutor cfgExecutor = new CfgUserMgmtOnMgmtVMExecutor();
      cfgExecutor.execute(usrMgmtServer, sssdConfigurationGenerator);
   }

   public void changeLocalAccountState(boolean enabled) {
      ChangeLocalAccountStateExecutor executor = new ChangeLocalAccountStateExecutor();
      executor.execute(enabled);
   }
}
