/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.impl;

import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.manager.concurrent.AsyncExecutors;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

/**
 * Created by xiaoliangl on 9/8/15.
 */
@Component
public class NodeSyncService {
   private static final Logger logger = Logger.getLogger(ClusterSyncService.class);

   @Autowired
   private IClusterEntityManager clusterEntityMgr;

   @Async(AsyncExecutors.NODE_SYNC_EXEC)
   public Future<NodeRead> asyncRefreshNodeStatus(String vmName) {
         logger.info("start to sync node: " + vmName);

      NodeRead nodeRead = clusterEntityMgr.refreshNodeStatus(vmName, false);
      if (logger.isDebugEnabled()) {
         logger.debug("finish to sync node: " + vmName);
      }
      return new AsyncResult<>(nodeRead);
   }
}
