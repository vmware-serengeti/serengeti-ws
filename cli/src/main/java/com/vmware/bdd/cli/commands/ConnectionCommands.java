/*****************************************************************************
 *      Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *      Licensed under the Apache License, Version 2.0 (the "License");
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
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.stereotype.Component;

import com.vmware.bdd.cli.rest.RestClient;

@Component
public class ConnectionCommands implements CommandMarker {
   @Autowired
   RestClient conn;
   @CliAvailabilityIndicator({"connect help"})
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "connect", help = "Connect a serengeti server")
   public void conn(
         @CliOption(key = { "host" }, mandatory = true, 
         help = "The serengeti host with optional port number, e.g. hostname:port") final String hostName) {

      conn.connect(hostName);
   }
}