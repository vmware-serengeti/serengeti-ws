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
package com.vmware.bdd.security;

import java.util.Arrays;

import mockit.Mock;
import mockit.MockUp;
import mockit.NonStrictExpectations;

import org.springframework.security.core.Authentication;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;
import com.vmware.bdd.entity.User;
import com.vmware.bdd.entity.Users;
import com.vmware.bdd.security.service.impl.UserService;
import com.vmware.bdd.utils.FileUtils;
import com.vmware.bdd.utils.TestFileUtils;
import com.vmware.bdd.vc.AuthenticateVcUser;

public class TestUserAuthenticationProvider {

   public static final String UsersFile = "Users.xml";

   @Test
   public void testAuthenticate() throws Exception {
      Authentication authentication = new MockUp<Authentication>() {
         @Mock
         Object getPrincipal() {
            return "serengeti";
         }

         @Mock
         Object getCredentials() {
            return "password";
         }

         @Mock
         public String getName() {
            return "serengeti";
         }
      }.getMockInstance();

      new NonStrictExpectations(){
         @SuppressWarnings("unused")
         Configuration configuration;
         {
            Configuration.getString("vim.host");
            returns("vc-aurora-db0.prom.eng.vmware.com");
         };
         {
            Configuration.getString("vim.thumbprint");
            returns("78:97:dc:69:96:83:77:a8:33:0c:5c:62:73:ae:50:5c:62:fb:b0:09");
         };
         {
            Configuration.getInt("vim.port", 443);
            returns(443);
         }
      };

      new MockUp<AuthenticateVcUser>() {
         @Mock
         public void authenticateUser(String name, String password) throws Exception {
            return;
         }
      };

      Users users = new Users();
      User user = new User();
      user.setName("serengeti");
      users.setUsers(Arrays.asList(user));
      TestFileUtils.createXMLFile(users, FileUtils.getConfigFile(UsersFile, "Users"));

      UserAuthenticationProvider provider = new UserAuthenticationProvider();
      provider.setUserService(new UserService());
      provider.authenticate(authentication);

      TestFileUtils.deleteXMLFile(FileUtils.getConfigFile(UsersFile, "Users"));
   }

}
