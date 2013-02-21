/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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

import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.BddMessageUtil;

public class VHMReceiveListener implements TaskListener {

   private static final long serialVersionUID = -7523538749427650436L;

   private static final Logger logger = Logger.getLogger(VHMReceiveListener.class);

   private String clusterName;

   public VHMReceiveListener(String clusterName) {
      super();
      this.clusterName = clusterName;
   }

   @Override
   public void onSuccess() {
      logger.debug("cluster limit " + clusterName
            + " task listener called onSuccess");

      ClusterEntity.updateStatus(clusterName, ClusterStatus.RUNNING);
   }

   @Override
   public void onFailure() {
      logger.debug("cluster limit listener called onFailure");

      // will not delete the cluster info, assuming the error can be recovered
      ClusterEntity.updateStatus(clusterName, ClusterStatus.RUNNING);
      logger.error("failed to cluster limit " + clusterName 
            + " set its status as RUNNING");
   }

   @Override
   public void onMessage(Map<String, Object> mMap) {
      logger.debug("cluster limit " + clusterName
            + " task listener received message " + mMap);

      AuAssert.check(mMap.get(BddMessageUtil.FINISH_FIELD) instanceof Boolean
            && mMap.get(BddMessageUtil.SUCCEED_FIELD) instanceof Boolean
            && mMap.get(BddMessageUtil.PROGRESS_FIELD) instanceof Double
            && (Double) mMap.get(BddMessageUtil.PROGRESS_FIELD) <= 100);
   }

   @Override
   public String[] getTaskCommand(String clusterName, String fileName) {
      return null;
   }

}
