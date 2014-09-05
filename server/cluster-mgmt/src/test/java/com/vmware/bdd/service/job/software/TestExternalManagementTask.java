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
package com.vmware.bdd.service.job.software;

import java.util.Map;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.vmware.bdd.service.job.software.external.ExternalManagementTask;
import com.vmware.bdd.service.sp.MockConcurrentClusterEntityManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;

public class TestExternalManagementTask {

   @Test
   public void createClusterTasklet() throws Exception {
      ExternalManagementTask task =
            new ExternalManagementTask("testCluster",
                  ManagementOperation.CREATE, new ClusterBlueprint(),
                  new MockStatusUpdator(),
                  new MockConcurrentClusterEntityManager(),
                  new MockSoftwareManager(), null);

      Map<String, Object> result = task.call();
      Assert.assertTrue("should get success result", (Boolean)result.get("succeed"));
   }
}
