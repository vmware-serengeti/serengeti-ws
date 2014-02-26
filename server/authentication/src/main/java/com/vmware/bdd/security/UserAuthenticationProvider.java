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

import org.apache.log4j.Logger;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.vmware.bdd.security.exception.VCConnectException;
import com.vmware.bdd.security.service.IAuthenticationService;
import com.vmware.bdd.security.service.impl.UserAuthenticationService;
import com.vmware.bdd.security.service.impl.UserService;
import com.vmware.bdd.security.service.impl.VCAuthenticationAdapter;
import com.vmware.vim.vmomi.client.exception.ConnectionException;

public class UserAuthenticationProvider implements AuthenticationProvider {
   private static final Logger logger = Logger
         .getLogger(UserAuthenticationProvider.class);
   private UserService userService;

   @Override
   public Authentication authenticate(Authentication authentication)
         throws AuthenticationException {

      String userName = (String) authentication.getPrincipal();
      String passwd = (String) authentication.getCredentials();

      try {
         IAuthenticationService userAuthenticationService =
               new UserAuthenticationService(new VCAuthenticationAdapter(
                     userName, passwd));
         userAuthenticationService.validate();
         UserDetails user =
               userService.loadUserByUsername(authentication.getName());
         UserAuthenticationToken accountAuthenticationToken =
               new UserAuthenticationToken(user.getAuthorities());
         return accountAuthenticationToken;
      } catch (AuthenticationServiceException serviceException) {
         throw serviceException;
      } catch (UsernameNotFoundException userNotfoundException) {
         throw userNotfoundException;
      } catch (Exception e) {
         if (e instanceof ConnectionException) {
            String errorMsg = "vCenter connect failed: " + e.getMessage();
            logger.error(errorMsg);
            throw new VCConnectException(errorMsg, e);
         } else {
            logger.error("Authentication error: " + e.getMessage());
            throw new BadCredentialsException(e.getMessage());   
         }
      }

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
