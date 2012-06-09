/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.spectypes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HadoopDistroMap {
   private String name;
   @Expose
   @SerializedName("hadoop")
   private String hadoopUrl;
   @Expose
   @SerializedName("pig")
   private String pigUrl;
   @Expose
   @SerializedName("hive")
   private String hiveUrl;

   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getHadoopUrl() {
      return hadoopUrl;
   }
   public void setHadoopUrl(String hadoopUrl) {
      this.hadoopUrl = hadoopUrl;
   }
   public String getPigUrl() {
      return pigUrl;
   }
   public void setPigUrl(String pigUrl) {
      this.pigUrl = pigUrl;
   }
   public String getHiveUrl() {
      return hiveUrl;
   }
   public void setHiveUrl(String hiveUrl) {
      this.hiveUrl = hiveUrl;
   }
}
