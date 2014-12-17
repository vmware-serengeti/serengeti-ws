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
package com.vmware.bdd.security.tls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vmware.bdd.apitypes.Password;

/**
 * Created By xiaoliangl on 11/28/14.
 */
public class TestTlsClient {
   private final static String KEY_STORE_PATH = System.getProperty("java.io.tmpdir") + File.separator + "keystore.jks";

   @BeforeMethod
   public void beforeMethod() throws IOException {
      InputStream is = TestTlsClient.class.getResourceAsStream("/com/vmware/bdd/security/tls/keystore.jks");

      FileOutputStream fos = new FileOutputStream(KEY_STORE_PATH);

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


      trustManager = new SimpleServerTrustManager();
      trustManager.setTrustStorePath(KEY_STORE_PATH);
      trustManager.setPassword(new Password() {
         @Override
         public String getPlainString() {
            return "changeit";
         }

         @Override
         public char[] getPlainChars() {
            return getPlainString().toCharArray();
         }
      });
      trustManager.setTrustStoreType("JKS");

      helper.setTrustManager(trustManager);
   }

   private SimpleServerTrustManager trustManager;
   private TlsTcpClient helper = new TlsTcpClient();

   @AfterMethod
   public void afterMethod() {
      new File(KEY_STORE_PATH).delete();
   }

   @Test(expectedExceptions = {UntrustedCertificateException.class})
   public void testConnectFirstly() throws IOException {


      try {
         helper.checkCertificateFirstly("wiki.eng.vmware.com", 443, false);
      } catch (UntrustedCertificateException uce) {
         CertificateInfo certInfo = uce.getCertInfo();
         Assert.assertNotNull(certInfo);
         Assert.assertEquals(certInfo.getSubjectDn(), "CN=*.eng.vmware.com, OU=IT, O=\"VMware, Inc.\", L=Palo Alto, ST=California, C=US");
         Assert.assertEquals(certInfo.getIssuerDn(), "CN=DigiCert High Assurance CA-3, OU=www.digicert.com, O=DigiCert Inc, C=US");
         Assert.assertEquals(certInfo.getSerialNumber(), "0C8FBE4535E383074BE071A8AF58245E");
         Assert.assertEquals(certInfo.getSha1Fingerprint(), "5E835E96FB3C677927B0D3EBE6E0463B6778770B");
         Assert.assertNotNull(certInfo.getNotAfter());
         Assert.assertNotNull(certInfo.getNotBefore());

         throw uce;
      }

//      helper.checkCertificateFirstly("10.112.113.182", 8443);
//      helper.checkCertificateFirstly("10.112.113.137", 636);
   }

   @Test
   public void testConnectAgainAndAgain() throws IOException {

      helper.checkCertificateFirstly("wiki.eng.vmware.com", 443, true);

      helper.checkCertificateFirstly("wiki.eng.vmware.com", 443, true);
   }

   @Test
   public void testConnectAgain() throws IOException {

      helper.checkCertificateFirstly("wiki.eng.vmware.com", 443, true);
   }

   @Test
   public void testConnectAgain1() throws IOException {

      helper.checkCertificateFirstly("pek2-aurora-dev-01-113-dhcp137.eng.vmware.com", 636, true);
   }

   @Test(expectedExceptions = {TruststoreException.class})
   public void testCheckCertFirstly_wrongPassphrase()  {
      trustManager.setPassword(new Password() {
         @Override
         public String getPlainString() {
            return "badpassword";
         }

         @Override
         public char[] getPlainChars() {
            return getPlainString().toCharArray();
         }
      });
      helper.checkCertificateFirstly("wiki.eng.vmware.com", 443, false);
   }

   @Test(expectedExceptions = {TruststoreException.class})
   public void test_keystoreNotFound()  {
      trustManager.setTrustStorePath("filenotfound");
      helper.checkCertificateFirstly("wiki.eng.vmware.com", 443, false);
   }

   @Test(expectedExceptions = {TlsConnectionException.class})
   public void testCheckCertFirstly_plain()  {

      helper.checkCertificateFirstly("www.vmware.com", 80, false);
   }

   @Test(expectedExceptions = {TlsConnectionException.class})
   public void testCheckCertFirstly_unknownServer()  {

      helper.checkCertificateFirstly("www.unknown.com", 80, false);
   }
}
