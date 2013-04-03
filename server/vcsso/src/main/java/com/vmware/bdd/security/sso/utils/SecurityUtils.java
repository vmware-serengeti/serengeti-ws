/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.security.sso.utils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.opensaml.xml.signature.X509Data;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

import com.vmware.bdd.utils.Configuration;
import com.vmware.vim.sso.client.DefaultSecurityTokenServiceFactory;
import com.vmware.vim.sso.client.SecurityTokenService;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig.ConnectionConfig;

public class SecurityUtils {
   private static final Logger logger = Logger.getLogger(SecurityUtils.class);
   private static final String SSO_CRTS_DIR = "sts_crts_dir";
   private static final String X509 = "X.509";

   public static SecurityTokenService getSTSClient(String stsLocation) {
      //get sts certs file dir
      String stsFileDir = Configuration.getString(SSO_CRTS_DIR);
      String stsFileName = stsFileDir + "/sts.crt";
      FileInputStream bis = null;
      try {
         URL stsURL = new URL(stsLocation); // non-HTTPS connections are not recommended!

         // SSL trust requires one or both the following two parameters to be specified:
         bis = new FileInputStream(stsFileName);
         CertificateFactory cf = CertificateFactory.getInstance(X509);

         List<X509Certificate> stsCerts = new ArrayList<X509Certificate>();
         while (bis.available() > 0) {
            stsCerts.add((X509Certificate)cf.generateCertificate(bis));
         }
         X509Certificate[] certs = stsCerts.toArray(new X509Certificate[stsCerts.size()]);

         ConnectionConfig connConfig = new ConnectionConfig(stsURL, certs, null);

         // SSO-Client should also know STS signing certificates in order to validate SAML assertions
         //X509Certificate[] stsSigningCertificates = ... // usually obtained via SSO admin API, configuration manager getTrustedRootCertificates

         // Create STS client configuration object
         SecurityTokenServiceConfig config = new SecurityTokenServiceConfig(connConfig, connConfig.getTrustedRootCertificates(), null);

         // Create STS client
         SecurityTokenService stsClient = DefaultSecurityTokenServiceFactory.getSecurityTokenService(config);
         return stsClient;
      } catch (MalformedURLException badURLException) {
         logger.error("Authentication error :" + badURLException.getMessage());
         throw new AuthenticationServiceException(badURLException.getMessage());
      } catch (FileNotFoundException stsFileException) {
         logger.error("Authentication error :" + stsFileException.getMessage());
         throw new AuthenticationServiceException(stsFileException.getMessage());
      } catch (CertificateException stsCertException) {
         logger.error("Authentication error :" + stsCertException.getMessage());
         throw new AuthenticationServiceException(stsCertException.getMessage());
      } catch (Exception e) {
         logger.error("Authentication error :" + e.getMessage());
         throw new BadCredentialsException(e.getMessage());
      } finally {
         if (bis != null) {
            try {
               bis.close();
            } catch (Exception e) {
               logger.error(e.getMessage() + "\n Can not close " + stsFileName + ".");
            }
         }
      }
   }

   public static List<X509Certificate> getCertsFromx509Data(List<X509Data> x509Data) throws CertificateException {
      List<X509Certificate> certList = new ArrayList<X509Certificate>();
      if (x509Data != null && !x509Data.isEmpty()) {
         List<org.opensaml.xml.signature.X509Certificate> certs =
               x509Data.get(0).getX509Certificates();
         if (certs != null) {
            for (org.opensaml.xml.signature.X509Certificate cert : certs) {
               // Instantiate a java.security.cert.X509Certificate object out of the
               // base64 decoded byte[] of the certificate
               X509Certificate x509Cert = null;

               CertificateFactory cf =
                     CertificateFactory.getInstance("X.509");
               x509Cert =
                     (X509Certificate) cf
                           .generateCertificate(new ByteArrayInputStream(
                                 org.opensaml.xml.util.Base64.decode(cert
                                       .getValue())));
               if (x509Cert != null) {
                  certList.add(x509Cert);
               }
            }
         }
      }
      return certList;
   }
}
