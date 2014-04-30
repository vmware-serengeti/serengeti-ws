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
package com.vmware.bdd.security.sso;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This class supports vc sso authentication through a sso token or saml2 assertion
 *
 */
public class VCSamlTokenAuthenticationFilter extends
      AbstractAuthenticationProcessingFilter {
   private static final String SPRING_SECURITY_FROM_SAML_TOKEN_KEY = "VCSSOToken";
   private String samlTokenParameter = SPRING_SECURITY_FROM_SAML_TOKEN_KEY;
   private boolean postOnly = true;

   protected VCSamlTokenAuthenticationFilter() {
      super("/sp/vcsso");
   }

   @Override
   public Authentication attemptAuthentication(HttpServletRequest request,
         HttpServletResponse response) throws AuthenticationException,
         IOException, ServletException {
      String errorMsg = "";
      if (postOnly && !request.getMethod().equals("POST")) {
         errorMsg = "Authentication method not supported: " + request.getMethod();
         logger.error(errorMsg);
         throw new AuthenticationServiceException(errorMsg);
      }
      Assertion samlToken = null;
      try {
         samlToken = obtainSamlToken(request);
      } catch (Exception e) {
         errorMsg = "Obtain SAML token failed: " + e.getMessage();
         logger.error(errorMsg);
         throw new AuthenticationServiceException(errorMsg);
      }
      SamlAuthenticationToken authRequest = new SamlAuthenticationToken(samlToken);
      // Allow subclasses to set the "details" property
      setDetails(request, authRequest);

      return this.getAuthenticationManager().authenticate(authRequest);
   }

   /*
    * Convert the SAML token string which obtain from the http request to Assertion object.   
    */
   protected Assertion obtainSamlToken(HttpServletRequest request)
         throws AuthenticationException, ParserConfigurationException,
         SAXException, IOException, ConfigurationException,
         UnmarshallingException, XMLParserException {
      String encodeSamlToken = request.getParameter(samlTokenParameter);
      if (StringUtils.isEmpty(encodeSamlToken)) {
         throw new PreAuthenticatedCredentialsNotFoundException(
               "SAML token cannot be empty!");
      }
      String samlToken = new String(org.opensaml.xml.util.Base64.decode(encodeSamlToken.trim()), "UTF-8");
      Reader reader = new CharArrayReader(samlToken.toCharArray());
      Document doc = new BasicParserPool().parse(reader);
      Element samlAssertionElement = doc.getDocumentElement();
      if (samlAssertionElement == null
            || !"Assertion".equals(samlAssertionElement.getLocalName())
            || !SAMLConstants.SAML20_NS.equals(samlAssertionElement
                  .getNamespaceURI())) {
         throw new AuthenticationServiceException(
               "Missing or invalid SAML Assertion");
      }

      // Unmarshall SAML Response into an OpenSAML Java object.
      DefaultBootstrap.bootstrap();
      UnmarshallerFactory unmarshallerFactory =
            org.opensaml.Configuration.getUnmarshallerFactory();
      Unmarshaller unmarshaller =
            unmarshallerFactory.getUnmarshaller(samlAssertionElement);
      Assertion samlAssertion =
            (Assertion) unmarshaller.unmarshall(samlAssertionElement);

      return samlAssertion;
   }

   public void setPostOnly(boolean postOnly) {
      this.postOnly = postOnly;
   }

   protected void setDetails(HttpServletRequest request,
         SamlAuthenticationToken authRequest) {
      authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
   }
}
