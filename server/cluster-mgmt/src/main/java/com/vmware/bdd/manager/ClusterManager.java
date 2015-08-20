/***************************************************************************

 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package com.vmware.bdd.manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import com.vmware.bdd.service.resmgmt.IVcInventorySyncService;
import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.vcservice.VcContext;
import com.vmware.bdd.aop.annotation.ClusterManagerPointcut;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterStatus;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.apitypes.LimitInstruction;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.VcClusterMap;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NetworkEntity;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.entity.VcResourcePoolEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.exception.ClusterHealServiceException;
import com.vmware.bdd.exception.ClusterManagerException;
import com.vmware.bdd.exception.VcProviderException;
import com.vmware.bdd.exception.WarningMessageException;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.IClusterHealService;
import com.vmware.bdd.service.IClusteringService;
import com.vmware.bdd.service.IExecutionService;
import com.vmware.bdd.service.impl.ClusterUserMgmtValidService;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.resmgmt.INetworkService;
import com.vmware.bdd.service.resmgmt.INodeTemplateService;
import com.vmware.bdd.service.resmgmt.IResourceService;
import com.vmware.bdd.service.utils.VcResourceUtils;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.specpolicy.ClusterSpecFactory;
import com.vmware.bdd.specpolicy.CommonClusterExpandPolicy;
import com.vmware.bdd.spectypes.IronfanStack;
import com.vmware.bdd.spectypes.VcCluster;
import com.vmware.bdd.usermgmt.UserMgmtConstants;
import com.vmware.bdd.utils.AuAssert;
import com.vmware.bdd.utils.ClusterUtil;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.JobUtils;
import com.vmware.bdd.utils.ValidationUtils;
import com.vmware.bdd.utils.Version;

public class ClusterManager {
   static final Logger logger = Logger.getLogger(ClusterManager.class);

   private ClusterConfigManager clusterConfigMgr;

   private INetworkService networkManager;

   private JobManager jobManager;

   private IResourceService resMgr;

   private IClusterEntityManager clusterEntityMgr;

   private RackInfoManager rackInfoMgr;

   private IClusteringService clusteringService;

   private IClusterHealService clusterHealService;

   private IExecutionService executionService;
   private SoftwareManagerCollector softwareManagerCollector;

   private ShrinkManager shrinkManager;

   @Autowired
   private INodeTemplateService nodeTemplateService;

   @Autowired
   private ClusterUserMgmtValidService clusterUserMgmtValidService;

   @Autowired
   private VcResourceFilterBuilder vcResourceFilterBuilder;

   @Autowired
   private UnsupportedOpsBlocker opsBlocker;

   @Autowired
   private IVcInventorySyncService syncService;

   private static boolean extraPackagesExisted = false;
   private static HashSet<String> extraRequiredPackages = getExtraRequiredPackages();
   private static final String commRegex = "-[0-9]+\\.[0-9]+.*\\.rpm";

   private static HashSet<String> getExtraRequiredPackages() {
      HashSet<String> hs = new HashSet<String>();
      String extraPackStr =
            Configuration.getString(
                  Constants.SERENGETI_YUM_EXTRA_PACKAGES_CONFIG,
                  Constants.SERENGETI_YUM_EXTRA_PACKAGES).trim();
      if (!extraPackStr.isEmpty()) {
         String[] packs = extraPackStr.split(",");
         hs.addAll(Arrays.asList(packs));
      }
      return hs;
   }

   public JobManager getJobManager() {
      return jobManager;
   }

   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }

   public ClusterConfigManager getClusterConfigMgr() {
      return clusterConfigMgr;
   }

   public void setClusterConfigMgr(ClusterConfigManager clusterConfigMgr) {
      this.clusterConfigMgr = clusterConfigMgr;
   }

   public INetworkService getNetworkManager() {
      return networkManager;
   }

   public void setNetworkManager(INetworkService networkManager) {
      this.networkManager = networkManager;
   }

   public IResourceService getResMgr() {
      return resMgr;
   }

   @Autowired
   public void setResMgr(IResourceService resMgr) {
      this.resMgr = resMgr;
   }

   public IClusterEntityManager getClusterEntityMgr() {
      return clusterEntityMgr;
   }

   @Autowired
   public void setClusterEntityMgr(IClusterEntityManager clusterEntityMgr) {
      this.clusterEntityMgr = clusterEntityMgr;
   }

   public RackInfoManager getRackInfoMgr() {
      return rackInfoMgr;
   }

   @Autowired
   public void setRackInfoMgr(RackInfoManager rackInfoMgr) {
      this.rackInfoMgr = rackInfoMgr;
   }

   public IClusteringService getClusteringService() {
      return clusteringService;
   }

   @Autowired
   public void setClusteringService(IClusteringService clusteringService) {
      this.clusteringService = clusteringService;
   }

   public IClusterHealService getClusterHealService() {
      return clusterHealService;
   }

   @Autowired
   public void setClusterHealService(IClusterHealService clusterHealService) {
      this.clusterHealService = clusterHealService;
   }

   public IExecutionService getExecutionService() {
      return executionService;
   }

   @Autowired
   public void setExecutionService(IExecutionService executionService) {
      this.executionService = executionService;
   }

   @Autowired
   public void setSoftwareManagerCollector(SoftwareManagerCollector softwareManagerCollector) {
        this.softwareManagerCollector = softwareManagerCollector;
   }

   @Autowired
   public void setShrinkManager(ShrinkManager shrinkManager) {
      this.shrinkManager = shrinkManager;
   }

   public Map<String, Object> getClusterConfigManifest(String clusterName,
         List<String> targets, boolean needAllocIp) {
      ClusterCreate clusterConfig =
            clusterConfigMgr.getClusterConfig(clusterName, needAllocIp);
      Map<String, String> cloudProvider = resMgr.getCloudProviderAttributes();

      ClusterRead read = clusterEntityMgr.toClusterRead(clusterName, true);

      Map<String, Object> attrs = new HashMap<String, Object>();

      if (Constants.IRONFAN.equalsIgnoreCase(clusterConfig.getAppManager())) {
         SoftwareManager softwareManager = clusterConfigMgr.getSoftwareManager(clusterConfig.getAppManager());
         IronfanStack stack = (IronfanStack)filterDistroFromAppManager(softwareManager, clusterConfig.getDistro());
         CommonClusterExpandPolicy.expandDistro(clusterConfig, stack);
         
         attrs.put("cloud_provider", cloudProvider);
         attrs.put("cluster_definition", clusterConfig);         
      }

      if (read != null) {
         attrs.put("cluster_data", read);
      }
      if (targets != null && !targets.isEmpty()) {
         attrs.put("targets", targets);
      }
      return attrs;
   }

   private void writeJsonFile(Map<String, Object> clusterConfig, File file) {
      Gson gson =
            new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                  .setPrettyPrinting().create();
      String jsonStr = gson.toJson(clusterConfig);

      AuAssert.check(jsonStr != null);
      logger.debug("writing cluster manifest in json " + jsonStr + " to file "
            + file);
      BufferedWriter out = null;
      try {
         out =
               new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                     file), "UTF-8"));
         out.write(jsonStr);
      } catch (IOException ex) {
         logger.error(ex.getMessage()
               + "\n failed to write cluster manifest to file " + file);
         throw BddException.INTERNAL(ex, "Failed to write cluster manifest.");
      } finally {
         if (out != null) {
            try {
               out.close();
            } catch (IOException e) {
               logger.error("falied to close writer" + out, e);
            }
         }
      }
   }

   public File writeClusterSpecFile(String targetName, File workDir,
         boolean needAllocIp) {
      String clusterName = targetName.split("-")[0];
      String fileName = clusterName + ".json";
      List<String> targets = new ArrayList<String>(1);
      targets.add(targetName);
      Map<String, Object> clusterConfig =
            getClusterConfigManifest(clusterName, targets, needAllocIp);

      File file = new File(workDir, fileName);
      writeJsonFile(clusterConfig, file);

      return file;
   }

   private void refreshClusterStatus(String clusterName) {
      List<String> clusterNames = new ArrayList<String>();
      clusterNames.add(clusterName);
      refreshClusterStatus(clusterNames);
   }

   private void refreshClusterStatus(List<String> clusterNames) {
      for (String clusterName : clusterNames) {
         Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
         param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(
               new Date()));
         param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
               clusterName));
         JobParameters jobParameters = new JobParameters(param);
         try {
            long jobExecutionId =
                  jobManager.runJob(JobConstants.QUERY_CLUSTER_JOB_NAME,
                        jobParameters);
            TaskRead status = jobManager.getJobExecutionStatus(jobExecutionId);
            while (status.getStatus() != TaskRead.Status.COMPLETED
                  && status.getStatus() != TaskRead.Status.FAILED
                  && status.getStatus() != TaskRead.Status.ABANDONED
                  && status.getStatus() != TaskRead.Status.STOPPED) {
               Thread.sleep(1000);
               status = jobManager.getJobExecutionStatus(jobExecutionId);
            }
         } catch (Exception ex) {
            logger.error("failed to run query cluster job: " + clusterName, ex);
         }
      }
   }

   public ClusterRead getClusterByName(String clusterName, boolean realTime) {
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      // return the latest data from db
      if (realTime
            && cluster.getStatus().isSyncServiceStatus()) {
            // for not running cluster, we don't sync up status from chef
         refreshClusterStatus(clusterName);
      }

      return clusterEntityMgr.toClusterRead(clusterName);
   }

   public ClusterCreate getClusterSpec(String clusterName) {
      ClusterCreate spec = clusterConfigMgr.getClusterConfig(clusterName);
      spec.setVcClusters(null);
      spec.setDistroMap(null);
      spec.setSharedDatastorePattern(null);
      spec.setLocalDatastorePattern(null);
      spec.setNetworkings(null);
      spec.setNetworkConfig(null);
      spec.setName(null);
      spec.setValidateConfig(null);
      spec.setSpecFile(null);
      spec.setTopologyPolicy(null);
      spec.setHostToRackMap(null);
      spec.setHttpProxy(null);
      spec.setNoProxy(null);
      spec.setDistroVendor(null);
      spec.setDistroVersion(null);
      spec.setPassword(null);
      spec.setHostnamePrefix(null);
      NodeGroupCreate[] groups = spec.getNodeGroups();
      if (groups != null) {
         for (NodeGroupCreate group : groups) {
            group.setVcClusters(null);
            group.getStorage().setImagestoreNamePattern(null);
            group.getStorage().setDiskstoreNamePattern(null);
            group.setVmFolderPath(null);
            group.getStorage().setSplitPolicy(null);
            group.getStorage().setControllerType(null);
            group.getStorage().setAllocType(null);
            if (group.getPlacementPolicies() != null) {
               List<GroupAssociation> associations =
                     group.getPlacementPolicies().getGroupAssociations();
               if (associations != null && associations.isEmpty()) {
                  group.getPlacementPolicies().setGroupAssociations(null);
               }
            }
         }
      }
      return spec;
   }

   public List<ClusterRead> getClusters(Boolean realTime) {
      List<ClusterRead> clusters = new ArrayList<ClusterRead>();
      List<ClusterEntity> clusterEntities = clusterEntityMgr.findAllClusters();
      for (ClusterEntity entity : clusterEntities) {
         clusters.add(getClusterByName(entity.getName(), realTime));
      }
      return clusters;
   }

   @ClusterManagerPointcut
   public Long createCluster(ClusterCreate createSpec, org.apache.commons.configuration.Configuration newParams) throws Exception {
      logger.debug("entering createCluster: " + createSpec.getName());

      SoftwareManager softMgr = softwareManagerCollector.getSoftwareManager(createSpec.getAppManager());
      // @ Todo if specify hadoop stack, we can get hadoop stack by stack name. Otherwise, we will get a default hadoop stack.
      HadoopStack stack = clusterConfigMgr.filterDistroFromAppManager(softMgr, createSpec.getDistro());

      // if the distro is not specified by the REST Client, add it.
      if(CommonUtil.isBlank(createSpec.getDistro())) {
         createSpec.setDistro(stack.getDistro());
      }
      createSpec.setDistroVendor(stack.getVendor());
      createSpec.setDistroVersion(stack.getFullVersion());
      if (createSpec.getType() == null) {
         createSpec.setType(ClusterType.DEFAULT);
      }

      // create auto rps if vc cluster/rp is specified
      createAutoRps(createSpec);
      ClusterCreate clusterSpec =
            ClusterSpecFactory.getCustomizedSpec(createSpec, softMgr.getType());
      verifyRequiredPackages(clusterSpec);
      clusterSpec.verifyClusterNameLength();
      clusterSpec.validateNodeGroupNames();
      //Check the cpu, memory max configuration according vm hardware version
      if (clusterSpec != null && clusterSpec.getNodeGroups() != null) {
         for (NodeGroupCreate ng : clusterSpec.getNodeGroups()) {
            String templateVmId = this.nodeTemplateService.getNodeTemplateIdByName(clusterSpec.getTemplateName());
            if (templateVmId != null) {
               VcResourceUtils.checkVmMaxConfiguration(templateVmId,
                     ng.getCpuNum() == null ? 0 : ng.getCpuNum(),
                     ng.getMemCapacityMB() == null ? 0 : ng.getMemCapacityMB());
            }
         }
      }
      String name = clusterSpec.getName();

      validateInfraConfig(clusterSpec);
      if (softMgr.getType().equalsIgnoreCase(Constants.CLOUDERA_MANAGER_PLUGIN_TYPE)) {
         validateServiceUserAndGroupsInLdap(clusterSpec);
      }
      logger.info("start to create a cluster: " + name);

      List<String> dsNames = getUsedDS(clusterSpec.getDsNames());
      if (dsNames.isEmpty()) {
         throw ClusterConfigException.NO_DATASTORE_ADDED();
      }
      List<VcCluster> vcClusters = getUsedVcClusters(clusterSpec.getRpNames());
      if (vcClusters == null || vcClusters.isEmpty()) {
         throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
      }

      if(logger.isDebugEnabled()) {
         logger.debug("skipRefreshVc: " + newParams.getBoolean(Constants.SKIP_REFRESH_VC));
      }

      if(BooleanUtils.toBoolean(newParams.getBoolean(Constants.SKIP_REFRESH_VC))) {
         logger.info("skip refresh vc resources.");
      } else {
         VcResourceFilters filters = vcResourceFilterBuilder.build(dsNames,
               getRpNames(clusterSpec.getRpNames()), createSpec.getNetworkNames());
         syncService.refreshInventory(filters);
      }

      // validate accessibility
      validateDatastore(dsNames, vcClusters);
      validateNetworkAccessibility(createSpec.getName(), createSpec.getNetworkNames(), vcClusters);

      // get the current cluster clone type from the configuration file
      // if it is not set in configuration file, then use INSTANT clone for VC6/ESXi6
      String type = getClusterCloneType();
      clusterSpec.setClusterCloneType(type);

      //save configuration into meta-db, and extend configuration using default spec
      clusterConfigMgr.createClusterConfig(clusterSpec);

      clusterEntityMgr.updateClusterStatus(name, ClusterStatus.PROVISIONING);
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.PROVISION_ERROR.name()));
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            createSpec.getName()));
      param.put(JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM, new JobParameter(
            JobConstants.CLUSTER_NODE_SCOPE_VALUE));
      JobParameters jobParameters = new JobParameters(param);
      return jobManager.runJob(JobConstants.CREATE_CLUSTER_JOB_NAME,
            jobParameters);
   }

   private Map<String, Set<String>> getServiceGroupUsers(ClusterCreate clusterSpec) {
      Map<String, Set<String>> groupUsers = new HashMap<>();
      Map<String, Object> configuration = clusterSpec.getConfiguration();
      if (MapUtils.isNotEmpty(configuration)) {
         Map<String, Map<String, String>> serviceUsersConfigs = (Map<String, Map<String, String>>)
               configuration.get(UserMgmtConstants.SERVICE_USER_CONFIG_IN_SPEC_FILE);
         if (MapUtils.isNotEmpty(serviceUsersConfigs)) {
            for (Map<String, String> serviceUserConfig : serviceUsersConfigs.values()) {
               String groupName = serviceUserConfig.get(UserMgmtConstants.SERVICE_USER_GROUP);
               String userName = serviceUserConfig.get(UserMgmtConstants.SERVICE_USER_NAME);
               if (groupName != null && !groupName.isEmpty() && userName != null && !userName.isEmpty()) {
                  if (groupUsers.get(groupName) == null) {
                     Set<String> users = new HashSet<>();
                     groupUsers.put(groupName, users);
                  }
                  groupUsers.get(groupName).add(userName);
               }
            }
         }
      }
      return groupUsers;
   }

   private void validateServiceUserAndGroupsInLdap(ClusterCreate clusterSpec) {
      Map<String, Set<String>> groupUsers = getServiceGroupUsers(clusterSpec);
      if (!groupUsers.isEmpty()) {
         logger.info("going to validate Ldap user and groups for: " + new Gson().toJson(groupUsers));
         clusterUserMgmtValidService.validateGroupUsers(UserMgmtConstants.DEFAULT_USERMGMT_SERVER_NAME, groupUsers);
         logger.info("validate service user in Ldap succeed");
      }
   }

   private void validateInfraConfig(ClusterCreate clusterSpec) {
      Map<String, Map<String, String>> infraConfig = clusterSpec.getInfrastructure_config();

      if(MapUtils.isEmpty(infraConfig)) {
         logger.info("no infra configuration in cluster create spec!");
         return;
      }

      Map<String, String> userMgmtCfg = infraConfig.get(UserMgmtConstants.LDAP_USER_MANAGEMENT);

      if(MapUtils.isEmpty(userMgmtCfg)) {
         logger.debug("no user management configuration section.");
      } else {
         clusterUserMgmtValidService.validateUserMgmtConfig(userMgmtCfg);

         logger.info("user management configuration validated successfully!");
      }
   }

   private void createAutoRps(ClusterCreate createSpec) {
      if (createSpec.getVcClusters() == null) {
         return;
      }
      // user specify vc resource pools directly, create auto rp in meta db dynamically
      List<VcCluster> vcClusters = createSpec.getVcClusters();
      List<String> rpNames =
            clusterConfigMgr.getRpMgr().addAutoResourcePools(vcClusters, true);
      logger.info("added automation resource pools: " + rpNames);
      createSpec.setRpNames(rpNames);
   }

   private void validateNetworkAccessibility(final String clusterName,
         List<String> networkList, List<VcCluster> clusters) {
      if (networkList == null || networkList.isEmpty()) {
         throw ClusterConfigException.NETWORK_IS_NOT_SPECIFIED(clusterName);
      }
      Set<String> networkNames = new HashSet<String>();
      networkNames.addAll(networkList);
      logger.info("start to validate network if exsit in db.");
      verifyNetworkNamesExsitInDB(networkNames, clusterName);
      logger.info("start to validate network accessibility.");
      if (!resMgr.isNetworkAccessibleByCluster(networkList, clusters)) {
         List<String> clusterNames = new ArrayList<String>();
         for (VcCluster cluster : clusters) {
            clusterNames.add(cluster.getName());
         }
         throw ClusterConfigException.NETWORK_UNACCESSIBLE(networkList,
               clusterNames);
      }
   }

   private void validateDatastore(List<String> dsNames, List<VcCluster> clusters) {
      // validate if there is any datastore is accessible by one cluster
      logger.info("start to validate accessibility for datastores: " + dsNames
            + ", and clusters: " + clusters);
      boolean found = false;
      for (String dsName : dsNames) {
         for (VcCluster vcCluster : clusters) {
            if (resMgr.isDatastoreAccessibleByCluster(dsName,
                  vcCluster.getName())) {
               found = true;
               break;
            }
         }
      }
      if (!found) {
         // no any datastore is accessible by specified cluster
         List<String> vcClusterNames = new ArrayList<String>();
         for (VcCluster vcCluster : clusters) {
            vcClusterNames.add(vcCluster.getName());
         }
         throw ClusterConfigException.DATASTORE_UNACCESSIBLE(vcClusterNames,
               dsNames);
      }
   }

   private List<String> getUsedDS(List<String> specifiedDsNames) {
      if (specifiedDsNames == null || specifiedDsNames.isEmpty()) {
         specifiedDsNames = new ArrayList<String>();
         specifiedDsNames.addAll(clusterConfigMgr.getDatastoreMgr()
               .getAllDatastoreNames());
      }
      return specifiedDsNames;
   }

   private List<String> getRpNames(List<String> rpNames) {
      if(CollectionUtils.isEmpty(rpNames)) {
         List<String> newRpNameList = new ArrayList<>();
         newRpNameList.addAll(clusterConfigMgr.getRpMgr().getAllRPNames());
         return newRpNameList;
      }

      return rpNames;
   }

   private List<VcCluster> getUsedVcClusters(List<String> rpNames) {
      List<VcCluster> clusters = null;
      if (rpNames == null || rpNames.isEmpty()) {
         clusters = clusterConfigMgr.getRpMgr().getAllVcResourcePool();
      } else {
         clusters = new ArrayList<VcCluster>();
         StringBuffer nonexistentRpNames = new StringBuffer();
         for (String rpName : rpNames) {
            List<VcCluster> vcClusters =
                  clusterConfigMgr.getRpMgr().getVcResourcePoolByName(rpName);

            if (vcClusters == null) {
               nonexistentRpNames.append(rpName).append(",");
            } else {
               clusters.addAll(vcClusters);
            }
         }
         if (nonexistentRpNames.length() > 0) {
            nonexistentRpNames.delete(nonexistentRpNames.length()-1, nonexistentRpNames.length());
            throw VcProviderException
                  .RESOURCE_POOL_NOT_FOUND(nonexistentRpNames.toString());
         }
      }
      return clusters;
   }

   @ClusterManagerPointcut
   public Long configCluster(String clusterName, ClusterCreate createSpec)
         throws Exception {
      opsBlocker.blockUnsupportedOpsByCluster("configCluster", clusterName);

      logger.info("ClusterManager, config cluster " + clusterName);
      ClusterEntity cluster;

      if ((cluster = clusterEntityMgr.findByName(clusterName)) == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      if (!cluster.getStatus().isActiveServiceStatus()
            && !ClusterStatus.CONFIGURE_ERROR.equals(cluster.getStatus())
            && !ClusterStatus.SERVICE_STOPPED.equals(cluster.getStatus())) {
         logger.error("can not config cluster: " + clusterName + ", "
               + cluster.getStatus());
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "To update a cluster, its status must be RUNNING, CONFIGURE_ERROR or SERVICE_ERROR");
      }
      clusterConfigMgr.updateAppConfig(clusterName, createSpec);

      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.CONFIGURE_ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName,
            ClusterStatus.CONFIGURING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.CONFIG_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to configure cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.CONFIGURE_ERROR);
         throw e;
      }
   }

   public long enableLdap(String clusterName) throws Exception {
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.CONFIGURE_ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName,
            ClusterStatus.CONFIGURING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob("configLdapUserMgmtJob",
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to configure cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.CONFIGURE_ERROR);
         throw e;
      }
   }

   public Long resumeClusterCreation(String clusterName, org.apache.commons.configuration.Configuration newParams) throws Exception {
      logger.info("ClusterManager, resume cluster creation " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      if (cluster.getStatus() != ClusterStatus.PROVISION_ERROR) {
         logger.error("can not resume creation of cluster: " + clusterName
               + ", " + cluster.getStatus());
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "To update a cluster, its status must be PROVISION_ERROR or SERVICE_ERROR");
      }
      List<String> dsNames = getUsedDS(cluster.getVcDatastoreNameList());
      if (dsNames.isEmpty()) {
         throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
      }
      List<VcCluster> vcClusters = getUsedVcClusters(cluster.getVcRpNameList());
      if (vcClusters.isEmpty()) {
         throw ClusterConfigException.NO_DATASTORE_ADDED();
      }

      if(logger.isDebugEnabled()) {
         logger.debug("skipRefreshVc: " + newParams.getBoolean(Constants.SKIP_REFRESH_VC));
      }

      if(BooleanUtils.toBoolean(newParams.getBoolean(Constants.SKIP_REFRESH_VC))) {
         logger.info("skip refresh vc resources.");
      } else {
         VcResourceFilters filters = vcResourceFilterBuilder.build(dsNames,
               getRpNames(cluster.getVcRpNameList()), cluster.fetchNetworkNameList());
         syncService.refreshInventory(filters);
      }

      // validate accessibility
      validateDatastore(dsNames, vcClusters);
      validateNetworkAccessibility(cluster.getName(), cluster.fetchNetworkNameList(), vcClusters);
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.PROVISION_ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName,
            ClusterStatus.PROVISIONING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.RESUME_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to resume cluster creation for cluster "
               + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.PROVISION_ERROR);
         throw e;
      }
   }

   @ClusterManagerPointcut
   public Long deleteClusterByName(String clusterName) throws Exception {
      logger.info("ClusterManager, deleting cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (!cluster.getStatus().isStableStatus()) {
         logger.error("cluster: " + clusterName
               + " cannot be deleted, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.DELETION_NOT_ALLOWED_ERROR(clusterName,
               "To delete a cluster, its status must be RUNNING, STOPPED, ERROR, PROVISION_ERROR, CONFIGURE_ERROR or UPGRADE_ERROR");
      }
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.DELETING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.DELETE_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to delete cluster " + clusterName, e);
         cluster = clusterEntityMgr.findByName(clusterName);
         if (cluster != null) {
            clusterEntityMgr.updateClusterStatus(clusterName,
                  ClusterStatus.ERROR);
         }
         throw e;
      }
   }

   @ClusterManagerPointcut
   public Long upgradeClusterByName(String clusterName) throws Exception {
      logger.info("ClusterManager, upgrading cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);

      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (!clusterEntityMgr.needUpgrade(clusterName)) {
         logger.error("cluster " + clusterName + " is the latest version already");
         throw ClusterManagerException.ALREADY_LATEST_VERSION_ERROR(clusterName);
      }

      if (!cluster.getStatus().isStableStatus()) {
         logger.error("cluster: " + clusterName
               + " cannot be upgraded, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.UPGRADE_NOT_ALLOWED_ERROR(clusterName,
               "To upgrade a cluster, its status must be RUNNING, STOPPED, ERROR, CONFIGURE_ERROR or UPGRADE_ERROR");
      }
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM, new JobParameter(ClusterStatus.UPGRADE_ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.storeClusterLastStatus(clusterName);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.UPGRADING);
      clusterEntityMgr.updateNodesActionForUpgrade(clusterName, Constants.NODE_ACTION_UPGRADING);
      clusterEntityMgr.cleanupErrorForClusterUpgrade(clusterName);
      try {
         return jobManager.runJob(JobConstants.UPGRADE_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to upgrade cluster " + clusterName, e);
         cluster = clusterEntityMgr.findByName(clusterName);
         if (cluster != null) {
            clusterEntityMgr.updateClusterStatus(clusterName,
                  ClusterStatus.UPGRADE_ERROR);
         }
         throw e;
      }
   }

   public Long startCluster(String clusterName, boolean force) throws Exception {
      logger.info("ClusterManager, starting cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      if (cluster.getStatus().isActiveServiceStatus()) {
         logger.error("cluster " + clusterName + " is running already");
         throw ClusterManagerException.ALREADY_STARTED_ERROR(clusterName);
      }

      if (!ClusterStatus.STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " cannot be started, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.START_NOT_ALLOWED_ERROR(clusterName,
               "To start a cluster, its status must be STOPPED or ERROR");
      }

      cluster.setVhmTargetNum(-1);
      clusterEntityMgr.update(cluster);
      clusterEntityMgr.cleanupActionError(clusterName);
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(Constants.FORCE_CLUSTER_OPERATION_JOB_PARAM, new JobParameter(String.valueOf(force)));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.STARTING);
      try {
         return jobManager.runJob(JobConstants.START_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to start cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.ERROR);
         throw e;
      }
   }

   @ClusterManagerPointcut
   public Long stopCluster(String clusterName) throws Exception {
      logger.info("ClusterManager, stopping cluster " + clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      if (ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName + " is stopped already");
         throw ClusterManagerException.ALREADY_STOPPED_ERROR(clusterName);
      }

      if (!cluster.getStatus().isActiveServiceStatus()
            && !ClusterStatus.SERVICE_STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.ERROR.equals(cluster.getStatus())
            && !ClusterStatus.SERVICE_WARNING.equals(cluster.getStatus())) {
         logger.error("cluster " + clusterName
               + " cannot be stopped, it is in " + cluster.getStatus()
               + " status");
         throw ClusterManagerException.STOP_NOT_ALLOWED_ERROR(clusterName,
               "To stop a cluster, its status must be RUNNING, ERROR, SERVICE_WARNING or SERVICE_STOPPED");
      }
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.STOPPED.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.ERROR.name()));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.STOPPING);
      clusterEntityMgr.cleanupActionError(clusterName);
      try {
         return jobManager.runJob(JobConstants.STOP_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to stop cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.ERROR);
         throw e;
      }
   }

   @ClusterManagerPointcut
   public Long resizeCluster(String clusterName, String nodeGroupName,
         int instanceNum, boolean force, org.apache.commons.configuration.Configuration newParams) throws Exception {
      logger.info("ClusterManager, updating node group " + nodeGroupName
            + " in cluster " + clusterName + " reset instance number to "
            + instanceNum);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      List<String> dsNames = getUsedDS(cluster.getVcDatastoreNameList());
      if (dsNames.isEmpty()) {
         throw ClusterConfigException.NO_DATASTORE_ADDED();
      }

      List<VcCluster> vcClusters = getUsedVcClusters(cluster.getVcRpNameList());
      if (vcClusters.isEmpty()) {
         throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
      }

      if(logger.isDebugEnabled()) {
         logger.debug("skipRefreshVc: " + newParams.getBoolean(Constants.SKIP_REFRESH_VC));
      }

      if(BooleanUtils.toBoolean(newParams.getBoolean(Constants.SKIP_REFRESH_VC))) {
         logger.info("skip refresh vc resources.");
      } else {
         VcResourceFilters filters = vcResourceFilterBuilder.build(dsNames,
               getRpNames(cluster.getVcRpNameList()), cluster.fetchNetworkNameList());
         syncService.refreshInventory(filters);
      }

      // validate accessibility
      validateDatastore(dsNames, vcClusters);
      validateNetworkAccessibility(cluster.getName(), cluster.fetchNetworkNameList(), vcClusters);

      NodeGroupEntity group =
            clusterEntityMgr.findByName(cluster, nodeGroupName);
      if (group == null) {
         logger.error("nodegroup " + nodeGroupName + " of cluster "
               + clusterName + " does not exist");
         throw ClusterManagerException.NODEGROUP_NOT_FOUND_ERROR(nodeGroupName);
      }

      AuAssert.check(!group.getRoleNameList().isEmpty(), "roles should not be empty");
      SoftwareManager softMgr =
            softwareManagerCollector
                  .getSoftwareManager(cluster.getAppManager());
      NodeGroupInfo groupInfo = clusterEntityMgr.toNodeGroupInfo(clusterName, nodeGroupName);
      List<String> unsupportedRoles = softMgr.validateRolesForScaleOut(groupInfo);
      if (!unsupportedRoles.isEmpty()) {
         logger.info("can not resize node group with role: " + unsupportedRoles);
         throw ClusterManagerException.ROLES_NOT_SUPPORTED(unsupportedRoles);
      }

      if (!cluster.getStatus().isActiveServiceStatus()) {
         logger.error("cluster " + clusterName
               + " can be resized only in RUNNING status, it is now in "
               + cluster.getStatus() + " status");
         throw ClusterManagerException.UPDATE_NOT_ALLOWED_ERROR(clusterName,
               "To update a cluster, its status must be RUNNING");
      }

      if (instanceNum < group.getDefineInstanceNum()) {
         try {
            softMgr.validateRolesForShrink(groupInfo);
            return shrinkManager.shrinkNodeGroup(clusterName, nodeGroupName, instanceNum);
         } catch (Exception e) {
            if (!(e instanceof ShrinkException)) {
               logger.error("Failed to shrink cluster " + clusterName, e);
               throw ShrinkException.SHRINK_NODE_GROUP_FAILED(e, clusterName, e.getMessage());
            }
         }
      }

      //when use force, we allow user to rerun resize to fix bad nodes
      if ((instanceNum == group.getDefineInstanceNum()) && !force) {
         logger.error("the new instanceNum " + instanceNum + " shouldn't be the same as the old one ");
         throw ClusterManagerException.NO_NEED_TO_RESIZE(clusterName, nodeGroupName, instanceNum);
      }

      Integer instancePerHost = group.getInstancePerHost();
      if (instancePerHost != null && instanceNum % instancePerHost != 0) {
         throw BddException
               .INVALID_PARAMETER(
                     "instance number",
                     new StringBuilder(100)
                           .append(instanceNum)
                           .append(
                                 ".instanceNum must be evenly divisible by instancePerHost")
                           .toString());
      }

      ValidationUtils.validHostNumber(clusterEntityMgr, group, instanceNum);
      ValidationUtils.hasEnoughHost(rackInfoMgr, clusterEntityMgr, group,
            instanceNum);

      int oldInstanceNum = group.getDefineInstanceNum();
      group.setDefineInstanceNum(instanceNum);

      clusterEntityMgr.update(group);
      clusterEntityMgr.cleanupActionError(clusterName);
      // create job
      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      param.put(JobConstants.GROUP_NAME_JOB_PARAM, new JobParameter(
            nodeGroupName));
      param.put(JobConstants.GROUP_INSTANCE_NEW_NUMBER_JOB_PARAM,
            new JobParameter(Long.valueOf(instanceNum)));
      param.put(JobConstants.GROUP_INSTANCE_OLD_NUMBER_JOB_PARAM,
            new JobParameter(Long.valueOf(oldInstanceNum)));
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.VERIFY_NODE_STATUS_SCOPE_PARAM, new JobParameter(
            JobConstants.GROUP_NODE_SCOPE_VALUE));
      param.put(Constants.FORCE_CLUSTER_OPERATION_JOB_PARAM, new JobParameter(String.valueOf(force)));
      JobParameters jobParameters = new JobParameters(param);
      clusterEntityMgr.updateClusterStatus(clusterName, ClusterStatus.UPDATING);
      try {
         return jobManager.runJob(JobConstants.RESIZE_CLUSTER_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to resize cluster " + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.RUNNING);
         group.setDefineInstanceNum(oldInstanceNum);
         clusterEntityMgr.update(group);
         throw e;
      }
   }

   /**
    * set cluster parameters synchronously
    *
    * @param clusterName
    * @param activeComputeNodeNum
    * @param minComputeNodeNum
    * @param maxComputeNodeNum
    * @param enableAuto
    * @param ioPriority
    * @return
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   public List<String> syncSetParam(String clusterName,
         Integer activeComputeNodeNum, Integer minComputeNodeNum, Integer maxComputeNodeNum,
         Boolean enableAuto, Priority ioPriority) throws Exception {

      //allow set ioshare only.
      if(enableAuto != null || activeComputeNodeNum != null ||
            maxComputeNodeNum != null || minComputeNodeNum != null) {
         opsBlocker.blockUnsupportedOpsByCluster("syncSetElasticity", clusterName);
      }

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      ClusterRead clusterRead = getClusterByName(clusterName, false);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      clusterEntityMgr.cleanupActionError(clusterName);
      //update vm ioshares
      if (ioPriority != null) {
         prioritizeCluster(clusterName, ioPriority);
      }

      // as prioritizeCluster will update clusterEntity, here we need to refresh to avoid overriding
      cluster = clusterEntityMgr.findByName(clusterName);

      if (enableAuto != null && enableAuto != cluster.getAutomationEnable()) {
         if (enableAuto && cluster.getDistroVendor().equalsIgnoreCase(Constants.MAPR_VENDOR)) {
            logger.error("cluster " + clusterName + " is a MAPR distro, which cannot be auto scaled");
            throw BddException.NOT_ALLOWED_SCALING("cluster", clusterName);
         }
         cluster.setAutomationEnable(enableAuto);
      }

      if (minComputeNodeNum != null
            && minComputeNodeNum != cluster.getVhmMinNum()) {
         cluster.setVhmMinNum(minComputeNodeNum);
      }

      if (maxComputeNodeNum != null
            && maxComputeNodeNum != cluster.getVhmMaxNum()) {
         cluster.setVhmMaxNum(maxComputeNodeNum);
      }

      List<String> nodeGroupNames = new ArrayList<String>();
      if ((enableAuto != null || minComputeNodeNum != null || maxComputeNodeNum != null || activeComputeNodeNum != null)
            && !clusterRead.validateSetManualElasticity(nodeGroupNames)) {
         throw BddException.INVALID_PARAMETER("cluster", clusterName);
      }

      if (activeComputeNodeNum != null) {
         if (!activeComputeNodeNum.equals(cluster.getVhmTargetNum())) {
            cluster.setVhmTargetNum(activeComputeNodeNum);
         }
      }

      //enableAuto is only set during cluster running status and
      //other elasticity attributes are only set during cluster running/stop status
      if ((enableAuto != null)
            && !cluster.getStatus().isActiveServiceStatus()) {
         logger.error("Cannot change elasticity mode, when cluster "
               + clusterName + " is in " + cluster.getStatus() + " status");
         throw ClusterManagerException.SET_AUTO_ELASTICITY_NOT_ALLOWED_ERROR(
               clusterName, "The cluster's status must be RUNNING");
      }
      if (!cluster.getStatus().isActiveServiceStatus()
            && !ClusterStatus.SERVICE_STOPPED.equals(cluster.getStatus())
            && !ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         logger.error("Cannot change elasticity parameters, when cluster "
               + clusterName + " is in " + cluster.getStatus() + " status");
         throw ClusterManagerException.SET_AUTO_ELASTICITY_NOT_ALLOWED_ERROR(
               clusterName, "The cluster's status must be RUNNING, SERVICE_WARNING, SERVICE_ERROR or STOPPED");
      }

      clusterEntityMgr.update(cluster);

      //update vhm extra config file
      if (enableAuto != null || minComputeNodeNum != null || maxComputeNodeNum != null) {
         boolean success =
               clusteringService.setAutoElasticity(clusterName, false);
         if (!success) {
				throw ClusterManagerException
						.FAILED_TO_SET_AUTO_ELASTICITY_ERROR(clusterName,
								"Could not update elasticity configuration file");
         }
      }

      //waitForManual if switch to Manual and targetNodeNum is null
      if (enableAuto != null && !enableAuto
            && cluster.getVhmTargetNum() == null) {
         JobUtils.waitForManual(clusterName, executionService);
      }

      return nodeGroupNames;
   }

   /**
    * set cluster parameters asynchronously
    *
    * @param clusterName
    * @param activeComputeNodeNum
    * @param minComputeNodeNum
    * @param maxComputeNodeNum
    * @param enableAuto
    * @param ioPriority
    * @return
    * @throws Exception
    */
   @ClusterManagerPointcut
   public Long asyncSetParam(String clusterName, Integer activeComputeNodeNum,
         Integer minComputeNodeNum, Integer maxComputeNodeNum, Boolean enableAuto,
         Priority ioPriority)
         throws Exception {
      //allow set ioshare only.
      if(enableAuto != null || activeComputeNodeNum != null ||
            maxComputeNodeNum != null || minComputeNodeNum != null) {
         opsBlocker.blockUnsupportedOpsByCluster("asyncSetElasticity", clusterName);
      }

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      syncSetParam(clusterName, activeComputeNodeNum, minComputeNodeNum, maxComputeNodeNum,
            enableAuto, ioPriority);

      ClusterRead cluster = getClusterByName(clusterName, false);
      // cluster must be running status
      if (!cluster.getStatus().isActiveServiceStatus()) {
         String msg = "Cluster "+ clusterName +" is not running.";
         logger.error(msg);
         throw ClusterManagerException
               .SET_MANUAL_ELASTICITY_NOT_ALLOWED_ERROR(msg);
      }

      Map<String, JobParameter> param = new TreeMap<String, JobParameter>();
      param.put(JobConstants.TIMESTAMP_JOB_PARAM, new JobParameter(new Date()));
      param.put(JobConstants.CLUSTER_NAME_JOB_PARAM, new JobParameter(
            clusterName));
      // TODO: transfer SET_TARGET/UNLIMIT from CLI directly
      if (activeComputeNodeNum == null) {
         param.put(JobConstants.VHM_ACTION_JOB_PARAM, new JobParameter(
               LimitInstruction.actionWaitForManual));
      } else if (activeComputeNodeNum == -1) {
         param.put(JobConstants.VHM_ACTION_JOB_PARAM, new JobParameter(
               LimitInstruction.actionUnlimit));
      } else {
         param.put(JobConstants.VHM_ACTION_JOB_PARAM, new JobParameter(
               LimitInstruction.actionSetTarget));
      }
      if (activeComputeNodeNum != null) {
         param.put(JobConstants.ACTIVE_COMPUTE_NODE_NUMBER_JOB_PARAM,
               new JobParameter(Long.valueOf(activeComputeNodeNum)));
      }
      param.put(JobConstants.CLUSTER_SUCCESS_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      param.put(JobConstants.CLUSTER_FAILURE_STATUS_JOB_PARAM,
            new JobParameter(ClusterStatus.RUNNING.name()));
      JobParameters jobParameters = new JobParameters(param);
      try {
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.VHM_RUNNING);
         return jobManager.runJob(JobConstants.SET_MANUAL_ELASTICITY_JOB_NAME,
               jobParameters);
      } catch (Exception e) {
         logger.error("Failed to set manual elasticity for cluster "
               + clusterName, e);
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.RUNNING);
         throw e;
      }
   }

   @ClusterManagerPointcut
   @Transactional
   public void updateCluster(ClusterCreate clusterUpdate, boolean ignoreWarning) throws Exception {
      String clusterName = clusterUpdate.getName();
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }
      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      List<String> newRpList = clusterUpdate.getRpNames();

      if (!CollectionUtils.isEmpty(newRpList)){
         //Check whether the new resourcepools are valid in vc_resource_pool
         List<String> existRPs =
               validateGivenRp(newRpList);
         if (CollectionUtils.isEmpty(existRPs)) {
            throw ClusterConfigException.NO_RESOURCE_POOL_ADDED();
         }

         //Check whether the new resourcepools include the current resourcepools which are used by this cluster
         Set<VcResourcePoolEntity> usedVCRps = cluster.getUsedRps();
         List<String> usedRpList =
               new ArrayList<String>(usedVCRps.size());
         for (VcResourcePoolEntity rp : usedVCRps) {
            usedRpList.add(rp.getName());
         }

         logger.info("Updating resourcepools for cluster " + clusterName + " from "
               + usedRpList.toString() + " to " + newRpList.toString());
         if (!newRpList.containsAll(usedRpList)) {
            throw BddException.NEW_RP_EXCLUDE_OLD_RP(null, usedRpList.toString(),
                  newRpList.toString());
         }

         cluster.setVcRpNameList(newRpList);
      }

      List<String> newDsList = clusterUpdate.getDsNames();
      if (!CollectionUtils.isEmpty(newDsList)) {
         //Check whether the new datastores are valid vc_data_store
         if (CollectionUtils.isEmpty(validateGivenDS(newDsList))) {
            throw ClusterConfigException.NO_DATASTORE_ADDED();
         }

         //Check whether the new dsNames contain all datastores used by this cluster already
         List<String> usedDsList = cluster.getVcDatastoreNameList();
         logger.info("Updating dsNames for cluster " + clusterName + " from "
               + usedDsList + " to " + newDsList.toString());
         if (!ignoreWarning) {
            if (CollectionUtils.isEmpty(usedDsList)) {
               throw WarningMessageException.SET_EMPTY_DATASTORES_TO_NON_EMTPY(null, newDsList.toString());
            } else if (!newDsList.containsAll(usedDsList)) {
               throw WarningMessageException.NEW_DATASTORES_EXCLUDE_OLD_DATASTORES(null, newDsList.toString(), usedDsList.toString());
            }
         }

         cluster.setVcDatastoreNameList(clusterUpdate.getDsNames());
      }

      clusterEntityMgr.update(cluster);
   }

   private List<String> validateGivenDS(List<String> specifiedDsNames) {
      List<String> exitsDs = new ArrayList<String>();
      Set<String> allDs =
            clusterConfigMgr.getDatastoreMgr().getAllDatastoreNames();
      StringBuffer nonexistentDsNames = new StringBuffer();

      for (String dsName : specifiedDsNames) {
         if (!allDs.contains(dsName))
            nonexistentDsNames.append(dsName).append(",");
         else
            exitsDs.add(dsName);
      }

      if (nonexistentDsNames.length() > 0) {
         nonexistentDsNames.delete(nonexistentDsNames.length() - 1,
               nonexistentDsNames.length());
         throw VcProviderException
               .DATASTORE_NOT_FOUND(nonexistentDsNames.toString());
      }

      return exitsDs;
   }

   private List<String> validateGivenRp(List<String> specifiedRpNames) {
      List<String> exitsRps = new ArrayList<String>();
      Set<String> allRps = clusterConfigMgr.getRpMgr().getAllRPNames();
      StringBuffer nonexistentRpNames = new StringBuffer();

      for (String rpName : specifiedRpNames) {
         if (!allRps.contains(rpName))
            nonexistentRpNames.append(rpName).append(",");
         else
            exitsRps.add(rpName);
      }

      if (nonexistentRpNames.length() > 0) {
         nonexistentRpNames.delete(nonexistentRpNames.length() - 1,
               nonexistentRpNames.length());
         throw VcProviderException
               .RESOURCE_POOL_NOT_FOUND(nonexistentRpNames.toString());
      }

      return exitsRps;
   }



   /*
    * Change the disk I/O priority of the cluster or a node group
    */
   public void prioritizeCluster(String clusterName, Priority ioShares)
         throws Exception {
      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }

      if (ioShares.equals(cluster.getIoShares())) {
         return;
      }

      logger.info("Change all nodes' disk I/O shares to " + ioShares
            + " in the cluster " + clusterName);

      // cluster must be in RUNNING or STOPPEED status
      if (!cluster.getStatus().isActiveServiceStatus()
            && !ClusterStatus.STOPPED.equals(cluster.getStatus())) {
         String msg = "The cluster's status must be RUNNING or STOPPED";
         logger.error(msg);
         throw ClusterManagerException.PRIORITIZE_CLUSTER_NOT_ALLOWED_ERROR(
               clusterName, msg);
      }

      // get target nodeuster
      List<NodeEntity> targetNodes;
      targetNodes = clusterEntityMgr.findAllNodes(clusterName);

      if (targetNodes.isEmpty()) {
         throw ClusterManagerException.PRIORITIZE_CLUSTER_NOT_ALLOWED_ERROR(
               clusterName, "Target node set is empty");
      }

      clusterEntityMgr.cleanupActionError(clusterName);
      // call clustering service to set the io shares
      Map<String, String> failedNodes =
            clusteringService
                  .configIOShares(clusterName, targetNodes, ioShares);
      if (failedNodes.isEmpty()) {
         logger.info("configured " + targetNodes.size() + " nodes' IO share level to "
               + ioShares.toString());
      } else {
         // update node table
         for (String name : failedNodes.keySet()) {
            NodeEntity node = clusterEntityMgr.findNodeByName(name);
            node.setActionFailed(true);
            node.setErrMessage(failedNodes.get(name));
            clusterEntityMgr.update(node);
         }
         throw ClusterManagerException.PRIORITIZE_CLUSTER_FAILED(clusterName,
               failedNodes.size(), targetNodes.size());
      }

      // update io shares in db
      cluster.setIoShares(ioShares);
      clusterEntityMgr.update(cluster);
   }

   @ClusterManagerPointcut
   public Long fixDiskFailures(String clusterName, String groupName)
         throws Exception {
      opsBlocker.blockUnsupportedOpsByCluster("fixDisk", clusterName);

      ClusterEntity cluster = clusterEntityMgr.findByName(clusterName);
      if (cluster == null) {
         logger.error("cluster " + clusterName + " does not exist");
         throw BddException.NOT_FOUND("Cluster", clusterName);
      }
      SoftwareManager softMgr =
         softwareManagerCollector
               .getSoftwareManager(cluster.getAppManager());

      ValidationUtils.validateVersion(clusterEntityMgr, clusterName);

      ClusterStatus oldStatus = cluster.getStatus();

      if (!oldStatus.isActiveServiceStatus()) {
         throw ClusterHealServiceException.NOT_SUPPORTED(clusterName,
               "The cluster status must be RUNNING");
      }

      List<NodeGroupEntity> nodeGroups;

      if (groupName != null) {
         NodeGroupEntity nodeGroup =
               clusterEntityMgr.findByName(clusterName, groupName);
         if (nodeGroup == null) {
            logger.error("node group " + groupName + " does not exist");
            throw BddException.NOT_FOUND("group", groupName);
         }

         nodeGroups = new ArrayList<NodeGroupEntity>(1);
         nodeGroups.add(nodeGroup);
      } else {
         nodeGroups = clusterEntityMgr.findAllGroups(clusterName);
      }

      // only fix worker nodes that have datanode or tasktracker roles
      boolean workerNodesFound = false;
      JobParametersBuilder parametersBuilder = new JobParametersBuilder();
      List<JobParameters> jobParameterList = new ArrayList<JobParameters>();

      for (NodeGroupEntity nodeGroup : nodeGroups) {
         List<String> roles = nodeGroup.getRoleNameList();

         workerNodesFound = true;
         for (NodeEntity node : clusterEntityMgr.findAllNodes(clusterName,
               nodeGroup.getName())) {
            if (node.isObsoleteNode()) {
               logger.info("Ingore node " + node.getVmName()
                     + ", for it violate VM name convention."
                     + "or exceed defined group instance number. ");
               continue;
            }
            if (clusterHealService.hasBadDisks(node.getVmName())) {
               logger.warn("node " + node.getVmName()
                     + " has bad disks. Fixing it..");

               boolean vmPowerOn =
                     (node.getStatus().ordinal() != NodeStatus.POWERED_OFF
                           .ordinal());

               JobParameters nodeParameters =
                     parametersBuilder
                           .addString(JobConstants.CLUSTER_NAME_JOB_PARAM,
                                 clusterName)
                           .addString(JobConstants.TARGET_NAME_JOB_PARAM,
                                 node.getVmName())
                           .addString(JobConstants.GROUP_NAME_JOB_PARAM,
                                 nodeGroup.getName())
                           .addString(JobConstants.SUB_JOB_NODE_NAME,
                                 node.getVmName())
                           .addString(JobConstants.IS_VM_POWER_ON,
                                 String.valueOf(vmPowerOn)).toJobParameters();
               jobParameterList.add(nodeParameters);
            }
         }
      }

      if (!workerNodesFound) {
         throw ClusterHealServiceException
               .NOT_SUPPORTED(clusterName,
                     "only support fixing disk failures for worker/non-management nodes");
      }

      // all target nodes are healthy, simply return
      if (jobParameterList.isEmpty()) {
         logger.info("all target nodes are healthy, simply return");
         throw ClusterHealServiceException.NOT_NEEDED(clusterName);
      }

      try {
         clusterEntityMgr.updateClusterStatus(clusterName,
               ClusterStatus.MAINTENANCE);
         clusterEntityMgr.cleanupActionError(clusterName);
         return jobManager.runSubJobForNodes(
               JobConstants.FIX_NODE_DISK_FAILURE_JOB_NAME, jobParameterList,
               clusterName, oldStatus, oldStatus);
      } catch (Exception e) {
         logger.error("failed to fix disk failures, " + e.getMessage());
         throw e;
      }
   }

   public Map<String, String> getRackTopology(String clusterName, String topology) {
     ClusterRead cluster = clusterEntityMgr.toClusterRead(clusterName);
      Set<String> hosts = new HashSet<String>();
      List<NodeRead> nodes = new ArrayList<NodeRead>();
      for (NodeGroupRead nodeGroup : cluster.getNodeGroups()) {
         for (NodeRead node : nodeGroup.getInstances()) {
            if (node.getMoId() != null) {
               hosts.add(node.getHostName());
               nodes.add(node);
            }
         }
      }

      if (CommonUtil.isBlank(topology)) {
         topology = cluster.getTopologyPolicy().toString();
      }

      AuAssert.check(hosts.size() > 0);
      clusterConfigMgr.validateRackTopologyUploaded(hosts, topology);

      return clusterConfigMgr.buildTopology(nodes, topology);
   }

   public HadoopStack filterDistroFromAppManager(
         SoftwareManager softwareManager, String distroName) {
      return clusterConfigMgr.filterDistroFromAppManager(softwareManager,
            distroName);
   }

   private void verifyNetworkNamesExsitInDB(Set<String> networkNames,
         String clusterName) {
      NetworkEntity networkEntity = null;
      for (String networkName : networkNames) {
         networkEntity = networkManager.getNetworkEntityByName(networkName);
         if (networkEntity == null) {
            throw ClusterConfigException.NETWORK_IS_NOT_FOUND(networkName,
                  clusterName);
         }
      }
   }

   private void verifyRequiredPackages(ClusterCreate createSpec) {
      // check if the cluster is hadoop cluster, to differentiate from other cluster with customized roles
      boolean isHadoopCluster = false;
      NodeGroupCreate[] ngcs = createSpec.getNodeGroups();
      for (NodeGroupCreate nodeGroup : ngcs) {
         List<String> roles = nodeGroup.getRoles();
         for (String role : roles) {
            if (role.indexOf("hadoop") == 0 || role.indexOf("hbase") == 0 || role.indexOf("mapr") == 0) {
               isHadoopCluster = true;
               break;
            }
         }
         if (isHadoopCluster) {
            break;
         }
      }

      // check if the 2 packages(mailx and wsdl4j) have been installed on the serengeti management server.
      // they are needed by cluster creation for Ironfan.
      if (isHadoopCluster && createSpec.getAppManager().equals(Constants.IRONFAN)) {
         checkExtraRequiredPackages();
      }
   }

   private void checkExtraRequiredPackages() {
      logger.info("check if extra required packages(mailx and wsdl4j) have been installed for Ironfan.");
      if ( !extraPackagesExisted ) {
         File yumRepoPath = new File(Constants.SERENGETI_YUM_REPO_PATH);

         // use hs to record the packages that have not been added
         final HashSet<String> hs = new HashSet<String>();
         hs.addAll(extraRequiredPackages);

         // scan the files under the serengeti yum repo directory
         File[] rpmList = yumRepoPath.listFiles(new FileFilter() {
            public boolean accept(File f) {
               String fname = f.getName();
               int idx = fname.indexOf("-");

               if (idx > 0) {
                  String packName = fname.substring(0, idx);
                  if ( extraRequiredPackages.contains(packName) ) {
                     String regx = packName + commRegex;
                     Pattern pat = Pattern.compile(regx);
                     if ( pat.matcher(fname).matches() ) {
                        hs.remove(packName);
                        return true;
                     }
                  }
               }
               return false;
            }
         });

         if ( !hs.isEmpty() ) {
            logger.info("cannot find all the needed packages, stop and return error now. ");
            throw BddException.EXTRA_PACKAGES_NOT_FOUND(hs.toString());
         }

         logger.info("the check is successful: all needed packages are there.");
         extraPackagesExisted = true;
      }
   }

   private String getClusterCloneType() {
      String type = Configuration.getString("cluster.clone.service");
      if ( StringUtils.isBlank(type) ) {
         String version = "";
         VcContext.initVcContext();
         version = VcContext.getVcVersion();
         if ( !CommonUtil.isBlank(version) ) {
            if ( Version.compare(version, Constants.VCENTER_VERSION_6 ) < 0 ) {
               type = Constants.CLUSTER_CLONE_TYPE_FAST_CLONE;
            } else {
               logger.info("The VCenter version is equal or higher than 6.0. Set cluster clone type to instant.");
               type = Constants.CLUSTER_CLONE_TYPE_INSTANT_CLONE;
            }
         }
      }
      return type;
   }

   public void recoverClusters(List<VcClusterMap> clstMaps) throws Exception {
      List<String> nodesNotFound = new ArrayList<String>();
      List<ClusterEntity> clusters = clusterEntityMgr.findAllClusters();
      for ( ClusterEntity cluster : clusters ) {
         List<NodeEntity> nodes = clusterEntityMgr.findAllNodes(cluster.getName());
         for ( NodeEntity node : nodes ) {
            VcVirtualMachine vcVm = ClusterUtil.getVcVm(clusterEntityMgr, node);
            if ( null == vcVm ) {
               logger.info("The cluster node vm " + node.getVmName() + " is not found.");
               nodesNotFound.add(node.getVmName());
            } else {
               if ( null != clstMaps ) {
                  // for different data centers, we need to update the host name as well
                  // the moid has been updated during the getVcVm() method, for different data centers,
                  // we need to update the esxi host name as well, sine the esxi host name are generally
                  // different between 2 data centers
                  String srcHostName = node.getHostName();
                  for ( VcClusterMap clstMap : clstMaps ) {
                     Map<String, String> hostMap = clstMap.getHosts();
                     String tgtHostName = hostMap.get(srcHostName);
                     if ( tgtHostName != null ) {
                        node.setHostName(tgtHostName);
                        clusterEntityMgr.update(node);
                        break;
                     }
                  }
               }
               if ( node.isNotExist() ) {
                  clusterEntityMgr.refreshNodeByMobId(vcVm.getId(), false);
               }
            }
         }
      }
      if ( nodesNotFound.size() > 0 ) {
         String errMsg = "The following cluster node vms " + nodesNotFound.toString()
               + " cannot be found in vCenter Server.";
         logger.info(errMsg);
         throw BddException.CLUSTER_RECOVER_FAILED(nodesNotFound.toString());
      }
   }
}
