/******************************************************************************
 *   Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *****************************************************************************/
package com.vmware.bdd.usermgmt;

import java.io.File;

import org.springframework.stereotype.Component;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.security.CmsKeyStore;
import com.vmware.bdd.apitypes.Password;
import com.vmware.bdd.security.tls.TrustStoreConfig;

/**
 * Created By xiaoliangl on 12/18/14.
 */
@Component
public class LdapsTrustStoreConfig implements TrustStoreConfig {
   private String path = System.getProperties().getProperty("serengeti.home.dir") + File.separator + ".certs"+ File.separator + "serengeti-trust.jks";

   @Override
   public String getType() {
      return "JKS";
   }

   @Override
   public String getPath() {
      return path;
   }

   @Override
   public Password getPassword() {
      return new LdapsTrustStorePassword();
   }

}

/**
 * Created By xiaoliangl on 12/11/14.
 */
class LdapsTrustStorePassword implements Password{
   String pwd = Configuration.getString(CmsKeyStore.CMS_KEYSTORE_PSWD);
   char[] pwdChars = pwd.toCharArray();

   @Override
   public String getPlainString() {
      return pwd;
   }

   @Override
   public char[] getPlainChars() {
      return pwdChars;
   }
}
