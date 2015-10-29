/******************************************************************************
 *   Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.FileCopyUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.UserMgmtServer;
import com.vmware.bdd.exception.ValidationException;
import com.vmware.bdd.security.tls.UntrustedCertificateException;
import com.vmware.bdd.usermgmt.mocks.LdapsTrustStoreConfigMock;
import com.vmware.bdd.validation.ValidationError;

/**
 * Created By xiaoliangl on 12/1/14.
 */
@ContextConfiguration(locations = "classpath:/com/vmware/bdd/usermgmt/userMgmtServerValidService-test-context.xml")
public class TestUserMgmtServerValidService_Ldaps extends AbstractTestNGSpringContextTests {
   @Autowired
   private UserMgmtServerValidService validService;

   @BeforeClass
   public void setup() throws IOException {
      TestSssdConfigurationGenerator.setupSssdTemplates();
   }

   @AfterClass
   public void teardown() {
      TestSssdConfigurationGenerator.teardownSssdTemplates();
   }

   @BeforeMethod
   public void beforeMethod() throws IOException {
      InputStream is = TestUserMgmtServerValidService_Ldaps.class.getResourceAsStream("/com/vmware/bdd/usermgmt/keystore.jks");

      FileOutputStream fos = new FileOutputStream(LdapsTrustStoreConfigMock.KEY_STORE_PATH);

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
      new File(LdapsTrustStoreConfigMock.KEY_STORE_PATH).delete();
   }

   @Test
   public void testValidateCertificate_forceTrust() throws IOException {
      UserMgmtServer userMgmtServer = TestUserMgmtServerValidService_Ldap.loadTestData("ldaps-server.json");

      //expect no exception
      validService.validateServerInfo(userMgmtServer, true);
   }

   @Test(expectedExceptions = UntrustedCertificateException.class)
   public void testValidateCertificate_untrustedCert() throws IOException {
      UserMgmtServer userMgmtServer = TestUserMgmtServerValidService_Ldap.loadTestData("ldaps-server.json");

      //expect untrusted cert exception
      validService.validateServerInfo(userMgmtServer, false);
   }

   @Test(expectedExceptions = {ValidationException.class})
   public void testValidateCertificate_BadUrl() throws IOException {
      UserMgmtServer userMgmtServer = TestUserMgmtServerValidService_Ldap.loadTestData("ldaps-server-badurl.json");

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
