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
package com.vmware.bdd.plugin.ambari.api.model.stack2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiComponentDependency {
   @Expose
   private String href;

   @Expose
   @SerializedName("Dependencies")
   private ApiComponentDependencyInfo apiComponentDependencyInfo;

   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   public ApiComponentDependencyInfo getApiComponentDependencyInfo() {
      return apiComponentDependencyInfo;
   }

   public void setApiComponentDependencyInfo(
         ApiComponentDependencyInfo apiComponentDependencyInfo) {
      this.apiComponentDependencyInfo = apiComponentDependencyInfo;
   }

}
