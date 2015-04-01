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
package com.vmware.bdd.manager;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.entity.AppManagerEntity;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.utils.Constants;

/**
 * Created By xiaoliangl on 8/28/14.
 */
public class TestSoftwareManagerCollector_Utils {
   private final static String GOOD_CERT = "-----BEGIN CERTIFICATE-----\n" +
         "MIIDfzCCAmegAwIBAgIED9k4BTANBgkqhkiG9w0BAQsFADBwMRAwDgYDVQQGEwdV\n" +
         "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n" +
         "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRQwEgYDVQQDEwsxMC4xNDEu\n" +
         "Ny4zNTAeFw0xNDA4MjgwNzQ3MzNaFw0xNTAyMjQwNzQ3MzNaMHAxEDAOBgNVBAYT\n" +
         "B1Vua25vd24xEDAOBgNVBAgTB1Vua25vd24xEDAOBgNVBAcTB1Vua25vd24xEDAO\n" +
         "BgNVBAoTB1Vua25vd24xEDAOBgNVBAsTB1Vua25vd24xFDASBgNVBAMTCzEwLjE0\n" +
         "MS43LjM1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvKToZcD9au16\n" +
         "CN+Jk6+P9glncMO0Gn62h9V3Q0CU4Vve5oYK2hANfFk0kFaGXrE1qP+IXGBc8Rpi\n" +
         "ZAlj9jV1Syoqb6NkB8xaTyF2h6g6bCieYAvZObDCI8S5vtEabmGo/abA+wqiDPIv\n" +
         "LMNwYxhT8+WM7TdTXXd8w22rLIulPefTi4ao/buC18n9Ry0BdhD8yqBa4ZsUPY6r\n" +
         "xt7j5CmviG17heYSzZvcMx/lH6rqoEtc8v6O0J8FqeG4lxcY1NT6ZUC0z7eXIVFW\n" +
         "yl983QMf5BFDWDx1lNy/0rpyNjEtE9vAaUUGr78lOYM30qkmM7O5OJCh/3hh9fUw\n" +
         "wFXhA1nzjQIDAQABoyEwHzAdBgNVHQ4EFgQUWJUGp2QdHNDDWIx/oOQrwG+Hx84w\n" +
         "DQYJKoZIhvcNAQELBQADggEBAAY5hHIaQ20V4TSb9LapacjIv9UfCM4h263eX/GT\n" +
         "dl4Urpt5qkONw8v0waJYzx4x2BGjsFFUcIgTaDQ3RFouNPchQ0zouM260zEXMPGt\n" +
         "NHKgl3r/4V3bEq/an2OK8WqTJUiVLPmIxliQ/A5Tlv1pM6/i8RFsw+sc8ldQtOND\n" +
         "Sfig9ouhzU/bhT/Bs7xVaVy/wvHGfTmAd2nsOMPfPoIPv+nJmMKRPkJYsGgAvFvh\n" +
         "yQ1SCDIrtm49CxXDy2LdR88EiHCA+EZooOwvVQgLOzyeNyozOmXk8Ru/YlVfw+dE\n" +
         "dmgbEbcol0S/wLL9o5VacuVsBQGI22WkzHIHqjkrAT+lHNk=\n" +
         "-----END CERTIFICATE-----";
   private static Object[][] BAD_CERT_DATA = null;

   static {
      ArrayList<Object[]> bad_certs = new ArrayList<>();

      bad_certs.add(new Object[]{"-----BEGIN CERTIFICATE-----\n" + "-----END CERTIFICATE-----"});
      bad_certs.add(new Object[]{""});
      bad_certs.add(new Object[]{null});
      bad_certs.add(new Object[]{"abcdef"});


      bad_certs.add(new Object[]{"MIIDfzCCAmegAwIBAgIED9k4BTANBgkqhkiG9w0BAQsFADBwMRAwDgYDVQQGEwdV\n" +
            "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n" +
            "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRQwEgYDVQQDEwsxMC4xNDEu\n" +
            "Ny4zNTAeFw0xNDA4MjgwNzQ3MzNaFw0xNTAyMjQwNzQ3MzNaMHAxEDAOBgNVBAYT\n" +
            "B1Vua25vd24xEDAOBgNVBAgTB1Vua25vd24xEDAOBgNVBAcTB1Vua25vd24xEDAO\n"});

      bad_certs.add(new Object[]{"MIIDfzCCAmegAwIBAgIED9k4BTANBgkqhkiG9w0BAQsFADBwMRAwDgYDVQQGEwdV\n" +
            "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n" +
            "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRQwEgYDVQQDEwsxMC4xNDEu\n" +
            "Ny4zNTAeFw0xNDA4MjgwNzQ3MzNaFw0xNTAyMjQwNzQ3MzNaMHAxEDAOBgNVBAYT\n" +
            "B1Vua25vd24xEDAOBgNVBAgTB1Vua25v"});



      BAD_CERT_DATA = new Object[bad_certs.size()][];
      bad_certs.toArray(BAD_CERT_DATA);
   }

   @Test
   public void testSaveCertificate() {
      SoftwareManagerCollector.saveSslCertificate(GOOD_CERT, ".");
   }

   @DataProvider(name = "TestSoftwareManagerCollector.BAD_CERT_DATA")
   public final Object[][] getBadCertData() {
      return BAD_CERT_DATA;
   }

   @Test(expectedExceptions = SoftwareManagerCollectorException.class,
         expectedExceptionsMessageRegExp = "Bad certificate.", dataProvider = "TestSoftwareManagerCollector.BAD_CERT_DATA")
   public void testSaveCertificate1(String badCert) {
      SoftwareManagerCollector.saveSslCertificate(badCert, ".");
   }

   @Test(expectedExceptions = SWMgrCollectorInternalException.class,
         expectedExceptionsMessageRegExp = "Failed to save SSL certificate to key store.")
   public void testSaveCertificate3() {
      SoftwareManagerCollector.saveSslCertificate(GOOD_CERT, "/");
   }

   @Test(expectedExceptions = SWMgrCollectorInternalException.class,
         expectedExceptionsMessageRegExp = "Failed to read the private key file: key-file.")
   public void testLoadPrivateKey_Exceptional() {
      SoftwareManagerCollector.loadPrivateKey("key-file");
   }

   @Test
   public void testToAppManagerAdd() {
      AppManagerAdd appManagerAddDefault1 = new AppManagerAdd();
      appManagerAddDefault1.setName(Constants.IRONFAN);
      appManagerAddDefault1.setDescription(Constants.IRONFAN_DESCRIPTION);
      appManagerAddDefault1.setType(Constants.IRONFAN);
      appManagerAddDefault1.setUrl("ftp://address");
      appManagerAddDefault1.setUsername("");
      appManagerAddDefault1.setPassword("");
      appManagerAddDefault1.setSslCertificate("");

      AppManagerAdd appManagerAdd = SoftwareManagerCollector.toAppManagerAdd(new AppManagerEntity(appManagerAddDefault1));
      Assert.assertEquals(appManagerAddDefault1, appManagerAdd);
      Assert.assertEquals(appManagerAddDefault1.hashCode(), appManagerAdd.hashCode());
   }

}
