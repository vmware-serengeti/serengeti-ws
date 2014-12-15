/*****************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
 ****************************************************************************/
package com.vmware.bdd.cli.commands;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.MgmtVMCfgClient;
import com.vmware.bdd.cli.rest.UserMgmtRestClient;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.TlsHelper;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.validation.ValidationError;

@Component
public class UserMgmtCommands implements CommandMarker {
   @Autowired
   private UserMgmtRestClient userMgmtRestClient;

   @Autowired
   private MgmtVMCfgClient mgmtCfgOnMgmtVMClient;

   @CliAvailabilityIndicator({"usermgmt help"})
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "usermgmt enableldap", help = "Add an AD/LDAP server and enable its selected users to login to management VM.")
   public void enableLdap(
         @CliOption(key = {"cfgfile"}, mandatory = true, help = "The config JSON file path.") final String serverInfoFile) {
      UserMgmtServer userMgmtServer = null;

      try {
         ObjectMapper objectMapper = new ObjectMapper();
         FileReader fr = null;
         try {
            fr = new FileReader(serverInfoFile);
            userMgmtServer = objectMapper.readValue(new BufferedReader(fr), UserMgmtServer.class);
         } catch (FileNotFoundException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_USERMGMT, null,
                  Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
                  "config JSON file not found.");
         } catch (JsonMappingException|JsonParseException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_USERMGMT, null,
                  Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
                  "Failed to parse the JSON file.");
         } catch (IOException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_USERMGMT, null,
                  Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
                  "IO error on reading the JSON file.");
         } finally {
            if(fr != null) {
               try {
                  fr.close();
               } catch (IOException e) {
                  //nothing to do
               }
            }
         }

         if(userMgmtServer == null) {
            return;
         }
         userMgmtServer.setName(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

         boolean continued = false;
         try {
            userMgmtRestClient.addUserMgmtServer(userMgmtServer, false);
            continued = true;
         }catch (UntrustedCertificateException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_USERMGMT, null,
                  Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
                  e.getMessage());

            TlsHelper.presentUserWithCert(e.getCertInfo(), System.out);

            List<String> warningList = new ArrayList<>();
            warningList.add("The LDAP server's certificate is not trusted yet!!");

            if(CommandsUtils.showWarningMsg("",
                  Constants.OUTPUT_OBJECT_USERMGMT, Constants.OUTPUT_OP_ENABLE_LDAP,
                  warningList, false, "Do you want to trust it now(yes/no)?")) {
               userMgmtRestClient.addUserMgmtServer(userMgmtServer, true);
               continued = true;

            } else {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_USERMGMT, null,
                     Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_STOP, "The server certificate is not trusted, so the command will stop.");
            }
         }

         if(continued) {
            try {
               mgmtCfgOnMgmtVMClient.enableLdapOnMgmtVM();
            } catch (Exception e1) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, null,
                     Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
                     "Enable AD/LDAP failed on management VM: " + e1.getMessage());

               System.out.println("try recover the old server configuration.");
               userMgmtRestClient.removeUserMgmtServer(userMgmtServer.getName());
               throw e1;
            }
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_USERMGMT, null, "The AD/LDAP server has been enabled on management VM.");
         }

      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, null,
               Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      } catch (ValidationException e) {
         StringBuilder sbr = new StringBuilder(e.getMessage());

         Map<String, ValidationError> validationErrors = e.getErrors();
         if (validationErrors.size() > 0) {
            sbr.append('\n');
         }

         for (Map.Entry<String, ValidationError> errorEntry : validationErrors.entrySet()) {
            sbr.append("    ").append(errorEntry.getKey()).append(": ").append(errorEntry.getValue().getMessage()).append('\n');
         }

         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_USERMGMT, null,
               Constants.OUTPUT_OP_ENABLE_LDAP, Constants.OUTPUT_OP_RESULT_FAIL,
               sbr.toString());
      }
   }

   @CliCommand(value = "usermgmt disablelocalaccount", help = "disable the local account's login in management VM.")
   public void disableLocalAccount() {
      try {
         mgmtCfgOnMgmtVMClient.disableLocalAccountOnMgmtVM();
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_USERMGMT, null,
               Constants.OUTPUT_OP_RESULT_DELETE);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DATASTORE, null,
               Constants.OUTPUT_OP_DELETE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "usermgmt info", help = "get AD/LDAP server info and management VM's Configuration.")
   public void getInfo() {
      UserMgmtServer userMgmtServer = userMgmtRestClient.get(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);
      ObjectMapper objectMapper = new ObjectMapper();

      System.out.println("AD/LDAP server info: ");
      try {
         System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userMgmtServer));
      } catch (IOException e) {
         e.printStackTrace();
      }

      Map<String, String> mgmtVMcfg = mgmtCfgOnMgmtVMClient.get();

      StringBuilder cfgStrBuilder = new StringBuilder("management VM's configuration: ").append('\n');
      cfgStrBuilder.append("current user management mode: ").append(mgmtVMcfg.get(UserMgmtConstants.VMCONFIG_MGMTVM_CUM_MODE));

      System.out.println(cfgStrBuilder.toString());

   }


}
