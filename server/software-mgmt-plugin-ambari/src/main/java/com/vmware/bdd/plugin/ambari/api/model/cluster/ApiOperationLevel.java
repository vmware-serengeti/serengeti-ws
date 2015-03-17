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
package com.vmware.bdd.plugin.ambari.api.model.cluster;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by qjin on 3/15/15.
 */
public class ApiOperationLevel {
   @Expose
   @SerializedName("level")
   private String level;

   @Expose
   @SerializedName("cluster_name")
   private String clusterName;

   public ApiOperationLevel(String level, String clusterName) {
      this.level = level;
      this.clusterName = clusterName;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public String getLevel() {
      return level;
   }

   public void setLevel(String level) {
      this.level = level;
   }
}
