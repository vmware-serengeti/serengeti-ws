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

   private String targetName;
   private StatusUpdater statusUpdater;
   private int queryInteral = 1000 * 10;
   private ClusterEntityManager clusterEntityMgr;
   private volatile boolean stop;

   public ProgressMonitor(String targetName, StatusUpdater statusUpdater,
         ClusterEntityManager clusterEntityMgr) {
      this.targetName = targetName;
      this.statusUpdater = statusUpdater;
      this.clusterEntityMgr = clusterEntityMgr;
   }

   /**
    * @return the stop
    */
   public boolean isStop() {
      return stop;
   }

   /**
    * @param stop
    *           the stop to set
    */
   public void setStop(boolean stop) {
      this.stop = stop;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.command.TracedRunnable#doWork()
    */
   @Override
   public void doWork() throws Exception {
      logger.info("start monitor operation progress, target name :"
            + targetName);
      SoftwareManagementClient monitorClient = new SoftwareManagementClient();
      monitorClient.init();
      OperationStatusWithDetail detailedStatus =
            monitorClient.getOperationStatusWithDetail(targetName);
      logger.info("progress finished? "
            + detailedStatus.getOperationStatus().isFinished());
      while (detailedStatus != null && !stop) {
         if (detailedStatus.getOperationStatus().getProgress() < 100) {
            int progress = detailedStatus.getOperationStatus().getProgress();
            statusUpdater.setProgress(((double) progress) / 100);
         }
         clusterEntityMgr.handleOperationStatus(targetName.split("-")[0], detailedStatus);
         logger.info("operation has not finished. wait again");
         try {
            Thread.sleep(queryInteral);
         } catch (InterruptedException e) {
            logger.info("Monitor thread was stopped");
            break;
         }
         logger.info("before query progress");
         detailedStatus =
               monitorClient.getOperationStatusWithDetail(targetName);
         logger.info(detailedStatus.toString());
         logger.info("after query progress");
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
