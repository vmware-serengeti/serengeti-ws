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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.cli.rest.AppManagerRestClient;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;

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

   @CliCommand(value = "appmanager add", help = "Add an App Manager instance")
   public void addAppManager(
         @CliOption(key = { "name" }, mandatory = true, help = "The instance name") final String name,
         @CliOption(key = { "description" }, mandatory = true, help = "The instance description") final String description,
         @CliOption(key = { "type" }, mandatory = true, help = "The provider type, ClouderaManager or Ambari") final String type,
         @CliOption(key = { "url" }, mandatory = true, help = "The instance URL") final String url,
         @CliOption(key = { "username" }, mandatory = false, help = "The username to login software manager server") final String username,
         @CliOption(key = { "password"}, mandatory = false, help = "The password") final String password,
         @CliOption(key = { "sslCertificateFile"}, mandatory = true, help = "The ssl certificate file of the instance") final String path) {

      // rest invocation
      try {
         AppManagerAdd appManagerAdd = new AppManagerAdd();
         appManagerAdd.setName(name);
         appManagerAdd.setDescription(description);
         appManagerAdd.setType(type);
         appManagerAdd.setUrl(url);
         appManagerAdd.setUsername(username);
         appManagerAdd.setPassword(password);
         appManagerAdd.setSslCertificate(CommandsUtils.dataFromFile(path));
         restClient.add(appManagerAdd);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_APPMANAGER, name,
               Constants.OUTPUT_OP_RESULT_ADD);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_APPMANAGER, name,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
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
         @CliOption(key = { "supportedStacks" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The supported stacks") final boolean supportedStacks) {
      // rest invocation
      try {
         if (CommandsUtils.isBlank(name)) {
            AppManagerRead[] appmanagers = restClient.getAll();
            if (appmanagers != null) {
               prettyOutputAppManagerInfo(appmanagers);
            }
         } else {
            if (supportedStacks) {
               HadoopStack[] stacks = restClient.getStacks(name);
               if (stacks != null && stacks.length > 0) {
                  prettyOutputAppManagerStacks(stacks);
               }
            } else {
               AppManagerRead appmanager = restClient.get(name);
               if (appmanager != null) {
                  prettyOutputAppManagerInfo(appmanager);
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
    * @param stacks
    */
   private void prettyOutputAppManagerStacks(HadoopStack[] stacks) {
      if (stacks != null && stacks.length > 0) {
         LinkedHashMap<String, List<String>> stackColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getDistro"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_VENDOR, Arrays.asList("getVendor"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_VERSION, Arrays.asList("getFullVersion"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_HVE, Arrays.asList("isHveSupported"));
         stackColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_ROLES, Arrays.asList("getRoles"));

         try {
            CommandsUtils.printInTableFormat(
                  stackColumnNamesWithGetMethodNames, stacks,
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

   private void prettyOutputAppManagerInfo(AppManagerRead appmanager) {
      if (appmanager != null)
         prettyOutputAppManagerInfo(new AppManagerRead[] { appmanager });
   }
}
