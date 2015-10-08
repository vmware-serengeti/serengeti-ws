/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.cli.http;

import com.vmware.bdd.cli.commands.Constants;
import com.vmware.bdd.cli.config.RunWayConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by xiaoliangl on 10/8/15.
 */
@ContextConfiguration(locations = {"classpath:META-INF/spring/spring-shell-plugin.xml"})
public class TestSecureHttpClient extends AbstractTestNGSpringContextTests {
   private final static Logger LOGGER = Logger.getLogger(TestSecureHttpClient.class);
   @Autowired
   private HttpClient secureHttpClient;

   public void test1() throws IOException {
      RunWayConfig.autoRun();
      LOGGER.info("start");
      String url = "https://10.117.8.239:443/" + Constants.REST_PATH_LOGIN;
      HttpPost loginPost = new HttpPost(url);


      NameValuePair[] loginCredentials = new NameValuePair[] {
            new BasicNameValuePair("j_username", "userName") ,
            new BasicNameValuePair("j_password", "password")
      };

      //handling non-ascii username and password. Encoding by Apache HTTP Client.
      HttpEntity requestEntity = new UrlEncodedFormEntity(Arrays.asList(loginCredentials),
            Charset.forName("UTF-8"));
      loginPost.setEntity(requestEntity);

      HttpResponse loginResponse = secureHttpClient.execute(loginPost);

      LOGGER.debug(loginResponse.getStatusLine());
   }
}
