/*****************************************************************************
 *   Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

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
import com.vmware.bdd.cli.rest.UserMgmtServerRestClient;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.TlsHelper;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.usermgmt.UserMgmtMode;
import com.vmware.bdd.validation.ValidationErrorsStringBuilder;

@Component
public class UserMgmtServerCommands implements CommandMarker {
   @Autowired
   private UserMgmtServerRestClient userMgmtServerRestClient;

   @Autowired
   private MgmtVMCfgClient mgmtCfgOnMgmtVMClient;

   @CliAvailabilityIndicator({"usermgmtserver help"})
   public boolean isCommandAvailable() {
      return true;
   }


   @CliCommand(value = "usermgmtserver get", help = "get the default AD/LDAP server info.")
   public void getUserMgmtServer(
   ) {
      try {
         UserMgmtServer userMgmtServer = userMgmtServerRestClient.get(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

         if(userMgmtServer == null) {
            System.out.println("The AD/LDAP server is not added.");
            return;
         }


         LinkedHashMap<String, List<String>> distroColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         distroColumnNamesWithGetMethodNames.put(
               "TYPE", Arrays.asList("getType"));
         distroColumnNamesWithGetMethodNames
               .put("PRIMARY URL", Arrays.asList("getPrimaryUrl"));
         distroColumnNamesWithGetMethodNames
               .put("LOGIN", Arrays.asList("getUserName"));
         distroColumnNamesWithGetMethodNames
               .put("PASSWORD", Arrays.asList("getPassword"));
         distroColumnNamesWithGetMethodNames.put(
               "GROUP SEARCH BASE", Arrays.asList("getBaseGroupDn"));
         distroColumnNamesWithGetMethodNames.put(
               "USER SEARCH BASE", Arrays.asList("getBaseUserDn"));
         distroColumnNamesWithGetMethodNames.put(
               "MANAGEMENT VM USER GROUP", Arrays.asList("getMgmtVMUserGroupDn"));

         userMgmtServer.setPassword("******");
         try {
            CommandsUtils.printInTableFormat(
                  distroColumnNamesWithGetMethodNames, new Object[]{userMgmtServer},
                  Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            CommandOutputHelper.GET_LDAP_OUTPUT.printFailure(e);
         }
      } catch (CliRestException e) {
         CommandOutputHelper.GET_LDAP_OUTPUT.printFailure(e);
      }
   }

   @CliCommand(value = "usermgmtserver modify", help = "Change the default AD/LDAP server info.")
   public void modifyUserMgmtServer(
         @CliOption(key = {"cfgfile"}, mandatory = true, help = "The AD/LDAP server configuration JSON file path.") final String cfgFilePath,
         @CliOption(key = {"trustCertificate"}, mandatory = false, help = "Trust the server certificate.") final Boolean trustCertificateFlag) {

      final UserMgmtServer userMgmtServer = parseUserMgmtServer(cfgFilePath, CommandOutputHelper.MODIFY_LDAP_OUTPUT);
      if (userMgmtServer == null) {
         return;
      }
      userMgmtServer.setName(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

      try {
         try {
            userMgmtServerRestClient.modifyUserMgmtServer(userMgmtServer,
                  trustCertificateFlag == null ? false : trustCertificateFlag);
            CommandOutputHelper.MODIFY_LDAP_OUTPUT.printSuccess();
         } catch (UntrustedCertificateException e) {
            if (handleUntrustedCertificate(e, CommandOutputHelper.MODIFY_LDAP_OUTPUT,
                  new Runnable() {
                     @Override
                     public void run() {
                        userMgmtServerRestClient.modifyUserMgmtServer(userMgmtServer, true);
                     }
                  })) {
               CommandOutputHelper.MODIFY_LDAP_OUTPUT.printSuccess();
            }
         }
      } catch (CliRestException e) {
         CommandOutputHelper.MODIFY_LDAP_OUTPUT.printFailure(e.getMessage());
      } catch (ValidationException e) {
         handleValidationErrors(e, CommandOutputHelper.MODIFY_LDAP_OUTPUT);
      }
   }

   @CliCommand(value = "usermgmtserver add", help = "Add an AD/LDAP server as the default user management server for VMs.")
   public void addUserMgmtServer(
         @CliOption(key = {"cfgfile"}, mandatory = true, help = "The AD/LDAP server configuration JSON file path.") final String cfgFilePath,
         @CliOption(key = {"trustCertificate"}, mandatory = false, help = "Trust the server certificate.") final Boolean trustCertificateFlag
         /*,@CliOption(key = {"enableOnMgmtVM"}, mandatory = true, help = "Enable the new AD/LDAP server on management VM.") final boolean enableOnMgmtVM*/) {
      try {
         final UserMgmtServer userMgmtServer = parseUserMgmtServer(cfgFilePath, CommandOutputHelper.MODIFY_LDAP_OUTPUT);

         if (userMgmtServer == null) {
            return;
         }
         userMgmtServer.setName(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME);

         boolean continued = false;
         try {
            userMgmtServerRestClient.addUserMgmtServer(userMgmtServer,
                  trustCertificateFlag == null ? false : trustCertificateFlag);
            continued = true;
         } catch (UntrustedCertificateException e) {
            continued = handleUntrustedCertificate(e, CommandOutputHelper.ADD_LDAP_OUTPUT,
                  new Runnable() {
                     @Override
                     public void run() {
                        userMgmtServerRestClient.addUserMgmtServer(userMgmtServer, true);
                     }
                  });
         }

         if (continued) {
            try {
               mgmtCfgOnMgmtVMClient.config(UserMgmtMode.MIXED.name());
            } catch (Exception e1) {
               CommandOutputHelper.ADD_LDAP_OUTPUT.printFailure(e1);
               System.out.println("Try to recover the old server configuration.");
               userMgmtServerRestClient.removeUserMgmtServer(userMgmtServer.getName());
               System.out.println("Recover successfully.");
               throw e1;
            }

            CommandOutputHelper.ADD_LDAP_OUTPUT.printSuccess();
         }

      } catch (CliRestException e) {
         CommandOutputHelper.ADD_LDAP_OUTPUT.printFailure(e);
      } catch (ValidationException e) {
         handleValidationErrors(e, CommandOutputHelper.ADD_LDAP_OUTPUT);
      }
   }

   private boolean handleUntrustedCertificate(UntrustedCertificateException e,
                                              CommandOutputHelper commandOutputHelper,
                                              Runnable runnable) {
      commandOutputHelper.printWarning(e.getMessage());

      TlsHelper.presentUserWithCert(e.getCertInfo(), System.out);

      List<String> warningList = new ArrayList<>();
      warningList.add("The LDAP server's certificate is not trusted yet!!");

      boolean userConfirmed = commandOutputHelper.promptWarning(
            warningList, false, "Do you want to trust it now(yes/no)?");

      if (userConfirmed) {
         runnable.run();
      } else {
         commandOutputHelper.printFailure("The server certificate is not trusted, so the command will stop.");
      }

      return userConfirmed;
   }

   private void handleValidationErrors(ValidationException validationEx, CommandOutputHelper outputHelper) {
      outputHelper.printFailure(validationEx);

      ValidationErrorsStringBuilder stringBuilder = new ValidationErrorsStringBuilder();

      System.out.println(stringBuilder.toString(validationEx.getErrors()));
   }

   private UserMgmtServer parseUserMgmtServer(String cfgFilePath, CommandOutputHelper MODIFY_LDAP_OUTPUT) {
      ObjectMapper objectMapper = new ObjectMapper();
      FileReader fr = null;
      try {
         fr = new FileReader(cfgFilePath);
         return objectMapper.readValue(new BufferedReader(fr), UserMgmtServer.class);
      } catch (FileNotFoundException e) {
         MODIFY_LDAP_OUTPUT.printFailure("File " +  cfgFilePath + " not found.");
      } catch (JsonMappingException | JsonParseException e) {
         MODIFY_LDAP_OUTPUT.printFailure("Failed to parse file " + cfgFilePath);
      } catch (IOException e) {
         MODIFY_LDAP_OUTPUT.printFailure("IO error on reading file " + cfgFilePath);
      } finally {
         if (fr != null) {
            try {
               fr.close();
            } catch (IOException e) {
               //nothing to do
            }
         }
      }

      return null;
   }
}
