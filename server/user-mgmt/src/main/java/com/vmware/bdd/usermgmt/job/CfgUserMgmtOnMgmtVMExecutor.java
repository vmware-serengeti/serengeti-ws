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

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.usermgmt.SssdLdapConstantMappings;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ShellCommandExecutor;

/**
 * Created By xiaoliangl on 12/12/14.
 */
public class CfgUserMgmtOnMgmtVMExecutor {
   private final static Logger LOGGER = Logger.getLogger(CfgUserMgmtOnMgmtVMExecutor.class);

   public void execute(UserMgmtServer userMgmtServer, SssdLdapConstantMappings sssdLdapConstantMappings) {
      File workDir = CommandUtil.createWorkDir(System.currentTimeMillis());
      File specFile = new File(workDir, "CfgUserMgmtServerOnMgmtVMStep.json");

      writeJsonFile(userMgmtServer, specFile, sssdLdapConstantMappings);

      String specFilePath = specFile.getAbsolutePath();

      // Copy node upgrade tarball to node
      String chefCmd = "sudo chef-client -z -j \"" + specFilePath + "\"";
      //@todo handle errors
      ShellCommandExecutor.execCmd(chefCmd, null, null,
            120, "CfgUserMgmtServerOnMgmtVMStep");

      LOGGER.info("CfgUserMgmtOnMgmtVM finished");
      workDir.delete();
   }


   class SssdLdapParam {
      private Map<String, String> sssd_ldap = new HashMap<>();
      private List<String> run_list = new ArrayList<>();
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

      Gson gson = new Gson();
      String json = gson.toJson(sssdLdapParam);
      AuAssert.check(json != null);
      LOGGER.debug("writing Configuration manifest in json " + json + " to file " + file);
      BufferedWriter out = null;
      try {
         out =
               new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                     file), "UTF-8"));
         out.write(json);
      } catch (IOException ex) {
         LOGGER.error(ex.getMessage() + "\n failed to write cluster manifest to file " + file);
         throw BddException.INTERNAL(ex, "Failed to write cluster manifest.");
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
