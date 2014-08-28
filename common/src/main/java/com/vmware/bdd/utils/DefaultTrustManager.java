/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.utils;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;

public class DefaultTrustManager implements X509TrustManager {

   private static final Logger logger = Logger
         .getLogger(DefaultTrustManager.class);

   private KeyStore keyStore;

   public DefaultTrustManager() {
   }

   @Override
   public void checkClientTrusted(X509Certificate[] chain, String authType)
         throws CertificateException {
   }

   @Override
   public void checkServerTrusted(X509Certificate[] chain, String authType)
         throws CertificateException {
      logger.info("Starts to check server certificate.");

      try {
         KeyStore keyStore = CommonUtil.loadAppMgrKeyStore(Constants.APPMANAGER_KEYSTORE_PATH);
         if (keyStore == null) {
            logger.error("Cannot read appmanager keystore.");
            return;
         }
         MessageDigest md5 = MessageDigest.getInstance("MD5");
         String md5Fingerprint = "";
         for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            md5.update(cert.getEncoded());
            md5Fingerprint = CommonUtil.toHexString(md5.digest());
            logger.debug("Certificate No. " + i + ": " + cert);
            if (keyStore.getCertificate(md5Fingerprint) != null) {
               if (i == chain.length - 1) {
                  return;
               } else {
                  continue;
               }
            }
            logger.error("md5 finger print: " + md5Fingerprint);
            logger.error("Unknown certificate: " + cert);
            throw SoftwareManagementPluginException.UNKNOWN_CERTIFICATE(
                  cert.getSubjectDN().toString());
         }
      } catch (NoSuchAlgorithmException e) {
         logger.error("SSL Algorithm error: " + e.getMessage(), e);
      } catch (KeyStoreException e) {
         logger.error("Key store error: " + e.getMessage(), e);
      }
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return null;
   }

}
