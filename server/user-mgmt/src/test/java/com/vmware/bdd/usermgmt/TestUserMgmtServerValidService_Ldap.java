/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.usermgmt;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.validation.ValidationError;

/**
 * Created By xiaoliangl on 12/1/14.
 */
@ContextConfiguration(classes = {TestUserMgmtServerValidServiceContext.class})
public class TestUserMgmtServerValidService_Ldap extends AbstractTestNGSpringContextTests {

   @Autowired
   private UserMgmtServerValidService validService;

   @Test
   public void testValidateServerInfo() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      validService.searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateServerInfo_DnNotFound() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server-dnnotfound.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      System.out.println(new Gson().toJson(userMgmtServer));

      try {
         validService.searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());
      } catch (ValidationException ve) {
         Assert.assertFalse(ve.getErrors().isEmpty());

         Map<String, ValidationError> errorMap = ve.getErrors();
         Assert.assertEquals(errorMap.size(), 2);

         ValidationError validationError = errorMap.get("BaseUserDn");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "BASE_USER_DN.NOT_FOUND");

         validationError = errorMap.get("BaseGroupDn");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "BASE_GROUP_DN.NOT_FOUND");

         throw ve;
      }

   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateServerInfo_AdmGroupNotFound() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server-admgroupnotfound.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      System.out.println(new Gson().toJson(userMgmtServer));

      try {
         validService.validateServerInfo(userMgmtServer, false);
      } catch (ValidationException ve) {
         Assert.assertFalse(ve.getErrors().isEmpty());

         Map<String, ValidationError> errorMap = ve.getErrors();
         Assert.assertEquals(errorMap.size(), 1);

         ValidationError validationError = errorMap.get("AdminGroup");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "ADMIN_GROUP.NOT_FOUND");

         throw ve;
      }
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateServerInfo_wrongCredential() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server-badcredential.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      try {
         validService.searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());
      } catch (ValidationException ve) {
         Assert.assertFalse(ve.getErrors().isEmpty());

         Map<String, ValidationError> errorMap = ve.getErrors();
         Assert.assertEquals(errorMap.size(), 1);

         ValidationError validationError = errorMap.get("UserCredential");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "UserCredential.Invalid");

         throw ve;
      }
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateServerInfo_invalidUserName() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server-invalidusername.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      try {
         validService.searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());
      } catch (ValidationException ve) {
         Assert.assertFalse(ve.getErrors().isEmpty());

         Map<String, ValidationError> errorMap = ve.getErrors();
         Assert.assertEquals(errorMap.size(), 1);

         ValidationError validationError = errorMap.get("UserName");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "USERNAME.INVALID_DN");

         throw ve;
      }
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateServerInfo_invalidDn() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server-invaliddn.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      try {
         validService.searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());
      } catch (ValidationException ve) {
         Assert.assertFalse(ve.getErrors().isEmpty());

         Map<String, ValidationError> errorMap = ve.getErrors();
         Assert.assertEquals(errorMap.size(), 2);

         ValidationError validationError = errorMap.get("BaseUserDn");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "BASE_USER_DN.INVALID_DN");

         validationError = errorMap.get("BaseGroupDn");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "BASE_GROUP_DN.INVALID_DN");

         throw ve;
      }
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateServerInfo_badUrl() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerValidService_Ldap.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldap-server-badurl.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      try {
         validService.searchGroupDn(userMgmtServer, userMgmtServer.getMgmtVMUserGroupDn());
      } catch (ValidationException ve) {
         Assert.assertFalse(ve.getErrors().isEmpty());

         Map<String, ValidationError> errorMap = ve.getErrors();
         Assert.assertEquals(errorMap.size(), 1);

         ValidationError validationError = errorMap.get("PrimaryUrl");
         Assert.assertNotNull(validationError);
         Assert.assertEquals(validationError.getPrimaryCode(), "PrimaryUrl.CannotConnect");

         throw ve;
      }
   }
}
