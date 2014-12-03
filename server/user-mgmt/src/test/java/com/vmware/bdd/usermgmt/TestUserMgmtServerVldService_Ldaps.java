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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import com.vmware.bdd.validation.ValidationError;

/**
 * Created By xiaoliangl on 12/1/14.
 */
@ContextConfiguration(classes = {TestUserMgmtServerValidServiceContext.class})
public class TestUserMgmtServerVldService_Ldaps extends AbstractTestNGSpringContextTests {

   @Autowired
   private UserMgmtServerValidService validService;

   @BeforeMethod
   public void beforeMethod() throws IOException {
      InputStream is = TestUserMgmtServerVldService_Ldaps.class.getResourceAsStream("/com/vmware/bdd/usermgmt/keystore.jks");

      FileOutputStream fos = new FileOutputStream(TestUserMgmtServerValidServiceContext.KEY_STORE_PATH);

      try {
         byte[] temp = new byte[512];

         int count = is.read(temp);

         while (count != -1) {
            fos.write(temp, 0, count);
            count = is.read(temp);
         }
      } finally {
         try {
            is.close();
         } finally {
            fos.close();
         }
      }
   }

   @AfterMethod
   public void afterMethod() {
      new File(TestUserMgmtServerValidServiceContext.KEY_STORE_PATH).delete();
   }

   @Test(expectedExceptions = {UntrustedCertificateException.class})
   public void testValidateCertificate_UntrustedCert() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerVldService_Ldaps.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldaps-server.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      System.out.println(new Gson().toJson(userMgmtServer));

      validService.validateCertificate(userMgmtServer, false);
   }

   @Test
   public void testValidateCertificate_UntrustedCert1() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerVldService_Ldaps.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldaps-server.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      System.out.println(new Gson().toJson(userMgmtServer));

      System.setProperty("javax.net.ssl.trustStore", TestUserMgmtServerValidServiceContext.KEY_STORE_PATH);
      System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

      validService.validateServerInfo(userMgmtServer, true);
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateCertificate_BadUrl() {
      Gson gson = new Gson();

      InputStream ris = TestUserMgmtServerVldService_Ldaps.class.getResourceAsStream("/com/vmware/bdd/usermgmt/ldaps-server-badurl.json");

      UserMgmtServer userMgmtServer = gson.fromJson(new InputStreamReader(ris), UserMgmtServer.class);

      System.out.println(new Gson().toJson(userMgmtServer));

      System.setProperty("javax.net.ssl.trustStore", TestUserMgmtServerValidServiceContext.KEY_STORE_PATH);
      System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
      try {
         validService.validateServerInfo(userMgmtServer, true);

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
