/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.cli.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.Connect;
import com.vmware.bdd.cli.auth.LoginClientImpl;
import com.vmware.bdd.cli.auth.LoginResponse;
import com.vmware.bdd.cli.commands.CookieCache;

/**
 * Test case for RestClient
 * Created By xiaoliangl on 8/27/14.
 */
@ContextConfiguration(locations = {"classpath:com/vmware/bdd/cli/command/tests/restclient-test-context.xml"})
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RestClientTest extends AbstractTestNGSpringContextTests {
   private static Object[][] DATA = null;

   static {
      ArrayList<Object[]> dataList = new ArrayList<>();

      dataList.add(new Object[]{new LoginResponse(200, "JSESSIONID=B6926322AF4D8A8B9CEF3906D5735D41"), Connect.ConnectType.SUCCESS});
      dataList.add(new Object[]{new LoginResponse(200, "JSESSIONID=B6926322AF4D8A8B9CEF3906D5735D41"), Connect.ConnectType.SUCCESS});
      dataList.add(new Object[]{new LoginResponse(401, null), Connect.ConnectType.UNAUTHORIZATION});
      dataList.add(new Object[]{new LoginResponse(500, null), Connect.ConnectType.ERROR});
      dataList.add(new Object[]{new LoginResponse(200, null), Connect.ConnectType.SUCCESS});
      dataList.add(new Object[]{new LoginResponse(302, null), Connect.ConnectType.ERROR});

      DATA = new Object[dataList.size()][];
      dataList.toArray(DATA);
   }

   @DataProvider(name = "RestClientTest.LoginDP")
   public static Object[][] getTestData() {
      return DATA;
   }


   @Autowired
   private RestClient restClient;

   @Autowired
   private LoginClientImpl loginClient;


   @Test(dataProvider = "RestClientTest.LoginDP")
   public void testLogin(LoginResponse loginResponse, Connect.ConnectType expectedConnectType) throws IOException {
      Mockito.when(loginClient.login(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(
            loginResponse);
      Connect.ConnectType connectType = restClient.connect("127.0.0.1:8443", "root", "vmware");

      Assert.assertEquals(connectType, expectedConnectType);

      if(loginResponse.getSessionId() != null) {
         Assert.assertEquals(loginResponse.getSessionId(), CookieCache.get(CookieCache.COOKIE));
      }
   }

   @AfterTest
   public void tearDown() {
      new File("cli.properties").delete();
   }

   @Test
   public void testLoginWithException() throws IOException {
      Mockito.when(loginClient.login(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenThrow(
            new IOException("can't connect to network")
      );

      Connect.ConnectType connectType = restClient.connect("127.0.0.1:8443", "root", "vmware");

      Assert.assertEquals(connectType, Connect.ConnectType.ERROR);

   }
}
