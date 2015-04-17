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
package com.vmware.bdd.ssh;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Created By xiaoliangl on 1/4/15.
 */
@Component
public class SshExecService {
   private static final Logger LOGGER = Logger.getLogger(SshExecService.class);

   private SSHClient sshClient;

   public void exec(String host, int port, String userName, String[] cmds, int timeoutInSecond) {
      sshClient = new SSHClient();
      try {
         sshClient.addHostKeyVerifier(new HostKeyVerifier() {
            @Override
            public boolean verify(String hostname, int port, PublicKey key) {
               return true;
            }
         });

         sshClient.connect(host, port);
         sshClient.authPublickey(userName);

         for (String cmd : cmds) {
            exec(sshClient, cmd, timeoutInSecond);
         }

      } catch (IOException e) {
         throw new SshExecException("failed connect via ssh", e);
      } finally {
         try {
            sshClient.close();
         } catch (IOException e) {
            //do nothing
         }
      }

   }

   private String readFromStream(InputStream in) throws IOException {
      StringBuffer result = new StringBuffer();
      BufferedInputStream inputStream = new BufferedInputStream(in);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      while(bufferedReader.ready()) {
         result.append(bufferedReader.readLine());
      }
      return result.toString();
   }

   protected void exec(SSHClient client, String cmd, int timeoutInSecond) throws IOException {
      LOGGER.debug("exec remote cmd via ssh: " + cmd);

      Session session = null;
      try {
         session = client.startSession();
         session.allocateDefaultPTY();

         Session.Command command = session.exec(cmd);
         LOGGER.debug(IOUtils.readFully(command.getInputStream()).toString());

         command.join(timeoutInSecond, TimeUnit.SECONDS);

         int exitStatus = command.getExitStatus();
         LOGGER.debug("cmd exit status: " + exitStatus);
         if(exitStatus != 0) {
            StringBuilder output = new StringBuilder();
            output.append(readFromStream(command.getErrorStream()));
            output.append(readFromStream(command.getInputStream()));
            //@TODO improve error handling
            throw new SshExecException("exec failed with exit code: " + exitStatus + output.toString(), null);
         }
         LOGGER.info("cmd exec successfully!");
      } catch (IOException e) {
         throw new SshExecException("failed exec", e);
      } finally {
         if (session != null) {
            try {
               session.close();
            } catch (IOException e) {
               //do nothing
            }
         }
      }
   }
}
