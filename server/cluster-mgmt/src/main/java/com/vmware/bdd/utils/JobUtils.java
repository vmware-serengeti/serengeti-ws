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
package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.LimitInstruction;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.command.VHMMessageTask;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NicEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IExecutionService;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.spectypes.NicSpec;
import org.springframework.batch.core.scope.context.ChunkContext;

public class JobUtils {
   private static final Logger logger = Logger.getLogger(JobUtils.class);

   /**
    * Query out existing nodes in one cluster
    *
    * @param cluster
    * @return
    */
   public static List<BaseNode> getExistingNodes(ClusterCreate cluster,
         IClusterEntityManager entityMgr) {

      List<BaseNode> existingNodes = new ArrayList<BaseNode>();
      ClusterEntity clusterEntity = entityMgr.findByName(cluster.getName());
      for (NodeGroupCreate group : cluster.getNodeGroups()) {
         NodeGroupEntity groupEntity =
               entityMgr.findByName(clusterEntity, group.getName());
         List<NodeEntity> nodeEntities = groupEntity.getNodes();
         List<NodeEntity> listEntities = new ArrayList<NodeEntity>();
         listEntities.addAll(nodeEntities);
         existingNodes
               .addAll(convertNodeEntities(cluster, group, listEntities));
      }
      return existingNodes;
   }

   public static List<BaseNode> convertNodeEntities(ClusterCreate cluster,
         NodeGroupCreate group, List<NodeEntity> nodeEntities) {
      List<BaseNode> nodes = new ArrayList<BaseNode>();
      if (nodeEntities == null) {
         return nodes;
      }
      for (NodeEntity nodeEntity : nodeEntities) {
         BaseNode node = convertNode(cluster, group, nodeEntity);
         nodes.add(node);
      }
      return nodes;
   }

   private static BaseNode convertNode(ClusterCreate cluster,
         NodeGroupCreate group, NodeEntity entity) {
      logger.debug("convert node " + entity.getVmName());
      VcVirtualMachine vcVm = null;
      if (entity.getMoId() != null) {
         vcVm = VcCache.getIgnoreMissing(entity.getMoId());
      }
      BaseNode node = new BaseNode(entity.getVmName());
      node.setTargetHost(entity.getHostName());
      if (entity.getVcRp() != null) {
         node.setTargetVcCluster(entity.getVcRp().getVcCluster());
         node.setTargetRp(entity.getVcRp().getVcResourcePool());
      }
      node.setTargetRack(entity.getRack());
      node.setCluster(cluster);
      node.setNodeGroup(group);
      node.setVmMobId(entity.getMoId());
      node.setVmFolder(entity.getNodeGroup().getVmFolderPath());
      if (vcVm != null) {
         node.setNics(convertNics(entity.getNics()));
         node.setGuestHostName(entity.getGuestHostName());
      }
      return node;
   }

   private static Map<String, NicSpec> convertNics(Set<NicEntity> nicEntities) {
      Map<String, NicSpec> nicSpecMap = new HashMap<String, NicSpec>();
      if (nicEntities != null) {
         for (NicEntity nicEntity : nicEntities) {
            String pgName = nicEntity.getNetworkEntity().getPortGroup();
            NicSpec nicSpec = new NicSpec();
            nicSpec.setIpv4Address(nicEntity.getIpv4Address());
            nicSpec.setIpv6Address(nicEntity.getIpv6Address());
            nicSpec.setMacAddress(nicEntity.getMacAddress());
            nicSpec.setNetworkName(nicEntity.getNetworkEntity().getName());
            nicSpec.setNetTrafficDefinitionSet(nicEntity.getNetTrafficDefs());
            nicSpecMap.put(pgName, nicSpec);
         }
      }
      return nicSpecMap;
   }

   /**
    * separate non exist node from existing node list. The condition is vcVm is
    * null. also add used ip address to a set, that will be used in later create
    * VM method.
    *
    * @param existingNodes
    * @param deletedNodes
    * @param occupiedIpSets
    */
   public static void removeNonExistNodes(List<BaseNode> existingNodes,
         Map<String, Set<String>> occupiedIpSets) {
      List<BaseNode> notExisted = new ArrayList<BaseNode>();
      for (BaseNode node : existingNodes) {
         if (node.getVmMobId() == null) {
            notExisted.add(node);
         } else {
            adjustOccupiedIpSets(occupiedIpSets, node, true);
         }
      }
      existingNodes.removeAll(notExisted);
   }

