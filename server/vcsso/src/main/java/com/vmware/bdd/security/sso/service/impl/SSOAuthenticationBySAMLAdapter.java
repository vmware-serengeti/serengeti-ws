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
import org.opensaml.saml2.core.Response;
import org.springframework.security.core.AuthenticationException;

import com.vmware.bdd.security.sso.service.ISAMLAuthenticationAdapter;
import com.vmware.bdd.security.sso.utils.SecurityUtils;

public class SSOAuthenticationBySAMLAdapter implements ISAMLAuthenticationAdapter {
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
      SecurityUtils.validateTokenFromSSO(assertion);
      //      token valid time period
      SecurityUtils.validateTimePeriod(conditions);
      //      token audience
      SecurityUtils.validateAudienceURI(conditions);
      //    HOK signature check
      SecurityUtils.validateSignature(response, assertion);
   }
}
