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
package com.vmware.bdd.plugin.ambari.api.model.cluster.request;

import java.util.HashMap;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiPostRequestInfo {

   @Expose
   @SerializedName("context")
   private String context;

   @Expose
   @SerializedName("command")
   private String command;

   @Expose
   @SerializedName("operation_level")
   private ApiOperationLevel operationLevel;

   @Expose
   @SerializedName("parameters")
   private HashMap<String, String> parameters;

   @Expose
   @SerializedName("query")
   private String query;

   @Expose
   @SerializedName("resource_filters")
   private List<String> resourceFilters;

   public String getContext() {
      return context;
   }

   public void setContext(String context) {
      this.context = context;
   }

   public String getCommand() {
      return command;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public ApiOperationLevel getOperationLevel() {
      return operationLevel;
   }

   public void setOperationLevel(ApiOperationLevel operationLevel) {
      this.operationLevel = operationLevel;
   }

   public HashMap<String, String> getParameters() {
      return parameters;
   }

   public void setParameters(HashMap<String, String> parameters) {
      this.parameters = parameters;
   }

   public String getQuery() {
      return query;
   }

   public void setQuery(String query) {
      this.query = query;
   }

   public List<String> getResourceFilters() {
      return resourceFilters;
   }

   public void setResourceFilters(List<String> resourceFilters) {
      this.resourceFilters = resourceFilters;
   }

}
