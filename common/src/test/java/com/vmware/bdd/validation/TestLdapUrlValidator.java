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
public class TestLdapUrlValidator {
   private final static Object[][] DATA = new Object[][]{
         {true, "ldap://hello:389"},
         {true, "ldap://hello"},
         {true, "ldaps://10.112.113.137"},
         {true, "ldap://10.112.113.137:389"},
         {true, "ldaps://ldap.vmware.com:2231"},
         {false, "http://ldap.vmware.com:2231"},
         {false, "this is a test"},
         {false, "ldaps://ldap.vmware.com:2231/hello"},
         {true, "ldaps://ldap-bde.vmware.com:339"},
         {true, "ldap://ldap.itd.umich.edu:222/o=University%20of%20Michigan,c=US"},
         {true, "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US?postalAddress"},
         {true, "ldap://host.com:6666/o=University%20of%20Michigan,c=US??sub?(cn=Babs%20Jensen)"},
         {true, "ldap://ldap.itd.umich.edu/c=GB?objectClass?one"},
         {true, "ldap://ldap.question.com/o=Question%3f,c=US?mail"},
         {true, "ldap://ldap.netscape.com/o=Babsco,c=US??(int=%5c00%5c00%5c00%5c04)"},
         /*{true, "ldap://ldap.netscape.com/??sub??!bindname=cn=Manager%2co=Foo"},*/

         {false, "ldaps://ldap-bde.vmware.com:339@#$!"},
         {false, "ldaps://ldap-bde.vmware.com@:339"},
         {false, "ldaps://ldap-bde.vmware#.com:339"},
         {false, "ldaps://ldap-bde$.vmware.com:339"},
         {true, "   "},
         {true, null},
   };

   @DataProvider(name = "TestLdapUrlValidator.Default")
   Object[][] getTestData() {
      return DATA;
   }

   @Test(dataProvider = "TestLdapUrlValidator.Default")
   public void testIsValid(boolean valid, String url) {
      LdapUrlValidator validator = new LdapUrlValidator();

      Assert.assertEquals(validator.isValid(url, null), valid);
   }
}
