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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.Connect.ConnectType;
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
         @CliOption(key = { "host" }, mandatory = false, unspecifiedDefaultValue = "localhost:8443", help = "The serengeti server hostname and port number, e.g. hostname:port") final String hostName) {
      if (!validateHostPort(hostName)) {
         return;
      }
      Map<String,String> loginInfo = new HashMap<String,String>();
      String username = null;
      String password = null;
      loginInfo.put(Constants.LOGIN_USERNAME, username);
      loginInfo.put(Constants.LOGIN_PASSWORD, password);
      try {
         if (CommandsUtils.isBlank(username)) {
            if(!CommandsUtils.prompt(Constants.CONNECT_ENTER_USER_NAME, CommandsUtils.PromptType.USER_NAME, loginInfo)){
               return ;
            }
         }
         if (CommandsUtils.isBlank(password)) {
            if(!CommandsUtils.prompt(Constants.CONNECT_ENTER_PASSWORD, CommandsUtils.PromptType.PASSWORD, loginInfo)){
               return ;
            }
         }
         connect(hostName, loginInfo, 3);
         getServerVersion(hostName);
      } catch (Exception e) {
         System.out.println();
         printConnectionFailure(e.getMessage());
      }
   }

   private static void printConnectionFailure(String message) {
      System.out.println(Constants.OUTPUT_OBJECT_CONNECT + " "
            + Constants.OUTPUT_OP_RESULT_FAIL + ": " + message);
   }

   private boolean connect(final String hostName, final Map<String, String> loginInfo, int count) throws Exception {
      if (count < 0) {
         return false;
      }
      ConnectType connectType = conn.connect(hostName, loginInfo.get(Constants.LOGIN_USERNAME), loginInfo.get(Constants.LOGIN_PASSWORD));
      if (connectType == ConnectType.UNAUTHORIZATION) {
         if (count == 0) {
            return false;
         }
         if (!CommandsUtils.prompt(Constants.CONNECT_ENTER_PASSWORD, CommandsUtils.PromptType.PASSWORD, loginInfo)) {
            return false;
         } else {
            count--;
            connect(hostName, loginInfo, count);
         }
      }
      return true;
   }

   private void getServerVersion(final String hostName) throws Exception {
      final String path = Constants.REST_PATH_HELLO;
      final HttpMethod httpverb = HttpMethod.GET;

      @SuppressWarnings("unchecked")
      HashMap<String, String> serverInfo = conn.getObjectByPath(HashMap.class, path, httpverb, false);
      String serverVersion = serverInfo.get("version");
      String cliVersion = com.vmware.bdd.utils.Constants.VERSION;
      if (!cliVersion.equals(serverVersion)) {
         System.out.println("Warning: CLI version "+ cliVersion + " does not match with management server version " + serverVersion + ".");
         System.out.println("You must use the same version for CLI and management server. Otherwise, some commands may not be compatible.");
      }
   }

   private boolean validateHostPort(String hostName) {
      String hostPort = "(.*:0*8443)";
      Pattern hostPortPattern = Pattern.compile(hostPort);
      if (!hostPortPattern.matcher(hostName).matches()) {
         System.out.println(Constants.CONNECT_PORT_IS_WRONG);
         return false;
      }
      return true;
   }

}
