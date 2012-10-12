/******************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
 ******************************************************************************/
package com.vmware.bdd.cli.command.tests;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import com.vmware.bdd.cli.commands.CookieCache;

public class CookieCacheTest {

   @Test
   public void testPut() {
      String cookieName="Cookie";
      String cookieValue="JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54F";
      CookieCache.put(cookieName, cookieValue);
      assertNotNull(CookieCache.get(cookieName));
      assertEquals(CookieCache.get(cookieName),cookieValue);
      CookieCache.clear();
   }

   @Test
   public void testGet() {
      String cookieName="Cookie";
      String cookieValue="JSESSIONID=2AAF431F59ACEE1CC68B43C87772C54EE";
      CookieCache.put(cookieName, cookieValue);
      assertNotNull(CookieCache.get(cookieName));
      assertEquals(CookieCache.get(cookieName),cookieValue);
      CookieCache.clear();
   }
}
