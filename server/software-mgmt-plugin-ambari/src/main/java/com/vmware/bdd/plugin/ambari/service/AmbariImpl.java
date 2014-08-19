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
package com.vmware.bdd.plugin.ambari.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import javax.ws.rs.NotFoundException;

import com.vmware.bdd.plugin.ambari.api.model.ApiPersist;
import com.vmware.bdd.plugin.ambari.api.model.cluster.TaskStatus;
import com.vmware.bdd.software.mgmt.plugin.monitor.StatusPoller;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.BootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapHostStatus;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupConfiguration;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponent;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponents;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiService;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTask;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTaskInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ClusterRequestStatus;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiConfiguration;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiConfigurationInfo;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersionInfo;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.exception.AmException;
import com.vmware.bdd.plugin.ambari.model.AmClusterDef;
import com.vmware.bdd.plugin.ambari.model.AmNodeDef;
import com.vmware.bdd.plugin.ambari.poller.ClusterOperationPoller;
import com.vmware.bdd.plugin.ambari.poller.HostBootstrapPoller;
import com.vmware.bdd.plugin.ambari.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
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

public class AmbariImpl implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(AmbariImpl.class);
   private static final int REQUEST_MAX_RETRY_TIMES = 10;
   public static final String AMBARI = "ambari";

   private String privateKey;
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
      this.apiManager = new ApiManager(url, username, password);
      this.privateKey = privateKey;
   }

   @Override
   public String getName() {
      return Constants.AMBARI_PLUGIN_NAME;
   }

   @Override
   public String getDescription() {
      // TODO Auto-generated method stub
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
               hadoopStack.setRoles(roles);

               hadoopStacks.add(hadoopStack);
            }
         }
      }
      return hadoopStacks;
   }

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
         clusterDef = new AmClusterDef(blueprint, privateKey);
         provisionCluster(clusterDef, reportQueue);
         success = true;

         // All nodes use cluster message after cluster provision successfully.
         Map<String, NodeReport> nodeReports = clusterDef.getCurrentReport().getNodeReports();
         for (String nodeReportKey : nodeReports.keySet()) {
            nodeReports.get(nodeReportKey).setUseClusterMsg(true);
         }
         clusterDef.getCurrentReport().setAction("Successfully create cluster");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_SUCCESS.getProgress());
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to create cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         String errorMessage = errorMessage("Failed to create cluster " + blueprint.getName(), e);
         logger.error(errorMessage);

         throw SoftwareManagementPluginException.CREATE_CLUSTER_EXCEPTION(e, AMBARI, blueprint.getName());
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reportStatus(clusterDef.getCurrentReport(), reportQueue);
      }
      return success;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   private boolean isProvisioned(String clusterName)
         throws SoftwareManagementPluginException {
      try {
         for (ApiCluster apiCluster : apiManager.getClusterList().getClusters()) {
            if (apiCluster.getClusterInfo().getClusterName()
                  .equals(clusterName)) {
               return true;
            }
         }
      } catch (Exception e) {
         throw AmException.UNSURE_CLUSTER_EXIST(AMBARI, clusterName);
      }
      return false;
   }

   private void provisionCluster(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      try {
         if (!isProvisioned(clusterDef.getName())) {
            bootstrap(clusterDef, reportQueue);

            createBlueprint(clusterDef, reportQueue);

            provisionWithBlueprint(clusterDef, reportQueue);
         } else {
            if (isClusterProvisionedByBDE(clusterDef)) {
               bootstrap(clusterDef, reportQueue);

               createBlueprint(clusterDef, reportQueue);

               provisionWithBlueprint(clusterDef, reportQueue);
            }
         }

      } catch (Exception e) {
         String errorMessage = errorMessage("Failed to provision cluster " + clusterDef.getName(), e);
         logger.error(errorMessage);

         throw SoftwareManagementPluginException.CREATE_CLUSTER_EXCEPTION(e, AMBARI, clusterDef.getName());
      }
   }

   private boolean isClusterProvisionedByBDE(final AmClusterDef clusterDef) {
      /*
      For cluster resume/resize, the cluster is already exist, we need to check if this cluster is created by BDE.
      So far, just check if all hostnames exist in Ambari Cluster are included in given blueprint
       */
      String clusterName = clusterDef.getName();
      Set<String> hostnames = new HashSet<String>();
      for (AmNodeDef node : clusterDef.getNodes()) {
         hostnames.add(node.getFqdn());
      }
      for (ApiHost apiHost : apiManager.getCluster(clusterName).getApiHosts()) {
         if (!hostnames.contains(apiHost.getApiHostInfo().getHostName())) {
            throw SoftwareManagementPluginException.CLUSTER_ALREADY_EXIST(clusterName);
         }
      }
      return true;
   }

   private void bootstrap(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      bootstrap(clusterDef, null, reportQueue);
   }

   private void bootstrap(final AmClusterDef clusterDef,
         final List<String> addedHosts,
         final ClusterReportQueue reportQueue)
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

         ApiBootstrap apiBootstrapRequest =
               apiManager.createBootstrap(clusterDef.toApiBootStrap(addedHosts));

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

         int bootstrapedHostCount =
               apiBootstrapStatus.getApiBootstrapHostStatus().size();
         int needBootstrapHostCount = -1;
         if (addedHosts == null) {
            needBootstrapHostCount = clusterDef.getNodes().size();
         } else {
            needBootstrapHostCount = addedHosts.size();
         }
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

            String actionFailure = "Failed to bootstrap host";
            if (addedHosts != null) {
               clusterDef.getCurrentReport().setNodesError(actionFailure, addedHosts);
            } else {
               clusterDef.getCurrentReport().setErrMsg(actionFailure);
            }

            throw AmException.BOOTSTRAP_FAILED(notBootstrapNodes != null? notBootstrapNodes.toArray() : null);
         }
      } catch (Exception e) {
         clusterDef.getCurrentReport().setErrMsg("Failed to bootstrap host");
         String errorMessage = errorMessage("Failed to bootstrap hosts of cluster " + clusterDef.getName(), e);
         logger.error(errorMessage);
         throw AmException.BOOTSTRAP_FAILED_EXCEPTION(e, clusterDef.getName());
      } finally {
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
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

         if (!isBlueprintCreated(clusterDef)) {
            apiManager.createBlueprint(clusterName, clusterDef.toApiBlueprint());
         } else {
            if (isBlueprintCreatedByBDE(clusterDef)) {

               // For cluster resume/resize, the blueprint is already exist, we need to delete this blueprint first.
               if (isBlueprintCreatedByBDE(clusterDef)) {
                  apiManager.deleteBlueprint(clusterName);
               }

               apiManager.createBlueprint(clusterName, clusterDef.toApiBlueprint());
            }
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
      String clusterName = clusterDef.getName();
      ApiBlueprint apiBlueprint = apiManager.getBlueprint(clusterName);

      Map<String, Set> GroupNamesWithComponents = new HashMap<String, Set>();
      for (AmNodeDef node : clusterDef.getNodes()) {
         Set<String> components = new HashSet<String>();
         GroupNamesWithComponents.put(node.getName(), components);
      }

      for (ApiHostGroup apiHostGroup : apiBlueprint.getApiHostGroups()) {
         String groupName = apiHostGroup.getName();
         if (!GroupNamesWithComponents.containsKey(groupName)) {
            throw AmException.BLUEPRINT_ALREADY_EXIST(clusterName);
         }
         Set<String> components = GroupNamesWithComponents.get(groupName);
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

         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterName, 120);

         // For cluster resume/resize, the blueprint is already exist, we need to delete this cluster first.
         if (isProvisioned(clusterName) && isClusterProvisionedByBDE(clusterDef)) {
            try {
               if (hasHosts(clusterName)) {
                  ApiRequest apiRequestSummary = apiManager.stopAllServicesInCluster(clusterName);
                  doSoftwareOperation(clusterName, apiRequestSummary, clusterDef.getCurrentReport(), reportQueue);
               }
            } catch (Exception e) {
               String errMsg = getErrorMsg("stop all services", e);
               logger.error(errMsg, e);
               throw SoftwareManagementPluginException.STOP_CLUSTER_EXCEPTION(e, AMBARI, clusterName);
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
            throw SoftwareManagementPluginException.CREATE_CLUSTER_FAIL(AMBARI, clusterDef.getName());
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

   private boolean hasHosts(String clusterName) {
      return !apiManager.getClusterHostsList(clusterName).getApiHosts().isEmpty();
   }

   private String getErrorMsg(String action, Exception e) {
      StringBuilder errMsg = new StringBuilder();
      errMsg.append("Failed to " + action);
      if (e != null && e.getMessage() != null && !e.getMessage().isEmpty()) {
         errMsg.append(": " + e.getMessage());
      }
      return errMsg.toString();
   }
   @Override
   public List<String> validateScaling(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return new ArrayList<String>();
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return true;
   }

   @Override
   public boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      boolean success = false;
      AmClusterDef clusterDef = null;
      try {
         logger.info("Blueprint:");
         logger.info(ApiUtils.objectToJson(blueprint));
         logger.info("Start cluster " + blueprint.getName() + " scale out.");
         clusterDef = new AmClusterDef(blueprint, privateKey);
         bootstrap(clusterDef, addedNodeNames, reports);
         provisionComponents(clusterDef, addedNodeNames, reports);
         success = true;

         clusterDef.getCurrentReport().setNodesAction("", addedNodeNames);
         clusterDef.getCurrentReport().setNodesStatus(ServiceStatus.STARTED, addedNodeNames);
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_SUCCESS.getProgress());
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (Exception e) {
         clusterDef.getCurrentReport().setNodesError(
               "Failed to bootstrap nodes for " + e.getMessage(),
               addedNodeNames);
         clusterDef.getCurrentReport().setSuccess(false);
         String errorMessage = errorMessage("Failed to scale out cluster " + blueprint.getName(), e);
         logger.error(errorMessage, e);
         throw SoftwareManagementPluginException.SCALE_OUT_CLUSTER_FAILED(e, AMBARI, blueprint.getName());
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reportStatus(clusterDef.getCurrentReport(), reports);
      }
      return success;
   }

   private boolean provisionComponents(AmClusterDef clusterDef, List<String> addedNodeNames,
         ClusterReportQueue reports) throws Exception {
      logger.info("Installing roles " + addedNodeNames);
      ApiStackServiceList stackServiceList =
            apiManager.getStackWithCompAndConfigs(clusterDef.getAmStack()
                  .getName(), clusterDef.getAmStack().getVersion());
      Map<String, String> configTypeToService = stackServiceList.configTypeToService();
      Map<String, ApiComponentInfo> componentToInfo = stackServiceList.componentToInfo();
      ApiHostComponents apiHostComponents = null;
      Set<String> serviceNames = getExistingClusterServices(clusterDef);
      List<String> targetHostNames = new ArrayList<>();
      List<AmNodeDef> targetNodeDefs = new ArrayList<>();
      for (AmNodeDef nodeDef : clusterDef.getNodes()) {
         if (addedNodeNames.contains(nodeDef.getName())) {
            if (apiHostComponents == null) {
               apiHostComponents =
                     createHostComponents(componentToInfo, serviceNames,
                           nodeDef);
            }
            targetHostNames.add(nodeDef.getFqdn());
            targetNodeDefs.add(nodeDef);
         }
      }
      removeHosts(clusterDef, targetHostNames, reports);
      apiManager.addHostsToCluster(clusterDef.getName(), targetHostNames);
      if (apiHostComponents.getHostComponents().isEmpty()) {
         logger.info("No roles need to install on hosts.");
         return true;
      }
      // add configurations
      createConfigGroups(clusterDef, configTypeToService, targetNodeDefs);
      installComponents(clusterDef, reports, apiHostComponents, targetHostNames);
      return startAllComponents(clusterDef, componentToInfo, apiHostComponents,
            targetHostNames, reports);
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
      if (apiRequestSummary.getApiRequestInfo() == null) {
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
         throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(AMBARI, clusterDef.getName());
      }
   }

   private boolean startAllComponents(AmClusterDef clusterDef,
         Map<String, ApiComponentInfo> componentToInfo,
         ApiHostComponents apiHostComponents, List<String> targetHostNames,
         ClusterReportQueue reports) 
         throws Exception {
      List<String> componentNames = new ArrayList<>();
      for (ApiHostComponent hostComponent : apiHostComponents.getHostComponents()) {
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
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(null, AMBARI, clusterDef.getName());
      }
      return success;
   }

   private void createConfigGroups(AmClusterDef clusterDef,
         Map<String, String> configTypeToService, List<AmNodeDef> targetNodeDefs) 
               throws SoftwareManagementPluginException {
      List<ApiConfigGroup> configGroups = new ArrayList<>();
      Map<String, ApiConfigGroup> serviceToGroup = new HashMap<>();
      for (AmNodeDef nodeDef : targetNodeDefs) {
         // for each node, one set of config group will be created
         // for each service, one config group will be created, which contains all property types belong to this service
         serviceToGroup.clear();
         List<Map<String, Object>> configs = nodeDef.getConfigurations();
         int i = 1;
         for (Map<String, Object> map : configs) {
            for (String type : map.keySet()) {
               String serviceName = configTypeToService.get(type + ".xml");
               ApiConfigGroup confGroup = serviceToGroup.get(serviceName);
               if (confGroup == null) {
                  confGroup = createConfigGroup(clusterDef, nodeDef, serviceName);
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
                  sameType =
                        createApiConfigGroupConf(i, type, serviceName,
                              confGroup);
               }
               Map<String, String> property = (Map<String, String>)map.get(type);
               sameType.getProperties().putAll(property);
            }
         }
         configGroups.addAll(serviceToGroup.values());
      }
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

   private ApiConfigGroup createConfigGroup(AmClusterDef clusterDef, AmNodeDef nodeDef,
         String serviceName) {
      ApiConfigGroup confGroup;
      confGroup = new ApiConfigGroup();
      ApiConfigGroupInfo info = new ApiConfigGroupInfo();
      confGroup.setApiConfigGroupInfo(info);
      info.setClusterName(clusterDef.getName());
      info.setDescription(serviceName + " configuration");
      info.setGroupName(nodeDef.getName());
      List<ApiHostInfo> hosts = new ArrayList<>();
      ApiHostInfo hostInfo = new ApiHostInfo();
      hostInfo.setHostName(nodeDef.getFqdn());
      hosts.add(hostInfo);
      info.setHosts(hosts);
      info.setTag(serviceName);
      List<ApiConfigGroupConfiguration> desiredConfigs = new ArrayList<>();
      info.setDesiredConfigs(desiredConfigs);
      return confGroup;
   }

   private boolean installComponents(AmClusterDef clusterDef,
         ClusterReportQueue reports, ApiHostComponents apiHostComponents,
         List<String> targetHostNames) throws Exception {
      // add components to target hosts concurrently
      apiManager.addComponents(clusterDef.getName(), targetHostNames, apiHostComponents);
      ApiRequest request = apiManager.installComponents(clusterDef.getName());
      ClusterOperationPoller poller =
            new ClusterOperationPoller(apiManager, request,
                  clusterDef.getName(), clusterDef.getCurrentReport(), reports,
                  ProgressSplit.PROVISION_CLUSTER.getProgress());
      poller.waitForComplete();

      boolean success = false;
      ApiRequest apiRequest =
            apiManager.getRequest(clusterDef.getName(), request
                  .getApiRequestInfo().getRequestId());
      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());
      if (!clusterRequestStatus.isFailedState()) {
         success = true;
      }
      if (!success) {
         throw SoftwareManagementPluginException.INSTALL_COMPONENTS_FAIL(AMBARI, clusterDef.getName());
      }
      return success;
   }

   private ApiHostComponents createHostComponents(
         Map<String, ApiComponentInfo> componentToInfo,
         Set<String> serviceNames, AmNodeDef nodeDef) {
      ApiHostComponents apiHostComponents = new ApiHostComponents();
      List<ApiHostComponent> hostComponents = new ArrayList<>();
      apiHostComponents.setHostComponents(hostComponents);
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
      return apiHostComponents;
   }

   private Set<String> getExistingClusterServices(AmClusterDef clusterDef) {
      List<ApiService> services = apiManager.getClusterServices(clusterDef.getName());
      Set<String> serviceNames = new HashSet<>();
      for (ApiService service : services) {
         serviceNames.add(service.getServiceInfo().getServiceName());
      }
      return serviceNames;
   }

   @Override
   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, null);
      String clusterName = clusterDef.getName();
      if (!isProvisioned(clusterName)) {
         throw AmException.CLUSTER_NOT_PROVISIONED(clusterName);
      }
      if (!isClusterProvisionedByBDE(clusterDef)) {
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED_NOT_PROV_BY_BDE(clusterName);
      }

      ClusterReport clusterReport = clusterDef.getCurrentReport();
      clusterReport.setAction("Ambari is starting services");
      clusterReport.setProgress(ProgressSplit.OPERATION_BEGIN.getProgress());
      reportStatus(clusterReport, reports);
      boolean success = false;
      //when start services, some tasks will fail with error msg "Host Role in invalid state".
      // The failed task are random(I had saw NodeManager, ResourceManager, NAGOIS failed), and the
      // root cause is not clear by now. Each time, when I retry, it succeed. So just add retry logic to make a
      // a temp fix for it.
      //TODO(qjin): find out the root cause of failure in startting services
      Exception resultException = null;
      try {
         ReflectionUtils.getPreStartServicesHook().preStartServices(clusterName, 120);
         for (int i = 0; i < REQUEST_MAX_RETRY_TIMES; i++) {
            ApiRequest apiRequestSummary;
            try {
               apiRequestSummary = apiManager.startAllServicesInCluster(clusterName);
               //when reach here, command is succeed. If ApiRequestInfo is null, it means the command has been
               //finished successfully, otherwise we need to wait for it using doSoftwareOperation
               if (apiRequestSummary.getApiRequestInfo() == null) {
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
            String errMsg = getErrorMsg("start all services", resultException);
            logger.error(errMsg, resultException);
            throw SoftwareManagementPluginException.START_CLUSTER_FAILED(null, AMBARI, clusterName);
         }

         clusterReport.setClusterAndNodesServiceStatus(ServiceStatus.STARTED);
         clusterReport.setClusterAndNodesAction("");
         clusterReport.clearAllNodesErrorMsg();
         reportStatus(clusterReport.clone(), reports);
         return true;
      }
   }

   private void waitServiceToStart(final String clusterName, final String serviceName) throws Exception {
      StatusPoller servicePoller = new StatusPoller() {
         @Override
         public boolean poll() {
            return apiManager.isServiceStarted(clusterName, serviceName);
         }
      };
      servicePoller.waitForComplete();
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
      AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, null);
      ClusterReport clusterReport = clusterDef.getCurrentReport();
      boolean success = false;
      try {
         if (!isProvisioned(clusterName)) {
            return true;
         }
         if (!isClusterProvisionedByBDE(clusterDef)) {
            throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED("Cannot stop a cluster that is not provisioned by Big Data Extension", null);
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
         String errMsg = getErrorMsg("stop all services", e);
         logger.error(errMsg, e);
         throw SoftwareManagementPluginException.STOP_CLUSTER_EXCEPTION(e, AMBARI, clusterName);
      }
   }

   private boolean doSoftwareOperation(String clusterName, ApiRequest apiRequestSummary,
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
         AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, null);
         if (!isClusterProvisionedByBDE(clusterDef)) {
            return true;
         }
         //Stop services if needed, when stop failed, we will try to forcely delete resource
         //although we may also fail because of resource dependency. In that case, we will
         //throw out the exception and fail
         if (!onStopCluster(clusterBlueprint, reports)) {
            logger.error("Ambari failed to stop services");
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
         throw SoftwareManagementPluginException.DELETE_CLUSTER_FAILED(e, AMBARI,clusterBlueprint.getName());
      }
   }

   @Override
   public boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames) throws SoftwareManagementPluginException {
      AmClusterDef clusterDef = null;
      try {
         logger.info("Delete nodes " + nodeNames + " from cluster " + blueprint.getName());
         clusterDef = new AmClusterDef(blueprint, privateKey);
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
         throw SoftwareManagementPluginException.DELETE_NODES_FAILED(e, AMBARI, nodeNames.toArray());
      }
      return true;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) {
      AmClusterDef clusterDef = new AmClusterDef(blueprint, privateKey);
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
      return amClusterValidator.validateBlueprint(blueprint);
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

   private void reportStatus(final ClusterReport clusterReport,
         final ClusterReportQueue reportQueue) {
      reportQueue.addClusterReport(clusterReport.clone());
   }

   @Override
   public String getVersion() {
      try {
         return apiManager.getVersion();
      } catch (Exception e) {
         // we print the log here for user to check the cause.
         String errMsg = "Cannot connect to the Software Manager, check the connection information.";
         logger.error(errMsg, e);
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

   private String errorMessage(String defaultMessage, Exception e) {
      String errorMessage = defaultMessage;
      if (e.getMessage() != null) {
         errorMessage = e.getMessage();
      }
      return errorMessage;
   }
}
