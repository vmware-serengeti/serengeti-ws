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

import com.google.gson.annotations.Expose;

public class AppManagerRead {

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

   //@Expose
   //private String password;

   //@Expose
   //private String privateKey;

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the provider
    */
   public String getProvider() {
      return provider;
   }

   /**
    * @param provider the provider to set
    */
   public void setProvider(String provider) {
      this.provider = provider;
   }

   /**
    * @return the host
    */
   public String getHost() {
      return host;
   }

   /**
    * @param host the host to set
    */
   public void setHost(String host) {
      this.host = host;
   }

   /**
    * @return the port
    */
   public int getPort() {
      return port;
   }

   /**
    * @param port the port to set
    */
   public void setPort(int port) {
      this.port = port;
   }

   /**
    * @return the username
    */
   public String getUsername() {
      return username;
   }

   /**
    * @param username the username to set
    */
   public void setUsername(String username) {
      this.username = username;
   }

   @Override
   public String toString() {
      return new StringBuffer().append("[AppManagerRead] ")
            .append("name:").append(this.name)
            .append(",provider:").append(this.provider)
            .append(",host:").append(this.host)
            .append(",port:").append(this.port)
            .append(",username:").append(this.username)
            .toString();
   }

}
