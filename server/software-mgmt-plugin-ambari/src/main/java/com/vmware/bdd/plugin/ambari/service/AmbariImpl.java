/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.vmware.aurora.util.HbaseRegionServerOptsUtil;
import com.vmware.bdd.apitypes.LatencyPriority;
import com.vmware.bdd.exception.SoftwareManagerCollectorException;
import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager_1_7_0;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager_2_0_0;
import com.vmware.bdd.plugin.ambari.api.model.ApiPersist;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapHostStatus;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.BootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterConfigurationInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterConfigurations;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupConfiguration;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponent;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponentsRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiService;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTask;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTaskInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ClusterRequestStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.TaskStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiConfiguration;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiConfigurationInfo;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackVersionInfo;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.exception.AmException;
import com.vmware.bdd.plugin.ambari.model.AmClusterDef;
import com.vmware.bdd.plugin.ambari.model.AmHostGroupInfo;
import com.vmware.bdd.plugin.ambari.model.AmNodeDef;
import com.vmware.bdd.plugin.ambari.model.AmNodeGroupDef;
import com.vmware.bdd.plugin.ambari.poller.ClusterOperationPoller;
import com.vmware.bdd.plugin.ambari.poller.ExternalNodesRegisterPoller;
import com.vmware.bdd.plugin.ambari.poller.HostBootstrapPoller;
import com.vmware.bdd.plugin.ambari.spectypes.HadoopRole;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.AbstractSoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.monitor.NodeReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;
import com.vmware.bdd.software.mgmt.plugin.utils.ReflectionUtils;
import com.vmware.bdd.software.mgmt.plugin.utils.ValidateRolesUtil;

