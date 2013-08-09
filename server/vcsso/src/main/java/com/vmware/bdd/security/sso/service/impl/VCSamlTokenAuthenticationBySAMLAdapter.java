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
