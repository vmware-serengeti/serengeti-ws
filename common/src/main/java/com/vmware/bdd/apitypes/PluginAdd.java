package com.vmware.bdd.apitypes;

import com.google.gson.annotations.Expose;

/**
 * Author: Xiaoding Bian
 * Date: 6/4/14
 * Time: 5:23 PM
 */
public class PluginAdd {

   @Expose
   private String name;

   @Expose
   private String provider;

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

   public String toString() {
      return null; // TODO
   }
}
