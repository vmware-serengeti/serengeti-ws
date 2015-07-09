package com.vmware.bdd.utils;

import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.aurora.vc.VcCache;
import com.vmware.aurora.vc.VcResourcePool;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;

public class ClusterUtil {
   private static final Logger logger = Logger.getLogger(ClusterUtil.class);

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
      String vmName = node.getVmName();
      String[] strs = vmName.split("-");
      String clusterName = strs[0];
      String groupName = strs[1];
      VcResourcePool vcResPool = VcVmUtil.getTargetRp(clusterName, groupName, node);

      // 2. get the vm by vm name from the target resource pool
      List<VcVirtualMachine> vmList = vcResPool.getChildVMs();
      for ( VcVirtualMachine vm : vmList ) {
         if ( vmName.equals(vm.getName()) ) {
            logger.info("The node vm " + node.getVmName() + " is found in vCenter by vm name.");
            vcVm = vm;
            break;
         }
      }

      // 3. update the node table in bde db by setting the correct vm moid
      if ( vcVm != null ) {
         String moId = vcVm.getId();
         node.setMoId(moId);
         clusterEntityMgr.update(node);
      }

      return vcVm;
   }
}
