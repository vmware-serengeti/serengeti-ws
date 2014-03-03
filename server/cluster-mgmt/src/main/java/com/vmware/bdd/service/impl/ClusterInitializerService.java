/***************************************************************************
 * Copyright (c) 2013 VMware, Inc. All Rights Reserved.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IClusterInitializerService;

public class ClusterInitializerService implements IClusterInitializerService {

   private static final Logger logger = Logger.getLogger(ClusterInitializerService.class);

   private static final ClusterStatus[] toProvisionError = {
         ClusterStatus.PROVISIONING
   };

   private static final ClusterStatus[] toError = {
         ClusterStatus.CONFIGURING,
         ClusterStatus.DELETING,
         ClusterStatus.STARTING,
         ClusterStatus.STOPPING,
         ClusterStatus.UPDATING,
         ClusterStatus.UPGRADING,
         ClusterStatus.VHM_RUNNING,
         ClusterStatus.VMRECONFIGURING,
         ClusterStatus.MAINTENANCE
   };

   private IClusterEntityManager clusterEntityManager;

   public IClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }

   @Override
   public void transformClusterStatus() {
      List<ClusterEntity> allClusters = clusterEntityManager.findAllClusters();
      Set<ClusterStatus> toProvisonErrorSet = new HashSet<ClusterStatus>(Arrays.asList(toProvisionError));
      Set<ClusterStatus> toErrorSet = new HashSet<ClusterStatus>(Arrays.asList(toError));
      for (ClusterEntity clusterEntity : allClusters) {
         ClusterStatus fromStatus = clusterEntity.getStatus();
         ClusterStatus toStatus;
         if (toProvisonErrorSet.contains(fromStatus)) {
            toStatus = ClusterStatus.PROVISION_ERROR;
         } else if (toErrorSet.contains(fromStatus)) {
            toStatus = ClusterStatus.ERROR;
         } else {
            continue;
         }

         clusterEntity.setStatus(toStatus);
         clusterEntityManager.update(clusterEntity);
         logger.info("update status from " + fromStatus.toString() + " to " + toStatus.toString()
               + " for cluster: " + clusterEntity.getName());
      }
   }
}
