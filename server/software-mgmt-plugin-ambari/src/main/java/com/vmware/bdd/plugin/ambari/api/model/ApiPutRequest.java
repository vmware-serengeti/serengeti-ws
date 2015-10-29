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

package com.vmware.bdd.plugin.ambari.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiPostRequestInfo;

/**
 * Created by qjin on 7/7/14.
 */
public class ApiPutRequest {
   @Expose
   @SerializedName("Body")
   private ApiBody body;

   @Expose
   @SerializedName("RequestInfo")
   private ApiPostRequestInfo requestInfo;

   public ApiPutRequest(ApiPostRequestInfo apiRequestInfo, ApiBody apiBody) {
      this.requestInfo = apiRequestInfo;
      this.body = apiBody;
   }

   public ApiBody getBody() {
      return body;
   }

   public void setBody(ApiBody body) {
      this.body = body;
   }

   public ApiPostRequestInfo getRequestInfo() {
      return requestInfo;
   }

   public void setRequestInfo(ApiPostRequestInfo requestInfo) {
      this.requestInfo = requestInfo;
   }
}
