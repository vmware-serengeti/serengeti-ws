/******************************************************************************
 *   Copyright (c) 2012-2013 VMware, Inc. All Rights Reservedrved
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
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import java.net.URI;

import org.testng.annotations.BeforeMethod;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatchers;
import org.springframework.test.web.client.ResponseCreators;
import org.springframework.web.client.RestTemplate;

@ContextConfiguration(locations = { "classpath:com/vmware/bdd/cli/command/tests/test-context.xml" })public abstract class MockRestServer extends AbstractTestNGSpringContextTests {
   @Autowired
   private RestTemplate restTemplate;
   
   protected MockRestServiceServer mockServer;
   
   @BeforeMethod
   public void setup() {
      mockServer = MockRestServiceServer.createServer(restTemplate);
   }
   
   protected void buildReqRespWithoutReqBody(final String restUrl,
         final HttpMethod method, final HttpStatus status,
         final String respBody, final String... location) throws Exception {
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.APPLICATION_JSON);
     
      if (location != null&& location.length > 0) {
         responseHeaders.setContentLength(0l);
         responseHeaders.setLocation(new URI(location[0]));
      }
      
      mockServer.expect(RequestMatchers.requestTo(restUrl)).andExpect(RequestMatchers.method(method))
      .andRespond(ResponseCreators.withResponse(respBody, responseHeaders, status, ""));
   }
   
   protected void buildReqRespWithoutRespBody(final String restUrl,
         final HttpMethod method, final HttpStatus status, final String reqBody) {
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.APPLICATION_JSON);
      
      mockServer
            .expect(RequestMatchers.requestTo(restUrl))
            .andExpect(RequestMatchers.method(method))
            .andExpect(RequestMatchers.header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .andExpect(RequestMatchers.body(reqBody))
            .andRespond(
                  ResponseCreators
                        .withResponse("", responseHeaders, status, ""));
   }
}
