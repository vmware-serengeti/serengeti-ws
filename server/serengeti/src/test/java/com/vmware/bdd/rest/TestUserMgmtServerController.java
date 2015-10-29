/******************************************************************************
 *   Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.rest;

import java.io.InputStreamReader;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.usermgmt.persist.UserMgmtServerEao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Created By xiaoliangl on 11/27/14.
 */
@ContextConfiguration(locations = { "classpath:mocks.xml", "classpath:test-usermgmtserver-controller.xml",
      "classpath:datasource-context.xml", "classpath:tx-context.xml" })
@WebAppConfiguration
@ActiveProfiles("webapp")
public class TestUserMgmtServerController extends AbstractTestNGSpringContextTests {

   @Autowired
   private UserMgmtServerEao serverEao;

   @Autowired
   private ClusterManager clusterMgr;

   @Resource
   private WebApplicationContext webAppCtx;

   private MockMvc mockMvc;

   private ObjectMapper objectMapper = UnitTestUtil.getJsonObjectMapper();

   @BeforeClass
   public void before() {
      mockMvc = MockMvcBuilders.webAppContextSetup(webAppCtx).build();
   }


   @Test
   public void testAdd() throws Exception {
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            new InputStreamReader(TestUserMgmtServerController.class.getResourceAsStream("/com/vmware/bdd/rest/ldap-server.json")),
            UserMgmtServer.class);

      MockHttpServletRequestBuilder builder = post("/vmconfig/usermgmtservers")
            .contentType(MediaType.APPLICATION_JSON).content(UnitTestUtil.convertObjectToJsonBytes(userMgmtServer));

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(200, result.getResponse().getStatus());
            Assert.assertEquals(0, result.getResponse().getContentLength());
         }
      });
   }

   @Test
   public void testModify() throws Exception {
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            new InputStreamReader(TestUserMgmtServerController.class.getResourceAsStream("/com/vmware/bdd/rest/ldap-server.json")),
            UserMgmtServer.class);

      MockHttpServletRequestBuilder builder = put("/vmconfig/usermgmtservers/" + userMgmtServer.getName())
            .contentType(MediaType.APPLICATION_JSON).content(UnitTestUtil.convertObjectToJsonBytes(userMgmtServer));

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(200, result.getResponse().getStatus());
            Assert.assertEquals(0, result.getResponse().getContentLength());
         }
      });
   }

   @Test
   public void testGet() throws Exception {
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            new InputStreamReader(TestUserMgmtServerController.class.getResourceAsStream("/com/vmware/bdd/rest/ldap-server.json")),
            UserMgmtServer.class);

      Mockito.when(this.serverEao.findByName(Mockito.anyString(), Mockito.eq(true))).thenReturn(userMgmtServer);

      MockHttpServletRequestBuilder builder = get("/vmconfig/usermgmtservers/" + userMgmtServer.getName());

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(200, result.getResponse().getStatus());
            Assert.assertEquals(0, result.getResponse().getContentLength());
         }
      });
   }

   @Test
   public void testEnableLdap_ExceptionalClusterName() throws Exception {
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            new InputStreamReader(TestUserMgmtServerController.class.getResourceAsStream("/com/vmware/bdd/rest/ldap-server.json")),
            UserMgmtServer.class);

//      Mockito.when(this.serverEao.findByName(Mockito.anyString(), Mockito.eq(true))).thenReturn(userMgmtServer);

      MockHttpServletRequestBuilder builder = post("/vmconfig/usermgmtservers/enableLdap/default server")
            .contentType(MediaType.APPLICATION_JSON).content(UnitTestUtil.convertObjectToJsonBytes(userMgmtServer));

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(400, result.getResponse().getStatus());
            BddErrorMessage errMsg = objectMapper.readValue(result.getResponse().getContentAsString(),
                  BddErrorMessage.class);

            Assert.assertNotNull(errMsg);
         }
      });
   }

   @Test
   public void testDelete() throws Exception {
      MockHttpServletRequestBuilder builder = delete("/vmconfig/usermgmtservers/ldap_server");

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(200, result.getResponse().getStatus());
            Assert.assertEquals(0, result.getResponse().getContentLength());
         }
      });
   }

   @Test
   public void testEnableLdap_EnableLdapException() throws Exception {
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            new InputStreamReader(TestUserMgmtServerController.class.getResourceAsStream("/com/vmware/bdd/rest/ldap-server.json")),
            UserMgmtServer.class);

      Mockito.when(this.clusterMgr.enableLdap(Mockito.anyString())).thenThrow(new Exception());

      MockHttpServletRequestBuilder builder = post("/vmconfig/usermgmtservers/enableLdap/default server")
            .contentType(MediaType.APPLICATION_JSON).content(UnitTestUtil.convertObjectToJsonBytes(userMgmtServer));

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(400, result.getResponse().getStatus());
            BddErrorMessage errMsg = objectMapper.readValue(result.getResponse().getContentAsString(),
                  BddErrorMessage.class);

            Assert.assertNotNull(errMsg);
         }
      });
   }

   @Test
   public void testEnableLdap() throws Exception {
      UserMgmtServer userMgmtServer = objectMapper.readValue(
            new InputStreamReader(TestUserMgmtServerController.class.getResourceAsStream("/com/vmware/bdd/rest/ldap-server.json")),
            UserMgmtServer.class);

//      Mockito.when(this.serverEao.findByName(Mockito.anyString(), Mockito.eq(true))).thenReturn(userMgmtServer);

      MockHttpServletRequestBuilder builder = post("/vmconfig/usermgmtservers/enableLdap/cluster1")
            .contentType(MediaType.APPLICATION_JSON).content(UnitTestUtil.convertObjectToJsonBytes(userMgmtServer));

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            Assert.assertEquals(200, result.getResponse().getStatus());
            Assert.assertEquals(0, result.getResponse().getContentLength());
         }
      });
   }
}
