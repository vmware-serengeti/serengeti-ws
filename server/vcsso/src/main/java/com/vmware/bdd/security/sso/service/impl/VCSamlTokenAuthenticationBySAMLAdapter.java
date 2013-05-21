package com.vmware.bdd.security.sso.service.impl;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Conditions;

import org.springframework.security.core.AuthenticationException;

import com.vmware.bdd.security.sso.service.ISAMLAuthenticationAdapter;
import com.vmware.bdd.security.sso.utils.SecurityUtils;

public class VCSamlTokenAuthenticationBySAMLAdapter implements
      ISAMLAuthenticationAdapter {
   private Assertion assertion;

   public VCSamlTokenAuthenticationBySAMLAdapter(Assertion assertion) {
      this.assertion = assertion;
   }

   @Override
   public void verify() throws AuthenticationException {
      samlVerify(assertion);
   }

   private void samlVerify(Assertion assertion) {
      Conditions conditions = assertion.getConditions();
      //    saml assertion verification by SSO
      SecurityUtils.validateTokenFromSSO(assertion);
      //      token valid time period
      SecurityUtils.validateTimePeriod(conditions);
   }   
}
