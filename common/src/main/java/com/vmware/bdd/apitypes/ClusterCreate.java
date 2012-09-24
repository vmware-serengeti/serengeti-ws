/***************************************************************************
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.apitypes;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.spectypes.HadoopDistroMap;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.spectypes.VcCluster;

/**
 * Cluster creation spec
 */
public class ClusterCreate {
   @Expose
   private String name;
   private ClusterType type;
   private String externalHDFS;
   @Expose
   @SerializedName("groups")
   private NodeGroupCreate[] nodeGroups;
   @Expose
   private String distro;
   private List<String> rpNames;
   @Expose
   @SerializedName("vc_clusters")
   private List<VcCluster> vcClusters;
   @Expose
   @SerializedName("template_id")
   private String templateId;
   @Expose
   @SerializedName("deploy_policy")
   private String deployPolicy;
   private List<String> dsNames;
   private String networkName;
   @Expose
   private List<NetworkAdd> networking;
   @Expose
   @SerializedName("distro_map")
   private HadoopDistroMap distroMap;
   @Expose
   @SerializedName("rack_topology_policy")
   private TopologyType topologyPolicy;
   @Expose
   @SerializedName("rack_topology")
   private Map<String, String> hostToRackMap;
   @Expose
   @SerializedName("vc_shared_datastore_pattern")
   private Set<String> sharedPattern;
   @Expose
   @SerializedName("vc_local_datastore_pattern")
   private Set<String> localPattern;
   @Expose
   @SerializedName("cluster_configuration")
   private Map<String, Object> configuration;
   private Boolean validateConfig = true;

   public ClusterCreate() {
   }

   public ClusterCreate(ClusterCreate cluster) {
      this.deployPolicy = cluster.deployPolicy;
      this.distro = cluster.distro;
      this.name = cluster.name;
      this.type = cluster.type;
      this.externalHDFS = cluster.externalHDFS;
      this.networkName = cluster.networkName;
      this.nodeGroups = cluster.nodeGroups;
      this.rpNames = cluster.rpNames;
      this.templateId = cluster.templateId;
      this.vcClusters = cluster.vcClusters;
      this.networking = cluster.networking;
      this.configuration = cluster.configuration;
      this.validateConfig = cluster.validateConfig;
      this.topologyPolicy = cluster.topologyPolicy;
      this.hostToRackMap = cluster.hostToRackMap;
   }

