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
package com.vmware.bdd.apitypes;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.vmware.bdd.validation.DnFormat;
import com.vmware.bdd.validation.LdapUrlFormat;

/**
 * Created By xiaoliangl on 11/24/14.
 */
public class UserMgmtServer {
   public enum Type {
      LDAP, AD_AS_LDAP
   }

   @NotNull
   @Size(min = 1, max = 50)
   private String name;

   @NotNull
   private Type type;

   @NotNull
   @DnFormat
   @Size(min = 1, max = 100)
   private String baseGroupDn;

   @NotNull
   @DnFormat
   @Size(min = 1, max = 100)
   private String baseUserDn;

   @NotNull
   @LdapUrlFormat
   @Size(min = 7, max = 200)
   private String primaryUrl;

   @LdapUrlFormat
   @Size(max = 50)
   private String secondaryUrl;

   @NotNull
   @Size(min = 1, max = 50)
   private String userName;

   @NotNull
   @Size(min = 1, max = 50)
   private String password;

   @NotNull
   @DnFormat
   @Size(min = 1, max = 100)
   private String mgmtVMUserGroupDn;

   public UserMgmtServer() {
   }

   /**
    *
    * @param name
    * @param type
    * @param baseGroupDn
    * @param baseUserDn
    * @param primaryUrl
    * @param secondaryUrl
    * @param userName
    * @param password
    */
   public UserMgmtServer(String name, Type type, String baseGroupDn, String baseUserDn, String primaryUrl, String secondaryUrl, String userName, String password, String mgmtVMUserGroupDn) {
      this.name = name;
      this.type = type;
      this.baseGroupDn = baseGroupDn;
      this.baseUserDn = baseUserDn;
      this.primaryUrl = primaryUrl;
      this.secondaryUrl = secondaryUrl;
      this.userName = userName;
      this.password = password;
      this.mgmtVMUserGroupDn = mgmtVMUserGroupDn;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }


   public String getBaseGroupDn() {
      return baseGroupDn;
   }

   public void setBaseGroupDn(String baseGroupDn) {
      this.baseGroupDn = baseGroupDn;
   }

   public String getBaseUserDn() {
      return baseUserDn;
   }

   public void setBaseUserDn(String baseUserDn) {
      this.baseUserDn = baseUserDn;
   }

   public String getPrimaryUrl() {
      return primaryUrl;
   }

   public void setPrimaryUrl(String primaryUrl) {
      this.primaryUrl = primaryUrl;
   }

   public String getSecondaryUrl() {
      return secondaryUrl;
   }

   public void setSecondaryUrl(String secondaryUrl) {
      this.secondaryUrl = secondaryUrl;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getMgmtVMUserGroupDn() {
      return mgmtVMUserGroupDn;
   }

   public void setMgmtVMUserGroupDn(String mgmtVMUserGroupDn) {
      this.mgmtVMUserGroupDn = mgmtVMUserGroupDn;
   }
}
