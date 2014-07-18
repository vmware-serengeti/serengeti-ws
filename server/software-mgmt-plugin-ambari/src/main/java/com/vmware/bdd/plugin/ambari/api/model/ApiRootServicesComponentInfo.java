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

public class ApiRootServicesComponentInfo {

   @Expose
   private String href;

   /**
    * @return the href
    */
   public String getHref() {
      return href;
   }

   /**
    * @param href the href to set
    */
   public void setHref(String href) {
      this.href = href;
   }

   /**
    * @return the componentName
    */
   public String getComponentName() {
      return componentName;
   }

   /**
    * @param componentName the componentName to set
    */
   public void setComponentName(String componentName) {
      this.componentName = componentName;
   }

   /**
    * @return the componentVersion
    */
   public String getComponentVersion() {
      return componentVersion;
   }

   /**
    * @param componentVersion the componentVersion to set
    */
   public void setComponentVersion(String componentVersion) {
      this.componentVersion = componentVersion;
   }

   @Expose
   @SerializedName("component_name")
   private String componentName;

   @Expose
   @SerializedName("component_version")
   private String componentVersion;

}
