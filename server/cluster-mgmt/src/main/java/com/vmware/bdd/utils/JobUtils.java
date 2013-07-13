package com.vmware.bdd.utils;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.ClusteringServiceException;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.placement.entity.BaseNode;
import com.vmware.bdd.service.IExecutionService;
import com.vmware.bdd.service.job.JobConstants;

public class JobUtils {
   private static final Logger logger = Logger.getLogger(JobUtils.class);

   /**
    * Query out existing nodes in one cluster
    * 
    * @param cluster
    * @return
    */
   public static List<BaseNode> getExistingNodes(ClusterCreate cluster,
         ClusterEntityManager entityMgr) {

      List<BaseNode> existingNodes = new ArrayList<BaseNode>();
      ClusterEntity clusterEntity = entityMgr.findByName(cluster.getName());
      for (NodeGroupCreate group : cluster.getNodeGroups()) {
         NodeGroupEntity groupEntity =
               entityMgr.findByName(clusterEntity, group.getName());
         Set<NodeEntity> nodeEntities = groupEntity.getNodes();
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
         node.setIpAddress(entity.getIpAddress());
         node.setGuestHostName(entity.getGuestHostName());
      }
      return node;
   }

   /**
    * separate non exist node from existing node list. The condition is vcVm is
    * null. also add used ip address to a set, that will be used in later create
    * VM method.
    * 
    * @param existingNodes
    * @param deletedNodes
    * @param occupiedIps
    */
   public static void removeNonExistNodes(List<BaseNode> existingNodes,
         List<BaseNode> deletedNodes, Set<String> occupiedIps) {
      for (BaseNode node : existingNodes) {
         if (node.getVmMobId() == null) {
            deletedNodes.add(node);
         } else {
            occupiedIps.add(node.getIpAddress());
         }
      }
      existingNodes.removeAll(deletedNodes);
   }

   /**
    * separate vc unreachable node from existing node list. if the node is
    * powered off, or powered on but ip address is not accessible, remove the
    * node from good nodes
    * 
    * @param existingNodes
    * @param deletedNodes
    * @param occupiedIps
    */
   public static void separateVcUnreachableNodes(List<BaseNode> existingNodes,
         List<BaseNode> deletedNodes, Set<String> occupiedIps) {
      for (BaseNode node : existingNodes) {
         if (node.getVmMobId() == null) {
            deletedNodes.add(node);
            continue;
         }
         VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getVmMobId());
         if (vm == null || (!vm.isPoweredOn())
               || (VcVmUtil.getIpAddress(vm, false) == null)) {
            deletedNodes.add(node);
            continue;
         }
         String haFlag = node.getNodeGroup().getHaFlag();
         if (haFlag != null
               && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
            if (!VcVmUtil.verifyFTState(vm)) {
               logger.info("FT secondary VM state incorrect for node "
                     + vm.getName() + ", " + "FT state " + vm.getFTState()
                     + " is unexpected.");
               deletedNodes.add(node);
               continue;
            }
         }
         occupiedIps.add(node.getIpAddress());
      }
      existingNodes.removeAll(deletedNodes);
   }

   public static long getVmIndex(String vmName) {
      String[] split = vmName.split("-");
      if (split.length < 3) {
         throw ClusteringServiceException.VM_NAME_VIOLATE_NAME_PATTERN(vmName);
      }
      try {
         return Long.valueOf(split[2]);
      } catch (Exception e) {
         logger.error("vm name " + vmName
               + " violate serengeti vm name definition.");
         throw ClusteringServiceException.VM_NAME_VIOLATE_NAME_PATTERN(vmName);
      }
   }

   public static void verifyNodesStatus(List<NodeEntity> nodes,
         NodeStatus expectedStatus, boolean ignoreMissing) {
      for (NodeEntity node : nodes) {
         if (node.getStatus() != expectedStatus) {
            if (ignoreMissing && node.getStatus() == NodeStatus.NOT_EXIST) {
               continue;
            }
            if (expectedStatus == NodeStatus.VM_READY) {
               // verify from VC 
               VcVirtualMachine vm = VcCache.getIgnoreMissing(node.getMoId());
               if (vm == null || (!vm.isPoweredOn())
                     || (VcVmUtil.getIpAddress(vm, false) == null)) {
                  throw ClusteringServiceException.VM_STATUS_ERROR(
                        node.getVmName(), node.getStatus().toString(),
                        expectedStatus.toString());
               }

               String haFlag = node.getNodeGroup().getHaFlag();
               if (haFlag != null
                     && Constants.HA_FLAG_FT.equals(haFlag.toLowerCase())) {
                  if (!VcVmUtil.verifyFTState(vm)) {
                     logger.info("FT secondary VM state incorrect for node "
                           + vm.getName() + ", " + "FT state "
                           + vm.getFTState() + " is unexpected.");
                     throw ClusteringServiceException.ENABLE_FT_FAILED(null,
                           node.getVmName());
                  }
               }
            }
         }
      }
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

   public static void waitForManual(String clusterName, IExecutionService executionService){
      logger.info("start notify VHM swithing to manual mode");
      Map<String, Object> sendParam = new HashMap<String, Object>();
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_VERSION, Constants.VHM_PROTOCOL_VERSION);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_CLUSTER_NAME, clusterName);
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_RECEIVE_ROUTE_KEY, CommonUtil.getUUID());
      sendParam.put(Constants.SET_MANUAL_ELASTICITY_INFO_ACTION, LimitInstruction.actionWaitForManual);

      Map<String, Object> ret = null;
      try {
         ret = executionService.execute(new VHMMessageTask(sendParam, null));
         if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            throw TaskException.EXECUTION_FAILED(errorMessage);
         }
      } catch (Exception e) {
         throw TaskException.EXECUTION_FAILED("failed to notify VHM swithing to manual for cluster: " + clusterName);
      }
   }
}
