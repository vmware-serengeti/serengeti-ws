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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackVersionInfo;
import com.vmware.bdd.plugin.ambari.api.model.BootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.ClusterRequestStatus;
import com.vmware.bdd.plugin.ambari.exception.AmException;
import com.vmware.bdd.plugin.ambari.model.AmClusterDef;
import com.vmware.bdd.plugin.ambari.poller.ClusterProvisionPoller;
import com.vmware.bdd.plugin.ambari.poller.HostBootstrapPoller;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

public class AmbariImpl implements SoftwareManager {

   private static final Logger logger = Logger.getLogger(AmbariImpl.class);

   private String privateKey;
   private ApiManager apiManager;

   private final static int INVALID_PROGRESS = -1;

   private enum ProgressSplit {
      BOOTSTRAP_HOSTS(10),
      CREATE_BLUEPRINT(30),
      PROVISION_CLUSTER(50),
      PROVISION_SUCCESS(100);

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

   public ApiManager getApiManager() {
      return this.apiManager;
   }

   @Override
   public String getName() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getDescription() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getType() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean echo() {
      return true;
      // TODO Auto-generated method stub

   }

   @Override
   public HealthStatus getStatus() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<String> getSupportedRoles(HadoopStack hadoopStack) {
      // TODO Auto-generated method stub
      return null;
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
         clusterDef = new AmClusterDef(blueprint, privateKey);
         provisionCluster(clusterDef, reportQueue);
         success = true;
         clusterDef.getCurrentReport().setAction("Successfully Create Cluster");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_SUCCESS.getProgress());
         clusterDef.getCurrentReport().setSuccess(true);
      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to Create Cluster");
         clusterDef.getCurrentReport().setSuccess(false);
         logger.error(e.getMessage());
         throw SoftwareManagementPluginException.CREATE_CLUSTER_FAILED(
               e.getMessage(), e);
      } finally {
         clusterDef.getCurrentReport().setFinished(true);
         reportStatus(clusterDef, reportQueue);
      }
      return success;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   public boolean isProvisioned(String clusterName) throws AmException {
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
         final ClusterReportQueue reportQueue) throws AmException {
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
         throw AmException.PROVISION_FAILED(clusterDef.getName());
      }
   }

   public void bootstrap(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue) throws AmException {
      try {
         clusterDef.getCurrentReport().setAction("Bootstrapping host");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.BOOTSTRAP_HOSTS.getProgress());
         reportStatus(clusterDef, reportQueue);

         ApiBootstrap apiBootstrapRequest =
               apiManager.createBootstrap(clusterDef.toApibootStrap());

         HostBootstrapPoller poller =
               new HostBootstrapPoller(apiManager, apiBootstrapRequest,
                     clusterDef.getCurrentReport(), reportQueue,
                     ProgressSplit.CREATE_BLUEPRINT.getProgress());
         poller.waitForComplete();

         boolean success = false;
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
         }
         if (!success) {
            // TODO Get error message
            clusterDef.getCurrentReport().setAction("Failed to bootstrap host");
            throw AmException.BOOTSTRAP_REQUEST_FAILED(apiBootstrapRequest
                  .getRequestId());
         }
      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction("Failed to bootstrap host");
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to bootstrap host");
         logger.error(e.getMessage());
         throw AmException.BOOTSTRAP_FAILED(clusterDef.getName());
      } finally {
         reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
      }
   }

   public void createBlueprint(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue) throws AmException {
      try {
         clusterDef.getCurrentReport().setAction("Create blueprint");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.CREATE_BLUEPRINT.getProgress());
         reportStatus(clusterDef, reportQueue);

         if (!isBlueprintcreated(clusterDef)) {
            apiManager.createBlueprint(clusterDef.getName(),
                  clusterDef.toApiBlueprint());
         } else {
            // TODO  consider cluster resume
         }
      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction("Failed to create blueprint");
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction("Failed to create blueprint");
         logger.error(e.getMessage());
         throw AmException.CREATE_BLUEPRINT_FAILED(clusterDef.getName());
      } finally {
         reportStatus(clusterDef, reportQueue);
      }
   }

   public boolean isBlueprintcreated(final AmClusterDef clusterDef)
         throws AmException {
      try {
         for (ApiBlueprint apiBlueprint : apiManager.blueprintList()
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

   public void provisionWithBlueprint(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue) throws AmException {
      try {
         clusterDef.getCurrentReport().setAction(
               "Provisioning cluster with blueprint");
         clusterDef.getCurrentReport().setProgress(
               ProgressSplit.PROVISION_CLUSTER.getProgress());
         reportStatus(clusterDef, reportQueue);

         String clusterName = clusterDef.getName();
         ApiRequest apiRequestSummary =
               apiManager.provisionCluster(clusterDef.getName(),
                     clusterDef.toApiClusterBlueprint());

         ClusterProvisionPoller poller =
               new ClusterProvisionPoller(apiManager, apiRequestSummary,
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
            // TODO Get error message
            throw AmException.PROVISION_FAILED(clusterName);
         }

      } catch (SoftwareManagementPluginException ex) {
         clusterDef.getCurrentReport().setAction(
               "Failed to provision cluster with blueprint");
         throw ex;
      } catch (Exception e) {
         clusterDef.getCurrentReport().setAction(
               "Failed to provision cluster with blueprint");
         logger.error(e.getMessage());
         throw AmException
               .PROVISION_WITH_BLUEPRINT_FAILED(clusterDef.getName());
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
   public boolean startCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO(qjin): need to stop all services in Ambari deployed cluster
      return true;
   }

   @Override
   public boolean onDeleteCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
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
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint,
         List<String> distroRoles) throws ValidationException {
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

   private void reportStatus(final AmClusterDef clusterDef,
         final ClusterReportQueue reportQueue) {
      reportQueue.addClusterReport(clusterDef.getCurrentReport().clone());
   }

   @Override
   public String getVersion() {
      // TODO Auto-generated method stub
      return null;
   }
}