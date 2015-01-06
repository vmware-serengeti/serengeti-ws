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
package com.vmware.bdd.service.impl;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.ssh.SshExecService;
import com.vmware.bdd.usermgmt.job.ExecOutputLogger;
import com.vmware.bdd.utils.Constants;

/**
 * Created By xiaoliangl on 12/31/14.
 */
@Component
public class NodeLdapUserMgmtConfService {
   private static final Logger LOGGER = Logger.getLogger(NodeLdapUserMgmtConfService.class);

   private String sshUser;
   private int sshPort;

   private SshExecService sshExecService = new SshExecService();

   private int TIMEOUT = Configuration.getInt("usermgmt.command.exec.timeout", 60);



   public NodeLdapUserMgmtConfService() {
      this.sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      this.sshPort = Configuration.getInt(Constants.SSH_PORT_CONFIG_NAME, Constants.DEFAULT_SSH_PORT);
   }

   public void disableLocalUsers(String[] ips) {
      String[] remoteCmds = new String[]{
            "sudo usermod -L serengeti",
            "sudo usermod -L root"
      };

      for(String ip : ips) {
         try {
            sshExecService.exec(ip, sshPort, sshUser, remoteCmds, TIMEOUT);
         } catch (Exception e) {
            LOGGER.error("failed to disable local users for node: " + ip, e);
         }
      }
   }

   public void configureSssd(String[] ips, String localSssdConfFile) {
      String uploadedSssdConfFilePath = "/tmp/sssd.conf." + System.currentTimeMillis();
      String[] remoteCmds = new String[]{
            String.format("sudo mv -f %1s /etc/sssd/sssd.conf", uploadedSssdConfFilePath),
            "sudo chown root:root /etc/sssd/sssd.conf",
            "sudo chmod 600 /etc/sssd/sssd.conf",
            "sudo authconfig --enablesssd --enablesssdauth --enablemkhomedir --updateall",
            "sudo service sssd restart"
      };

      for(String ip : ips) {
         try {
            transferSssdConf(localSssdConfFile, ip, uploadedSssdConfFilePath);

            sshExecService.exec(ip, sshPort, sshUser, remoteCmds, TIMEOUT);
         } catch (Exception e) {
            LOGGER.error("failed to configure sssd for node: " + ip, e);
         }
      }
   }

   private void transferSssdConf(String specFilePath, String ip, String targetFilePath) {
      CommandLine cmdLine = new CommandLine("scp")
            .addArgument(specFilePath)
            .addArgument(ip + ":" + targetFilePath);

      DefaultExecutor executor = new DefaultExecutor();

      executor.setStreamHandler(new PumpStreamHandler(
            new ExecOutputLogger(LOGGER, false), //output logger
            new ExecOutputLogger(LOGGER, true)) //error logger
      );

      executor.setWatchdog(new ExecuteWatchdog(1000l * 120l));

      try {
         int exitVal = executor.execute(cmdLine);
         if(exitVal != 0) {
            throw new RuntimeException("CFG_LDAP_FAIL", null);
         }
      } catch (IOException e) {
         throw new RuntimeException("CFG_LDAP_FAIL", e);
      }
   }
}
