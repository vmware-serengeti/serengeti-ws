/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.sp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.ShellCommandExecutor;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.SSHUtil;
/**
 * Store Procedure of setting password for a vm
 */
public class SetVMPasswordSP implements Callable<Void> {

   private static final Logger logger = Logger.getLogger(SetVMPasswordSP.class);
   private String nodeIP;
   private String password;

   private String privateKeyFile;
   private String sshUser;
   private int sshPort;
   private final static int SETUP_PASSWORDLESS_LOGIN_TIMEOUT = 120; //in seconds
   private int setupPasswordLessLoginTimeout;

   public SetVMPasswordSP(String nodeIP, String password) {
      this.nodeIP = nodeIP;
      this.password = password;

      this.sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      this.sshPort = Configuration.getInt(Constants.SSH_PORT_CONFIG_NAME, Constants.DEFAULT_SSH_PORT);
      String keyFileName= Configuration.getString(Constants.SSH_PRIVATE_KEY_CONFIG_NAME, Constants.SSH_PRIVATE_KEY_FILE_NAME);
      String serengetiHome = Configuration.getString(Constants.SERENGETI_HOME, Constants.DEFAULT_SERENGETI_HOME);
      this.privateKeyFile = serengetiHome + "/.ssh/" + keyFileName;
      this.setupPasswordLessLoginTimeout = Configuration.getInt(Constants.PASSWORDLESS_LOGIN_TIMEOUT, SETUP_PASSWORDLESS_LOGIN_TIMEOUT);
   }

   @Override
   public Void call() throws Exception {
      setPasswordForNode();
      return null;
   }

   private boolean setPasswordForNode() throws Exception {
      logger.info("Setting password of " + nodeIP);

      setupPasswordLessLogin(nodeIP);

      //Release build: if user set customized password, set the customized password for it
      //               if user didn't set customized password, set random password for it
      //Beta build:    if user set customized password, set the customized password for it
      //               if user didn't set password, use default password
      String buildType = Configuration.getString(Constants.SERENGETI_BUILD_TYPE, Constants.RELEASE_BUILD);
      if (buildType.equalsIgnoreCase(Constants.RELEASE_BUILD)) {
         if (this.password == null) {
            if (setRandomPassword() == false) {
               logger.error("Set random password for " + nodeIP + " failed.");
               return false;
            }
         } else {
            if (setCustomizedPassword(password) == false) {
               logger.error("Set customized password for " + nodeIP + " failed.");
               return false;
            }
         }
      } else if (buildType.equalsIgnoreCase(Constants.BETA_BUILD)) {
         if (this.password != null) {
            if (setCustomizedPassword(password) == false) {
               logger.error("Set customized password for " + nodeIP + " failed.");
               return false;
            }
         }
      }

      if (!removeSSHLimit()) {
         logger.error("Remove ssh limit for " + nodeIP + " failed.");
         return false;
      }

      return true;
   }

   private boolean removeSSHLimit() {
      String scriptFileName = Configuration.getString(Constants.REMOVE_SSH_LIMIT_SCRIPT, Constants.DEFAULT_REMOVE_SSH_LIMIT_SCRIPT);
      String script = getScriptName(scriptFileName);
      String cmd = "sudo " + script;
      boolean removeSSHLimitSucceed = false;
      SSHUtil sshUtil = new SSHUtil();
      removeSSHLimitSucceed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, null, null);

