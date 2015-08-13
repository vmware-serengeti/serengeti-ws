/*
 * **************************************************************************
 *  * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  **************************************************************************
 */

package com.vmware.bdd.plugin.ambari.api.model;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiPostRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequestsPostResourceFilter;

public class ApiPostRequest {

   @Expose
   @SerializedName("Requests/resource_filters")
   private List<ApiRequestsPostResourceFilter> apiRequestsResourceFilters;

   @Expose
   @SerializedName("RequestInfo")
   private ApiPostRequestInfo requestInfo;

   public ApiPostRequest(ApiPostRequestInfo apiRequestInfo) {
      this.requestInfo = apiRequestInfo;
   }

   public ApiPostRequest(ApiPostRequestInfo apiRequestInfo, List<ApiRequestsPostResourceFilter> apiRequestsResourceFilters) {
      this(apiRequestInfo);
      this.apiRequestsResourceFilters = apiRequestsResourceFilters;
   }

   public ApiPostRequestInfo getRequestInfo() {
      return requestInfo;
   }

   public void setRequestInfo(ApiPostRequestInfo requestInfo) {
      this.requestInfo = requestInfo;
   }

   public List<ApiRequestsPostResourceFilter> getApiRequestsResourceFilters() {
      return apiRequestsResourceFilters;
   }

   public void setApiRequestsResourceFilters(
         List<ApiRequestsPostResourceFilter> apiRequestsResourceFilters) {
      this.apiRequestsResourceFilters = apiRequestsResourceFilters;
   }

}
