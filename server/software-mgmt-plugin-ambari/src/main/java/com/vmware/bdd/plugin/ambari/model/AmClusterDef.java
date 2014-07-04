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

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprintInfo;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostGroup;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;

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

   public AmClusterDef(ClusterBlueprint blueprint, String privateKey) {
      this.name = blueprint.getName();
      this.version = blueprint.getHadoopStack().getFullVersion();
      this.verbose = true;
      this.sshKey = privateKey;
      this.user = "serengeti";

      this.nodes = new ArrayList<AmNodeDef>();
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         for (NodeInfo node : group.getNodes()) {
            AmNodeDef nodeDef = new AmNodeDef();
            nodeDef.setName(node.getName());
            nodeDef.setIp(node.getMgtIpAddress());
            nodeDef.setFqdn(node.getHostname());
            nodeDef.setRackInfo(node.getRack());
            nodeDef.setBlueprintConfigurationsToAm(group.getConfiguration());
            nodeDef.setComponents(group.getRoles());
            nodeDef.setVolumns(node.getVolumes());
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

   public ApiBootstrap toApibootStrap() {
      ApiBootstrap apiBootstrap = new ApiBootstrap();
      apiBootstrap.setVerbose(verbose);
      List<String> hosts = new ArrayList<String>();
      for (AmNodeDef node : getNodes()) {
         hosts.add(node.getFqdn());
      }
      apiBootstrap.setHosts(hosts);
      apiBootstrap.setSshKey(sshKey);
      apiBootstrap.setUser(user);
      return apiBootstrap;
   }

   public ApiBlueprint toApiBlueprint() {
      ApiBlueprint apiBlueprint = new ApiBlueprint();

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

}
