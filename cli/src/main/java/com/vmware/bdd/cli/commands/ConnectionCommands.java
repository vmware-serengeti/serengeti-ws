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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jline.ConsoleReader;

import org.springframework.beans.factory.annotation.Autowired;
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

   private enum PromptType {
      USER_NAME, PASSWORD
   }

   @CliAvailabilityIndicator({"connect help"})
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "connect", help = "Connect a serengeti server")
   public void conn(
         @CliOption(key = { "host" }, mandatory = true, help = "The serengeti host with optional port number, e.g. hostname:port") final String hostName,
         @CliOption(key = { "userName" }, mandatory = false, help = "The serengeti user name") final String userName,
         @CliOption(key = { "password" }, mandatory = false, help = "The serengeti password") final String password) {
      Map<String,String> loginInfo = new HashMap<String,String>();
      loginInfo.put("userName", userName);
      loginInfo.put("password", password);
      if (CommandsUtils.isBlank(userName)) {
         if(!prompt(Constants.CONNECT_ENTER_USER_NAME, PromptType.USER_NAME, loginInfo)){
            return ;
         }
      }
      if (CommandsUtils.isBlank(password)) {
         if(!prompt(Constants.CONNECT_ENTER_PASSWORD, PromptType.PASSWORD, loginInfo)){
            return ;
         }
      }
      connect(hostName, loginInfo, 3);
   }

   private boolean connect(final String hostName, final Map<String,String> loginInfo, int count) {
      if (count <= 0) {
         return false;
      } else {
         ConnectType connectType = conn.connect(hostName, loginInfo.get("userName"), loginInfo.get("password"));
         if (connectType == ConnectType.UNAUTHORIZATION) {
            if (!prompt(Constants.CONNECT_ENTER_PASSWORD, PromptType.PASSWORD,
                  loginInfo)) {
               return false;
            } else {
               count--;
               connect(hostName, loginInfo, count);
            }
         }
      }
      return true;
   }

   private boolean prompt(String msg, PromptType promptType, Map<String,String> loginInfo) {
      int k = 0;
      String enter = "";
      while (k < 3) {
         System.out.println(msg);
         enter = readEnter(msg, promptType);
         if (!CommandsUtils.isBlank(enter)) {
            if (promptType == PromptType.USER_NAME) {
               loginInfo.put("userName", enter);
            } else {
               loginInfo.put("password", enter);
            }
            break;
         } else {
            StringBuilder warningMsg = new StringBuilder();
            if (promptType == PromptType.USER_NAME) {
               warningMsg.append(Constants.CONNECT_USER_NAME);
            } else {
               warningMsg.append(Constants.CONNECT_PASSWORD);
            }
            warningMsg.append(Constants.CONNECT_CAN_NOT_BE_NULL);
            System.out.println(warningMsg.toString());
         }
         k++;
      }
      return k < 3;
   }

   private String readEnter(String msg,PromptType promptType){
      String enter="";
      try {
         ConsoleReader reader = new ConsoleReader();;
         if (promptType == PromptType.USER_NAME){
            enter=reader.readLine();
         }else if (promptType == PromptType.PASSWORD){
            enter=reader.readLine(new Character('*'));
         }
      } catch (IOException e) {
         e.printStackTrace();
      }      
      return enter;
   }

   
}