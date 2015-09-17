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
package com.vmware.bdd.cli.auth;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import sun.net.httpserver.HttpServerImpl;

/**
 * Login Client Test
 * Created by Xiaoliangl on 8/26/14.
 */
public class LoginClientImplTest {

   private final static Object[][] DATA = new Object[][]{
         {"root", "vmware", 200, "B6926322AF4D8A8B9CEF3906D5735D41"},
         {"ro ot", "vmw are", 200, "B6926322AF4D8A8B9CEF3906D5735D41"},
         {"ro你好ot", "vmw你好are", 200, "B6926322AF4D8A8B9CEF3906D5735D41"},
         {"root", "vmware", 500, null},
         {"root", "vmware", 302, null}

   };


   @DataProvider(name = "loginClientImplTest.Default")
   public Object[][] getTestData() {
      return DATA;
   }

   @Test(dataProvider = "loginClientImplTest.Default")
   public void testLogin(String userName,
                         String passWord,
                         int responseCode,
                         String sessionId) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
      LoginTestTemplate testTemplate = new LoginTestTemplate(userName, passWord, responseCode, sessionId);
      testTemplate.start();

      try {
         LoginClientImpl loginClient = new LoginClientImpl();
         LoginResponse loginResponse = loginClient.login("http://127.0.0.1:4587/serengeti/j_spring_security_check", userName, passWord);

         Assert.assertEquals(loginResponse.getResponseCode(), responseCode);

         Assert.assertEquals(loginResponse.toString(),
               String.format("responseCode:%1s;sessionId:%2s", loginResponse.getResponseCode(), loginResponse.getSessionId()));

         if(sessionId != null) {
            Assert.assertEquals(loginResponse.getSessionId(), "JSESSIONID=" + sessionId);
         }
      } finally {
         testTemplate.stop();
      }

   }

}

class LoginTestTemplate {
   private HttpServer httpServer;

   private HttpHandler httpHandler;

   public LoginTestTemplate(final String expectedUserName, final String expectedPassword, final int responseCode, final String sessionId) {

      httpHandler = new HttpHandler() {
         @Override
         public void handle(HttpExchange httpExchange) throws IOException {
            Headers headers = httpExchange.getRequestHeaders();

            Assert.assertEquals("application/x-www-form-urlencoded; charset=UTF-8", headers.getFirst("Content-Type"));
            Assert.assertEquals("POST", httpExchange.getRequestMethod());

            InputStream reqStream = httpExchange.getRequestBody();

            Reader reader = new InputStreamReader(reqStream);

            StringBuilder sb = new StringBuilder();

            char[] tmp = new char[16];
            int count = reader.read(tmp);
            while (count > 0) {
               sb.append(tmp, 0, count);
               count = reader.read(tmp);
            }

//            String val = URLDecoder.decode(sb.toString(), "UTF-8");

            List<NameValuePair> namePasswordPairs = URLEncodedUtils.parse(sb.toString(), Charset.forName("UTF-8"));
            Assert.assertEquals(namePasswordPairs.get(0).getValue(), expectedUserName);
            Assert.assertEquals(namePasswordPairs.get(1).getValue(), expectedPassword);

            if (sessionId != null) {
               Headers responseHeaders = httpExchange.getResponseHeaders();
               responseHeaders.set(LoginClientImpl.SET_COOKIE_HEADER, "JSESSIONID=" + sessionId + "; Path=/serengeti; Secure");
            }

            String response = "LoginClientImplTest";
            httpExchange.sendResponseHeaders(responseCode, response.length());

            BufferedOutputStream os = new BufferedOutputStream(httpExchange.getResponseBody());

            os.write(response.getBytes());
            os.close();

         }
      };
   }

   public void start() throws IOException {
      InetSocketAddress ipAddr = new InetSocketAddress("127.0.0.1", 4587);
      httpServer = HttpServerImpl.create(ipAddr, 4587);
      httpServer.createContext("/serengeti/j_spring_security_check", httpHandler);
      httpServer.start();
   }

   public void stop() {
      httpServer.stop(5);
   }
}
