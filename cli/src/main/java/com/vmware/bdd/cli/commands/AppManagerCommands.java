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

package com.vmware.bdd.cli.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jline.console.ConsoleReader;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.cli.rest.AppManagerRestClient;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.utils.CommonUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:18 PM
 */
@Component
public class AppManagerCommands implements CommandMarker {

   @Autowired
   private AppManagerRestClient restClient;

   @CliAvailabilityIndicator({ "appmanager help" })
   public boolean isCommandAvailable() {
      return true;
   }

   /*@CliCommand(value = "appmanager types", help = "List all App Manager types")
   public void getAppManagerTypes() {
      try {
         String[] types = restClient.getTypes();
         for (String type : types) {
            System.out.println(type);
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               "", Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }*/

   @CliCommand(value = "appmanager add", help = "Add an App Manager instance")
   public void addAppManager(
         @CliOption(key = { "name" }, mandatory = true, help = "The instance name") final String name,
         @CliOption(key = { "description" }, mandatory = false, help = "The instance description") final String description,
         @CliOption(key = { "type" }, mandatory = true, help = "The provider type, ClouderaManager or Ambari") final String type,
         @CliOption(key = { "url" }, mandatory = true, help = "The instance URL, e.g. http://hostname:port") final String url) {

      AppManagerAdd appManagerAdd = new AppManagerAdd();
      appManagerAdd.setName(name);
      appManagerAdd.setDescription(description);
      appManagerAdd.setType(type);
      appManagerAdd.setUrl(url);

      Map<String,String> loginInfo = new HashMap<String,String>();
      String username = null;
      String password = null;
      loginInfo.put(Constants.LOGIN_USERNAME, username);
      loginInfo.put(Constants.LOGIN_PASSWORD, password);
      try {
         if (CommandsUtils.isBlank(username)) {
            if (!CommandsUtils.prompt(Constants.CONNECT_ENTER_USER_NAME,
                  CommandsUtils.PromptType.USER_NAME, loginInfo)) {
               return;
            }
         }
         if (CommandsUtils.isBlank(password)) {
            if (!CommandsUtils.prompt(Constants.CONNECT_ENTER_PASSWORD, CommandsUtils.PromptType.PASSWORD,
                  loginInfo)) {
               return;
            }
         }
      } catch (Exception e) {
         System.out.println();
         System.out.println(Constants.OUTPUT_OBJECT_APPMANAGER + " "
               + Constants.OUTPUT_OP_RESULT_FAIL + ": " + e.getMessage());
         return;
      }
      username = loginInfo.get(Constants.LOGIN_USERNAME);
      password = loginInfo.get(Constants.LOGIN_PASSWORD);
      appManagerAdd.setUsername(username);
      appManagerAdd.setPassword(password);

      String sslCertificateFilePath = null;
      String sslCertificate = null;
      if (url.toLowerCase().startsWith("https")) {
         int k = 0;
         while (k < 3) {
            try {
               ConsoleReader reader = new ConsoleReader();
               reader.setPrompt(Constants.PARAM_PROMPT_SSL_CERTIFICATE_MESSAGE);
               sslCertificateFilePath = reader.readLine();

               if (CommonUtil.isBlank(sslCertificateFilePath)) {
                  System.out.println("File path cannot be null.");
               } else {
                  try {
                     sslCertificate =
                           CommandsUtils.dataFromFile(sslCertificateFilePath);
                     break;
                  } catch (FileNotFoundException e) {
                     System.out.println("Cannot find file " + sslCertificateFilePath);
                  } catch (IOException e) {
                     System.out.println("Cannot read file " + sslCertificateFilePath);
                  }
               }
            } catch (IOException e) {
               System.out.println("Cannot read file path.");
            }
            k++;
         }
         if (k < 3) {
            appManagerAdd.setSslCertificate(sslCertificate);
         } else {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
                  name, Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                  "failed to read ssl certificate file");
            return;
         }
      }

