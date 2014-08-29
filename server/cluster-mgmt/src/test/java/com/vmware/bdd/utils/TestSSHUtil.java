/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

import com.google.common.io.Files;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;

public class TestSSHUtil {
   private final String username = "serengeti";
   private final String hostIP = "127.0.0.1";
   private final String privateKeyFile = "src/test/resources/id_rsa";
   private final String tempPrivateKeyFile = privateKeyFile + ".temp";
   private final int port = 22000;

   private SshServer sshd;

   @BeforeMethod(alwaysRun = true)
   public void setUp() throws Exception {
      deleteFile(tempPrivateKeyFile);
      Files.copy(new File(privateKeyFile), new File(tempPrivateKeyFile));
      setupSSHServer();
   }

   @AfterMethod(alwaysRun = true)
   public void tearDown() {
      try {
         sshd.stop(true);
      } catch (InterruptedException e) {}
      deleteFile(tempPrivateKeyFile);
   }

   private void setupSSHServer() throws IOException {
      sshd = SshServer.setUpDefaultServer();
      sshd.setFileSystemFactory(new NativeFileSystemFactory());
      sshd.setPort(port);
      sshd.setShellFactory(new ProcessShellFactory());
      sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(tempPrivateKeyFile));
      sshd.setCommandFactory(new ScpCommandFactory());
      sshd.setCommandFactory(new ScpCommandFactory(new CommandFactory() {
         public Command createCommand(String command) {
            return new ProcessShellFactory(command.split(" ")).create();
         }
      }));
      sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
         @Override
         public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) {
            return true;
         }
      });
      sshd.start();
   }

   @Test
   public void testExecCmd() throws Exception {
      SSHUtil sshUtil = new SSHUtil();
      Assert.assertTrue(sshUtil.execCmd(username, tempPrivateKeyFile, hostIP, port, "hostname", null, null));
      new File(tempPrivateKeyFile).delete();
   }

   @Test
   public void testExecCmdWithoutPrivateKeyFile() {
      SSHUtil sshUtil = new SSHUtil();
      boolean getException = false;
      try {
         sshUtil.execCmd(username, null, hostIP, port, "hostname", null, null);
      } catch (Exception e) {
         getException = true;
      }
      Assert.assertTrue(getException);
   }

   private void deleteFile(String fileName) {
      File tempFile = new File(fileName);
      if (tempFile.exists()) {
         if (!tempFile.delete()) {
            System.out.println("delete file :" + tempFile.getAbsolutePath() + " failed");
         }
      }
   }
}
