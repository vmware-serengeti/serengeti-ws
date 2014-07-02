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
import java.util.List;
import java.util.Map;

import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
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

      ClusterReportQueue queue = new ClusterReportQueue();
      /*
      Thread progressThread = null;
      ExternalProgressMonitor monitor =
            new ExternalProgressMonitor(targetName, queue, statusUpdater,
                  lockedClusterEntityManager);
      progressThread = new Thread(monitor, "ProgressMonitor-" + targetName);
      progressThread.setDaemon(true);
      progressThread.start();
      */

      boolean success = false;

      Thread monitor = new Drain(queue);
      monitor.start();

      try {
         switch(managementOperation) {
            case CREATE:
               success = softwareManager.createCluster(clusterBlueprint, queue);
               break;
            case CONFIGURE:
               success = softwareManager.reconfigCluster(clusterBlueprint, queue);
               break;
            case PRE_DESTROY:
               success = softwareManager.onDeleteCluster(clusterBlueprint.getName(), queue);
               break;
            case DESTROY:
               success = softwareManager.deleteCluster(clusterBlueprint.getName(), queue);
            case START:
               success = softwareManager.startCluster(clusterBlueprint.getName(), queue);
               break;
            case STOP:
               success = softwareManager.onStopCluster(clusterBlueprint.getName(),queue);
               break;
            default:
               success = true;
         }
      } catch (Throwable t) {
         logger.error(" operation : " + managementOperation.name()
               + " failed on cluster: " + targetName, t);
      } finally {
         /*
         if (progressThread != null) {
            if (monitor != null) {
               monitor.setStop(true); // tell monitor to stop monitoring then the thread will exit
               progressThread.interrupt(); // wake it up to stop immediately if it's sleeping
               progressThread.join();
            }
            */
         if (monitor != null) {
            monitor.interrupt();
            monitor.join();
         }
      }

      result.put("succeed", success);

      return result;
   }

   private class Drain extends Thread {
      private ClusterReportQueue queue;

      public Drain(ClusterReportQueue queue) {
         this.queue = queue;
      }

      @Override
      public void run() {
         while (true) {
            List<ClusterReport> reports = queue.pollClusterReport();
            for (ClusterReport report : reports) {
               logger.info("Action: " + report.getAction() + ", Progress: " + report.getProgress());
            }
            try {
               Thread.sleep(5000);
            } catch (InterruptedException e) {
               break;
            }
         }
      }

   }
}
