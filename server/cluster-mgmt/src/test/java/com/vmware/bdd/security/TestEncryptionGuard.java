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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.vmware.aurora.global.Configuration;

public class TestEncryptionGuard {
   private static final String VC_PROPERTIES_NAME = "vc.properties";

   @BeforeClass
   public static void setUp() throws Exception {
      PropertiesConfiguration config = new PropertiesConfiguration(VC_PROPERTIES_NAME);
      String fileName = config.getPath();
      String path = fileName.substring(0, fileName.length() - VC_PROPERTIES_NAME.length());
      String keyFilePath = path + "guard.key";
      Configuration.setString("cms.guard_keystore", keyFilePath);
   }

   @Test
   public void testEncryptDecrypt() throws Exception {
      String clearText = "testABC";
      String encrypted = EncryptionGuard.encode(clearText);
      System.out.println("Encrypted string: " + encrypted);
      Assert.assertTrue(encrypted != null && encrypted.length() > 16, "Should get string longer than salt");
      String decrypted = EncryptionGuard.decode(encrypted);

      Assert.assertTrue(clearText.equals(decrypted), "Should get same decrypted string");
   }

   @Test
   public void testShortEecryptedValue() throws Exception {
      String clearText = "testABC";
      String encrypted = EncryptionGuard.encode(clearText);
      String invalid = encrypted.substring(0, 10);
      try {
         EncryptionGuard.decode(invalid);
         Assert.assertTrue(false, "Should get exception.");
      } catch (Exception e) {
         Assert.assertTrue(true, "Got expected exception.");
      }
   }
}
