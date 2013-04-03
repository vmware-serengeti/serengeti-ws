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
package com.vmware.bdd.security.sso.service.impl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.cert.CertificateException;
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
import org.springframework.security.core.AuthenticationException;

import com.vmware.bdd.security.sso.service.ISAMLAuthenticationAdapter;
import com.vmware.bdd.security.sso.utils.SecurityUtils;
import com.vmware.bdd.utils.Configuration;
import com.vmware.vim.sso.client.DefaultTokenFactory;
import com.vmware.vim.sso.client.SamlToken;
import com.vmware.vim.sso.client.SecurityTokenService;

public class SSOAuthenticationBySAMLAdapter implements ISAMLAuthenticationAdapter {
   private static final Logger logger = Logger
         .getLogger(SSOAuthenticationBySAMLAdapter.class);

   private static final String STS_PROP_KEY = "sts";
   private Response response;
   private Assertion assertion;

   public SSOAuthenticationBySAMLAdapter(Response response, Assertion assertion) {
      this.response = response;
      this.assertion = assertion;
   }

   @Override
   public void verify() throws AuthenticationException {
      samlVerify(response, assertion);
   }

   private void samlVerify(Response response, Assertion assertion) {
      Conditions conditions = assertion.getConditions();
      //    saml assertion verification by SSO
      validateTokenFromSSO(response, assertion);
      //      token valid time period
      validateTimePeriod(conditions);
      //      token audience
      validateAudienceURI(conditions);
      //    HOK signature check
      validateSignature(response, assertion);
   }   

   private void validateTokenFromSSO(Response response, Assertion assertion) {
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

   private X509Certificate[] getCertsFromAssertion(Assertion assertion)
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

   private String getSamlLoginURI() throws BadCredentialsException{
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

   private void validateTimePeriod(Conditions conditions) {
      long beforeTime = conditions.getNotBefore().getMillis();
      long afterTime = conditions.getNotOnOrAfter().getMillis();
      long currentTime = System.currentTimeMillis();
      if (currentTime < beforeTime || currentTime > afterTime) {
         String errorMsg = "SAML token has an invalid time period.";
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }
   }

   private void validateAudienceURI(Conditions conditions) {
      String loginUrl = getSamlLoginURI();
      List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
      Audience audience = audienceRestrictions.get(0).getAudiences().get(0);
      if (!loginUrl.equals(audience.getAudienceURI())) {
         String errorMsg= "SAML token has an invalid audience URI.";
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }
   }

   private void validateSignature(Response response, Assertion assertion) {
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
