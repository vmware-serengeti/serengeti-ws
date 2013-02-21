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
import com.vmware.bdd.dal.DAL;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.Saveable;
import com.vmware.bdd.manager.NetworkManager;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.BddMessageUtil;
import com.vmware.bdd.utils.ClusterCmdUtil;

public class DeleteClusterListener implements TaskListener {
   private static final long serialVersionUID = 4700192439286556099L;

   private static final Logger logger = Logger.getLogger(DeleteClusterListener.class);

   private String clusterName;
   private NetworkManager networkManager;

   public DeleteClusterListener(String clusterName,
         NetworkManager networkManager) {
      super();
      this.clusterName = clusterName;
      this.networkManager = networkManager;
   }

   @Override
   public void onSuccess() {
      logger.debug("delete cluster " + clusterName
            + " task listener called onSuccess");
      DAL.inRwTransactionDo(new Saveable<Void>() {

         @Override
         public Void body() throws Exception {
            ClusterEntity cluster =
                  ClusterEntity.findClusterEntityByName(clusterName);
            AuAssert.check(cluster != null);

            if (cluster.getNetwork().getAllocType() == NetworkEntity.AllocType.IP_POOL) {
               networkManager.free(cluster.getNetwork(), cluster.getId());
            }
            cluster.delete();
            return null;
         }
      });
   }

   @Override
   public void onFailure() {
      logger.debug("delete cluster listener called onFailure");

      ClusterEntity.updateStatus(clusterName, ClusterStatus.ERROR);
      logger.error("failed to delete cluster " + clusterName 
            + " set its status as ERROR");
   }

   @Override
   public void onMessage(Map<String, Object> mMap) {
      logger.debug("delete cluster " + clusterName
            + " task listner received message " + mMap);

      BddMessageUtil.validate(mMap, clusterName);
   }

   public String[] getTaskCommand(String clusterName, String fileName) {
      return ClusterCmdUtil.getDeleteClusterCmdArray(clusterName, fileName);
   }
}
