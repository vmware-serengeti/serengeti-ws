/******************************************************************************
 *   Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
import java.util.List;

import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
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

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   private int TIMEOUT = Configuration.getInt("usermgmt.command.exec.timeout", 60);



   public NodeLdapUserMgmtConfService() {
      this.sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
      this.sshPort = Configuration.getInt(Constants.SSH_PORT_CONFIG_NAME, Constants.DEFAULT_SSH_PORT);
   }

   public void disableLocalUsers(List<NodeEntity> nodeEntityList) {
      String sudoCmd = CommonUtil.getCustomizedSudoCmd();
      String[] remoteCmds = new String[]{
            sudoCmd + " usermod -L serengeti",
            sudoCmd + " usermod -L root"
      };

      for(NodeEntity nodeEntity : nodeEntityList) {
         clusterEntityManager.updateNodeAction(nodeEntity, "Disabling Local Users");
         try {
            sshExecService.exec(nodeEntity.getPrimaryMgtIpV4(), sshPort, sshUser, remoteCmds, TIMEOUT);
            nodeEntity.setAction("Disable Local Users successfully!");
         } catch (Exception e) {
            LOGGER.error("failed to disable local users for node: " + nodeEntity, e);
            nodeEntity.setAction("Disable Local Users failed!");
            nodeEntity.setActionFailed(true);
         } finally {
            clusterEntityManager.update(nodeEntity);
         }
      }
   }

   public void configureLdap(List<NodeEntity> nodeEntityList, String localSssdConfFile, String adminGroupName) {
      String uploadedSssdConfFilePath = "/tmp/sssd.conf." + System.currentTimeMillis();
      String sudoCmd = CommonUtil.getCustomizedSudoCmd();
      String[] remoteCmds = new String[]{
            sudoCmd + " " + UserMgmtConstants.CONFIG_LDAP_SCRIPT + " " + uploadedSssdConfFilePath
      };
      String enableSudoCmd = "echo 'Do not need to enable sudo'";
      if (adminGroupName != null) {
         enableSudoCmd = sudoCmd + " " + UserMgmtConstants.ENABLE_SUDO_SCRIPT  + " " + adminGroupName;
      }
      String[] enableSudoCmds = new String[] {enableSudoCmd};

      for(NodeEntity nodeEntity : nodeEntityList) {
         clusterEntityManager.updateNodeAction(nodeEntity, "Enabling LDAP");
         try {
            transferFile(localSssdConfFile, nodeEntity.getPrimaryMgtIpV4(), uploadedSssdConfFilePath);
            sshExecService.exec(nodeEntity.getPrimaryMgtIpV4(), sshPort, sshUser, remoteCmds, TIMEOUT);
            sshExecService.exec(nodeEntity.getPrimaryMgtIpV4(), sshPort, sshUser, enableSudoCmds, TIMEOUT);

            nodeEntity.setAction("Enable LDAP succeeded");
         } catch (Exception e) {
            //Todo(qjin: error handling need to be enhanced here.)
            LOGGER.error("failed to configure LDAP for node: " + nodeEntity, e);
            nodeEntity.setAction("Enable LDAP failed");
            nodeEntity.setActionFailed(true);
            nodeEntity.setErrMessage(e.getMessage());
         } finally {
            clusterEntityManager.update(nodeEntity);
         }
      }
   }

   private void transferFile(String srcFilePath, String ip, String targetFilePath) {
      CommandLine cmdLine = new CommandLine("scp")
            .addArgument(srcFilePath)
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

   public IClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }
}
