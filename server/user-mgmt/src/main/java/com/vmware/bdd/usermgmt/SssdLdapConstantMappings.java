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

import java.util.Properties;

import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.UserMgmtServer;

/**
 * Created By xiaoliangl on 12/10/14.
 */
@Component
public class SssdLdapConstantMappings {
   public static final String LDAP_USER_OBJECT_CLASS = "ldap_user_object_class";
   public static final String LDAP_SCHEMA = "ldap_schema";
   public static final String LDAP_USER_NAME = "ldap_user_name";
   public static final String LDAP_GROUP_GID_NUMBER = "ldap_group_gid_number";
   public static final String LDAP_USER_GID_NUMBER = "ldap_user_gid_number";
   public static final String LDAP_USER_UID_NUMBER = "ldap_user_uid_number";
   public static final String LDAP_GROUP_OBJECT_CLASS = "ldap_group_object_class";
   public static final String LDAP = UserMgmtServer.Type.LDAP.name()+ '.';
   public static final String AD = UserMgmtServer.Type.AD_AS_LDAP.name()+'.';

   private Properties mapping = null;

   public SssdLdapConstantMappings() {
      mapping = new Properties();

      mapping.setProperty(LDAP + LDAP_GROUP_OBJECT_CLASS, "posixGroup");
      mapping.setProperty(AD + LDAP_GROUP_OBJECT_CLASS, "Group");

      mapping.setProperty(LDAP + LDAP_USER_OBJECT_CLASS, "posixAccount");
      mapping.setProperty(AD + LDAP_USER_OBJECT_CLASS, "User");

      mapping.setProperty(LDAP + LDAP_SCHEMA, "rfc2307");
      mapping.setProperty(AD + LDAP_SCHEMA, "rfc2307bis");

      mapping.setProperty(LDAP + LDAP_USER_NAME, "uid");
      mapping.setProperty(AD + LDAP_USER_NAME, "msSFU30Name");

      mapping.setProperty(LDAP + LDAP_GROUP_GID_NUMBER, "gidNumber");
      mapping.setProperty(AD + LDAP_GROUP_GID_NUMBER, "gidNumber");

      mapping.setProperty(LDAP + LDAP_USER_GID_NUMBER, "gidNumber");
      mapping.setProperty(AD + LDAP_USER_GID_NUMBER, "gidNumber");

      mapping.setProperty(LDAP + LDAP_USER_UID_NUMBER, "uidNumber");
      mapping.setProperty(AD + LDAP_USER_UID_NUMBER, "uidNumber");
   }

   public interface Getter {
      String get(String commonName);
   }

   public Getter getterFirst(final UserMgmtServer.Type type) {
      return new Getter() {
         @Override
         public String get(String commonName) {
            return SssdLdapConstantMappings.this.get(type, commonName);
         }
      };
   }

   public String get(UserMgmtServer.Type type, String commonName) {
      return mapping.getProperty(type.name()+ "."+ commonName);
   }
}
