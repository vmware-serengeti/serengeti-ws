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

import com.vmware.bdd.security.service.IAuthenticationAdapter;
import com.vmware.bdd.security.sso.utils.SecurityUtils;
import com.vmware.vim.sso.client.SecurityTokenService;
import com.vmware.vim.sso.client.TokenSpec;

public class SSOAuthenticationAdapter implements IAuthenticationAdapter{

   private static final int TOKEN_LIFE_TIME = 60;

   private String stsLocation;
   private String userName;
   private String passwd;

   public SSOAuthenticationAdapter(String userName, String passwd,
         String stsLocation) {
      this.userName = userName;
      this.passwd = passwd;
      this.stsLocation = stsLocation;
   }

   @Override
   public void verify() throws Exception {
      ssoVerify();
   }

   private void ssoVerify() throws Exception{
      SecurityTokenService stsClient = SecurityUtils.getSTSClient(stsLocation);
      // Describe the requested token properties using a TokenSpec
      TokenSpec tokenSpec = new TokenSpec.Builder(TOKEN_LIFE_TIME).createTokenSpec();
      // Acquire the requested token
      stsClient.acquireToken(userName, passwd, tokenSpec);
   }

}
