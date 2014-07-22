/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.spectypes.HadoopDistroMap;
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
   @Expose
   @SerializedName("appManager")
   private String appManager;
   private ClusterType type;
   private String externalHDFS;
   private String externalMapReduce;
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
   private String packagesExistStatus;
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
   @Deprecated
   private String templateId;
   @Expose
   @SerializedName("deploy_policy")
   @Deprecated
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
      this.appManager = cluster.appManager;
      this.type = cluster.type;
      this.distroVendor = cluster.distroVendor;
      this.distroVersion = cluster.distroVersion;
      this.externalHDFS = cluster.externalHDFS;
      this.externalMapReduce = cluster.externalMapReduce;
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

   @RestRequired
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getAppManager() {
      return appManager;
   }

   public void setAppManager(String appManager) {
      this.appManager = appManager;
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

   public String getExternalMapReduce() {
      return externalMapReduce;
   }

   public void setExternalMapReduce(String externalMapReduce) {
      this.externalMapReduce = externalMapReduce;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   @RestIgnore
   public String getDistroVendor() {
      return distroVendor;
   }

   public void setDistroVendor(String distroVendor) {
      this.distroVendor = distroVendor;
   }

   @RestIgnore
   public String getDistroVersion() {
      return distroVersion;
   }

   public void setDistroVersion(String distroVersion) {
      this.distroVersion = distroVersion;
   }

   public String getPackagesExistStatus() {
      return packagesExistStatus;
   }

   public void setPackagesExistStatus(String packagesExistStatus) {
      this.packagesExistStatus = packagesExistStatus;
   }

   @RestIgnore
   public String getHttpProxy() {
      return httpProxy;
   }

   public void setHttpProxy(String httpProxy) {
      this.httpProxy = httpProxy;
   }

   @RestIgnore
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

   @RestIgnore
   public List<NetworkAdd> getNetworkings() {
      return networkings;
   }

   public void setNetworkings(List<NetworkAdd> networkings) {
      this.networkings = networkings;
   }

   @RestIgnore
   public List<VcCluster> getVcClusters() {
      return vcClusters;
   }

   public void setVcClusters(List<VcCluster> vcClusters) {
      this.vcClusters = vcClusters;
   }

   @RestIgnore
   public String getTemplateId() {
      return templateId;
   }

   public void setTemplateId(String templateId) {
      this.templateId = templateId;
   }

   @RestIgnore
   public List<String> getPackageRepos() {
      return packageRepos;
   }

   public void setPackageRepos(List<String> packageRepos) {
      this.packageRepos = packageRepos;
   }

   @RestIgnore
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

   @RestIgnore
   public Map<String, String> getHostToRackMap() {
      return hostToRackMap;
   }

   public void setHostToRackMap(Map<String, String> hostToRackMap) {
      this.hostToRackMap = hostToRackMap;
   }

   @RestIgnore
   public Set<String> getSharedDatastorePattern() {
      return sharedDatastorePattern;
   }

   public void setSharedDatastorePattern(Set<String> sharedDatastorePattern) {
      this.sharedDatastorePattern = sharedDatastorePattern;
   }

   @RestIgnore
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
   @RestIgnore
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
//                  if (nodeGroupCreate.getRoles().contains(
//                        HadoopRole.HADOOP_TASKTRACKER.toString())) {//compute node
//                     PlacementPolicy placementPolicy =
//                           nodeGroupCreate.getPlacementPolicies();
//                     if (placementPolicy != null) {
//                        List<GroupAssociation> groupAssociations =
//                              placementPolicy.getGroupAssociations();
//                        if (groupAssociations != null) {
//                           GroupAssociationType associationType =
//                                 groupAssociations.get(0).getType();
//                           if (associationType != null
//                                 && associationType == GroupAssociationType.STRICT) {
//                              continue;
//                           }
//                        }
//                     }
//                  }
                  failedMsgList.add(Constants.TEMPFS_NOT_ALLOWED);
                  // TEMPFS is a temporary feature, so comment off it to remove role dependency
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
   public boolean containsComputeOnlyNodeGroups(SoftwareManager softwareManager) {
      for (NodeGroupCreate nodeGroup : this.getNodeGroups()) {
         if (softwareManager.isComputeOnlyRoles(nodeGroup.getRoles())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Validate nodeGroupCreates member formats and values in the ClusterCreate.
    */
   public void validateClusterCreate(List<String> failedMsgList, List<String> warningMsgList) {
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
      }
      if (!warningMsgList.isEmpty() && !warningMsgList.get(0).startsWith("Warning: ")) {
         warningMsgList.set(0, "Warning: " + warningMsgList.get(0));
      }

      //TODO emma: confirm with CLI validation
      // been in software manager
//      validateRoleWithWarning(failedMsgList, warningMsgList);
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

   // For HDFS2, apache, mapr, and gphd distros do not have hdfs2 features.
   public boolean supportedWithHdfs2() {
      if (this.getDistroVendor().equalsIgnoreCase(Constants.DEFAULT_VENDOR)
            || this.getDistroVendor().equalsIgnoreCase(Constants.MAPR_VENDOR)
            || this.getDistroVendor().equalsIgnoreCase(Constants.GPHD_VENDOR)){
         return false;
      }
      return true;
   }

   public void validateCDHVersion(List<String> warningMsgList) {
      // If current distro's version is greater than cdh4.2.1, the FQDN must be configured.
      if (this.getDistroVendor().equalsIgnoreCase(Constants.CDH_VENDOR)) {
         Pattern pattern = Pattern.compile(Constants.CDH_PATTERN);
         Matcher matcher=pattern.matcher(this.getDistroVersion());
         if (!matcher.find()) {
            return;
         }
         String version = this.getDistroVersion().substring(matcher.start(), matcher.end());
         if (compare(version, "4.2.1") > 0) {
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

   public ClusterBlueprint toBlueprint() {
       ClusterBlueprint blueprint = new ClusterBlueprint();

       blueprint.setName(name);
       blueprint.setInstanceNum(totalInstances()); //TODO: check
       // TODO: topology
       blueprint.setConfiguration(configuration);
       blueprint.setExternalHDFS(externalHDFS);
       blueprint.setExternalMapReduce(externalMapReduce);

       // set HadoopStack
       HadoopStack hadoopStack = new HadoopStack();
       hadoopStack.setDistro(distro);
       hadoopStack.setVendor(distroVendor);
       hadoopStack.setFullVersion(distroVersion); // TODO
       blueprint.setHadoopStack(hadoopStack);

       blueprint.setNeedToValidateConfig(validateConfig == null ? false
            : validateConfig);

       // set nodes/nodegroups
       List<NodeGroupInfo> nodeGroupInfos = new ArrayList<NodeGroupInfo>();
       if (nodeGroups != null) {
          for (NodeGroupCreate group : nodeGroups) {
             NodeGroupInfo nodeGroupInfo = group.toNodeGroupInfo();
             nodeGroupInfos.add(nodeGroupInfo);
          }
       }
       blueprint.setNodeGroups(nodeGroupInfos);
       return blueprint;
   }

}
