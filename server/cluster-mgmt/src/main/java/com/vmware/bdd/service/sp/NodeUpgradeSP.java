/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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

import java.util.Map;
import java.util.concurrent.Callable;

import com.vmware.bdd.ssh.SshExecService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ShellCommandExecutor;
import org.apache.log4j.Logger;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

/**
 * Store Procedure for start a vm
 */

public class NodeUpgradeSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(NodeUpgradeSP.class);

   private NodeEntity node;
   private String serverVersion;
   private static final int connTimeoutInSec = 600;

   public NodeUpgradeSP(NodeEntity node, String serverVersion) {
      this.node = node;
      this.serverVersion = serverVersion;
   }

   @Override
   public Void call() throws Exception {
      upgrade();
      return null;
   }

   //For node upgrade, we only support N ~ N+1 upgrade
   private void upgrade() throws Exception {

      logger.info("Upgrading cluster node " + node.getVmName());

      if (node.getMoId() != null) {
         if (node.canBeUpgrade()) {
            upgradeNode();
         } else {
            setBootupUUID();
         }
      }

   }

   private boolean needUpgradedToM10() {
      //TODO(qjin): check mount point format on node to decide whether need to upgrade
      return true;
   }

   public NodeEntity getNode() {
      return node;
   }

   private void upgradeNode(){
      if (serverVersion.equalsIgnoreCase(Constants.BDE_SERVER_VERSION_2_2)) {
         if (!needUpgradedToM10()) {
            return;
         }
      }

      String nodeVmName = node.getVmName();
      String nodeIp = node.getPrimaryMgtIpV4();

      String sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);

      try {

         // Copy node upgrade tarball to node
         String uploadTarballCommand = "scp " + Constants.NODE_UPGRADE_FILE_PATH
               + Constants.NODE_UPGRADE_TARBALL_FILE_NAME + " " + sshUser + "@" + nodeIp + ":/tmp/";
         ShellCommandExecutor.execCmd(uploadTarballCommand, null, null,
               connTimeoutInSec, Constants.NODE_ACTION_DOWNLOAD_PACKAGES);

         // Copy node upgrade script file to node
         String uploadScriptFileCommand = "scp " + Constants.NODE_UPGRADE_FILE_PATH
               + Constants.NODE_UPGRADE_SCRIPT_FILE_NAME + " " + sshUser + "@" + nodeIp + ":/tmp/";
         ShellCommandExecutor.execCmd(uploadScriptFileCommand, null, null,
               connTimeoutInSec, Constants.NODE_ACTION_DOWNLOAD_PACKAGES);

         // Upgrade cluster node
         String sudoCmd = CommonUtil.getCustomizedSudoCmd();
         String upgradeNodeCommand = "ssh -tt " + sshUser + "@" + nodeIp + " '" + sudoCmd + " bash /tmp/" + Constants.NODE_UPGRADE_SCRIPT_FILE_NAME + "'";
         ShellCommandExecutor.execCmd(upgradeNodeCommand, null, null,
               0, Constants.NODE_ACTION_INSTALL_PACKAGES);

      } catch (Exception e) {
         logger.error("Failed to run upgrade script of cluster node " + nodeVmName);
         throw BddException.UPGRADE(e, e.getMessage());
      }
   }

   private void setBootupUUID() {
      VcContext.inVcSessionDo(new VcSession<Void>() {
         @Override
         protected Void body() throws Exception {
            if (node.getMoId() == null) {
               return null;
            }
            VcVirtualMachine vcVm = VcCache.getIgnoreMissing(String.valueOf(node.getMoId()));
            if (vcVm == null) {
               return null;
            }
            Map<String, String> bootupConfigs = vcVm.getGuestConfigs();
            AuAssert.check(bootupConfigs != null);

            /* serengeti operation flag - a random generated uuid
            * a script inside the vm compares this uuid with its stored value, if they are
            * different, this VM decides it's started by Serengeti, otherwise, it's started
            * by third parties.
            */
            VcVmUtil.addBootupUUID(bootupConfigs);
            vcVm.setGuestConfigs(bootupConfigs);

            return null;
         }

         protected boolean isTaskSession() {
            return true;
         }
      });
   }
}
