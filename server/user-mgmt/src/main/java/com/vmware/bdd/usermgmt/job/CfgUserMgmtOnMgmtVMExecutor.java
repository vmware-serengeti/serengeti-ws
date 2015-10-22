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
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.usermgmt.SssdConfigurationGenerator;

/**
 * Created By xiaoliangl on 12/12/14.
 */
public class CfgUserMgmtOnMgmtVMExecutor {
   private final static Logger LOGGER = Logger.getLogger(CfgUserMgmtOnMgmtVMExecutor.class);

   private final static int TIMEOUT = Configuration.getInt("usermgmt.command.exec.timeout", 120);
   private final String sudoCmd = CommonUtil.getCustomizedSudoCmd();

   public void execute(UserMgmtServer userMgmtServer, SssdConfigurationGenerator sssdLdapConstantMappings) {

      String taskDir = System.getProperty("serengeti.home.dir") + File.separator + "logs" + File.separator + "task";

      File workDir = new File(taskDir + File.separator + System.currentTimeMillis());
      workDir.mkdirs();
      File specFile = new File(workDir, "enableUserMgmt.json");

      writeJsonFile(userMgmtServer, specFile, sssdLdapConstantMappings);

      String specFilePath = specFile.getAbsolutePath();

      try {
         execChefClient(specFilePath);
         enableSudo(userMgmtServer.findAdminGroupName());
         LOGGER.info("execute ChefClient for enable_LDAP is finished.");
      } finally {
         workDir.delete();
         LOGGER.info("enable_LDAP spec file is deleted successful.");
      }
   }

   private void execCommand(CommandLine cmdLine) {
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

   private void enableSudo(String adminGroupName) {
      CommandLine cmdLine = new CommandLine(sudoCmd)
            .addArgument(UserMgmtConstants.ENABLE_SUDO_SCRIPT)
            .addArgument(adminGroupName);
      execCommand(cmdLine);
   }

   private void execChefClient(String specFilePath) {
      CommandLine cmdLine = new CommandLine(sudoCmd)
            .addArgument("chef-client")
            .addArgument("-z")
            .addArgument("-j")
            .addArgument("\"" + specFilePath + "\"")
            .addArgument("-c")
            .addArgument("/opt/serengeti/.chef/knife.rb");

      execCommand(cmdLine);
   }


   class SssdLdapParam {
      private Map<Object, Object> sssd_ldap = new HashMap<>();
      private List<String> run_list = new ArrayList<>();

      public Map<Object, Object> getSssd_ldap() {
         return sssd_ldap;
      }
      public List<String> getRun_list() {
         return run_list;
      }
   }

   private void writeJsonFile(UserMgmtServer userMgmtServer, File file, SssdConfigurationGenerator sssdLdapConstantMappings) {
      SssdLdapParam sssdLdapParam = new SssdLdapParam();

      //initialize by template
      sssdLdapParam.sssd_ldap.putAll(sssdLdapConstantMappings.get(userMgmtServer.getType()));

      //override values.
      sssdLdapParam.sssd_ldap.put("ldap_group_search_base", userMgmtServer.getBaseGroupDn());
      sssdLdapParam.sssd_ldap.put("ldap_user_search_base", userMgmtServer.getBaseUserDn());
      sssdLdapParam.sssd_ldap.put("ldap_uri", userMgmtServer.getPrimaryUrl());
      sssdLdapParam.sssd_ldap.put("ldap_default_bind_dn", userMgmtServer.getUserName());
      sssdLdapParam.sssd_ldap.put("ldap_default_authtok", userMgmtServer.getPassword());
      sssdLdapParam.sssd_ldap.put("ldap_access_filter", "memberOf=" + userMgmtServer.getMgmtVMUserGroupDn());

      sssdLdapParam.run_list.add("recipe[sssd_ldap]");


      try {
         ObjectMapper objectMapper = new ObjectMapper();
         String json = objectMapper.writeValueAsString(sssdLdapParam);

         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("writing Configuration manifest in json " + json + " to file " + file);
         }

         try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));) {
            out.write(json);
         }
      } catch (IOException ex) {
         LOGGER.error(ex.getMessage() + "\n failed to write enable_LDAP spec file " + file);
         throw new UserMgmtExecException("WRITE_CFG_LDAP_JSON_FAIL", ex);
      }
   }
}
