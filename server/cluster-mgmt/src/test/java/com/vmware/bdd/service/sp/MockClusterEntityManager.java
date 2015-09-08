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

import org.mockito.Mockito;

import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.manager.ClusterEntityManager;

@MockClass(realClass = ClusterEntityManager.class)
public class MockClusterEntityManager extends
      ClusterEntityManager {
   private boolean started;

   public synchronized boolean isStarted() {
      return started;
   }

   public synchronized void setStarted(boolean started) {
      this.started = started;
   }

   @Mock
   public NodeEntity getNodeByMobId(String vmId) {
      return Mockito.mock(NodeEntity.class);
   }

   @Mock
   public NodeEntity getNodeByVmName(String vmName) {
      return Mockito.mock(NodeEntity.class);
   }

   @Mock
   synchronized public void refreshNodeByMobId(String vmId, String action,
         boolean inSession) {
   }

   @Mock
   synchronized public void refreshNodeByVmName(String vmId, String vmName,
         String nodeAction, boolean inSession) {
   }

  /* @Override
   public void syncUp(String clusterName, boolean updateClusterStatus) {
      try {
         started = true;
         Thread.sleep(200);
         System.out.println("Sleep 200ms.");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }*/

   public void removeVmReference(String vmId) {
   }
}
