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
package com.vmware.bdd.model;

import com.cloudera.api.model.ApiConfig;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiHost;
import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:50 PM
 */
public class CmNodeDef implements Serializable {

   private static final long serialVersionUID = -561299694244815038L;

   @Expose
   private String nodeId;

   @Expose
   private String ipAddress;

   @Expose
   private String fqdn;

   @Expose
   private String rackId;

   @Expose
   private Map<String, String> configs;

   public String getNodeId() {
      return nodeId;
   }

   public void setNodeId(String nodeId) {
      this.nodeId = nodeId;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getFqdn() {
      return fqdn;
   }

   public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
   }

   public String getRackId() {
      return rackId;
   }

   public void setRackId(String rackId) {
      this.rackId = rackId;
   }

   public Map<String, String> getConfigs() {
      return configs;
   }

   public void setConfigs(Map<String, String> configs) {
      this.configs = configs;
   }

   public ApiHost toCmHost() {
      ApiHost apiHost = new ApiHost();
      apiHost.setHostId(this.nodeId);
      apiHost.setIpAddress(this.ipAddress);
      apiHost.setHostname(this.fqdn);
      apiHost.setRackId(this.rackId);
      apiHost.setConfig(new ApiConfigList());
      return apiHost;
   }
}
