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
package com.vmware.bdd.plugin.ambari.service;

import java.net.URL;

import junit.framework.Assert;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;

import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

public class TestAmbariFactory {

   private static ApiRootResource apiRootResource;

   @MockClass(realClass = ApiManager.class)
   public static class MockApiManager {
      @Mock
      public String healthCheck(){
         return "succeed";
      }
   }

   @MockClass(realClass = AmbariManagerClientbuilder.class)
   public static class MockAmbariManagerClientbuilder {

      private AmbariManagerClientbuilder builder = new AmbariManagerClientbuilder();

      @Mock
      public AmbariManagerClientbuilder withHost(String host) {
         return builder;
      }

      @Mock
      public AmbariManagerClientbuilder withPort(int port) {
         return builder;
      }

      @Mock
      public AmbariManagerClientbuilder withBaseURL(URL url) {
         return builder;
      }

      @Mock
      public AmbariManagerClientbuilder withUsernamePassword(String user, String password) {
         return builder;
      }

      @Mock
      public ApiRootResource build() {
         return apiRootResource;
      }
   }

   @Test
   public void testGetSoftwareManager() {
      Mockit.setUpMock(MockAmbariManagerClientbuilder.class);
      Mockit.setUpMock(MockApiManager.class);
      apiRootResource = Mockito.mock(ApiRootResource.class);
      SoftwareManagerFactory softwareManagerFactory = new AmbariFactory();
      AmbariImpl softwareManager = (AmbariImpl) softwareManagerFactory.getSoftwareManager(
                  "http://127.0.0.1:8080", "admin", "admin".toCharArray(), "RSA_CERT");
      Assert.assertEquals(Constants.AMBARI_PLUGIN_NAME, softwareManager.getName());
   }
}