      try {
         restClient.add(appManagerAdd);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_RESULT_ADD);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   /**
    * <p>
    * Display appmanager list. eg. appmanager list -name cm
    * </p>
    *
    * @param name
    *           The appmanager name
    */
   @CliCommand(value = "appmanager list", help = "Display App Manager list.")
   public void listAppManager(
         @CliOption(key = { "name" }, mandatory = false, help = "The appmanager name") final String name,
         @CliOption(key = { "distros" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The supported distros") final boolean distros,
         @CliOption(key = { "distro" }, mandatory = false, help = "The distro name") final String distroName,
         @CliOption(key = { "roles" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The roles") final boolean roles,
         @CliOption(key = { "configurations" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The configurations") final boolean configurations) {
      // rest invocation
      try {
         if (CommandsUtils.isBlank(name)) {
            if (distros) {
               AppManagerRead[] appmanagers = restClient.getAll();
               for (AppManagerRead appmanager : appmanagers) {
                  prettyOutputAppManagerStacks(appmanager);
               }
            } else {
               AppManagerRead[] appmanagers = restClient.getAll();
               prettyOutputAppManagerInfo(appmanagers);
            }
         } else {
            AppManagerRead appmanager = restClient.get(name);
            if (distros) {
               prettyOutputAppManagerStacks(appmanager);
            } else if (CommandsUtils.isBlank(distroName)) {
               prettyOutputAppManagerInfo(appmanager);
            } else {
               if (roles) {
                  String[] stackRoles = restClient.getRoles(name, distroName);
                  for (String stackRole : stackRoles) {
                     System.out.println(stackRole);
                  }
               } else if (configurations) {
                  System.out.println(restClient.getConfigurations(name, distroName));
               }
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   /**
    * @param appmanager
    */
   private void prettyOutputAppManagerStacks(AppManagerRead appmanager) {
      printSeperator();
      String description = appmanager.getDescription();
      if (description == null) description = "";

      // list cluster level params
      LinkedHashMap<String, String> clusterParams =
            new LinkedHashMap<String, String>();
      clusterParams.put("APP MANAGER NAME", appmanager.getName());
      clusterParams.put("DESCRIPTION", description);
      clusterParams.put("TYPE", appmanager.getType());
      clusterParams.put("URL", appmanager.getUrl());
      for (String key : clusterParams.keySet()) {
         System.out.printf(Constants.OUTPUT_INDENT + "%-26s:"
               + Constants.OUTPUT_INDENT + "%s\n", key, clusterParams.get(key));
      }
      System.out.println();

      DistroRead[] distros = restClient.getDistros(appmanager.getName());
      if (distros != null && distros.length > 0) {
         LinkedHashMap<String, List<String>> stackColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
         stackColumnNamesWithGetMethodNames
               .put(Constants.FORMAT_TABLE_COLUMN_VENDOR,
                     Arrays.asList("getVendor"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_VERSION,
               Arrays.asList("getVersion"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_HVE,
               Arrays.asList("isHveSupported"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_ROLES, Arrays.asList("getRoles"));

         try {
            CommandsUtils.printInTableFormat(
                  stackColumnNamesWithGetMethodNames, distros,
                  Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }
      }
   }

   private void prettyOutputAppManagerInfo(AppManagerRead[] appmanagers) {
      if (appmanagers != null) {
         LinkedHashMap<String, List<String>> appManagerColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         appManagerColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
         appManagerColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_DESCRIPTION, Arrays.asList("getDescription"));
         appManagerColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_TYPE, Arrays.asList("getType"));
         appManagerColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_URL, Arrays.asList("getUrl"));
         appManagerColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_USERNAME, Arrays.asList("getUsername"));
         //appManagerColumnNamesWithGetMethodNames.put(
         //      Constants.FORMAT_TABLE_COLUMN_VERSION, Arrays.asList("getVersion"));
         //appManagerColumnNamesWithGetMethodNames.put(
         //      Constants.FORMAT_TABLE_COLUMN_CLUSTER_NAME, Arrays.asList("getManagedClusters"));

         try {
            CommandsUtils.printInTableFormat(
                  appManagerColumnNamesWithGetMethodNames, appmanagers,
                  Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }
      }
   }

   private void printSeperator() {
      StringBuffer seperator =
            new StringBuffer().append(Constants.OUTPUT_INDENT);
      for (int i = 0; i < Constants.SEPERATOR_LEN; i++) {
         seperator.append("=");
      }
      System.out.println(seperator.toString());
      System.out.println();
   }

   private void prettyOutputAppManagerInfo(AppManagerRead appmanager) {
      if (appmanager != null)
         prettyOutputAppManagerInfo(new AppManagerRead[] { appmanager });
   }
}
