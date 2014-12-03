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
package com.vmware.bdd.usermgmt.job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.service.ISoftwareSyncUpService;
import com.vmware.bdd.service.job.DefaultStatusUpdater;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ShellCommandExecutor;
import com.vmware.bdd.usermgmt.SssdLdapConstantMappings;

public class CfgUserMgmtServerOnMgmtVMStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(CfgUserMgmtServerOnMgmtVMStep.class);
   private ISoftwareSyncUpService serviceSyncup;

   @Autowired
   private SssdLdapConstantMappings sssdLdapConstantMappings;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
                                   JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String userMgmtServerJson = getJobParameters(chunkContext).getString("UserMgmtServer");
      Gson gson = new Gson();
      UserMgmtServer userMgmtServer = gson.fromJson(userMgmtServerJson, UserMgmtServer.class);


      String jobName = chunkContext.getStepContext().getJobName();
      logger.info("target : " + "MgmtVM" + ", operation: "
            + "ConfigureUserMgmtServer" + ", jobname: " + jobName);

      File workDir = CommandUtil.createWorkDir(getJobExecutionId(chunkContext));
      File specFile = new File(workDir, "CfgUserMgmtServerOnMgmtVMStep.json");

      writeJsonFile(userMgmtServer, specFile);

      String specFilePath = specFile.getAbsolutePath();


      StatusUpdater statusUpdater =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));

      // Copy node upgrade tarball to node
      String chefCmd = "sudo chef-client -z -j \"" + specFilePath + "\"";
      //@todo handle errors
      ShellCommandExecutor.execCmd(chefCmd, null, null,
            120, "CfgUserMgmtServerOnMgmtVMStep");



         /*if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.CURRENT_ERROR_MESSAGE, errorMessage);
            putIntoJobExecutionContext(chunkContext,
                  JobConstants.SOFTWARE_MANAGEMENT_STEP_FAILE, true);
            throw TaskException.EXECUTION_FAILED(errorMessage);
         }*/

      return RepeatStatus.FINISHED;
   }


   class SssdLdapParam {
      private Map<String, String> sssd_ldap = new HashMap<>();
      private List<String> run_list = new ArrayList<>();
   }

   private void writeJsonFile(UserMgmtServer userMgmtServer, File file) {
      SssdLdapParam sssdLdapParam = new SssdLdapParam();

      SssdLdapConstantMappings.Getter constantGetter = sssdLdapConstantMappings.getterFirst(userMgmtServer.getType());

      sssdLdapParam.sssd_ldap.put(SssdLdapConstantMappings.LDAP_SCHEMA, constantGetter.get(SssdLdapConstantMappings.LDAP_SCHEMA));

      sssdLdapParam.sssd_ldap.put(SssdLdapConstantMappings.LDAP_USER_OBJECT_CLASS, constantGetter.get(SssdLdapConstantMappings.LDAP_USER_OBJECT_CLASS));
      sssdLdapParam.sssd_ldap.put(SssdLdapConstantMappings.LDAP_USER_NAME, constantGetter.get(SssdLdapConstantMappings.LDAP_USER_NAME));
      sssdLdapParam.sssd_ldap.put(SssdLdapConstantMappings.LDAP_GROUP_OBJECT_CLASS, constantGetter.get(SssdLdapConstantMappings.LDAP_GROUP_OBJECT_CLASS));

      sssdLdapParam.sssd_ldap.put("ldap_tls_reqcert", "never");

      sssdLdapParam.sssd_ldap.put("ldap_group_search_base", userMgmtServer.getBaseGroupDn());
      sssdLdapParam.sssd_ldap.put("ldap_user_search_base", userMgmtServer.getBaseUserDn());
      sssdLdapParam.sssd_ldap.put("ldap_uri", userMgmtServer.getPrimaryUrl());
      sssdLdapParam.sssd_ldap.put("ldap_default_bind_dn", userMgmtServer.getUserName());
      sssdLdapParam.sssd_ldap.put("ldap_default_authtok", userMgmtServer.getPassword());

      //enable auth by groupdn
      sssdLdapParam.sssd_ldap.put("access_provider", "ldap");
      sssdLdapParam.sssd_ldap.put("ldap_access_filter", "memberOf=" + userMgmtServer.getMgmtVMUserGroupDn());

      sssdLdapParam.run_list.add("recipe[sssd_ldap]");

      Gson gson = new Gson();
      String json = gson.toJson(sssdLdapParam);
      AuAssert.check(json != null);
      logger.debug("writing Configuration manifest in json " + json + " to file "
            + file);
      BufferedWriter out = null;
      try {
         out =
               new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                     file), "UTF-8"));
         out.write(json);
      } catch (IOException ex) {
         logger.error(ex.getMessage()
               + "\n failed to write cluster manifest to file " + file);
         throw BddException.INTERNAL(ex, "Failed to write cluster manifest.");
      } finally {
         if (out != null) {
            try {
               out.close();
            } catch (IOException e) {
               logger.error("falied to close writer" + out, e);
            }
         }
      }
   }

}
