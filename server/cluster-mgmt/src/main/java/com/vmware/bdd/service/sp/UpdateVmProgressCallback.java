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

import java.util.concurrent.Callable;

import com.vmware.aurora.composition.concurrent.ExecutionResult;
import com.vmware.bdd.manager.ClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;

public class UpdateVmProgressCallback extends BaseProgressCallback {
   private ClusterEntityManager clusterEntityMgr;
   private String clusterName;
   public UpdateVmProgressCallback(ClusterEntityManager clusterEntityMgr,
         StatusUpdater statusUpdator, String clusterName) {
      super(statusUpdator);
      this.clusterEntityMgr = clusterEntityMgr;
      this.clusterName = clusterName;
   }

   @Override
   public void progressUpdate(Callable<Void> sp, ExecutionResult result, boolean compensate, int total) {
      super.progressUpdate(sp, result, compensate, total);
      clusterEntityMgr.syncUp(clusterName, false);
   }
}
