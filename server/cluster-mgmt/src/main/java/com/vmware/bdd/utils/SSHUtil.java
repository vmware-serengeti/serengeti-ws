/***************************************************************************
 * Copyright (c) 2013-2014 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.utils;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHUtil {
   private JSch jsch;
   private Session session;
   private final Logger logger = Logger.getLogger(SSHUtil.class);

   private void connect(String user, String privateKeyFile,
         String hostIP, int sshPort) {
      jsch = new JSch();

      try {
         session = jsch.getSession(user, hostIP, sshPort);
         jsch.addIdentity(privateKeyFile);
         java.util.Properties config = new java.util.Properties();
         config.put("StrictHostKeyChecking", "no");
         session.setConfig(config);
         session.setTimeout(Constants.SSH_SESSION_TIMEOUT);
         session.connect();
         logger.debug("SSH session is connected!");
      } catch (JSchException e) {
         e.printStackTrace();
      }
   }

   public boolean execCmd(String user, String privateKeyFile,
         String hostIP, int sshPort, String command, InputStream in, OutputStream out) {
      AuAssert.check(command != null);

      connect(user, privateKeyFile, hostIP, sshPort);

      ChannelExec channel = null;
      logger.info("going to exec command");
      try {
         channel = (ChannelExec) session.openChannel("exec");

         if (channel != null) {
            logger.debug("SSH channel is opened!");
            channel.setPty(true); // to enable sudo
            channel.setCommand(command);
            channel.setInputStream(in);
            channel.setOutputStream(out);

            channel.connect();
            if (!channel.isConnected()) {
               logger.error("Cannot setup SSH channel connection.");
            }

            while (true) {
               if (channel.isClosed()) {
                  int exitStatus = channel.getExitStatus();
                  logger.debug("Exit status from exec is: " + exitStatus);

                  if (exitStatus == 0) {
                     return true;
                  }
                  return false;
               }

               //to avoid CPU busy
               try {
                  Thread.sleep(200);
               } catch (InterruptedException e) {
               }
            }
         } else {
            logger.error("Cannot open SSH channel to " + hostIP + ".");
            return false;
         }
      } catch (JSchException e) {
         e.printStackTrace();
      } finally {
         if (channel != null && channel.isConnected()) {
            channel.disconnect();
         }

         if (session != null && session.isConnected()) {
            session.disconnect();
         }
      }
      return false;
   }
}
