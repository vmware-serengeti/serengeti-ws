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
package com.vmware.bdd.plugin.ambari.service.impl;

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
   public Set<String> getSupportedRoles() {
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
               hadoopStack.setDistro(apiStackVersionInfo.getStackName(), apiStackVersionInfo.getStackVersion());
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
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      boolean success = false;
      try {
         AmClusterDef clusterDef = new AmClusterDef(blueprint, privateKey);
         if (!isProvisioned(clusterDef.getName())) { // TODO: if provision failed the first time, isProvisioned is true, should consider resume
            provisionCluster(clusterDef);
         }
         success = true;
      } catch (Exception e) {
         throw AmException.PROVISION_FAILED(blueprint.getName());
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

   public void provisionCluster(final AmClusterDef clusterDef)
         throws AmException {
      try {
         ApiBootstrap apiBootstrap = bootstrap(clusterDef);
         bootstrapping(apiBootstrap, clusterDef);

         createBlueprint(clusterDef);

         provisionWithBlueprint(clusterDef);

      } catch (Exception e) {
         throw AmException.PROVISION_FAILED(clusterDef.getName());
      }
   }

   public ApiBootstrap bootstrap(final AmClusterDef clusterDef)
         throws AmException {
      try {
         ApiBootstrap apiBootstrapRequest =
               apiManager.createBootstrap(clusterDef.toApibootStrap());
         return apiBootstrapRequest;
      } catch (Exception e) {
         throw AmException.BOOTSTRAP_FAILED(clusterDef.getName());
      }
   }

   public void bootstrapping(ApiBootstrap apiBootstrap,
         final AmClusterDef clusterDef) throws AmException {
      try {
         boolean success = false;
         ApiBootstrapStatus apiBootstrapStatus = null;
         while (true) {
            apiBootstrapStatus =
                  apiManager.bootstrapStatus(apiBootstrap.getRequestId());
            BootstrapStatus bootstrapStatus =
                  BootstrapStatus.valueOf(apiBootstrapStatus.getStatus());
            if (bootstrapStatus.isCompletedState()) {
               if (!bootstrapStatus.isFailedState()) {
                  success = true;
               }
               break;
            } else {
               Thread.sleep(3000);
            }
         }
         if (!success) {
            throw AmException.BOOTSTRAP_REQUEST_FAILED(apiBootstrap
                  .getRequestId());
         }

         int bootstrapedHostCount =
               apiBootstrapStatus.getApiBootstrapHostStatus().size();
         int needBootstrapHostCount = clusterDef.getNodes().size();
         if (needBootstrapHostCount != bootstrapedHostCount) {
            throw AmException.BOOTSTRAP_ALL_HOSTS_FAILED(apiBootstrap
                  .getRequestId());
         }
      } catch (Exception e) {
         throw AmException
               .BOOTSTRAP_REQUEST_FAILED(apiBootstrap.getRequestId());
      }
   }

   public void createBlueprint(final AmClusterDef clusterDef)
         throws AmException {
      try {
         if (!isBlueprintcreated(clusterDef)) {
            apiManager.createBlueprint(clusterDef.getName(),
                  clusterDef.toApiBlueprint());
         }
      } catch (Exception e) {
         throw AmException.CREATE_BLUEPRINT_FAILED(clusterDef.getName());
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

   public void provisionWithBlueprint(final AmClusterDef clusterDef)
         throws AmException {
      try {
         ApiRequest apiRequest =
               apiManager.provisionCluster(clusterDef.getName(),
                     clusterDef.toApiClusterBlueprint());
         provisioning(clusterDef.getName(), apiRequest);
      } catch (Exception e) {
         throw AmException
               .PROVISION_WITH_BLUEPRINT_FAILED(clusterDef.getName());
      }
   }

   private void provisioning(String clusterName, ApiRequest apiRequestSummary) {
      try {
         boolean success = false;
         while (true) {
            ApiRequest apiRequest =
                  apiManager.request(clusterName, apiRequestSummary
                        .getApiRequestInfo().getRequestId());
            ClusterRequestStatus clusterRequestStatus =
                  ClusterRequestStatus.valueOf(apiRequest.getApiRequestInfo()
                        .getRequestStatus());
            if (clusterRequestStatus.isCompletedState()) {
               if (!clusterRequestStatus.isFailedState()) {
                  success = true;
               }
               break;
            } else {
               Thread.sleep(3000);
            }
         }
         if (!success) {
            throw AmException.PROVISION_FAILED(clusterName);
         }
      } catch (Exception e) {
         throw AmException.PROVISION_FAILED(clusterName);
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
}
