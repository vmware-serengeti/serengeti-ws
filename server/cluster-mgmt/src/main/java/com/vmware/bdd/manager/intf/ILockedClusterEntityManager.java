/***************************************************************************
 * Copyright (c) 2013-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.manager.intf;

import com.vmware.bdd.software.mgmt.thrift.OperationStatusWithDetail;

public interface ILockedClusterEntityManager {
   public IClusterEntityManager getClusterEntityMgr();
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr);

   public boolean handleOperationStatus(String clusterName,
         OperationStatusWithDetail status, boolean lastUpdate);
   public void syncUp(String clusterName, boolean updateClusterStatus);

   public void removeVmReference(String clusterName, String vmId);

   public void syncUpNode(String clusterName, String nodeName);

   public void refreshNodeByMobId(String clusterName, String vmId,
         boolean inSession);

   public void setNodeConnectionState(String clusterName, String vmName);

   public void refreshNodeByMobId(String clusterName, String vmId,
         String action, boolean inSession);

   public void refreshNodeByVmName(String clusterName, String vmId,
         String vmName, boolean inSession);

   public void refreshNodeByVmName(String clusterName, String vmId,
         String vmName, String nodeAction, boolean inSession);
}
