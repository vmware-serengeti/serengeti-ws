/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.stack;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiArtifactInfo {

   @Expose
   @SerializedName("kerberos_descriptor")
   private String kerberosDescriptor;

   @Expose
   @SerializedName("service_name")
   private String serviceName;

   @Expose
   @SerializedName("stack_name")
   private String stackName;

   @Expose
   @SerializedName("stack_version")
   private String stackVersion;

   public String getKerberosDescriptor() {
      return kerberosDescriptor;
   }

   public void setKerberosDescriptor(String kerberosDescriptor) {
      this.kerberosDescriptor = kerberosDescriptor;
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

}
