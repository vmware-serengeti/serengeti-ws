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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrapHostStatus;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackServiceComponent;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackVersionInfo;
import com.vmware.bdd.plugin.ambari.api.model.ApiTask;
import com.vmware.bdd.plugin.ambari.api.model.ApiTaskInfo;
import com.vmware.bdd.plugin.ambari.api.model.BootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.ClusterRequestStatus;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.exception.AmException;
import com.vmware.bdd.plugin.ambari.model.AmClusterDef;
import com.vmware.bdd.plugin.ambari.model.AmHealthState;
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

public class AmbariImpl implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(AmbariImpl.class);
   private static final int REQUEST_MAX_RETRY_TIMES = 10;

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
      switch (apiManager.healthCheck()) {
      case Constants.HEALTH_STATUS:
         return true;
      default:
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
      ApiStackList stackList = apiManager.stackList();
      for (ApiStack apiStack : stackList.getApiStacks()) {
         for (ApiStackVersion apiStackVersionSummary : apiManager
               .stackVersionList(apiStack.getApiStackName().getStackName())
               .getApiStackVersions()) {
            ApiStackVersionInfo apiStackVersionInfoSummary =
                  apiStackVersionSummary.getApiStackVersionInfo();
            ApiStackVersion apiStackVersion =
                  apiManager.stackVersion(
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
                     apiManager.stackServiceListWithComponents(
                           hadoopStack.getVendor(),
                           hadoopStack.getFullVersion());
               for (ApiStackService apiStackService : apiStackServiceList
                     .getApiStackServices()) {
                  for (ApiStackServiceComponent apiComponent : apiStackService
                        .getServiceComponents()) {
                     roles.add(apiComponent.getApiServiceComponent()
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
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      boolean success = false;
      AmClusterDef clusterDef = null;
      try {
         logger.info("Start cluster " + blueprint.getName() + " creation.");
         clusterDef = new AmClusterDef(blueprint, privateKey);
         provisionCluster(clusterDef, reportQueue);
         success = true;
         clusterDef.getCurrentReport().setAction("Successfully Create Cluster");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_SUCCESS.getProgress());
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         logger.error(e.getMessage());
         throw SoftwareManagementPluginException.CREATE_CLUSTER_FAILED(
               e.getMessage(), e);
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

   public boolean isProvisioned(String clusterName)
         throws SoftwareManagementPluginException {
      try {
         for (ApiCluster apiCluster : apiManager.clusterList().getClusters()) {
            if (apiCluster.getClusterInfo().getClusterName()
                  .equals(clusterName)) {
               return true;
            }
         }
      } catch (Exception e) {
         throw AmException.UNSURE_CLUSTER_EXIST(clusterName);
      }
      return false;
   }

   public void provisionCluster(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      try {
         if (!isProvisioned(clusterDef.getName())) {
            bootstrap(clusterDef, reportQueue);

            createBlueprint(clusterDef, reportQueue);

            provisionWithBlueprint(clusterDef, reportQueue);
         } else {
            /*
            For cluster resume/resize, the cluster is already exist, we need to check if this cluster is created by BDE.
            So far, just check if all IPs exist in Cloudera Cluster are included in given blueprint
             */
            // TODO
         }

      } catch (Exception e) {
         throw SoftwareManagementPluginException.CREATE_CLUSTER_FAILED(
               e.getMessage(), e);
      }
   }

   public void bootstrap(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      try {
         logger.info("Bootstrapping hosts of cluster " + clusterDef.getName());
         clusterDef.getCurrentReport().setAction("Bootstrapping host");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.BOOTSTRAP_HOSTS.getProgress());
         reportStatus(clusterDef.getCurrentReport(), reportQueue);

         ApiBootstrap apiBootstrapRequest =
               apiManager.createBootstrap(clusterDef.toApibootStrap());

         HostBootstrapPoller poller =
               new HostBootstrapPoller(apiManager, apiBootstrapRequest,
                     clusterDef.getCurrentReport(), reportQueue,
                     ProgressSplit.CREATE_BLUEPRINT.getProgress());
         poller.waitForComplete();

         boolean success = false;
         boolean allHostsBootstrapped = true;
         ApiBootstrapStatus apiBootstrapStatus =
               apiManager.bootstrapStatus(apiBootstrapRequest.getRequestId());
         BootstrapStatus bootstrapStatus =
               BootstrapStatus.valueOf(apiBootstrapStatus.getStatus());
         if (!bootstrapStatus.isFailedState()) {
            success = true;
         }

         int bootstrapedHostCount =
               apiBootstrapStatus.getApiBootstrapHostStatus().size();
         int needBootstrapHostCount = clusterDef.getNodes().size();
         if (needBootstrapHostCount != bootstrapedHostCount) {
            success = false;
            allHostsBootstrapped = false;
         }
         if (!success) {
            String errmsg = null;
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
               errmsg = "Hosts ";
               errmsg +=
                     StringUtils.join(notBootstrapNodes, ",")
                           + " are not boopstrapped. please check the hostname of those nodes are correct.";
            } else {
               errmsg = apiBootstrapStatus.getLog();
            }
            clusterDef.getCurrentReport().setAction("Failed to bootstrap host");
            throw AmException.BOOTSTRAP_FAILED(errmsg, null);
         }
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to bootstrap host");
         logger.error(e.getMessage());
         throw AmException.BOOTSTRAP_FAILED(e.getMessage(), e);
      } finally {
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
   }

   public void createBlueprint(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException {
      try {
         logger.info("Creating blueprint of cluster " + clusterDef.getName());
         clusterDef.getCurrentReport().setAction("Create blueprint");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.CREATE_BLUEPRINT.getProgress());
         reportStatus(clusterDef.getCurrentReport(), reportQueue);

         if (!isBlueprintcreated(clusterDef)) {
            apiManager.createBlueprint(clusterDef.getName(),
                  clusterDef.toApiBlueprint());
         } else {
            // TODO  consider cluster resume
         }
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to create blueprint");
         logger.error(e.getMessage());
         throw AmException.CREATE_BLUEPRINT_FAILED(e.getMessage(), e);
      } finally {
         reportStatus(clusterDef.getCurrentReport(), reportQueue);
      }
   }

   public boolean isBlueprintcreated(final AmClusterDef clusterDef)
         throws SoftwareManagementPluginException {
      try {
         for (ApiBlueprint apiBlueprint : apiManager.blueprintList()
               .getApiBlueprints()) {
            if (clusterDef.getName().equals(
                  apiBlueprint.getApiBlueprintInfo().getBlueprintName())) {
               return true;
            }
         }
      } catch (Exception e) {
         throw AmException.UNSURE_BLUEPRINT_EXIST(e.getMessage(), e);
      }
      return false;
   }

   public void provisionWithBlueprint(final AmClusterDef clusterDef,
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
               apiManager.request(clusterName, apiRequestSummary
                     .getApiRequestInfo().getRequestId());
         ClusterRequestStatus clusterRequestStatus =
               ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                     .getRequestStatus());
         if (!clusterRequestStatus.isFailedState()) {
            success = true;
         }
         if (!success) {
            throw SoftwareManagementPluginException.CREATE_CLUSTER_FAILED(
                  "Failed to provision cluster with blueprint", null);
         }

      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction(
               "Failed to provision cluster with blueprint");
         logger.error(e.getMessage());
         throw AmException.PROVISION_WITH_BLUEPRINT_FAILED(e.getMessage(), e);
      } finally {
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
   }

   @Override
   public List<String> validateScaling(NodeGroupInfo group) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      AmClusterDef clusterDef = new AmClusterDef(clusterBlueprint, null);
      String clusterName = clusterDef.getName();
      try {
         ClusterReport clusterReport = clusterDef.getCurrentReport();
         clusterReport.setAction("Ambari is starting services");
         clusterReport.setProgress(ProgressSplit.OPERATION_BEGIN.getProgress());
         boolean success = false;
         //when start services, some tasks will fail with error msg "Host Role in invalid state".
         // The failed task are random(I had saw NodeManager, ResourceManager, NAGOIS failed), and the
         // root cause is not clear by now. Each time, when I retry, it succeed. So just add retry logic to make a
         // a temp fix for it.
         //TODO(qjin): find out the root cause of failure in startting services
         for (int i = 0; i < REQUEST_MAX_RETRY_TIMES; i++) {
            ApiRequest apiRequestSummary = apiManager.startAllServicesInCluster(clusterName);
            try {
               success = doSoftwareOperation(clusterBlueprint.getName(), apiRequestSummary, clusterReport, reports);
               if (!success) {
                  logger.warn("Failed to start cluster services, retrying after 5 seconds...");
                  try {
                     Thread.sleep(5000);
                  } catch (Exception e) {
                     logger.info("interrupted when sleeping, trying to start cluster services immediately");
                  }
               } else {
                  break;
               }
            } catch (Exception e) {
               logger.warn("Got exception when start cluster", e);
            }
         }
         if (!success) {
            logger.error("Ambari failed to start services");
            throw SoftwareManagementPluginException.START_CLUSTER_FAILED(clusterName, null);
         }
      } catch (Exception e) {
         logger.error("Ambari got an exception when start services in cluster", e);
         throw SoftwareManagementPluginException.START_CLUSTER_FAILED(clusterName, e);
      }
      return true;
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

      try {
         clusterReport.setAction("Ambari is stopping services");
         clusterReport.setProgress(ProgressSplit.OPERATION_BEGIN.getProgress());

         //TODO(qjin): here we only consider unstopped services, maybe need to handle other kinds of state(STOPPING, STARTING) carefully
         ApiRequest apiRequestSummary = apiManager.stopAllServicesInCluster(clusterName);

         boolean success = doSoftwareOperation(clusterName, apiRequestSummary, clusterReport, reports);

         if (!success) {
            logger.error("Failed to stop all services in cluster");
            throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(clusterName, null);
         }
         return true;
      } catch (Exception e) {
         logger.error("Ambari got an exception when stopping cluster services: ", e);
         throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(clusterName, e);
      }
   }

   private boolean doSoftwareOperation(String clusterName, ApiRequest apiRequestSummary,
                                       ClusterReport clusterReport, ClusterReportQueue reports) throws Exception{
      reportStatus(clusterReport, reports);
      ClusterOperationPoller poller =
            new ClusterOperationPoller(apiManager, apiRequestSummary,
                  clusterName, clusterReport, reports,
                  ProgressSplit.OPERATION_FINISHED.getProgress());
      poller.waitForComplete();

      boolean success = false;
      ApiRequest apiRequest =
            apiManager.requestWithTasks(clusterName, apiRequestSummary
                  .getApiRequestInfo().getRequestId());
      ClusterRequestStatus clusterRequestStatus =
            ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                  .getRequestStatus());
      if (!clusterRequestStatus.isFailedState()) {
         success = true;
      } else {
         logger.error("Failed to do request, the apiRequestInfo is :" + ApiUtils.objectToJson(apiRequest.getApiRequestInfo()));
         List<ApiTask> apiTasks = apiRequest.getApiTasks();
         logger.info("ApiTaskInfo are: ");
         for (ApiTask apiTask : apiTasks) {
            ApiTaskInfo apiTaskInfo = apiTask.getApiTaskInfo();
            logger.info(ApiUtils.objectToJson(apiTaskInfo));
            logger.info("command: " + apiTaskInfo.getCommandDetail() +
                        "role: " + apiTaskInfo.getRole() +
                        "StructuredOut: " + apiTaskInfo.getStructuredOut() +
                        "stderr: " + apiTaskInfo.getStderr() +
                        "status: " + apiTaskInfo.getStatus());
            if (apiTaskInfo != null && apiTaskInfo.getStderr() != null) {
               logger.error(apiTaskInfo.getCommandDetail() + ": " + apiTaskInfo.getStderr());
            }
         }
      }
      return success;
   }

   @Override
   public boolean onDeleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      try {
         //Stop services if needed
         //TODO(qjin): need to check if we there is any error msg
         onStopCluster(clusterBlueprint, reports);
         ApiRequest response = apiManager.deleteCluster(clusterBlueprint.getName());
         return true;
      } catch (Exception e) {
         logger.error("Ambari got an exception when deleting cluster", e);
         throw SoftwareManagementPluginException.DELETE_CLUSTER_FAILED(clusterBlueprint.getName(), e);
      }
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
      AmHealthState state = apiManager.getClusterStatus(blueprint.getName());
      if (AmHealthState.HEALTHY == state) {
         clusterDef.getCurrentReport().setStatus(ServiceStatus.RUNNING);
      } else {
         clusterDef.getCurrentReport().setStatus(ServiceStatus.FAILED);
      }
      Map<String, AmHealthState> hostStates =
            apiManager.getHostStatus(blueprint.getName());
      Map<String, NodeReport> nodeReports =
            clusterDef.getCurrentReport().getNodeReports();
      for (AmNodeDef node : clusterDef.getNodes()) {
         String fqdn = node.getFqdn();
         AmHealthState health = hostStates.get(fqdn);
         if (AmHealthState.HEALTHY == health) {
            nodeReports.get(node.getName()).setStatus(ServiceStatus.RUNNING);
         } else {
            nodeReports.get(node.getName()).setStatus(ServiceStatus.FAILED);
         }
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
      // TODO Auto-generated method stub
      return false;
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
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException {
      List<HadoopStack> hadoopStacks = getSupportedStacks();
      Collections.<HadoopStack> sort(hadoopStacks);
      return hadoopStacks.get(0);
   }
}
