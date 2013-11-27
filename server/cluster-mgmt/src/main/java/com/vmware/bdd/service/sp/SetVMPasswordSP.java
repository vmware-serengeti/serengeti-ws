/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
import java.io.OutputStream;
import java.util.concurrent.Callable;

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

   public SetVMPasswordSP(String nodeIP, String password) {
      this.nodeIP = nodeIP;
      this.password = password;
   }

   @Override
   public Void call() throws Exception {
      setPasswordForNode();
      return null;
   }

   private boolean setPasswordForNode() throws Exception {
      logger.info("Setting password of " + nodeIP);

      String privateKeyFile =
            Configuration.getString(Constants.SSH_PRIVATE_KEY_CONFIG_NAME, Constants.SSH_PRIVATE_KEY_FILE_NAME);
      String sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      int sshPort = Configuration.getInt(Constants.SSH_PORT_CONFIG_NAME, Constants.DEFAULT_SSH_PORT);

      String cmd = generateSetPasswdCommand(Constants.SET_PASSWORD_SCRIPT_CONFIG_NAME, password);

      InputStream in = null;
      try {
         in = parseInputStream(new String(password + Constants.NEW_LINE + password + Constants.NEW_LINE));
         boolean setPasswordSucceed = false;
         for (int i = 0; i < Constants.SET_PASSWORD_MAX_RETRY_TIMES; i++) {
            SSHUtil sshUtil = new SSHUtil();
            setPasswordSucceed = sshUtil.execCmd(sshUser, privateKeyFile, nodeIP, sshPort, cmd, in, null);
            if (setPasswordSucceed) {
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
            logger.info("set password for " + nodeIP + " succeed");
            return true;
         } else {
            logger.info("set password for " + nodeIP + " failed");
            throw new Exception(Constants.CHECK_WHETHER_SSH_ACCESS_AVAILABLE);
         }
      } finally {
         if (in != null) {
            in.close();
         }
      }
   }

   public ByteArrayInputStream parseInputStream(String in)throws Exception
   {
       ByteArrayInputStream input=new ByteArrayInputStream(in.getBytes());
       return input;
   }

   private String generateSetPasswdCommand(String setPasswdScriptConfig, String password) {
      String scriptFileName = Configuration.getString(setPasswdScriptConfig, Constants.DEFAULT_SET_PASSWORD_SCRIPT);
      return "sudo " + scriptFileName + " -u";
   }

   public String getNodeIP() {
      return nodeIP;
   }

   public void setNodeIP(String nodeIP) {
      this.nodeIP = nodeIP;
   }
}
