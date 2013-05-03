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
package com.vmware.bdd.service.job.software.ironfan;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ProgressMonitor;
import com.vmware.bdd.software.mgmt.impl.SoftwareManagementClient;
import com.vmware.bdd.software.mgmt.thrift.ClusterAction;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
import com.vmware.bdd.utils.Configuration;

public class IronfanSoftwareManagementTask implements ISoftwareManagementTask {
   private static final Logger logger = Logger
         .getLogger(IronfanSoftwareManagementTask.class);

   private ClusterOperation clusterOperation;
   private StatusUpdater statusUpdater;
   private ClusterEntityManager clusterEntityMgr;

   public IronfanSoftwareManagementTask(ClusterOperation clusterOperation,
         StatusUpdater updater, ClusterEntityManager mgr) {
      this.clusterOperation = clusterOperation;
      this.statusUpdater = updater;
      this.clusterEntityMgr = mgr;
   }

   @Override
   public synchronized Map<String, Object> call() throws Exception {
      logger.info("cluster : " + clusterOperation.getTargetName()
            + " operation: " + clusterOperation.getAction().toString());
      Map<String, Object> result = new HashMap<String, Object>();
      //This is for create cluster test only. As there is UT in TestClusteringJobs.testCreateCluster
      if(Configuration.getBoolean("management.thrift.mock",false)){
         result.put("succeed", true);
         result.put("exitCode", 0);
         return result;
      }
      final SoftwareManagementClient client = new SoftwareManagementClient();
      client.init();
      
      Thread progressThread = null;
      ProgressMonitor monitor = null;
      ClusterAction action = clusterOperation.getAction();
      if (action != ClusterAction.STOP
            && action != ClusterAction.ENABLE_CHEF_CLIENT
            && action != ClusterAction.DESTROY) {
         monitor =
               new ProgressMonitor(clusterOperation.getTargetName(),
                     statusUpdater, clusterEntityMgr);
         progressThread =
               new Thread(monitor, "ProgressMonitor-"
                     + clusterOperation.getTargetName());
         progressThread.setDaemon(true);
         progressThread.start();
      }

      int exitCode = 1;
      try {
         exitCode = client.runClusterOperation(clusterOperation);
      } catch (Throwable t) {
         logger.error(" operation : " + clusterOperation.getAction()
               + " failed on cluster: " + clusterOperation.getTargetName(), t);
      } finally {
         if (progressThread != null) {
            if (monitor != null) {
               monitor.setStop(true);
            }
            if (progressThread.isAlive()) {
               progressThread.interrupt();
            }
         }
      }
      OperationStatusWithDetail detailedStatus =
            client.getOperationStatusWithDetail(clusterOperation
                  .getTargetName());
      statusUpdater.setProgress(((double) (detailedStatus.getOperationStatus()
            .getProgress())) / 100);
      boolean finished =
            clusterEntityMgr.handleOperationStatus(
                  clusterOperation.getTargetName().split("-")[0], detailedStatus);
      logger.info("updated progress. finished? " + finished);

      boolean succeed = true;
      succeed = exitCode == 0;
      result.put("succeed", succeed);
      if (!succeed) {
         result.put("exitCode", exitCode);
         String errorMessage =
               detailedStatus.getOperationStatus().getError_msg();
         if (errorMessage == null) {
            errorMessage = "command exited with non zero";
         }
         result.put("errorMessage", errorMessage);
         if (errorMessage != null) {
            logger.error("command execution failed, error message is"
                  + result.get("errorMessage"));
         }
      }
      if (client != null) {
         client.close();
      }
      return result;
   }


}
