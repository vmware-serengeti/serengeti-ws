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
package com.vmware.bdd.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.BddErrorMessage;
import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.validation.ValidationError;
import com.vmware.bdd.validation.ValidationErrors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Created By xiaoliangl on 11/27/14.
 */
@ContextConfiguration(locations = "classpath:testrestapp.xml")
@WebAppConfiguration
@ActiveProfiles("webapp")
public class TestUserMgmtServerControllerValidation extends AbstractTestNGSpringContextTests {
   private final static Logger LOGGER = Logger.getLogger(TestUserMgmtServerControllerValidation.class);


   @Resource
   private WebApplicationContext webAppCtx;

   private MockMvc mockMvc;

   private ObjectMapper objectMapper = UnitTestUtil.getJsonObjectMapper();

   @BeforeClass
   public void before() {
      mockMvc = MockMvcBuilders.webAppContextSetup(webAppCtx).build();
   }

   private static Object[][] DATAs = {};

   private static final String[][] UMS_FIELDS= {
         {null, null, null, null, null, null, null, null, null},
         {"", null, "ou=users,dc=bde,dc=vmware,dc=com", "ou=groups,dc=bde,dc=vmware,dc=com", "", "", "", "", ""},
         {"ldapserver1", "LDAP", "ou=users,dc=bde,dc=vmware,dc=com", "ou=groups,dc=bde,dc=vmware,dc=com", "http://10.112.113.137:8080", "http://10.112.113.137:8080", "xiaoliangl", "password", "cn=hadoop-users,ou=groups,dc=bde,dc=vmware,dc=com"},
         {"ldapserver1", "LDAP", "ou=users,,dc=bde,dc=vmware,dc=com", "ou=groups-=,dc=bde,dc=vmware,dc=com", "ldap://10.112.113.137:8080", "", "xiaoliangl", "password", "cn=hadoop-users,ou=groups,dc=bde,dc=vmware,dc=com"},
   };

   private static final String[][][] ERRORS_FIELDS = {
         {
               {"name", "NotNull.userMgmtServer.name", "may not be null"},
               {"primaryUrl", "NotNull.userMgmtServer.primaryUrl", "may not be null"},
               {"userName", "NotNull.userMgmtServer.userName", "may not be null"},
               {"password", "NotNull.userMgmtServer.password", "may not be null"},
               {"baseGroupDn", "NotNull.userMgmtServer.baseGroupDn", "may not be null"},
               {"baseUserDn", "NotNull.userMgmtServer.baseUserDn", "may not be null"},
               {"type", "NotNull.userMgmtServer.type", "may not be null"},
               {"mgmtVMUserGroupDn", "NotNull.userMgmtServer.mgmtVMUserGroupDn", "may not be null"}
         },
         {
               {"name", "Size.userMgmtServer.name", "size must be between 1 and 50"},
               {"primaryUrl", "Size.userMgmtServer.primaryUrl", "size must be between 7 and 200"},
               {"userName", "Size.userMgmtServer.userName", "size must be between 1 and 50"},
               {"password", "Size.userMgmtServer.password", "size must be between 1 and 50"},
               {"type", "NotNull.userMgmtServer.type", "may not be null"},
               {"mgmtVMUserGroupDn", "Size.userMgmtServer.mgmtVMUserGroupDn", "size must be between 1 and 100"}
         },
         {
               {"secondaryUrl", "LdapUrlFormat.userMgmtServer.secondaryUrl", "Bad LDAP URL"},
               {"primaryUrl", "LdapUrlFormat.userMgmtServer.primaryUrl", "Bad LDAP URL"},
         },
         {
               {"baseGroupDn", "DnFormat.userMgmtServer.baseGroupDn", "Bad Distinguished Names"},
               {"baseUserDn", "DnFormat.userMgmtServer.baseUserDn", "Bad Distinguished Names"},
         }
   };


   static {
      List<Object[]> data = new ArrayList<>();

      UserMgmtServer userMgmtServer = null;
      ValidationErrors validationErrors = null;
      ValidationError validationError = null;

      for (int i = 0; i < UMS_FIELDS.length; i++) {
         String[] umsFields = UMS_FIELDS[i];
         userMgmtServer = new UserMgmtServer(umsFields[0], umsFields[1] == null ? null : UserMgmtServer.Type.valueOf(umsFields[1]), umsFields[2], umsFields[3], umsFields[4], umsFields[5], umsFields[6], umsFields[7], umsFields[8]);

         validationErrors = new ValidationErrors();

         String[][] errorFields = ERRORS_FIELDS[i];
         for (int j = 0; j < errorFields.length; j++) {
            validationError = new ValidationError(errorFields[j][1], errorFields[j][2]);
            validationErrors.addError(errorFields[j][0], validationError);
         }

         data.add(new Object[]{userMgmtServer, validationErrors});
      }

      DATAs = data.toArray(DATAs);
   }

   @DataProvider(name = "errorDatas")
   Object[][] getTestData() { return DATAs;}

   @Test(dataProvider = "errorDatas")
   public void test(UserMgmtServer userMgmtServer, final ValidationErrors validationErrors) throws Exception {
      MockHttpServletRequestBuilder builder = post("/vmconfig/usermgmtservers").contentType(MediaType.APPLICATION_JSON).content(UnitTestUtil.convertObjectToJsonBytes(userMgmtServer));

      ResultActions result = mockMvc.perform(builder);

      result.andDo(new ResultHandler() {
         @Override
         public void handle(MvcResult result) throws Exception {
            String jsonResponse = result.getResponse().getContentAsString();

            LOGGER.info("response code is: " + result.getResponse().getStatus());

            BddErrorMessage actualValidationErrors = objectMapper.readValue(jsonResponse, BddErrorMessage.class);

            Map<String, ValidationError> actualErrors = actualValidationErrors.getErrors();
            Map<String, ValidationError> expectErrors = validationErrors.getErrors();

            Assert.assertEquals(actualErrors.size(), expectErrors.size());

            TestUserMgmtServerControllerValidation.assertMaps(actualErrors, expectErrors);
         }
      });

      System.out.println(result);
   }

   public static  <K, V> void assertMaps(Map<K, V> actualErrors, Map<K, V> expectErrors) {
      int equals = 0;

      for(Map.Entry<K, V> actualEntry : actualErrors.entrySet()) {
         if(actualEntry.getValue().equals(expectErrors.get(actualEntry.getKey()))) {
            equals ++;
         } else {
            LOGGER.error("found not equals: " + actualEntry.getValue() + " VS. " + expectErrors.get(actualEntry.getKey()));
         }
      }

      Assert.assertEquals(equals, expectErrors.size());
   }
}
