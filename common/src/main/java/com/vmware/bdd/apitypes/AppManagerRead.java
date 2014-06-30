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

import java.util.List;

import com.google.gson.annotations.Expose;

public class AppManagerRead {

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

   //@Expose
   //private String password;

   @Expose
   private String sslCertificate;

   @Expose
   private List<String> managedClusters;

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
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   /**
    * @param description the description to set
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * @return the type
    */
   public String getType() {
      return type;
   }

   /**
    * @param type the type to set
    */
   public void setType(String type) {
      this.type = type;
   }

   /**
    * @return the url
    */
   public String getUrl() {
      return url;
   }

   /**
    * @param url the url to set
    */
   public void setUrl(String url) {
      this.url = url;
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

   /**
    * @return the ssl certificate
    */
   public String getSslCertificate() {
      return sslCertificate;
   }

   /**
    * @param sslCertificate the ssl certificate to set
    */
   public void setSslCertificate(String sslCertificate) {
      this.sslCertificate = sslCertificate;
   }

   /**
    * @return the list of managed cluster names
    */
   public List<String> getManagedClusters() {
      return managedClusters;
   }

   /**
    * @param managedClusters the list of managed cluster names to set
    */
   public void setManagedClusters(List<String> managedClusters) {
      this.managedClusters = managedClusters;
   }

   @Override
   public String toString() {
      return new StringBuffer().append("[AppManagerRead] ")
            .append("name:").append(this.name)
            .append("description:").append(this.description)
            .append(",type:").append(this.type)
            .append(",url:").append(this.url)
            .append(",username:").append(this.username)
            .append(",sslCertificate:").append(this.sslCertificate)
            .append(",managedClusters:").append(this.managedClusters)
            .toString();
   }

}
