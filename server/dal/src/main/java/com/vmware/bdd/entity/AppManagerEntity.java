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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.AppManagerAdd;
import com.vmware.bdd.software.mgmt.plugin.model.PluginInfo;

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

   @Column(name = "provider", nullable = false)
   private String provider;

   @Column(name = "host")
   private String host;

   @Column(name = "port")
   private int port;

   @Column(name = "username")
   private String username;

   @Column(name = "password")
   private String password;

   @Column(name = "private_key")
   private String privateKey;

   static final Logger logger = Logger.getLogger(ClusterEntity.class);

   public AppManagerEntity() {
   }

   public AppManagerEntity(String name, String provider, String host, int port, String username, String password, String privateKey) {
      this.name = name;
      this.provider = provider;
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      this.privateKey = privateKey;
   }

   public AppManagerEntity(AppManagerAdd appManagerAdd) {
      this.name = appManagerAdd.getName();
      this.provider = appManagerAdd.getProvider();
      this.host = appManagerAdd.getHost();
//      this.port = this.provider.getDefaultPort();
      if (appManagerAdd.getPort() != -1) {
         this.port = appManagerAdd.getPort();
      }
      this.username = appManagerAdd.getUsername();
      this.password = appManagerAdd.getPassword();
      this.privateKey = appManagerAdd.getPrivateKey();
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getProvider() {
      return provider;
   }

   public void setProvider(String provider) {
      this.provider = provider;
   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
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

   public String getPrivateKey() {
      return privateKey;
   }

   public void setPrivateKey(String privateKey) {
      this.privateKey = privateKey;
   }

   public PluginInfo toPluginInfo() {
      PluginInfo pluginInfo = new PluginInfo();
      pluginInfo.setName(this.name);
      pluginInfo.setHost(this.host);
      pluginInfo.setPort(this.port);
      pluginInfo.setUsername(this.username);
      pluginInfo.setPassword(this.password);
      pluginInfo.setPrivateKey(this.privateKey);
      return pluginInfo;
   }
}
