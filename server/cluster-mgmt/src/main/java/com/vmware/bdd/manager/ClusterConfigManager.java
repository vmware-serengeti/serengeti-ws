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
package com.vmware.bdd.manager;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.vmware.aurora.util.HbaseRegionServerOptsUtil;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.*;
import org.apache.commons.collections.MapUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.DiskSpec.AllocationType;
import com.vmware.bdd.apitypes.*;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupRacks;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupRacks.GroupRacksType;
import com.vmware.bdd.apitypes.StorageRead.DiskScsiControllerType;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.IpBlockEntity;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NodeGroupAssociation;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.exception.UniqueConstraintViolationException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.resmgmt.IDatastoreService;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.IResourcePoolService;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.specpolicy.CommonClusterExpandPolicy;
import com.vmware.bdd.spectypes.VcCluster;

public class ClusterConfigManager {
   private static final long serialVersionUID = 1L;
   private static final Logger logger = Logger
         .getLogger(ClusterConfigManager.class);

   private IResourcePoolService rpMgr;
   private INetworkService networkMgr;
   private RackInfoManager rackInfoMgr;
   private IDatastoreService datastoreMgr;
   private IClusterEntityManager clusterEntityMgr;
   private IClusteringService clusteringService;

   private SoftwareManagerCollector softwareManagerCollector;

   private static final String TEMPLATE_ID = "template_id";
   private static final String HTTP_PROXY = "serengeti.http_proxy";
   private static final String NO_PROXY = "serengeti.no_proxy";
   private static final String ELASTIC_RUNTIME_AUTOMATION_ENABLE =
         "elastic_runtime.automation.enable";
   private String templateId = Configuration.getString(TEMPLATE_ID.toString(),
         "centos57-x64");
   private String httpProxy = Configuration
         .getString(HTTP_PROXY.toString(), "");
   private String noProxy = Configuration.getStrings(NO_PROXY.toString(), "");
   private boolean automationEnable = Configuration.getBoolean(
         ELASTIC_RUNTIME_AUTOMATION_ENABLE, false);

   public IDatastoreService getDatastoreMgr() {
      return datastoreMgr;
   }

   public void setDatastoreMgr(IDatastoreService datastoreMgr) {
      this.datastoreMgr = datastoreMgr;
   }

   public RackInfoManager getRackInfoMgr() {
      return rackInfoMgr;
   }

   public void setRackInfoMgr(RackInfoManager rackInfoMgr) {
      this.rackInfoMgr = rackInfoMgr;
   }

   public IResourcePoolService getRpMgr() {
      return rpMgr;
   }

   public void setRpMgr(IResourcePoolService rpMgr) {
      this.rpMgr = rpMgr;
   }

   public INetworkService getNetworkMgr() {
      return networkMgr;
   }

   public void setNetworkMgr(INetworkService networkMgr) {
      this.networkMgr = networkMgr;
   }

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   @Autowired
   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   @Autowired
   public void setSoftwareManagerCollector(
         SoftwareManagerCollector softwareManagerCollector) {
      this.softwareManagerCollector = softwareManagerCollector;
   }

   private void applyInfraChanges(ClusterCreate cluster,
         ClusterBlueprint blueprint) {
      cluster.setConfiguration(blueprint.getConfiguration());
      sortNodeGroups(cluster, blueprint);
      // as we've sorted node groups, so here we can assume node group are in same location in the array.
      for (int i = 0; i < blueprint.getNodeGroups().size(); i++) {
         NodeGroupInfo group = blueprint.getNodeGroups().get(i);
         NodeGroupCreate groupCreate = cluster.getNodeGroups()[i];
         groupCreate.setConfiguration(group.getConfiguration());
         groupCreate.setRoles(group.getRoles());
         groupCreate.setInstanceType(group.getInstanceType());
         groupCreate.setPlacementPolicies(group.getPlacement());
         if (groupCreate.getStorage() == null) {
            groupCreate.setStorage(new StorageRead());
         }
         groupCreate.getStorage().setSizeGB(group.getStorageSize());
      }
      cluster.setExternalHDFS(blueprint.getExternalHDFS());
      cluster.setExternalMapReduce(blueprint.getExternalMapReduce());
   }

   private void sortNodeGroups(ClusterCreate cluster, ClusterBlueprint blueprint) {
      NodeGroupCreate[] sortedGroups =
            new NodeGroupCreate[cluster.getNodeGroups().length];
      for (int i = 0; i < blueprint.getNodeGroups().size(); i++) {
         NodeGroupInfo groupInfo = blueprint.getNodeGroups().get(i);
         if (cluster.getNodeGroups()[i].getName().equals(groupInfo.getName())) {
            // to save query time
            sortedGroups[i] = cluster.getNodeGroups()[i];
         }
         sortedGroups[i] = cluster.getNodeGroup(groupInfo.getName());
      }
      cluster.setNodeGroups(sortedGroups);
   }

