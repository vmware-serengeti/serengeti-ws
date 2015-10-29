/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.stack2;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiArtifact;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiKerberosDescriptor;

public class ApiStackServiceInfo {

   @Expose
   private String comments;

   @Expose
   @SerializedName("config_types")
   private Object configTypes;

   @Expose
   @SerializedName("service_name")
   private String serviceName;

   @Expose
   @SerializedName("service_version")
   private String serviceVersion;

   @Expose
   @SerializedName("stack_name")
   private String stackName;

   @Expose
   @SerializedName("stack_version")
   private String stackVersion;

   @Expose
   @SerializedName("user_name")
   private String userName;
   
   // Just for ambari server 2.0
   @Expose
   @SerializedName("custom_commands")
   private List<String> customCommands;
   
   @Expose
   @SerializedName("display_name")
   private String displayName;

   @Expose
   @SerializedName("required_services")
   private List<String> required_services;

   @Expose
   @SerializedName("service_check_supported")
   private Boolean serviceCheckSupported;

   @Expose
   @SerializedName("kerberos_descriptor")
   private ApiKerberosDescriptor kerberosDescriptor;
   
   @Expose
   @SerializedName("configurations")
   private List<ApiConfiguration> configurations;
   
   @Expose
   @SerializedName("artifacts")
   private List<ApiArtifact> artifacts;
   
   public String getComments() {
      return comments;
   }

   public void setComments(String comments) {
      this.comments = comments;
   }

   public String getServiceName() {
      return serviceName;
   }

   public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
   }

   public String getServiceVersion() {
      return serviceVersion;
   }

   public void setServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
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

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public Object getConfigTypes() {
      // TODO consider convert this object to a real instance in future 
      return configTypes;
   }

   public void setConfigTypes(Object configTypes) {
      this.configTypes = configTypes;
   }

   public List<String> getCustomCommands() {
      return customCommands;
   }

   public void setCustomCommands(List<String> customCommands) {
      this.customCommands = customCommands;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public List<String> getRequired_services() {
      return required_services;
   }

   public void setRequired_services(List<String> required_services) {
      this.required_services = required_services;
   }

   public Boolean getServiceCheckSupported() {
      return serviceCheckSupported;
   }

   public void setServiceCheckSupported(Boolean serviceCheckSupported) {
      this.serviceCheckSupported = serviceCheckSupported;
   }

   public ApiKerberosDescriptor getKerberosDescriptor() {
      return kerberosDescriptor;
   }

   public void setKerberosDescriptor(ApiKerberosDescriptor kerberosDescriptor) {
      this.kerberosDescriptor = kerberosDescriptor;
   }
}
