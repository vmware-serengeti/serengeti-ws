/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.ClusterRead.ClusterStatus;
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.BddMessageUtil;
import com.vmware.bdd.utils.ClusterCmdUtil;

public class UpdateClusterListener implements TaskListener {
   private static final long serialVersionUID = -4144721086697547540L;

   private static final Logger logger = Logger
         .getLogger(UpdateClusterListener.class);

   private String clusterName;

   public UpdateClusterListener(String clusterName) {
      super();
      this.clusterName = clusterName;
   }

   @Override
   public void onSuccess() {
      logger.debug("update cluster " + clusterName
            + " task listener called onSuccess");

      ClusterEntity.updateStatus(clusterName, ClusterStatus.RUNNING);
   }

   @Override
   public void onFailure() {
      logger.debug("update cluster listener called onFailure");
      DAL.inRwTransactionDo(new Saveable<Void>() {
         @Override
         public Void body() throws Exception {
            ClusterEntity cluster =
                  ClusterEntity.findClusterEntityByName(clusterName);
            AuAssert.check(cluster != null);
            cluster.setStatus(ClusterStatus.RUNNING);
            logger.error("failed to update cluster " + clusterName
                  + " revert to previous status");
            return null;
         }
      });
   }

   @Override
   public void onMessage(Map<String, Object> mMap) {
      logger.debug("update cluster " + clusterName
            + " task listner received message " + mMap);

      BddMessageUtil.validate(mMap, clusterName);

      ClusterEntity cluster =
            ClusterEntity.findClusterEntityByName(clusterName);
      AuAssert.check(cluster != null);

      // parse cluster data from message and store them in db
      String description =
            (new Gson()).toJson(mMap.get(BddMessageUtil.CLUSTER_DATA_FIELD));
      BddMessageUtil.processClusterData(clusterName, description);
   }

   public String[] getTaskCommand(String clusterName, String fileName) {
      return ClusterCmdUtil.getUpdatetClusterCmdArray(clusterName, fileName);
   }
}