   @Transactional
   public ClusterEntity createClusterConfig(ClusterCreate cluster) {
      String name = cluster.getName();
      if (name == null || name.isEmpty()) {
         throw ClusterConfigException.CLUSTER_NAME_MISSING();
      }

      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();

      String appManager = cluster.getAppManager();
      if (appManager == null) {
         appManager = Constants.IRONFAN;
      }
      SoftwareManager softwareManager = getSoftwareManager(appManager);
      HadoopStack stack =
            filterDistroFromAppManager(softwareManager, cluster.getDistro());
      if (cluster.getDistro() == null || stack == null) {
         throw BddException.INVALID_PARAMETER("distro", cluster.getDistro());
      }
      // only check roles validity in server side, but not in CLI and GUI, because roles info exist in server side.
      ClusterBlueprint blueprint = cluster.toBlueprint();
      try {
         softwareManager.validateBlueprint(cluster.toBlueprint());
         cluster.validateClusterCreate(failedMsgList, warningMsgList);
      } catch (ValidationException e) {
         failedMsgList.addAll(e.getFailedMsgList());
         warningMsgList.addAll(e.getWarningMsgList());
      }

      if (!failedMsgList.isEmpty()) {
         throw ClusterConfigException.INVALID_SPEC(failedMsgList);
      }

      if (!validateRacksInfo(cluster, failedMsgList)) {
         throw ClusterConfigException.INVALID_PLACEMENT_POLICIES(failedMsgList);
      }

      String localRepoURL = cluster.getLocalRepoURL();
      if (!CommonUtil.isBlank(localRepoURL)
            && !validateLocalRepoURL(localRepoURL)) {
         throw ClusterConfigException.INVALID_LOCAL_REPO_URL(failedMsgList);
      }

      try {
         ClusterEntity entity = clusterEntityMgr.findByName(name);
         if (entity != null) {
            logger.info("can not create cluster " + name
                  + ", which is already existed.");
            throw BddException.ALREADY_EXISTS("Cluster", name);
         }

         updateInfrastructure(cluster, softwareManager, blueprint);
         // persist cluster config
         logger.debug("begin to add cluster config for " + name);
         Gson gson = new Gson();
         ClusterEntity clusterEntity = new ClusterEntity(name);
         clusterEntity.setAppManager(cluster.getAppManager());
         clusterEntity.setDistro(cluster.getDistro());
         clusterEntity.setDistroVendor(cluster.getDistroVendor());
         clusterEntity.setDistroVersion(cluster.getDistroVersion());
         clusterEntity.setStartAfterDeploy(true);
         clusterEntity.setPassword(cluster.getPassword());

         // set cluster version
         clusterEntity.setVersion(clusterEntityMgr.getServerVersion());

         if (cluster.containsComputeOnlyNodeGroups(softwareManager)) {
            clusterEntity.setAutomationEnable(automationEnable);
         } else {
            clusterEntity.setAutomationEnable(null);
         }
         clusterEntity.setVhmMinNum(-1);
         clusterEntity.setVhmMaxNum(-1);

         if (cluster.getRpNames() != null && cluster.getRpNames().size() > 0) {
            logger.debug("resource pool " + cluster.getRpNames()
                  + " specified for cluster " + name);
            clusterEntity.setVcRpNameList(cluster.getRpNames());
         } else {
            logger.debug("no resource pool name specified, use global configuration.");
         }
         if (cluster.getDsNames() != null && !cluster.getDsNames().isEmpty()) {
            logger.debug("datastore " + cluster.getDsNames()
                  + " specified for cluster " + name);
            clusterEntity.setVcDatastoreNameList(cluster.getDsNames());
         } else {
            logger.debug("no datastore name specified, use global configuration.");
         }

         clusterEntity.setNetworkConfig(validateAndConvertNetNamesToNetConfigs(
               cluster.getNetworkConfig(), cluster.getDistroVendor()
                     .equalsIgnoreCase(Constants.MAPR_VENDOR)));
         clusterEntity.setVhmJobTrackerPort("50030");
         if (cluster.getConfiguration() != null
               && cluster.getConfiguration().size() > 0) {
            clusterEntity.setHadoopConfig((new Gson()).toJson(cluster
                  .getConfiguration()));

            updateVhmJobTrackerPort(cluster, clusterEntity);
         }


         setInfraConfig(cluster, clusterEntity);

         setAdvancedProperties(cluster.getExternalHDFS(),
               cluster.getExternalMapReduce(), localRepoURL,
               cluster.getExternalNamenode(),
               cluster.getExternalSecondaryNamenode(),
               cluster.getExternalDatanodes(),
               cluster.getClusterCloneType(),
               clusterEntity);
         NodeGroupCreate[] groups = cluster.getNodeGroups();
         if (groups != null && groups.length > 0) {
            clusterEntity.setNodeGroups(convertNodeGroupsToEntities(gson,
                  clusterEntity, cluster.getDistro(), groups,
                  cluster.isValidateConfig()));

            //make sure memory size is no less than MIN_MEM_SIZE
            validateMemorySize(clusterEntity.getNodeGroups(), failedMsgList);
            if (!failedMsgList.isEmpty()) {
               throw ClusterConfigException.INVALID_SPEC(failedMsgList);
            }
         }
         //Set HBASE_REGISIONSERVER_OPTS for ironfan hbase_regionserer whose latencySensitivity is set to HIGH
         if(!CommonUtil.isBlank(cluster.getAppManager()) && Constants.IRONFAN.equals(cluster.getAppManager()))
            for(int i=0; i<clusterEntity.getNodeGroups().size(); i++){
               NodeGroupEntity group = clusterEntity.getNodeGroups().get(i);
               String groupRoles = group.getRoles();
               if((group.getLatencySensitivity() == LatencyPriority.HIGH)
                     && ((groupRoles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE.toString())))){
                     setHbase_RegionServer_Opts(cluster, group);
                     if (cluster.getConfiguration() != null
                           && cluster.getConfiguration().size() > 0) {
                        clusterEntity.setHadoopConfig((new Gson()).toJson(cluster
                              .getConfiguration()));
                     }
                     break;
               }
            }


         if (cluster.getTopologyPolicy() == null) {
            clusterEntity.setTopologyPolicy(TopologyType.NONE);
         } else {
            clusterEntity.setTopologyPolicy(cluster.getTopologyPolicy());
         }

         if (clusterEntity.getTopologyPolicy() == TopologyType.HVE) {
            boolean hveSupported = false;
            if (clusterEntity.getDistro() != null) {
               HadoopStack hadoopStack =
                     filterDistroFromAppManager(softwareManager,
                           clusterEntity.getDistro());
               if (hadoopStack != null) {
                  hveSupported = hadoopStack.isHveSupported();
               }
            }
            if (!hveSupported) {
               throw ClusterConfigException.INVALID_TOPOLOGY_POLICY(
                     clusterEntity.getTopologyPolicy(),
                     "current Hadoop distribution does not support HVE.");
            }
         }

         clusterEntityMgr.insert(clusterEntity);
         logger.debug("finished to add cluster config for " + name);
         return clusterEntity;
      } catch (UniqueConstraintViolationException ex) {
         logger.info("can not create cluster " + name
               + ", which is already existed.");
         throw BddException.ALREADY_EXISTS(ex, "Cluster", name);
      }
   }

   /**
    * Set the HBASE_REGIONSERVER_OPTS for hbase_regionserver node when the nodegroup's latencySensitivity is High
    * @param cluster
    * @param ng
    */
   private void setHbase_RegionServer_Opts(ClusterCreate cluster, NodeGroupEntity ng) {
      Map<String, Object> conf = cluster.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         cluster.setConfiguration(conf);
      }

      Map<String, Object> hbase = (Map<String, Object>) conf.get("hbase");
      if (hbase == null) {
         hbase = new HashMap<String, Object>();
         conf.put("hbase", hbase);
      }

      Map<String, Object> hbaseEnv = (Map<String, Object>) hbase.get("hbase-env.sh");
      if (hbaseEnv == null) {
         hbaseEnv = new HashMap<String, Object>();
         hbase.put("hbase-env.sh", hbaseEnv);
      }

      if (hbaseEnv.get("HBASE_REGIONSERVER_OPTS") == null) {
         Gson gson = new Gson();
         int roleNums = gson.fromJson(ng.getRoles(), List.class).size();
         hbaseEnv.put("HBASE_REGIONSERVER_OPTS", HbaseRegionServerOptsUtil
                     .getIronfanHbaseRegionServerStringParameter(
                           ng.getMemorySize(), roleNums));
         logger.info("hbase-env.sh" + hbase.get("HBASE_REGIONSERVER_OPTS"));
      }
   }

   private void setInfraConfig(ClusterCreate cluster, ClusterEntity clusterEntity) {
      Map<String, Map<String, String>> infraConfigs = cluster.getInfrastructure_config();

      if(MapUtils.isNotEmpty(infraConfigs)) {
         clusterEntity.setInfraConfig(InfrastructureConfigUtils.write(infraConfigs));
      }
   }

   private void setAdvancedProperties(String externalHDFS,
         String externalMapReduce, String localRepoURL,
         String externalNamenode, String externalSecondaryNamenode,
         Set<String> externalDatanodes, String clusterCloneType,ClusterEntity clusterEntity) {
      if (!CommonUtil.isBlank(externalHDFS)
            || !CommonUtil.isBlank(externalMapReduce)
            || !CommonUtil.isBlank(localRepoURL)
            || !CommonUtil.isBlank(externalNamenode)
            || !CommonUtil.isBlank(externalSecondaryNamenode)
            || !CommonUtil.isBlank(clusterCloneType)
            || (externalDatanodes != null && !externalDatanodes.isEmpty())) {
         Map<String, Object> advancedProperties = new HashMap<String, Object>();
         advancedProperties.put("ExternalHDFS", externalHDFS);
         advancedProperties.put("ExternalMapReduce", externalMapReduce);
         advancedProperties.put("LocalRepoURL", localRepoURL);
         advancedProperties.put("ExternalNamenode", externalNamenode);
         advancedProperties.put("ExternalSecondaryNamenode", externalSecondaryNamenode);
         advancedProperties.put("ExternalDatanodes", externalDatanodes);
         advancedProperties.put("ClusterCloneType", clusterCloneType);
         Gson g = new Gson();
         clusterEntity.setAdvancedProperties(g.toJson(advancedProperties));
      }
   }

   public HadoopStack filterDistroFromAppManager(
         SoftwareManager softwareManager, String distroName) {
      List<HadoopStack> hadoopStacks = softwareManager.getSupportedStacks();
      if (!CommonUtil.isBlank(distroName)) {
         for (HadoopStack hadoopStack : hadoopStacks) {
            if (distroName.equalsIgnoreCase(hadoopStack.getDistro())) {
               return hadoopStack;
            }
         }
         throw BddException.NOT_FOUND("Distro", distroName);
      } else {
         return softwareManager.getDefaultStack();
      }
   }

   private void updateInfrastructure(ClusterCreate cluster,
         SoftwareManager softwareManager, ClusterBlueprint blueprint) {
      softwareManager.updateInfrastructure(blueprint);
      applyInfraChanges(cluster, blueprint);
   }

   private Map<NetTrafficType, List<ClusterNetConfigInfo>> validateAndConvertNetNamesToNetConfigs(
         Map<NetTrafficType, List<String>> netNamesInfo, boolean isMaprDistro) {
      Map<NetTrafficType, List<ClusterNetConfigInfo>> netConfigs =
            new HashMap<NetTrafficType, List<ClusterNetConfigInfo>>();
      Map<String, Set<String>> port2names = new HashMap<String, Set<String>>();

      for (NetTrafficType type : netNamesInfo.keySet()) {
         netConfigs.put(type, new ArrayList<ClusterNetConfigInfo>());
         for (String name : netNamesInfo.get(type)) {
            NetworkEntity networkEntity = networkMgr.getNetworkEntityByName(name);

            String pg = networkEntity.getPortGroup();
            Boolean isGenerateHostname = networkEntity.getIsGenerateHostname();
            String hostnamePrefix = HostnameManager.getHostnamePrefix();

            ClusterNetConfigInfo netConfig = new ClusterNetConfigInfo(type, name, pg, networkEntity.getDnsType(), isGenerateHostname, hostnamePrefix);
            netConfigs.get(type).add(netConfig);

            if (!port2names.containsKey(pg)) {
               port2names.put(pg, new HashSet<String>());
            }
            port2names.get(pg).add(name);
         }
      }

      if (isMaprDistro && port2names.size() > 1) {
         throw BddException.MULTI_NETWORKS_FOR_MAPR_DISTRO();
      }

      // if nw1,nw2 are both refer to pg1, should not use them in one cluster
      for (String pg : port2names.keySet()) {
         if (port2names.get(pg).size() > 1) {
            throw BddException.PG_REFERENCED_MULTI_TIMES();
         }
      }

      return netConfigs;
   }

   private Map<NetTrafficType, List<String>> convertNetConfigsToNetNames(
         Map<NetTrafficType, List<ClusterNetConfigInfo>> netConfigs) {
      Map<NetTrafficType, List<String>> netNamesInfo =
            new HashMap<NetTrafficType, List<String>>();
      for (NetTrafficType type : netConfigs.keySet()) {
         netNamesInfo.put(type, new ArrayList<String>());
         for (ClusterNetConfigInfo config : netConfigs.get(type)) {
            netNamesInfo.get(type).add(config.getNetworkName());
         }
      }
      return netNamesInfo;
   }

   private void validateMemorySize(List<NodeGroupEntity> nodeGroups,
         List<String> failedMsgList) {
      boolean validated = true;
      StringBuilder invalidNodeGroupNames = new StringBuilder();
      for (NodeGroupEntity nodeGroup : nodeGroups) {
         if (nodeGroup.getMemorySize() < Constants.MIN_MEM_SIZE) {
            validated = false;
            invalidNodeGroupNames.append(nodeGroup.getName()).append(",");
         }
      }
      if (!validated) {
         StringBuilder errorMsgBuff = new StringBuilder();
         invalidNodeGroupNames.delete(invalidNodeGroupNames.length() - 1,
               invalidNodeGroupNames.length());
         failedMsgList.add(errorMsgBuff
               .append(
                     "'memCapacityMB' cannot be less than "
                           + Constants.MIN_MEM_SIZE + " in group ")
               .append(invalidNodeGroupNames.toString())
               .append(" in order for nodes to run normally").toString());
      }
   }

   private void updateVhmJobTrackerPort(ClusterCreate cluster,
         ClusterEntity clusterEntity) {
      if (cluster.getConfiguration().containsKey("hadoop")) {
         @SuppressWarnings("unchecked")
         Map<String, Object> hadoopConfig =
               (Map<String, Object>) cluster.getConfiguration().get("hadoop");
         if (hadoopConfig.containsKey("mapred-site.xml")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> maprConfig =
                  (Map<String, Object>) hadoopConfig.get("mapred-site.xml");
            String jobtrackerAddress =
                  (String) maprConfig.get("mapred.job.tracker.http.address");
            if (jobtrackerAddress != null) {
               String[] items = jobtrackerAddress.split(":");
               String port = items[items.length - 1];
               Pattern pattern = Pattern.compile("[0-9]{1,5}");
               Matcher matcher = pattern.matcher(port);
               if (matcher.matches()) {
                  clusterEntity.setVhmJobTrackerPort(port);
               }
            }
         }
      }
   }

   private boolean validateRacksInfo(ClusterCreate cluster,
         List<String> failedMsgList) {
      boolean valid = true;
      Map<String, NodeGroupCreate> allGroups =
            new TreeMap<String, NodeGroupCreate>();
      if (cluster.getNodeGroups() == null) {
         return valid;
      }

      List<RackInfo> racksInfo = rackInfoMgr.exportRackInfo();

      if ((cluster.getTopologyPolicy() == TopologyType.HVE || cluster
            .getTopologyPolicy() == TopologyType.RACK_AS_RACK)
            && racksInfo.isEmpty()) {
         valid = false;
         throw ClusterConfigException
               .TOPOLOGY_WITH_NO_MAPPING_INFO_EXIST(cluster.getTopologyPolicy()
                     .toString());
      }

      for (NodeGroupCreate nodeGroupCreate : cluster.getNodeGroups()) {
         allGroups.put(nodeGroupCreate.getName(), nodeGroupCreate);
      }

      for (NodeGroupCreate ngc : cluster.getNodeGroups()) {
         PlacementPolicy policies = ngc.getPlacementPolicies();
         if (policies != null && policies.getGroupAssociations() != null) {
            continue;
         }

         if (ngc.getStorage() != null
               && ngc.getStorage().getType() != null
               && ngc.getStorage().getType()
                     .equals(DatastoreType.SHARED.toString())) {
            continue;
         }

         if (policies != null && policies.getGroupRacks() != null) {
            if (racksInfo.isEmpty()) {
               valid = false;
               throw ClusterConfigException
                     .RACKPOLICY_WITH_NO_MAPPING_INFO_EXIST(ngc.getName());
            }

            GroupRacks r = policies.getGroupRacks();
            GroupRacksType rackType = r.getType();
            Set<String> specifiedRacks =
                  new HashSet<String>(Arrays.asList(r.getRacks()));

            if (rackType.equals(GroupRacksType.SAMERACK)
                  && specifiedRacks.size() != 1) {
               throw ClusterConfigException.MUST_DEFINE_ONE_RACK(ngc.getName());
            }

            List<String> intersecRacks = new ArrayList<String>();
            int intersecHostNum = 0;
            int maxIntersecHostNum = 0;

            for (RackInfo rackInfo : racksInfo) {
               if (specifiedRacks.isEmpty() || specifiedRacks.size() == 0
                     || specifiedRacks.contains(rackInfo.getName())) {
                  intersecHostNum += rackInfo.getHosts().size();
                  intersecRacks.add(rackInfo.getName());
                  if (rackInfo.getHosts().size() > maxIntersecHostNum) {
                     maxIntersecHostNum = rackInfo.getHosts().size();
                  }
               }
            }

            if (intersecRacks.size() == 0) {
               valid = false;
               throw ClusterConfigException.NO_VALID_RACK(ngc.getName());
            }

            if (ngc.calculateHostNum() != null) {
               if (rackType.equals(GroupRacksType.ROUNDROBIN)
                     && ngc.calculateHostNum() > intersecHostNum) {
                  valid = false;
                  throw ClusterConfigException.LACK_PHYSICAL_HOSTS(
                        ngc.calculateHostNum(), ngc.getName(), intersecHostNum);
               } else if (rackType.equals(GroupRacksType.SAMERACK)
                     && ngc.calculateHostNum() > maxIntersecHostNum) {
                  valid = false;
                  throw ClusterConfigException.LACK_PHYSICAL_HOSTS(
                        ngc.calculateHostNum(), ngc.getName(),
                        maxIntersecHostNum);
               }
            }

            if (specifiedRacks.isEmpty()) {
               r.setRacks(new String[0]);
            } else {
               r.setRacks(intersecRacks.toArray(new String[intersecRacks.size()]));
            }
         }
      }
      return valid;
   }

   private List<NodeGroupEntity> convertNodeGroupsToEntities(Gson gson,
         ClusterEntity clusterEntity, String distro, NodeGroupCreate[] groups,
         boolean validateWhiteList) {
      List<NodeGroupEntity> nodeGroups = new LinkedList<NodeGroupEntity>();
      for (NodeGroupCreate group : groups) {
         NodeGroupEntity groupEntity =
               convertGroup(gson, clusterEntity, group, distro,
                     validateWhiteList);
         if (groupEntity != null) {
            nodeGroups.add(groupEntity);
         }
      }
      return nodeGroups;
   }

   private NodeGroupEntity convertGroup(Gson gson, ClusterEntity clusterEntity,
         NodeGroupCreate group, String distro, boolean validateWhiteList) {
      NodeGroupEntity groupEntity = new NodeGroupEntity();
      if (group.getRoles() == null || group.getRoles().isEmpty()) {
         throw ClusterConfigException.NO_HADOOP_ROLE_SPECIFIED(group.getName());
      }

      groupEntity.setCluster(clusterEntity);
      int cpuNum = group.getCpuNum() == null ? 0 : group.getCpuNum();
      if (!VcVmUtil.validateCPU(clusteringService.getTemplateVmId(), cpuNum)) {
         throw VcProviderException.CPU_NUM_NOT_MULTIPLE_OF_CORES_PER_SOCKET(
               group.getName(), clusteringService.getTemplateVmName());
      }

      groupEntity.setCpuNum(cpuNum);
      groupEntity.setDefineInstanceNum(group.getInstanceNum());
      groupEntity.setMemorySize(group.getMemCapacityMB() == null ? 0 : group
            .getMemCapacityMB());
      groupEntity.setSwapRatio(group.getSwapRatio());
      groupEntity.setName(group.getName());
      groupEntity.setNodeType(group.getInstanceType());

      PlacementPolicy policies = group.getPlacementPolicies();
      if (policies != null) {
         List<GroupAssociation> associons = policies.getGroupAssociations();
         if (associons != null) {
            Set<NodeGroupAssociation> associonEntities =
                  new HashSet<NodeGroupAssociation>();
            for (GroupAssociation a : associons) {
               NodeGroupAssociation ae = new NodeGroupAssociation();
               ae.setAssociationType(a.getType());
               ae.setNodeGroup(groupEntity);
               ae.setReferencedGroup(a.getReference());
               associonEntities.add(ae);
            }
            groupEntity.setGroupAssociations(associonEntities);
         }
         if (policies.getInstancePerHost() != null) {
            groupEntity.setInstancePerHost(policies.getInstancePerHost());
         }

         if (policies.getGroupRacks() != null) {
            groupEntity.setGroupRacks((new Gson()).toJson(policies
                  .getGroupRacks()));
         }
      }

      if (group.getRpNames() != null && group.getRpNames().size() > 0) {
         groupEntity.setVcRpNameList(group.getRpNames());
      }

      /*
       * do not support node group level networks temporarilly
      if (group.getNetworkNames() != null && group.getNetworkNames().size() > 0) {
         groupEntity.setVcNetworkNames(group.getNetworkNames());
      }
      */

      Set<String> roles = new LinkedHashSet<String>();
      convertStorage(group, groupEntity, roles);
      roles.addAll(group.getRoles());

      groupEntity.setRoles(gson.toJson(roles));

      if (group.getInstanceNum() <= 0) {
         logger.warn("Zero or negative instance number for group "
               + group.getName() + ", remove the group from cluster spec.");
         return null;
      }

      List<String> dsNames = groupEntity.getVcDatastoreNameList();
      if (dsNames == null) {
         dsNames = clusterEntity.getVcDatastoreNameList();
      }
      Set<String> sharedPattern;
      Set<String> localPattern;
      if (dsNames != null) {
         sharedPattern = datastoreMgr.getSharedDatastoresByNames(dsNames);
         localPattern = datastoreMgr.getLocalDatastoresByNames(dsNames);
      } else {
         sharedPattern = datastoreMgr.getAllSharedDatastores();
         localPattern = datastoreMgr.getAllLocalDatastores();
      }

      SoftwareManager softwareManager =
            getSoftwareManager(clusterEntity.getAppManager());
      CommonClusterExpandPolicy.expandGroupInstanceType(groupEntity, group,
            sharedPattern, localPattern, softwareManager);
      String haFlag = group.getHaFlag();
      if (haFlag == null) {
         groupEntity.setHaFlag(Constants.HA_FLAG_OFF);
      } else {
         groupEntity.setHaFlag(haFlag);
      }
      LatencyPriority latencySensitivity = group.getLatencySensitivity();
      if(latencySensitivity != null)
         groupEntity.setLatencySensitivity(latencySensitivity);
      else
         groupEntity.setLatencySensitivity(LatencyPriority.NORMAL);
      groupEntity.setReservedCpu_ratio(group.getReservedCpu_ratio());
      groupEntity.setReservedMem_ratio(group.getReservedMem_ratio());
      if (group.getConfiguration() != null
            && group.getConfiguration().size() > 0) {
         groupEntity.setHadoopConfig(gson.toJson(group.getConfiguration()));
      }
      // set vm folder path
      groupEntity.setVmFolderPath(clusterEntity);
      logger.debug("finished to convert node group config for "
            + group.getName());
      return groupEntity;
   }

   private void convertStorage(NodeGroupCreate group,
         NodeGroupEntity groupEntity, Set<String> roles) {
      if (group.getStorage() != null) {
         groupEntity.setStorageSize(group.getStorage().getSizeGB());
         groupEntity.setDiskNum(getDiskNumberForNodeGroup(group));
         groupEntity.setShareDatastore(isShareDatastoreForNodeGroup(group));
         //currently, ignore input from CLI and hard code here
         String storageType = group.getStorage().getType();
         if (storageType != null) {
            if (storageType.equalsIgnoreCase(DatastoreType.TEMPFS.name())) {
               groupEntity.setStorageType(DatastoreType.TEMPFS);
            } else if (storageType.equalsIgnoreCase(DatastoreType.LOCAL.name())) {
               groupEntity.setStorageType(DatastoreType.LOCAL);
            } else {
               groupEntity.setStorageType(DatastoreType.SHARED);
            }
         }
         groupEntity.setVcDatastoreNameList(group.getStorage().getDsNames());
         groupEntity.setSdDatastoreNameList(group.getStorage()
               .getDsNames4System());
         groupEntity.setDdDatastoreNameList(group.getStorage()
               .getDsNames4Data());
      }

      if (groupEntity.getStorageType() == DatastoreType.LOCAL) {
         // only when explicitly set to local, we'll choose local storage
         if (group.getHaFlag() != null
               && Constants.HA_FLAG_FT.equals(group.getHaFlag().toLowerCase())) {
            throw ClusterConfigException.LOCAL_STORAGE_USED_FOR_FT_GROUP(group
                  .getName());
         }
      }
   }

   @Transactional(readOnly = true)
   public ClusterCreate getClusterConfig(String clusterName) {
      return getClusterConfig(clusterName, true);
   }

   @Transactional(readOnly = true)
   public ClusterCreate getClusterConfig(String clusterName, boolean needAllocIp) {
      ClusterEntity clusterEntity = clusterEntityMgr.findByName(clusterName);
      if (clusterEntity == null) {
         throw ClusterConfigException.CLUSTER_CONFIG_NOT_FOUND(clusterName);
      }
      ClusterCreate clusterConfig = new ClusterCreate();
      clusterConfig.setName(clusterEntity.getName());
      clusterConfig.setAppManager(clusterEntity.getAppManager());
      clusterConfig.setDistro(clusterEntity.getDistro());
      Map<NetTrafficType, List<ClusterNetConfigInfo>> networkConfigInfo = clusterEntity.getNetworkConfigInfo();
      if (networkConfigInfo != null && !networkConfigInfo.isEmpty()) {
         for (NetTrafficType trafficType : networkConfigInfo.keySet()) {
            clusterConfig.setHostnamePrefix(networkConfigInfo.get(trafficType).get(0).getHostnamePrefix());
            break;
         }
      }
      convertClusterConfig(clusterEntity, clusterConfig, needAllocIp);

      Gson gson =
            new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
      String manifest = gson.toJson(clusterConfig);
      logger.debug("final cluster manifest " + manifest);
      return clusterConfig;
   }

   @SuppressWarnings("unchecked")
   private void convertClusterConfig(ClusterEntity clusterEntity,
         ClusterCreate clusterConfig, boolean needAllocIp) {
      logger.debug("begin to expand config for cluster "
            + clusterEntity.getName());
      clusterConfig.setDistroVendor(clusterEntity.getDistroVendor());
      clusterConfig.setDistroVersion(clusterEntity.getDistroVersion());
      clusterConfig.setAppManager(clusterEntity.getAppManager());
      clusterConfig.setHttpProxy(httpProxy);
      clusterConfig.setNoProxy(noProxy);
      clusterConfig.setTopologyPolicy(clusterEntity.getTopologyPolicy());
      clusterConfig.setPassword(clusterEntity.getPassword());

      Map<String, String> hostToRackMap = rackInfoMgr.exportHostRackMap();
      if ((clusterConfig.getTopologyPolicy() == TopologyType.RACK_AS_RACK || clusterConfig
            .getTopologyPolicy() == TopologyType.HVE)
            && hostToRackMap.isEmpty()) {
         logger.error("trying to use host-rack topology which is absent");
         throw ClusterConfigException.INVALID_TOPOLOGY_POLICY(
               clusterConfig.getTopologyPolicy(), "no rack information.");
      }
      clusterConfig.setHostToRackMap(hostToRackMap);

      clusterConfig.setTemplateId(templateId);
      if (clusterEntity.getVcRpNames() != null) {
         logger.debug("resource pool specified at cluster level.");
         String[] rpNames =
               clusterEntity.getVcRpNameList().toArray(
                     new String[clusterEntity.getVcRpNameList().size()]);
         List<VcCluster> vcClusters =
               rpMgr.getVcResourcePoolByNameList(rpNames);
         clusterConfig.setVcClusters(vcClusters);
         clusterConfig.setRpNames(clusterEntity.getVcRpNameList());
      } else {
         // set all vc clusters
         clusterConfig.setVcClusters(rpMgr.getAllVcResourcePool());
         logger.debug("no resource pool config at cluster level.");
      }

      if (clusterEntity.getVcDatastoreNameList() != null) {
         logger.debug("datastore specified at cluster level.");
         Set<String> sharedPattern =
               datastoreMgr.getSharedDatastoresByNames(clusterEntity
                     .getVcDatastoreNameList());
         clusterConfig.setSharedDatastorePattern(sharedPattern);
         Set<String> localPattern =
               datastoreMgr.getLocalDatastoresByNames(clusterEntity
                     .getVcDatastoreNameList());
         clusterConfig.setLocalDatastorePattern(localPattern);
         clusterConfig.setDsNames(clusterEntity.getVcDatastoreNameList());
      } else {
         // set all shared and local datastores
         clusterConfig.setSharedDatastorePattern(datastoreMgr
               .getAllSharedDatastores());
         clusterConfig.setLocalDatastorePattern(datastoreMgr
               .getAllLocalDatastores());
         logger.debug("no datastore config at cluster level.");
      }
      List<NodeGroupCreate> nodeGroups = new ArrayList<NodeGroupCreate>();

      // TODO need more role checks

      List<NodeGroupEntity> nodeGroupEntities = clusterEntity.getNodeGroups();
      long instanceNum = 0;

      for (NodeGroupEntity ngEntity : nodeGroupEntities) {
         NodeGroupCreate group =
               convertNodeGroups(clusterEntity, ngEntity,
                     clusterEntity.getName());
         nodeGroups.add(group);
         instanceNum += group.getInstanceNum();
      }
      clusterConfig.setNodeGroups(nodeGroups
            .toArray(new NodeGroupCreate[nodeGroups.size()]));

      List<String> networkNames = clusterEntity.fetchNetworkNameList();

      // TODO: refactor this function to support nodeGroup level networks
      List<NetworkAdd> networkingAdds =
            allocatNetworkIp(networkNames, clusterEntity, instanceNum,
                  needAllocIp);
      clusterConfig.setNetworkings(networkingAdds);
      clusterConfig.setNetworkConfig(convertNetConfigsToNetNames(clusterEntity
            .getNetworkConfigInfo()));

      if (clusterEntity.getHadoopConfig() != null) {
         Map<String, Object> hadoopConfig =
               (new Gson())
                     .fromJson(clusterEntity.getHadoopConfig(), Map.class);
         clusterConfig.setConfiguration(hadoopConfig);
      }
      if (!CommonUtil.isBlank(clusterEntity.getAdvancedProperties())) {
         Gson gson = new Gson();
         Map<String, String> advancedProperties =
               gson.fromJson(clusterEntity.getAdvancedProperties(), Map.class);
         clusterConfig.setExternalHDFS(advancedProperties.get("ExternalHDFS"));
         clusterConfig.setExternalMapReduce(advancedProperties
               .get("ExternalMapReduce"));
         clusterConfig.setLocalRepoURL(advancedProperties.get("LocalRepoURL"));
         clusterConfig.setClusterCloneType(advancedProperties.get("ClusterCloneType"));
         clusterConfig.setExternalNamenode(advancedProperties.get("ExternalNamenode"));
         clusterConfig.setExternalSecondaryNamenode(advancedProperties.get("ExternalSecondaryNamenode"));
         if (advancedProperties.get("ExternalDatanodes") != null) {
            clusterConfig.setExternalDatanodes(gson.fromJson(gson.toJson(advancedProperties.get("ExternalDatanodes")), HashSet.class));
         }
      }

      // for clusters from previous releases, the clone type is fast clone
      setDefaultClusterCloneType(clusterConfig);

      if(!CommonUtil.isBlank(clusterEntity.getInfraConfig())) {
         clusterConfig.setInfrastructure_config(InfrastructureConfigUtils.read(clusterEntity.getInfraConfig()));
      }
   }

   private List<NetworkAdd> allocatNetworkIp(List<String> networkNames,
         ClusterEntity clusterEntity, long instanceNum, boolean needAllocIp) {
      List<NetworkAdd> networkings = new ArrayList<NetworkAdd>();

      for (String networkName : networkNames) {
         NetworkEntity networkEntity =
               networkMgr.getNetworkEntityByName(networkName);

         if (needAllocIp) {
            NetworkAdd network = new NetworkAdd();
            network.setPortGroup(networkEntity.getPortGroup());
            network.setName(networkName);
            network
                  .setDhcp(networkEntity.getAllocType() == NetworkEntity.AllocType.DHCP);
            if (!network.getIsDhcp()) {
               logger.debug("using static ip.");
               List<IpBlockEntity> ipBlockEntities =
                     networkMgr.getAllocatedIpBlocks(networkEntity,
                           clusterEntity.getId());
               long allocatedIpNum = IpBlockEntity.count(ipBlockEntities);
               if (allocatedIpNum < instanceNum) {
                  long newNum = instanceNum - allocatedIpNum;
                  List<IpBlockEntity> newIpBlockEntities =
                        networkMgr.alloc(networkEntity, clusterEntity.getId(),
                              newNum);
                  ipBlockEntities.addAll(newIpBlockEntities);
               }
               network.setDns1(networkEntity.getDns1());
               network.setDns2(networkEntity.getDns2());
               network.setGateway(networkEntity.getGateway());
               network.setNetmask(networkEntity.getNetmask());
               List<IpBlock> ips = new ArrayList<IpBlock>();
               for (IpBlockEntity ipBlockEntity : ipBlockEntities) {
                  IpBlock ip = new IpBlock();
                  ip.setBeginIp(ipBlockEntity.getBeginAddress());
                  ip.setEndIp(ipBlockEntity.getEndAddress());
                  ips.add(ip);
               }
               network.setIpBlocks(ips);
            }
            networkings.add(network);
         }
      }
      return networkings;
   }

   @SuppressWarnings("unchecked")
   private NodeGroupCreate convertNodeGroups(ClusterEntity clusterEntity,
         NodeGroupEntity ngEntity, String clusterName) {
      Gson gson = new Gson();
      List<String> groupRoles = gson.fromJson(ngEntity.getRoles(), List.class);
      NodeGroupCreate group = new NodeGroupCreate();
      group.setName(ngEntity.getName());
      group.setRoles(groupRoles);
      int cpu = ngEntity.getCpuNum();
      if (cpu > 0) {
         group.setCpuNum(cpu);
      }

      int memory = ngEntity.getMemorySize();
      if (memory > 0) {
         group.setMemCapacityMB(memory);
      }

      Float swapRatio = ngEntity.getSwapRatio();
      if (swapRatio != null && swapRatio > 0) {
         group.setSwapRatio(swapRatio);
      }

      if (ngEntity.getNodeType() != null) {
         group.setInstanceType(ngEntity.getNodeType());
      }

      group.setInstanceNum(ngEntity.getDefineInstanceNum());

      Integer instancePerHost = ngEntity.getInstancePerHost();
      Set<NodeGroupAssociation> associonEntities =
            ngEntity.getGroupAssociations();
      String ngRacks = ngEntity.getGroupRacks();
      if (instancePerHost == null
            && (associonEntities == null || associonEntities.isEmpty())
            && ngRacks == null) {
         group.setPlacementPolicies(null);
      } else {
         PlacementPolicy policies = new PlacementPolicy();
         policies.setInstancePerHost(instancePerHost);
         if (ngRacks != null) {
            policies.setGroupRacks((GroupRacks) new Gson().fromJson(ngRacks,
                  GroupRacks.class));
         }
         if (associonEntities != null) {
            List<GroupAssociation> associons =
                  new ArrayList<GroupAssociation>(associonEntities.size());
            for (NodeGroupAssociation ae : associonEntities) {
               GroupAssociation a = new GroupAssociation();
               a.setReference(ae.getReferencedGroup());
               a.setType(ae.getAssociationType());
               associons.add(a);
            }
            policies.setGroupAssociations(associons);
         }

         group.setPlacementPolicies(policies);
      }

      String rps = ngEntity.getVcRpNames();
      if (rps != null && rps.length() > 0) {
         logger.debug("resource pool specified at node group "
               + ngEntity.getName());
         String[] rpNames = gson.fromJson(rps, String[].class);
         List<VcCluster> vcClusters =
               rpMgr.getVcResourcePoolByNameList(rpNames);
         group.setVcClusters(vcClusters);
         group.setRpNames(Arrays.asList(rpNames));
      }

      expandGroupStorage(ngEntity, group);
      group.setHaFlag(ngEntity.getHaFlag());
      group.setLatencySensitivity(ngEntity.getLatencySensitivity());
      group.setReservedCpu_ratio(ngEntity.getReservedCpu_ratio());
      group.setReservedMem_ratio(ngEntity.getReservedMem_ratio());
      if (ngEntity.getHadoopConfig() != null) {
         Map<String, Object> hadoopConfig =
               (new Gson()).fromJson(ngEntity.getHadoopConfig(), Map.class);
         group.setConfiguration(hadoopConfig);
      }

      group.setVmFolderPath(ngEntity.getVmFolderPath());
      return group;
   }

   private void expandGroupStorage(NodeGroupEntity ngEntity,
         NodeGroupCreate group) {
      int storageSize = ngEntity.getStorageSize();
      DatastoreType storageType = ngEntity.getStorageType();

      List<String> storeNames = ngEntity.getVcDatastoreNameList();
      List<String> dataDiskStoreNames = ngEntity.getDdDatastoreNameList();
      List<String> systemDiskStoreNames = ngEntity.getSdDatastoreNameList();

      logger.debug("expanding group storage for cluster " + ngEntity.getCluster().getName());
      if (storageSize <= 0 && storageType == null
            && (storeNames == null || storeNames.isEmpty())) {
         logger.debug("no storage specified for node group "
               + ngEntity.getName());
      }

      logger.debug("storage size is " + storageSize + " for node group "
            + ngEntity.getName());
      logger.debug("storage type is " + storageType + " for node group "
            + ngEntity.getName());
      logger.debug("diskNum is " + ngEntity.getDiskNum() + " for node group "
            + ngEntity.getName());
      logger.debug("shareDatastore is " + ngEntity.isShareDatastore() + " for node group "
            + ngEntity.getName());
      logger.debug("storage name pattern is " + storeNames + " for node group "
            + ngEntity.getName());
      logger.debug("system disk storage name pattern is "
            + systemDiskStoreNames + " for node group " + ngEntity.getName());
      logger.debug("data disk storage name pattern is " + dataDiskStoreNames
            + " for node group " + ngEntity.getName());
      StorageRead storage = new StorageRead();
      group.setStorage(storage);
      storage.setSizeGB(storageSize);
      storage.setDiskNum(ngEntity.getDiskNum());
      storage.setShareDatastore(ngEntity.isShareDatastore());
      if (storageType != null) {
         storage.setType(storageType.toString().toLowerCase());
      }

      if (systemDiskStoreNames != null && !systemDiskStoreNames.isEmpty()) {
         // when getDatastoreNamePattern return null, the ImagestoreNamePattern will be set to proper
         // values in NodeGroupCreate.getImagestoreNamePattern()
         storage.setImagestoreNamePattern(getDatastoreNamePattern(systemDiskStoreNames));
         storage.setDsNames4System(systemDiskStoreNames);
      } else {
         storage.setImagestoreNamePattern(getDatastoreNamePattern(storeNames));
      }

      if (dataDiskStoreNames != null && !dataDiskStoreNames.isEmpty()) {
         // when getDatastoreNamePattern return null, the DiskstoreNamePattern will be set to proper
         // values in NodeGroupCreate.getDiskstoreNamePattern()
         storage.setDiskstoreNamePattern(getDatastoreNamePattern(dataDiskStoreNames));
         storage.setDsNames4Data(dataDiskStoreNames);
      } else {
         storage.setDiskstoreNamePattern(getDatastoreNamePattern(storeNames));
      }
      storage.setShares(ngEntity.getCluster().getIoShares());

      // set storage split policy based on group roles
      SoftwareManager softwareManager =
            getSoftwareManager(ngEntity.getCluster().getAppManager());
      if (softwareManager.twoDataDisksRequired(group.toNodeGroupInfo())) {
         logger.debug("use bi_sector disk layout for zookeeper only group.");
         storage.setSplitPolicy(DiskSplitPolicy.BI_SECTOR);
      } else {
         if (storage.getType().equalsIgnoreCase(DatastoreType.LOCAL.toString())) {
            logger.debug("use even split disk layout for local datastore.");
            storage.setSplitPolicy(DiskSplitPolicy.EVEN_SPLIT);
         } else {
            logger.debug("use aggregate split disk layout.");
            storage.setSplitPolicy(DiskSplitPolicy.AGGREGATE);
         }
      }

      // set disk scsi controller type
      setDiskAttributes(storageType, storage, storeNames);
   }

   private void setDiskAttributes(DatastoreType storageType,
         StorageRead storage, List<String> storeNames) {
      if (storageType == null) {
         // check store names to see if local type storage is chosen.
         Set<String> storePattern =
               datastoreMgr.getLocalDatastoresByNames(storeNames);
         if (storePattern != null && !storePattern.isEmpty()) {
            logger.info("datastore type is not set, but local datastore is used. Set scsi controller type to paravirtual");
            storage
                  .setControllerType(DiskScsiControllerType.PARA_VIRTUAL_CONTROLLER);
            storage.setAllocType(AllocationType.THICK.name());
         } else {
            storage.setControllerType(DiskScsiControllerType.LSI_CONTROLLER);
            storage.setAllocType(AllocationType.THIN.name());
         }
         return;
      }
      if (storageType != DatastoreType.LOCAL) {
         // if storage type is specified, set controller type based on storage type
         storage.setControllerType(DiskScsiControllerType.LSI_CONTROLLER);
         storage.setAllocType(AllocationType.THIN.name());
      } else {
         storage
               .setControllerType(DiskScsiControllerType.PARA_VIRTUAL_CONTROLLER);
         storage.setAllocType(AllocationType.THICK.name());
      }
   }

   private List<String> getDatastoreNamePattern(List<String> storeNames) {
      if (storeNames == null || storeNames.isEmpty()) {
         return null;
      }
      Set<String> storePattern = null;
      storePattern = datastoreMgr.getDatastoresByNames(storeNames);
      if (storePattern == null || storePattern.isEmpty()) {
         String datastoreNames = new Gson().toJson(storeNames);
         logger.error("datastore " + datastoreNames + " not found in BDE");
         throw BddException.NOT_FOUND("datastore", datastoreNames);
      }
      return new ArrayList<>(storePattern);
   }

   @Transactional
   public void updateAppConfig(String clusterName, ClusterCreate clusterCreate) {
      logger.debug("Update configuration for cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }
      SoftwareManager softwareManager =
            getSoftwareManager(cluster.getAppManager());
      // read distro and distroVersion from ClusterEntity and set to ClusterCreate
      clusterCreate.setDistro(cluster.getDistro());
      clusterCreate.setDistroVersion(cluster.getDistroVersion());
      if (!CommonUtil.isBlank(cluster.getAdvancedProperties())) {
         Gson gson = new Gson();
         Map<String, String> advancedProperties =
               gson.fromJson(cluster.getAdvancedProperties(), Map.class);
         clusterCreate.setExternalHDFS(advancedProperties.get("ExternalHDFS"));
         clusterCreate.setExternalMapReduce(advancedProperties
               .get("ExternalMapReduce"));
         clusterCreate.setLocalRepoURL(advancedProperties.get("LocalRepoURL"));
         clusterCreate.setClusterCloneType(advancedProperties.get("ClusterCloneType"));
      }

      // for clusters from previous releases, the clone type is fast clone
      setDefaultClusterCloneType(clusterCreate);

      // only check roles validity in server side, but not in CLI and GUI, because roles info exist in server side.
      ClusterBlueprint blueprint = clusterCreate.toBlueprint();
      try {
         softwareManager.validateBlueprint(blueprint);
      } catch (ValidationException e) {
         throw ClusterConfigException.INVALID_SPEC(e.getFailedMsgList());
      }

      updateInfrastructure(clusterCreate, softwareManager, blueprint);
      Map<String, Object> clusterLevelConfig = clusterCreate.getConfiguration();

      if (clusterLevelConfig != null && clusterLevelConfig.size() > 0) {
         logger.debug("Cluster level app config is updated.");
         cluster.setHadoopConfig((new Gson()).toJson(clusterLevelConfig));
         updateVhmJobTrackerPort(clusterCreate, cluster);
      } else {
         logger.debug("cluster configuration is not set in cluster spec, so treat it as an empty configuration.");
         cluster.setHadoopConfig(null);
      }
      setAdvancedProperties(clusterCreate.getExternalHDFS(),
            clusterCreate.getExternalMapReduce(),
            clusterCreate.getLocalRepoURL(),
            clusterCreate.getExternalNamenode(),
            clusterCreate.getExternalSecondaryNamenode(),
            clusterCreate.getExternalDatanodes(),
            clusterCreate.getClusterCloneType(),
            cluster);
      updateNodegroupAppConfig(clusterCreate, cluster,
            clusterCreate.isValidateConfig());
   }

   private void updateNodegroupAppConfig(ClusterCreate clusterCreate,
         ClusterEntity cluster, boolean validateWhiteList) {
      Gson gson = new Gson();
      List<NodeGroupEntity> groupEntities = cluster.getNodeGroups();
      Map<String, NodeGroupEntity> groupMap =
            new HashMap<String, NodeGroupEntity>();
      for (NodeGroupEntity entity : groupEntities) {
         groupMap.put(entity.getName(), entity);
      }

      Set<String> updatedGroups = new HashSet<String>();
      NodeGroupCreate[] groupCreates = clusterCreate.getNodeGroups();
      if (groupCreates == null) {
         return;
      }
      for (NodeGroupCreate groupCreate : groupCreates) {
         Map<String, Object> groupConfig = groupCreate.getConfiguration();
         if (groupConfig != null && groupConfig.size() > 0) {
            NodeGroupEntity groupEntity = groupMap.get(groupCreate.getName());
            // validate hadoop config
            groupEntity.setHadoopConfig(gson.toJson(groupConfig));
            updatedGroups.add(groupCreate.getName());
         }
      }
      for (NodeGroupEntity entity : groupEntities) {
         if (updatedGroups.contains(entity.getName())) {
            continue;
         }
         entity.setHadoopConfig(null);
      }
   }

   /**
    * validate if rack topology of all hosts is uploaded
    * 
    * @param hosts
    * @param topology
    */
   @Transactional
   public void validateRackTopologyUploaded(Set<String> hosts, String topology) {
      Map<String, String> rackMap = rackInfoMgr.exportHostRackMap();
      List<String> invalidHosts = new ArrayList<String>();
      for (String hostName : hosts) {
         if (!rackMap.containsKey(hostName)) {
            invalidHosts.add(hostName);
         }
      }
      if (invalidHosts.size() > 0) {
         if (topology.equalsIgnoreCase(TopologyType.HVE.toString())
               || topology.equalsIgnoreCase(TopologyType.RACK_AS_RACK
                     .toString())) {
            throw ClusterConfigException
                  .TOPOLOGY_WITH_NO_MAPPING_INFO_EXIST(topology);
         }
      }
   }

   /**
    * build rack topology of the nodes according to the topology
    * 
    * @param nodes
    * @param topology
    * @return
    */
   @Transactional
   public Map<String, String> buildTopology(List<NodeRead> nodes,
         String topology) {
      topology = topology.toUpperCase();
      Map<String, String> map = new HashMap<String, String>();
      Map<NetTrafficType, List<IpConfigInfo>> ipConfigMap = null;
      Map<String, String> hostRackMap = rackInfoMgr.exportHostRackMap();
      Iterator<Entry<NetTrafficType, List<IpConfigInfo>>> ipConfigsIt = null;
      Entry<NetTrafficType, List<IpConfigInfo>> ipConfigEntry = null;
      List<IpConfigInfo> ipConfigs = null;
      for (NodeRead node : nodes) {
         ipConfigMap = node.getIpConfigs();
         ipConfigsIt = ipConfigMap.entrySet().iterator();
         while (ipConfigsIt.hasNext()) {
            ipConfigEntry = ipConfigsIt.next();
            ipConfigs = ipConfigEntry.getValue();
            if (ipConfigs != null && ipConfigs.size() > 0) {
               for (IpConfigInfo ipConfig : ipConfigs) {
                  if (!CommonUtil.isBlank(ipConfig.getIpAddress())) {
                     switch (topology) {
                     case "HOST_AS_RACK":
                        map.put(ipConfig.getIpAddress(),
                              "/" + node.getHostName());
                        break;
                     case "RACK_AS_RACK":
                        map.put(ipConfig.getIpAddress(),
                              "/" + hostRackMap.get(node.getHostName()));
                        break;
                     case "HVE":
                        map.put(ipConfig.getIpAddress(),
                              "/" + hostRackMap.get(node.getHostName()) + "/"
                                    + node.getHostName());
                        break;
                     case "NONE":
                     default:
                        map.put(ipConfig.getIpAddress(), "/default-rack");
                        break;
                     }
                  }
               }
            }
         }
      }
      return map;
   }

   public SoftwareManager getSoftwareManager(String appManager) {
      return softwareManagerCollector.getSoftwareManager(appManager);
   }

   private boolean validateLocalRepoURL(String localRepoURL) {
      boolean succ = true;
      HttpClientBuilder builder = HttpClientBuilder.create();
      CloseableHttpClient httpClient = builder.build();

      // test the connection to the given url
      try {
         HttpGet httpGet = new HttpGet(localRepoURL);
         HttpResponse resp = httpClient.execute(httpGet);
         StatusLine status = resp.getStatusLine();
         if (status.getStatusCode() >= 400) {
            succ = false;
         }
      } catch (Exception e) {
         succ = false;
         logger.error(e.getMessage());
      } finally {
         if (null != httpClient) {
            try {
               httpClient.close();
            } catch (IOException e) {
               logger.error("Unknown errors in closing the http connection.");
            }
         }
      }

      return succ;
   }

   private void setDefaultClusterCloneType(ClusterCreate clusterCreate) {
      String cloneType = clusterCreate.getClusterCloneType();
      if (CommonUtil.isBlank(cloneType)) {
         clusterCreate.setClusterCloneType(Constants.CLUSTER_CLONE_TYPE_FAST_CLONE);
      }
   }

   /*
    * Get the disks number for each node group specified in cluster spec file or in global settings.
    * The total storage of a node splits into the specified number of disks.
    * 0 by default which means using the default storage split policy defined in PlacementPlanner#placeDisk().
    */
   private int getDiskNumberForNodeGroup(NodeGroupCreate group) {
      String storageType = null;
      Integer diskNumber = null;
      if (group.getStorage() != null) {
         storageType = group.getStorage().getType();
         diskNumber = group.getStorage().getDiskNum();
      }
      if (storageType == null) {
         storageType = DatastoreType.LOCAL.name();
      }
      if (diskNumber != null) {
         return diskNumber;
      } else {
         return Configuration.getInt(String.format("storage.%1$s.disk_number_per_node", storageType.toLowerCase()), 0);
      }
   }

   /*
    * Get the shareDatastore setting for each node group specified in cluster spec file or in global settings.
    * true by default which means sharing a datastore with other disks rather than allocating the whole datastore to a single disk.
    */
   private boolean isShareDatastoreForNodeGroup(NodeGroupCreate group) {
      String storageType = null;
      Boolean isShareDatastore = null;
      if (group.getStorage() != null) {
         storageType = group.getStorage().getType();
         isShareDatastore = group.getStorage().isShareDatastore();
      }
      if (storageType == null) {
         storageType = DatastoreType.LOCAL.name();
      }
      if (isShareDatastore != null) {
         return isShareDatastore;
      } else {
         return Configuration.getBoolean(String.format("storage.%1$s.is_share_datastore", storageType.toLowerCase()), true);
      }
   }
}
