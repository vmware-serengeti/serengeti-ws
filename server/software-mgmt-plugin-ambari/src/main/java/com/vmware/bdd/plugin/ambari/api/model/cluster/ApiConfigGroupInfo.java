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

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiConfigGroupInfo {

   @Expose
   @SerializedName("cluster_name")
   private String clusterName;

   @Expose
   @SerializedName("group_name")
   private String groupName;

   @Expose
   private String tag;

   @Expose
   private String description;

   @Expose
   private List<ApiHostInfo> hosts;

   @Expose
   @SerializedName("desired_configs")
   private List<ApiConfigGroupConfiguration> desiredConfigs;

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public String getGroupName() {
      return groupName;
   }

   public void setGroupName(String groupName) {
      this.groupName = groupName;
   }

   public String getTag() {
      return tag;
   }

   public void setTag(String tag) {
      this.tag = tag;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public List<ApiHostInfo> getHosts() {
      return hosts;
   }

   public void setHosts(List<ApiHostInfo> hosts) {
      this.hosts = hosts;
   }

   public List<ApiConfigGroupConfiguration> getDesiredConfigs() {
      return desiredConfigs;
   }

   public void setDesiredConfigs(List<ApiConfigGroupConfiguration> desiredConfigs) {
      this.desiredConfigs = desiredConfigs;
   }
}
