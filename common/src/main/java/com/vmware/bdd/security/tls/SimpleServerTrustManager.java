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
package com.vmware.bdd.security.tls;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.Password;
import com.vmware.bdd.utils.ByteArrayUtils;

/**
 * Created By xiaoliangl on 12/11/14.
 */
public class SimpleServerTrustManager implements X509TrustManager {
   private final static Logger LOGGER = Logger.getLogger(SimpleServerTrustManager.class);

   private String trustStorePath;
   private Password password;
   private String trustStoreType;

   public TrustCertCallBack getTrustCertCallBack() {
      return trustCertCallBack;
   }

   public void setTrustCertCallBack(TrustCertCallBack trustCertCallBack) {
      this.trustCertCallBack = trustCertCallBack;
   }

   private TrustCertCallBack trustCertCallBack;


   public String getTrustStoreType() {
      return trustStoreType;
   }

   public void setTrustStoreType(String trustStoreType) {
      this.trustStoreType = trustStoreType;
   }

   public String getTrustStorePath() {
      return trustStorePath;
   }

   public void setTrustStorePath(String trustStorePath) {
      this.trustStorePath = trustStorePath;
   }

   public Password getPassword() {
      return password;
   }

   public void setPassword(Password password) {
      this.password = password;
   }

   @Override
   public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
      throw new RuntimeException("not implemented");
   }

   @Override
   public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
      boolean trusted = true;

      if (x509Certificates == null || x509Certificates.length == 0) {
         return;
      }

      X509Certificate leafCert = x509Certificates[0];
      String sha1Fingerprint = null;
            KeyStore keyStore = null;
      InputStream in = null;
      try {
         in = new FileInputStream(trustStorePath);
         keyStore = KeyStore.getInstance(trustStoreType);
         keyStore.load(in, password.getPlainChars());
      } catch (IOException e) {
         throw new KeystoreException(e);
      } catch (NoSuchAlgorithmException e) {
         throw new KeystoreException(e);
      } catch (KeyStoreException e) {
         throw new KeystoreException(e);
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException e) {
               //do nothing
            }
         }
      }

      try {
         MessageDigest sha1 = MessageDigest.getInstance("SHA1");

         sha1.update(leafCert.getEncoded());
         sha1Fingerprint = ByteArrayUtils.byteArrayToHexString(sha1.digest());

         if (!keyStore.isCertificateEntry(sha1Fingerprint)) {
            trusted = false;
         }
      } catch (NoSuchAlgorithmException e) {
         throw new TlsInitException(e);
      } catch (KeyStoreException e) {
         throw new TlsInitException(e);
      }

      if (!trusted) {
         if (trustCertCallBack.doTrustOnFirstUse(leafCert)) {
            LOGGER.warn("user confirms to trust the certificate.");

            storeTrustedCert(keyStore, leafCert, sha1Fingerprint);

         } else {
            throw new UntrustedCertificateException(TlsHelper.getCertificateInfo(leafCert));
         }
      }
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
   }

   /**
    * Adds a given certificate to the keystore.
    * @param cert The server certificate
    * @param sha1Fingerprint
    */
   private void storeTrustedCert(KeyStore keyStore, Certificate cert, String sha1Fingerprint) {
      /**
       * Write back to file
       */
      FileOutputStream keyStoreFile = null;
      try {
         keyStore.setCertificateEntry(sha1Fingerprint, cert);
         keyStoreFile = new FileOutputStream(trustStorePath);
         keyStore.store(keyStoreFile, password.getPlainChars());
      } catch (FileNotFoundException e) {
      } catch (CertificateException e) {
      } catch (NoSuchAlgorithmException e) {
      } catch (KeyStoreException e) {
      } catch (IOException e) {
      } finally {
         if(keyStoreFile != null) {
            try {
               keyStoreFile.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
