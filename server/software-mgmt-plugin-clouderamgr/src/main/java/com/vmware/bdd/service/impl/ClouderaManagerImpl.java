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
package com.vmware.bdd.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.vmware.bdd.exception.CmException;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.cloudera.api.ApiRootResource;
import com.cloudera.api.ClouderaManagerClientBuilder;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiBulkCommandList;
import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiClusterVersion;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiConfig;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostInstallArguments;
import com.cloudera.api.model.ApiHostRef;
import com.cloudera.api.model.ApiHostRefList;
import com.cloudera.api.model.ApiParcel;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.model.ApiServiceState;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v6.RootResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.vmware.bdd.model.CmClusterDef;
import com.vmware.bdd.model.support.AvailableManagementService;
import com.vmware.bdd.model.CmNodeDef;
import com.vmware.bdd.model.support.AvailableParcelStage;
import com.vmware.bdd.model.CmRoleDef;
import com.vmware.bdd.model.CmServiceDef;
import com.vmware.bdd.model.CmServiceRoleType;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.utils.Constants;

/**
 * Author: Xiaoding Bian
 * Date: 6/11/14
 * Time: 5:57 PM
 */
public class ClouderaManagerImpl implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(ClouderaManagerImpl.class);

   private final int versionCdh = 5; // TODO: only test CDH5 so far
   private final String usernameForHosts = "serengeti";
   private String privateKey;
   private RootResourceV6 apiResourceRootV6;

   private static final int API_POLL_PERIOD_MS = 500;

   public ClouderaManagerImpl(String cmServerHost, int port, String user,
         String password, String privateKey) throws CmException {
      ApiRootResource apiRootResource = new ClouderaManagerClientBuilder().withHost(cmServerHost)
            .withPort(port).withUsernamePassword(user, password).build();
      this.apiResourceRootV6 = apiRootResource.getRootV6();
      this.privateKey = privateKey; // TODO: privateKey should bind to a given cluster rather than this instance
   }

   @Override
   public String getName() {
      return null;
   }

   @Override
   public String getDescription() {
      return null;
   }

   @Override
   public String getType() {
      return null;
   }

   @Override
   public Set<String> getSupportedRoles() {
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() {
      return null;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack) {
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint) throws SoftwareManagementPluginException {
      return true;
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint) throws Exception{
      boolean success = false;
      try {
         CmClusterDef clusterDef = new CmClusterDef(blueprint);
         provisionManagement();
         if (!isProvisioned(clusterDef.getName())) { // TODO: if provision failed the first time, isProvisioned is true, should consider resume
            provisionCluster(clusterDef);
            provisionParcels(clusterDef);
         } else {
            syncHostsId(clusterDef); // TODO: temp code
         }

         configureServices(clusterDef);
         start(clusterDef, true);
      } catch (Exception e) {
         logger.error("Failed to create Cloudera cluster " + blueprint.getName() + ": " + e.getMessage());
         throw CmException.CREATE_CLUSTER_FAILED(e, blueprint.getName());
      }

      return success;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint) {
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group, List<NodeInfo> addedNodes) {
      return false;
   }

   @Override
   public boolean startCluster(String clusterName) {
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName) {
      return false;
   }

   @Override
   public boolean onStopCluster(String clusterName) {
      return false;
   }

   @Override
   public boolean onDeleteCluster(String clusterName) {
      return false;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes) {
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes) {
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes) {
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes) {
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      return null;
   }

   @Override
   public String queryClusterStatus(ClusterBlueprint blueprint) {
      return null;
   }

   @Override
   public boolean echo() {
      return true;
   }

   @Override
   public HealthStatus getStatus() {
      return HealthStatus.Connected;
   }

   @Override
   public boolean validateRoles(ClusterBlueprint blueprint, List<String> roles) throws SoftwareManagementPluginException {
      return true;
   }

   @Override
   public boolean validateCliConfigurations(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      return true;
   }

   private boolean unprovision(CmClusterDef cluster) throws CmException {
      try {
         if (!cluster.isEmpty()) {
            if (isProvisioned(cluster.getName())) {
               apiResourceRootV6.getClustersResource().deleteCluster(cluster.getName());
            }
         }
      } catch (Exception e) {
         throw new CmException(e, cluster.getName());
      }
      return true;
   }

   private boolean isProvisioned(String clusterName) throws CmException {
      try {
         for (ApiCluster apiCluster : apiResourceRootV6.getClustersResource().readClusters(DataView.SUMMARY)) {
            if (apiCluster.getName().equals(clusterName)) {
               return true;
            }
         }
      } catch (Exception e) {
         throw CmException.UNSURE_CLUSTER_EXIST(clusterName);
      }
      return false;
   }

   private boolean isStarted(CmClusterDef cluster) throws CmException {
      final Set<String> servicesNotStarted = new HashSet<String>();
      try {

         if (!isConfigured(cluster)) {
            return false;
         }

         for (CmServiceDef serviceDef : cluster.getServices()) {
            servicesNotStarted.add(serviceDef.getName());
         }

         for (ApiService apiService : apiResourceRootV6.getClustersResource()
               .getServicesResource(cluster.getName())
               .readServices(DataView.SUMMARY)) {
            for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .getRolesResource(apiService.getName()).readRoles()) {
               if (apiRole.getRoleState().equals(ApiRoleState.STARTED)) {
                  servicesNotStarted.remove(apiRole.getName());
               }
            }
         }

      } catch (Exception e) {
         throw new CmException(e, cluster.getName());
      }
      return servicesNotStarted.isEmpty();
   }

   public boolean stop(CmClusterDef cluster) throws CmException {
      try {
         if (!cluster.isEmpty()) {
            if (isConfigured(cluster) && !isStopped(cluster)) {
               execute(apiResourceRootV6.getClustersResource().stopCommand("cluster"));
            }
         }
      } catch (Exception e) {
         throw new CmException(e, cluster.getName());
      }
      return true;
   }

   private boolean isStopped(CmClusterDef cluster) throws CmException {
      final Set<String> servicesNotStopped = cluster.allServiceNames();
      try {
         if (isConfigured(cluster)) {
            for (ApiService apiService : apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName())
                  .readServices(DataView.SUMMARY)) {
               for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                     .getRolesResource(apiService.getName()).readRoles()) {
                  if (apiRole.getRoleState().equals(ApiRoleState.STOPPED)) {
                     servicesNotStopped.remove(apiRole.getName());
                  }
               }
            }
         }
      } catch (Exception e) {
         throw new CmException(e, cluster.getName());
      }
      return servicesNotStopped.isEmpty();
   }

   private boolean isConfigured(CmClusterDef cluster) throws CmException {
      boolean executed = false;
      final Set<String> servicesNotConfigured = new HashSet<String>();
      try {
         if (isProvisioned(cluster.getName())) {
            for (CmServiceDef serviceDef : cluster.getServices()) {
               servicesNotConfigured.add(serviceDef.getName());
            }

            for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .readServices(DataView.SUMMARY)) {
               for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                     .getRolesResource(apiService.getName()).readRoles()) {
                  servicesNotConfigured.remove(apiRole.getName());
               }
            }
            executed = true;
         }

      } catch (Exception e) {
         throw new CmException(e, cluster.getName());
      }
      //return executed && servicesNotConfigured.size() == 0;
      return true;
   }

   public boolean initialize(CmClusterDef cluster) throws CmException {
      boolean executed = false;
      try {

         logger.info("Cluster Initializing");

         /*
         Map<String, String> configuration = cluster.getServiceConfiguration(versionApi).get(
               AvailableManagementService.CM.getId());
         configuration.remove("cm_database_name");
         configuration.remove("cm_database_type");
         executed = AvailableManagementService.CM.getId() != null
               && provisionCmSettings(configuration).size() >= configuration.size();
               */

         logger.info("Cluster Initialized");

      } catch (Exception e) {
         logger.error("Cluster Initialize failed");
         throw new CmException(e, cluster.getName());
      }

      return executed;
   }

   private Map<String, String> provisionCmSettings(Map<String, String> config) throws InterruptedException {

      Map<String, String> configPostUpdate = new HashMap<String, String>();
      ApiConfigList apiConfigList = new ApiConfigList();
      if (config != null && !config.isEmpty()) {
         for (String key : config.keySet()) {
            apiConfigList.add(new ApiConfig(key, config.get(key)));
         }
         apiResourceRootV6.getClouderaManagerResource().updateConfig(apiConfigList);
      }
      apiConfigList = apiResourceRootV6.getClouderaManagerResource().getConfig(DataView.SUMMARY);
      for (ApiConfig apiConfig : apiConfigList) {
         configPostUpdate.put(apiConfig.getName(), apiConfig.getValue());
      }

      return configPostUpdate;

   }

   private void provisionManagement() {

      boolean cmsProvisionRequired = false;
      try {
         cmsProvisionRequired = apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource()
               .readService(DataView.SUMMARY) == null;
         logger.info("cmsProvisionRequired: " + cmsProvisionRequired);
      } catch (RuntimeException e) {
         cmsProvisionRequired = true;
      }

      if (cmsProvisionRequired) {
         final ApiHostRef apiHostRef = new ApiHostRef("55b37a12-fda1-4548-b02a-37cd851057fc");

         boolean licenseDeployed = false;

         try {
            licenseDeployed = apiResourceRootV6.getClouderaManagerResource().readLicense() != null;
            logger.info("licenseDeployed: " + licenseDeployed);
         } catch (Exception e) {
            // ignore
         }
         if (!licenseDeployed) {
            apiResourceRootV6.getClouderaManagerResource().beginTrial();
            licenseDeployed = true;
         }
         final boolean enterpriseDeployed = licenseDeployed;

         if (licenseDeployed) {
            logger.info("Start provisioning CM mgmt services");
            ApiService cmsServiceApi = new ApiService();
            List<ApiRole> cmsRoleApis = new ArrayList<ApiRole>();
            cmsServiceApi.setName(AvailableManagementService.MANAGEMENT.getName());
            cmsServiceApi.setType(AvailableManagementService.MANAGEMENT.getId());

            for (AvailableManagementService type : AvailableManagementService.values()) {
               if (type.getParent() != null && (!type.requireEnterprise() || enterpriseDeployed)) {
                  ApiRole cmsRoleApi = new ApiRole();
                  cmsRoleApi.setName(type.getName());
                  cmsRoleApi.setType(type.getId());
                  cmsRoleApi.setHostRef(apiHostRef);
                  cmsRoleApis.add(cmsRoleApi);
               }
            }
            cmsServiceApi.setRoles(cmsRoleApis);
            logger.info("cmsService to setup: " + cmsServiceApi.toString());
            apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().setupCMS(cmsServiceApi);
            logger.info("Finished setup CMS service");

            /* TODO: currently only use default "ApiRoleConfigGroup", support customized configGroup in future
            A role config group contains roles of the same role type sharing the same configuration.
            While each role has to belong to a group, a role config group may be empty. There exists
            a default role config group for each role type. Default groups cannot be removed nor created.
            The name of a role config group is unique and cannot be changed. The configuration of individual
            roles may be overridden on role level.
            */
         }
      }
   }

   private void installHosts(final CmClusterDef cluster) throws Exception {
      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      List<String> Ips = new ArrayList<String>();
      List<String> hostnames = new ArrayList<String>();

      // for resize
      Set<String> existIps = new HashSet<String>();
      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         existIps.add(apiHost.getIpAddress());
      }

      for (CmNodeDef node : cluster.getNodes()) {
         if (existIps.contains(node.getIpAddress())) { // TODO: check health status, delete it if not available
            continue;
         }
         apiHosts.add(node.toCmHost());
         Ips.add(node.getIpAddress());
         hostnames.add(node.getFqdn());
      }

      if (hostnames.size() != 0) {
         ApiHostInstallArguments apiHostInstallArguments = new ApiHostInstallArguments();
         apiHostInstallArguments.setHostNames(hostnames);
         apiHostInstallArguments.setSshPort(22);
         apiHostInstallArguments.setUserName(usernameForHosts);
         apiHostInstallArguments.setPrivateKey(privateKey);
         apiHostInstallArguments.setParallelInstallCount(20);

         // Install CM agents. TODO: show steps msg
         execute("InstallHosts", apiResourceRootV6.getClouderaManagerResource().hostInstallCommand(apiHostInstallArguments));

         // TODO: set rack
      }

      // CM will generate a random ID for each added host, we need to retrieve them for further roles creation.
      syncHostsId(cluster);
      logger.info("cluster spec after synced hosts Id: " + (new Gson()).toJson(cluster));
   }

   private void syncHostsId(final CmClusterDef clusterDef) {
      Map<String, CmNodeDef> ipToNode = clusterDef.ipToNode();
      Map<String, List<CmRoleDef>> ipToRoles = clusterDef.ipToRoles();
      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         if (ipToNode.containsKey(apiHost.getIpAddress())) {
            ipToNode.get(apiHost.getIpAddress()).setNodeId(apiHost.getHostId());
         }
         if (ipToRoles.containsKey(apiHost.getIpAddress())) {
            for (CmRoleDef role : ipToRoles.get(apiHost.getIpAddress())) {
               role.setNodeRef(apiHost.getHostId());
            }
         }
      }
   }

   /**
    * sync roles' name by role type and hostRef
    * @param clusterDef
    */
   private void syncRolesId(final CmClusterDef clusterDef) {
      Map<String, List<CmRoleDef>> nodeRefToRoles = clusterDef.nodeRefToRoles();
      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterDef.getName())
            .readServices(DataView.SUMMARY)) {
         for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(clusterDef.getName())
               .getRolesResource(apiService.getName()).readRoles()) {
            for (CmRoleDef roleDef : nodeRefToRoles.get(apiRole.getHostRef().getHostId())) {
               if (apiRole.getType().equalsIgnoreCase(roleDef.getType())) {
                  roleDef.setName(apiRole.getName());
               }
            }
         }
      }
   }

   public void deleteHosts(final CmClusterDef cluster) throws Exception {
      ApiHostRefList hosts = apiResourceRootV6.getClustersResource().removeAllHosts(cluster.getName());

      for (ApiHostRef host : hosts.getHosts()) {
         apiResourceRootV6.getHostsResource().deleteHost(host.getHostId());
      }
   }

   private void provisionCluster(final CmClusterDef cluster) throws Exception {

      execute(apiResourceRootV6.getClouderaManagerResource().inspectHostsCommand());

      final ApiClusterList clusterList = new ApiClusterList();
      ApiCluster apiCluster = new ApiCluster();
      apiCluster.setName(cluster.getName());
      apiCluster.setVersion(ApiClusterVersion.valueOf(cluster.getVersion()));
      apiCluster.setFullVersion(cluster.getFullVersion());
      clusterList.add(apiCluster);

      apiResourceRootV6.getClustersResource().createClusters(clusterList);

      // install hosts
      installHosts(cluster);

      List<ApiHost> apiHosts = new ArrayList<ApiHost>();
      List<ApiHostRef> apiHostRefs = new ArrayList<ApiHostRef>();

      for (CmNodeDef node : cluster.getNodes()) {
         apiHostRefs.add(new ApiHostRef(node.getNodeId()));
      }

      logger.info("apiHostRefs: " + apiHostRefs.toString());

      // Add hosts to this cluster
      apiResourceRootV6.getClustersResource().addHosts(cluster.getName(), new ApiHostRefList(apiHostRefs));
   }


   /*
   A Parcel encapsulate a specific product and version. For example, (CDH 4.1).
   A parcel is downloaded, distributed to all the machines of a cluster and then allowed to be activated.
    */
   public void provisionParcels(final CmClusterDef cluster) throws InterruptedException, IOException {

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[]{new ApiConfig("PARCEL_UPDATE_FREQ", "1")})));

      final Set<String> repositoriesRequired = new HashSet<String>();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         CmServiceRoleType serviceType = CmServiceRoleType.valueOf(serviceDef.getType());
         repositoriesRequired.add(serviceType.getRepository().toString(cluster.getVersion()));
      }

      logger.info("parcel repo required: " + repositoriesRequired + " cluster: " + cluster.getName());

      final List<String> repositoriesRequiredOrdered = new ArrayList<String>();
      for (String repository : repositoriesRequired) {
         if (repository.equals(Constants.CDH_REPO_PREFIX)) {
            repositoriesRequiredOrdered.add(0, repository);
         } else {
            repositoriesRequiredOrdered.add(repository);
         }
      }

      // validate this cluster has access to all Parcels it requires
      execute("WaitForParcelsAvailability", new Callback() {
         @Override
         public boolean poll() {
            for (ApiParcel parcel : apiResourceRootV6.getClustersResource().getParcelsResource(cluster.getName())
                  .readParcels(DataView.FULL).getParcels()) {
               try {
                  repositoriesRequired.remove(parcel.getProduct());
               } catch (IllegalArgumentException e) {
                  // ignore
               }
            }
            // TODO: if one required parcel is not available, will run forever, need timeout/validation
            return repositoriesRequired.isEmpty();
         }
      });

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[] { new ApiConfig("PARCEL_UPDATE_FREQ", "60") })));

      for (String repository : repositoriesRequiredOrdered) {
         DefaultArtifactVersion parcelVersion = null;
         for (ApiParcel apiParcel : apiResourceRootV6.getClustersResource().getParcelsResource(cluster.getName())
               .readParcels(DataView.FULL).getParcels()) {
            DefaultArtifactVersion parcelVersionTmp = new DefaultArtifactVersion(apiParcel.getVersion());
            if (apiParcel.getProduct().equals(repository)) {
               if (!apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX) || versionCdh == parcelVersionTmp.getMajorVersion()) {
                  if (parcelVersion == null || parcelVersion.compareTo(parcelVersionTmp) < 0) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                  }
               }
            }
         }

         logger.info("exact parcel version: " + parcelVersion);

         final ParcelResource apiParcelResource = apiResourceRootV6.getClustersResource()
               .getParcelsResource(cluster.getName()).getParcelResource(repository, parcelVersion.toString());
         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DOWNLOADED.ordinal()) {
            logger.info("Downloading...");

            Callback poll = new Callback() {
               @Override
               public boolean poll() {
                  /*
                  The ApiParcelState is:
                  ApiParcelState{progress=1612955648, progressTotal=1836961055, count=0, countTotal=1, warnings=null, errors=null}
                  Can be used for progress report
                  */
                  return apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.DOWNLOADED.toString());
               }
            };

            if (apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.DOWNLOADING.toString())) {
               execute("Another thread is downloading this parcel, wait for it...", poll);
            } else {
               execute(apiParcelResource.startDownloadCommand(), poll, false);
            }
         }

         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DISTRIBUTED.ordinal()) {
            logger.info("Distributing....");
            execute(apiParcelResource.startDistributionCommand(), new Callback() {
               @Override
               public boolean poll() {
                  return apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.DISTRIBUTED.toString());
               }
            }, false);
         }
         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.ACTIVATED.ordinal()) {
            logger.info("Activating....");
            execute(apiParcelResource.activateCommand(), new Callback() {
               @Override
               public boolean poll() {
                  return apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.ACTIVATED.toString());
               }
            }, false);
         }
      }
   }

   public void configureServices(final CmClusterDef cluster) throws Exception {
      logger.info("Creating cluster services");
      ApiServiceList serviceList = new ApiServiceList();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         CmServiceRoleType type = CmServiceRoleType.valueOf(serviceDef.getType());
         ApiService apiService = new ApiService();
         apiService.setType(type.getId());
         apiService.setName(serviceDef.getName());
         apiService.setDisplayName(serviceDef.getDisplayName());

         ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
         // TODO: support user defined configs

         // config service dependencies
         Set<CmServiceRoleType> serviceTypes = cluster.allServiceTypes();
         switch (type) {
            case YARN: //TODO: Compute Only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               break;
            case MAPREDUCE: //TODO: Compute Only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               break;
            case HBASE: //TODO: HBase only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.serviceNameOfType(CmServiceRoleType.ZOOKEEPER)));
               break;
            case SOLR:
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.serviceNameOfType(CmServiceRoleType.ZOOKEEPER)));
               break;
            case SOLR_INDEXER:
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
               apiServiceConfig.add(new ApiConfig("solr_service", cluster.serviceNameOfType(CmServiceRoleType.SOLR)));
               break;
            case HUE:
               apiServiceConfig.add(new ApiConfig("hue_webhdfs", cluster.serviceNameOfType(CmServiceRoleType.HDFS_HTTP_FS)));
               apiServiceConfig.add(new ApiConfig("oozie_service", cluster.serviceNameOfType(CmServiceRoleType.OOZIE)));
               apiServiceConfig.add(new ApiConfig("hive_service", cluster.serviceNameOfType(CmServiceRoleType.HIVE)));
               if (serviceTypes.contains(CmServiceRoleType.HBASE)) {
                  apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
               }
               if (serviceTypes.contains(CmServiceRoleType.IMPALA)) {
                  apiServiceConfig.add(new ApiConfig("impala_service", cluster.serviceNameOfType(CmServiceRoleType.IMPALA)));
               }
               if (serviceTypes.contains(CmServiceRoleType.SOLR)) {
                  apiServiceConfig.add(new ApiConfig("solr_service", cluster.serviceNameOfType(CmServiceRoleType.SOLR)));
               }
               if (serviceTypes.contains(CmServiceRoleType.SQOOP)) {
                  apiServiceConfig.add(new ApiConfig("sqoop_service", cluster.serviceNameOfType(CmServiceRoleType.SQOOP)));
               }
               if (serviceTypes.contains(CmServiceRoleType.HBASE_THRIFT_SERVER)) {
                  apiServiceConfig.add(new ApiConfig("hue_hbase_thrift", cluster
                        .serviceNameOfType(CmServiceRoleType.HBASE_THRIFT_SERVER)));
               }
               break;
            case SQOOP:
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains(CmServiceRoleType.YARN) ? cluster.serviceNameOfType(CmServiceRoleType.YARN) : cluster
                     .serviceNameOfType(CmServiceRoleType.MAPREDUCE)));
               break;
            case OOZIE:
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains(CmServiceRoleType.YARN) ? cluster.serviceNameOfType(CmServiceRoleType.YARN) : cluster
                     .serviceNameOfType(CmServiceRoleType.MAPREDUCE)));
               break;
            case HIVE:
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains(CmServiceRoleType.YARN) ? cluster.serviceNameOfType(CmServiceRoleType.YARN) : cluster
                     .serviceNameOfType(CmServiceRoleType.MAPREDUCE)));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster
                     .serviceNameOfType(CmServiceRoleType.ZOOKEEPER)));
               break;
            case IMPALA:
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
               apiServiceConfig.add(new ApiConfig("hive_service", cluster.serviceNameOfType(CmServiceRoleType.HIVE)));
               break;
            case FLUME:
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType(CmServiceRoleType.HDFS)));
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType(CmServiceRoleType.HBASE)));
            default:
               break;
         }
         apiService.setConfig(apiServiceConfig);

         List<ApiRole> apiRoles = new ArrayList<ApiRole>();
         for (CmRoleDef roleDef : serviceDef.getRoles()) {
            ApiRole apiRole = new ApiRole();
            apiRole.setType(roleDef.getType());
            apiRole.setHostRef(new ApiHostRef(roleDef.getNodeRef()));
            ApiConfigList roleConfigList = new ApiConfigList();
            if (roleDef.getConfigs() != null) {
               for (String key : roleDef.getConfigs().keySet()) {
                  roleConfigList.add(new ApiConfig(key, roleDef.getConfigs().get(key)));
               }
            }
            apiRole.setConfig(roleConfigList);
            apiRoles.add(apiRole);
         }

         apiService.setRoles(apiRoles);
         serviceList.add(apiService);
      }

      logger.info("services to create: " + (new Gson()).toJson(serviceList));
      apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).createServices(serviceList); // TODO: resume/resize
      logger.info("Finished create services");
      syncRolesId(cluster);

      // Necessary, since createServices a habit of kicking off async commands (eg ZkAutoInit )
      for (CmServiceRoleType type : cluster.allServiceTypes()) {
         for (ApiCommand command : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
               .listActiveCommands(cluster.serviceNameOfType(type), DataView.SUMMARY)) {
            execute(command, false);
         }
      }

      execute(apiResourceRootV6.getClustersResource().deployClientConfig(cluster.getName()));
   }


   /**
    * start services/roles, assume the roles' IDs are already synched
    * @param cluster
    * @param isFirstStart
    * @return
    * @throws CmException
    */
   public boolean start(final CmClusterDef cluster, final boolean isFirstStart) throws CmException {

      boolean executed = true;
      try {

         if (!cluster.isEmpty()) {
            if (!isConfigured(cluster)) {
               configureServices(cluster);
            }
            if (!isStarted(cluster)) {
               for (CmServiceDef serviceDef : cluster.getServices()) {
                  if (isFirstStart) {
                     preStartServices(cluster, serviceDef);
                     for (CmRoleDef roleDef : serviceDef.getRoles()) {
                        preStartRoles(cluster, serviceDef, roleDef);
                     }
                  }
                  startService(cluster, serviceDef);
                  if (isFirstStart) {
                     postStartServices(cluster, serviceDef);
                     for (CmRoleDef roleDef : serviceDef.getRoles()) {
                        postStartRoles(cluster, serviceDef, roleDef);
                     }
                  }
               }
            } else {
               executed = false;
            }

            // push into provision phase once OPSAPS-13194/OPSAPS-12870 is addressed
            startManagement(cluster);
         }
      } catch (Exception e) {
         throw new CmException(e, cluster.getName());
      }

      return executed;
   }

   private void preStartServices(final CmClusterDef cluster, CmServiceDef serviceDef) throws InterruptedException {
      switch (CmServiceRoleType.valueOfId(serviceDef.getType())) {
         case HIVE:
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHiveWarehouseCommand(serviceDef.getName()));
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .hiveCreateMetastoreDatabaseTablesCommand(serviceDef.getName()), false);
            break;
         case OOZIE:
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .installOozieShareLib(serviceDef.getName()), false);
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .createOozieDb(serviceDef.getName()), false);
            break;
         case HBASE:
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHBaseRootCommand(serviceDef.getName()));
         case ZOOKEEPER:
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .zooKeeperInitCommand(serviceDef.getName()), false);
            break;
         case SOLR:
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .initSolrCommand(serviceDef.getName()), false);
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createSolrHdfsHomeDirCommand(serviceDef.getName()));
            break;
         case SQOOP:
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createSqoopUserDirCommand(serviceDef.getName()));
            break;
         default:
            break;
      }
   }

   private void preStartRoles(final CmClusterDef cluster, CmServiceDef serviceDef, CmRoleDef roleDef) throws IOException,
         InterruptedException {

      switch (CmServiceRoleType.valueOfId(roleDef.getType())) {
         case HDFS_NAMENODE:
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .getRoleCommandsResource(serviceDef.getName())
                        .formatCommand(new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  true);
            break;
         case YARN_RESOURCE_MANAGER:
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createYarnNodeManagerRemoteAppLogDirCommand(serviceDef.getName()));
            break;
         case YARN_JOB_HISTORY:
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createYarnJobHistoryDirCommand(serviceDef.getName()));
            break;
         case HUE_SERVER:
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .getRoleCommandsResource(serviceDef.getName())
                        .syncHueDbCommand(
                              new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  false);
            break;
         default:
            break;
      }
   }

   private void postStartServices(final CmClusterDef cluster, CmServiceDef serviceDef) throws IOException,
         InterruptedException {

      switch (CmServiceRoleType.valueOfId(serviceDef.getType())) {
         default:
            break;
      }
   }

   private void postStartRoles(final CmClusterDef cluster, CmServiceDef serviceDef, CmRoleDef roleDef) throws IOException,
         InterruptedException {

      switch (CmServiceRoleType.valueOfId(roleDef.getType())) {
         case HDFS_NAMENODE:
            ApiRoleNameList formatList = new ApiRoleNameList();
            formatList.add(roleDef.getName());
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .hdfsCreateTmpDir(serviceDef.getName()));
            break;
         default:
            break;
      }
   }

   private void startService(CmClusterDef cluster, CmServiceDef serviceDef) throws InterruptedException, IOException {
      execute(
            "Start " + serviceDef.getName(),
            apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .startCommand(serviceDef.getName()));
   }

   private void startManagement(final CmClusterDef cluster) throws InterruptedException {

      try {
         if (apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().readService(DataView.SUMMARY)
               .getServiceState().equals(ApiServiceState.STOPPED)) {
            execute("Start " + AvailableManagementService.MANAGEMENT.getId().toLowerCase(), apiResourceRootV6
                  .getClouderaManagerResource().getMgmtServiceResource().startCommand());
         }
      } catch (RuntimeException exception) {
         // ignore
      }
   }


   public void unconfigureServices(final CmClusterDef cluster) throws Exception {
      final Set<CmServiceRoleType> types = new TreeSet<CmServiceRoleType>(Collections.reverseOrder());
      types.addAll(cluster.allServiceTypes());
      for (String serviceName : cluster.allServiceNames()) {
         ServicesResourceV6 servicesResource = apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName());
         if (servicesResource != null) {
            servicesResource.deleteService(serviceName);
         }
      }
   }

   private ApiCommand execute(final ApiBulkCommandList bulkCommand, boolean checkReturn) throws InterruptedException {
      ApiCommand lastCommand = null;
      for (ApiCommand command : bulkCommand) {
         lastCommand = execute(command, checkReturn);
      }
      return lastCommand;
   }

   private ApiCommand execute(final ApiCommand command) throws InterruptedException {
      return execute(command, true);
   }

   private ApiCommand execute(String label, final ApiCommand command) throws InterruptedException {
      return execute(label, command, true);
   }

   private ApiCommand execute(final ApiCommand command, boolean checkReturn) throws InterruptedException {
      return execute(command.getName(), command, checkReturn);
   }

   private ApiCommand execute(String label, final ApiCommand command, boolean checkReturn) throws InterruptedException {
      return execute(label, command, new Callback() {
         @Override
         public boolean poll() {
            return apiResourceRootV6.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
         }
      }, checkReturn);
   }

   private ApiCommand execute(ApiCommand command, Callback callback, boolean checkReturn) throws InterruptedException {
      return execute(command.getName(), command, callback, checkReturn);
   }

   private ApiCommand execute(String label, Callback callback) throws InterruptedException {
      return execute(label, null, callback, false);
   }

   private ApiCommand execute(String label, ApiCommand command, Callback callback, boolean checkReturn)
         throws InterruptedException {
      label = WordUtils.capitalize(label.replace("-", " ").replace("_", " ")).replace(" ", "");
      logger.info("label: " + label);
      if (command != null) {
         logger.info("Waiting for command: " + label);
      }
      ApiCommand commandReturn = null;
      while (true) {
         if (callback.poll()) {
            if (checkReturn && command != null
                  && !(commandReturn = apiResourceRootV6.getCommandsResource().readCommand(command.getId())).getSuccess()) {
               logger.info("Failed to run command: " + command);
               throw new RuntimeException("Command [" + command + "] failed [" + commandReturn + "]");
            }
            return commandReturn;
         }
         Thread.sleep(API_POLL_PERIOD_MS);
      }
   }

   private static abstract class Callback {
      public abstract boolean poll();
   }
}
