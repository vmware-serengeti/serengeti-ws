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

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import com.vmware.bdd.security.service.impl.UserService;
import com.vmware.bdd.security.sso.service.ISAMLAuthenticationService;
import com.vmware.bdd.security.sso.service.impl.SAMLAuthenticationService;
import com.vmware.bdd.security.sso.service.impl.SSOAuthenticationBySAMLAdapter;

public class SamlAuthenticationProvider implements AuthenticationProvider {
   private static final Logger logger = Logger
         .getLogger(SamlAuthenticationProvider.class);

   private UserService userService;

   @Override
   public Authentication authenticate(Authentication authentication)
         throws AuthenticationException {
      String errorMsg = "";
      Response response = (Response) authentication.getCredentials();
      Assertion assertion = response.getAssertions().get(0);
      if(assertion == null) {
         errorMsg = "SAML authenticate failed. Assertion cannot be null.";
         logger.error(errorMsg);
         throw new BadCredentialsException(errorMsg);
      }

      ISAMLAuthenticationService samlAuthenticationService =
            new SAMLAuthenticationService(new SSOAuthenticationBySAMLAdapter(
                  response, assertion));
      samlAuthenticationService.validate();

      UserDetails user =
            userService.loadUserByUsername(assertion.getSubject().getNameID().getValue());
      SamlAuthenticationToken samlAuthenticationToken =
            new SamlAuthenticationToken(response, user.getAuthorities());
      return samlAuthenticationToken;
   }

   @Override
   public boolean supports(Class<?> authentication) {
      return true;
   }

   public UserService getUserService() {
      return userService;
   }

   public void setUserService(UserService userService) {
      this.userService = userService;
   }

}