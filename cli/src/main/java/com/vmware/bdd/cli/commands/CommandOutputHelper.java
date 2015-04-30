/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
 *****************************************************************************/
package com.vmware.bdd.cli.commands;

import java.util.List;

/**
 * Created By xiaoliangl on 12/25/14.
 */
public class CommandOutputHelper {
   private String type;
   private String opName;
   private String failure;
   private String warning;
   private String success;

   public CommandOutputHelper(String type, String opName, String failResult, String warningResult, String successResult) {
      this.type = type;
      this.opName = opName;
      this.failure = failResult;
      this.warning = warningResult;
      this.success = successResult;
   }

   public void printSuccess() {
      String result = "";
      switch (opName) {
         case Constants.OUTPUT_OP_ADD:
            result = Constants.OUTPUT_OP_RESULT_ADD;
            break;
         case Constants.OUTPUT_OP_GET:
            result = Constants.OUTPUT_OP_RESULT_GET;
            break;
         case Constants.OUTPUT_OP_MODIFY:
            result = Constants.OUTPUT_OP_RESULT_MODIFY;
            break;
         default:
      }
      CommandsUtils.printCmdSuccess(type, result);
   }

   public void printFailure(String message) {
      CommandsUtils.printCmdFailure(type, opName, failure, message);
   }

   public void printFailure(Throwable throwable) {
      CommandsUtils.printCmdFailure(type, opName, failure, throwable.getMessage());
   }

   public void printWarning(String message) {
      CommandsUtils.printCmdFailure(type, opName, warning, message);
   }

   public boolean promptWarning(List<String> warningList, boolean yes, String message) {
      return CommandsUtils.showWarningMsg("", type, opName, warningList, yes, message);
   }

   static {
      ADD_LDAP_OUTPUT = new CommandOutputHelper(Constants.OUTPUT_OBJECT_USERMGMT,
            Constants.OUTPUT_OP_ADD, Constants.OUTPUT_OP_RESULT_FAIL, Constants.OUTPUT_OP_RESULT_WARN, Constants.OUTPUT_OP_RESULT_SUCCEEDED);

      GET_LDAP_OUTPUT = new CommandOutputHelper(Constants.OUTPUT_OBJECT_USERMGMT,
            Constants.OUTPUT_OP_GET, Constants.OUTPUT_OP_RESULT_FAIL, Constants.OUTPUT_OP_RESULT_WARN, Constants.OUTPUT_OP_RESULT_SUCCEEDED);

      MODIFY_LDAP_OUTPUT = new CommandOutputHelper(Constants.OUTPUT_OBJECT_USERMGMT,
            Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL, Constants.OUTPUT_OP_RESULT_WARN, Constants.OUTPUT_OP_RESULT_SUCCEEDED);

      MODIFY_MGMTVMCFG_OUTPUT = new CommandOutputHelper(Constants.OUTPUT_OBJECT_MGMTVMCFG,
            Constants.OUTPUT_OP_MODIFY, Constants.OUTPUT_OP_RESULT_FAIL, Constants.OUTPUT_OP_RESULT_WARN, Constants.OUTPUT_OP_RESULT_SUCCEEDED);

      GET_MGMTVMCFG_OUTPUT = new CommandOutputHelper(Constants.OUTPUT_OBJECT_MGMTVMCFG,
            Constants.OUTPUT_OP_GET, Constants.OUTPUT_OP_RESULT_FAIL, Constants.OUTPUT_OP_RESULT_WARN, Constants.OUTPUT_OP_RESULT_SUCCEEDED);
   }

   public static CommandOutputHelper ADD_LDAP_OUTPUT;
   public static CommandOutputHelper MODIFY_LDAP_OUTPUT;
   public static CommandOutputHelper GET_LDAP_OUTPUT;
   public static CommandOutputHelper MODIFY_MGMTVMCFG_OUTPUT;
   public static CommandOutputHelper GET_MGMTVMCFG_OUTPUT;
}
