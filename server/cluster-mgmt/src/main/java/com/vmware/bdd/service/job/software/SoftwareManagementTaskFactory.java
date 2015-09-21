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
package com.vmware.bdd.service.job.software;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.manager.ClusterConfigManager;
import com.vmware.bdd.manager.ClusterManager;
import org.springframework.batch.core.scope.context.ChunkContext;

import com.vmware.bdd.command.ClusterCmdUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.external.ExternalManagementTask;
import com.vmware.bdd.service.job.software.thrift.ThriftSoftwareManagementTask;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.thrift.ClusterAction;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 * 
 */
public class SoftwareManagementTaskFactory {

   public static ISoftwareManagementTask createThriftTask(String targetName,
         String specFileName, StatusUpdater statusUpdater,
         ManagementOperation managementOperation,
         ILockedClusterEntityManager lockClusterEntityMgr) {
      ClusterOperation clusterOperation = new ClusterOperation();
      switch (managementOperation) {
      case CREATE:
         clusterOperation.setAction(ClusterAction.CREATE);
         break;
      case RESIZE:
         clusterOperation.setAction(ClusterAction.CREATE);
         break;
      case QUERY:
         String clusterName = targetName.split("-")[0];
         boolean needQuery = checkAndResetNodePowerStatusChanged(clusterName, 
               lockClusterEntityMgr.getClusterEntityMgr());
         if (needQuery) {
            clusterOperation.setAction(ClusterAction.QUERY);
            break;
         } else {
            return null;
         }
      case UPDATE:
         clusterOperation.setAction(ClusterAction.UPDATE);
         break;
      case START:
         clusterOperation.setAction(ClusterAction.START);
         break;
      case STOP:
         //When stop ironfan deployed cluster, don't need to stop
         //hadoop services, return directly.
         return null;
      case DESTROY:
         clusterOperation.setAction(ClusterAction.DESTROY);
         break;
      case CONFIGURE:
         clusterOperation.setAction(ClusterAction.CONFIGURE);
         break;
      default:
         return null;
      }
      clusterOperation.setTargetName(targetName);
      clusterOperation.setSpecFileName(specFileName);
      clusterOperation.setLogLevel(ClusterCmdUtil.getLogLevel());
      ThriftSoftwareManagementTask task =
            new ThriftSoftwareManagementTask(clusterOperation, statusUpdater,
                  lockClusterEntityMgr);
      return task;
   }

   private static boolean checkAndResetNodePowerStatusChanged(String clusterName,
         IClusterEntityManager clusterEntityMgr) {
      boolean statusStale = false;

      for (NodeEntity node : clusterEntityMgr.findAllNodes(clusterName)) {
         if (node.isPowerStatusChanged()) {
            switch (node.getStatus()) {
            case VM_READY:
               statusStale = true;
               break;
            case SERVICE_READY:
            case BOOTSTRAP_FAILED:
               node.setPowerStatusChanged(false);
               break;
            default:
               break;
            }
         }
      }

      return statusStale;
   }

   public static ISoftwareManagementTask createExternalMgtTask(
         String targetName, ManagementOperation managementOperation,
         ClusterBlueprint clusterBlueprint, StatusUpdater statusUpdater,
         ILockedClusterEntityManager lockedClusterEntityManager,
         SoftwareManager softwareManager, ChunkContext chunkContext) {
      ISoftwareManagementTask task = new ExternalManagementTask(targetName, managementOperation,
            clusterBlueprint, statusUpdater, lockedClusterEntityManager,
            softwareManager, chunkContext);
      return task;
   }
}
