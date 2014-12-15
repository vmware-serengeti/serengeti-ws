/******************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.validation;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created By xiaoliangl on 11/25/14.
 */
public class TestDnUrlValidator {
   private final static Object[][] DATA = new Object[][]{
         {true, "ou=users,dc=bde,dc=vmware,dc=com"},
         {true, "dc=bde,dc=vmware,dc=com"},
         {false, "dc=bde,,dc=vmware,dc=com"},
         {false, "dc=b=de,dc=vmware,dc=com"},
         {false, "this is a test"},
         {true, "cn=Users,dc=bde,dc=vmware,dc=com"},
         {true, "   "},
         {true, null},
   };

   @DataProvider(name = "TestDnUrlValidator.Default")
   Object[][] getTestData() {
      return DATA;
   }

   @Test(dataProvider = "TestDnUrlValidator.Default")
   public void testIsValid(boolean valid, String url) {
      DnValidator validator = new DnValidator();

      Assert.assertEquals(validator.isValid(url, null), valid);
   }
}
