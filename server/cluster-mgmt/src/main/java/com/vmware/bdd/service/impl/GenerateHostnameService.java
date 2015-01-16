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
package com.vmware.bdd.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.aurora.composition.concurrent.Scheduler;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.ClusterNetConfigInfo;
import com.vmware.bdd.apitypes.NetworkDnsType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NicEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.HostnameManager;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IGenerateHostnameService;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.sp.NoProgressUpdateCallback;
import com.vmware.bdd.service.sp.NodeGenerateHostnameSP;
import com.vmware.bdd.spectypes.NicSpec;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.ScriptForUpdatingEtcHostsGenerator;

public class GenerateHostnameService implements IGenerateHostnameService {

   private static final Logger logger = Logger.getLogger(GenerateHostnameService.class);

   private IClusterEntityManager clusterEntityMgr;
   private String scriptForUpdatingEtcHosts;

   @Override
   public boolean generateHostnameForNodes(String clusterName, StatusUpdater statusUpdator) {
      logger.info("Generate hostname for all nodes of cluster " + clusterName + ".");

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);

      List<Callable<Void>> storeNodeProcedures = new ArrayList<Callable<Void>>();

      try {

         int needGenerateCount = 0;
         for (NetTrafficType netTrafficType : cluster.getNetworkConfigInfo().keySet()) {
            for (ClusterNetConfigInfo netConfigInfo : cluster.getNetworkConfigInfo().get(netTrafficType)) {
               NetworkDnsType dnsType = netConfigInfo.getDnsType();
               if (NetworkDnsType.isOthers(dnsType) || (NetworkDnsType.isNormal(dnsType) && netConfigInfo.getIsGenerateHostname())) {
                  needGenerateCount += 1;
               }
            }
         }
         if (needGenerateCount == 0) {
            logger.info("No need to run generate hostname step because the DNS type of all Nics of cluster " + clusterName + " are Dynamic or Normal with isGenerateHostname set to false.");
            return true;
         }

         scriptForUpdatingEtcHosts = generateScriptUpdatingEtcHosts(cluster);
         for (NodeEntity node : nodes) {
            NodeGenerateHostnameSP nodeGenerateHostnameSP = new NodeGenerateHostnameSP(node, scriptForUpdatingEtcHosts);
            storeNodeProcedures.add(nodeGenerateHostnameSP);
         }

         if (storeNodeProcedures.isEmpty()) {
            logger.info("no VM is available. Return directly.");
            return true;
         }

         Callable<Void>[] storeNodeProceduresArray = storeNodeProcedures.toArray(new Callable[0]);
         NoProgressUpdateCallback callback = new NoProgressUpdateCallback();
         ExecutionResult[] result =
               Scheduler
               .executeStoredProcedures(
                     com.vmware.aurora.composition.concurrent.Priority.BACKGROUND,
                     storeNodeProceduresArray, callback);

         boolean success = true;
         int total = 0;
         for (int i = 0; i < storeNodeProceduresArray.length; i++) {
            Throwable nodeGenerateHostnameSPException = result[i].throwable;
            NodeGenerateHostnameSP sp = (NodeGenerateHostnameSP) storeNodeProceduresArray[i];
            NodeEntity node = sp.getNode();
            if (result[i].finished && nodeGenerateHostnameSPException == null) {
               updateNodeData(node);
               ++total;
            } else if (nodeGenerateHostnameSPException != null) {
               updateNodeData(node, false, nodeGenerateHostnameSPException.getMessage(), CommonUtil.getCurrentTimestamp());
               logger.error("Failed to generate hostname for cluster node " + node.getVmName(), nodeGenerateHostnameSPException);
               success = false;
            }
         }
         logger.info("Hostname of " + total + " nodes are generated.");

         return success;
      } catch (InterruptedException e) {
         logger.error("error in generate hostname for cluster nodes", e);
         throw BddException.FAILED_TO_GENERATE_HOSTNAME(e, e.getMessage());
      }
   }

   @Override
   public boolean generateHostnameForNodesFailed(String clusterName, StatusUpdater statusUpdator) {
      boolean generateHostnameFailed = false;
      List<NodeEntity> nodes = getNodes(clusterName);
      for (NodeEntity node : nodes) {
         if (Constants.NODE_ACTION_GENERATE_HOSTNAME_FAILED.equals(node.getAction())) {
            generateHostnameFailed = true;
            break;
         }
      }
      return generateHostnameFailed;
   }

   private void updateNodeData(NodeEntity node) {
      updateNodeData(node, true, null, null);
   }

   @Transactional
   private void updateNodeData(NodeEntity node, boolean generated, String errorMessage, String errorTimestamp) {
      node = clusterEntityMgr.getNodeWithNicsByMobId(node.getMoId());
      String nodeVmName = node.getVmName();
      if (generated) {
         logger.info("Successfully generate hostname for cluster node " + nodeVmName);
         node.setAction(Constants.NODE_ACTION_GENERATE_HOSTNAME_SUCCEED);
         node.setActionFailed(false);
         node.setErrMessage(null);
         clusterEntityMgr.update(node);
      } else {
         logger.error("Failed to generate hostname for cluster node " + nodeVmName);
         node.setAction(Constants.NODE_ACTION_GENERATE_HOSTNAME_FAILED);
         node.setActionFailed(true);
         String[] messages = errorMessage.split(":");
         if (messages != null && messages.length > 0) {
            node.setErrMessage(errorTimestamp + " " + messages[messages.length-1]);
         } else {
            node.setErrMessage(errorTimestamp + " " + "Generate hostname for node " + nodeVmName + " failed.");
         }
         clusterEntityMgr.update(node);
      }
   }

   private List<NodeEntity> getNodes(String clusterName) {
      return clusterEntityMgr.findAllNodes(clusterName);
   }

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   private String generateScriptUpdatingEtcHosts(ClusterEntity cluster) {
      String hostsContent = "";
      for (NodeGroupEntity nodeGroup : cluster.getNodeGroups()) {
         for (NodeEntity node : nodeGroup.getNodes()) {
            Map<NetTrafficType, List<ClusterNetConfigInfo>> networkConfigInfo = node.getNodeGroup().getCluster().getNetworkConfigInfo();
            Set<NicEntity> nics = node.getNics();
            for (NicEntity nic : nics) {
               for (NicSpec.NetTrafficDefinition netTrafficDefinition: nic.getNetTrafficDefs()) {
                  List<ClusterNetConfigInfo> netConfigInfos = networkConfigInfo.get(netTrafficDefinition.getTrafficType());
                  for (ClusterNetConfigInfo netConfigInfo : netConfigInfos) {
                     if (NetworkDnsType.isOthers(netConfigInfo.getDnsType())
                           || (NetworkDnsType.isNormal(netConfigInfo.getDnsType()) && netConfigInfo.getIsGenerateHostname())) {
                        String hostname = HostnameManager.generateHostname(node, nic);
                        hostsContent += nic.getIpv4Address() + ":" + hostname + ",";
                     }
                  }
               }
            }
         }
      }

      ScriptForUpdatingEtcHostsGenerator scriptForUpdatingEtcHostsGenerator = new ScriptForUpdatingEtcHostsGenerator();
      String scriptForUpdatingEtcHosts = scriptForUpdatingEtcHostsGenerator.generateScriptForUpdatingEtcHosts(cluster.getName(), hostsContent);

      return scriptForUpdatingEtcHosts;
   }
}
