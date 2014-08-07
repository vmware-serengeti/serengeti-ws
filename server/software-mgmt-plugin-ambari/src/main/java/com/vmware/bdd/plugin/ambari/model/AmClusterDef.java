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
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprintInfo;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;

public class AmClusterDef implements Serializable {

   private static final long serialVersionUID = 5585914268769234047L;

   @Expose
   private String name;

   @Expose
   private String version;

   @Expose
   private boolean verbose;

   @Expose
   private String sshKey;

   @Expose
   private String user;

   @Expose
   private List<AmNodeDef> nodes;

   @Expose
   private AmStackDef amStack;

   private ClusterReport currentReport;

   @Expose
   private List<Map<String, Object>> configurations;

   public AmClusterDef(ClusterBlueprint blueprint, String privateKey) {
      this.name = blueprint.getName();
      this.version = blueprint.getHadoopStack().getFullVersion();
      this.verbose = true;
      this.sshKey = privateKey;
      this.user = Constants.AMBARI_SSH_USER;
      this.currentReport = new ClusterReport(blueprint);
      this.configurations =
            AmUtils.toAmConfigurations(blueprint.getConfiguration());

      this.nodes = new ArrayList<AmNodeDef>();
      HdfsVersion hdfs = getDefaultHdfsVersion(this.version);
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         for (NodeInfo node : group.getNodes()) {
            AmNodeDef nodeDef = new AmNodeDef();
            nodeDef.setName(node.getName());
            nodeDef.setIp(node.getMgtIpAddress());
            nodeDef.setFqdn(node.getHostname());
            nodeDef.setRackInfo(node.getRack());
            nodeDef.setConfigurations(AmUtils.toAmConfigurations(group
                  .getConfiguration()));
            nodeDef.setComponents(group.getRoles());
            nodeDef.setVolumns(node.getVolumes(), hdfs);
            this.nodes.add(nodeDef);
         }
      }

      AmStackDef stackDef = new AmStackDef();
      stackDef.setName(blueprint.getHadoopStack().getVendor());
      stackDef.setVersion(blueprint.getHadoopStack().getFullVersion());
      this.amStack = stackDef;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public boolean isVerbose() {
      return verbose;
   }

   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }

   public String getSshKey() {
      return sshKey;
   }

   public void setSshKey(String sshKey) {
      this.sshKey = sshKey;
   }

   public String getUser() {
      return user;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public List<AmNodeDef> getNodes() {
      return nodes;
   }

   public void setNodes(List<AmNodeDef> nodes) {
      this.nodes = nodes;
   }

   public AmStackDef getAmStack() {
      return amStack;
   }

   public void setAmStack(AmStackDef amStack) {
      this.amStack = amStack;
   }

   public ClusterReport getCurrentReport() {
      return currentReport;
   }

   public void setCurrentReport(ClusterReport currentReport) {
      this.currentReport = currentReport;
   }

   public List<Map<String, Object>> getConfigurations() {
      return configurations;
   }

   public void setConfigurations(List<Map<String, Object>> configurations) {
      this.configurations = configurations;
   }

   public ApiBootstrap toApiBootStrap() {
      return toApiBootStrap(null);
   }

   public ApiBootstrap toApiBootStrap(List<String> hostNames) {
      ApiBootstrap apiBootstrap = new ApiBootstrap();
      apiBootstrap.setVerbose(verbose);
      List<String> hosts = new ArrayList<String>();
      for (AmNodeDef node : getNodes()) {
         if (hostNames == null) {
            hosts.add(node.getFqdn());
         } else if (hostNames.contains(node.getName())) {
            hosts.add(node.getFqdn());
         }
      }
      apiBootstrap.setHosts(hosts);
      apiBootstrap.setSshKey(sshKey);
      apiBootstrap.setUser(user);
      return apiBootstrap;
   }

   public ApiBlueprint toApiBlueprint() {
      ApiBlueprint apiBlueprint = new ApiBlueprint();

      apiBlueprint.setConfigurations(configurations);

      ApiBlueprintInfo apiBlueprintInfo = new ApiBlueprintInfo();
      apiBlueprintInfo.setStackName(amStack.getName());
      apiBlueprintInfo.setStackVersion(amStack.getVersion());
      apiBlueprint.setApiBlueprintInfo(apiBlueprintInfo);

      List<ApiHostGroup> apiHostGroups = new ArrayList<ApiHostGroup>();
      for (AmNodeDef node : nodes) {
         apiHostGroups.add(node.toApiHostGroupForBlueprint());
      }
      apiBlueprint.setApiHostGroups(apiHostGroups);
      return apiBlueprint;
   }

   public ApiClusterBlueprint toApiClusterBlueprint() {
      ApiClusterBlueprint apiClusterBlueprint = new ApiClusterBlueprint();
      apiClusterBlueprint.setBlueprint(name);

      List<ApiHostGroup> apiHostGroups = new ArrayList<ApiHostGroup>();
      for (AmNodeDef node : nodes) {
         apiHostGroups.add(node.toApiHostGroupForClusterBlueprint());
      }
      apiClusterBlueprint.setApiHostGroups(apiHostGroups);
      return apiClusterBlueprint;
   }

   private static HdfsVersion getDefaultHdfsVersion(String distroVersion) {
      if (distroVersion.startsWith("2")) {
         return HdfsVersion.V2;
      } else {
         return HdfsVersion.V1;
      }
   }

}
