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
package com.vmware.aurora.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.log4j.Logger;

public class JksKeyStoreUtil {
   private static final String KEYSTORE_TYPE = "jks";
   private static Logger logger = Logger.getLogger(JksKeyStoreUtil.class);

   public static KeyStore loadKeyStore(String storeFilePath, String storePasswd)
         throws NoSuchAlgorithmException,
                CertificateException,
                IOException,
                KeyStoreException {
      InputStream in = null;
      KeyStore store = null;
      try {
         store = KeyStore.getInstance(KEYSTORE_TYPE);
         File ks = new File(storeFilePath);
         if (ks.exists()) {
            in = new FileInputStream(ks);
         } else {
            logger.error("Keystore " + storeFilePath + " doesn't exist");
         }
         store.load(in, storePasswd.toCharArray());
      } finally {
         if (in != null) {
            in.close();
         }
      }
      return store;
   }

   public static void serializeKeyStore(String storeFilePath, KeyStore store, String storePasswd)
         throws NoSuchAlgorithmException,
                CertificateException,
                IOException,
                KeyStoreException {
      OutputStream outStream = null;
      try {
         outStream = new FileOutputStream(new File(storeFilePath));
         store.store(outStream, storePasswd.toCharArray());
      } finally {
         if (outStream != null) {
            outStream.close();
         }
      }
   }
}
