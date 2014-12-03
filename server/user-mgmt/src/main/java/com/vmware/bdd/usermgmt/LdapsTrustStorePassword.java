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

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.security.CmsKeyStore;
import com.vmware.bdd.apitypes.Password;

/**
 * Created By xiaoliangl on 12/11/14.
 */
public class LdapsTrustStorePassword implements Password{
   @Override
   public String getPlainString() {
      return Configuration.getString(CmsKeyStore.CMS_KEYSTORE_PSWD);
   }

   @Override
   public char[] getPlainChars() {
      return getPlainString().toCharArray();
   }
}
