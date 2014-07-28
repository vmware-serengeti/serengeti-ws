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
package com.vmware.bdd.plugin.ambari.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;

public class AmNodeDef implements Serializable {

   private static final long serialVersionUID = 5585914239769234047L;

   @Expose
   private String name;

   @Expose
   private String ip;

   @Expose
   private String fqdn;

   @Expose
   private String rackInfo;

   @Expose
   private List<Map<String, Object>> configurations;

   @Expose
   private List<String> components;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getIp() {
      return ip;
   }

   public void setIp(String ip) {
      this.ip = ip;
   }

   public String getFqdn() {
      return fqdn;
   }

   public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
   }

   public String getRackInfo() {
      return rackInfo;
   }

   public void setRackInfo(String rackInfo) {
      this.rackInfo = rackInfo;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public List<String> getComponents() {
      return components;
   }

   public void setComponents(List<String> components) {
      this.components = components;
   }

   public void setVolumns(List<String> volumns) {
      for (String component : components) {
         switch (component) {
         case "NAMENODE":
            addConfiguration("hdfs-site", "dfs.namenode.name.dir",
                  dataDirs(volumns, "/hdfs/namenode"));
            break;
         case "SECONDARY_NAMENODE":
            addConfiguration("hdfs-site", "dfs.namenode.checkpoint.dir",
                  dataDirs(volumns, "/hdfs/namesecondary"));
            break;
         case "DATANODE":
            addConfiguration("hdfs-site", "dfs.datanode.data.dir",
                  dataDirs(volumns, "/hdfs/data"));
            break;
         case "NODEMANAGER":
            addConfiguration("yarn-site", "yarn.nodemanager.local-dirs",
                  dataDirs(volumns, "/yarn/local"));
            break;
         default:
            break;
         }
      }
   }

   private String dataDirs(List<String> volumes, String postFix) {
      if ("/hdfs/namesecondary".equals(postFix)) {
         return volumes.get(0);
      }
      List<String> dirList = new ArrayList<String>();
      for (String volume : volumes) {
         dirList.add(volume + postFix);
      }
      return StringUtils.join(dirList, ",");
   }

   public void addConfiguration(String configurationType, String propertyName,
         String propertyValue) {
      Map<String, String> property = new HashMap<String, String>();
      property.put(propertyName, propertyValue);
      this.configurations = AmUtils.toAmConfigurations(this.configurations, configurationType, property);
   }

   public ApiHostGroup toApiHostGroupForBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();

      apiHostGroup.setName(name);
      apiHostGroup.setCardinality("1");

      List<Map<String, Object>> apiConfigurations =
            new ArrayList<Map<String, Object>>();
      if (configurations != null) {
         apiConfigurations = configurations;
      }
      apiHostGroup.setConfigurations(apiConfigurations);

      List<ApiComponentInfo> apiComponents = new ArrayList<ApiComponentInfo>();
      for (String componentName : components) {
         ApiComponentInfo apiComponent = new ApiComponentInfo();
         apiComponent.setName(componentName);
         apiComponents.add(apiComponent);
      }
      apiHostGroup.setApiComponents(apiComponents);
      return apiHostGroup;
   }

   public ApiHostGroup toApiHostGroupForClusterBlueprint() {
      ApiHostGroup apiHostGroup = new ApiHostGroup();

      apiHostGroup.setName(name);

      List<Map<String, Object>> apiConfigurations =
            new ArrayList<Map<String, Object>>();
      if (configurations != null) {
         apiConfigurations = configurations;
      }
      apiHostGroup.setConfigurations(apiConfigurations);

      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      ApiHost apiHost = new ApiHost();
      apiHost.setFqdn(fqdn);
      apiHosts.add(apiHost);
      apiHostGroup.setApiHosts(apiHosts);
      return apiHostGroup;
   }

}
