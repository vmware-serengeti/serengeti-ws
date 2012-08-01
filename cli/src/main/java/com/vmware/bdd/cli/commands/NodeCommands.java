/*****************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.NodeRestClient;

@Component
public class NodeCommands implements CommandMarker {
   @Autowired
   private NodeRestClient restClient;

   @CliAvailabilityIndicator({ "node help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "node list", help = "Get node information")
   public void getNode(
         @CliOption(key = { "name" }, mandatory = true, help = "The node name") final String name) {

      // rest invocation
      try {
         restClient.get(name);
      }catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODE,
               name, Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }
}
