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

import com.jcraft.jsch.JSchException;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.utils.ShellCommandExecutor;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.SSHUtil;
import com.vmware.bdd.exception.SetPasswordException;
/**
 * Store Procedure of setting password for a vm
 */
public class SetVMPasswordSP implements Callable<Void> {

   private static final Logger logger = Logger.getLogger(SetVMPasswordSP.class);
   private NodeEntity node;
   private String nodeIP;
   private String password;

   private String privateKeyFile;
   private String sshUser;
   private int sshPort;
   private final static int SETUP_PASSWORDLESS_LOGIN_TIMEOUT = 120; //in seconds
   private int setupPasswordLessLoginTimeout;

   public SetVMPasswordSP(NodeEntity node, String password) {
      this.node = node;
      this.nodeIP = node.getPrimaryMgtIpV4();
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

   public boolean setPasswordForNode() throws Exception {
      logger.info("Setting password of " + nodeIP);

      setupPasswordLessLogin(nodeIP);

      // if user set customized password, set the customized password for it
      // if user didn't set customized password, set random password for it
      if (this.password == null) {
         setRandomPassword();
      } else {
         if (CommonUtil.passwordContainInvalidCharacter(password)) {
            logger.error("Set customized password for " + nodeIP + " failed. Password contains invalid characters");
            throw SetPasswordException.PASSWORD_CONTAIN_INVALID_CHARACTER();
         }
         setCustomizedPassword(password);
      }
      removeSSHLimit();

      return true;
   }

   private boolean removeSSHLimit() throws Exception {
      String scriptFileName = Configuration.getString(Constants.REMOVE_SSH_LIMIT_SCRIPT, Constants.DEFAULT_REMOVE_SSH_LIMIT_SCRIPT);
      String script = getScriptName(scriptFileName);
      String cmd = "sudo " + script;
      boolean succeed = false;
      SSHUtil sshUtil = new SSHUtil();
      for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
         try {
            succeed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, null, null);
            if (succeed) {
               logger.info("Remove ssh limit for " + nodeIP + " succceed");
               return true;
            }
         } catch (JSchException e) {
            logger.warn("Caught exception when remove ssh limit for " + nodeIP);
         }
      }
      throw SetPasswordException.FAIL_TO_REMOVE_SSH_LIMIT(nodeIP);
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
      int jschExceptionCount = 0;
      for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
         try {
            SSHUtil sshUtil = new SSHUtil();
            setPasswordSucceed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, in, null);
         } catch (JSchException e) {
            if (++jschExceptionCount == Constants.SET_PASSWORD_MAX_RETRY_TIMES) {
               throw SetPasswordException.GOT_JSCH_EXCEPTION_WHEN_SET_PASSWORD(e, nodeIP);
            }
         }
         if (setPasswordSucceed) {
            handleTty();
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
         logger.error("set password for " + nodeIP + " failed");
         throw SetPasswordException.FAIL_TO_SET_PASSWORD(nodeIP, null);
      }
   }

   private boolean setupPasswordLessLogin(String hostIP) throws Exception {
      String scriptName = Configuration.getString(Constants.PASSWORDLESS_LOGIN_SCRIPT, Constants.DEFAULT_PASSWORDLESS_LOGIN_SCRIPT);
      String script = getScriptName(scriptName);

      String user = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      String password = Configuration.getString(Constants.SERENGETI_DEFAULT_PASSWORD);
      String cmd = script + " " + hostIP + " " + user + " " + password;
      int sleepTime = Configuration.getInt(Constants.SSH_SLEEP_TIME_BEFORE_RETRY, Constants.DEFAULT_SSH_SLEEP_TIME_BEFORE_RETRY);
      int timeoutCount = 0;
      for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
         try {
            ShellCommandExecutor.execCmd(cmd, null, null, this.setupPasswordLessLoginTimeout, Constants.MSG_SETTING_UP_PASSWORDLESS_LOGIN + hostIP + ".");
            logger.info("Set passwordless login successfully for " + hostIP);
            return true;
         } catch (Exception e) {
            if (e.getMessage().contains(Constants.EXEC_COMMAND_TIMEOUT)) {
               timeoutCount++;
            }
            logger.warn("Set passwordless login no. " + i + " and got exception: " + e.getMessage(), e);
            //sometimes the sshd daemon may not ready, add sleep can avoid the race. Now, the longest wait time is 150s
            try {
               Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
               logger.warn("Interrupted when waiting for setupPasswordlessLogin, retry immediately.", ie);
            }
         }
      }
      logger.error("Failed to set passwordless login for " + hostIP);
      if (timeoutCount == Constants.SET_PASSWORD_MAX_RETRY_TIMES) {
         throw SetPasswordException.SETUP_PASSWORDLESS_LOGIN_TIMEOUT(null, hostIP);
      }
      throw SetPasswordException.FAIL_TO_SETUP_PASSWORDLESS_LOGIN(hostIP);
   }

   private void handleTty() throws Exception {
      setupLoginTty();
      refreshTty();
   }

   private void refreshTty() {
      String ttyName = Configuration.getString(Constants.SERENGETI_TTY_NAME, Constants.SERENGETI_DEFAULT_TTY_NAME);
      String cmd = "ps aux | grep " + ttyName + " | grep -v \"grep\" | awk '{print $2}' | sudo xargs kill -9";
      SSHUtil sshUtil = new SSHUtil();
      //if refresh failed, user still can manually refresh tty by Ctrl+C, so don't need to check whether
      //it succeed or not
      try {
         boolean refreshTtySucceed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, null, null);
         logger.info("Refresh " + ttyName + " on " + nodeIP + (refreshTtySucceed ? "succeed" : "failed") + ".");
      } catch (JSchException e) {
         logger.error("Got exception when refresh tty on " + nodeIP, e);
      }
   }

   private void setupLoginTty() throws Exception {
      String setupTtyScriptName = Configuration.getString(Constants.SERENGETI_SETUP_LOGIN_TTY_SCRIPT, Constants.SERENGETI_DEFAULT_SETUP_LOGIN_TTY_SCRIPT);
      String setupTtyScript = getScriptName(setupTtyScriptName);
      String cmd = "sudo " + setupTtyScript;
      String action = "Setup login tty for " + nodeIP;
      logger.info(action + " command is: " + cmd);
      SSHUtil sshUtil = new SSHUtil();
      String errMsg = null;
      for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
         try {
            if (sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, null, null)) {
               logger.info(action + " succeed.");
               return;
            }
         } catch (JSchException e) {
            logger.warn("Got exception when " + action, e);
            if (errMsg == null) {
               errMsg = e.getMessage();
            }
         }
         try {
            Thread.sleep(3000);
         } catch (InterruptedException e1) {
            logger.info("Interrupted when waiting for setup login tty, retry immediately...");
         }
      }
      logger.info(action + " failed");
      throw SetPasswordException.FAIL_TO_SETUP_LOGIN_TTY(nodeIP, errMsg);
   }

   public ByteArrayInputStream parseInputStream(String in) throws Exception {
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

   public NodeEntity getNodeEntity() {
      return node;
   }
}
