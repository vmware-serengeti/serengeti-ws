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
package com.vmware.bdd.plugin.clouderamgr.service;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.cloudera.api.ApiRootResource;
import com.cloudera.api.ClouderaManagerClientBuilder;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostList;
import com.cloudera.api.v2.HostsResourceV2;
import com.cloudera.api.v6.RootResourceV6;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;

public class TestClouderaManagerFactory {

   private static ApiRootResource apiRootResource;
   private static RootResourceV6 rootResourceV6;
   private static HostsResourceV2 hostsResourceV2;
   private static ApiHostList apiHostList;

   @MockClass(realClass = ClouderaManagerClientBuilder.class)
   public static class MockClouderaManagerClientBuilder {
      private ClouderaManagerClientBuilder builder = new ClouderaManagerClientBuilder();
      @Mock
      public ClouderaManagerClientBuilder withHost(String host) {
         return builder;
      }

      @Mock
      public ClouderaManagerClientBuilder withPort(int port) {
         return builder;
      }

      @Mock
      public ClouderaManagerClientBuilder withUsernamePassword(String user, String password) {
         return builder;
      }

      @Mock
      public ApiRootResource build() {
         return apiRootResource;
      }
   }

   @Test
   public void testGetSoftwareManager() {
      Mockit.setUpMock(MockClouderaManagerClientBuilder.class);
      List<ApiHost> list = new ArrayList<ApiHost>();
      ApiHost host = new ApiHost();
      host.setHostname("127.0.0.1");
      host.setIpAddress("127.0.0.1");
      host.setHostId("host1");
      list.add(host);
      apiRootResource = Mockito.mock(ApiRootResource.class);
      rootResourceV6 = Mockito.mock(RootResourceV6.class);
      hostsResourceV2 = Mockito.mock(HostsResourceV2.class);
      apiHostList = Mockito.mock(ApiHostList.class);
      Mockito.when(apiHostList.getHosts()).thenReturn(list);
      Mockito.when(hostsResourceV2.readHosts(DataView.SUMMARY)).thenReturn(apiHostList);
      Mockito.when(rootResourceV6.getHostsResource()).thenReturn(hostsResourceV2);
      Mockito.when(apiRootResource.getRootV6()).thenReturn(rootResourceV6);
      SoftwareManagerFactory softwareManagerFactory = new ClouderaManagerFactory();
      ClouderaManagerImpl softwareManager =
            (ClouderaManagerImpl) softwareManagerFactory.getSoftwareManager(
                  "http://127.0.0.1:7180", "admin", "admin".toCharArray(),
                  "RSA_CERT");
      System.out.println(softwareManager.getCmServerHostId());
      assertEquals(softwareManager.getCmServerHostId(), "host1");
   }
}
