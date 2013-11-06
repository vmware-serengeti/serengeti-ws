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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.vmware.aurora.exception.CommonException;
import com.vmware.bdd.exception.EncryptionException;
import com.vmware.bdd.utils.AuAssert;

public class EncryptionGuard {

   private static final String UTF8_ENCODING = "UTF8";

   // explicitly declare algorithm, block mode, padding mode.
   // once need to change them, change the internal methods if necessary.
   private static final String ALGORITHM = "AES";
   private static final String BLOCK_MODE = "CBC";
   private static final String PADDING = "PKCS5Padding";
   private static final String TRANSFORMATION = ALGORITHM + "/" + BLOCK_MODE
         + "/" + PADDING;

   // initialization vector required for CBC
   private static final byte[] IV_PARAMETER = { (byte) 0x51, (byte) 0x2c,
         (byte) 0x3a, (byte) 0xb4, (byte) 0x87, (byte) 0xa0, (byte) 0xa1,
         (byte) 0x79, (byte) 0x56, (byte) 0x73, (byte) 0x56, (byte) 0x7d,
         (byte) 0xc2, (byte) 0x1f, (byte) 0xeb, (byte) 0x73 };

   // fix salt size
   private static final int SALT_SIZE = 16;

   /**
    * Encrypt the clear text against given secret key.
    * 
    * @param clearText
    *           the clear string
    * @return the encrypted string, or null if the clear string is null
    * @throws CommonException
    *            if input arguments is null
    */
   public static String encode(String clearText)
         throws GeneralSecurityException, UnsupportedEncodingException {
      if (clearText == null) {
         return null;
      }

      Key key = GuardKeyStore.getEncryptionKey();
      String salt = SaltGenerator.genRandomString(SALT_SIZE);

      String inputText = salt + clearText; // add salt
      byte[] clearBytes = inputText.getBytes(UTF8_ENCODING);

      Cipher cipher = getCiperInternal(Cipher.ENCRYPT_MODE, key);
      byte[] encryptedBytes = cipher.doFinal(clearBytes);

      Base64 base64 = new Base64(0); // 0 - no chunking
      return salt + base64.encodeToString(encryptedBytes);
   }

   /**
    * Decrypt the encrypted text against given secret key.
    * 
    * @param encodedText
    *           the encrypted string
    * @return the clear string, or null if encrypted string is null
    * @throws CommonException
    *            if input arguments is null
    */
   public static String decode(String encodedText)
         throws GeneralSecurityException, UnsupportedEncodingException {
      if (encodedText == null) {
         return null;
      }

      if (encodedText.length() < SALT_SIZE) {
         throw EncryptionException.SHORT_ENCRYPTED_STRING(encodedText);
      }

      Key key = GuardKeyStore.getEncryptionKey();
      String salt = encodedText.substring(0, SALT_SIZE);
      String encryptedText = encodedText.substring(SALT_SIZE);

      Base64 base64 = new Base64(0); // 0 - no chunking
      byte[] encryptedBytes = base64.decode(encryptedText);

      Cipher cipher = getCiperInternal(Cipher.DECRYPT_MODE, key);
      byte[] outputBytes = cipher.doFinal(encryptedBytes);

      String outputText = new String(outputBytes, UTF8_ENCODING);
      AuAssert.check(salt.equals(outputText.substring(0, SALT_SIZE))); // Assert salt

      return outputText.substring(SALT_SIZE);
   }

   /**
    * Get a cipher instance when need it, not share between multiple threads
    * because cipher is not thread safe.
    * 
    * @param opmode
    *           the operation mode
    * @param key
    *           the key
    * @return initialized cipher instance
    */
   private static Cipher getCiperInternal(int opmode, Key key)
         throws GeneralSecurityException {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      IvParameterSpec ips = new IvParameterSpec(IV_PARAMETER, 0, 16);
      cipher.init(opmode, key, ips);
      return cipher;
   }
}
