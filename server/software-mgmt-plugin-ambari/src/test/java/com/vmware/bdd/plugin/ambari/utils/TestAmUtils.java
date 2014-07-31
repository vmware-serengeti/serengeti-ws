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
package com.vmware.bdd.plugin.ambari.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequest;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;

public class TestAmUtils {
   @Test
   public void testCreateInstallComponentRequest() {
      ApiHostsRequest request = AmUtils.createInstallComponentsRequest();
      String json = ApiUtils.objectToJson(request);
      System.out.println("Got json: " + json);
      String expectedString = "{\"Body\":{\"HostRoles\":{\"state\":\"INSTALLED\"}},\"RequestInfo\":{\"context\":\"Installing components\",\"query\":\"HostRoles/state=INIT\"}}";
//      Assert.assertTrue(json.equals(expectedString), "Should get: " + expectedString);
   }
}
