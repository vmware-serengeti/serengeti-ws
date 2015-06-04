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
package com.vmware.bdd.service.sp;

import java.util.concurrent.Callable;

import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.ShellCommandExecutor;

public class NodeGenerateHostnameSP implements Callable<Void> {

   private static final Logger logger = Logger.getLogger(NodeGenerateHostnameSP.class);

   private NodeEntity node;
   private String nodeMgtIp;
   private String scriptForUpdatingEtcHosts;
   private static final int connTimeoutInSec = 600;

   public NodeGenerateHostnameSP(NodeEntity node, String scriptForUpdatingEtcHosts) {
      this.node = node;
      this.nodeMgtIp = node.getPrimaryMgtIpV4();
      this.scriptForUpdatingEtcHosts = scriptForUpdatingEtcHosts;
   }

   @Override
   public Void call() throws Exception {
      generateHostnameFornode();
      return null;
   }

   private void generateHostnameFornode() throws Exception {
      logger.info("Generating hostname for cluster node " + node.getVmName());

      try {
         String sshUser = getSshUser();
         String originalFilePath = Constants.SRCIPT_FOR_UPDATING_ETC_HOSTS_DIR + scriptForUpdatingEtcHosts;
         String targetFilePath = "/tmp/" + scriptForUpdatingEtcHosts;

         // Upload script file to node
         String uploadScriptFileCommand = "scp " + originalFilePath + " " + sshUser + "@" + nodeMgtIp + ":" + targetFilePath;
         ShellCommandExecutor.execCmd(uploadScriptFileCommand, null, null, connTimeoutInSec, Constants.NODE_ACTION_GENERATE_HOSTNAME);

         // Run script file on node
         String sudoCmd = CommonUtil.getCustomizedSudoCmd();
         String generateHostnameCmd = "ssh -tt " + sshUser + "@" + nodeMgtIp + " 'set -e; chmod +x " + targetFilePath + "; " + sudoCmd + " bash " + targetFilePath + "; rm -rf " + targetFilePath + "; set +e;'";
         ShellCommandExecutor.execCmd(generateHostnameCmd, null, null, connTimeoutInSec, Constants.NODE_ACTION_GENERATE_HOSTNAME);
      } catch (Exception e) {
         logger.error("Failed to generate hostname of cluster node " + node.getVmName());
         throw BddException.FAILED_TO_GENERATE_HOSTNAME(e, e.getMessage());
      }
   }

   private String getSshUser() {
      return Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);
   }

   public NodeEntity getNode() {
      return node;
   }

}
