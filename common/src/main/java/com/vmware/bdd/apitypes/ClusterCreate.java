/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

import java.io.Serializable;
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
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.NodeGroup.PlacementPolicy.GroupAssociation.GroupAssociationType;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.spectypes.HadoopDistroMap;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ConfigInfo;
import com.vmware.bdd.utils.Constants;

/**
 * Cluster creation spec
 */
public class ClusterCreate implements Serializable {
   private static final long serialVersionUID = -7460690272330642247L;

   @Expose
   private String name;
   private ClusterType type;
   private String externalHDFS;
   @Expose
   @SerializedName("groups")
   private NodeGroupCreate[] nodeGroups;
   @Expose
   private String distro;
   @Expose
   @SerializedName("distro_vendor")
   private String distroVendor;
   @Expose
   @SerializedName("distro_version")
   private String distroVersion;
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

   private Map<NetTrafficType, List<String>> networkConfig;

   @Expose
   private List<NetworkAdd> networkings;

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
   private Set<String> sharedDatastorePattern;
   @Expose
   @SerializedName("vc_local_datastore_pattern")
   private Set<String> localDatastorePattern;

   // datastore patterns for system/swap disks
   //   private Set<String> imagestorePattern;

   @Expose
   @SerializedName("cluster_configuration")
   private Map<String, Object> configuration;

   private Boolean validateConfig = true;

   private Boolean specFile = false;

   private String password;

   public ClusterCreate() {
   }