   /**
    *
    * @param occupiedIpSets
    * @param node
    * @param add
    *           true to add, false to remove
    */
   public static void adjustOccupiedIpSets(
         Map<String, Set<String>> occupiedIpSets, BaseNode node, boolean add) {
      if (!add && occupiedIpSets.isEmpty()) {
         return;
      }

      for (String portGroup : node.getNics().keySet()) {
         if (!occupiedIpSets.containsKey(portGroup)) {
            Set<String> ipSet = new HashSet<String>();
            occupiedIpSets.put(portGroup, ipSet);
         }

         String ip = node.fetchIpAddressOfPortGroup(portGroup);
         Set<String> ips = occupiedIpSets.get(portGroup);
         if (add) {
            ips.add(ip);
         } else {
            ips.remove(ip);
         }
      }
   }

   /**
    * separate vc unreachable node from existing node list. if the node is
    * powered off, or powered on but ip address is not accessible, remove the
    * node from good nodes
    *
    * @param existingNodes
    * @param deletedNodes
    * @param occupiedIpSets
    */
   public static void separateVcUnreachableNodes(List<BaseNode> existingNodes,
         List<BaseNode> deletedNodes, Map<String, Set<String>> occupiedIpSets,
         IClusterEntityManager entityMgr, ClusterCreate cluster) {
      ClusterEntity clusterEntity = entityMgr.findByName(cluster.getName());
      for (NodeGroupCreate group : cluster.getNodeGroups()) {
         NodeGroupEntity groupEntity =
               entityMgr.findByName(clusterEntity, group.getName());
         List<NodeEntity> nodeEntities = groupEntity.getNodes();
         if (nodeEntities == null) {
            continue;
         }
         for (NodeEntity nodeEntity : nodeEntities) {
            BaseNode node = convertNode(cluster, group, nodeEntity);
            VcVirtualMachine vm = null;
            if (node.getVmMobId() != null) {
               vm = VcCache.getIgnoreMissing(node.getVmMobId());
            } else
               logger.warn("Node " + node.getVmName() + " mobid is null.");
            if (vm == null) {
               logger.warn("Cannot find VM in VcCache for node "
                     + node.getVmName() + " whose mobid is "
                     + node.getVmMobId() + ".");
               vm =
                     ClusterUtil.findAndUpdateNodeVmByName(entityMgr,
                           nodeEntity);
               if (vm == null) {
                  logger.warn("Cannot find VM by VM path " + node.getVmName()
                        + " whose mobid is " + node.getVmMobId() + ".");
               } else {
                  node.setVmMobId(vm.getId());
               }
            }
            if (vm == null || (!vm.isPoweredOn())
                  || !VcVmUtil.checkIpAddresses(vm)) {
               if (vm == null) {
                  logger.warn("Node " + node.getVmName()
                        + " will be deleted because cannot find VM.");
               } else if (!vm.isPoweredOn()) {
                  logger.warn("Node " + node.getVmName()
                        + " will be deleted because it's not powered on.");
               } else {
                  logger.warn("Node " + node.getVmName()
                        + " will be deleted because no valid ip address.");
                  List<String> ips = VcVmUtil.getAllIpAddresses(vm);
                  logger.warn("ip addresses for node " + node.getVmName()
                              + ": " + (ips == null ? "None" : ips.toString()));
               }
               deletedNodes.add(node);
               continue;
            }
            String haFlag = node.getNodeGroup().getHaFlag();
            if (haFlag != null
                  && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
               if (!VcVmUtil.verifyFTState(vm)) {
                  logger.warn("FT secondary VM status is incorrect for node "
                        + vm.getName() + ". " + "FT status " + vm.getFTState().toString()
                        + " is unexpected.");
                  deletedNodes.add(node);
                  continue;
               }
            }
            existingNodes.add(node);
            adjustOccupiedIpSets(occupiedIpSets, node, true);
         }
      }
   }

