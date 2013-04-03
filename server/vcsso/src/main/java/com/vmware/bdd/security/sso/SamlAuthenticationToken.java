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

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class SamlAuthenticationToken extends AbstractAuthenticationToken {
   /**
    * 
    */
   private static final long serialVersionUID = -4518528127563230732L;

   private Object credentials;

   public SamlAuthenticationToken(Object credentials) {
      super(null);
      this.credentials = credentials;
      setAuthenticated(false);
   }

   public SamlAuthenticationToken(Object credentials,
         Collection<? extends GrantedAuthority> authorities) {
      super(authorities);
      this.credentials = credentials;
      super.setAuthenticated(true);
   }

   @Override
   public Object getCredentials() {
      return credentials;
   }

   @Override
   public Object getPrincipal() {
      return "";
   }

   public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
      if (isAuthenticated) {
          throw new IllegalArgumentException(
              "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
      }

      super.setAuthenticated(false);
  }

}
