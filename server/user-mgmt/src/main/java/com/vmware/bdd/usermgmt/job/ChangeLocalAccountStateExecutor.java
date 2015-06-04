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
package com.vmware.bdd.usermgmt.job;

import java.io.File;
import java.io.IOException;

import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;

/**
 * Created By xiaoliangl on 12/24/14.
 */
public class ChangeLocalAccountStateExecutor {
   private final static String SET_PASSWORD_COMMAND = System.getProperties().getProperty("serengeti.home.dir")
         + File.separator + "sbin"
         + File.separator + "set-password";

   private final static int TIMEOUT = Configuration.getInt("usermgmt.command.exec.timeout", 120);

   private final static Logger LOGGER = Logger.getLogger(ChangeLocalAccountStateExecutor.class);

   public void execute(boolean enabled) {
      if(enabled) {
         changeLocalAccountState("-U");//unlock accounts
      } else {
         changeLocalAccountState("-L");//lock accounts
      }
   }

   private void changeLocalAccountState(String argument) {
      //String chefCmd = "sudo /opt/serengeti/sbin/set-password L";
      String sudoCmd = CommonUtil.getCustomizedSudoCmd();
      CommandLine cmdLine = new CommandLine(sudoCmd)
            .addArgument(SET_PASSWORD_COMMAND)
            .addArgument(argument);

      DefaultExecutor executor = new DefaultExecutor();

      executor.setStreamHandler(new PumpStreamHandler(
            new ExecOutputLogger(LOGGER, false), //output logger
            new ExecOutputLogger(LOGGER, true))  //error logger
      );

      executor.setWatchdog(new ExecuteWatchdog(1000l * TIMEOUT));

      try {
         int exitVal = executor.execute(cmdLine);
         if(exitVal != 0) {
            throw new UserMgmtExecException("CHANGE_LOCAL_ACCOUNT_STATE_FAIL", null);
         }
      } catch (IOException e) {
         throw new UserMgmtExecException("CHANGE_LOCAL_ACCOUNT_STATE_FAIL", e);
      }
   }

}
