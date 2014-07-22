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
package com.vmware.bdd.plugin.ambari.api.model.stack;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiConfigurationInfo {

   @Expose
   @SerializedName("property_description")
   private String propertyDescription;

   @Expose
   @SerializedName("property_name")
   private String propertyName;

   @Expose
   @SerializedName("property_value")
   private String propertyValue;

   @Expose
   @SerializedName("stack_name")
   private String stackName;

   @Expose
   @SerializedName("stack_version")
   private String stackVersion;

   @Expose
   @SerializedName("type")
   private String Type;

   public String getPropertyDescription() {
      return propertyDescription;
   }

   public void setPropertyDescription(String propertyDescription) {
      this.propertyDescription = propertyDescription;
   }

   public String getPropertyName() {
      return propertyName;
   }

   public void setPropertyName(String propertyName) {
      this.propertyName = propertyName;
   }

   public String getPropertyValue() {
      return propertyValue;
   }

   public void setPropertyValue(String propertyValue) {
      this.propertyValue = propertyValue;
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

   public String getType() {
      return Type;
   }

   public void setType(String type) {
      Type = type;
   }

}