   public ClusterCreate(ClusterCreate cluster) {
      this.deployPolicy = cluster.deployPolicy;
      this.distro = cluster.distro;
      this.name = cluster.name;
      this.type = cluster.type;
      this.distroVendor = cluster.distroVendor;
      this.distroVersion = cluster.distroVersion;
      this.externalHDFS = cluster.externalHDFS;
      this.networkConfig = cluster.networkConfig;
      this.networkings = cluster.networkings;
      this.nodeGroups = cluster.nodeGroups;
      this.rpNames = cluster.rpNames;
      this.templateId = cluster.templateId;
      this.vcClusters = cluster.vcClusters;
      this.configuration = cluster.configuration;
      this.validateConfig = cluster.validateConfig;
      this.topologyPolicy = cluster.topologyPolicy;
      this.hostToRackMap = cluster.hostToRackMap;
      this.password = cluster.password;
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

   public String getDistroVendor() {
      return distroVendor;
   }

   public void setDistroVendor(String distroVendor) {
      this.distroVendor = distroVendor;
   }

   public String getDistroVersion() {
      return distroVersion;
   }

   public void setDistroVersion(String distroVersion) {
      this.distroVersion = distroVersion;
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

   public List<String> getNetworkNames() {
      List<String> networks = new ArrayList<String>();
      if (getNetworkConfig() != null && !getNetworkConfig().isEmpty()) {
         for (List<String> nets : getNetworkConfig().values()) {
            for (String netName : nets) {
               networks.add(netName);
            }
         }
      }
      return networks;
   }

   public Map<NetTrafficType, List<String>> getNetworkConfig() {
      return networkConfig;
   }

   public void setNetworkConfig(Map<NetTrafficType, List<String>> networkConfig) {
      this.networkConfig = networkConfig;
   }

   public List<NetworkAdd> getNetworkings() {
      return networkings;
   }

   public void setNetworkings(List<NetworkAdd> networkings) {
      this.networkings = networkings;
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

   public Set<String> getSharedDatastorePattern() {
      return sharedDatastorePattern;
   }

   public void setSharedDatastorePattern(Set<String> sharedDatastorePattern) {
      this.sharedDatastorePattern = sharedDatastorePattern;
   }

   public Set<String> getLocalDatastorePattern() {
      return localDatastorePattern;
   }

   public void setLocalDatastorePattern(Set<String> localDatastorePattern) {
      this.localDatastorePattern = localDatastorePattern;
   }

   public Boolean isValidateConfig() {
      return validateConfig;
   }

   public void setValidateConfig(Boolean validateConfig) {
      this.validateConfig = validateConfig;
   }

   /**
    * @return the deployPolicy
    */
   public String getDeployPolicy() {
      return deployPolicy;
   }

   /**
    * @param deployPolicy the deployPolicy to set
    */
   public void setDeployPolicy(String deployPolicy) {
      this.deployPolicy = deployPolicy;
   }

   public Boolean isSpecFile() {
      return specFile;
   }

   public void setSpecFile(Boolean specFile) {
      this.specFile = specFile;
   }

   public boolean validateNodeGroupPlacementPolicies(
         List<String> failedMsgList, List<String> warningMsgList) {
      boolean valid = true;

      Map<String, NodeGroupCreate> allGroups =
            new TreeMap<String, NodeGroupCreate>();

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

   public void validateStorageType(List<String> failedMsgList) {
      for (NodeGroupCreate nodeGroupCreate : getNodeGroups()) {
         StorageRead storageDef = nodeGroupCreate.getStorage();
         if (storageDef != null) {
            String storageType = storageDef.getType();

            if (storageType != null) {
               storageType = storageType.toUpperCase();
               //only support storage type of TEMPFS/LOCAL/SHARED
               if (!storageType.equals(DatastoreType.TEMPFS.toString())
                     && !storageType.equals(DatastoreType.LOCAL.toString())
                     && !storageType.equals(DatastoreType.SHARED.toString())) {
                  failedMsgList.add("Invalid storage type " + storageType
                        + ". " + Constants.STORAGE_TYPE_ALLOWED);
               } else if (storageType.equals(DatastoreType.TEMPFS.toString())) {//tempfs disk type
                  if (nodeGroupCreate.getRoles().contains(
                        HadoopRole.HADOOP_TASKTRACKER.toString())) {//compute node
                     PlacementPolicy placementPolicy =
                           nodeGroupCreate.getPlacementPolicies();
                     if (placementPolicy != null) {
                        List<GroupAssociation> groupAssociations =
                              placementPolicy.getGroupAssociations();
                        if (groupAssociations != null) {
                           GroupAssociationType associationType =
                                 groupAssociations.get(0).getType();
                           if (associationType != null
                                 && associationType == GroupAssociationType.STRICT) {
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
   }

   private void validateSwapRatio(NodeGroupCreate[] nodeGroups,
         List<String> failedMsgList) {
      boolean validated = true;
      StringBuilder invalidNodeGroupNames = new StringBuilder();
      for (NodeGroupCreate nodeGroup : nodeGroups) {
         if (nodeGroup.getSwapRatio() <= 0) {
            validated = false;
            invalidNodeGroupNames.append(nodeGroup.getName()).append(",");
         }
      }
      if (!validated) {
         StringBuilder errorMsgBuff = new StringBuilder();
         invalidNodeGroupNames.delete(invalidNodeGroupNames.length() - 1,
               invalidNodeGroupNames.length());
         failedMsgList.add(errorMsgBuff
               .append("The 'swapRatio' must be greater than 0 in group ")
               .append(invalidNodeGroupNames.toString())
               .append(".").toString());
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
      if (getNodeGroups() == null) {
         return false;
      }

      for (NodeGroupCreate ngc : getNodeGroups()) {
         List<String> nodeGroupRoles = ngc.getRoles();
         if (nodeGroupRoles == null || nodeGroupRoles.isEmpty()) {
            valid = false;
            failedMsgList.add("Missing role attribute for node group "
                  + ngc.getName() + ".");
         } else {
            roles.addAll(ngc.getRoles());
         }
      }

      if (validateHDFSUrl()) {
         if (getNodeGroups() == null) {
            valid = false;
            failedMsgList.add("Missing JobTracker or TaskTracker role.");
         } else {
            if (roles.contains("hadoop_namenode")
                  || roles.contains("hadoop_datanode")) {
               valid = false;
               failedMsgList.add("Duplicate NameNode or DataNode role.");
            }
            if (!roles.contains("hadoop_jobtracker")
                  && !roles.contains("hadoop_resourcemanager")) {
               valid = false;
               failedMsgList.add("Missing JobTracker or ResourceManager role.");
            }
            if (!roles.contains("hadoop_tasktracker")
                  && !roles.contains("hadoop_nodemanager")) {
               valid = false;
               failedMsgList.add("Missing TaskTracker or NodeManager role.");
            }
         }
      } else { //case 2
         // get involved service types of the spec file
         EnumSet<ServiceType> serviceTypes = EnumSet.noneOf(ServiceType.class);
         for (ServiceType service : ServiceType.values()) {
            //identify partially match
            int matched = 0;
            for (HadoopRole role : service.getRoles()) {
               if (roles.contains(role.toString())) {
                  matched++;
               }
            }
            if (matched == service.getRoles().size()) {
               serviceTypes.add(service);
            } else if (matched != 0) {
               failedMsgList
                     .add("Cannot find one or more roles in " + service + " " + service.getRoles()
                           + " in the cluster specification file.");
               valid = false;
            }
         }

         boolean isYarn = serviceTypes.contains(ServiceType.YARN);
         if (isYarn && serviceTypes.contains(ServiceType.MAPRED)) {
            failedMsgList.add("You cannot set " + ServiceType.MAPRED + " "
                  + ServiceType.MAPRED.getRoles() + " and " + ServiceType.YARN
                  + " " + ServiceType.YARN.getRoles()
                  + " \nat the same time.");
            valid = false;
         }
         //validate the relationships of services
         if (valid == true && !serviceTypes.isEmpty()) {
            for (ServiceType service : serviceTypes) {
               EnumSet<ServiceType> dependency = service.depend(isYarn);
               if (dependency != null && !serviceTypes.containsAll(dependency)) {
                  failedMsgList.add("Some dependent services " + dependency
                        + " " + service
                        + " relies on cannot be found in the spec file.");
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
            if (!"hdfs".equalsIgnoreCase(uri.getScheme())
                  || uri.getHost() == null) {
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

   public int totalInstances() {
      int num = 0;

      for (int i = 0; i < nodeGroups.length; i++) {
         num += nodeGroups[i].getInstanceNum();
      }

      return num;
   }

   public NodeGroupCreate getNodeGroup(String name) {
      AuAssert.check(name != null);

      for (NodeGroupCreate nodeGroup : this.nodeGroups) {
         if (nodeGroup.getName().equals(name)) {
            return nodeGroup;
         }
      }
      return null;
   }

   /**
    * Check if any compute only node group exists.
    */
   public boolean containsComputeOnlyNodeGroups() {
      for (NodeGroupCreate nodeGroup : this.getNodeGroups()) {
         if (CommonUtil.isComputeOnly(nodeGroup.getRoles(), distroVendor)) {
            return true;
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
      int masterCount = 0, jobtrackerCount = 0, resourcemanagerCount = 0, hbasemasterCount =
            0, zookeeperCount = 0, workerCount = 0, numOfJournalNode = 0;
      boolean appendWarningStr = false;
      if (warningMsgList != null && warningMsgList.isEmpty()) {
         appendWarningStr = true;
      }

      //Find NodeGroupCreate array from current ClusterCreate instance.
      NodeGroupCreate[] nodeGroupCreates = getNodeGroups();
      AuAssert.check(nodeGroupCreates != null && nodeGroupCreates.length > 0);
      // check external HDFS
      if (hasHDFSUrlConfigured() && !validateHDFSUrl()) {
         failedMsgList.add(new StringBuilder().append("externalHDFS=")
               .append(getExternalHDFS()).toString());
      }

      // check placement policies
      validateNodeGroupPlacementPolicies(failedMsgList, warningMsgList);

      validateNodeGroupRoles(failedMsgList);

      // check supported storage type: LOCAL/SHARED/TEMPFS For tempfs relationship: if a compute node has 
      // strict association with a data node, its disk type can be set to "TEMPFS". Otherwise, it is not 
      // allowed to use tempfs as the disk type.
      validateStorageType(failedMsgList);

      // check node group's swapRatio
      validateSwapRatio(nodeGroupCreates, failedMsgList);

      for (NodeGroupCreate nodeGroupCreate : nodeGroupCreates) {
         // check node group's instanceNum
         checkInstanceNum(nodeGroupCreate, failedMsgList);
         // check CPU number and memory capacity
         checkCPUAndMemory(nodeGroupCreate, failedMsgList, warningMsgList);
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
               case RESOURCEMANAGER:
                  resourcemanagerCount++;
                  if (nodeGroupCreate.getInstanceNum() >= 0
                        && nodeGroupCreate.getInstanceNum() != 1) {
                     failedMsgList.add(Constants.WRONG_NUM_OF_RESOURCEMANAGER);
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
                  } else if (nodeGroupCreate.getInstanceNum() > 0
                        && nodeGroupCreate.getInstanceNum() % 2 == 0) {
                     warningMsgList.add(Constants.ODD_NUM_OF_ZOOKEEPER);
                  }
                  break;
               case JOURNAL_NODE:
                  numOfJournalNode += nodeGroupCreate.getInstanceNum();
                  if (nodeGroupCreate.getRoles().contains(
                        HadoopRole.HADOOP_DATANODE.toString())
                        || nodeGroupCreate.getRoles().contains(
                              HadoopRole.HADOOP_CLIENT_ROLE.toString())) {
                     failedMsgList
                           .add(Constants.DATA_CLIENT_NODE_JOURNALNODE_COEXIST);
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
                  if (roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE
                        .toString())
                        && !roles.contains(HadoopRole.HADOOP_DATANODE
                              .toString())) {
                     warningMsgList
                           .add(Constants.REGISONSERVER_DATANODE_SEPERATION);
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
      if ((jobtrackerCount > 1) || (resourcemanagerCount > 1)
            || (zookeeperCount > 1) || (hbasemasterCount > 1)) {
         failedMsgList.add(Constants.WRONG_NUM_OF_NODEGROUPS);
      }
      if (workerCount == 0) {
         warningMsgList.add(Constants.WRONG_NUM_OF_WORKERNODES);
      }
      if (numOfJournalNode > 0 && !namenodeHACheck) {
         failedMsgList.add(Constants.NO_NAMENODE_HA);
      }
      if (!warningMsgList.isEmpty() && appendWarningStr) {
         warningMsgList.set(0, "Warning: " + warningMsgList.get(0));
      }
   }

   /**
    * Validate nodeGroupCreates member formats and values in the ClusterCreate of Mapr.
    */
   public void validateClusterCreateOfMapr(List<String> failedMsgList,
         final List<String> distroRoles) {
      NodeGroupCreate[] nodeGroupCreates = getNodeGroups();
      for (NodeGroupCreate nodeGroupCreate : nodeGroupCreates) {
         checkNodeGroupRoles(nodeGroupCreate, distroRoles, failedMsgList);
      }
   }

   private void checkCPUAndMemory(NodeGroupCreate nodeGroup,
         List<String> failedMsgList, List<String> warningMsgList) {
      Integer cpuNum = nodeGroup.getCpuNum();
      Integer memCap = nodeGroup.getMemCapacityMB();
      if (cpuNum != null && cpuNum <= 0) {
         failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
               .append(".").append("cpuNum=").append(cpuNum).append(".")
               .toString());
      }
      if (memCap != null) {
         if (memCap <= 0) {
            failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
                  .append(".").append("memCapacityMB=").append(memCap)
                  .append(".").toString());
         } else {
            // make VM memory value devisible by 4
            makeVmMemoryDivisibleBy4(nodeGroup, warningMsgList);
         }
      }
   }

   private void makeVmMemoryDivisibleBy4(NodeGroupCreate nodeGroup,
         List<String> warningMsgList) {
      int memoryCap = nodeGroup.getMemCapacityMB();
      if (memoryCap > 0) {
         //VM's memory must be divisible by 4, otherwise VM can not be started
         long converted = CommonUtil.makeVmMemoryDivisibleBy4(memoryCap);
         if (converted < memoryCap) {
            nodeGroup.setMemCapacityMB((int) converted);
            warningMsgList.add(Constants.CONVERTED_MEMORY_DIVISIBLE_BY_4
                  + "For group " + nodeGroup.getName() + ", " + converted
                  + " replaces " + memoryCap + " for the memCapacityMB value.");
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
            .append(nodeGroup.getInstanceNum()).append(".").toString());
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
               .append(".").append("roles=")
               .append(rolesMsg.toString()).append(".").toString());
      }
      return validated;
   }



   //define role of the node group .
   private enum NodeGroupRole {
      MASTER, JOB_TRACKER, RESOURCEMANAGER, WORKER, CLIENT, HBASE_MASTER, ZOOKEEPER, JOURNAL_NODE, NONE
   }

   private List<NodeGroupRole> getNodeGroupRoles(NodeGroupCreate nodeGroupCreate) {
      List<NodeGroupRole> groupRoles = new ArrayList<NodeGroupRole>();
      //Find roles list from current  NodeGroupCreate instance.
      List<String> roles = nodeGroupCreate.getRoles();
      for (NodeGroupRole role : NodeGroupRole.values()) {
         if (roles != null && matchRole(role, roles)) {
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
         case RESOURCEMANAGER:
            if (roles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString())) {
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
                  || roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE.toString())
                  || roles.contains(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString())) {
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

   // For HDFS2, at present, serengeti support cdh4 of Cloudera, and PHD.
   public boolean supportedWithHdfs2() {
      if (this.getDistroVendor().equalsIgnoreCase(Constants.CDH_VENDOR)) {
         Pattern pattern = Pattern.compile(Constants.CDH4_PATTERN);
         if (pattern.matcher(this.getDistroVersion()).matches()) {
            return true;
         }
      }
      if (this.getDistroVendor().equalsIgnoreCase(Constants.PHD_VENDOR)) {
         return true;
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

   public void validateCDHVersion(List<String> warningMsgList) {
      // If current distro's version is greater than cdh4.2.1, the FQDN must be configured.
      if (this.getDistroVendor().equalsIgnoreCase(Constants.CDH_VENDOR)) {
         Pattern pattern = Pattern.compile(Constants.CDH4_PATTERN);
         if (!pattern.matcher(this.getDistroVersion()).matches()) {
            return;
         }
         if (compare(this.getDistroVersion(), "4.2.1") > 0) {
            warningMsgList.add(Constants.MUST_CONFIGURE_FQDN);
         }
      }
   }

   private int compare(String srcVersion, String destVersion) {
      String[] srcVersionArray = srcVersion.split("\\.");
      String[] destVersionArray = destVersion.split("\\.");
      for (int i = 0; i < srcVersionArray.length; i++) {
         if (i >= destVersionArray.length) {
            return compare(destVersionArray, srcVersionArray, 1);
         }
         if (Integer.parseInt(srcVersionArray[i]) > Integer
               .parseInt(destVersionArray[i])) {
            return 1;
         } else if (Integer.parseInt(srcVersionArray[i]) < Integer
               .parseInt(destVersionArray[i])) {
            return -1;
         }
      }
      if (destVersionArray.length > srcVersionArray.length) {
         return compare(srcVersionArray, destVersionArray, -1);
      }
      return 0;
   }

   private int compare(String[] srcVersionArray, String[] destVersionArray, int type) {
      for (int j = srcVersionArray.length; j < destVersionArray.length; j++) {
         if (Integer.parseInt(destVersionArray[j]) > 0) {
            return type;
         }
      }
      return 0;
   }

   public void validateNodeGroupNames() {
      if (nodeGroups != null && nodeGroups.length > 0) {
         StringBuffer invalidNodeGroupNames = new StringBuffer();
         for (NodeGroupCreate nodeGroup : nodeGroups) {
            if (CommonUtil.isBlank(nodeGroup.getName())
                  || !CommonUtil.validateNodeGroupName(nodeGroup.getName())) {
               invalidNodeGroupNames.append("'").append(nodeGroup.getName())
                     .append("'").append(",");
            }
         }
         if (invalidNodeGroupNames.length() > 0) {
            invalidNodeGroupNames.delete(invalidNodeGroupNames.length() - 1,
                  invalidNodeGroupNames.length());
            throw ClusterConfigException
                  .NODE_GROUP_NAME_IS_INVALID(invalidNodeGroupNames.toString());
         }
      } else {
         throw ClusterConfigException.NODE_GROUP_NOT_EXISTING();
      }
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public void verifyClusterNameLength() {
      final int MAX_VC_OBJECT_NAME_LENGTH = 80;
      final int MAX_VM_INDEX_LENGTH = 4;
      final int VM_LINK_SYMBOL_LENGTH = 2;
      final int RP_LINK_SYMBOL_LENGTH = 1;
      int clusterNameLength = getName().length();
      int maxNodeGroupNameLength = 0;
      if (getNodeGroups() == null || getNodeGroups().length == 0) {
         maxNodeGroupNameLength = 6;
      } else {
         int nodeGroupNameLength = 0;
         for (NodeGroupCreate nodeGroup : this.getNodeGroups()) {
            nodeGroupNameLength = nodeGroup.getName().length();
            if (maxNodeGroupNameLength < nodeGroupNameLength) {
               maxNodeGroupNameLength = nodeGroupNameLength;
            }
         }
      }
      int rpNeedLength =
            MAX_VC_OBJECT_NAME_LENGTH - ConfigInfo.getSerengetiUUID().length()
                  - RP_LINK_SYMBOL_LENGTH;
      if (rpNeedLength <= 0) {
         throw ClusterConfigException.UUID_TOO_LONG(MAX_VC_OBJECT_NAME_LENGTH
               - RP_LINK_SYMBOL_LENGTH);
      }
      int vmNeedLength =
            MAX_VC_OBJECT_NAME_LENGTH - MAX_VM_INDEX_LENGTH
                  - VM_LINK_SYMBOL_LENGTH - maxNodeGroupNameLength;
      if (vmNeedLength <= 0) {
         throw ClusterConfigException
               .NODE_GROUP_NAME_TOO_LONG(MAX_VC_OBJECT_NAME_LENGTH
                     - MAX_VM_INDEX_LENGTH - VM_LINK_SYMBOL_LENGTH);
      }
      int clusterNameMaxLength =
            vmNeedLength - rpNeedLength > 0 ? rpNeedLength : vmNeedLength;
      if (clusterNameMaxLength < clusterNameLength) {
         throw ClusterConfigException
               .CLUSTER_NAME_TOO_LONG(clusterNameMaxLength);
      }
   }

}
