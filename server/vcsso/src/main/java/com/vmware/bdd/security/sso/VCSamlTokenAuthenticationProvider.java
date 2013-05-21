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
