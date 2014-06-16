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
   public boolean equals(Object o) {
      return false;
   }

   @Override
   public int hashCode() {
      return 0;
   }
}
