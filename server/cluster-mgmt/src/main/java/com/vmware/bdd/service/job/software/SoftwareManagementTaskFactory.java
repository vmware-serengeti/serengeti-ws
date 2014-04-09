/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.command.ClusterCmdUtil;
import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.ironfan.IronfanSoftwareManagementTask;
import com.vmware.bdd.software.mgmt.thrift.ClusterAction;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 * 
 */
public class SoftwareManagementTaskFactory {

   public static ISoftwareManagementTask createCommandTask(String targetName,
         String specFileName, StatusUpdater statusUpdater,
         ManagementOperation managementOperation,
         ILockedClusterEntityManager lockClusterEntityMgr) {
      ClusterOperation clusterOperation = new ClusterOperation();
      switch (managementOperation) {
      case CREATE:
         clusterOperation.setAction(ClusterAction.CREATE);
         break;
      case QUERY:
         clusterOperation.setAction(ClusterAction.QUERY);
         break;
      case UPDATE:
         clusterOperation.setAction(ClusterAction.UPDATE);
         break;
      case START:
         clusterOperation.setAction(ClusterAction.START);
         break;
      case STOP:
         clusterOperation.setAction(ClusterAction.STOP);
         break;
      case DESTROY:
         clusterOperation.setAction(ClusterAction.DESTROY);
         break;
      case CONFIGURE:
         clusterOperation.setAction(ClusterAction.CONFIGURE);
         break;
      default:
         break;
      }
      clusterOperation.setTargetName(targetName);
      clusterOperation.setSpecFileName(specFileName);
      clusterOperation.setLogLevel(ClusterCmdUtil.getLogLevel());
      IronfanSoftwareManagementTask task =
            new IronfanSoftwareManagementTask(clusterOperation, statusUpdater,
                  lockClusterEntityMgr);
      return task;
   }

}
