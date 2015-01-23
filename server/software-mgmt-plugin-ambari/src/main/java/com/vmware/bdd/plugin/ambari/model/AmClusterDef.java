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
import java.util.Set;

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
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

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

   private boolean isComputeOnly = false;

   private String externalNamenode;

   private String externalSecondaryNamenode;

   private Set<String> externalDatanodes;

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

      this.nodes = new ArrayList<AmNodeDef>();
      HdfsVersion hdfs = getDefaultHdfsVersion(this.version);
      if (blueprint.hasTopologyPolicy()) {
         setRackTopologyFileName(blueprint);
      }
      //set service user to configuration
      //Todo(qjin): better to not change the blueprint and move this to the toAmConfigurations
      updateServiceUserConfigInBlueprint(blueprint);
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
      if (blueprint.getExternalNamenode() != null && blueprint.getExternalDatanodes() != null) {
         this.isComputeOnly = true;

         this.externalNamenode = blueprint.getExternalNamenode();
         this.externalSecondaryNamenode = blueprint.getExternalSecondaryNamenode();
         this.externalDatanodes = blueprint.getExternalDatanodes();

         AmNodeDef namenodeDef = new AmNodeDef();
         namenodeDef.setName(name+"-external-namenode");
         namenodeDef.setFqdn(externalNamenode);
         List<String> namenodeRoles = new ArrayList<String>();
         namenodeRoles.add("NAMENODE");
         if (externalSecondaryNamenode == null || externalNamenode.equals(externalSecondaryNamenode)) {
            namenodeRoles.add("SECONDARY_NAMENODE");
         }
         namenodeDef.setComponents(namenodeRoles);
         this.nodes.add(namenodeDef);

         if (externalSecondaryNamenode !=  null && !externalNamenode.equals(externalSecondaryNamenode)) {
            AmNodeDef secondaryNamenodeDef = new AmNodeDef();
            secondaryNamenodeDef.setName(name+"-external-secondaryNamenode");
            secondaryNamenodeDef.setFqdn(externalSecondaryNamenode);
            List<String> secondaryNamenodeRoles = new ArrayList<String>();
            secondaryNamenodeRoles.add("SECONDARY_NAMENODE");
            secondaryNamenodeDef.setComponents(secondaryNamenodeRoles);
            this.nodes.add(secondaryNamenodeDef);
         }

         int datanodeIndex = 0;
         for (String externalDatanode : externalDatanodes) {
            AmNodeDef datanodeDef = new AmNodeDef();
            datanodeDef.setName(name + "-external-datanode-" + datanodeIndex);
            datanodeDef.setFqdn(externalDatanode);
            List<String> datanodeDefRoles = new ArrayList<String>();
            datanodeDefRoles.add("DATANODE");
            datanodeDef.setComponents(datanodeDefRoles);
            this.nodes.add(datanodeDef);
            datanodeIndex ++;
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

   public Set<String> getExternalDatanodes() {
      return externalDatanodes;
   }

   public void setExternalDatanodes(Set<String> externalDatanodes) {
      this.externalDatanodes = externalDatanodes;
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

   private void updateServiceUserConfigInBlueprint(ClusterBlueprint blueprint) {
      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         return;
      }
      Map<String, Map<String, String>> serviceUserConfigs = (Map<String, Map<String, String>>)
            conf.get(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE);
      if (MapUtils.isEmpty(serviceUserConfigs)) {
         return;
      }
      //Todo(qjin:) For hdfs and other services, if need to modify other configs related with servcie user, also need to
      //handle seperately, that config changes should be reflected in the blueprint
      for (Map.Entry<String, Map<String, String>> serviceUserConfig: serviceUserConfigs.entrySet()) {
         String serviceUser = serviceUserConfig.getValue().get(UserMgmtConstants.SERVICE_USER_NAME);
         if (!StringUtils.isBlank(serviceUser)) {
            String serviceUserParentConfigName = serviceUserConfig.getKey().toLowerCase() + "-env";
            String serviceUserConfigName = serviceUserConfig.getKey().toLowerCase() + "_user";
            Map<String, String> serviceConfig = (Map<String, String>)conf.get(serviceUserParentConfigName);
            if (serviceConfig == null) {
               serviceConfig = new HashMap<>();
            }
            serviceConfig.put(serviceUserConfigName, serviceUser);
            conf.put(serviceUserParentConfigName, serviceConfig);
         }
      }
      conf.remove(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE);
   }

}
