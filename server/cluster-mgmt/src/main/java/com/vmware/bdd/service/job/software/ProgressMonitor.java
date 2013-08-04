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

import org.apache.log4j.Logger;

import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.software.mgmt.impl.SoftwareManagementClient;
import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;
import com.vmware.bdd.utils.TracedRunnable;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 * 
 */
public class ProgressMonitor extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(ProgressMonitor.class);
   private static final long QUERY_INTERVAL_DEFAULT = 1000 * 10; // 10 seconds
   private static final long QUERY_INTERVAL_LONG = 1000 * 60 * 5; // 5 minutes
   private static final long QUERY_INTERVAL_MAX = 1000 * 60 * 15; // 15 minutes
   private static final int BIG_CLUSTER_NODES_COUNT = 100;

   private String targetName;
   private StatusUpdater statusUpdater;
   private String lastErrorMsg = null;
   private long queryInterval = QUERY_INTERVAL_DEFAULT;
   private ClusterEntityManager clusterEntityMgr;
   private volatile boolean stop;

   public ProgressMonitor(String targetName, StatusUpdater statusUpdater,
         ClusterEntityManager clusterEntityMgr) {
      this.targetName = targetName;
      this.statusUpdater = statusUpdater;
      this.clusterEntityMgr = clusterEntityMgr;
   }

   /**
    * @return should stop monitoring?
    */
   public boolean isStop() {
      return stop;
   }

   /**
    * @param stop
    *           stop monitoring
    */
   public void setStop(boolean stop) {
      this.stop = stop;
   }

   public String getLastErrorMsg() {
      return lastErrorMsg;
   }

   private void setLastErrorMsg(String errorMsg) {
      this.lastErrorMsg = errorMsg;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.command.TracedRunnable#doWork()
    */
   @Override
   public void doWork() throws Exception {
      boolean exit = false;

      logger.info("start monitor operation progress for target " + targetName);
      OperationStatusWithDetail detailedStatus = null;
      SoftwareManagementClient monitorClient = new SoftwareManagementClient();
      monitorClient.init();

      while (!exit) {
         try {
            Thread.sleep(queryInterval);
         } catch (InterruptedException e) {
            logger.info("monitor thread is waked up by caller to stop monitoring");
            stop = true;
         }
         if (stop) {
            logger.info("received stop signal, will do one more query then exit, so as to retrieve the final progress after Ironfan exits.");
            exit = true;
         }

         logger.info("progress query started");
         detailedStatus = monitorClient.getOperationStatusWithDetail(targetName);
         if (null == detailedStatus) {
            logger.error("Failed to query progress. Something wrong with the Monitor Service?");
            break;
         }
         logger.info("progress finished? " + detailedStatus.getOperationStatus().isFinished());
         logger.debug(detailedStatus.toString());
         logger.info("progress query completed");
         if (detailedStatus.getOperationStatus().getProgress() < 100) {
            int progress = detailedStatus.getOperationStatus().getProgress();
            statusUpdater.setProgress(((double) progress) / 100);
         }
         setLastErrorMsg(detailedStatus.getOperationStatus().getErrorMsg());
         clusterEntityMgr.handleOperationStatus(targetName.split("-")[0], detailedStatus);

         // for large scale cluster (100+ nodes), don't need to query too frequent which will cause too much overhead on Chef Server
         if (queryInterval == QUERY_INTERVAL_DEFAULT) {
            int size = detailedStatus.getClusterData().getClusterSize();
            if (size > BIG_CLUSTER_NODES_COUNT) {
               queryInterval = Math.min(QUERY_INTERVAL_MAX, QUERY_INTERVAL_LONG * (size / BIG_CLUSTER_NODES_COUNT));
               logger.info("progress query interval set to " + queryInterval / 1000 + "s because this cluster has " + size + " nodes");
            }
         }
      }
      if (monitorClient != null) {
         monitorClient.close();
      }
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.command.TracedRunnable#onStart()
    */
   @Override
   public void onStart() throws Exception {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.command.TracedRunnable#onFinish()
    */
   @Override
   public void onFinish() throws Exception {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.command.TracedRunnable#onException(java.lang.Throwable)
    */
   @Override
   public void onException(Throwable t) {
      logger.error(t.getMessage());
   }

}
