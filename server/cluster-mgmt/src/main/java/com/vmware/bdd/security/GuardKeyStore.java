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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import org.apache.log4j.Logger;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.security.CmsKeyStore;
import com.vmware.aurora.util.AuAssert;
import com.vmware.bdd.exception.EncryptionException;

public class GuardKeyStore {
   private static Logger logger = Logger.getLogger(GuardKeyStore.class);
   private static final String GUARD_KEYSTORE_ALIAS = "cms.guard_keystore.alias";
   private static final String GUARD_KEYSTORE = "cms.guard_keystore";

   private static final String KEYSTORE_TYPE = "JCEKS";

   private static KeyStore keyStore = null;
   private static String keyStorePath;
   private static boolean init = false;

   public static final String DEFAULT_GUARD_KEYSTORE_PATH = "/opt/serengeti/.certs/guard.key";

   static {
      keyStorePath = Configuration.getString(GUARD_KEYSTORE, DEFAULT_GUARD_KEYSTORE_PATH);

      try {
         keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
         File path = new File(keyStorePath);
         if (path.exists()) {
            String storepass = Configuration.getString(CmsKeyStore.CMS_KEYSTORE_PSWD);
            if (storepass != null) {  // not initialize if store pass is null
               InputStream in = null;
               try {
                  in = new FileInputStream(path);
                  keyStore.load(in, storepass.toCharArray());
               } finally {
                  if (in != null) {
                     in.close();
                  }
               }
            }
            init = true;
         } else {
            logger.error("guard keystore doesn't exist");
         }
      } catch (Exception ex) {
         logger.error("cannot load guard keystore", ex);
      }
   }

   public static Key getKey(String keyAlias, char[] keyPassword) 
   throws GeneralSecurityException, EncryptionException {
      if (init) {
         AuAssert.check(keyStore != null);
         return keyStore.getKey(keyAlias, keyPassword);
      } else {
         throw EncryptionException.KEYSTORE_NOT_INITIALIZED(keyStorePath);
      }
   }

   public static Key getEncryptionKey() 
   throws GeneralSecurityException, EncryptionException {
      if (!init) {
         throw EncryptionException.KEYSTORE_NOT_INITIALIZED(keyStorePath);
      }
      String alias = Configuration.getString(GUARD_KEYSTORE_ALIAS);
      String keypass = Configuration.getString(CmsKeyStore.CMS_KEYSTORE_PSWD);  // same as store pass
      return (keypass == null) ? null : getKey(alias, keypass.toCharArray());  // return null if key pass is null
   }
}

