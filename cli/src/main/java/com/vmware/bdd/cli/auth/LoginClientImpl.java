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

import com.vmware.bdd.cli.commands.Constants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Http Login Client
 */
@Component
public class LoginClientImpl implements LoginClient {
   private final static Logger LOGGER = Logger.getLogger(LoginClientImpl.class);
   protected static final String SET_COOKIE_HEADER = "Set-Cookie";

   @Autowired
   private HttpClient client1;
   /**
    *
    * attempt login by posting credentials to serengeti server
    *
    * @param serengetiURL https://host:8443/serengeti/api/
    * @param userName vc user name
    * @param password vc password
    * @throws IOException connection exception
    */
   public LoginResponse login(final String serengetiURL, String userName, String password) throws IOException {
      String url = serengetiURL + Constants.REST_PATH_LOGIN;
      HttpPost loginPost = new HttpPost(url);


      NameValuePair[] loginCredentials = new NameValuePair[] {
            new BasicNameValuePair("j_username", userName) ,
            new BasicNameValuePair("j_password", password)
      };

      //handling non-ascii username and password. Encoding by Apache HTTP Client.
      HttpEntity requestEntity = new UrlEncodedFormEntity(Arrays.asList(loginCredentials),
            Charset.forName("UTF-8"));
      loginPost.setEntity(requestEntity);

      HttpResponse response;

      try {
         response = client1.execute(loginPost);

         LOGGER.debug("resp code is: " + response.getStatusLine());
         int responseCode = response.getStatusLine().getStatusCode();

         LoginResponse loginResponse;
         if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            //normal response
            String cookieValue = null;
            Header[] setCookieHeaders = response.getHeaders(SET_COOKIE_HEADER);

            if (ArrayUtils.isNotEmpty(setCookieHeaders)) {
               cookieValue = setCookieHeaders[0].getValue();

               if (StringUtils.isNotBlank(cookieValue) && cookieValue.contains(";")) {
                  cookieValue = cookieValue.split(";")[0];
               }
            }

            loginResponse = new LoginResponse(responseCode, cookieValue);
         } else {
            loginResponse = new LoginResponse(responseCode, null);
         }

         return loginResponse;
      } finally {
         loginPost.releaseConnection();
      }
   }
}
