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
package com.vmware.bdd.service.job.software.external;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

/**
 * Author: Xiaoding Bian
 * Date: 6/11/14
 * Time: 2:58 PM
 */
public class ExternalManagementTask implements ISoftwareManagementTask {

   private static final Logger logger = Logger.getLogger(ExternalManagementTask.class);
   private String targetName;
   private ManagementOperation managementOperation;
   private ClusterBlueprint clusterBlueprint;
   private StatusUpdater statusUpdater;
   private ILockedClusterEntityManager lockedClusterEntityManager;
   private SoftwareManager softwareManager;

   public ExternalManagementTask(String targetName, ManagementOperation managementOperation,
         ClusterBlueprint clusterBlueprint, StatusUpdater statusUpdater,
         ILockedClusterEntityManager lockedClusterEntityManager, SoftwareManager softwareManager) {
      this.targetName = targetName;
      this.managementOperation = managementOperation;
      this.clusterBlueprint = clusterBlueprint;
      this.statusUpdater = statusUpdater;
      this.lockedClusterEntityManager = lockedClusterEntityManager;
      this.softwareManager = softwareManager;
   }

   @Override
   public Map<String, Object> call() throws Exception {

      Map<String, Object> result = new HashMap<String, Object>();

      // TODO: start software operation monitor
      ClusterReportQueue queue = new ClusterReportQueue();
      boolean success = false;
      try {
         switch(managementOperation) {
            case CREATE:
               // TODO: set targetName
               success = softwareManager.createCluster(clusterBlueprint, queue);
               break;
            case CONFIGURE:
               success = softwareManager.reconfigCluster(clusterBlueprint, queue);
               break;
            default:
               success = true;
         }
      } catch (Exception e) {

      } finally {
         // TODO: stop monitor
      }

      result.put("succeed", success);

      return result;
   }
}
