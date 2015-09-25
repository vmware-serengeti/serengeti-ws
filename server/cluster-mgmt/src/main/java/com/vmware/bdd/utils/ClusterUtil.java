package com.vmware.bdd.utils;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.TrackableTasklet;
import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.aurora.vc.vcservice.VcSession;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class ClusterUtil {
   private static final Logger logger = Logger.getLogger(ClusterUtil.class);
   @Autowired
   private IClusterEntityManager clusterEntityMgr;

   public static VcVirtualMachine getVcVm(IClusterEntityManager clusterEntityMgr, NodeEntity node) {
      String vmId = node.getMoId();
      VcVirtualMachine vcVm = VcCache.getIgnoreMissing(vmId);
      if ( vcVm == null ) {
         // the vm moid could change in cases when vm is re-registered
         // because of vc host crashing or other exceptions, we need to
         // find the vm in vc by its name and vc hierarchy and then update
         // the bde db table with the correct information
         logger.info("The node vm " + node.getVmName() + " is not found in vCenter by moid.");
         vcVm = findAndUpdateNodeVmByName(clusterEntityMgr, node);
      }
      return vcVm;
   }

   public static VcVirtualMachine findAndUpdateNodeVmByName(IClusterEntityManager clusterEntityMgr, NodeEntity node) {
      VcVirtualMachine vcVm = null;

      // 1. get the vc cluster and resource pool containing the vm
      final String vmName = node.getVmName();
      String[] strs = vmName.split("-");
      String clusterName = strs[0];
      String groupName = strs[1];
      final VcResourcePool vcResPool = VcVmUtil.getTargetRp(clusterName, groupName, node);

      // 2. get the vm by vm name from the target resource pool
      // when we get here, it means some vms under the resource pool might have
      // changed after the last VcCache refresh, so we should update it first to get
      // the new vm objects
      VcContext.inVcSessionDo(new VcSession<Void>() {
         public Void body() throws Exception {
            vcResPool.update();
            return null;
         }
      });
      // get the vm by name now
      vcVm = VcVmUtil.findVmInRp(vcResPool, vmName);

      // 3. update the node table in bde db by setting the correct vm moid
      if ( vcVm != null ) {
         logger.info("The node vm " + node.getVmName() + " is found in vCenter by vm name.");
         String moId = vcVm.getId();
         String hostName = vcVm.getHost().getName();
         node.setMoId(moId);
         node.setHostName(hostName);
         clusterEntityMgr.update(node);
      }

      return vcVm;
   }

   public static List<String> getNodesFromNodeGroups (String nodeGroupNameList) throws TaskException {

      List<String> nodeGroupNames = new ArrayList<String>() ;

      for (String nodeGroupName : nodeGroupNameList.split(",")){
         nodeGroupNames.add(nodeGroupName);
      }

      return nodeGroupNames;
   }

   public static List<NodeEntity> getReadyVmFromNodeGroups (List<NodeEntity> addNodes) throws TaskException {
      List<NodeEntity> toBeAddNodes = null;
      for (NodeEntity node : addNodes) {
         if (node.getStatus().ordinal() >= NodeStatus.VM_READY.ordinal()) {
            if (toBeAddNodes == null) {
               toBeAddNodes = new ArrayList<NodeEntity>();
            }
            toBeAddNodes.add(node);
         }
      }
      return toBeAddNodes;
   }

   public static List<NodeEntity> getVmFromNodeGroups (String nodeGroupNameList, List<NodeEntity> addNodes,
                                                       IClusterEntityManager clusterEntityMgr, String clusterName) throws TaskException {
      List<String> nodeGroupNames = null;
      List<NodeEntity> toBeAddNodes = null;
      List<NodeEntity> nodesInGroup = null;
      nodeGroupNames = ClusterUtil.getNodesFromNodeGroups(nodeGroupNameList);

      for (String nodeGroupName: nodeGroupNames) {
         nodesInGroup = clusterEntityMgr.findAllNodes(clusterName, nodeGroupName);
         for(NodeEntity ne: nodesInGroup) {
            addNodes.add(ne);
         }
      }

      toBeAddNodes = ClusterUtil.getReadyVmFromNodeGroups(addNodes);

      return toBeAddNodes;
   }


}
