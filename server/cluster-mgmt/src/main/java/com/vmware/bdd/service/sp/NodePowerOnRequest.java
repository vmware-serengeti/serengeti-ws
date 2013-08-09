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
package com.vmware.bdd.service.sp;

import org.apache.log4j.Logger;

import com.vmware.aurora.util.CmsWorker.SimpleRequest;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.utils.Constants;

public class NodePowerOnRequest extends SimpleRequest {
   private static final Logger logger = Logger.getLogger(NodePowerOnRequest.class);
   private ClusterEntityManager entityMgr;
   private String vmId;

   public NodePowerOnRequest(ClusterEntityManager entityMgr, String vmId) {
      this.entityMgr = entityMgr;
      this.vmId = vmId;
   }

   @Override
   protected boolean execute() {
      logger.info("Start to waiting for VM " + vmId +
      		" post power on status");
      QueryIpAddress query =
         new QueryIpAddress(Constants.VM_POWER_ON_WAITING_SEC);
      query.setVmId(vmId);
      try {
         query.call();
      } catch (Exception e) {
         logger.error("Failed to query ip address of vm: " + vmId);
      }
      entityMgr.refreshNodeByMobId(vmId, false);
      return true;
   }
}
