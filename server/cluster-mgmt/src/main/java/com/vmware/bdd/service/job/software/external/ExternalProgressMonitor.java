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

import java.util.List;

import org.apache.log4j.Logger;

import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.utils.TracedRunnable;

/**
 * @author Jarred Li
 * @version 0.8
 * @since 0.8
 *
 */
public class ExternalProgressMonitor extends TracedRunnable {
   private static final Logger logger = Logger.getLogger(ExternalProgressMonitor.class);
   private static final long QUERY_INTERVAL_DEFAULT = 1000 * 10; // 10 seconds

   private ClusterReportQueue queue;
   private String targetName;
   private StatusUpdater statusUpdater;
   private String lastErrorMsg = null;
   private long queryInterval = QUERY_INTERVAL_DEFAULT;
   private ILockedClusterEntityManager clusterEntityMgr;
   private volatile boolean stop;

   public ExternalProgressMonitor(String targetName, ClusterReportQueue queue,
         StatusUpdater statusUpdater,
         ILockedClusterEntityManager clusterEntityMgr) {
      this.targetName = targetName;
      this.queue = queue;
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
         List<ClusterReport> reports = queue.pollClusterReport();
         if (reports.isEmpty()) {
            logger.debug("No reports found. Waiting ...");
            continue;
         }
         ClusterReport lastestReport = reports.get(reports.size() - 1);
         lastestReport.getProgress();
         logger.info("progress finished? " + lastestReport.isFinished());
         if (!lastestReport.isFinished()) {
            int progress = lastestReport.getProgress();
            if (statusUpdater != null) {
               statusUpdater.setProgress(((double) progress) / 100);
            }
            logger.info("cluster  finished? " + lastestReport.isFinished());
         }

         setLastErrorMsg(lastestReport.getErrMsg());
         clusterEntityMgr.handleOperationStatus(targetName.split("-")[0], lastestReport, exit);
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
