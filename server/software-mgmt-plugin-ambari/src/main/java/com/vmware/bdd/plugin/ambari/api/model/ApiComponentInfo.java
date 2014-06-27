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
package com.vmware.bdd.plugin.ambari.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiComponentInfo {

   @Expose
   private String name;

   @Expose
   @SerializedName("component_name")
   private String componentName;

   @Expose
   @SerializedName("service_name")
   private String serviceName;

   @Expose
   @SerializedName("stack_name")
   private String stackName;

   @Expose
   @SerializedName("stack_version")
   private String stackVersion;

   @Expose
   private String cardinality;

   @Expose
   @SerializedName("component_category")
   private String componentCategory;

   @Expose
   @SerializedName("is_client")
   private Boolean isClient;

   @Expose
   @SerializedName("is_master")
   private Boolean isMaster;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getComponentName() {
      return componentName;
   }

   public void setComponentName(String componentName) {
      this.componentName = componentName;
   }

   public String getServiceName() {
      return serviceName;
   }

   public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
   }

   public String getStackName() {
      return stackName;
   }

   public void setStackName(String stackName) {
      this.stackName = stackName;
   }

   public String getStackVersion() {
      return stackVersion;
   }

   public void setStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
   }

   public String getCardinality() {
      return cardinality;
   }

   public void setCardinality(String cardinality) {
      this.cardinality = cardinality;
   }

   public String getComponentCategory() {
      return componentCategory;
   }

   public void setComponentCategory(String componentCategory) {
      this.componentCategory = componentCategory;
   }

   public Boolean isClient() {
      return isClient;
   }

   public void setClient(Boolean isClient) {
      this.isClient = isClient;
   }

   public Boolean isMaster() {
      return isMaster;
   }

   public void setMaster(Boolean isMaster) {
      this.isMaster = isMaster;
   }
}
