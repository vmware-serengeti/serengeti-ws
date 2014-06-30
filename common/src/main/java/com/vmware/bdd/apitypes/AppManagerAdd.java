/***************************************************************************
 * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
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

package com.vmware.bdd.apitypes;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:23 PM
 */
public class AppManagerAdd {

   @Expose
   private String name;

   @Expose
   private String description;

   @Expose
   private String type;

   @Expose
   private String url;

   @Expose
   private String username;

   @Expose
   private String password;

   @Expose
   private String sslCertificate;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getSslCertificate() {
      return sslCertificate;
   }

   public void setSslCertificate(String sslCertificate) {
      this.sslCertificate = sslCertificate;
   }

   public String toString() {
      return (new Gson()).toJson(this);
   }
}