public class AmbariImpl extends AbstractSoftwareManager implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(AmbariImpl.class);

   private final int REQUEST_MAX_RETRY_TIMES = 10;
   public static final String MIN_SUPPORTED_VERSION = "1.6.0";
   private static final String UNKNOWN_VERSION = "UNKNOWN";

   private final String privateKey;

   private ApiManager apiManager;

   private enum ProgressSplit {
      BOOTSTRAP_HOSTS(10),
      CREATE_BLUEPRINT(30),
      PROVISION_CLUSTER(50),
      PROVISION_SUCCESS(100),
      OPERATION_BEGIN(0),
      OPERATION_FINISHED(100);

      private int progress;

      private ProgressSplit(int progress) {
         this.progress = progress;
      }

      public int getProgress() {
         return progress;
      }
   }

   public AmbariImpl(String amServerHost, int port, String username,
         String password, String privateKey) {
      this.apiManager = new ApiManager(amServerHost, port, username, password);
      this.privateKey = privateKey;
   }

   public AmbariImpl(URL url, String username, String password, String privateKey) {
      ApiManager tmpApiManager = new ApiManager(url, username, password);
      String ambariVersion = tmpApiManager.getVersion();
      logger.info("Ambari version is " + ambariVersion);
      if (AmUtils.isAmbariServerBelow_2_0_0(ambariVersion)) {
         this.apiManager = new ApiManager_1_7_0(url, username, password);
      } else {
         this.apiManager = new ApiManager_2_0_0(url, username, password);
      }
      this.privateKey = privateKey;
   }

   protected AmbariImpl(AmbariManagerClientbuilder clientbuilder, String privateKey) {
      this.apiManager = new ApiManager(clientbuilder);
      this.privateKey = privateKey;
   }

   @Override
   public String getName() {
      return Constants.AMBARI_PLUGIN_NAME;
   }

   @Override
   public String getDescription() {
      return null;
   }

   @Override
   public String getType() {
      return Constants.AMBARI_PLUGIN_NAME;
   }

   @Override
   public boolean echo() {
      try {
         switch (apiManager.healthCheck()) {
         case Constants.HEALTH_STATUS:
            return true;
         default:
            return false;
         }
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public boolean validateServerVersion() throws SoftwareManagerCollectorException {
      String version = getVersion();
      logger.info("Min supported version of " + getType() + " is: " + MIN_SUPPORTED_VERSION);
      logger.info("Version of new software manager is: " + version);
      //For ambari, we only support 1.6.0 and 1.6.1, its next version is 1.7.0, so only need to check major and minor version
      if (version.equals(UNKNOWN_VERSION)) {
         logger.error("Validate server version failed.");
         throw SoftwareManagerCollectorException.INVALID_VERSION(Constants.AMBARI_PLUGIN_NAME, version);
      }
      logger.info("Validate server version succeed.");
      return true;
   }

   @Override
   public HealthStatus getStatus() {
      switch (apiManager.healthCheck()) {
      case Constants.HEALTH_STATUS:
         return HealthStatus.Connected;
      default:
         return HealthStatus.Disconnected;
      }
   }

   @Override
   public List<HadoopStack> getSupportedStacks()
         throws SoftwareManagementPluginException {
      List<HadoopStack> hadoopStacks = new ArrayList<HadoopStack>();
      ApiStackList stackList = apiManager.getStackList();
      for (ApiStack apiStack : stackList.getApiStacks()) {
         for (ApiStackVersion apiStackVersionSummary : apiManager
               .getStackVersionList(apiStack.getApiStackName().getStackName())
               .getApiStackVersions()) {
            ApiStackVersionInfo apiStackVersionInfoSummary =
                  apiStackVersionSummary.getApiStackVersionInfo();
            ApiStackVersion apiStackVersion =
                  apiManager.getStackVersion(
                          apiStackVersionInfoSummary.getStackName(),
                          apiStackVersionInfoSummary.getStackVersion());
            ApiStackVersionInfo apiStackVersionInfo =
                  apiStackVersion.getApiStackVersionInfo();
            if (apiStackVersionInfo.isActive()) {
               HadoopStack hadoopStack = new HadoopStack();
               hadoopStack.setDistro(apiStackVersionInfo.getStackName(),
                     apiStackVersionInfo.getStackVersion());
               hadoopStack
               .setFullVersion(apiStackVersionInfo.getStackVersion());
               hadoopStack.setVendor(apiStackVersionInfo.getStackName());

               List<String> roles = new ArrayList<String>();
               ApiStackServiceList apiStackServiceList =
                     apiManager.getStackServiceListWithComponents(
                           hadoopStack.getVendor(),
                           hadoopStack.getFullVersion());
               for (ApiStackService apiStackService : apiStackServiceList
                     .getApiStackServices()) {
                  for (ApiStackComponent apiComponent : apiStackService
                        .getServiceComponents()) {
                     roles.add(apiComponent.getApiComponent()
                           .getComponentName());
                  }
               }
               hadoopStack.setHveSupported(true);
               hadoopStack.setRoles(roles);

               hadoopStacks.add(hadoopStack);
            }
         }
      }
      return hadoopStacks;
   }

   @SuppressWarnings("unchecked")
   @Override
   public String getSupportedConfigs(HadoopStack stack) {
      Map<String, Object> configs = new HashMap<String, Object>();
      ApiStackServiceList apiStackServiceList = apiManager.getStackServiceListWithConfigurations(stack.getVendor(), stack.getFullVersion());
      for (ApiStackService apiStackService : apiStackServiceList.getApiStackServices()) {
         for (ApiConfiguration apiConfiguration : apiStackService.getApiConfigurations()) {
            ApiConfigurationInfo apiConfigurationInfo = apiConfiguration.getApiConfigurationInfo();
            String configType = apiConfigurationInfo.getType().split(".xml")[0];
            String configProperty = apiConfigurationInfo.getPropertyName();
            Set<String> configProperties = new HashSet<String>();
            if (configs.isEmpty()) {
               configProperties.add(configProperty);
            } else {
               if (configs.containsKey(configType)) {
                  configProperties = (Set<String>) configs.get(configType);
                  configProperties.add(configProperty);
               }
            }
            configs.put(configType, configProperties);
         }
      }
      return ApiUtils.objectToJson(configs);
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reportQueue)
               throws SoftwareManagementPluginException {
      boolean success = false;
      AmClusterDef clusterDef = null;
      try {
         logger.info("Blueprint:");
         logger.info(ApiUtils.objectToJson(blueprint));
         logger.info("Start cluster " + blueprint.getName() + " creation.");

         setHaseRegionConfig(blueprint);
         logger.info("Blueprint: After  setHaseRegionConfig");
         logger.info(ApiUtils.objectToJson(blueprint));

         String ambariServerVersion = getVersion();
         clusterDef = new AmClusterDef(blueprint, privateKey, ambariServerVersion);

         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterDef.getName());

         provisionCluster(clusterDef, reportQueue);
         success = true;

         // All nodes use cluster message after cluster provision successfully.
         Map<String, NodeReport> nodeReports = clusterDef.getCurrentReport().getNodeReports();
         for (String nodeReportKey : nodeReports.keySet()) {
            nodeReports.get(nodeReportKey).setUseClusterMsg(true);
         }
         clusterDef.getCurrentReport().setAction("");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_SUCCESS.getProgress());
         clusterDef.getCurrentReport().setSuccess(success);
      } catch (Exception e) {
         clusterDef.getCurrentReport().setSuccess(success);
         logger.error("Failed to create cluster " + blueprint.getName(), e);

         throw SoftwareManagementPluginException.CREATE_CLUSTER_EXCEPTION(e, Constants.AMBARI_PLUGIN_NAME, blueprint.getName());
      } finally {
         clusterDef.getCurrentReport().setFinished(true);

         if(success) {
            clusterDef.getCurrentReport().setClusterAndNodesServiceStatus(ServiceStatus.STARTED);
         }

         reportStatus(clusterDef.getCurrentReport(), reportQueue);
      }
      return success;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      return null;
   }

   protected boolean isProvisioned(String clusterName)
         throws SoftwareManagementPluginException {
      try {
         for (ApiCluster apiCluster : apiManager.getClusterList().getClusters()) {
            if (apiCluster.getClusterInfo().getClusterName()
                  .equals(clusterName)) {
               return true;
            }
         }
      } catch (Exception e) {
         throw AmException.UNSURE_CLUSTER_EXIST(Constants.AMBARI_PLUGIN_NAME, clusterName);
      }
      return false;
   }

   private void provisionCluster(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
               throws SoftwareManagementPluginException {
      try {
         if (!isProvisioned(clusterDef.getName()) || isClusterProvisionedByBDE(clusterDef)) {
            bootstrap(clusterDef, reportQueue);

            registerExternalNodes(clusterDef);

            createBlueprint(clusterDef, reportQueue);

            provisionWithBlueprint(clusterDef, reportQueue);

            setClusterHostsRackInfo(clusterDef);

            restartRequiredServices(clusterDef, reportQueue);
         }

      } catch (Exception e) {
         String errorMessage = errorMessage("Failed to provision cluster " + clusterDef.getName(), e);
         logger.error(errorMessage);

         throw SoftwareManagementPluginException.CREATE_CLUSTER_EXCEPTION(e, Constants.AMBARI_PLUGIN_NAME, clusterDef.getName());
      }
   }

   private void setClusterHostsRackInfo(final AmClusterDef clusterDef) throws SoftwareManagementPluginException {
      setClusterHostsRackInfo(clusterDef, null);
   }

   private void setClusterHostsRackInfo(final AmClusterDef clusterDef, final List<String> addedNodeNames) throws SoftwareManagementPluginException {
      Map<String, Set<String>> rackHostsMap = clusterDef.getRackHostsMap(addedNodeNames);

      if (rackHostsMap == null) {
         return;
      }

      for (String rackinfo : rackHostsMap.keySet()) {
         apiManager.setClusterHostsRackInfo(clusterDef.getName(), rackinfo, rackHostsMap.get(rackinfo));
      }
   }

   protected boolean isClusterProvisionedByBDE(final AmClusterDef clusterDef) {
      /*
      For cluster resume/resize, the cluster is already exist, we need to check if this cluster is created by BDE.
      So far, just check if all hostnames exist in Ambari Cluster are included in given blueprint. To avoid potential
      user limitation, will not throw out any exception, just give out an warning
       */
      String clusterName = clusterDef.getName();
      Set<String> hostnames = new HashSet<String>();
      for (AmNodeDef node : clusterDef.getNodes()) {
         hostnames.add(node.getFqdn());
      }
      for (ApiHost apiHost : apiManager.getCluster(clusterName).getApiHosts()) {
         if (!hostnames.contains(apiHost.getApiHostInfo().getHostName())) {
            logger.warn("Host " + apiHost.getApiHostInfo().getHostName() + " managed by Ambari doesn't exists in BDE");
         }
      }
      return true;
   }

   private void registerExternalNodes(final AmClusterDef clusterDef) throws SoftwareManagementPluginException {

      String failedExternalHosts = null;

      try {
         List<String> externalNodes = new ArrayList<String> ();

         if (clusterDef.isValidExternalNamenode()) {
            externalNodes.add(clusterDef.getExternalNamenode());
         }

         if(clusterDef.isValidExternalSecondaryNamenode()) {
            externalNodes.add(clusterDef.getExternalSecondaryNamenode());
         }

         if (!externalNodes.isEmpty()) {
            ExternalNodesRegisterPoller poller = new ExternalNodesRegisterPoller(apiManager, externalNodes);
            poller.waitForComplete();
            if (poller.getRegisterFailedHosts() != null && !poller.getRegisterFailedHosts().isEmpty()) {
               failedExternalHosts = poller.getRegisterFailedHosts().toString();
               throw AmException.REGISTER_HOSTS_FAILED_EXCEPTION(null, failedExternalHosts, clusterDef.getName());
            }
         }
      } catch (Exception e) {
         String errorMessage = errorMessage("Failed to register external hosts of cluster " + clusterDef.getName(), e);
         logger.error(errorMessage);
         throw AmException.REGISTER_HOSTS_FAILED_EXCEPTION(e, failedExternalHosts, clusterDef.getName());
      }
   }

   private void bootstrap(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
               throws SoftwareManagementPluginException {
      bootstrap(clusterDef, null, reportQueue, false);
   }

   private void bootstrap(final AmClusterDef clusterDef,
         final List<String> addedHosts,
         final ClusterReportQueue reportQueue, boolean force)
               throws SoftwareManagementPluginException {
      try {
         if (addedHosts != null) {
            logger.info("Bootstrapping hosts " + addedHosts);
            clusterDef.getCurrentReport().setNodesAction("Bootstrapping host", addedHosts);
            clusterDef.getCurrentReport().setProgress(
                  ProgressSplit.BOOTSTRAP_HOSTS.getProgress());
         } else {
            logger.info("Bootstrapping hosts of cluster " + clusterDef.getName());
            clusterDef.getCurrentReport().setAction("Bootstrapping host");
            clusterDef.getCurrentReport().setProgress(
                  ProgressSplit.BOOTSTRAP_HOSTS.getProgress());
         }
         reportStatus(clusterDef.getCurrentReport(), reportQueue);

         ApiBootstrap apiBootstrap = clusterDef.toApiBootStrap(addedHosts);

         removeRegisteredHosts(apiBootstrap);

         if (CollectionUtils.isEmpty(apiBootstrap.getHosts())) {
            logger.info("No hosts need to boostrap Because all hosts are registered.");
            return;
         }

         ApiBootstrap apiBootstrapRequest = apiManager.createBootstrap(apiBootstrap);

         HostBootstrapPoller poller =
               new HostBootstrapPoller(apiManager, apiBootstrapRequest,
                     clusterDef.getCurrentReport(), reportQueue,
                     ProgressSplit.CREATE_BLUEPRINT.getProgress());
         poller.waitForComplete();
         logger.debug("Bootstrap request id: " + apiBootstrapRequest.getRequestId());

         boolean success = false;
         boolean allHostsBootstrapped = true;
         ApiBootstrapStatus apiBootstrapStatus =
               apiManager.getBootstrapStatus(apiBootstrapRequest.getRequestId());
         BootstrapStatus bootstrapStatus =
               BootstrapStatus.valueOf(apiBootstrapStatus.getStatus());
         logger.debug("Bootstrap status " + bootstrapStatus);
         if (!bootstrapStatus.isFailedState()) {
            success = true;
         }

         int bootstrapedHostCount = apiBootstrapStatus.getApiBootstrapHostStatus().size();

         int needBootstrapHostCount = clusterDef.getNeedBootstrapHostCount(addedHosts);

         logger.debug("Need to bootstrap host number: " + needBootstrapHostCount);
         logger.debug("Got bootstrap status number: " + bootstrapedHostCount);
         if (needBootstrapHostCount != bootstrapedHostCount) {
            success = false;
            allHostsBootstrapped = false;
         }
         if (!success) {
            List<String> notBootstrapNodes = new ArrayList<String>();
            if (!allHostsBootstrapped) {
               for (AmNodeDef node : clusterDef.getNodes()) {
                  boolean nodeBootstrapped = false;
                  for (ApiBootstrapHostStatus apiBootstrapHostStatus : apiBootstrapStatus
                        .getApiBootstrapHostStatus()) {
                     if (node.getFqdn().equals(
                           apiBootstrapHostStatus.getHostName())) {
                        nodeBootstrapped = true;
                        break;
                     }
                  }
                  if (!nodeBootstrapped) {
                     notBootstrapNodes.add(node.getFqdn());
                  }
               }
            }
            setBootstrapNodeError(clusterDef, addedHosts);
            if (!force ||
                  //if use force, but all nodes failed to boostrap, throw exception
                  (force && (bootstrapedHostCount == 0))) {
               throw AmException.BOOTSTRAP_FAILED(notBootstrapNodes != null? notBootstrapNodes.toArray() : null);
            }
         }
      } catch (Exception e) {
         setBootstrapNodeError(clusterDef, addedHosts);
         String errorMessage = errorMessage("Failed to bootstrap hosts of cluster " + clusterDef.getName(), e);
         logger.error(errorMessage);
         throw AmException.BOOTSTRAP_FAILED_EXCEPTION(e, clusterDef.getName());
      } finally {
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
   }

   private void removeRegisteredHosts(ApiBootstrap apiBootstrap) {
      // No need to bootstrap registered hosts
      List<ApiHost> registeredHosts = apiManager.getRegisteredHosts().getApiHosts();
      List<String> boostrapHosts = apiBootstrap.getHosts();

      for (ApiHost registeredHost : registeredHosts) {
         boostrapHosts.remove(registeredHost.getApiHostInfo().getHostName());
      }

      apiBootstrap.setHosts(boostrapHosts);
   }

   private void setBootstrapNodeError(final AmClusterDef clusterDef,
         final List<String> addedHosts) {
      String actionFailure = Constants.HOST_BOOTSTRAP_MSG;
      if (addedHosts != null) {
         clusterDef.getCurrentReport().setNodesError(actionFailure, addedHosts);
      } else {
         clusterDef.getCurrentReport().setErrMsg(actionFailure);
      }
   }

   private void createBlueprint(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
               throws SoftwareManagementPluginException {
      String clusterName = clusterDef.getName();
      try {
         logger.info("Creating blueprint of cluster " + clusterName);
         clusterDef.getCurrentReport().setAction("Create blueprint");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.CREATE_BLUEPRINT.getProgress());
         reportStatus(clusterDef.getCurrentReport(), reportQueue);

         ApiBlueprint apiBlueprint = clusterDef.toApiBlueprint();
         if (!isBlueprintCreated(clusterDef)) {
            apiManager.createBlueprint(clusterName, apiBlueprint);
         } else {
            // For cluster resume/resize, the blueprint is already exist, we need to delete this blueprint first.
            if (isBlueprintCreatedByBDE(clusterDef)) {
               apiManager.deleteBlueprint(clusterName);
            }

            apiManager.createBlueprint(clusterName, apiBlueprint);
         }
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to create blueprint");
         String errorMessage = errorMessage("Failed to create blueprint of cluster " + clusterName, e);
         logger.error(errorMessage);
         throw AmException.CREATE_BLUEPRINT_FAILED(e, clusterName);
      } finally {
         reportStatus(clusterDef.getCurrentReport(), reportQueue);
      }
   }

   private boolean isBlueprintCreated(final AmClusterDef clusterDef)
         throws SoftwareManagementPluginException {
      try {
         for (ApiBlueprint apiBlueprint : apiManager.getBlueprintList()
               .getApiBlueprints()) {
            if (clusterDef.getName().equals(
                  apiBlueprint.getApiBlueprintInfo().getBlueprintName())) {
               return true;
            }
         }
      } catch (Exception e) {
         throw AmException.UNSURE_BLUEPRINT_EXIST(clusterDef.getName());
      }
      return false;
   }

   private boolean isBlueprintCreatedByBDE(final AmClusterDef clusterDef)
         throws SoftwareManagementPluginException {
      /*
      For cluster resume/resize, the blueprint is already exist, we need to check if this blueprint is created by BDE.
      So far, just check if all goup names and components exist in Ambari Cluster are included in given blueprint
       */
      ApiBlueprint apiBlueprint = clusterDef.toApiBlueprint();
      String clusterName = clusterDef.getName();
      ApiBlueprint apiBlueprintFromAm = apiManager.getBlueprint(clusterName);

      Map<String, Set> groupNamesWithComponents = new HashMap<String, Set>();
      for (ApiHostGroup hostGroup : apiBlueprint.getApiHostGroups()) {
         HashSet<String> components = new HashSet<String>();
         groupNamesWithComponents.put(hostGroup.getName(), components);
      }

      for (ApiHostGroup apiHostGroup : apiBlueprintFromAm.getApiHostGroups()) {
         String groupName = apiHostGroup.getName();
         if (!groupNamesWithComponents.containsKey(groupName)) {
            throw AmException.BLUEPRINT_ALREADY_EXIST(clusterName);
         }
         @SuppressWarnings("unchecked")
         Set<String> components = groupNamesWithComponents.get(groupName);
         if (components != null && !components.isEmpty()) {
            for (ApiComponentInfo apiComponent : apiHostGroup.getApiComponents()) {
               if (!components.contains(apiComponent.getName())) {
                  throw AmException.BLUEPRINT_ALREADY_EXIST(clusterName);
               }
            }
         }
      }
      return true;
   }

   private void provisionWithBlueprint(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
               throws SoftwareManagementPluginException {
      try {
         logger.info("Provisioning cluster " + clusterDef.getName() + " with blueprint " + clusterDef.getName());
         clusterDef.getCurrentReport().setAction(
               "Provisioning cluster with blueprint");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_CLUSTER.getProgress());
         reportStatus(clusterDef.getCurrentReport(), reportQueue);

         String clusterName = clusterDef.getName();

         // For cluster resume/resize, the blueprint is already exist, we need to delete this cluster first.
         if (isProvisioned(clusterName) && isClusterProvisionedByBDE(clusterDef)) {
            try {
               if (hasHosts(clusterName)) {
                  ApiRequest apiRequestSummary = apiManager.stopAllServicesInCluster(clusterName);
                  doSoftwareOperation(clusterName, apiRequestSummary, clusterDef.getCurrentReport(), reportQueue);
               }
            } catch (Exception e) {
               logger.error("Failed to stop all services: ", e);
               throw SoftwareManagementPluginException.STOP_CLUSTER_EXCEPTION(e, Constants.AMBARI_PLUGIN_NAME, clusterName);
            }
            apiManager.deleteCluster(clusterName);
         }

         ApiRequest apiRequestSummary =
               apiManager.provisionCluster(clusterDef.getName(),
                     clusterDef.toApiClusterBlueprint());

         ClusterOperationPoller poller =
               new ClusterOperationPoller(apiManager, apiRequestSummary,
                     clusterName, clusterDef.getCurrentReport(), reportQueue,
                     ProgressSplit.PROVISION_SUCCESS.getProgress());
         poller.waitForComplete();

         boolean success = false;
         ApiRequest apiRequest =
               apiManager.getRequest(clusterName, apiRequestSummary
                     .getApiRequestInfo().getRequestId());
         ClusterRequestStatus clusterRequestStatus =
               ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                     .getRequestStatus());
         if (!clusterRequestStatus.isFailedState()) {
            success = true;
         }
         if (!success) {
            throw SoftwareManagementPluginException.CREATE_CLUSTER_FAIL(Constants.AMBARI_PLUGIN_NAME, clusterDef.getName());
         }

      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction(
               "Failed to provision cluster with blueprint");
         String errorMessage = errorMessage("Failed to provision cluster " + clusterDef.getName() + " with blueprint", e);
         logger.error(errorMessage);
         throw AmException.PROVISION_WITH_BLUEPRINT_FAILED(e, clusterDef.getName());
      } finally {
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
   }

   private void setHaseRegionConfig(ClusterBlueprint blueprint) {
      //Check whether latencySensivity is set to high,and the roles include HBASE_REGIONSERVER, then set HBASE_REGIONSERVER_OPTS
      List<ApiConfiguration> configList =
            apiManager.getServiceConfiguration(
                  blueprint.getHadoopStack().getVendor(),
                  blueprint.getHadoopStack().getFullVersion(),
                  "HBASE");
      logger.info("Get Ambari hbase opts template");
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (group.getLatencySensitivity() == (LatencyPriority.HIGH)
               && group.getRoles().contains(
               HadoopRole.HBASE_REGIONSERVER_ROLE.toString())) {
            logger.info("Start to set hbase region opts");
            setHbaseAmbariRegionServerOpts(configList, group);
            break;
         }
      }
   }
   /**
    * Set HBASE_REGIONSERVER_OPTS for ambari hbase regionserver
    * @param configList
    * @param group
    */
   private void setHbaseAmbariRegionServerOpts(List<ApiConfiguration> configList, NodeGroupInfo group){
      String configurationType = "hbase-env";
      Map<String, Object> conf = group.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         group.setConfiguration(conf);
      }
      Map<String, Object> confHbaseEnv =
            (Map<String, Object>) conf.get(configurationType);
      if (confHbaseEnv == null) {
         confHbaseEnv = new HashMap<String, Object>();
         conf.put(configurationType, confHbaseEnv);
      }
      if (confHbaseEnv.get("hbase_regionserver_heapsize") == null) {
         long hbaseHeapsizeMhz = HbaseRegionServerOptsUtil.getHeapSizeMhz(
               group.getMemorySize(), group.getRoles().size());
         confHbaseEnv.put("hbase_regionserver_heapsize",
               String.valueOf(hbaseHeapsizeMhz) + "m");
      }
      if (confHbaseEnv.get("hbase_regionserver_xmn_ratio") == null)
         confHbaseEnv.put("hbase_regionserver_xmn_ratio", "0.33");
      if (confHbaseEnv.get("content") == null) {
         String hbase_env_template = null;
         //Get hbase regionserver opts jinja template from ambari rest api
         for (ApiConfiguration tmp : configList) {
            if (tmp.getApiConfigurationInfo().getType()
                  .contains(configurationType)) {
               hbase_env_template = tmp.getApiConfigurationInfo().getPropertyValue();
               hbase_env_template = hbase_env_template.replaceAll(" -XX:CMSInitiatingOccupancyFraction=\\d\\d ", " ");
               hbase_env_template = hbase_env_template + HbaseRegionServerOptsUtil
                           .getAmbariHbaseRegionServerStringParameter();
               break;
            }
         }

         confHbaseEnv.put("content", hbase_env_template);

         logger.info("hbase xmn ratio is " + confHbaseEnv
               .get("hbase_regionserver_xmn_ratio"));
         logger.info("hbase ops is " + confHbaseEnv.get("content"));
         logger.info("hbase regionserver heap size is " + confHbaseEnv
               .get("hbase_regionserver_heapsize"));
      }
   }

   private boolean hasHosts(String clusterName) {
      return !apiManager.getClusterHostsList(clusterName).getApiHosts().isEmpty();
   }

   @Override
   public List<String> validateRolesForScaleOut(NodeGroupInfo group) {
      // resize of job tracker and name node is not supported
      List<String> roles = group.getRoles();
      List<String> unsupportedRoles = new ArrayList<String>();
      if (roles.isEmpty()) {
         // no unsupported roles
         return new ArrayList<String>();
      }
      if (roles.contains(HadoopRole.NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.SECONDARY_NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.SECONDARY_NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.JOBTRACKER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.JOBTRACKER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.RESOURCEMANAGER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.RESOURCEMANAGER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.ZOOKEEPER_SERVER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.ZOOKEEPER_SERVER_ROLE.toString());
      }
      return unsupportedRoles;
   }

   @Override
   public void validateRolesForShrink(NodeGroupInfo groupInfo)
         throws SoftwareManagementPluginException {
      ValidateRolesUtil.validateRolesForShrink(AmUtils.getConfDir(), groupInfo);
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      boolean success = false;
      AmClusterDef clusterDef = null;
      try {
         clusterDef = new AmClusterDef(blueprint, privateKey, getVersion());

         String clusterName = clusterDef.getName();

         logger.info("Configuring cluster " + clusterName);

         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterName);

         ApiConfigGroupList apiConfigGroupList = apiManager.getConfigGroupsList(clusterName);

         updateConfigGroups(apiConfigGroupList, clusterDef, reports);
         success = true;

         clusterDef.getCurrentReport().setAction("Reconfiguring cluster succeeded");
         clusterDef.getCurrentReport().setProgress(100);
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Reconfiguring Cluster failed");
         clusterDef.getCurrentReport().setSuccess(false);
         throw e;
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reports.addClusterReport(clusterDef.getCurrentReport().clone());
      }
      return success;
   }

   private void updateConfigGroups(ApiConfigGroupList apiConfigGroupList, AmClusterDef clusterDef, ClusterReportQueue reports) {
      try {

         String action = "Reconfiguring cluster";
         clusterDef.getCurrentReport().setAction(action);
         reports.addClusterReport(clusterDef.getCurrentReport().clone());

         List<ApiHostGroup> apiHostGroupsFromSpec = clusterDef.toApiClusterBlueprint().getApiHostGroups();
         logger.info("apiHostGroupsFromSpec: "+ ApiUtils.objectToJson(apiHostGroupsFromSpec));

         logger.info("apiConfigGroupList: "+ ApiUtils.objectToJson(apiConfigGroupList));
         logger.info("ApiConfigGroups from ambari server: "+ ApiUtils.objectToJson(apiConfigGroupList.getConfigGroups()));

         for (ApiConfigGroup group : apiConfigGroupList.getConfigGroups()) {

            logger.info("ApiConfigGroup from ambari server: "+ ApiUtils.objectToJson(group));

            ApiConfigGroupInfo apiConfigGroupInfo = group.getApiConfigGroupInfo();
            if (apiConfigGroupInfo != null && apiConfigGroupInfo.getDesiredConfigs() != null) {

               for(ApiHostGroup apiHostGroupFromSpec : apiHostGroupsFromSpec) {
                  if (apiHostGroupFromSpec.getName().equals(apiConfigGroupInfo.getGroupName())) {
                     updateConfigGroup(apiConfigGroupInfo, apiHostGroupFromSpec);
                  }
               }

            }

         }
      } catch (Exception e) {
         String errMsg = "Failed to configure services for cluster " + clusterDef.getName() + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
         logger.error(errMsg);
         throw SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(e);
      }
   }

   private void updateConfigGroup(ApiConfigGroupInfo apiConfigGroupInfo, ApiHostGroup apiHostGroupFromClusterSpec) {
      updateConfigGroup(apiConfigGroupInfo, apiHostGroupFromClusterSpec, Collections.<String> emptyList());
   }

   private void updateConfigGroup(ApiConfigGroupInfo apiConfigGroupInfo, List<String> newHosts) {
      updateConfigGroup(apiConfigGroupInfo, null, newHosts);
   }

   private void updateConfigGroup(ApiConfigGroupInfo apiConfigGroupInfo, ApiHostGroup apiHostGroupFromClusterSpec, List<String> newHosts) {
      try {

         String clusterName = apiConfigGroupInfo.getClusterName();
         ApiConfigGroup newApiConfigGroup = new ApiConfigGroup();

         ApiConfigGroupInfo newApiConfigGroupInfo = new ApiConfigGroupInfo();

         newApiConfigGroupInfo.setId(apiConfigGroupInfo.getId());
         newApiConfigGroupInfo.setClusterName(apiConfigGroupInfo.getClusterName());
         newApiConfigGroupInfo.setGroupName(apiConfigGroupInfo.getGroupName());
         newApiConfigGroupInfo.setTag(apiConfigGroupInfo.getTag());
         newApiConfigGroupInfo.setDescription(apiConfigGroupInfo.getDescription());

         List<ApiHostInfo> hosts = new ArrayList<ApiHostInfo>();
         for (ApiHostInfo apiHostInfo : apiConfigGroupInfo.getHosts()) {
            ApiHostInfo newApiHostInfo = new ApiHostInfo();
            newApiHostInfo.setHostName(apiHostInfo.getHostName());
            hosts.add(newApiHostInfo);
         }
         if (CollectionUtils.isNotEmpty(newHosts)) {
            for (String newHost : newHosts) {
               ApiHostInfo newApiHostInfo = new ApiHostInfo();
               newApiHostInfo.setHostName(newHost);
               hosts.add(newApiHostInfo);
            }
         }
         newApiConfigGroupInfo.setHosts(hosts);

         List<ApiConfigGroupConfiguration> desiredConfigs = new ArrayList<ApiConfigGroupConfiguration>();

         logger.info("ApiConfigGroupConfiguration: " + ApiUtils.objectToJson(apiConfigGroupInfo.getDesiredConfigs()));
         logger.info("apiHostGroupFromClusterSpec: " + ApiUtils.objectToJson(apiHostGroupFromClusterSpec));

         String tag = "version" + Calendar.getInstance().getTimeInMillis();
         for(ApiConfigGroupConfiguration apiConfigGroupConfiguration : apiConfigGroupInfo.getDesiredConfigs()) {

            ApiConfigGroupConfiguration desiredConfig = new ApiConfigGroupConfiguration();

            desiredConfig.setType(apiConfigGroupConfiguration.getType());

            desiredConfig.setTag(tag);

            Map<String, String> properties = new HashMap<String, String>();

            ApiClusterConfigurations apiClusterConfigurations = apiManager.getClusterConfigurationsWithTypeAndTag(clusterName, apiConfigGroupConfiguration.getType(), apiConfigGroupConfiguration.getTag());

            logger.info("ApiClusterConfigurations: "+ ApiUtils.objectToJson(apiClusterConfigurations));

            for(ApiClusterConfigurationInfo apiClusterConfigurationInfo : apiClusterConfigurations.getConfigurations()) {

               Map<String, String> propertiesFromClusterSpec = new HashMap<String, String>();
               if (apiHostGroupFromClusterSpec != null) {
                  for (Map<String, Object> configurationFromClusterSpec : apiHostGroupFromClusterSpec.getConfigurations()) {
                     propertiesFromClusterSpec = (Map<String, String>) configurationFromClusterSpec.get(apiClusterConfigurationInfo.getType());
                     if (propertiesFromClusterSpec != null) {
                        break;
                     }
                  }
               }

               logger.info("propertiesFromClusterSpec: "+ ApiUtils.objectToJson(propertiesFromClusterSpec));

               Map<String, String> propertiesFromAmbariServer = apiClusterConfigurationInfo.getProperties();
               for(String propertyKey : propertiesFromAmbariServer.keySet()) {

                  // TODO sync up configurations from ambari server to BDE server

                  // TODO sync up configurations from BDE server to ambari server

                  String valueOfPropertyFromClusterSpec = null;

                  if (propertiesFromClusterSpec != null) {
                     valueOfPropertyFromClusterSpec = propertiesFromClusterSpec.get(propertyKey);
                  }

                  String valueOfPropertyFromAmbariServer = propertiesFromAmbariServer.get(propertyKey);

                  logger.info("Value of property from cluster spec: " + valueOfPropertyFromClusterSpec);
                  logger.info("Value of property from ambari server: " + valueOfPropertyFromAmbariServer);

                  // Just update properties which contains mount piont start with /mnt/datax for cluster upgrading
                  if (valueOfPropertyFromClusterSpec != null && valueOfPropertyFromClusterSpec.contains("/mnt/data0")) {
                     properties.put(propertyKey, valueOfPropertyFromClusterSpec);
                  } else {
                     properties.put(propertyKey, valueOfPropertyFromAmbariServer);
                  }

               }

            }

            desiredConfig.setProperties(properties);
            desiredConfigs.add(desiredConfig);

         }

         newApiConfigGroupInfo.setDesiredConfigs(desiredConfigs);

         newApiConfigGroup.setApiConfigGroupInfo(newApiConfigGroupInfo);

         logger.info("The new config group: " + ApiUtils
               .objectToJson(newApiConfigGroup));

         apiManager.updateConfigGroup(clusterName, apiConfigGroupInfo.getId(),
               newApiConfigGroup);

      } catch (Exception e) {
         String errMsg = "Failed to configure services for config group " + apiConfigGroupInfo.getId() + ((e.getMessage() == null) ? "" : (", " + e.getMessage()));
         logger.error(errMsg);
         throw SoftwareManagementPluginException.CONFIGURE_SERVICE_FAILED(e);
      }
   }

   @Override
   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reports, boolean forceScaleOut)
               throws SoftwareManagementPluginException {
      boolean success = false;
      AmClusterDef clusterDef = null;
      try {
         logger.info("Blueprint:");
         logger.info(ApiUtils.objectToJson(blueprint));
         logger.info("Start cluster " + blueprint.getName() + " scale out.");
         setHaseRegionConfig(blueprint);
         logger.info("After config hbase for Blueprint:");

         logger.info(ApiUtils.objectToJson(blueprint));
         clusterDef = new AmClusterDef(blueprint, privateKey, getVersion());

         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterDef.getName(), addedNodeNames, forceScaleOut);

         bootstrap(clusterDef, addedNodeNames, reports, forceScaleOut);

         provisionComponents(clusterDef, addedNodeNames, reports);

         setClusterHostsRackInfo(clusterDef, addedNodeNames);

         restartRequiredServices(clusterDef, reports);

         success = true;

         clusterDef.getCurrentReport().setNodesAction("", addedNodeNames);
         clusterDef.getCurrentReport().setNodesStatus(ServiceStatus.STARTED, addedNodeNames);
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_SUCCESS.getProgress());
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (Exception e) {
         clusterDef.getCurrentReport().clearAllNodesErrorMsg();
         clusterDef.getCurrentReport().setAction("");
         clusterDef.getCurrentReport().setNodesError(
               "Failed to bootstrap nodes for " + e.getMessage(),
               addedNodeNames);
         clusterDef.getCurrentReport().setSuccess(false);
         String errorMessage = errorMessage("Failed to scale out cluster " + blueprint.getName(), e);
         logger.error(errorMessage, e);

         throw SoftwareManagementPluginException.SCALE_OUT_CLUSTER_FAILED(e, Constants.AMBARI_PLUGIN_NAME, blueprint.getName());
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reportStatus(clusterDef.getCurrentReport(), reports);
      }
      return success;
   }

   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reports)
               throws SoftwareManagementPluginException {
      return scaleOutCluster(blueprint, addedNodeNames, reports, false);
   }

   private boolean provisionComponents(AmClusterDef clusterDef, List<String> addedNodeNames,
         ClusterReportQueue reports) throws Exception {
      logger.info("Installing roles " + addedNodeNames);
      ApiStackServiceList stackServiceList =
            apiManager.getStackWithCompAndConfigs(clusterDef.getAmStack()
                  .getName(), clusterDef.getAmStack().getVersion());
      Map<String, String> configTypeToService = stackServiceList.configTypeToService();
      Map<String, ApiComponentInfo> componentToInfo = stackServiceList.componentToInfo();
      ApiHostComponentsRequest apiHostComponentsRequest = null;
      Set<String> serviceNames = getExistingClusterServices(clusterDef);
      List<String> targetHostNames = new ArrayList<>();
      List<AmNodeDef> targetNodeDefs = new ArrayList<>();
      for (AmNodeDef nodeDef : clusterDef.getNodes()) {
         if (addedNodeNames.contains(nodeDef.getName())) {
            if (apiHostComponentsRequest == null) {
               apiHostComponentsRequest =
                     createHostComponents(componentToInfo, serviceNames,
                           nodeDef);
            }
            targetHostNames.add(nodeDef.getFqdn());
            targetNodeDefs.add(nodeDef);
         }
      }
      removeHosts(clusterDef, targetHostNames, reports);
      apiManager.addHostsToCluster(clusterDef.getName(), targetHostNames);
      if (apiHostComponentsRequest.getHostComponents().isEmpty()) {
         logger.info("No roles need to install on hosts.");
         return true;
      }
      // add configurations
      configNodes(clusterDef, configTypeToService, targetNodeDefs);
      installComponents(clusterDef, reports, apiHostComponentsRequest, targetHostNames);
      return startAllComponents(clusterDef, componentToInfo, apiHostComponentsRequest, targetHostNames, reports);
   }

   private boolean restartRequiredServices(AmClusterDef clusterDef, ClusterReportQueue reports) throws Exception {
      boolean isRestarted = true;

      List<ApiRequest> apiRequestsSummary = apiManager.restartRequiredServices(clusterDef.getName());

      ClusterOperationPoller poller = new ClusterOperationPoller(apiManager, apiRequestsSummary,
            clusterDef.getName(), clusterDef.getCurrentReport(), reports,
            ProgressSplit.PROVISION_SUCCESS.getProgress());
      poller.waitForComplete();

      for (ApiRequest apiRequestSummary : apiRequestsSummary) {
         ApiRequest apiRequest = apiManager.getRequest(clusterDef.getName(), apiRequestSummary.getApiRequestInfo().getRequestId());
         ClusterRequestStatus clusterRequestStatus = ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo().getRequestStatus());
         if (clusterRequestStatus.isFailedState()) {
            isRestarted = false;
         }
      }

      if (!isRestarted) {
         throw SoftwareManagementPluginException.RESTART_CLUSTER_SERVICE_FAILED(null, Constants.AMBARI_PLUGIN_NAME, clusterDef.getName());
      }

      return isRestarted;
   }

   private void removeHosts(AmClusterDef clusterDef,
         List<String> targetHostNames, ClusterReportQueue reports)
               throws Exception {
      List<String> existingHosts =
            apiManager.getExistingHosts(clusterDef.getName(), targetHostNames);
      if (existingHosts.isEmpty()) {
         logger.debug("No host exists in cluster.");
         return;
      }
      deleteAssociatedConfGroups(clusterDef, existingHosts);
      stopAllComponents(clusterDef, existingHosts, reports);
      removeHostsFromCluster(clusterDef, existingHosts);
   }

   private void deleteAssociatedConfGroups(AmClusterDef clusterDef,
         List<String> existingHosts) {
      for (String hostName : existingHosts) {
         List<String> groups =
               apiManager.getAssociatedConfigGroups(clusterDef.getName(),
                     hostName);
         for (String groupId : groups) {
            apiManager.deleteConfigGroup(clusterDef.getName(), groupId);
         }
      }
   }

   private void removeHostsFromCluster(AmClusterDef clusterDef, List<String> existingHosts) {
      for (String hostName : existingHosts) {
         apiManager.deleteAllComponents(clusterDef.getName(), hostName);
         apiManager.deleteHost(clusterDef.getName(), hostName);
      }
   }

   private void stopAllComponents(AmClusterDef clusterDef,
         List<String> existingHosts, ClusterReportQueue reports)
               throws Exception {
      ApiRequest apiRequestSummary =
            apiManager.stopAllComponentsInHosts(clusterDef.getName(),
                  existingHosts);
      if (apiRequestSummary == null || apiRequestSummary.getApiRequestInfo() == null) {
         logger.debug("No components need to be stopped.");
         return;
      }

      ClusterOperationPoller poller =
            new ClusterOperationPoller(apiManager, apiRequestSummary,
                  clusterDef.getName(), clusterDef.getCurrentReport(), reports,
                  ProgressSplit.PROVISION_SUCCESS.getProgress());
      poller.waitForComplete();

      boolean success = false;
      ApiRequest apiRequest =
            apiManager.getRequest(clusterDef.getName(), apiRequestSummary
                  .getApiRequestInfo().getRequestId());
      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());
      if (!clusterRequestStatus.isFailedState()) {
         success = true;
      }
      if (!success) {
         throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(Constants.AMBARI_PLUGIN_NAME, clusterDef.getName(), null);
      }
   }

   private boolean startAllComponents(AmClusterDef clusterDef,
         Map<String, ApiComponentInfo> componentToInfo,
         ApiHostComponentsRequest apiHostComponentsRequest, List<String> targetHostNames,
         ClusterReportQueue reports)
               throws Exception {
      List<String> componentNames = new ArrayList<>();
      for (ApiHostComponent hostComponent : apiHostComponentsRequest.getHostComponents()) {
         String componentName = hostComponent.getHostComponent().getComponentName();
         ApiComponentInfo compInfo = componentToInfo.get(componentName);
         if (compInfo.isClient()) {
            continue;
         }
         componentNames.add(componentName);
      }
      if (componentNames.isEmpty()) {
         logger.debug("Client only roles installed.");
         return true;
      }
      logger.debug("Starting roles: " + componentNames);
      ApiRequest apiRequestSummary =
            apiManager.startComponents(clusterDef.getName(), targetHostNames,
                  componentNames);
      ClusterOperationPoller poller =
            new ClusterOperationPoller(apiManager, apiRequestSummary,
                  clusterDef.getName(), clusterDef.getCurrentReport(), reports,
                  ProgressSplit.PROVISION_SUCCESS.getProgress());
      poller.waitForComplete();

      boolean success = false;
      ApiRequest apiRequest =
            apiManager.getRequest(clusterDef.getName(), apiRequestSummary
                  .getApiRequestInfo().getRequestId());
      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());
      if (!clusterRequestStatus.isFailedState()) {
         success = true;
      }
      if (!success) {
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(null, Constants.AMBARI_PLUGIN_NAME, clusterDef.getName());
      }
      return success;
   }

   private void configNodes(AmClusterDef clusterDef,
         Map<String, String> configTypeToService, List<AmNodeDef> targetNodeDefs)
               throws SoftwareManagementPluginException {

      Map<Integer, List<String>> newHostsMap = getNewHostsMap(targetNodeDefs);

      List<AmNodeGroupDef> nodeGroups = clusterDef.getNodeGroupsByNodes(targetNodeDefs);
      List<AmHostGroupInfo> amHostGroupsInfo = clusterDef.getAmHostGroupsInfoByNodeGroups(nodeGroups, configTypeToService);

      String clusterName = clusterDef.getName();
      ApiConfigGroupList apiConfigGroupList = apiManager.getConfigGroupsList(clusterName);
      for (ApiConfigGroup group : apiConfigGroupList.getConfigGroups()) {
         logger.info("ApiConfigGroup from ambari server: " + ApiUtils.objectToJson(group));

         ApiConfigGroupInfo apiConfigGroupInfo = group.getApiConfigGroupInfo();
         if (apiConfigGroupInfo == null) {
            logger.warn("The apiConfigGroupInfo of config group is null.");
            continue;
         }

         if (CollectionUtils.isEmpty(apiConfigGroupInfo.getDesiredConfigs())) {
            logger.warn("The desired configs of config group are null.");
         }

         for(AmHostGroupInfo amHostGroupInfo : amHostGroupsInfo) {
            logger.info("amHostGroupInfo from spec: " + ApiUtils.objectToJson(amHostGroupInfo));

            String configGroupName = apiConfigGroupInfo.getGroupName();
            String configGroupTag = apiConfigGroupInfo.getTag();

            List<String> newHosts = newHostsMap.get(amHostGroupInfo.getVolumesCount());
            logger.info("New hosts " + ApiUtils.objectToJson(newHosts) + " which have " + amHostGroupInfo.getVolumesCount() + " volumes.");

            // Add new hosts to the config group of Ambari server if have the same volumes count in a group
            if (amHostGroupInfo.getConfigGroupName().equals(configGroupName) && (newHosts != null)) {
               logger.info("Add hosts " + ApiUtils.objectToJson(newHosts) +" to config group " + configGroupName + " tag " + configGroupTag + ".");
               updateConfigGroup(apiConfigGroupInfo, newHosts);
               amHostGroupInfo.removeOldTag(configGroupTag);
            } else if (configGroupName.equals(amHostGroupInfo.getNodeGroupName())) {
               // Update config group name to GROUP_NAME_vol* of Ambari server if it is the same config group
               logger.info("Just update config group name " + configGroupName + " to " + amHostGroupInfo.getConfigGroupName());
               apiConfigGroupInfo.setGroupName(amHostGroupInfo.getConfigGroupName());
               updateConfigGroup(apiConfigGroupInfo, newHosts);
               amHostGroupInfo.removeOldTag(configGroupTag);
            } else if (configGroupName.equals(clusterName + ":" + amHostGroupInfo.getNodeGroupName())) { // For Ambari version >= 2.0, the config group name is like <CLUSTER_NAME>:<GROUP_NAME>
               // Update config group name to GROUP_NAME_vol* of Ambari server if it is the same config group
               logger.info("Just update config group name " + clusterName + ":" + amHostGroupInfo.getNodeGroupName() + " to " + amHostGroupInfo.getConfigGroupName());
               apiConfigGroupInfo.setGroupName(amHostGroupInfo.getConfigGroupName());
               updateConfigGroup(apiConfigGroupInfo, newHosts);
               amHostGroupInfo.removeOldTag(configGroupTag);
            } else {
               for (ApiHostInfo apiHostInfo : apiConfigGroupInfo.getHosts()) {
                  String host = apiHostInfo.getHostName();
                  boolean isRemoved = amHostGroupInfo.removeOldHostFromTag(host, configGroupTag);
                  logger.info("Removing host " + host +" from amHostGroupInfo " + amHostGroupInfo.getConfigGroupName() + " is " + isRemoved + ".");
               }
            }

         }
      }

      Set<AmHostGroupInfo> newAmHostGroupsInfo = new HashSet<AmHostGroupInfo> ();
      for(AmHostGroupInfo amHostGroupInfo : amHostGroupsInfo) {
         logger.info("After removing hosts the amHostGroupInfo is " + ApiUtils.objectToJson(amHostGroupInfo));
         Map<String, Set<String>> tag2Hosts = amHostGroupInfo.getTag2Hosts();
         if (tag2Hosts == null || tag2Hosts.isEmpty()) {
            continue;
         }
         for(String tag : tag2Hosts.keySet()) {
            if (CollectionUtils.isNotEmpty(tag2Hosts.get(tag))) {
               boolean isAdded = newAmHostGroupsInfo.add(amHostGroupInfo);
               if (isAdded) {
                  logger.info("Hosts " + ApiUtils.objectToJson(amHostGroupInfo.getHosts()) + " are in amHostGroupInfo " + amHostGroupInfo.getConfigGroupName() + ".");
               }
               break;
            }
         }
      }

      createConfigGroups(clusterDef, newAmHostGroupsInfo, configTypeToService);
   }

   private Map<Integer, List<String>> getNewHostsMap(List<AmNodeDef> targetNodeDefs) {
      Map<Integer, List<String>> newHostsMap = new HashMap<Integer, List<String>>();
      for (AmNodeDef targetNodeDef : targetNodeDefs) {
         int volumesCount = targetNodeDef.getVolumesCount();
         String host = targetNodeDef.getFqdn();
         if (newHostsMap.isEmpty()) {
            newHostsMap.put(volumesCount, new ArrayList<String>(Arrays.asList(host)));
         } else {
            List<String> newHosts = newHostsMap.get(volumesCount);
            if (newHosts != null) {
               newHosts.add(host);
               newHostsMap.put(volumesCount, newHosts);
            } else {
               newHostsMap.put(volumesCount, new ArrayList<String>(Arrays.asList(host)));
            }
         }
      }

      return newHostsMap;
   }

   private void createConfigGroups(AmClusterDef clusterDef, Set<AmHostGroupInfo> newAmHostGroupsInfo, Map<String, String> configTypeToService) {
      logger.info("The new host groups are " + ApiUtils.objectToJson(newAmHostGroupsInfo) + ".");
      List<ApiConfigGroup> configGroups = new ArrayList<>();
      Map<String, ApiConfigGroup> serviceToGroup = new HashMap<>();
      for (AmHostGroupInfo amHostGroupInfo : newAmHostGroupsInfo) {

         // for each nodeGroup with the same number of disks, one set of config group will be created
         // for each service, one config group will be created, which contains all property types belong to this service
         serviceToGroup.clear();
         List<Map<String, Object>> configs = amHostGroupInfo.getConfigurations();
         int i = 1;
         for (Map<String, Object> map : configs) {
            for (String type : map.keySet()) {
               String serviceName = configTypeToService.get(type + ".xml");

               if (!amHostGroupInfo.getTag2Hosts().keySet().contains(serviceName)) {
                  continue;
               }

               ApiConfigGroup confGroup = serviceToGroup.get(serviceName);
               if (confGroup == null) {
                  confGroup = createConfigGroup(clusterDef, amHostGroupInfo, serviceName);
                  serviceToGroup.put(serviceName, confGroup);
               }
               ApiConfigGroupConfiguration sameType = null;
               for (ApiConfigGroupConfiguration config : confGroup
                     .getApiConfigGroupInfo().getDesiredConfigs()) {
                  if (config.getType().equals(type)) {
                     sameType = config;
                     break;
                  }
               }
               if (sameType == null) {
                  sameType = createApiConfigGroupConf(i, type, serviceName, confGroup);
               }
               Map<String, String> property = (Map<String, String>)map.get(type);
               sameType.getProperties().putAll(property);
            }
         }
         configGroups.addAll(serviceToGroup.values());
      }
      if (configGroups.isEmpty()) {
         return;
      }
      logger.info("Start to create config groups: " + configGroups);
      apiManager.createConfigGroups(clusterDef.getName(), configGroups);
   }

   private ApiConfigGroupConfiguration createApiConfigGroupConf(int i,
         String type, String serviceName, ApiConfigGroup confGroup) {
      ApiConfigGroupConfiguration sameType;
      sameType = new ApiConfigGroupConfiguration();
      sameType.setType(type);
      sameType.setTag(serviceName + i);
      sameType.setProperties(new HashMap<String, String>());
      i ++;
      confGroup.getApiConfigGroupInfo().getDesiredConfigs()
      .add(sameType);
      return sameType;
   }

   private ApiConfigGroup createConfigGroup(AmClusterDef clusterDef, AmHostGroupInfo amHostGroupInfo, String serviceName) {
      ApiConfigGroup confGroup;
      confGroup = new ApiConfigGroup();
      ApiConfigGroupInfo info = new ApiConfigGroupInfo();
      confGroup.setApiConfigGroupInfo(info);
      info.setClusterName(clusterDef.getName());
      info.setDescription(serviceName + " configuration");
      info.setGroupName(amHostGroupInfo.getConfigGroupName());

      List<ApiHostInfo> hosts = new ArrayList<>();
      for (String host : amHostGroupInfo.getTag2Hosts().get(serviceName)) {
         ApiHostInfo hostInfo = new ApiHostInfo();
         hostInfo.setHostName(host);
         hosts.add(hostInfo);
      }
      info.setHosts(hosts);

      info.setTag(serviceName);
      List<ApiConfigGroupConfiguration> desiredConfigs = new ArrayList<>();
      info.setDesiredConfigs(desiredConfigs);
      return confGroup;
   }

   private boolean installComponents(AmClusterDef clusterDef,
         ClusterReportQueue reports, ApiHostComponentsRequest apiHostComponentsRequest,
         List<String> targetHostNames) throws Exception {
      // add components to target hosts concurrently
      apiManager.addComponents(clusterDef.getName(), targetHostNames, apiHostComponentsRequest);
      ApiRequest request = apiManager.installComponents(clusterDef.getName());
      ClusterOperationPoller poller =
            new ClusterOperationPoller(apiManager, request,
                  clusterDef.getName(), clusterDef.getCurrentReport(), reports,
                  ProgressSplit.PROVISION_CLUSTER.getProgress());
      poller.waitForComplete();

      boolean success = false;

      ApiRequest apiRequest = null;

      try {
         apiRequest = apiManager.getRequest(clusterDef.getName(), request
               .getApiRequestInfo().getRequestId());
      } catch (NullPointerException npe) {
         // quick fix for the moment. should be handled properly later. Bug 1493094
         logger.error("can't get ApiRequest Object from Ambari.", npe);
         throw SoftwareManagementPluginException.INSTALL_COMPONENTS_FAIL(Constants.AMBARI_PLUGIN_NAME, clusterDef.getName());
      }

      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());
      if (!clusterRequestStatus.isFailedState()) {
         success = true;
      }
      if (!success) {
         throw SoftwareManagementPluginException.INSTALL_COMPONENTS_FAIL(Constants.AMBARI_PLUGIN_NAME, clusterDef.getName());
      }
      return success;
   }

   private ApiHostComponentsRequest createHostComponents(
         Map<String, ApiComponentInfo> componentToInfo,
         Set<String> serviceNames, AmNodeDef nodeDef) {
      ApiHostComponentsRequest apiHostComponentsRequest = new ApiHostComponentsRequest();
      List<ApiHostComponent> hostComponents = new ArrayList<>();
      apiHostComponentsRequest.setHostComponents(hostComponents);
      for (String componentName : nodeDef.getComponents()) {
         ApiComponentInfo definedCompInfo = componentToInfo.get(componentName);
         if (definedCompInfo == null) {
            logger.error("Component " + componentName + " is not supported. This should not happen.");
            continue;
         }
         String serviceName = definedCompInfo.getServiceName();
         if (!serviceNames.contains(serviceName)) {
            logger.info("Service " + serviceName + " is removed from Ambari, igonre component " + componentName);
            continue;
         }
         ApiHostComponent component = new ApiHostComponent();
         hostComponents.add(component);
         ApiComponentInfo componentInfo = new ApiComponentInfo();
         componentInfo.setComponentName(componentName);
         component.setHostComponent(componentInfo);
      }
      return apiHostComponentsRequest;
   }

   private Set<String> getExistingClusterServices(AmClusterDef clusterDef) {
      List<ApiService> services = apiManager.getClusterServices(clusterDef.getName());
      Set<String> serviceNames = new HashSet<>();
      for (ApiService service : services) {
         serviceNames.add(service.getServiceInfo().getServiceName());
      }
      return serviceNames;
   }

   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      return startCluster(clusterBlueprint, reportQueue, false);
   }

   @Override
   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports, boolean forceStart)
         throws SoftwareManagementPluginException {
      AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, privateKey, getVersion());

      String clusterName = clusterDef.getName();
      ClusterReport clusterReport = clusterDef.getCurrentReport();
      if (!isProvisioned(clusterName)) {
         throw AmException.CLUSTER_NOT_PROVISIONED(clusterName);
      }
      if (!isClusterProvisionedByBDE(clusterDef)) {
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED_NOT_PROV_BY_BDE(clusterName);
      }

      clusterReport.setAction("Ambari is starting services");
      clusterReport.setProgress(ProgressSplit.OPERATION_BEGIN.getProgress());
      reportStatus(clusterReport, reports);

      boolean success = false;
      //In ambari1.6.0, when start services, some tasks will fail with error msg "Host Role in invalid state".
      // The failed task are random(I had saw NodeManager, ResourceManager, NAGOIS failed), and the
      // root cause is not clear by now. Each time, when I retry, it succeed. So just add retry logic to make a
      // a temp fix for it.
      //TODO(qjin): find out the root cause of failure in startting services
      Exception resultException = null;
      try {
         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterName, Collections.EMPTY_LIST, forceStart);
         for (int i = 0; i < getRequestMaxRetryTimes() && !success; i++) {
            ApiRequest apiRequestSummary;
            try {
               apiRequestSummary = apiManager.startAllServicesInCluster(clusterName);
               //when reach here, command is succeed. If ApiRequestInfo is null, it means the command has been
               //finished successfully, otherwise we need to wait for it using doSoftwareOperation
               if (apiRequestSummary == null || apiRequestSummary.getApiRequestInfo() == null) {
                  success = true;
                  return true;
               }
               success = doSoftwareOperation(clusterBlueprint.getName(), apiRequestSummary, clusterReport, reports);
            } catch (Exception e) {
               resultException = e;
               logger.warn("Failed to start cluster services, retrying after 5 seconds...", e);
               try {
                  Thread.sleep(5000);
               } catch (InterruptedException interrupt) {
                  logger.info("interrupted when sleeping, trying to start cluster services immediately");
               }
            }
         }
      } finally {
         if (!success) {
            logger.error("Failed to start all services: ", resultException);
            throw SoftwareManagementPluginException.START_CLUSTER_FAILED(resultException, Constants.AMBARI_PLUGIN_NAME, clusterName);
         }

         clusterReport.setClusterAndNodesServiceStatus(ServiceStatus.STARTED);
         clusterReport.setClusterAndNodesAction("");
         clusterReport.clearAllNodesErrorMsg();
         reportStatus(clusterReport.clone(), reports);
         return true;
      }
   }

   @Override
   public boolean deleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      return true;
   }

   @Override
   public boolean onStopCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      String clusterName = clusterBlueprint.getName();
      AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, privateKey, getVersion());

      ClusterReport clusterReport = clusterDef.getCurrentReport();
      try {
         if (!isProvisioned(clusterName)) {
            return true;
         }
         if (!isClusterProvisionedByBDE(clusterDef)) {
            throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(Constants.AMBARI_PLUGIN_NAME,
                  clusterName, "Cannot stop a cluster that is not provisioned by Big Data Extension");
         }
         clusterReport.setAction("Ambari is stopping services");
         clusterReport.setProgress(ProgressSplit.OPERATION_BEGIN.getProgress());
         reportStatus(clusterReport, reports);
         ApiRequest apiRequestSummary = apiManager.stopAllServicesInCluster(clusterName);
         if (apiRequestSummary == null || apiRequestSummary.getApiRequestInfo() == null) {
            logger.info("Services is already stopped in cluster " + clusterName);
            return true;
         }
         doSoftwareOperation(clusterName, apiRequestSummary, clusterReport, reports);
         clusterReport.setClusterAndNodesAction("");
         clusterReport.clearAllNodesErrorMsg();
         clusterReport.setClusterAndNodesServiceStatus(ServiceStatus.STOPPED);
         reportStatus(clusterReport.clone(), reports);
         return true;
      } catch (Exception e) {
         logger.error("Failed to stop all services: ", e);
         throw SoftwareManagementPluginException.STOP_CLUSTER_EXCEPTION(e, Constants.AMBARI_PLUGIN_NAME, clusterName);
      }
   }

   protected boolean doSoftwareOperation(String clusterName, ApiRequest apiRequestSummary,
         ClusterReport clusterReport, ClusterReportQueue reports) throws Exception{

      if (apiRequestSummary == null) {
         return true;
      }
      ClusterOperationPoller poller =
            new ClusterOperationPoller(apiManager, apiRequestSummary,
                  clusterName, clusterReport, reports,
                  ProgressSplit.OPERATION_FINISHED.getProgress());
      poller.waitForComplete();

      boolean success = false;
      ApiRequest apiRequest =
            apiManager.getRequestWithTasks(clusterName, apiRequestSummary
                  .getApiRequestInfo().getRequestId());
      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());
      if (!clusterRequestStatus.isFailedState()) {
         success = true;
      } else {
         logger.error("Failed to do request: " + ApiUtils.objectToJson(apiRequest.getApiRequestInfo()));
         List<ApiTask> apiTasks = apiRequest.getApiTasks();
         Map<String, NodeReport> nodeReports = clusterReport.getNodeReports();
         HashMap<String, List<String>> errMsg = new HashMap<>();
         for (ApiTask apiTask : apiTasks) {
            ApiTaskInfo taskInfo = apiTask.getApiTaskInfo();
            if (TaskStatus.valueOf(taskInfo.getStatus()).isFailedState()) {
               if (!errMsg.containsKey(taskInfo.getHostName())) {
                  List<String> errs = new ArrayList<>();
                  errMsg.put(taskInfo.getHostName(), errs);
               }
               String taskErrMsg = taskInfo.getCommandDetail() + " " + taskInfo.getStatus();
               errMsg.get(taskInfo.getHostName()).add(taskErrMsg);
               logger.error("command: " + taskInfo.getCommandDetail() +
                     "role: " + taskInfo.getRole() +
                     "stderr: " + taskInfo.getStderr() +
                     "status: " + taskInfo.getStatus());
            }
         }
         for (NodeReport nodeReport: nodeReports.values()) {
            if (errMsg.containsKey(nodeReport.getHostname())) {
               nodeReport.setErrMsg(errMsg.get(nodeReport.getHostname()).toString());
            }
         }
         String requestErrorMsg = "Failed to execute request: " +
               apiRequest.getApiRequestInfo().getRequestStatus() + ". Refer to each node for details.";
         clusterReport.setErrMsg(requestErrorMsg);
         reportStatus(clusterReport.clone(), reports);
         throw new RuntimeException(requestErrorMsg);
      }
      return success;
   }

   @Override
   public boolean onDeleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      try {
         String clusterName = clusterBlueprint.getName();
         if (!echo()) {
            logger.warn("Ambari server is unavailable when deleting cluster " + clusterName + ". Will delete VMs forcely.");
            logger.warn("You may need to delete cluster resource on ambari server manually.");
            return true;
         }
         if (!isProvisioned(clusterName)) {
            return true;
         }
         AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, privateKey, getVersion());

         if (!isClusterProvisionedByBDE(clusterDef)) {
            return true;
         }
         //Stop services if there is started services, when stop failed, we will try to forcely delete resource
         //although we may also fail because of resource dependency. In that case, we will
         //throw out the exception and fail
         try {
            if (!onStopCluster(clusterBlueprint, reports)) {
               logger.error("Ambari failed to stop services");
            }
         } catch (Exception e) {
            logger.error("Got exception when stop all services before delete cluster, try to delete cluster forcely", e);
         }
         List<String> serviceNames = apiManager.getClusterServicesNames(clusterName);
         if (serviceNames != null && !serviceNames.isEmpty()) {
            for (String serviceName : serviceNames) {
               apiManager.deleteService(clusterName, serviceName);
            }
         }
         if (apiManager.getHostsSummaryInfo(clusterName) != null) {
            List<ApiHost> hosts = apiManager.getHostsSummaryInfo(clusterName).getApiHosts();
            if (hosts != null && !hosts.isEmpty()) {
               for (ApiHost host : hosts) {
                  assert (host.getApiHostInfo() != null);
                  String hostName = host.getApiHostInfo().getHostName();
                  apiManager.deleteHost(clusterName, hostName);
               }
            }
         }
         apiManager.deleteCluster(clusterName);

         if (isBlueprintCreated(clusterDef) && isBlueprintCreatedByBDE(clusterDef)) {
            apiManager.deleteBlueprint(clusterName);
         }
         ApiPersist persist = new ApiPersist("");
         apiManager.updatePersist(persist);
         return true;
      } catch (Exception e) {
         logger.error("Ambari got an exception when deleting cluster", e);
         throw SoftwareManagementPluginException.DELETE_CLUSTER_FAILED(e, Constants.AMBARI_PLUGIN_NAME, clusterBlueprint.getName());
      }
   }

   @Override
   public boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames) throws SoftwareManagementPluginException {
      AmClusterDef clusterDef = null;
      try {
         logger.info("Delete nodes " + nodeNames + " from cluster " + blueprint.getName());
         clusterDef = new AmClusterDef(blueprint, privateKey, getVersion());

         List<String> targetHostNames = new ArrayList<>();
         for (AmNodeDef nodeDef : clusterDef.getNodes()) {
            if (nodeNames.contains(nodeDef.getName())) {
               targetHostNames.add(nodeDef.getFqdn());
            }
         }
         removeHosts(clusterDef, targetHostNames, null);
      } catch (Exception e) {
         clusterDef.getCurrentReport().setSuccess(false);
         String errorMessage = errorMessage("Failed to delete nodes " + nodeNames + " from cluster " + blueprint.getName(), e);
         logger.error(errorMessage, e);
         throw SoftwareManagementPluginException.DELETE_NODES_FAILED(e, Constants.AMBARI_PLUGIN_NAME, nodeNames.toArray());
      }
      return true;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      return false;
   }

   @Override
   public void decommissionNode(ClusterBlueprint blueprint, String nodeGroupName, String nodeName, ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      String clusterName = blueprint.getName();
      boolean succeed = false;
      AmClusterDef clusterDef = null;
      String errMsg = null;

      logger.info("Begin decommission host " + nodeName);
      try {
         clusterDef = new AmClusterDef(blueprint, privateKey, getVersion());

         //decommission components
         logger.info("decommission components");
         updateNodeAction(clusterDef, "Decommission components", nodeName, reportQueue);
         String hostFQDN = getNodeFQDN(clusterDef, nodeName);
         decommissionComponentsOnHost(clusterName, hostFQDN);
         updateNodeAction(clusterDef, "Decommission components succeed", nodeName, reportQueue);
         logger.info("Decommission components succeed");

         //stop components on host, remove components and host
         logger.info("Stopping components, removing components and host");
         List<String> hostNames = new ArrayList<>();
         hostNames.add(hostFQDN);
         updateNodeAction(clusterDef, "Removing components and host", nodeName, reportQueue);

         stopAllComponentsInHost(clusterDef, hostNames, reportQueue);
         apiManager.deleteAllComponents(clusterDef.getName(), hostFQDN);
         apiManager.deleteHost(clusterDef.getName(), hostFQDN);

         //TODO(qjin): For improvement, restart ZOOKEEPER_SERVER and NAGIOS_SERVER on each host if necessary
         succeed = true;
      } catch (Exception e) {
         errMsg = e.getMessage();
         logger.error("Got exception when decommission node " + nodeName, e);
         throw SoftwareManagementPluginException.DECOMISSION_FAILED(clusterName, nodeGroupName, nodeName, errMsg);
      } finally {
         if (succeed) {
            logger.info("Decommission node " + nodeName + " succeed");
            updateNodeAction(clusterDef, "Host decommissioned", nodeName, reportQueue);
         } else {
            if (clusterDef != null) {
               updateNodeAction(clusterDef, "Host decommission failed", nodeName, reportQueue);
               updateNodeErrorMsg(clusterDef, errMsg, nodeName, reportQueue);
               logger.error("Decommission node " + nodeName + " failed.");
            }
         }
      }
   }

   private void stopAllComponentsInHost(AmClusterDef clusterDef, List<String> hostNames, ClusterReportQueue reportQueue) throws Exception {
      List<String> existingHosts =
            apiManager.getExistingHosts(clusterDef.getName(), hostNames);
      if (existingHosts.isEmpty()) {
         logger.debug("No host exists in cluster.");
         return;
      }
      deleteAssociatedConfGroups(clusterDef, existingHosts);
      stopAllComponents(clusterDef, existingHosts, reportQueue);
   }

   private String getNodeFQDN(AmClusterDef clusterDef, String nodeName) {
      List<AmNodeDef> nodes = clusterDef.getNodes();
      AmNodeDef node = null;
      for (AmNodeDef nodeDef: nodes) {
         if (nodeDef.getName().equals(nodeName)) {
            node = nodeDef;
         }
      }
      return node.getFqdn();
   }

   private void updateNodeAction(AmClusterDef clusterDef, String action, String nodeName, ClusterReportQueue reportQueue) {
      List<String> nodeNames = new ArrayList<>();
      nodeNames.add(nodeName);
      clusterDef.getCurrentReport().setNodesAction(action, nodeNames);
      reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
   }

   private void updateNodeErrorMsg(AmClusterDef clusterDef, String errMsg, String nodeName, ClusterReportQueue reportQueue) {
      List<String> nodeNames = new ArrayList<>();
      nodeNames.add(nodeName);
      clusterDef.getCurrentReport().setNodesError(errMsg, nodeNames);
      reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
   }
   private void decommissionComponentsOnHost(String clusterName, String hostFQDN) {
      List<ApiHostComponent> apiHostComponents = apiManager.getHostComponents(clusterName, hostFQDN);
      for (ApiHostComponent hostComponent: apiHostComponents) {
         String componentName = hostComponent.getHostComponent().getComponentName();
         if (componentName.equals("NODEMANAGER")) {
            apiManager.decommissionComponent(clusterName, hostFQDN, "YARN", "RESOURCEMANAGER", "NODEMANAGER");
         }
         if (componentName.equals("DATANODE")) {
            apiManager.decommissionComponent(clusterName, hostFQDN, "HDFS", "NAMENODE", "DATANODE");
         }
      }
   }
   @Override
   public boolean recomissionNode(String clusterName, NodeInfo node, ClusterReportQueue reportQueue) throws SoftwareManagementPluginException {
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
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) {
      AmClusterDef clusterDef = new AmClusterDef(blueprint, privateKey, getVersion());

      try {
         ServiceStatus status =
               apiManager.getClusterStatus(blueprint.getName(),
                     blueprint.getHadoopStack());
         clusterDef.getCurrentReport().setStatus(status);

         Map<String, ServiceStatus> hostStates =
               apiManager.getHostStatus(blueprint.getName());
         Map<String, NodeReport> nodeReports =
               clusterDef.getCurrentReport().getNodeReports();
         for (AmNodeDef node : clusterDef.getNodes()) {
            String fqdn = node.getFqdn();
            nodeReports.get(node.getName()).setStatus(hostStates.get(fqdn));
         }
      } catch (NotFoundException e) {
         logger.info("Cluster " + blueprint.getName() + " does not exist in server.");
         return null;
      }
      return clusterDef.getCurrentReport().clone();
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint)
         throws ValidationException {
      AmClusterValidator amClusterValidator = new AmClusterValidator();
      amClusterValidator.setApiManager(apiManager);
      return validateBlueprint(amClusterValidator, blueprint);
   }

   protected boolean validateBlueprint(AmClusterValidator amClusterValidator, ClusterBlueprint blueprint)
         throws ValidationException {
      return amClusterValidator.validateBlueprint(blueprint);
   }

   @Override
   public boolean hasHbase(ClusterBlueprint blueprint) {
      boolean hasHbase = false;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (HadoopRole.hasHBaseRole(group.getRoles())) {
            hasHbase = true;
            break;
         }
      }
      return hasHbase;
   }

   @Override
   public boolean hasMgmtRole(List<String> roles) {
      return HadoopRole.hasMgmtRole(roles);
   }

   @Override
   public boolean isComputeOnlyRoles(List<String> roles) {
      return false;
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      EnumSet<HadoopRole> enumRoles = HadoopRole.getEnumRoles(group.getRoles());
      if ((enumRoles.size() == 1 || (enumRoles.size() == 2 && enumRoles
            .contains(HadoopRole.JOURNALNODE_ROLE)))
            && (enumRoles.contains(HadoopRole.ZOOKEEPER_SERVER_ROLE))) {
         return true;
      }
      return false;
   }

   @Override
   public boolean hasComputeMasterGroup(ClusterBlueprint blueprint) {
      return false;
   }

   private void reportStatus(final ClusterReport clusterReport,
         final ClusterReportQueue reportQueue) {
      if (reportQueue != null) {
         reportQueue.addClusterReport(clusterReport.clone());
      }
   }

   @Override
   public String getVersion() {
      try {
         return apiManager.getVersion();
      } catch (Exception e) {
         // we print the log here for user to check the cause.
         String errMsg = "Cannot connect to the Software Manager, check the connection information.";
         logger.error(errMsg, e);
         return UNKNOWN_VERSION;
      }
   }

   @Override
   public HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException {
      List<HadoopStack> hadoopStacks = getSupportedStacks();
      Collections.<HadoopStack> sort(hadoopStacks);
      return hadoopStacks.get(0);
   }

   private String errorMessage(String defaultMessage, Exception e) {
      String errorMessage = defaultMessage;
      if (e.getMessage() != null) {
         errorMessage = e.getMessage();
      }
      return errorMessage;
   }

   public ApiManager getApiManager() {
      return apiManager;
   }

   public void setApiManager(ApiManager apiManager) {
      this.apiManager = apiManager;
   }

   public int getRequestMaxRetryTimes() {
      return REQUEST_MAX_RETRY_TIMES;
   }

   @Override
   public boolean hasMountPointStartwithDatax(String clusterName) {
      boolean hasMountPointStartwithDatax = false;

      ApiBlueprint apiBlueprint = apiManager.getBlueprint(clusterName);
      String apiBlueprintJson = ApiUtils.objectToJson(apiBlueprint);
      if (apiBlueprintJson.contains("/mnt/data0")) {
         hasMountPointStartwithDatax = true;
      }

      return hasMountPointStartwithDatax;
   }

   @Override
   public void restartClusterRequiredServices(ClusterBlueprint blueprint, ClusterReportQueue reports) throws Exception {
      String ambariServerVersion = getVersion();
      AmClusterDef clusterDef = new AmClusterDef(blueprint, privateKey, ambariServerVersion);
      restartRequiredServices(clusterDef, reports);
   }

}
