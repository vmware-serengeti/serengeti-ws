package com.vmware.bdd.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.PluginAdd;
import com.vmware.bdd.software.mgmt.plugin.model.PluginInfo;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 2:15 PM
 */

@Entity
@SequenceGenerator(name = "IdSequence", sequenceName = "plugin_seq", allocationSize = 1)
@Table(name = "plugin")
public class PluginEntity extends EntityBase {

   @Column(name = "name", unique = true, nullable = false)
   private String name;

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

   public PluginEntity() {
   }

   public PluginEntity(String name, String host, int port, String username, String password, String privateKey) {
      this.name = name;
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      this.privateKey = privateKey;
   }

   public PluginEntity(PluginAdd pluginAdd) {
      this.name = pluginAdd.getName();
      this.host = pluginAdd.getHost();
//      this.port = this.provider.getDefaultPort();
      if (pluginAdd.getPort() != -1) {
         this.port = pluginAdd.getPort();
      }
      this.username = pluginAdd.getUsername();
      this.password = pluginAdd.getPassword();
      this.privateKey = pluginAdd.getPrivateKey();
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
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
