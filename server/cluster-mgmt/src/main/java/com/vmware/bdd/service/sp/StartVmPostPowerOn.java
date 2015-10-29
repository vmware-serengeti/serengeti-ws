/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.entity.NicEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.impl.ClusterUpgradeService;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.VcVmUtil;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.IPrePostPowerOn;
import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;

import java.util.Set;

public class StartVmPostPowerOn implements IPrePostPowerOn {
   private static final Logger logger = Logger.getLogger(StartVmPostPowerOn.class);
   private static long checkPeriod = 10 * 1000; // 10 seconds

   private String vmId;
   private VcVirtualMachine vm;
   private long timeout;
   private Set<String> portGroups;
   private IClusterEntityManager clusterEntityMgr;
   /**
    * After a VM is powered on, wait for the guest information to be
    * available at most for <tt>timeout</tt> in seconds, and retrieve the
    * guest information.
    *
    * @param portGroups
    * @param timeoutInSeconds
    */
   public StartVmPostPowerOn(Set<String> portGroups, int timeoutInSeconds) {
      this.portGroups = portGroups;
      this.timeout = timeoutInSeconds * 1000L;
   }

   public StartVmPostPowerOn(Set<String> portGroups, int timeoutInSeconds, IClusterEntityManager clusterEntityMgr) {
      this.portGroups = portGroups;
      this.timeout = timeoutInSeconds * 1000L;
      this.clusterEntityMgr = clusterEntityMgr;
   }

   @Override
   public Void call() throws Exception {
      long start = System.currentTimeMillis();
      vm = VcCache.getIgnoreMissing(vmId);
      while (System.currentTimeMillis() - start < timeout) {
         boolean stop = VcContext.inVcSessionDo(new VcSession<Boolean>() {
            @Override
            protected Boolean body() throws Exception {
               if (vm != null && vm.isPoweredOn()) {
                  return false;
               } else {
                  // stop waiting, since vm is not found
                  logger.info("vm is not found or is powered off in VC, " + "stop waiting for ip address.");
                  return true;
               }
            }
         });

         // check if all ipaddresses for portGroups are valid
         int found = 0;
         for (String pgName : portGroups) {
            String ip = VcVmUtil.getIpAddressOfPortGroup(vm, pgName, false);
            if (!ip.equals(Constants.NULL_IPV4_ADDRESS)) {
               logger.info("got one ip, vm: " + vm.getName() + ", portgroup: " + pgName + ", ip: " + ip);
               found += 1;
            }
         }

         if (found == portGroups.size()) {
            break;
         }

         if (stop) {
            break;
         }
         Thread.sleep(checkPeriod);
      }
      if (clusterEntityMgr != null) {
         NodeEntity node = clusterEntityMgr.getNodeWithNicsByMobId(vmId);
         for (NicEntity nicEntity : node.getNics()) {
            VcVmUtil.populateNicInfo(nicEntity, node.getMoId(), nicEntity.getNetworkEntity().getPortGroup());
         }
         upgradeNode(node);
      }
      return null;
   }

   @Override
   public void setVm(VcVirtualMachine vm) {
      vmId = vm.getId();
      this.vm = vm;
   }

   @Override
   public VcVirtualMachine getVm() {
      return vm;
   }

   public String getVmId() {
      return vmId;
   }

   public void setVmId(String vmId) {
      this.vmId = vmId;
   }

   private void upgradeNode(NodeEntity node) {
      String serverVersion = clusterEntityMgr.getServerVersion();
      String vmName = node.getVmName();
      if (node.needUpgrade(serverVersion) && node.canBeUpgrade()) {
         logger.debug("vm " + vmName + "is going to upgrade");
         clusterEntityMgr.updateNodeActionForUpgrade(node, Constants.NODE_ACTION_UPGRADING);
         NodeUpgradeSP nodeUpgrade = new NodeUpgradeSP(node, serverVersion);
         try {
            nodeUpgrade.call();
            updateNodeData(node);
         } catch (Exception e) {
            updateNodeData(node, false , e.getMessage());
            throw BddException.UPGRADE(e, e.getMessage());
         }
      }
   }

   private void updateNodeData(NodeEntity node) {
      updateNodeData(node, true, null);
   }

   @Transactional
   private void updateNodeData(NodeEntity node, boolean upgraded, String errorMessage) {
      node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());
      String serverVersion = clusterEntityMgr.getServerVersion();
      String nodeVmName = node.getVmName();
      if (upgraded) {
         logger.info("Successfully upgrade cluster node " + nodeVmName);
         node.setVersion(serverVersion);
         node.setAction(Constants.NODE_ACTION_UPGRADE_SUCCEED);
         node.setActionFailed(false);
         node.setErrMessage(null);
      } else {
         logger.error("Failed to upgrade cluster node " + nodeVmName);
         node.setAction(Constants.NODE_ACTION_UPGRADE_FAILED);
         node.setActionFailed(true);
         String errorTimestamp = CommonUtil.getCurrentTimestamp();
         String[] messages = errorMessage.split(":");
         if (messages != null && messages.length > 0) {
            node.setErrMessage(errorTimestamp + " " + messages[messages.length-1]);
         } else {
            node.setErrMessage(errorTimestamp + " " + "Upgrading node " + nodeVmName + " failed.");
         }
      }
      clusterEntityMgr.update(node);
   }

}
