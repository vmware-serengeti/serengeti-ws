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
package com.vmware.bdd.usermgmt.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import com.vmware.bdd.apitypes.UserMgmtServer;

/**
 * Created By xiaoliangl on 12/17/14.
 */
@Entity(name = "usermgmtserver")
public class UserMgmtServerEntity {

   @Id
   @Column
   private String name;

   @Column
   @Enumerated(EnumType.STRING)
   private UserMgmtServer.Type type;

   @Column
   private String baseGroupDn;

   @Column
   private String baseUserDn;

   @Column
   private String primaryUrl;

   @Column
   private String userName;

   @Column
   private String password;

   @Column
   private String mgmtVMUserGroupDn;

   public UserMgmtServerEntity() {}

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public UserMgmtServer.Type getType() {
      return type;
   }

   public void setType(UserMgmtServer.Type type) {
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

   public void copyFrom(UserMgmtServer userMgmtServer) {
      setName(userMgmtServer.getName());
      setType(userMgmtServer.getType());
      setBaseGroupDn(userMgmtServer.getBaseGroupDn());
      setBaseUserDn(userMgmtServer.getBaseUserDn());
      setMgmtVMUserGroupDn(userMgmtServer.getMgmtVMUserGroupDn());
      setPrimaryUrl(userMgmtServer.getPrimaryUrl());
      setUserName(userMgmtServer.getUserName());
      setPassword(userMgmtServer.getPassword());
   }

   public UserMgmtServer copyTo() {
      UserMgmtServer userMgmtServer = new UserMgmtServer();
      userMgmtServer.setName(getName());
      userMgmtServer.setType(getType());
      userMgmtServer.setBaseGroupDn(getBaseGroupDn());
      userMgmtServer.setBaseUserDn(getBaseUserDn());
      userMgmtServer.setMgmtVMUserGroupDn(getMgmtVMUserGroupDn());
      userMgmtServer.setPrimaryUrl(getPrimaryUrl());
      userMgmtServer.setUserName(getUserName());
      userMgmtServer.setPassword(getPassword());

      return userMgmtServer;
   }
}
