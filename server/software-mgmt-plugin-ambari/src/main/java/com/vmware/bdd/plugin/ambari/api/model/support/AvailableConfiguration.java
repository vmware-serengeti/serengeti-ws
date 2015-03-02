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
package com.vmware.bdd.plugin.ambari.api.model.support;

import java.util.List;

import com.google.gson.annotations.Expose;

public class AvailableConfiguration {

   @Expose
   private String versionHdpMin;

   @Expose
   private String versionHdpMax;

   @Expose
   private List<AvailableConfigurationInfo> configurations;

   public String getVersionHdpMin() {
      return versionHdpMin;
   }

   public void setVersionHdpMin(String versionHdpMin) {
      this.versionHdpMin = versionHdpMin;
   }

   public String getVersionHdpMax() {
      return versionHdpMax;
   }

   public void setVersionHdpMax(String versionHdpMax) {
      this.versionHdpMax = versionHdpMax;
   }

   public List<AvailableConfigurationInfo> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<AvailableConfigurationInfo> configurations) {
      this.configurations = configurations;
   }

}
