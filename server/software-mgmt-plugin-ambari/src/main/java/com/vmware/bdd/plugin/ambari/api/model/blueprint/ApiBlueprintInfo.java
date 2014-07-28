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
package com.vmware.bdd.plugin.ambari.api.model.blueprint;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiBlueprintInfo {

   @Expose
   @SerializedName("blueprint_name")
   private String blueprintName;

   @Expose
   @SerializedName("stack_name")
   private String StackName;

   @Expose
   @SerializedName("stack_version")
   private String stackVersion;

   public String getStackName() {
      return StackName;
   }

   public void setStackName(String stackName) {
      StackName = stackName;
   }

   public String getStackVersion() {
      return stackVersion;
   }

   public void setStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
   }

   public String getBlueprintName() {
      return blueprintName;
   }

   public void setBlueprintName(String blueprintName) {
      this.blueprintName = blueprintName;
   }

}
