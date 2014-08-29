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
import java.util.ArrayList;
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

   /*@CliCommand(value = "appmanager types", help = "List all app manager types")
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

   @CliCommand(value = "appmanager add", help = "Add an application manager instance")
   public void addAppManager(
         @CliOption(key = { "name" }, mandatory = true, help = "The instance name") final String name,
         @CliOption(key = { "description" }, mandatory = false, help = "The instance description") final String description,
         @CliOption(key = { "type" }, mandatory = true, help = "The provider type, ClouderaManager or Ambari") final String type,
         @CliOption(key = { "url" }, mandatory = true, help = "The instance URL, e.g. http://hostname:port") final String url) {

      AppManagerAdd appManagerAdd = new AppManagerAdd();
      appManagerAdd.setName(name);
      appManagerAdd.setDescription(description);

      //validate appmanager type
      String[] types = restClient.getTypes();
      boolean found = false;
      for (String t : types) {
         if (type.equals(t)) {
            found = true;
            break;
         }
      }
      if (found) {
         appManagerAdd.setType(type);
      } else {
         CommandsUtils.printCmdFailure(
               Constants.OUTPUT_OBJECT_APPMANAGER,
               name,
               Constants.OUTPUT_OP_ADD,
               Constants.OUTPUT_OP_RESULT_FAIL,
               "Invalid type " + type + ". Valid types are "
                     + Arrays.asList(types) + ".");
         return;
      }

      //validate url in appManagerAdd
      List<String> errorMsgs = new ArrayList<String>();
      if (!CommonUtil.validateUrl(url, errorMsgs)) {
         CommandsUtils.printCmdFailure(
               Constants.OUTPUT_OBJECT_APPMANAGER,
               name,
               Constants.OUTPUT_OP_ADD,
               Constants.OUTPUT_OP_RESULT_FAIL,
               "Invalid URL: " + CommonUtil.mergeErrorMsgList(errorMsgs) + ".");
         return;
      }
      appManagerAdd.setUrl(url);

      Map<String, String> loginInfo = getAccount();
      if (null == loginInfo) {
         return;
      }
      appManagerAdd.setUsername(loginInfo.get(Constants.LOGIN_USERNAME));
      appManagerAdd.setPassword(loginInfo.get(Constants.LOGIN_PASSWORD));

      if (url.toLowerCase().startsWith("https")) {
         String sslCertificate = getSslCertificate();
         if (null != sslCertificate) {
            appManagerAdd.setSslCertificate(sslCertificate);
         } else {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
                  name, Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
                  "Failed to read ssl certificate file.");
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

   private String getSslCertificate() {
      String sslCertificateFilePath;
      String sslCertificate = "";
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
                  System.out.println("Cannot find file " + sslCertificateFilePath + ".");
               } catch (IOException e) {
                  System.out.println("Cannot read file " + sslCertificateFilePath + ".");
               }
            }
         } catch (IOException e) {
            System.out.println("Cannot read file path.");
         }
         k++;
      }
      if (k < 3) {
         return sslCertificate;
      } else {
         return null;
      }
   }

   private Map<String, String> getAccount() {
      Map<String,String> loginInfo = new HashMap<String,String>();
      String username = null;
      String password = null;
      loginInfo.put(Constants.LOGIN_USERNAME, username);
      loginInfo.put(Constants.LOGIN_PASSWORD, password);
      try {
         if (CommandsUtils.isBlank(username)) {
            if (!CommandsUtils.prompt(Constants.CONNECT_ENTER_USER_NAME,
                  CommandsUtils.PromptType.USER_NAME, loginInfo)) {
               return null;
            }
         }
         if (CommandsUtils.isBlank(password)) {
            if (!CommandsUtils.prompt(Constants.CONNECT_ENTER_PASSWORD, CommandsUtils.PromptType.PASSWORD,
                  loginInfo)) {
               return null;
            }
         }
      } catch (Exception e) {
         System.out.println();
         System.out.println(Constants.OUTPUT_OBJECT_APPMANAGER + " "
               + Constants.OUTPUT_OP_RESULT_FAIL + ": " + e.getMessage());
         return null;
      }
      return loginInfo;
   }

   /**
    * <p>
    * Display appmanager list. eg. appmanager list -name cm
    * </p>
    *
    * @param name
    *           The appmanager name
    */
   @CliCommand(value = "appmanager list", help = "Display list of application managers.")
   public void listAppManager(
         @CliOption(key = { "name" }, mandatory = false, help = "The application manager name") final String name,
         @CliOption(key = { "distros" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "List the supported distributions") final boolean distros,
         @CliOption(key = { "distro" }, mandatory = false, help = "The distribution name") final String distro,
         @CliOption(key = { "roles" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "List the roles") final boolean roles,
         @CliOption(key = { "configurations" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "List the configurations") final boolean configurations) {
      // parameters validation
      if (distros && !CommandsUtils.isBlank(distro)) {
         // appmanager list --distros --distro <distro>
         CommandsUtils
               .printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER, name,
                     Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
                     "Cannot use --distros and --distro <distro name> at the same time.");
         return;
      }

      if (CommandsUtils.isBlank(name) && !CommandsUtils.isBlank(distro)) {
         // appmanager list --distro <distro>
         CommandsUtils
               .printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER, name,
                     Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
                     "--distro <distro name> must be used with --name <application manager name>.");
         return;
      }

      if ((CommandsUtils.isBlank(name) || CommandsUtils.isBlank(distro))
            && (roles || configurations)) {
         // appmanager list --roles
         // appmanager list --configurations
         // need to have both --name <name> and --distro <distro>
         CommandsUtils
               .printCmdFailure(
                     Constants.OUTPUT_OBJECT_APPMANAGER,
                     name,
                     Constants.OUTPUT_OP_LIST,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     "--roles or --configurations must be used with --name <application manager name> and --distro <distro name>.");
         return;
      }

      if (roles && configurations) {
         // appmanager list --roles --configurations
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               "Cannot use --roles and --configurations in the same command.");
         return;
      }

      // rest invocation
      try {
         if (CommandsUtils.isBlank(name)) {
            if (distros) {
               // appmanager list --distros
               AppManagerRead[] appmanagers = restClient.getAll();
               for (AppManagerRead appmanager : appmanagers) {
                  prettyOutputAppManagerDistros(appmanager);
               }
            } else {
               // appmanager list
               AppManagerRead[] appmanagers = restClient.getAll();
               prettyOutputAppManagerInfo(appmanagers);
            }
         } else {
            if (distros) {
               // appmanager list --name <name> --distros
               AppManagerRead appmanager = restClient.get(name);
               prettyOutputAppManagerDistros(appmanager);
            } else if (CommandsUtils.isBlank(distro)) {
               // appmanager list --name <name>
               AppManagerRead appmanager = restClient.get(name);
               prettyOutputAppManagerInfo(appmanager);
            } else {
               if (roles) {
                  // appmanager list --name <name> --distro <distro> --roles
                  String[] distroRoles = restClient.getRoles(name, distro);
                  if (null != distroRoles) {
                     for (String distroRole : distroRoles) {
                        System.out.println(distroRole);
                     }
                  }
               } else if (configurations) {
                  // appmanager list --name <name> --distro <distro> --configurations
                  System.out.println(restClient.getConfigurations(name, distro));
               } else {
                  // appmanager list --name <name> --distro <distro>
                  DistroRead distroRead = restClient.getDistroByName(name, distro);
                  prettyOutputAppManagerDistro(new DistroRead[]{distroRead});
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
   private void prettyOutputAppManagerDistros(AppManagerRead appmanager) {
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
      prettyOutputAppManagerDistro(distros);
   }

   private void prettyOutputAppManagerDistro(DistroRead[] distros) {
      if (distros != null && distros.length > 0) {
         LinkedHashMap<String, List<String>> distroColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
         distroColumnNamesWithGetMethodNames
               .put(Constants.FORMAT_TABLE_COLUMN_VENDOR,
                     Arrays.asList("getVendor"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_VERSION,
               Arrays.asList("getVersion"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_HVE,
               Arrays.asList("isHveSupported"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_ROLES, Arrays.asList("getRoles"));

         try {
            CommandsUtils.printInTableFormat(
                  distroColumnNamesWithGetMethodNames, distros,
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
         //appManagerColumnNamesWithGetMethodNames.put(
         //      Constants.FORMAT_TABLE_COLUMN_USERNAME, Arrays.asList("getUsername"));
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
      if (appmanager != null) {
         AppManagerRead[] appmanagers = new AppManagerRead[] { appmanager };
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

   @CliCommand(value = "appmanager modify", help = "Modify an application manager instance")
   public void modifyAppManager(
         @CliOption(key = { "name" }, mandatory = true, help = "The instance name") final String name,
         //@CliOption(key = { "description" }, mandatory = false, help = "The instance description") final String description,
         //@CliOption(key = { "type" }, mandatory = false, help = "The provider type, ClouderaManager or Ambari") final String type,
         @CliOption(key = { "url" }, mandatory = false, help = "The instance URL, e.g. http://hostname:port") final String url,
         @CliOption(key = { "changeAccount" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Change login account") final boolean changeAccount,
         @CliOption(key = { "changeCertificate" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Change ssl certificate") final boolean changeCertificate) {

      if (url == null && !changeAccount && !changeCertificate) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL,
               "Must use at least one of the following: --url, --changeAccount or --changeCertificate.");
         return;
      }

      if(url != null && !changeAccount) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL,
               "--url must be used together with --changeAccount");
         return;
      }

      if (Constants.IRONFAN.equals(name)) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL,
               "Cannot modify default application manager.");
         return;
      }

      try {
         AppManagerRead appManagerRead = restClient.get(name);
         // Display a warning if there is any cluster using this appmanager
         List<String> clusters = appManagerRead.getManagedClusters();
         if (clusters != null && clusters.size() > 0) {
            List<String> warningMsgList = new ArrayList<String>(1);
            warningMsgList.add("Application manager " + name
                  + " is used by clusters " + clusters + ".");
            if (!CommandsUtils.showWarningMsg(name,
                  Constants.OUTPUT_OBJECT_APPMANAGER,
                  Constants.OUTPUT_OP_MODIFY, warningMsgList, false)) {
               return;
            }
         }

         AppManagerAdd appManagerAdd = new AppManagerAdd();
         appManagerAdd.setName(name);
         //appManagerAdd.setDescription(description);
         appManagerAdd.setDescription(appManagerRead.getDescription());
         //appManagerAdd.setType(type);
         appManagerAdd.setType(appManagerRead.getType());
         if (url == null) {
            appManagerAdd.setUrl(appManagerRead.getUrl());
         } else {
            //validate url in appManagerAdd
            List<String> errorMsgs = new ArrayList<String>();
            if (!CommonUtil.validateUrl(url, errorMsgs)) {
               CommandsUtils.printCmdFailure(
                     Constants.OUTPUT_OBJECT_APPMANAGER,
                     name,
                     Constants.OUTPUT_OP_ADD,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     "Invalid URL: " + CommonUtil.mergeErrorMsgList(errorMsgs) + ".");
               return;
            }
            appManagerAdd.setUrl(url);
         }

         if (changeAccount) {
            Map<String, String> loginInfo = getAccount();
            if (null == loginInfo) {
               return;
            }
            appManagerAdd.setUsername(loginInfo.get(Constants.LOGIN_USERNAME));
            appManagerAdd.setPassword(loginInfo.get(Constants.LOGIN_PASSWORD));
         } else {
            appManagerAdd.setUsername(appManagerRead.getUsername());
            appManagerAdd.setPassword(appManagerRead.getPassword());
         }

         if ((url != null && url.toLowerCase().startsWith("https"))
               || (url == null && changeCertificate && appManagerAdd
                     .getUrl().toLowerCase().startsWith("https"))) {
            // new URL starts with https or
            // changeCertificate for old URL starts with https (no new URL)
            String sslCertificate = getSslCertificate();
            if (null == sslCertificate) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
                     name, Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL,
                     "Fail to get ssl certificate.");
               return;
            }
            appManagerAdd.setSslCertificate(sslCertificate);
         } else if (url == null && changeCertificate
               && !appManagerAdd.getUrl().toLowerCase().startsWith("https")) {
            // changeCertificate for old URL does not start with https (no new URL)
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
                  name, Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL,
                  "Cannot set ssl certificate for http service.");
            return;
         } else if (url != null && !url.toLowerCase().startsWith("https")) {
            // new url does not start with https
            appManagerAdd.setSslCertificate(null);
         } else {
            // no new url or changeCertificate
            appManagerAdd.setSslCertificate(appManagerRead.getSslCertificate());
         }

         restClient.modify(appManagerAdd);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_RESULT_MODIFY);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER,
               name, Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }

   }

   @CliCommand(value = "appmanager delete", help = "Delete an application manager instance")
   public void deleteAppManager(
         @CliOption(key = { "name" }, mandatory = true, help = "The application manager name") final String name) {
      try {
         restClient.delete(name);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_APPMANAGER, name,
               Constants.OUTPUT_OP_RESULT_DELETE);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER, name,
               Constants.OUTPUT_OP_DELETE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }
}
