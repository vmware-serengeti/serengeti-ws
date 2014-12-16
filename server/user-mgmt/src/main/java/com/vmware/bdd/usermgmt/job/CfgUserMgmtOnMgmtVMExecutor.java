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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.usermgmt.SssdLdapConstantMappings;

/**
 * Created By xiaoliangl on 12/12/14.
 */
public class CfgUserMgmtOnMgmtVMExecutor {
   private final static Logger LOGGER = Logger.getLogger(CfgUserMgmtOnMgmtVMExecutor.class);

   private final static int TIMEOUT = Configuration.getInt("usermgmt.command.exec.timeout", 120);

   public void execute(UserMgmtServer userMgmtServer, SssdLdapConstantMappings sssdLdapConstantMappings) {
      File workDir = CommandUtil.createWorkDir(System.currentTimeMillis());
      File specFile = new File(workDir, "enableUserMgmt.json");

      writeJsonFile(userMgmtServer, specFile, sssdLdapConstantMappings);

      String specFilePath = specFile.getAbsolutePath();

      try {
         execChefClient(specFilePath);
         LOGGER.info("execute ChefClient for enable_LDAP is finished.");
      } finally {
         workDir.delete();
         LOGGER.info("enable_LDAP spec file is deleted successful.");
      }
   }

   private void execChefClient(String specFilePath) {
      CommandLine cmdLine = new CommandLine("sudo")
            .addArgument("chef-client")
            .addArgument("-z")
            .addArgument("-j")
            .addArgument("\"" + specFilePath + "\"");

      DefaultExecutor executor = new DefaultExecutor();

      executor.setStreamHandler(new PumpStreamHandler(
                  new ExecOutputLogger(LOGGER, false), //output logger
                  new ExecOutputLogger(LOGGER, true)) //error logger
      );

      executor.setWatchdog(new ExecuteWatchdog(1000l * TIMEOUT));

      try {
         int exitVal = executor.execute(cmdLine);
         if(exitVal != 0) {
            throw new UserMgmtExecException("CFG_LDAP_FAIL", null);
         }
      } catch (IOException e) {
         throw new UserMgmtExecException("CFG_LDAP_FAIL", e);
      }
   }


   class SssdLdapParam {
      private Map<String, String> sssd_ldap = new HashMap<>();
      private List<String> run_list = new ArrayList<>();

      public Map<String, String> getSssd_ldap() {
         return sssd_ldap;
      }

      public void setSssd_ldap(Map<String, String> sssd_ldap) {
         this.sssd_ldap = sssd_ldap;
      }

      public List<String> getRun_list() {
         return run_list;
      }

      public void setRun_list(List<String> run_list) {
         this.run_list = run_list;
      }
   }

   private void writeJsonFile(UserMgmtServer userMgmtServer, File file, SssdLdapConstantMappings sssdLdapConstantMappings) {
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

      BufferedWriter out = null;
      try {
         ObjectMapper objectMapper = new ObjectMapper();
         String json = objectMapper.writeValueAsString(sssdLdapParam);

         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("writing Configuration manifest in json " + json + " to file " + file);
         }

         out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
         out.write(json);
      } catch (IOException ex) {
         LOGGER.error(ex.getMessage() + "\n failed to write enable_LDAP spec file " + file);
         throw new UserMgmtExecException("WRITE_CFG_LDAP_JSON_FAIL", ex);
      } finally {
         if (out != null) {
            try {
               out.close();
            } catch (IOException e) {
               //do nothing
            }
         }
      }
   }
}
