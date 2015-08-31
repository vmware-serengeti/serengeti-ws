package com.vmware.bdd.software.mgmt.plugin.intf;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;

public abstract class AbstractSoftwareManager implements SoftwareManager {

   @Override
   public boolean containsComputeOnlyNodeGroups(ClusterBlueprint blueprint) {
      for (NodeGroupInfo nodeGroup : blueprint.getNodeGroups()) {
         if (isComputeOnlyRoles(nodeGroup.getRoles())) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean hasMountPointStartwithDatax(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

}
