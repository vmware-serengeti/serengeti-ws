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

import com.google.gson.annotations.Expose;
import com.vmware.aurora.global.Configuration;
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

   private String ambariServerVersion;

   @Expose
   private List<Map<String, Object>> configurations;

   private boolean isComputeOnly = false;

   private String externalNamenode;

   private String externalSecondaryNamenode;

   private static final Map<String, String> serviceName2ServiceUserConfigName;

   static {
      serviceName2ServiceUserConfigName = new HashMap<>();
      serviceName2ServiceUserConfigName.put("HDFS", "hdfs");
      serviceName2ServiceUserConfigName.put("YARN", "yarn");
   }

   public AmClusterDef(ClusterBlueprint blueprint, String privateKey) {
      this(blueprint, privateKey, null);
   }

   public AmClusterDef(ClusterBlueprint blueprint, String privateKey, String ambariServerVersion) {
      this.name = blueprint.getName();
      this.version = blueprint.getHadoopStack().getFullVersion();
      this.verbose = true;
      this.sshKey = privateKey;
      this.user = Constants.AMBARI_SSH_USER;
      this.currentReport = new ClusterReport(blueprint);
      this.ambariServerVersion = ambariServerVersion;

      this.nodes = new ArrayList<AmNodeDef>();
      HdfsVersion hdfs = getDefaultHdfsVersion(this.version);
      if (blueprint.hasTopologyPolicy()) {
         setRackTopologyFileName(blueprint);
      }
      this.configurations =
            AmUtils.toAmConfigurations(blueprint.getConfiguration());
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
            nodeDef.setVolumns(node.getVolumes(), hdfs, ambariServerVersion);
            this.nodes.add(nodeDef);
         }
      }
      if (blueprint.getExternalNamenode() != null) {
         this.isComputeOnly = true;

         this.externalNamenode = blueprint.getExternalNamenode();
         this.externalSecondaryNamenode = blueprint.getExternalSecondaryNamenode();

         AmNodeDef namenodeDef = new AmNodeDef();
         namenodeDef.setName(name+"-external-namenode");
         namenodeDef.setFqdn(externalNamenode);
         List<String> namenodeRoles = new ArrayList<String>();
         namenodeRoles.add("NAMENODE");
         if (!isValidExternalSecondaryNamenode()) {
            namenodeRoles.add("SECONDARY_NAMENODE");
         }
         namenodeDef.setComponents(namenodeRoles);
         this.nodes.add(namenodeDef);

         if (isValidExternalSecondaryNamenode()) {
            AmNodeDef secondaryNamenodeDef = new AmNodeDef();
            secondaryNamenodeDef.setName(name+"-external-secondaryNamenode");
            secondaryNamenodeDef.setFqdn(externalSecondaryNamenode);
            List<String> secondaryNamenodeRoles = new ArrayList<String>();
            secondaryNamenodeRoles.add("SECONDARY_NAMENODE");
            secondaryNamenodeDef.setComponents(secondaryNamenodeRoles);
            this.nodes.add(secondaryNamenodeDef);
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

   public boolean isComputeOnly() {
      return isComputeOnly;
   }

   public void setComputeOnly(boolean isComputeOnly) {
      this.isComputeOnly = isComputeOnly;
   }

   public String getExternalNamenode() {
      return externalNamenode;
   }

   public void setExternalNamenode(String externalNamenode) {
      this.externalNamenode = externalNamenode;
   }

   public String getExternalSecondaryNamenode() {
      return externalSecondaryNamenode;
   }

   public void setExternalSecondaryNamenode(String externalSecondaryNamenode) {
      this.externalSecondaryNamenode = externalSecondaryNamenode;
   }

   public ApiBootstrap toApiBootStrap() {
      return toApiBootStrap(null);
   }

   public ApiBootstrap toApiBootStrap(List<String> hostNames) {
      ApiBootstrap apiBootstrap = new ApiBootstrap();
      apiBootstrap.setVerbose(verbose);
      List<String> hosts = new ArrayList<String>();
      for (AmNodeDef node : getNodes()) {

         // Generate all hosts for bootstrap except external namenode and secondary namenode
         if (isNodeGenerateFromExternalNamenode(node)) {
            continue;
         }

         if (isNodeGenerateFromExternalSecondaryNamenode(node)) {
            continue;
         }

         if (hostNames == null) {
            hosts.add(node.getFqdn());
         } else if (hostNames.contains(node.getName())) {
            hosts.add(node.getFqdn());
         }
      }
      apiBootstrap.setHosts(hosts);
      apiBootstrap.setSshKey(sshKey);
      apiBootstrap.setUser(user);
      if (!AmUtils.isAmbariServerBelow_2_0_0(ambariServerVersion)) {
         apiBootstrap.setUserRunAs(Configuration.getString("ambari.user_run_as"));
      }
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

   public int getNeedBootstrapHostCount(List<String> addedHosts) {
      int needBootstrapHostCount = -1;

      if (addedHosts == null) {
         needBootstrapHostCount = nodes.size();

         if (isValidExternalNamenode()) {
            needBootstrapHostCount -= 1;
         }

         if (isValidExternalSecondaryNamenode()) {
            needBootstrapHostCount -= 1;
         }
      } else {
         needBootstrapHostCount = addedHosts.size();
      }

      return needBootstrapHostCount;
   }
      
   public String getAmbariServerVersion() {
      return ambariServerVersion;
   }

   public void setAmbariServerVersion(String ambariServerVersion) {
      this.ambariServerVersion = ambariServerVersion;
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

   public boolean isValidExternalNamenode() {
      return this.externalNamenode != null && !this.externalNamenode.isEmpty();
   }

   public boolean isValidExternalSecondaryNamenode() {
      return this.externalSecondaryNamenode != null && !this.externalSecondaryNamenode.isEmpty() && !this.externalSecondaryNamenode.equals(this.externalNamenode);
   }

   private boolean isNodeGenerateFromExternalNamenode(AmNodeDef node) {
      return isValidExternalNamenode() && this.externalNamenode.equals(node.getFqdn());
   }

   private boolean isNodeGenerateFromExternalSecondaryNamenode(AmNodeDef node) {
      return isValidExternalSecondaryNamenode() && this.externalSecondaryNamenode.equals(node.getFqdn());
   }

   private static HdfsVersion getDefaultHdfsVersion(String distroVersion) {
      if (distroVersion.startsWith("2")) {
         return HdfsVersion.V2;
      } else {
         return HdfsVersion.V1;
      }
   }

   @SuppressWarnings("unchecked")
   private void setRackTopologyFileName(ClusterBlueprint blueprint) {
      String rackTopologyFileName = "/etc/hadoop/conf/topology.sh";
      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         blueprint.setConfiguration(conf);
      }

      Map<String, Object> confCoreSite = (Map<String, Object>) conf.get("core-site");
      if (confCoreSite == null) {
         confCoreSite = new HashMap<String, Object>();
         conf.put("core-site", confCoreSite);
      }
      if (confCoreSite.get("net.topology.script.file.name") == null) {
         confCoreSite.put("net.topology.script.file.name", rackTopologyFileName);
      }
      if (confCoreSite.get("topology.script.file.name") == null) {
         confCoreSite.put("topology.script.file.name", rackTopologyFileName);
      }
   }

}
