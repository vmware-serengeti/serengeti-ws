package com.vmware.bdd.software.mgmt.plugin.model;

import java.io.Serializable;

import com.google.gson.annotations.Expose;

/**
 * Author: Xiaoding Bian
 * Date: 6/10/14
 * Time: 5:20 PM
 */
public class PluginInfo implements Serializable{

   private static final long serialVersionUID = 3490819705601477200L;

   @Expose
   private String name;

   @Expose
   private String host;

   @Expose
   private int port;

   @Expose
   private String username;

   @Expose
   private String password;

   @Expose
   private String privateKey;

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

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      PluginInfo other = (PluginInfo) obj;
      if (host == null) {
         if (other.host != null)
            return false;
      } else if (!host.equals(other.host))
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (password == null) {
         if (other.password != null)
            return false;
      } else if (!password.equals(other.password))
         return false;
      if (port != other.port)
         return false;
      if (privateKey == null) {
         if (other.privateKey != null)
            return false;
      } else if (!privateKey.equals(other.privateKey))
         return false;
      if (username == null) {
         if (other.username != null)
            return false;
      } else if (!username.equals(other.username))
         return false;
      return true;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((host == null) ? 0 : host.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((password == null) ? 0 : password.hashCode());
      result = prime * result + port;
      result =
            prime * result + ((privateKey == null) ? 0 : privateKey.hashCode());
      result = prime * result + ((username == null) ? 0 : username.hashCode());
      return result;
   }
}
