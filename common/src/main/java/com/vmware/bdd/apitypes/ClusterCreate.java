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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.spectypes.HadoopDistroMap;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;

/**
 * Cluster creation spec
 */
public class ClusterCreate {
   @Expose
   private String name;
   private ClusterType type;
   private String vendor;
   private String externalHDFS;
   @Expose
   @SerializedName("groups")
   private NodeGroupCreate[] nodeGroups;
   @Expose
   private String distro;
   private String version;
   @Expose
   @SerializedName("http_proxy")
   private String httpProxy;
   @Expose
   @SerializedName("no_proxy")
   private String noProxy;
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
   //yum or ubuntu apt repos
   @Expose
   @SerializedName("distro_package_repos")
   private List<String> packageRepos;
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
      this.vendor = cluster.vendor;
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

   public String getVendor() {
      return vendor;
   }

   public void setVendor(String vendor) {
      this.vendor = vendor;
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

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getHttpProxy() {
      return httpProxy;
   }

   public void setHttpProxy(String httpProxy) {
      this.httpProxy = httpProxy;
   }

   public String getNoProxy() {
      return noProxy;
   }

   public void setNoProxy(String noProxy) {
      this.noProxy = noProxy;
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

   public List<String> getPackageRepos() {
      return packageRepos;
   }

   public void setPackageRepos(List<String> packageRepos) {
      this.packageRepos = packageRepos;
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

   public void validateTempfs(List<String> failedMsgList) {
      for (NodeGroupCreate nodeGroupCreate : getNodeGroups()) {
         StorageRead storageDef = nodeGroupCreate.getStorage();
         if (storageDef != null) {
            String storageType = storageDef.getType();
            if (storageType != null && storageType.equals(DatastoreType.TEMPFS.toString())) {//tempfs disk type
               if (nodeGroupCreate.getRoles().contains(HadoopRole.HADOOP_TASKTRACKER.toString())) {//compute node
                  PlacementPolicy placementPolicy = nodeGroupCreate.getPlacementPolicies();
                  if (placementPolicy != null) {
                     List<GroupAssociation> groupAssociations = placementPolicy.getGroupAssociations();
                     if (groupAssociations != null) {
                        GroupAssociationType associationType = groupAssociations.get(0).getType();
                        if (associationType != null && associationType == GroupAssociationType.STRICT) {
                           continue;
                        }
                     }
                  }
               }
               failedMsgList.add(Constants.TEMPFS_NOT_ALLOWED);
            }
         }
      }
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
         List<String> nodeGroupRoles = ngc.getRoles();
         if(nodeGroupRoles == null || nodeGroupRoles.isEmpty()) {
            valid = false;
            failedMsgList.add("missing role attribute for node group '" + ngc.getName() + "' ");
         } else {
            roles.addAll(ngc.getRoles());
         }
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

   /**
    * Validate nodeGroupCreates member formats and values in the ClusterCreate.
    */
   public void validateClusterCreate(List<String> failedMsgList,
         List<String> warningMsgList, final List<String> distroRoles) {
      // if hadoop2 namenode ha is enabled
      boolean namenodeHACheck = false;
      //role count
      int masterCount = 0, jobtrackerCount = 0, hbasemasterCount = 0, zookeeperCount = 0, workerCount = 0, numOfJournalNode = 0;
      //Find NodeGroupCreate array from current ClusterCreate instance.
      NodeGroupCreate[] nodeGroupCreates = getNodeGroups();
      if (nodeGroupCreates == null || nodeGroupCreates.length == 0) {
         failedMsgList.add(Constants.MULTI_INPUTS_CHECK);
         return;
      } else {
         // check external HDFS
         if (hasHDFSUrlConfigured() && !validateHDFSUrl()) {
            failedMsgList.add(new StringBuilder()
                         .append("externalHDFS=")
                         .append(getExternalHDFS()).toString());
         }

         // check placement policies
         validateNodeGroupPlacementPolicies(failedMsgList, warningMsgList);

         validateNodeGroupRoles(failedMsgList);

         // check tempfs relationship: if a compute node has strict association with a data node, its disk type
         // can be set to "TEMPFS". Otherwise, it is not allowed to use tempfs as the disk type.
         validateTempfs(failedMsgList);

         for (NodeGroupCreate nodeGroupCreate : nodeGroupCreates) {
            // check node group's instanceNum
            checkInstanceNum(nodeGroupCreate, failedMsgList);

            // check node group's roles
            checkNodeGroupRoles(nodeGroupCreate, distroRoles, failedMsgList);
            // get node group role.
            List<NodeGroupRole> groupRoles = getNodeGroupRoles(nodeGroupCreate);
            if (groupRoles != null) {
               for (NodeGroupRole role : groupRoles) {
                  switch (role) {
                  case MASTER:
                     masterCount++;
                     int numOfInstance = nodeGroupCreate.getInstanceNum();
                     if (numOfInstance >= 0 && numOfInstance != 1) {
                        if (numOfInstance != 2) { //namenode ha only support 2 nodes currently
                           collectInstanceNumInvalidateMsg(nodeGroupCreate,
                                 failedMsgList);
                        } else {
                           namenodeHACheck = true;
                        }
                     }
                     break;
                  case JOB_TRACKER:
                     jobtrackerCount++;
                     if (nodeGroupCreate.getInstanceNum() >= 0
                           && nodeGroupCreate.getInstanceNum() != 1) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_JOBTRACKER);
                     }
                     break;
                  case HBASE_MASTER:
                     hbasemasterCount++;
                     if (nodeGroupCreate.getInstanceNum() == 0) {
                        collectInstanceNumInvalidateMsg(nodeGroupCreate,
                              failedMsgList);
                     }
                     break;
                  case ZOOKEEPER:
                     zookeeperCount++;
                     if (nodeGroupCreate.getInstanceNum() > 0
                           && nodeGroupCreate.getInstanceNum() < 3) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_ZOOKEEPER);
                     } else if (nodeGroupCreate.getInstanceNum() > 0 && nodeGroupCreate.getInstanceNum() % 2 == 0) {
                        warningMsgList.add(Constants.ODD_NUM_OF_ZOOKEEPER);
                     }
                     break;
                  case JOURNAL_NODE:
                     numOfJournalNode += nodeGroupCreate.getInstanceNum();
                     if (nodeGroupCreate.getRoles().contains(HadoopRole.HADOOP_DATANODE.toString()) ||
                           nodeGroupCreate.getRoles().contains(HadoopRole.HADOOP_CLIENT_ROLE.toString())) {
                        failedMsgList.add(Constants.DATA_CLIENT_NODE_JOURNALNODE_COEXIST);
                     }
                     break;
                  case WORKER:
                     workerCount++;
                     if (nodeGroupCreate.getInstanceNum() == 0) {
                        collectInstanceNumInvalidateMsg(nodeGroupCreate,
                              failedMsgList);
                     } else if (isHAFlag(nodeGroupCreate)) {
                        warningMsgList.add(Constants.WORKER_CLIENT_HA_FLAG);
                     }

                     //check if datanode and region server are seperate
                     List<String> roles = nodeGroupCreate.getRoles();
                     if (roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE.toString()) && !roles.contains(HadoopRole.HADOOP_DATANODE.toString())) {
                        warningMsgList.add(Constants.REGISONSERVER_DATANODE_SEPERATION);
                     }
                     break;
                  case CLIENT:
                     if (isHAFlag(nodeGroupCreate)) {
                        warningMsgList.add(Constants.WORKER_CLIENT_HA_FLAG);
                     }
                     break;
                  case NONE:
                     warningMsgList.add(Constants.NOT_DEFINED_ROLE);
                     break;
                  default:
                  }
               }
            }
         }
         if (!supportedWithHdfs2()) {
            if (namenodeHACheck || masterCount > 1) {
               failedMsgList.add(Constants.CURRENT_DISTRO_CAN_NOT_SUPPORT_HDFS2);
            }
         } else if (namenodeHACheck) {
            if (numOfJournalNode >= 0 && numOfJournalNode < 3) {
               failedMsgList.add(Constants.WRONG_NUM_OF_JOURNALNODE);
            } else if (numOfJournalNode > 0 && numOfJournalNode % 2 == 0) {
               warningMsgList.add(Constants.ODD_NUM_OF_JOURNALNODE);
            }
            //check if zookeeper exists for automatic namenode ha failover
            if (zookeeperCount == 0) {
               failedMsgList.add(Constants.NAMENODE_AUTO_FAILOVER_ZOOKEEPER);
            }
         }
         if ((jobtrackerCount > 1) || (zookeeperCount > 1) || (hbasemasterCount > 1)) {
            failedMsgList.add(Constants.WRONG_NUM_OF_NODEGROUPS);
         }
         if (workerCount == 0) {
            warningMsgList.add(Constants.WRONG_NUM_OF_WORKERNODES);
         }
         if (numOfJournalNode > 0 && !namenodeHACheck) {
            failedMsgList.add(Constants.NO_NAMENODE_HA);
         }
      }
   }

   private boolean checkInstanceNum(NodeGroupCreate nodeGroup,
         List<String> failedMsgList) {
      boolean validated = true;
      if (nodeGroup.getInstanceNum() < 0) {
         validated = false;
         collectInstanceNumInvalidateMsg(nodeGroup, failedMsgList);
      }
      return validated;
   }

   private void collectInstanceNumInvalidateMsg(NodeGroupCreate nodeGroup,
         List<String> failedMsgList) {
      failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
            .append(".").append("instanceNum=")
            .append(nodeGroup.getInstanceNum()).toString());
   }

   private boolean checkNodeGroupRoles(NodeGroupCreate nodeGroup,
         List<String> distroRoles, List<String> failedMsgList) {
      List<String> roles = nodeGroup.getRoles();
      boolean validated = true;
      StringBuilder rolesMsg = new StringBuilder();
      if (roles != null) {
         for (String role : roles) {
            if (!distroRoles.contains(role)) {
               validated = false;
               rolesMsg.append(",").append(role);
            }
         }
      }
      if (!validated) {
         rolesMsg.replace(0, 1, "");
         failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
               .append(".").append("roles=").append("\"")
               .append(rolesMsg.toString()).append("\"").toString());
      }
      return validated;
   }

   //define role of the node group .
   private enum NodeGroupRole {
      MASTER, JOB_TRACKER, WORKER, CLIENT, HBASE_MASTER, ZOOKEEPER, JOURNAL_NODE, NONE
   }

   private List<NodeGroupRole> getNodeGroupRoles(NodeGroupCreate nodeGroupCreate) {
      List<NodeGroupRole> groupRoles = new ArrayList<NodeGroupRole>();
      //Find roles list from current  NodeGroupCreate instance.
      List<String> roles = nodeGroupCreate.getRoles();
      for (NodeGroupRole role : NodeGroupRole.values()) {
         if (roles !=null && matchRole(role, roles)) {
            groupRoles.add(role);
         }
      }
      if (groupRoles.size() == 0) {
         groupRoles.add(NodeGroupRole.NONE);
      }
      return groupRoles;
   }

   private boolean isHAFlag(NodeGroupCreate nodeGroupCreate) {
      return !CommonUtil.isBlank(nodeGroupCreate.getHaFlag())
            && !nodeGroupCreate.getHaFlag().equalsIgnoreCase("off");
   }

   /**
    * Check the roles was introduced, whether matching with system's specialize
    * role.
    */
   private boolean matchRole(NodeGroupRole role, List<String> roles) {
      switch (role) {
      case MASTER:
         if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      case JOB_TRACKER:
         if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      case HBASE_MASTER:
         if (roles.contains(HadoopRole.HBASE_MASTER_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      case ZOOKEEPER:
         if (roles.contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      case JOURNAL_NODE:
         if (roles.contains(HadoopRole.HADOOP_JOURNALNODE_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      case WORKER:
         if (roles.contains(HadoopRole.HADOOP_DATANODE.toString())
               || roles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())
               || roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      case CLIENT:
         if (roles.contains(HadoopRole.HADOOP_CLIENT_ROLE.toString())
               || roles.contains(HadoopRole.HIVE_ROLE.toString())
               || roles.contains(HadoopRole.HIVE_SERVER_ROLE.toString())
               || roles.contains(HadoopRole.PIG_ROLE.toString())
               || roles.contains(HadoopRole.HBASE_CLIENT_ROLE.toString())) {
            return true;
         } else {
            return false;
         }
      }
      return false;
   }

   // For HDFS2, at present, serengeti only support cdh4 of Cloudera.
   public boolean supportedWithHdfs2() {
      if (this.getVendor().equalsIgnoreCase(Constants.CLOUDERA_VENDOR)) {
         Pattern pattern = Pattern.compile(Constants.CDH4_1_PATTERN);
         if (pattern.matcher(this.getVersion()).matches()) {
            return true;
         }
      }
      return false;
   }

   public String getDefaultDistroName(DistroRead[] distros) {
      if (distros != null) {
         for (DistroRead distro : distros) {
            if (distro.getVendor().equalsIgnoreCase(Constants.DEFAULT_VENDOR)) {
               return distro.getName();
            }
         }
      }
      return null;
   }

}
