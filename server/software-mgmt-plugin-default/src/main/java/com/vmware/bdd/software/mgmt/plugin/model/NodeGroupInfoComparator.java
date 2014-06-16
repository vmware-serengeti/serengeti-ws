package com.vmware.bdd.software.mgmt.plugin.model;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.HadoopRole.RoleComparactor;


public class NodeGroupInfoComparator implements Comparator<NodeGroupInfo> {

   @Override
   public int compare(NodeGroupInfo ng1, NodeGroupInfo ng2) {
      if (ng1 == ng2) {
         return 0;
      }
      //null elements will be sorted behind the list
      if (ng1 == null) {
         return 1;
      } else if (ng2 == null) {
         return -1;
      }

      List<String> ng1Roles = ng1.getRoles();
      List<String> ng2Roles = ng2.getRoles();

      return compareBasedOnRoles(ng1Roles, ng2Roles);
   }

   private int compareBasedOnRoles(List<String> ng1Roles, List<String> ng2Roles) {
      if (ng1Roles == ng2Roles) {
         return 0;
      }
      if (ng1Roles == null || ng1Roles.isEmpty()) {
         return 1;
      } else if (ng2Roles == null || ng2Roles.isEmpty()) {
         return -1;
      }

      int ng1RolePos = findNodeGroupRoleMinIndex(ng1Roles);
      int ng2RolePos = findNodeGroupRoleMinIndex(ng2Roles);
      if (ng1RolePos < ng2RolePos) {
         return -1;
      } else if (ng1RolePos == ng2RolePos) {
         return 0;
      } else {
         return 1;
      }
   }

   private int findNodeGroupRoleMinIndex(List<String> ngRoles) {
      Collections.sort(ngRoles, new RoleComparactor());
      HadoopRole role = HadoopRole.fromString(ngRoles.get(0));
      return (null != role) ? role.ordinal() : -1;
   }

}
