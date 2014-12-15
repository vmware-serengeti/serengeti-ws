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
package com.vmware.bdd.usermgmt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;
import com.vmware.bdd.usermgmt.job.MgmtVmConfigJobService;
import com.vmware.bdd.usermgmt.persist.MgmtVmCfgEao;

/**
 * Created By xiaoliangl on 11/28/14.
 */
@Component
public class MgmtVmCfgService {

   @Autowired
   private MgmtVmCfgEao mgmtVmCfgEao;

   @Autowired
   private MgmtVmConfigJobService mgmtVmConfigJobService;

   @Autowired
   private UserMgmtServerService userMgmtServerService;

   private Map<UserMgmtMode, Map<UserMgmtMode, TransitAction>> allowedTransitions;

   public MgmtVmCfgService(){
      allowedTransitions = new HashMap<>();

      Map<UserMgmtMode, TransitAction> target = new HashMap<>();
      target.put(UserMgmtMode.MIXED, new TransitAction() {
         @Override
         public void perform(Map<String, String> newCfg) {
            MgmtVmCfgService.this.enableLdap(newCfg);
         }
      });

      allowedTransitions.put(UserMgmtMode.LOCAL, Collections.unmodifiableMap(target));

      target = new HashMap<>();
      target.put(UserMgmtMode.LDAP, new TransitAction() {
         @Override
         public void perform(Map<String, String> newCfg) {
            MgmtVmCfgService.this.disableLocalAccount(newCfg);
         }
      });
      allowedTransitions.put(UserMgmtMode.MIXED, Collections.unmodifiableMap(target));

      allowedTransitions = Collections.unmodifiableMap(allowedTransitions);
   }

   public Map<UserMgmtMode, Map<UserMgmtMode, TransitAction>> getAllowedTransitions() {
      return allowedTransitions;
   }


   public void config(Map<String, String> newConfig) {
      Map<String, String> currentCfg = mgmtVmCfgEao.findAll();

      if (newConfig.containsKey(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE)) {
         configUserMgmtService(currentCfg, newConfig);
         mgmtVmCfgEao.update(newConfig);
      } else {
         throw new BddException(null, "MGMTVM_CUM_CFG", "UNSUPPORTED_CFG");
      }
   }

   public Map<String, String> get() {
      return mgmtVmCfgEao.findAll();
   }


   private void configUserMgmtService(Map<String, String> currentCfg, Map<String, String> newCfg) {
      UserMgmtMode currentMode = UserMgmtMode.valueOf(currentCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));
      UserMgmtMode newMode = UserMgmtMode.valueOf(newCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));

      if(currentMode == newMode) {
         throw new BddException(null, "MGMTVM_CUM_CFG", "ALREADY_IN_TARGET_MODE", newMode);
      }

      Map<UserMgmtMode, TransitAction> targets = getAllowedTransitions().get(currentMode);
      if (targets == null) {
         throw new BddException(null, "MGMTVM_CUM_CFG", "MODE_TRANS_NOT_ALLOWED", currentMode, newMode);
      }

      TransitAction targetAction = targets.get(newMode);
      if(targetAction == null) {
         throw new BddException(null, "MGMTVM_CUM_CFG", "MODE_TRANS_NOT_ALLOWED", currentMode, newMode);
      }

      targetAction.perform(newCfg);
   }

   private void disableLocalAccount(Map<String, String> newCfg) {
      mgmtVmConfigJobService.disableLocalAccount();
   }

   private void enableLdap(Map<String, String> newCfg) {
      String userMgmtServerName = newCfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME);

      ValidationErrors errors = new ValidationErrors();
      if(userMgmtServerName == null || userMgmtServerName.length() == 0) {
         ValidationError validationErr = new ValidationError("MGMTVM_CUM_CFG.USER_MGMT_SERVER_NAME_MISSING", "UserMgmtServerName missing");
         errors.addError(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME, validationErr);
      }

      UserMgmtServer userMgmtServer = userMgmtServerService.getByName(userMgmtServerName, false);
      if(userMgmtServer == null) {
         ValidationError validationErr = new ValidationError("MGMTVM_CUM_CFG.NOT_FOUND", "Can't find a server with given UserMgmtServerName.");
         errors.addError(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_SERVERNAME, validationErr);
      }

      if(!errors.getErrors().isEmpty()) {
         throw new ValidationException(errors.getErrors());
      }

      mgmtVmConfigJobService.enableLdap(userMgmtServer);
   }

   public void setMgmtVmCfgEao(MgmtVmCfgEao mgmtVmCfgEao) {
      this.mgmtVmCfgEao = mgmtVmCfgEao;
   }

   public void setMgmtVmConfigJobService(MgmtVmConfigJobService mgmtVmConfigJobService) {
      this.mgmtVmConfigJobService = mgmtVmConfigJobService;
   }

   public void setUserMgmtServerService(UserMgmtServerService userMgmtServerService) {
      this.userMgmtServerService = userMgmtServerService;
   }

}
