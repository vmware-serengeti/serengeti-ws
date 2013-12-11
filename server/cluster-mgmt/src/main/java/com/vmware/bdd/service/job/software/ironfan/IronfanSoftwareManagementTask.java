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

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ProgressMonitor;
import com.vmware.bdd.software.mgmt.impl.SoftwareManagementClient;
import com.vmware.bdd.software.mgmt.thrift.ClusterAction;
import com.vmware.bdd.software.mgmt.thrift.ClusterOperation;

public class IronfanSoftwareManagementTask implements ISoftwareManagementTask {
   private static final Logger logger = Logger
         .getLogger(IronfanSoftwareManagementTask.class);

   private ClusterOperation clusterOperation;
   private StatusUpdater statusUpdater;
   private IExclusiveLockedClusterEntityManager clusterEntityMgr;

   public IronfanSoftwareManagementTask(ClusterOperation clusterOperation,
         StatusUpdater updater, IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.clusterOperation = clusterOperation;
      this.statusUpdater = updater;
      this.clusterEntityMgr = lockClusterEntityMgr;
   }

   @Override
   public synchronized Map<String, Object> call() throws Exception {
      logger.info("cluster : " + clusterOperation.getTargetName()
            + " operation: " + clusterOperation.getAction().toString());
      Map<String, Object> result = new HashMap<String, Object>();
      //This is for create cluster test only. As there is UT in TestClusteringJobs.testCreateCluster
      if (Configuration.getBoolean("management.thrift.mock", false)) {
         result.put("succeed", true);
         result.put("exitCode", 0);
         return result;
      }
      final SoftwareManagementClient client = new SoftwareManagementClient();
      client.init();

      if (clusterOperation.getAction().ordinal() != ClusterAction.QUERY.ordinal()) {
         //Reset node's provision attribute
         client.resetNodeProvisionAttribute(clusterOperation.getTargetName());
      }

      // start monitor thread
      Thread progressThread = null;
      ProgressMonitor monitor = null;
      monitor = new ProgressMonitor(clusterOperation.getTargetName(), statusUpdater, clusterEntityMgr);
      progressThread = new Thread(monitor, "ProgressMonitor-" + clusterOperation.getTargetName());
      progressThread.setDaemon(true);
      progressThread.start();

      // start cluster operation
      int exitCode = -1;
      try {
         exitCode = client.runClusterOperation(clusterOperation);
      } catch (Throwable t) {
         logger.error(" operation : " + clusterOperation.getAction()
               + " failed on cluster: " + clusterOperation.getTargetName(), t);
      } finally {
         if (progressThread != null) {
            if (monitor != null) {
               monitor.setStop(true); // tell monitor to stop monitoring then the thread will exit
               progressThread.interrupt(); // wake it up to stop immediately if it's sleeping
               progressThread.join();
            }
         }

         if (client != null) {
            client.close();
         }
      }

      // handle operation result
      boolean succeed = true;
      succeed = exitCode == 0;
      result.put("succeed", succeed);
      if (!succeed) {
         result.put("exitCode", exitCode);
         String errorMessage = monitor.getLastErrorMsg();
         if (errorMessage == null) {
            errorMessage = "command exited with " + exitCode;
         }
         result.put("errorMessage", errorMessage);
         logger.error("command execution failed. " + result.get("errorMessage"));
      }

      return result;
   }

}
