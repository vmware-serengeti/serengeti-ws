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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestThumbprintTrustManager {

   private final String thumbprint = "ABCDEFG123456789";
   private ThumbprintTrustManager tt;

   @BeforeMethod
   public void setup() throws Exception {
      tt = new ThumbprintTrustManager();
   }

   @Test(groups = { "TestThumbprintTrustManager" })
   public void testAdd() throws Exception {
      assertFalse(tt.hasThumbprint(thumbprint));
      tt.add(thumbprint);
      assertTrue(tt.hasThumbprint(thumbprint));
   }

   @Test(groups = { "TestThumbprintTrustManager" })
   public void testRemove() throws Exception {
      tt.add(thumbprint);
      assertTrue(tt.hasThumbprint(thumbprint));
      tt.remove(thumbprint);
      assertFalse(tt.hasThumbprint(thumbprint));
   }

   @Test(groups = { "TestThumbprintTrustManager" })
   public void testToHex() {
      byte[] bytes = thumbprint.getBytes();
      StringBuffer numbers = new StringBuffer();
      for (byte b : bytes) {
         int num = b & 0xff;
         numbers.append(Integer.toHexString(num));
      }
      assertEquals(ThumbprintTrustManager.toHex(bytes), numbers.toString());
   }
}
