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

import java.util.Map;
import java.util.concurrent.Callable;

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
import com.vmware.bdd.utils.ExecCommandUtil;
import com.vmware.bdd.utils.VcVmUtil;

/**
 * Store Procedure for start a vm
 */

public class NodeUpgradeSP implements Callable<Void> {
   private static final Logger logger = Logger.getLogger(NodeUpgradeSP.class);

   private NodeEntity node;

   public NodeUpgradeSP(NodeEntity node, String serverVersion) {
      this.node = node;
   }

   @Override
   public Void call() throws Exception {
      upgrade();
      return null;
   }

   private void upgrade() throws Exception {
      String nodeVmName = node.getVmName();
      String nodeIp = node.getPrimaryMgtIpV4();

      logger.info("Upgrading cluster node " + node.getVmName());

      if (node.getMoId() != null) {
         if (nodeIp == null || Constants.NULL_IPV4_ADDRESS.equals(nodeIp)) {
            setBootupUUID();
         } else {
            upgradeNodeSteps(nodeVmName, nodeIp);
         }
      }

   }

   public NodeEntity getNode() {
      return node;
   }

   private void upgradeNodeSteps(String nodeVmName, String nodeIp) {

      String sshUser = Configuration.getString(Constants.SSH_USER_CONFIG_NAME, Constants.DEFAULT_SSH_USER_NAME);

      try {

         // Copy node upgrade tarball to node
         String uploadTarballCommand = "scp " + Constants.NODE_UPGRADE_FILE_PATH + Constants.NODE_UPGRADE_TARBALL_FILE_NAME + " " + sshUser + "@" + nodeIp + ":/tmp/";
         ExecCommandUtil.execCmd(uploadTarballCommand, Constants.NODE_ACTION_DOWNLOAD_PACKAGES);

         // Copy node upgrade script file to node
         String uploadScriptFileCommand = "scp " + Constants.NODE_UPGRADE_FILE_PATH + Constants.NODE_UPGRADE_SCRIPT_FILE_NAME + " " + sshUser + "@" + nodeIp + ":/tmp/";
         ExecCommandUtil.execCmd(uploadScriptFileCommand, Constants.NODE_ACTION_DOWNLOAD_PACKAGES);

         // Upgrade cluster node
         String upgradeNodeCommand = "ssh -tt " + sshUser + "@" + nodeIp + " 'sudo bash /tmp/" + Constants.NODE_UPGRADE_SCRIPT_FILE_NAME + "'";
         ExecCommandUtil.execCmd(upgradeNodeCommand, Constants.NODE_ACTION_INSTALL_PACKAGES);

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