      logger.info("Remove ssh limit for " + nodeIP + (removeSSHLimitSucceed ? " succeed" : " failed") + ".");
      return removeSSHLimitSucceed;
   }

   private boolean setRandomPassword() throws Exception {
      logger.info("Setting random password for " + nodeIP);
      String scriptFileName = Configuration.getString(Constants.SET_PASSWORD_SCRIPT_CONFIG_NAME, Constants.DEFAULT_SET_PASSWORD_SCRIPT);
      String script = getScriptName(scriptFileName);
      String cmd = "sudo " + script + " -a";
      return setPassword(cmd, null);
   }

   private boolean setCustomizedPassword(String password) throws Exception {
      logger.info("Setting customized password for " + nodeIP);
      String cmd = generateSetPasswdCommand(Constants.SET_PASSWORD_SCRIPT_CONFIG_NAME);
      InputStream in = null;
      try {
         in = parseInputStream(new String(password + Constants.NEW_LINE + password + Constants.NEW_LINE));
         return setPassword(cmd, in);
      } finally {
         if (in != null) {
            in.close();
         }
      }
   }

   private boolean setPassword(String cmd, InputStream in) throws Exception {
      boolean setPasswordSucceed = false;
      for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
         SSHUtil sshUtil = new SSHUtil();
         setPasswordSucceed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, in, null);
         if (setPasswordSucceed) {
            //if refresh failed, user still can manually refresh tty by Ctrl+C, so don't need to check whether
            //it succeed or not
            refreshTty();
            break;
         } else {
            logger.info("Set password for " + nodeIP + " failed for " + (i + 1)
                  + " times. Retrying after 2 seconds....");
            try {
               Thread.sleep(2000);
            } catch (InterruptedException e) {
               logger.info("Sleep interrupted, retrying immediately");
            }
         }
      }

      if (setPasswordSucceed) {
         logger.info("Set password for " + nodeIP + " succeed");
         return true;
      } else {
         logger.info("set password for " + nodeIP + " failed");
         throw new Exception(Constants.SET_PASSWORD_FAILED + " for " + nodeIP);
      }
   }

   private void setupPasswordLessLogin(String hostIP) throws Exception {
      String scriptName = Configuration.getString(Constants.PASSWORDLESS_LOGIN_SCRIPT, Constants.DEFAULT_PASSWORDLESS_LOGIN_SCRIPT);
      String script = getScriptName(scriptName);

      String user = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      String password = Configuration.getString(Constants.SERENGETI_DEFAULT_PASSWORD);
      String cmd = script + " " + hostIP + " " + user + " " + password;
      for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
         try {
            ShellCommandExecutor.execCmd(cmd, null, null, this.setupPasswordLessLoginTimeout, Constants.MSG_SETTING_UP_PASSWORDLESS_LOGIN + hostIP + ".");
            logger.info("Set passwordless login successfully for " + hostIP);
            return;
         } catch (BddException e) {
            logger.warn("Set passwordless login no. " + i + " and got exception: " + e.getMessage());
         }
      }
      logger.error("Failed to set passwordless login for " + hostIP);
      throw new Exception(Constants.SET_PASSWORDLESS_LOGIN_FAILED + hostIP);
   }

   private boolean refreshTty() {
      String ttyName = Configuration.getString(Constants.SERENGETI_TTY_NAME, Constants.SERENGETI_DEFAULT_TTY_NAME);
      String cmd = "ps aux | grep " + ttyName + " | grep -v \"grep\" | awk '{print $2}' | sudo xargs kill -9";
      SSHUtil sshUtil = new SSHUtil();
      boolean refreshTtySucceed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, null, null);
      logger.debug("Refresh " + ttyName + " on " + nodeIP + (refreshTtySucceed ? "succeed" : "failed") + ".");
      return refreshTtySucceed;
   }

   public ByteArrayInputStream parseInputStream(String in)throws Exception
   {
       ByteArrayInputStream input=new ByteArrayInputStream(in.getBytes());
       return input;
   }

   private String generateSetPasswdCommand(String setPasswdScriptConfig) {
      String scriptFileName = Configuration.getString(setPasswdScriptConfig, Constants.DEFAULT_SET_PASSWORD_SCRIPT);
      String script = getScriptName(scriptFileName);
      return "sudo " + script + " -u";
   }

   private String getScriptName(String scriptFileName) {
      String serengetiSbinDir = Configuration.getString(Constants.SERENGETI_SBIN_DIR, Constants.DEFAULT_SERENGETI_SBIN_DIR);
      return serengetiSbinDir + "/" + scriptFileName;
   }

   public String getNodeIP() {
      return nodeIP;
   }

   public void setNodeIP(String nodeIP) {
      this.nodeIP = nodeIP;
   }
}
