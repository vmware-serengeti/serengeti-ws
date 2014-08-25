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

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.SetLocalRepoException;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.ShellCommandExecutor;

/**
 * Store Procedure of setting local repository on a node vm for application managers like CM/Ambari.
 */
public class SetLocalRepoSP implements Callable<Void> {

   private static final Logger logger = Logger.getLogger(SetLocalRepoSP.class);
   private NodeEntity node;
   private String nodeIP;
   private String repoId;
   private String localRepoURL;
   private int setupLocalRepoTimeout;
   private String sshUser;

   public SetLocalRepoSP(NodeEntity node, String repoId, String localRepoURL) {
      this.node = node;
      this.repoId = repoId;
      this.localRepoURL = localRepoURL;
      this.setupLocalRepoTimeout =
            Configuration.getInt(Constants.NODE_SET_LOCAL_REPO_TIMEOUT_SECONDS,
                  Constants.NODE_SET_LOCAL_REPO_TIMEOUT_SECONDS_DEFAULT);

      this.sshUser =
            Configuration.getString(Constants.SSH_USER_CONFIG_NAME,
                  Constants.DEFAULT_SSH_USER_NAME);
      this.nodeIP = node.getPrimaryMgtIpV4();

   }

   @Override
   public Void call() throws Exception {
      setupNodeLocalRepo();
      return null;
   }

   public boolean setupNodeLocalRepo() throws Exception {
      String setupLocalRepoScriptName =
            Configuration.getString(
                  Constants.NODE_APPMANAGER_SETUP_LOCAL_REPO_SCRIPT,
                  Constants.NODE_APPMANAGER_SETUP_LOCAL_REPO_SCRIPT_DEFAULT);
      String sbinDir =
            Configuration.getString(Constants.SERENGETI_SBIN_DIR,
                  Constants.DEFAULT_SERENGETI_SBIN_DIR);

      String setupLocalRepoScript = sbinDir + "/" + setupLocalRepoScriptName;
      String cmd =
            "sudo " + setupLocalRepoScript + " " + repoId + " " + localRepoURL;
      String action = "Setup local repo for " + nodeIP;

      logger.info(action + " command is: " + cmd);
      String errMsg = null;

      for (int i = 0; i < Constants.SET_LOCAL_REPO_MAX_RETRY_TIMES; i++) {
         try {
            // add the write permission on the directory /etc/yum.repos.d/
            String setLocalRepoCommand =
                  "ssh -tt " + sshUser + "@" + nodeIP + " '" + cmd + "'";

            logger.info("The command to remotely execute script of local repo setting is: "
                  + setLocalRepoCommand);
            ShellCommandExecutor.execCmd(setLocalRepoCommand, null, null,
                  setupLocalRepoTimeout,
                  Constants.NODE_ACTION_CHANGE_REPO_DIR_PERMISSION);

            return true;
         } catch (Exception e) {
            logger.warn("Got exception when " + action, e);
            if (errMsg == null) {
               errMsg = e.getMessage();
            }
         }

         try {
            Thread.sleep(3000);
         } catch (InterruptedException e1) {
            logger.info("Interrupted when waiting for local repo setup, retry immediately...");
         }
      }

      logger.info(action + " failed");
      throw SetLocalRepoException.FAIL_TO_SET_LOCAL_REPO(action, errMsg);
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

   public NodeEntity getNode() {
      return node;
   }

   public void setNode(NodeEntity node) {
      this.node = node;
   }

   public String getRepoId() {
      return repoId;
   }

   public void setRepoId(String repoId) {
      this.repoId = repoId;
   }

   public String getLocalRepoURL() {
      return localRepoURL;
   }

   public void setLocalRepoURL(String localRepoURL) {
      this.localRepoURL = localRepoURL;
   }

   public String getSshUser() {
      return sshUser;
   }

   public void setSshUser(String sshUser) {
      this.sshUser = sshUser;
   }

}
