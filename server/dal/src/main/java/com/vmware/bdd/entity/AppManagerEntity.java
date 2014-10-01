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

package com.vmware.bdd.entity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.security.EncryptionGuard;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:15 PM
 */

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "appmanager_seq", allocationSize = 1)
@Table(name = "appmanager")
public class AppManagerEntity extends EntityBase {

   @Column(name = "name", unique = true, nullable = false)
   private String name;

   @Column(name = "description")
   private String description;

   @Column(name = "type", nullable = false)
   private String type;

   @Column(name = "url")
   private String url;

   @Column(name = "username")
   private String username;

   @Column(name = "password")
   private String password;

   @Column(name = "ssl_certificate")
   private String sslCertificate;

   static final Logger logger = Logger.getLogger(AppManagerEntity.class);

   public AppManagerEntity() {
   }

   public AppManagerEntity(String name, String description, String type, String url, String username, String password, String sslCertificate) {
      this.name = name;
      this.description = description;
      this.type = type;
      this.url = url;
      this.username = username;
      this.setPassword(password);
      this.sslCertificate = sslCertificate;
   }

   public AppManagerEntity(AppManagerAdd appManagerAdd) {
      this.name = appManagerAdd.getName();
      this.description = appManagerAdd.getDescription();
      this.type = appManagerAdd.getType();
      this.url = appManagerAdd.getUrl();
      this.username = appManagerAdd.getUsername();
      this.setPassword(appManagerAdd.getPassword());
      this.sslCertificate = appManagerAdd.getSslCertificate();
   }

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
      if (this.password == null || this.password.isEmpty()) {
         return "";
      }

      String password = null;
      try {
         password = EncryptionGuard.decode(this.password);
      } catch (UnsupportedEncodingException e) {
         logger.warn("Unsupported Encoding Exception: ", e);
      } catch (GeneralSecurityException e) {
         logger.warn("General Security Exception: ", e);
      }
      return password;
   }

   public void setPassword(String password) {
      if ("".equals(password)) {
         this.password = "";
         return;
      }
      try {
         this.password = EncryptionGuard.encode(password);
      } catch (UnsupportedEncodingException e) {
         logger.warn("Unsupported Encoding Exception: ", e);
      } catch (GeneralSecurityException e) {
         logger.warn("General Security Exception: ", e);
      }
   }

   public String getSslCertificate() {
      return sslCertificate;
   }

   public void setSslCertificate(String sslCertificate) {
      this.sslCertificate = sslCertificate;
   }
}
