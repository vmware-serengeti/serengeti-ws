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
package com.vmware.bdd.plugin.clouderamgr.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.cloudera.api.model.ApiRoleConfigGroup;
import com.google.gson.GsonBuilder;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRoleContainer;
import com.vmware.bdd.plugin.clouderamgr.poller.host.HostInstallPoller;
import com.vmware.bdd.plugin.clouderamgr.exception.ClouderaManagerException;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableManagementService;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableParcelStage;
import com.vmware.bdd.plugin.clouderamgr.poller.ParcelProvisionPoller;
import com.vmware.bdd.plugin.clouderamgr.utils.CmUtils;
import com.vmware.bdd.plugin.clouderamgr.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;

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
import com.vmware.bdd.plugin.clouderamgr.model.CmClusterDef;
import com.vmware.bdd.plugin.clouderamgr.model.CmNodeDef;
import com.vmware.bdd.plugin.clouderamgr.model.CmRoleDef;
import com.vmware.bdd.plugin.clouderamgr.model.CmServiceDef;
import com.vmware.aurora.util.AuAssert;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

/**
 * Author: Xiaoding Bian
 * Date: 6/11/14
 * Time: 5:57 PM
 */
public class ClouderaManagerImpl implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(ClouderaManagerImpl.class);

   private final String usernameForHosts = "serengeti";
   private String privateKey;
   private RootResourceV6 apiResourceRootV6;
   private String cmServerHostId;
   private String domain;
   private String cmServerHost;
   private int cmPort;
   private String cmUsername;
   private String cmPassword;

   private final static int INVALID_PROGRESS = -1;
   private enum ProgressSplit {
      INSPECT_HOSTS(10),
      INSTALL_HOSTS_AGENT(40),
      VALIDATE_PARCELS_AVAILABILITY(45),
      DOWNLOAD_PARCEL(60),
      DISTRIBUTE_PARCEL(70),
      ACTIVATE_PARCEL(75),
      CONFIGURE_SERVICES(80),
      START_SERVICES(100);

      private int progress;
      private ProgressSplit(int progress) {
         this.progress = progress;
      }

      public int getProgress() {
         return progress;
      }
   }

   public ClouderaManagerImpl() {}

   public ClouderaManagerImpl(String cmServerHost, int port, String user,
         String password, String privateKey) throws ClouderaManagerException {
      this.cmServerHost = cmServerHost;
      this.cmPort = port;
      this.domain = "http://" + cmServerHost + ":" + cmPort;
      this.cmUsername = user;
      this.cmPassword = password;
      ApiRootResource apiRootResource = new ClouderaManagerClientBuilder().withHost(cmServerHost)
            .withPort(port).withUsernamePassword(user, password).build();
      this.apiResourceRootV6 = apiRootResource.getRootV6();
      this.privateKey = privateKey;
   }

   @Override
   public String getName() {
      return Constants.CDH_PLUGIN_NAME;
   }

   @Override
   public String getDescription() {
      return null;
   }

   @Override
   public String getType() {
      return Constants.CDH_PLUGIN_NAME;
   }

   @Override
   public Set<String> getSupportedRoles(HadoopStack hadoopStack) throws SoftwareManagementPluginException {
      try {
         return AvailableServiceRoleContainer.allRoles(CmUtils.majorVersionOfHadoopStack(hadoopStack));
      } catch (IOException e) {
         throw new SoftwareManagementPluginException(e.getMessage());
      }
   }

   @Override
   public List<HadoopStack> getSupportedStacks() throws SoftwareManagementPluginException {
      String randomClusterName = UUID.randomUUID().toString();
      final ApiClusterList clusterList = new ApiClusterList();
      ApiCluster apiCluster = new ApiCluster();
      apiCluster.setName(randomClusterName);
      apiCluster.setVersion(ApiClusterVersion.CDH5);
      clusterList.add(apiCluster);
      apiResourceRootV6.getClustersResource().createClusters(clusterList);

      List<HadoopStack> hadoopStacks = new ArrayList<HadoopStack>();
      for (ApiParcel apiParcel : apiResourceRootV6.getClustersResource().getParcelsResource(randomClusterName)
            .readParcels(DataView.SUMMARY).getParcels()) {
         if (apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX)) {
            DefaultArtifactVersion parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
            HadoopStack stack = new HadoopStack();
            stack.setDistro(apiParcel.getProduct(), parcelVersion.getMajorVersion() + "."
                  + parcelVersion.getMinorVersion() + "." + parcelVersion.getIncrementalVersion());
            stack.setFullVersion(apiParcel.getVersion());
            stack.setVendor(Constants.CDH_DISTRO_VENDOR);
            hadoopStacks.add(stack);
         }
      }
      apiResourceRootV6.getClustersResource().deleteCluster(randomClusterName);
      return hadoopStacks;
   }

   @Override
   public String getSupportedConfigs(HadoopStack hadoopStack)
         throws SoftwareManagementPluginException {
      try {
         return AvailableServiceRoleContainer.getSupportedConfigs(CmUtils.majorVersionOfHadoopStack(hadoopStack));
      } catch (IOException e) {
         throw new SoftwareManagementPluginException(e.getMessage());
      }
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint) throws ValidationException {
      return (new CmClusterValidator()).validateBlueprint(blueprint);
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
      boolean success = false;
      CmClusterDef clusterDef = null;
      try {
         clusterDef = new CmClusterDef(blueprint);
         //provisionManagement();
         provisionCluster(clusterDef, reportQueue);
         provisionParcels(clusterDef, reportQueue);
         configureServices(clusterDef, reportQueue);
         startServices(clusterDef, reportQueue, true);
         success = true;
         clusterDef.getCurrentReport().setAction("Successfully Create Cluster");
         clusterDef.getCurrentReport().setProgress(100);
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         logger.error(e.getMessage());
         throw SoftwareManagementPluginException.CREATE_CLUSTER_FAILED(e.getMessage(), e);
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }

      return success;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group, List<NodeInfo> addedNodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      try {
         if (!isProvisioned(clusterName)) {
            return true;
         }

         ApiHostRefList hosts = apiResourceRootV6.getClustersResource().listHosts(clusterName);

         apiResourceRootV6.getClustersResource().deleteCluster(clusterName);

         for (ApiHostRef host : hosts.getHosts()) {
            apiResourceRootV6.getHostsResource().deleteHost(host.getHostId());
         }

      } catch (Exception e) {
         throw SoftwareManagementPluginException.DELETE_CLUSTER_FAILED(clusterName, e);
      }
      return true;
   }

   @Override
   public boolean onStopCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return stopCluster(clusterName);

      //TODO(qjin): handle reports
   }

   @Override
   public boolean onDeleteCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // just stop this cluster
      return onStopCluster(clusterName, reports);
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      return null;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) {
      return null;
   }

   @Override
   public boolean echo() {
      try {
         String message = "Hello";
         return apiResourceRootV6.getToolsResource().echo(message).equals(message);
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public HealthStatus getStatus() {
      return HealthStatus.Connected;
   }

   private boolean unprovision(CmClusterDef cluster) throws ClouderaManagerException {
      try {
         if (!cluster.isEmpty()) {
            if (isProvisioned(cluster.getName())) {
               apiResourceRootV6.getClustersResource().deleteCluster(cluster.getName());
            }
         }
      } catch (Exception e) {
         throw new SoftwareManagementPluginException(cluster.getName(), e); //TODO
      }
      return false;
   }

   private boolean isProvisioned(String clusterName) throws ClouderaManagerException {
      for (ApiCluster apiCluster : apiResourceRootV6.getClustersResource().readClusters(DataView.SUMMARY)) {
         if (apiCluster.getName().equals(clusterName)) {
            return true;
         }
      }
      return false;
   }

   private boolean isStarted(CmClusterDef cluster) throws ClouderaManagerException {
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
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(cluster.getName(), e);
      }
      return servicesNotStarted.isEmpty();
   }

   private boolean needStop(String clusterName) {
      if (!isProvisioned(clusterName)) {
         return false;
      }

      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         if (apiService.getServiceState().equals(ApiServiceState.STARTED) || apiService.getServiceState().equals(ApiServiceState.STARTING)) {
            return true;
         }
      }

      return false;
   }

   private boolean needStart(String clusterName) {
      if (!isProvisioned(clusterName)) {
         return false;
      }
      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         if (apiService.getServiceState().equals(ApiServiceState.STARTED) || apiService.getServiceState().equals(ApiServiceState.STARTING)) {
            continue;
         } else {
            return true;
         }
      }
      return false;
   }

   private boolean stopCluster(String clusterName) throws SoftwareManagementPluginException {
      AuAssert.check(clusterName != null && !clusterName.isEmpty());
      try {
         if (isStopped(clusterName) || !needStop(clusterName)) {
            return true;
         }

         execute(apiResourceRootV6.getClustersResource().stopCommand(clusterName));
      } catch (Exception e) {
         throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(clusterName, e);
      }
      return true;
   }

   @Override
   public boolean startCluster(String clusterName, ClusterReportQueue reports) throws SoftwareManagementPluginException {
      AuAssert.check(clusterName != null && !clusterName.isEmpty());
      try {
         if (!needStart(clusterName)) {
            return true;
         }

         execute(apiResourceRootV6.getClustersResource().startCommand(clusterName));
      } catch (Exception e) {
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(clusterName, e);
      }
      return true;
   }

   private boolean isStopped(String clusterName) throws ClouderaManagerException {
      if (!isProvisioned(clusterName)) {
         return false;
      }

      for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(clusterName).readServices(DataView.SUMMARY)) {
         if (!apiService.getServiceState().equals(ApiServiceState.STOPPED)) {
            return false;
         }
      }
      return true;
   }

   private boolean isConfigured(CmClusterDef cluster) throws ClouderaManagerException {
      boolean executed = false;
      final Set<String> servicesNotConfigured = new HashSet<String>();
      try {
         if (isProvisioned(cluster.getName())) {
            for (CmServiceDef serviceDef : cluster.getServices()) {
               servicesNotConfigured.add(serviceDef.getName());
            }

            for (ApiService apiService : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .readServices(DataView.SUMMARY)) {
               servicesNotConfigured.remove(apiService.getName());
               /*
               for (ApiRole apiRole : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                     .getRolesResource(apiService.getName()).readRoles()) {
                  servicesNotConfigured.remove(apiRole.getName());
               }
               */
            }
            executed = true;
         }

      } catch (Exception e) {
         throw new SoftwareManagementPluginException(cluster.getName(), e);
      }
      return executed && servicesNotConfigured.size() == 0;
   }

   public boolean initialize(CmClusterDef cluster) throws ClouderaManagerException {
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
         throw new SoftwareManagementPluginException(cluster.getName(), e);
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
         // TODO: in the first stage, configure management services on Cloudera Manager Server
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

         final ApiHostRef apiHostRef = new ApiHostRef(getCmServerHostId());
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

   /**
    * install hosts agent, Reentrant
    * @param cluster
    * @param reportQueue
    * @throws Exception
    */
   private void installHosts(final CmClusterDef cluster, final ClusterReportQueue reportQueue) throws Exception {
      logger.info("Installing agent for each node of cluster: " + cluster.getName());
      List<String> ips = new ArrayList<String>();
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
         hostnames.add(node.getFqdn());
         ips.add(node.getIpAddress());
      }

      if (hostnames.size() != 0) {
         ApiHostInstallArguments apiHostInstallArguments = new ApiHostInstallArguments();
         apiHostInstallArguments.setHostNames(hostnames);
         apiHostInstallArguments.setSshPort(22);
         apiHostInstallArguments.setUserName(usernameForHosts);
         apiHostInstallArguments.setPrivateKey(privateKey);
         apiHostInstallArguments.setParallelInstallCount(20);

         // Install CM agents. TODO: show steps msg
         StatusPoller hostInstallPoller = null;
         try {

            final ApiCommand cmd =  apiResourceRootV6.getClouderaManagerResource().hostInstallCommand(apiHostInstallArguments);
            logger.info("install command id: " + cmd.getId());

            hostInstallPoller = new HostInstallPoller(apiResourceRootV6, cmd.getId(), cluster.getCurrentReport(), reportQueue,
                  ProgressSplit.INSTALL_HOSTS_AGENT.getProgress(), domain, cmUsername, cmPassword);
            executeAndReport("Installing Host Agents", cmd, ProgressSplit.INSTALL_HOSTS_AGENT.getProgress(),
                  cluster.getCurrentReport(), reportQueue, hostInstallPoller, true);
         } catch (Exception e) {
            logger.info(e.getMessage());

            Set<String> installedIPs = new HashSet<String>();
            for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
               installedIPs.add(apiHost.getIpAddress());
            }
            List<String> failedIps = new ArrayList<String>();
            for (String ip : ips) {
               if (!installedIPs.contains(ip)) {
                  failedIps.add(ip);
               }
            }

            String errMsg = "Failed to install agents on nodes: " + failedIps.toString()
                  + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
            logger.error(errMsg);
            throw ClouderaManagerException.INSTALL_AGENTS_FAIL(errMsg, e);
         }
      } else {
         cluster.getCurrentReport().setProgress(ProgressSplit.INSTALL_HOSTS_AGENT.getProgress());
         reportQueue.addClusterReport(cluster.getCurrentReport().clone());
      }
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
    * assume host IDs are already synced
    * @param clusterDef
    */
   private void updateRackId(final CmClusterDef clusterDef) {
      for (CmNodeDef node : clusterDef.getNodes()) {
         ApiHost host = apiResourceRootV6.getHostsResource().readHost(node.getNodeId());
         host.setRackId(node.getRackId());
         apiResourceRootV6.getHostsResource().updateHost(host.getHostId(), host);
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
               if (apiRole.getType().equalsIgnoreCase(roleDef.getType().getName())) {
                  roleDef.setName(apiRole.getName());
               }
            }
         }
      }
   }

   /**
    * Reentrant
    * @param cluster
    * @param reportQueue
    * @throws Exception
    */
   private void provisionCluster(final CmClusterDef cluster, final ClusterReportQueue reportQueue) throws Exception {

      if (!isProvisioned(cluster.getName())) {
         executeAndReport("Inspecting Hosts", apiResourceRootV6.getClouderaManagerResource().inspectHostsCommand(),
               ProgressSplit.INSPECT_HOSTS.getProgress(), cluster.getCurrentReport(), reportQueue, false);

         final ApiClusterList clusterList = new ApiClusterList();
         ApiCluster apiCluster = new ApiCluster();
         apiCluster.setName(cluster.getName());
         apiCluster.setVersion(ApiClusterVersion.valueOf(cluster.getVersion()));
         apiCluster.setFullVersion(cluster.getFullVersion());
         clusterList.add(apiCluster);

         apiResourceRootV6.getClustersResource().createClusters(clusterList);
      } else {
         /*
         For cluster resume/resize, the cluster is already exist, we need to check if this cluster is created by BDE.
         So far, just check if all IPs exist in Cloudera Cluster are included in given blueprint
          */
         Set<String> ips = new HashSet<String>();
         for (CmNodeDef node : cluster.getNodes()) {
            ips.add(node.getIpAddress());
         }
         for (ApiHostRef hostRef : apiResourceRootV6.getClustersResource().listHosts(cluster.getName())) {
            if (!ips.contains(apiResourceRootV6.getHostsResource().readHost(hostRef.getHostId()).getIpAddress())) {
               throw SoftwareManagementPluginException.CLUSTER_ALREADY_EXIST(cluster.getName(), null);
            }
         }
      }

      retry(2, new Retriable() {
         @Override
         public void doWork() throws Exception {
            installHosts(cluster, reportQueue);
         }
      });

      syncHostsId(cluster);
      updateRackId(cluster);
      logger.debug("cluster spec after synced hosts Id: " + new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .create().toJson(cluster));

      Set<ApiHostRef> toAddHosts = new HashSet<ApiHostRef>();
      for (CmNodeDef node : cluster.getNodes()) {
         toAddHosts.add(new ApiHostRef(node.getNodeId()));
      }

      for (ApiHostRef hostRef : apiResourceRootV6.getClustersResource().listHosts(cluster.getName())) {
         if (toAddHosts.contains(hostRef)) {
            toAddHosts.remove(hostRef);
         }
      }

      if (!toAddHosts.isEmpty()) {
         logger.info("apiHosts to add: " + toAddHosts.toString());

         // Add hosts to this cluster
         apiResourceRootV6.getClustersResource().addHosts(cluster.getName(), new ApiHostRefList(new ArrayList<ApiHostRef>(toAddHosts)));
      }
   }

   /** provision parcels, Reentrant
    * A Parcel encapsulate a specific product and version. For example, (CDH 4.1).
    * A parcel is downloaded, distributed to all the machines of a cluster and then allowed to be activated.
    *
    * @param cluster
    * @param reportQueue
    * @throws Exception
    */
   private void provisionParcels(final CmClusterDef cluster, final ClusterReportQueue reportQueue) throws Exception {

      if (isConfigured(cluster)) {
         // TODO: resize
         return;
      }

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[]{new ApiConfig("PARCEL_UPDATE_FREQ", "1")})));

      final Set<String> repositoriesRequired = new HashSet<String>();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         repositoriesRequired.add(serviceDef.getType().getRepository().toString(cluster.getVersion()));
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
      executeAndReport("Validating parcels availability", null, ProgressSplit.VALIDATE_PARCELS_AVAILABILITY.getProgress(),
            cluster.getCurrentReport(), reportQueue, new StatusPoller() {
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
      }, false);

      apiResourceRootV6.getClouderaManagerResource().updateConfig(
            new ApiConfigList(Arrays.asList(new ApiConfig[]{new ApiConfig("PARCEL_UPDATE_FREQ", "60")})));

      DefaultArtifactVersion expectVersion = null;
      if (cluster.getFullVersion() != null) {
         expectVersion = new DefaultArtifactVersion(cluster.getFullVersion());
      }

      for (String repository : repositoriesRequiredOrdered) {
         DefaultArtifactVersion parcelVersion = null;
         for (ApiParcel apiParcel : apiResourceRootV6.getClustersResource().getParcelsResource(cluster.getName())
               .readParcels(DataView.FULL).getParcels()) {
            DefaultArtifactVersion parcelVersionTmp = new DefaultArtifactVersion(apiParcel.getVersion());
            if (apiParcel.getProduct().equals(repository)) {
               if (apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX)) {
                  /*
                   * Policy for "CDH" parcel:
                   * 1) If specify fullVersion, try to find that parcel, if cannot, select the latest parcel(with highest version).
                   * 2) If fullVersion not specified, select the latest parcel
                   */
                  if (parcelVersion == null || parcelVersion.compareTo(parcelVersionTmp) < 0) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                  }
                  if (expectVersion != null && parcelVersionTmp.getMajorVersion() == expectVersion.getMajorVersion()
                        && parcelVersionTmp.getMinorVersion() == expectVersion.getMinorVersion()
                        && parcelVersionTmp.getIncrementalVersion() == expectVersion.getIncrementalVersion()) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                     break;
                  }
               }

               if (!apiParcel.getProduct().equals(Constants.CDH_REPO_PREFIX)) {
                  // For non-CDH parcel, just select the latest one
                  if (parcelVersion == null || parcelVersion.compareTo(parcelVersionTmp) < 0) {
                     parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
                  }
               }
            }
         }

         final ParcelResource apiParcelResource = apiResourceRootV6.getClustersResource()
               .getParcelsResource(cluster.getName()).getParcelResource(repository, parcelVersion.toString());
         String refMsg = referCmfUrlMsg(domain + "/cmf/parcel/status");
         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DOWNLOADED.ordinal()) {
            String action = "Downloading parcel...";

            ParcelProvisionPoller poll = new ParcelProvisionPoller(apiParcelResource, AvailableParcelStage.DOWNLOADED, cluster.getCurrentReport(),
                  reportQueue, ProgressSplit.DOWNLOAD_PARCEL.getProgress());

            if (apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.DOWNLOADING.toString())) {
               // Another thread is downloading this parcel, just wait for its completion
               executeAndReport(action, null, ProgressSplit.DOWNLOAD_PARCEL.getProgress(),
                     cluster.getCurrentReport(), reportQueue, poll, false);
            } else {
               // the ApiCommand instance for parcel is inaccessible, so do not check the return value
               executeAndReport(action, apiParcelResource.startDownloadCommand(),
                     ProgressSplit.DOWNLOAD_PARCEL.getProgress(),
                     cluster.getCurrentReport(), reportQueue, poll, false);
            }
            if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DOWNLOADED.ordinal()) {
               throw ClouderaManagerException.DOWNLOAD_PARCEL_FAIL(apiParcelResource.readParcel().getProduct(),
                     apiParcelResource.readParcel().getVersion(), refMsg);
            }
         }

         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DISTRIBUTED.ordinal()) {
            String action = "Distributing parcel...";

            final StatusPoller poller = new ParcelProvisionPoller(apiParcelResource, AvailableParcelStage.DISTRIBUTED, cluster.getCurrentReport(),
                  reportQueue, ProgressSplit.DISTRIBUTE_PARCEL.getProgress());

            executeAndReport(action, apiParcelResource.startDistributionCommand(),
                  ProgressSplit.DISTRIBUTE_PARCEL.getProgress(),
                  cluster.getCurrentReport(), reportQueue, poller, false);

            if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.DISTRIBUTED.ordinal()) {
               throw ClouderaManagerException.DISTRIBUTE_PARCEL_FAIL(apiParcelResource.readParcel().getProduct(),
                     apiParcelResource.readParcel().getVersion(), refMsg);
            }
         }
         if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.ACTIVATED.ordinal()) {
            String action = "Activating parcel...";

            executeAndReport(action, apiParcelResource.activateCommand(), ProgressSplit.ACTIVATE_PARCEL.getProgress(),
                  cluster.getCurrentReport(), reportQueue, new StatusPoller() {
               @Override
               public boolean poll() {
                  // activate parcel is pretty fast, so suppose we are no need to do much error handling/progress monitoring
                  // TODO: set a timeout
                  return apiParcelResource.readParcel().getStage().equals(AvailableParcelStage.ACTIVATED.toString());
               }
            }, false);

            if (AvailableParcelStage.valueOf(apiParcelResource.readParcel().getStage()).ordinal() < AvailableParcelStage.ACTIVATED.ordinal()) {
               throw ClouderaManagerException.ACTIVATE_PARCEL_FAIL(apiParcelResource.readParcel().getProduct(),
                     apiParcelResource.readParcel().getVersion(), refMsg);
            }
         }
      }
   }

   public void configureServices(final CmClusterDef cluster, final ClusterReportQueue reportQueue) {
      if (isConfigured(cluster)) {
         // TODO: resize
         syncRolesId(cluster);
         return;
      }
      String action = "Configuring cluster services";
      cluster.getCurrentReport().setAction(action);
      reportQueue.addClusterReport(cluster.getCurrentReport().clone());
      ApiServiceList serviceList = new ApiServiceList();

      for (CmServiceDef serviceDef : cluster.getServices()) {
         String typeName = serviceDef.getType().getDisplayName();
         ApiService apiService = new ApiService();
         apiService.setType(serviceDef.getType().getName());
         apiService.setName(serviceDef.getName());
         apiService.setDisplayName(serviceDef.getDisplayName());

         ApiServiceConfig apiServiceConfig = new ApiServiceConfig();

         if (serviceDef.getConfiguration() != null) {
            for (String key : serviceDef.getConfiguration().keySet()) {
               apiServiceConfig.add(new ApiConfig(key, serviceDef.getConfiguration().get(key)));
            }
         }

         Set<String> serviceTypes = cluster.allServiceTypes();
         switch (typeName) {
            case "YARN": //TODO: Compute Only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType("HDFS")));
               break;
            case "MAPREDUCE": //TODO: Compute Only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType("HDFS")));
               break;
            case "HBASE": //TODO: HBase only
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType("HDFS")));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.serviceNameOfType("ZOOKEEPER")));
               break;
            case "SOLR":
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType("HDFS")));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.serviceNameOfType("ZOOKEEPER")));
               break;
            case "SOLR_INDEXER":
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType("HBASE")));
               apiServiceConfig.add(new ApiConfig("solr_service", cluster.serviceNameOfType("SOLR")));
               break;
            case "HUE":
               apiServiceConfig.add(new ApiConfig("hue_webhdfs", cluster.serviceNameOfType("HDFS_HTTP_FS")));
               apiServiceConfig.add(new ApiConfig("oozie_service", cluster.serviceNameOfType("OOZIE")));
               apiServiceConfig.add(new ApiConfig("hive_service", cluster.serviceNameOfType("HIVE")));
               if (serviceTypes.contains("HBASE")) {
                  apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType("HBASE")));
               }
               if (serviceTypes.contains("IMPALA")) {
                  apiServiceConfig.add(new ApiConfig("impala_service", cluster.serviceNameOfType("IMPALA")));
               }
               if (serviceTypes.contains("SOLR")) {
                  apiServiceConfig.add(new ApiConfig("solr_service", cluster.serviceNameOfType("SOLR")));
               }
               if (serviceTypes.contains("SQOOP")) {
                  apiServiceConfig.add(new ApiConfig("sqoop_service", cluster.serviceNameOfType("SQOOP")));
               }
               if (serviceTypes.contains("HBASE_THRIFT_SERVER")) {
                  apiServiceConfig.add(new ApiConfig("hue_hbase_thrift", cluster
                        .serviceNameOfType("HBASE_THRIFT_SERVER")));
               }
               break;
            case "SQOOP":
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains("YARN") ? cluster.serviceNameOfType("YARN") : cluster
                     .serviceNameOfType("MAPREDUCE")));
               break;
            case "OOZIE":
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains("YARN") ? cluster.serviceNameOfType("YARN") : cluster
                     .serviceNameOfType("MAPREDUCE")));
               break;
            case "HIVE":
               apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", serviceTypes
                     .contains("YARN") ? cluster.serviceNameOfType("YARN") : cluster
                     .serviceNameOfType("MAPREDUCE")));
               apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster
                     .serviceNameOfType("ZOOKEEPER")));
               break;
            case "IMPALA":
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType("HDFS")));
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType("HBASE")));
               apiServiceConfig.add(new ApiConfig("hive_service", cluster.serviceNameOfType("HIVE")));
               break;
            case "FLUME":
               apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.serviceNameOfType("HDFS")));
               apiServiceConfig.add(new ApiConfig("hbase_service", cluster.serviceNameOfType("HBASE")));
            default:
               break;
         }

         apiService.setConfig(apiServiceConfig);

         List<ApiRole> apiRoles = new ArrayList<ApiRole>();
         for (CmRoleDef roleDef : serviceDef.getRoles()) {
            ApiRole apiRole = new ApiRole();
            apiRole.setType(roleDef.getType().getName());
            apiRole.setHostRef(new ApiHostRef(roleDef.getNodeRef()));
            ApiConfigList roleConfigList = new ApiConfigList();
            if (roleDef.getConfiguration() != null) {
               for (String key : roleDef.getConfiguration().keySet()) {
                  roleConfigList.add(new ApiConfig(key, roleDef.getConfiguration().get(key)));
               }
            }
            apiRole.setConfig(roleConfigList);
            apiRoles.add(apiRole);
         }

         apiService.setRoles(apiRoles);
         serviceList.add(apiService);
      }

      try {
         apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName()).createServices(serviceList);
         logger.info("Finished create services");

         updateRoleConfigGroups(cluster.getName());
         logger.info("Updated roles config groups");

         syncRolesId(cluster);

         // Necessary, since createServices a habit of kicking off async commands (eg ZkAutoInit )
         for (CmServiceDef serviceDef : cluster.getServices()) {
            for (ApiCommand command : apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .listActiveCommands(serviceDef.getName(), DataView.SUMMARY)) {
               execute(command);
            }
         }

         executeAndReport("Deploy client config", apiResourceRootV6.getClustersResource().deployClientConfig(cluster.getName()),
               ProgressSplit.CONFIGURE_SERVICES.getProgress(), cluster.getCurrentReport(), reportQueue);
      } catch (Exception e) {
         String errMsg = "Failed to configure services" + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
         logger.error(errMsg);
         throw SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(errMsg, e);
      }
   }

   /**
    * Update base role config groups to avoid showing error msg on CM GUI, these configurations do not take
    * effect actually, will be overridden by each role's configuration.
    * @param clusterName
    * @throws IOException
    */
   private void updateRoleConfigGroups(String clusterName) throws IOException {
      Map<String, String> nameMap = AvailableServiceRoleContainer.nameToDisplayName();
      if (nameMap == null || nameMap.isEmpty()) {
         return;
      }
      ServicesResourceV6 servicesResource = apiResourceRootV6.getClustersResource().getServicesResource(clusterName);
      for (ApiService service : servicesResource.readServices(DataView.SUMMARY)) {
         for (ApiRoleConfigGroup roleConfigGroup : servicesResource.getRoleConfigGroupsResource(service.getName())
               .readRoleConfigGroups()) {
            if (roleConfigGroup == null || !nameMap.containsKey(roleConfigGroup.getRoleType())) {
               continue;
            }

            String roleDisplayName = nameMap.get(roleConfigGroup.getRoleType());
            ApiConfigList configList = new ApiConfigList();
            boolean needUpdate = true;
            switch (roleDisplayName) {
               case "HDFS_NAMENODE":
                  configList.add(new ApiConfig(Constants.CONFIG_DFS_NAME_DIR_LIST, "/tmp/dfs/nn"));
                  break;
               case "HDFS_DATANODE":
                  configList.add(new ApiConfig(Constants.CONFIG_DFS_DATA_DIR_LIST, "/tmp/dfs/dn"));
                  break;
               case "HDFS_SECONDARY_NAMENODE":
                  configList.add(new ApiConfig(Constants.CONFIG_FS_CHECKPOINT_DIR_LIST, "/tmp/dfs/snn"));
                  break;
               case "YARN_NODE_MANAGER":
                  configList.add(new ApiConfig(Constants.CONFIG_NM_LOCAL_DIRS, "/tmp/yarn/nm"));
                  break;
               default:
                  needUpdate = false;
                  break;
            }

            if (needUpdate) {
               logger.info("Updating base role config group of type: " + roleDisplayName);
               roleConfigGroup.setConfig(configList);
               servicesResource.getRoleConfigGroupsResource(service.getName()).updateRoleConfigGroup(roleConfigGroup.getName(),
                     roleConfigGroup, Constants.ROLE_CONFIG_GROUP_UPDATE_NOTES);
            }
         }
      }
   }

   /**
    * start services/roles, assume the roles' IDs are already synched
    * @param cluster
    * @param isFirstStart
    * @return
    * @throws com.vmware.bdd.plugin.clouderamgr.exception.ClouderaManagerException
    */
   private boolean startServices(final CmClusterDef cluster, final ClusterReportQueue reportQueue, final boolean isFirstStart) throws ClouderaManagerException {

      boolean executed = true;
      int endProgress = ProgressSplit.START_SERVICES.getProgress();
      try {
         if (!cluster.isEmpty()) {
            if (!isConfigured(cluster)) {
               configureServices(cluster, reportQueue);
            }
            if (!isStarted(cluster)) {
               int leftServices = cluster.getServices().size();
               for (CmServiceDef serviceDef : cluster.getServices()) {
                  if (isFirstStart) {
                     // pre start
                     preStartServices(cluster, serviceDef, reportQueue);
                     for (CmRoleDef roleDef : serviceDef.getRoles()) {
                        preStartRoles(cluster, serviceDef, roleDef, reportQueue);
                     }
                  }

                  // start
                  startService(cluster, serviceDef, reportQueue);

                  if (isFirstStart) {
                     // post start
                     postStartServices(cluster, serviceDef, reportQueue);
                     for (CmRoleDef roleDef : serviceDef.getRoles()) {
                        postStartRoles(cluster, serviceDef, roleDef, reportQueue);
                     }
                  }
                  if (leftServices > 0) {
                     int currentProgress = cluster.getCurrentReport().getProgress();
                     int toProgress = currentProgress + (endProgress - currentProgress) / leftServices;
                     if (toProgress != currentProgress) {
                        cluster.getCurrentReport().setProgress(toProgress);
                        reportQueue.addClusterReport(cluster.getCurrentReport().clone());
                     }
                  }
                  leftServices -= 1;
               }
            } else {
               executed = false;
            }

            // push into provision phase once OPSAPS-13194/OPSAPS-12870 is addressed
            startManagement();

            cluster.getCurrentReport().setProgress(endProgress);
            reportQueue.addClusterReport(cluster.getCurrentReport().clone());
         }
      } catch (Exception e) {
         String errMsg = "Failed to start services" + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
         logger.error(errMsg);
         throw SoftwareManagementPluginException.START_SERVICE_FAILED(errMsg, e);
      }

      return executed;
   }

   private void preStartServices(final CmClusterDef cluster, CmServiceDef serviceDef, final ClusterReportQueue reportQueue) throws Exception {
      switch (serviceDef.getType().getDisplayName()) {
         case "HIVE":
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHiveWarehouseCommand(serviceDef.getName()));
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .hiveCreateMetastoreDatabaseTablesCommand(serviceDef.getName()), false);
            break;
         case "OOZIE":
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .installOozieShareLib(serviceDef.getName()), false);
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .createOozieDb(serviceDef.getName()), false);
            break;
         case "HBASE":
            executeAndReport("Creating HBase Root Dir", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createHBaseRootCommand(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
         case "ZOOKEEPER":

            executeAndReport("Initializing Zookeeper", apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .zooKeeperInitCommand(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .zooKeeperInitCommand(serviceDef.getName()), false);
            break;
         case "SOLR":
            execute(
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .initSolrCommand(serviceDef.getName()), false);
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createSolrHdfsHomeDirCommand(serviceDef.getName()));
            break;
         case "SQOOP":
            execute(apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                  .createSqoopUserDirCommand(serviceDef.getName()));
            break;
         default:
            break;
      }
   }

   private void preStartRoles(final CmClusterDef cluster, CmServiceDef serviceDef, CmRoleDef roleDef, ClusterReportQueue reportQueue) throws Exception {

      switch (roleDef.getType().getDisplayName()) {
         case "HDFS_NAMENODE":
            executeAndReport("Formating Namenode",
                  apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName())
                        .getRoleCommandsResource(serviceDef.getName()).formatCommand(
                        new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "YARN_RESOURCE_MANAGER":
            executeAndReport("Creating Remote Log Dir for ResourceManager", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).createYarnNodeManagerRemoteAppLogDirCommand(
                        serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "YARN_JOB_HISTORY":
            executeAndReport("Creating Dir for JobHistory", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).createYarnJobHistoryDirCommand(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, true);
            break;
         case "HUE_SERVER":
            executeAndReport("Syncing up Hue Database", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).getRoleCommandsResource(serviceDef.getName())
                  .syncHueDbCommand(new ApiRoleNameList(ImmutableList.<String>builder().add(roleDef.getName()).build())),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            break;
         default:
            break;
      }
   }

   private void postStartServices(final CmClusterDef cluster, CmServiceDef serviceDef, final ClusterReportQueue reportQueue) throws Exception {
      switch (serviceDef.getType().getDisplayName()) {
         default:
            break;
      }
   }

   private void postStartRoles(final CmClusterDef cluster, CmServiceDef serviceDef, CmRoleDef roleDef, final ClusterReportQueue reportQueue) throws Exception {
      switch (roleDef.getType().getDisplayName()) {
         case "HDFS_NAMENODE":
            ApiRoleNameList formatList = new ApiRoleNameList();
            formatList.add(roleDef.getName());
            executeAndReport("Creating HDFS Temp Dir", apiResourceRootV6.getClustersResource()
                  .getServicesResource(cluster.getName()).hdfsCreateTmpDir(serviceDef.getName()),
                  INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
            break;
         default:
            break;
      }
   }

   private void startService(CmClusterDef cluster, CmServiceDef serviceDef, final ClusterReportQueue reportQueue) throws Exception {
      executeAndReport("Starting Service " + serviceDef.getType().getDisplayName(), apiResourceRootV6.getClustersResource()
            .getServicesResource(cluster.getName()).startCommand(serviceDef.getName()),
            INVALID_PROGRESS, cluster.getCurrentReport(), reportQueue, false);
   }

   private void startManagement() {
      try {
         if (apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().readService(DataView.SUMMARY)
               .getServiceState().equals(ApiServiceState.STOPPED)) {
            logger.info("Starting Cloudera Management Service");
            execute(apiResourceRootV6.getClouderaManagerResource().getMgmtServiceResource().startCommand());
         }
      } catch (Exception e) {
         // ignore
      }
   }


   public void unconfigureServices(final CmClusterDef cluster) throws Exception {
      for (String serviceName : cluster.allServiceNames()) {
         ServicesResourceV6 servicesResource = apiResourceRootV6.getClustersResource().getServicesResource(cluster.getName());
         if (servicesResource != null) {
            servicesResource.deleteService(serviceName);
         }
      }
   }

   public String getCmServerHostId() {
      if (cmServerHostId != null) {
         return cmServerHostId;
      }

      for (ApiHost apiHost : apiResourceRootV6.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
         if (apiHost.getIpAddress().equals(cmServerHost) || apiHost.getHostname().equalsIgnoreCase(cmServerHost)) {
            this.cmServerHostId = apiHost.getHostId();
            return cmServerHostId;
         }
      }
      throw new SoftwareManagementPluginException("Cannot fetch the hostId of cloudera manager server");
   }

   private ApiCommand execute(final ApiBulkCommandList bulkCommand, boolean checkReturn) throws Exception {
      ApiCommand lastCommand = null;
      for (ApiCommand command : bulkCommand) {
         lastCommand = execute(command, checkReturn);
      }
      return lastCommand;
   }

   private ApiCommand execute(final ApiCommand command) throws Exception {
      return execute(command, true);
   }

   private ApiCommand execute(final ApiCommand command, boolean checkReturn) throws Exception {
      return executeAndReport(null, command, INVALID_PROGRESS, null, null, checkReturn);
   }

   private ApiCommand executeAndReport(String action, final ApiCommand command, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue) throws Exception {
      return executeAndReport(action, command, endProgress, currentReport, reportQueue, true);
   }

   private ApiCommand executeAndReport(String action, final ApiBulkCommandList bulkCommand, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue, boolean checkReturn) throws Exception {
      ApiCommand lastCommand = null;
      for (ApiCommand command : bulkCommand) {
         lastCommand = executeAndReport(action, command, endProgress, currentReport, reportQueue, checkReturn);
      }
      return lastCommand;
   }

   private ApiCommand executeAndReport(String action, final ApiCommand command, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue, boolean checkReturn) throws Exception {
      return executeAndReport(action, command, endProgress, currentReport, reportQueue,
            new StatusPoller() {
               @Override
               public boolean poll() {
                  return apiResourceRootV6.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
               }
            }, checkReturn);
   }

   private ApiCommand executeAndReport(String action, final ApiCommand command, int endProgress,
         ClusterReport currentReport, ClusterReportQueue reportQueue, StatusPoller poller,
         boolean checkReturn) throws Exception {

      if (action != null) {
         logger.info("Action: " + action);
         currentReport.setClusterAndNodesAction(action);
         reportQueue.addClusterReport(currentReport.clone());
      }

      ApiCommand commandReturn = null;
      poller.waitForComplete();

      if (checkReturn && command != null
            && !(commandReturn = apiResourceRootV6.getCommandsResource().readCommand(command.getId())).getSuccess()) {
         logger.info("Failed to run command: " + command);
         String errorMsg = command.getResultMessage();
         if (errorMsg == null) {
            if (command.getResultDataUrl() != null) {
               errorMsg = referCmfUrlMsg(command.getResultDataUrl());
            } else {
               errorMsg = referCmfUrlMsg(domain + "/cmf/command/" + command.getId() + "/details");
            }
         }
         throw new RuntimeException(errorMsg);
      }

      if (endProgress != INVALID_PROGRESS) {
         if (currentReport.getProgress() < endProgress) {
            currentReport.setProgress(endProgress);
            reportQueue.addClusterReport(currentReport.clone());
         }
      }

      return commandReturn;
   }

   private String referCmfUrlMsg(String url) {
      return "Please refer to " + url + " for details";
   }

   private static abstract class Retriable {
      public abstract void doWork() throws Exception;
   }

   private void retry(int retryTimes, Retriable operate) throws Exception {
      int i = 0;
      while (true) {
         i += 1;
         try {
            operate.doWork();
            return;
         } catch (Exception e) {
            if (i == retryTimes) {
               logger.info("Failed for all " + retryTimes + " times");
               throw e;
            } else {
               logger.info(
                     "Failed for the " + i + "st round: " + e.getMessage() + ", have " + (retryTimes - i) + " times to retry");
            }
         }
      }
   }

   @Override
   public List<String> validateScaling(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return new ArrayList<String>();
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean hasHbase(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean hasMgmtRole(List<String> roles) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean isComputeOnlyRoles(List<String> roles) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean hasComputeMasterGroup(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   /* (non-Javadoc)
    * @see com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager#getVersion()
    */
   @Override
   public String getVersion() {
      try {
         return apiResourceRootV6.getClouderaManagerResource().getVersion().getVersion();
      } catch (Exception e) {
         return "UNKNOWN";
      }
   }

   @Override
   public HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException {
      List<HadoopStack> hadoopStacks = getSupportedStacks();
      Collections.<HadoopStack> sort(hadoopStacks);
      return hadoopStacks.get(0);
   }
}
