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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.KeyInfoConfirmationDataType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.signature.X509Data;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

import com.vmware.bdd.utils.Configuration;
import com.vmware.vim.sso.client.DefaultSecurityTokenServiceFactory;
import com.vmware.vim.sso.client.DefaultTokenFactory;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.SecurityTokenService;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig;
import com.vmware.vim.sso.client.SecurityTokenServiceConfig.ConnectionConfig;

public class SecurityUtils {
   private static final Logger logger = Logger.getLogger(SecurityUtils.class);
   private static final String SSO_CRTS_DIR = "sts_crts_dir";
   private static final String X509 = "X.509";
   private static final String STS_PROP_KEY = "sts";

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

   public static void validateTokenFromSSO(Assertion assertion) {
      try {
         X509Certificate[] certs = getCertsFromAssertion(assertion);
         if (certs != null) {
            String stsLocation = Configuration.getString(STS_PROP_KEY);
            if (stsLocation == null) {
               throw new AuthenticationServiceException("SSO is not enabled");
            }
            SecurityTokenService stsClient =
                  SecurityUtils.getSTSClient(stsLocation);
            SamlToken ssoSamlToken =
                  DefaultTokenFactory.createTokenFromDom(assertion.getDOM(),
                        certs);
            boolean validFromSSO = stsClient.validateToken(ssoSamlToken);
            if (!validFromSSO) {
               throw new BadCredentialsException("invalid saml token");
            }
         }
      } catch (AuthenticationServiceException serviceException) {
         throw serviceException;
      } catch (BadCredentialsException badCredentialException) {
         throw badCredentialException;
      } catch (Exception e) {
         logger.error("Cannot validate the token by sso: " + e.getMessage());
         throw new BadCredentialsException(e.getMessage());
      }
   }

   private static X509Certificate[] getCertsFromAssertion(Assertion assertion)
         throws CertificateException {
      List<X509Certificate> certList = new ArrayList<X509Certificate>();
      Signature ds = assertion.getSignature();
      if (ds != null) {
         List<X509Data> x509Data = ds.getKeyInfo().getX509Datas();
         certList = SecurityUtils.getCertsFromx509Data(x509Data);
         if (certList.size() > 0) {
            X509Certificate[] certs =
                  certList.toArray(new X509Certificate[certList.size()]);
            return certs;
         }
      }
      return null;
   }

   public static void validateTimePeriod(Conditions conditions) {
      long beforeTime = conditions.getNotBefore().getMillis();
      long afterTime = conditions.getNotOnOrAfter().getMillis();
      long currentTime = System.currentTimeMillis();
      if (currentTime < beforeTime || currentTime > afterTime) {
         String errorMsg = "SAML token has an invalid time period.";
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }
   }

   public static void validateAudienceURI(Conditions conditions) {
      String loginUrl = getSamlLoginURI();
      List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
      Audience audience = audienceRestrictions.get(0).getAudiences().get(0);
      if (!loginUrl.equals(audience.getAudienceURI())) {
         String errorMsg= "SAML token has an invalid audience URI.";
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }
   }

   private static String getSamlLoginURI() throws BadCredentialsException{
      String ipAddress = "";
      try {
         Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
         NetworkInterface networkInterface = null;
         while (networkInterfaces.hasMoreElements()) {
            networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses =
                  networkInterface.getInetAddresses();
            InetAddress inetAddress = null;
            while (inetAddresses.hasMoreElements()) {
               inetAddress = inetAddresses.nextElement();
               if (!inetAddress.isLinkLocalAddress()
                     && !inetAddress.isLoopbackAddress()) {
                  ipAddress = inetAddress.getHostAddress();
               }
            }
         }
      } catch (SocketException e) {
         logger.error("Cannot obtain a network interface: " + e.getMessage());
      }
      if(ipAddress.isEmpty()) {
         String errorMsg = "Unknown host: Cannot obtain a valid ip address .";
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }
      return "http://" + ipAddress + ":8080/serengeti/sp/sso";
   }

   public static void validateSignature(Response response, Assertion assertion) {
      Signature responseSignature = response.getSignature();
      Subject subject = assertion.getSubject();
      SubjectConfirmationData subjectConfirmationData =
            subject.getSubjectConfirmations().get(0)
                  .getSubjectConfirmationData();
      KeyInfoConfirmationDataType keyInfoConfirmationData =
            (KeyInfoConfirmationDataType) subjectConfirmationData;
      //Get the <ds:X509Data/> elements
      KeyInfo keyInfo = (KeyInfo) keyInfoConfirmationData.getKeyInfos().get(0);
      List<X509Data> x509Data = keyInfo.getX509Datas();
      List<X509Certificate> certList = new ArrayList<X509Certificate>();
      try {
         certList = SecurityUtils.getCertsFromx509Data(x509Data);

         BasicX509Credential publicCredential = new BasicX509Credential();
         publicCredential.setEntityCertificate(certList.get(0));

         SignatureValidator validator =
               new SignatureValidator(publicCredential);
         validator.validate(responseSignature);
      } catch (Exception e) {
         String errorMsg =
               "SAML token cannot validate the response signatrue: "
                     + e.getMessage();
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }
   }
}
