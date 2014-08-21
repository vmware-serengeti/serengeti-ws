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
package com.vmware.bdd.plugin.ambari.api.model.cluster;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;

public class ApiConfigGroup {

   @Expose
   private String href;

   @Expose
   @SerializedName("ConfigGroup")
   private ApiConfigGroupInfo apiConfigGroupInfo;

   public ApiConfigGroupInfo getApiConfigGroupInfo() {
      return apiConfigGroupInfo;
   }

   public void setApiConfigGroupInfo(ApiConfigGroupInfo apiConfigGroupInfo) {
      this.apiConfigGroupInfo = apiConfigGroupInfo;
   }

   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   public String toString() {
      return ApiUtils.objectToJson(this);
   }
}