   public static void verifyNodeStatus(NodeEntity node,
         NodeStatus expectedStatus, boolean ignoreMissing) {
      if (node.getStatus() != expectedStatus) {
         if (ignoreMissing
               && (node.getStatus() == NodeStatus.NOT_EXIST || node
                     .isDisconnected())) {
            return;
         }
         if (node.isDisconnected()) {
            logger.info("Node "
                  + node.getVmName()
                  + " cannot be controlled through VC. Remove it from VC manually, and then repeat the operarion.");
            throw ClusteringServiceException.VM_UNAVAILABLE(node.getVmName());
         }
         // verify from VC
         VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getMoId());

         if (expectedStatus == NodeStatus.VM_READY) {

            if (vm == null || (!vm.isPoweredOn())) {
               throw ClusteringServiceException.VM_STATUS_ERROR(node
                     .getStatus().toString(), expectedStatus.toString());
            }

            if (!VcVmUtil.checkIpAddresses(vm)) {
               // throw out clear information
               throw ClusteringServiceException.CANNOT_GET_IP_ADDRESS(node
                     .getVmName());
            }
            String haFlag = node.getNodeGroup().getHaFlag();
            if (haFlag != null
                  && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
               if (!VcVmUtil.verifyFTState(vm)) {
                  logger.info("FT secondary VM state incorrect for node "
                        + vm.getName() + ", " + "FT state " + vm.getFTState()
                        + " is unexpected.");
                  throw ClusteringServiceException.ENABLE_FT_FAILED(null,
                        node.getVmName());
               }
            }
         } else {
            if (vm == null || (!vm.isPoweredOff())) {
               throw ClusteringServiceException.VM_STATUS_ERROR(node
                     .getStatus().toString(), expectedStatus.toString());
            }
         }
      }
   }

   public static boolean verifyNodesStatus(List<NodeEntity> nodes,
         NodeStatus expectedStatus, boolean ignoreMissing) {
      boolean success = true;
      for (NodeEntity node : nodes) {
         try {
            verifyNodeStatus(node, expectedStatus, ignoreMissing);
         } catch (Exception e) {
            node.setActionFailed(true);
            logger.debug("Node verify failed for " + node.getVmName()
                  + ", for " + e.getMessage());
            if (node.getErrMessage() == null || node.getErrMessage().isEmpty()) {
               node.setErrMessage(CommonUtil.getCurrentTimestamp() + " " + e.getMessage());
               logger.debug("Set node error message for node "
                     + node.getVmName() + " to: " + e.getMessage());
            }
            success = false;
         }
      }
      return success;
   }

   public static String getSubJobParameterPrefixKey(int stepNumber,
         int paramIndex) {
      return JobConstants.SUB_JOB_PARAMETERS_KEY_PREFIX + stepNumber + "."
            + paramIndex;
   }

   public static String getSubJobParameterPrefixValue(int stepNumber,
         int paramIndex) {
      return JobConstants.SUB_JOB_PARAMETERS_VALUE_PREFIX + stepNumber + "."
            + paramIndex;
   }

   public static void waitForManual(String clusterName,
         IExecutionService executionService) {
      logger.info("start notify VHM swithing to manual mode");
      Map<String, Object> sendParam = new HashMap<String, Object>();
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_VERSION,
            Constants.VHM_PROTOCOL_VERSION);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_CLUSTER_NAME,
            clusterName);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_RECEIVE_ROUTE_KEY,
            CommonUtil.getUUID());
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_ACTION,
            LimitInstruction.actionWaitForManual);

      Map<String, Object> ret = null;
      try {
         ret = executionService.execute(new VHMMessageTask(sendParam, null));
         if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            throw TaskException.EXECUTION_FAILED(errorMessage);
         }
      } catch (Exception e) {
         logger.error("failed to notify VHM switching to manual mode for cluster: "
               + clusterName);
      }
   }

   public static boolean VerifyClusterNodes(String clusterName,
         String verifyScope, String groupName, long oldInstanceNum,
         IClusterEntityManager clusterEntityMgr) {

      // only when node is created, we need to verify node status
      if (verifyScope != null
            && verifyScope.equals(JobConstants.GROUP_NODE_SCOPE_VALUE)) {
         return verifyGroupVmReady(clusterName, groupName, oldInstanceNum,
               clusterEntityMgr);
      } else {
         return verifyAllVmReady(clusterName, clusterEntityMgr);
      }
   }

   private static boolean verifyGroupVmReady(String clusterName,
         String groupName, long oldInstanceNum,
         IClusterEntityManager clusterEntityMgr) {
      if (groupName == null) {
         logger.info("No group name specified, ignore node status verification.");
         return true;
      }
      List<NodeEntity> nodes =
            clusterEntityMgr.findAllNodes(clusterName, groupName);
      List<NodeEntity> toBeVerified = new ArrayList<NodeEntity>();
      for (NodeEntity node : nodes) {
         long index = CommonUtil.getVmIndex(node.getVmName());
         if (index < oldInstanceNum) {
            // do not verify existing nodes from last successful deployment
            continue;
         }
         toBeVerified.add(node);
      }
      return verifyNodesStatus(toBeVerified, NodeStatus.VM_READY, false);
   }

   private static boolean verifyAllVmReady(String clusterName,
         IClusterEntityManager clusterEntityMgr) {
      List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(clusterName);
      return verifyNodesStatus(nodes, NodeStatus.VM_READY, false);
   }

   public static String getJobParameter(ChunkContext context, String parameterKey) {
      return context.getStepContext().getStepExecution().getJobParameters().getString(parameterKey);
   }

   public static boolean getJobParameterForceClusterOperation(ChunkContext chunkContext) {
      String forceStartString = JobUtils.getJobParameter(chunkContext, JobConstants.FORCE_CLUSTER_OPERATION_JOB_PARAM);
      return CommonUtil.getBooleanFromString(forceStartString, false);
   }

   public static void forceClusterOperationRecordError(boolean force, Logger logger) {
      if(force) {
         logger.warn(Constants.FORCE_CLUSTER_OPERATION_IGNORE_EXCEPTION);
      }
   }

}
