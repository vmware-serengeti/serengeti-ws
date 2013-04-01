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
package com.vmware.bdd.manager.task;

import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.bdd.command.MessageHandler;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.Constants;

public class VHMReceiveListener implements MessageHandler {

   private static final long serialVersionUID = -7523538749427650436L;

   private static final Logger logger = Logger
         .getLogger(VHMReceiveListener.class);

   private String clusterName;
   private StatusUpdater statusUpdater;

   public VHMReceiveListener() {
      super();
   }

   public VHMReceiveListener(String clusterName, StatusUpdater statusUpdater) {
      super();
      this.clusterName = clusterName;
      this.statusUpdater = statusUpdater;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public StatusUpdater getStatusUpdater() {
      return statusUpdater;
   }

   public void setStatusUpdater(StatusUpdater statusUpdater) {
      this.statusUpdater = statusUpdater;
   }

   @Override
   public void onMessage(Map<String, Object> mMap) {
      logger.debug("cluster limit " + clusterName
            + " task listener received message " + mMap);

      AuAssert.check(mMap.get(Constants.FINISH_FIELD) instanceof Boolean
            && mMap.get(Constants.SUCCEED_FIELD) instanceof Boolean
            && mMap.get(Constants.PROGRESS_FIELD) instanceof Double
            && (Double) mMap.get(Constants.PROGRESS_FIELD) <= 100);
   }

   @Override
   public void setProgress(double progress) {
      statusUpdater.setProgress(progress);
   }

}
