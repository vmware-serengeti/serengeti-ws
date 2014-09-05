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
package com.vmware.bdd.service.sp;

import mockit.Mock;
import mockit.MockClass;

import com.vmware.bdd.manager.ConcurrentWriteLockedClusterEntityManager;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;

@MockClass(realClass = ConcurrentWriteLockedClusterEntityManager.class)
public class MockConcurrentClusterEntityManager extends
      ConcurrentWriteLockedClusterEntityManager {

   private static IClusterEntityManager clusterEntityMgr = new MockClusterEntityManager();
   @Mock
   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Mock
   synchronized public void refreshNodeByMobId(String vmId, String action,
         boolean inSession) {
   }

   @Mock
   synchronized public void refreshNodeByVmName(String vmId, String vmName,
         String nodeAction, boolean inSession) {
   }

   @Mock
   public boolean handleOperationStatus(String clusterName,
         ClusterReport report, boolean lastUpdate) {
      return report.isFinished();
   }

}
