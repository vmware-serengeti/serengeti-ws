/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager;

import com.vmware.bdd.dal.IAppManagerDAO;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * For ClouderaManager, Ambari, some ops are not supported in M9.
 * https://wiki.eng.vmware.com/BigData/Releases/M9/CM#Operations_support_matrix_for_new_app_manager:_Cloudera_Manager_and_Ambari
 */
@Component
public class UnsupportedOpsBlocker {
   private final static Logger LOGGER = Logger.getLogger(UnsupportedOpsBlocker.class);

   @Autowired
   private IClusterEntityManager clusterEntityManager;

   @Autowired
   private IAppManagerDAO appManager;


   public void blockUnsupportedOpsByCluster(String ops, String clusterName) {
      ClusterEntity clusterEntity = clusterEntityManager.findByName(clusterName);

      if (clusterEntity == null) {
         LOGGER.error(String.format("cluster %1s not found!", clusterName));
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (CommonUtil.isBlank(clusterEntity.getAppManager())) {
         LOGGER.error(String.format("cluster %1s has no app manager!", clusterName));
         throw BddException.CLUSTER_HAS_NO_APP_MGR(clusterName);
      }

      AppManagerEntity appMgrEntity = appManager.findByName(clusterEntity.getAppManager());

      if (appMgrEntity == null) {
         LOGGER.error(String.format("app manager %1s not found!", clusterEntity.getAppManager()));
         throw BddException.APP_MGR_NOT_FOUND(clusterName);
      }

      if (CommonUtil.isBlank(appMgrEntity.getType())) {
         LOGGER.error(String.format("app manager %1s has no type!", appMgrEntity.getName()));
         throw BddException.APP_MGR_TYPE_IS_BLANK(appMgrEntity.getName());
      }

      blockUnsupportedOpsByAppMgr(ops, appMgrEntity.getType());
   }

   public void blockUnsupportedOpsByAppMgr(String ops, String appMgr) {
      if (!Constants.IRONFAN.equals(appMgr)) {
         LOGGER.error(String.format("Ops %1s is blocked for appMgr (%2s)", ops, appMgr));
         throw BddException.UNSUPPORTED_OPS(ops, appMgr);
      }
   }
}