   public Map<String, Object> getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Map<String, Object> configuration) {
      if (configuration == null) {
         configuration = new HashMap<String, Object>();
      }
      this.configuration = configuration;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public ClusterType getType() {
      return type;
   }

   public void setType(ClusterType type) {
      this.type = type;
   }

   public void setExternalHDFS(String externalHDFS) {
      this.externalHDFS = externalHDFS;
   }

   public String getExternalHDFS() {
      return externalHDFS;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public List<String> getRpNames() {
      return rpNames;
   }

   public void setRpNames(List<String> rpNames) {
      this.rpNames = rpNames;
   }

   public String getNetworkName() {
      return networkName;
   }

   public void setNetworkName(String networkName) {
      this.networkName = networkName;
   }

   public NodeGroupCreate[] getNodeGroups() {
      return nodeGroups;
   }

   public void setNodeGroups(NodeGroupCreate[] nodeGroups) {
      this.nodeGroups = nodeGroups;
   }

   public List<String> getDsNames() {
      return dsNames;
   }

   public void setDsNames(List<String> dsNames) {
      this.dsNames = dsNames;
   }

   public List<VcCluster> getVcClusters() {
      return vcClusters;
   }

   public void setVcClusters(List<VcCluster> vcClusters) {
      this.vcClusters = vcClusters;
   }

   public String getTemplateId() {
      return templateId;
   }

   public void setTemplateId(String templateId) {
      this.templateId = templateId;
   }

   public String getDeployPolicy() {
      return deployPolicy;
   }

   public void setDeployPolicy(String deployPolicy) {
      this.deployPolicy = deployPolicy;
   }

   public List<NetworkAdd> getNetworking() {
      return networking;
   }

   public void setNetworking(List<NetworkAdd> networking) {
      this.networking = networking;
   }

   public HadoopDistroMap getDistroMap() {
      return distroMap;
   }

   public void setDistroMap(HadoopDistroMap distroMap) {
      this.distroMap = distroMap;
   }

   public TopologyType getTopologyPolicy() {
      return topologyPolicy;
   }

   public void setTopologyPolicy(TopologyType topologyPolicy) {
      this.topologyPolicy = topologyPolicy;
   }

   public Map<String, String> getHostToRackMap() {
      return hostToRackMap;
   }

   public void setHostToRackMap(Map<String, String> hostToRackMap) {
      this.hostToRackMap = hostToRackMap;
   }

   public Set<String> getSharedPattern() {
      return sharedPattern;
   }

   public void setSharedPattern(Set<String> sharedPattern) {
      this.sharedPattern = sharedPattern;
   }

   public Set<String> getLocalPattern() {
      return localPattern;
   }

   public void setLocalPattern(Set<String> localPattern) {
      this.localPattern = localPattern;
   }

   public Boolean isValidateConfig() {
      return validateConfig;
   }

   public void setValidateConfig(Boolean validateConfig) {
      this.validateConfig = validateConfig;
   }

   public boolean validateNodeGroupPlacementPolicies(List<String> failedMsgList,
         List<String> warningMsgList) {
      boolean valid = true;

      Map<String, NodeGroupCreate> allGroups = new TreeMap<String, NodeGroupCreate>();

      if (getNodeGroups() == null) {
         return valid;
      }

      for (NodeGroupCreate nodeGroupCreate : getNodeGroups()) {
         allGroups.put(nodeGroupCreate.getName(), nodeGroupCreate);
      }

      for (NodeGroupCreate ngc : getNodeGroups()) {
         if (!ngc.validatePlacementPolicies(this, allGroups, failedMsgList,
               warningMsgList)) {
            valid = false;
         }
      }

      return valid;
   }

   /*
    * Validate 2 cases. Case 1: compute node group with external hdfs node group.
    * Case 2: The dependency check of HDFS, MapReduce, HBase, Zookeeper, Hadoop 
    * Client(Pig, Hive, Hadoop Client), and HBase Client Combinations. The rules are below:
    * - HDFS includes roles of "haddop_namenode" and "hadoop_datanode";
    * - MapReduce includes roles of "haddop_jobtracker" and "hadoop_takstracker";
    * - HBase includes roles of "hbase_master" and "hbase_regionserver;
    * - Zookeeper includes a single role of "zookeeper";
    * - Hadoop Client includes roles of "hadoop_client";
    * - HBase client includes roles of "hbase_client";
    * - Pig includes roles of "pig";
    * - Hive includes roles of "hive";
    * - Hive Server includes roles of "hive_server";
    * - MapReduce depends on HDFS, HBase depends on HDFS and Zookeeper;
    * - Pig, Hive, Hive Server depends on MapReduce, HBase Client depends on HBase;
    * - Hadoop Client depends on HDFS.   
    */
   public boolean validateNodeGroupRoles(List<String> failedMsgList) {
      boolean valid = true;
      Set<String> roles = new HashSet<String>();
      for (NodeGroupCreate ngc : getNodeGroups()) {
         roles.addAll(ngc.getRoles());
      }

      if (validateHDFSUrl()) {
         if (getNodeGroups() == null) {
            valid = false;
            failedMsgList.add("missing jobtracker/tasktracker role");
         } else {
            if (roles.contains("hadoop_namenode") || roles.contains("hadoop_datanode")) {
               valid = false;
               failedMsgList.add("redundant namenode/datanode role");
            }
            if (!roles.contains("hadoop_jobtracker")
                  || !roles.contains("hadoop_tasktracker")) {
               valid = false;
               failedMsgList.add("missing jobtracker/tasktracker role");
            }
         }
      } else { //case 2
         // get involved service types of the spec file
         EnumSet<ServiceType> serviceTypes = EnumSet.noneOf(ServiceType.class);
         for (ServiceType service : ServiceType.values()) {
            //identify partially match
            int matched = 0;
            for (HadoopRole role: service.getRoles()) {
               if (roles.contains(role.toString())) {
                  matched++;
               }
            }
            if (matched == service.getRoles().size()) {
               serviceTypes.add(service);
            } else if (matched != 0) {
               failedMsgList.add("some roles in " + service + " " + service.getRoles() + " cannot be found in the spec file");
               valid = false;
            }
         }

         //validate the relationships of services
         if (valid == true && !serviceTypes.isEmpty()) {
            for (ServiceType service: serviceTypes) {
               EnumSet<ServiceType> dependency = service.depend();
               if (dependency != null && !serviceTypes.containsAll(dependency)) {
                  failedMsgList.add("some dependent services " + dependency + " " + service + " relies on cannot be found in the spec file");
                  valid = false;
               }
            }
         }
      }
      
      return valid;
   }

   public boolean hasHDFSUrlConfigured() {
      return getExternalHDFS() != null && !getExternalHDFS().isEmpty();
   }

   public boolean validateHDFSUrl() {
      if (hasHDFSUrlConfigured()) {
         try {
            URI uri = new URI(getExternalHDFS());
            if (!"hdfs".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
               return false;
            }
            return true;
         } catch (Exception ex) {
            ex.printStackTrace();
            return false;
         }
      }
      return false;
   }
}
