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

import org.opensaml.saml2.core.Assertion;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import com.vmware.bdd.security.service.impl.UserService;
import com.vmware.bdd.security.sso.service.ISAMLAuthenticationService;
import com.vmware.bdd.security.sso.service.impl.SAMLAuthenticationService;
import com.vmware.bdd.security.sso.service.impl.VCSamlTokenAuthenticationBySAMLAdapter;

public class VCSamlTokenAuthenticationProvider implements
      AuthenticationProvider {

   private UserService userService;

   @Override
   public Authentication authenticate(Authentication authentication)
         throws AuthenticationException {
      Assertion assertion = (Assertion) authentication.getCredentials();

      ISAMLAuthenticationService samlAuthenticationService =
            new SAMLAuthenticationService(new VCSamlTokenAuthenticationBySAMLAdapter(assertion));
      samlAuthenticationService.validate();

      UserDetails user =
            userService.loadUserByUsername(assertion.getSubject().getNameID().getValue());
      SamlAuthenticationToken samlAuthenticationToken =
            new SamlAuthenticationToken(assertion, user.getAuthorities());
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
