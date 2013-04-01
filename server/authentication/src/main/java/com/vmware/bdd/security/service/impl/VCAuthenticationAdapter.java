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
package com.vmware.bdd.security.service.impl;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.security.service.IAuthenticationAdapter;
import com.vmware.bdd.vc.AuthenticateVcUser;

public class VCAuthenticationAdapter implements IAuthenticationAdapter{

   private AuthenticateVcUser authenticateVcUser;
   private String userName;
   private String passwd;

   public VCAuthenticationAdapter(String userName, String passwd) {
      this(userName, passwd, null);
   }

   public VCAuthenticationAdapter(String userName, String passwd,
         AuthenticateVcUser authenticateVcUser) {
      this.userName = userName;
      this.passwd = passwd;
      this.authenticateVcUser = authenticateVcUser;
   }

   private void vcVerify(String userName, String passwd) throws Exception {
      if (authenticateVcUser == null) {
         String vcHost = Configuration.getString("vim.host");
         int vcPort = Configuration.getInt("vim.port", 443);
         String vcThumbprint = Configuration.getString("vim.thumbprint", null);
         authenticateVcUser = new AuthenticateVcUser(vcHost, vcPort, vcThumbprint);         
      }
      authenticateVcUser.authenticateUser(userName, passwd);
   }

   @Override
   public void verify() throws Exception {
      vcVerify(userName, passwd);
   }

}
