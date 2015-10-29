/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
