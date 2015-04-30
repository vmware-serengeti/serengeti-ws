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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.cli.rest.AppManagerRestClient;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.DistroRestClient;

@Component
public class DistroCommands implements CommandMarker {
   @Autowired
   private DistroRestClient restClient;
   @Autowired
   private AppManagerRestClient appManagerRestClient;

   @CliAvailabilityIndicator({ "distro help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "distro list", help = "Get distribution information")
   public void getDistro(
         @CliOption(key = { "name" }, mandatory = false, help = "The distribution name") final String name,
         @CliOption(key = { "appManager" }, mandatory = false, help = "The application manager name") final String appManager,
         @CliOption(key = { "detail" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show detail information") final boolean detail) {

      String appmanager;
      if (CommandsUtils.isBlank(appManager)) {
         appmanager = Constants.IRONFAN;
      } else {
         appmanager = appManager;
      }
      // rest invocation
      try {
         if (name == null) {
            DistroRead[] distros = appManagerRestClient.getDistros(appmanager);
            prettyOutputDistrosInfo(distros);
         } else {
            DistroRead distro = appManagerRestClient.getDistroByName(appmanager, name);
            prettyOutputDistroInfo(distro);
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DISTRO,
               Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   private void prettyOutputDistrosInfo(DistroRead[] distros) {
      if (distros != null) {
         Arrays.sort(distros);
         LinkedHashMap<String, List<String>> distroColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_VENDOR, Arrays.asList("getVendor"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_VERSION, Arrays.asList("getVersion"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_HVE, Arrays.asList("isHveSupported"));
         distroColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_ROLES, Arrays.asList("getRoles"));
         try {
            CommandsUtils.printInTableFormat(
                  distroColumnNamesWithGetMethodNames, distros,
                  Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            String distroName = null;
            if (distros.length == 1) {
               distroName = distros[0].getName();
            }
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_DISTRO,
                  Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      }
   }

   private void prettyOutputDistroInfo(DistroRead distroRead) {
      if (distroRead != null)
         prettyOutputDistrosInfo(new DistroRead[] { distroRead });
   }
}
