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

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.vmware.aurora.util.StringUtil;

/**
 * Created By xiaoliangl on 11/25/14.
 */
public class LdapUrlValidator implements ConstraintValidator<LdapUrlFormat, String> {

   private Pattern pattern = Pattern.compile(
//         "^ldap(s?)\\:\\/\\/[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(:[0-9]*)?$"
         "^(ldap(?:s?))\\:\\/\\/([0-9a-zA-Z](?:[-.\\w]*[0-9a-zA-Z])*)(?:\\:([0-9]*))?(\\/?([a-z][a-z0-9-]*)=(?![ #])(((?![\\\\=\"+,;<>]).)|(\\\\[ \\\\#=\"+,;<>])|(\\\\[a-f0-9][a-f0-9]))*(,([a-z][a-z0-9-]*)=(?![ #])(((?![\\\\=\"+,;<>]).)|(\\\\[ \\\\#=\"+,;<>])|(\\\\[a-f0-9][a-f0-9]))*)*)*$"
   );

   @Override
   public void initialize(LdapUrlFormat ldapUrlFormat) {

   }

   @Override
   public boolean isValid(String str, ConstraintValidatorContext constraintValidatorContext) {
      //ldap(s)://address:port
      /*
      ^ldap(s?)\:\/\/
      [0-9a-zA-Z]
      ([-.\w]*[0-9a-zA-Z])*
      (:(0-9)*)*(\/?)$
      */

      return StringUtil.isNullOrWhitespace(str) || pattern.matcher(str).matches();

   }
}
