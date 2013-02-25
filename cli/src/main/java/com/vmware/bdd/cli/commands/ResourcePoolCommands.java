/*****************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.ResourcePoolAdd;
import com.vmware.bdd.apitypes.ResourcePoolRead;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.ResourcePoolRestClient;

@Component
public class ResourcePoolCommands implements CommandMarker {
   @Autowired
   private ResourcePoolRestClient restClient;

   @CliAvailabilityIndicator({ "resourcepool help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "resourcepool add", help = "Add a new resource pool")
   public void addResourcePool(
         @CliOption(key = { "name" }, mandatory = true, help = "The resource pool name") final String name,
         @CliOption(key = { "vcrp" }, mandatory = true, help = "The vc rp name") final String vcrp,
         @CliOption(key = { "vccluster" }, mandatory = true, help = "The vc cluster name") final String vccluster) {
      //build ResourcePoolAdd object
      ResourcePoolAdd rpAdd = new ResourcePoolAdd();
      rpAdd.setName(name);
      rpAdd.setResourcePoolName(vcrp);
      rpAdd.setVcClusterName(vccluster);

      //rest invocation
      try {
         restClient.add(rpAdd);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_RESOURCEPOOL, name,
               Constants.OUTPUT_OP_RESULT_ADD);
      }catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_RESOURCEPOOL, name,
               Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "resourcepool delete", help = "Delete an unused resource pool")
   public void deleteResourcePool(
         @CliOption(key = { "name" }, mandatory = true, help = "The resource pool name") final String name) {

      //rest invocation
      try {
         restClient.delete(name);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_RESOURCEPOOL, name,
               Constants.OUTPUT_OP_RESULT_DELETE);
      }catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_RESOURCEPOOL, name,
               Constants.OUTPUT_OP_DELETE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "resourcepool list", help = "Get resource pool information")
   public void getResourcePool(
         @CliOption(key = { "name" }, mandatory = false, help = "The resource pool name") final String name,
         @CliOption(key = { "detail" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show node information") final boolean detail) {

      try {
         if (name == null) {
            ResourcePoolRead[] rps = restClient.getAll();
            if (rps != null) {
               prettyOutputResourcePoolsInfo(rps, detail);
            }
         } else {
            ResourcePoolRead rp = restClient.get(name);
            if (rp != null) {
               prettyOutputResourcePoolInfo(rp, detail);
            }
         }
      }catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_RESOURCEPOOL,
               name, Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   private void prettyOutputResourcePoolInfo(ResourcePoolRead rp, boolean detail) {
      if (rp != null)
         prettyOutputResourcePoolsInfo(new ResourcePoolRead[] { rp }, detail);
   }

   private void prettyOutputResourcePoolsInfo(ResourcePoolRead[] rps,
         boolean detail) {
      if (rps != null) {
         LinkedHashMap<String, List<String>> rpColumnNamesWithGetMethodNames =
               new LinkedHashMap<String, List<String>>();
         rpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getRpName"));
         rpColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_PATH, Arrays.asList("findPath"));
         //TODO when backend support it, we can remove the comment.
//         rpColumnNamesWithGetMethodNames.put(
//               Constants.FORMAT_TABLE_COLUMN_RAM_MB,
//               Arrays.asList("getTotalRAMInMB"));
//         rpColumnNamesWithGetMethodNames.put(
//               Constants.FORMAT_TABLE_COLUMN_CPU_MHZ,
//               Arrays.asList("getTotalCPUInMHz"));
         try {
            if (detail) {
               LinkedHashMap<String, List<String>> nodeColumnNamesWithGetMethodNames =
                     new LinkedHashMap<String, List<String>>();
               nodeColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_NAME,
                     Arrays.asList("getName"));
               nodeColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_IP,
                     Arrays.asList("getIp"));
               nodeColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_ROLES,
                     Arrays.asList("getRoles"));

               for (ResourcePoolRead rp : rps) {
                  if (rp != null) {
                     CommandsUtils.printInTableFormat(
                           rpColumnNamesWithGetMethodNames,
                           new ResourcePoolRead[] { rp },
                           Constants.OUTPUT_INDENT);
                     NodeRead[] nodes = rp.getNodes();
                     if (nodes != null) {
                        System.out.println();
                        CommandsUtils
                              .printInTableFormat(
                                    nodeColumnNamesWithGetMethodNames,
                                    nodes,
                                    new StringBuilder()
                                          .append(Constants.OUTPUT_INDENT)
                                          .append(Constants.OUTPUT_INDENT)
                                          .toString());
                     }
                  }
                  System.out.println();
               }

            } else {
               CommandsUtils.printInTableFormat(
                     rpColumnNamesWithGetMethodNames, rps,
                     Constants.OUTPUT_INDENT);
            }
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }
      }
   }
}
