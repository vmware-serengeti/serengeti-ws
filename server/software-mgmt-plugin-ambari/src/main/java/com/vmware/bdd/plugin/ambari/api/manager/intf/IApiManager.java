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
package com.vmware.bdd.plugin.ambari.api.manager.intf;

import java.util.List;
import java.util.Map;

import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprintList;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponents;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequestList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiService;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackComponentList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersionList;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;

public interface IApiManager {

   public ApiStackList getStackList() throws AmbariApiException;

   public ApiStack getStack(String stackName) throws AmbariApiException;

   public ApiStackVersionList getStackVersionList(String stackName) throws AmbariApiException;

   public ApiStackVersion getStackVersion(String stackName, String stackVersion) throws AmbariApiException;

   public ApiStackServiceList getStackServiceList(String stackName,
         String stackVersion) throws AmbariApiException;

   public ApiStackServiceList getStackServiceListWithComponents(String stackName,
         String stackVersion) throws AmbariApiException;

   public ApiStackServiceList getStackServiceListWithConfigurations(String stackName,
         String stackVersion) throws AmbariApiException;

   public ApiStackService getStackService(String stackName, String stackVersion,
         String stackServiceName) throws AmbariApiException;

   public List<String> getExistingHosts(String clusterName, List<String> hostNames)
         throws AmbariApiException;
   public void addHostsToCluster(String clusterName,
         List<String> hostNames) throws AmbariApiException;

   public void addComponents(String clusterName, List<String> hostNames,
         ApiHostComponents components) throws AmbariApiException;

   public ApiRequest installComponents(String clusterName) throws AmbariApiException;

   public void createConfigGroups(String clusterName,
         List<ApiConfigGroup> configGroups) throws AmbariApiException;

   public ApiRequest startComponents(String clusterName,
         List<String> hostNames, List<String> components)
         throws AmbariApiException;

   public ApiRequest stopAllComponentsInHosts(String clusterName,
         List<String> hostNames)  throws AmbariApiException;

   public void deleteAllComponents(String clusterName, String hostName)
         throws AmbariApiException;

   public List<String> getAssociatedConfigGroups(String clusterName,
         String hostName) throws AmbariApiException;

   public void deleteConfigGroup(String clusterName,
         String groupId) throws AmbariApiException;

   public ApiStackServiceList getStackWithCompAndConfigs(String stackName,
         String stackVersion) throws AmbariApiException;

   public ApiStackService getStackServiceWithComponents(String stackName,
         String stackVersion, String stackServiceName) throws AmbariApiException;

   public ApiStackComponentList getStackComponentList(String stackName,
         String stackVersion, String stackServiceName) throws AmbariApiException;

   public ApiStackComponent getStackComponent(String stackName, String stackVersion,
         String stackServiceName, String stackComponentName) throws AmbariApiException;

   public ApiClusterList getClusterList() throws AmbariApiException;

   public ApiCluster getCluster(String clusterName) throws AmbariApiException;

   public List<ApiService> getClusterServices(String clusterName) throws AmbariApiException;

   public ApiRequest stopAllServicesInCluster(String clusterName) throws AmbariApiException;

   public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException;

   public List<String> getClusterServicesNames(String clusterName) throws AmbariApiException;

   public ApiRequest provisionCluster(String clusterName,
         ApiClusterBlueprint apiClusterBlueprint) throws AmbariApiException;

   public ApiBlueprintList getBlueprintList() throws AmbariApiException;

   public ApiBlueprint getBlueprint(String blueprintName) throws AmbariApiException;

   public boolean deleteBlueprint(String blueprintName) throws AmbariApiException;

   public ApiRequestList getRequestList(String clusterName) throws AmbariApiException;

   public ApiRequest getRequest(String clusterName, Long requestId) throws AmbariApiException;

   public ApiBootstrap createBootstrap(ApiBootstrap bootstrap) throws AmbariApiException;

   public ApiBootstrapStatus getBootstrapStatus(Long bootstrapId) throws AmbariApiException;

   public ApiBlueprint createBlueprint(String blueprintName,
         ApiBlueprint blueprint) throws AmbariApiException;

   public boolean deleteCluster(String clusterName) throws AmbariApiException;

   public ApiRequest getRequestWithTasks(String clusterName, Long requestId) throws AmbariApiException;

   public ServiceStatus getClusterStatus(String clusterName) throws AmbariApiException;

   public ApiHostList getHostsSummaryInfo(String clusterName);

   public boolean deleteService(String clusterName, String serviceName);

   public ApiRequest deleteHost(String clusterName, String fqdn);
   
   public Map<String, ServiceStatus> getHostStatus(String clusterName) throws AmbariApiException;

   public String healthCheck() throws AmbariApiException;

   public String getVersion() throws AmbariApiException;
}
