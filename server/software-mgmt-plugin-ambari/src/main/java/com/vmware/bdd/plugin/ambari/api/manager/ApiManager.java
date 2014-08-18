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
package com.vmware.bdd.plugin.ambari.api.manager;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.model.ApiPersist;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.manager.intf.IApiManager;
import com.vmware.bdd.plugin.ambari.api.model.ApiBody;
import com.vmware.bdd.plugin.ambari.api.model.ApiErrorMessage;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostsRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.ApiPutRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiRootServicesComponents;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprintList;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiAlert;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponents;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequestList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiService;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceAlert;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceAlertList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceStatus;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackComponentList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersionList;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;

public class ApiManager implements IApiManager {

   private static final Logger logger = Logger.getLogger(ApiManager.class);

   private RootResourceV1 apiResourceRootV1;

   public ApiManager(String amServerHost, int port, String user, String password) {
      ApiRootResource amApiRootResource =
            new AmbariManagerClientbuilder().withHost(amServerHost)
                  .withPort(port).withUsernamePassword(user, password).build();

      apiResourceRootV1 = amApiRootResource.getRootV1();
   }

   public ApiManager(URL baseUrl, String user, String password) {
      ApiRootResource amApiRootResource =
            new AmbariManagerClientbuilder().withBaseURL(baseUrl)
                  .withUsernamePassword(user, password).build();

      apiResourceRootV1 = amApiRootResource.getRootV1();
   }

   @Override
   public ApiStackList getStackList() throws AmbariApiException {
      Response response = apiResourceRootV1.getStacks2Resource().readStacks();
      String stacksJson = handleAmbariResponse(response);
      logger.debug("Response of stack list from ambari server:");
      logger.debug(stacksJson);
      ApiStackList apiStackList =
            ApiUtils.jsonToObject(ApiStackList.class, stacksJson);
      return apiStackList;
   }

   @Override
   public ApiStack getStack(String stackName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource().readStack(stackName);
      String stackJson = handleAmbariResponse(response);
      logger.debug("Response of stack from ambari server:");
      logger.debug(stackJson);
      ApiStack apiStack = ApiUtils.jsonToObject(ApiStack.class, stackJson);
      return apiStack;
   }

   @Override
   public ApiService readService(String clusterName, String serviceName) throws AmbariApiException {
      Response response = apiResourceRootV1.getClustersResource().getServicesResource(clusterName).readService(serviceName);
      String serviceJson = handleAmbariResponse(response);
      logger.debug("Service " + serviceName + " is: " + serviceJson);
      ApiService apiService = ApiUtils.jsonToObject(ApiService.class, serviceJson);
      return apiService;
   }

   @Override
   public boolean isServiceStarted(String clusterName, String serviceName) throws AmbariApiException {
      ApiService service = readService(clusterName, serviceName);
      String serviceState = service.getServiceInfo().getState();
      return ApiServiceStatus.STARTED.name().equalsIgnoreCase(serviceState);
   }

   @Override
   public ApiStackVersionList getStackVersionList(String stackName)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName).readStackVersions();
      String stackVersionsJson = handleAmbariResponse(response);
      logger.debug("Response of version list of stack from ambari server:");
      logger.debug(stackVersionsJson);
      ApiStackVersionList apiStackVersionList =
            ApiUtils.jsonToObject(ApiStackVersionList.class, stackVersionsJson);
      return apiStackVersionList;
   }

   @Override
   public ApiStackVersion getStackVersion(String stackName, String stackVersion)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .readStackVersion(stackVersion);
      String stackVersionJson = handleAmbariResponse(response);
      logger.debug("Response of version of stack from ambari server:");
      logger.debug(stackVersionJson);
      ApiStackVersion apiStackVersion =
            ApiUtils.jsonToObject(ApiStackVersion.class, stackVersionJson);
      return apiStackVersion;
   }

   @Override
   public ApiStackServiceList getStackServiceList(String stackName,
         String stackVersion) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion).readServices();
      String apiStackServicesJson = handleAmbariResponse(response);
      logger.debug("Response of service list of stack from ambari server:");
      logger.debug(apiStackServicesJson);
      ApiStackServiceList apiStackServices =
            ApiUtils.jsonToObject(ApiStackServiceList.class,
                  apiStackServicesJson);
      return apiStackServices;
   }

   @Override
   public ApiStackServiceList getStackServiceListWithComponents(
         String stackName, String stackVersion) throws AmbariApiException {
      return getServicesWithFilter(stackName, stackVersion,
            "serviceComponents/*,serviceComponents/dependencies");
   }

   @Override
   public ApiStackServiceList getStackServiceListWithConfigurations(
         String stackName, String stackVersion) throws AmbariApiException {
      return getServicesWithFilter(stackName, stackVersion,
            "configurations/StackConfigurations/type");
   }

   @Override
   public ApiStackService getStackServiceWithComponents(String stackName,
         String stackVersion, String serviceName) throws AmbariApiException {
      return getServiceWithFilter(stackName, stackVersion, serviceName,
            "serviceComponents/*,serviceComponents/dependencies");
   }

   @Override
   public ApiStackService getStackService(String stackName,
         String stackVersion, String stackServiceName)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .readService(stackServiceName);
      String apiStackServiceJson = handleAmbariResponse(response);
      logger.debug("Response of service of stack from ambari server:");
      logger.debug(apiStackServiceJson);
      ApiStackService apiStackService =
            ApiUtils.jsonToObject(ApiStackService.class, apiStackServiceJson);
      return apiStackService;
   }

   @Override
   public ApiStackComponentList getStackComponentList(String stackName,
         String stackVersion, String stackServiceName)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .getComponentsResource(stackServiceName)
                  .readComponents();
      String stackComponentsJson = handleAmbariResponse(response);
      logger.debug("Response of component list of service from ambari server:");
      logger.debug(stackComponentsJson);
      ApiStackComponentList apiServiceComponents =
            ApiUtils.jsonToObject(ApiStackComponentList.class,
                  stackComponentsJson);
      return apiServiceComponents;
   }

   @Override
   public ApiStackComponent getStackComponent(String stackName,
         String stackVersion, String stackServiceName, String stackComponentName)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .getComponentsResource(stackServiceName)
                  .readComponent(stackComponentName);
      String stackComponentJson = handleAmbariResponse(response);
      logger.debug("Response of component of service from ambari server:");
      logger.debug(stackComponentJson);
      ApiStackComponent apiServiceComponent =
            ApiUtils.jsonToObject(ApiStackComponent.class,
                  stackComponentJson);
      return apiServiceComponent;
   }

   @Override
   public ApiClusterList getClusterList() throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource().readClusters();
      String clustersJson = handleAmbariResponse(response);
      logger.debug("Response of cluster list from ambari server:");
      logger.debug(clustersJson);
      ApiClusterList apiClusterList =
            ApiUtils.jsonToObject(ApiClusterList.class, clustersJson);
      return apiClusterList;
   }

   @Override
   public ApiCluster getCluster(String clusterName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource().readCluster(clusterName);
      String clusterJson = handleAmbariResponse(response);
      logger.debug("Response of cluster from ambari server:");
      logger.debug(clusterJson);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      return apiCluster;
   }

   @Override
   public List<ApiService> getClusterServices(String clusterName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource().readCluster(clusterName);
      String clusterJson = handleAmbariResponse(response);
      logger.trace("in getClusterServicesNames, cluster info is " + clusterJson);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      return apiCluster.getApiServices();
   }

   @Override
   public ApiRequest stopAllServicesInCluster(String clusterName) throws AmbariApiException {
      logger.info("Ambari is stopping all services in cluster " + clusterName);
      ApiServiceInfo serviceInfo = new ApiServiceInfo();
      serviceInfo.setState(ApiServiceStatus.INSTALLED.name());
      ApiBody body = new ApiBody();
      body.setServiceInfo(serviceInfo);
      ApiRequestInfo requestInfo = new ApiRequestInfo();
      requestInfo.setContext("Stop All Services");
      ApiPutRequest stopRequest = new ApiPutRequest(requestInfo, body);
      String request = ApiUtils.objectToJson(stopRequest);
      logger.debug("The request in stop cluster is :" + request);

      Response response =
            apiResourceRootV1.getClustersResource()
                  .getServicesResource(clusterName)
                  .stopAllServices(clusterName, "true", request);
      String stopServicesJson = handleAmbariResponse(response);
      logger.debug("The response when ambari stop cluster is :"
            + stopServicesJson);
      return ApiUtils.jsonToObject(ApiRequest.class, stopServicesJson);
   }

   @Override
   public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException {
      ApiServiceInfo serviceInfo = new ApiServiceInfo();
      serviceInfo.setState(ApiServiceStatus.STARTED.name());
      ApiBody body = new ApiBody();
      body.setServiceInfo(serviceInfo);
      ApiRequestInfo requestInfo = new ApiRequestInfo();
      requestInfo.setContext("Start All Services");
      ApiPutRequest stopRequest = new ApiPutRequest(requestInfo, body);
      String request = ApiUtils.objectToJson(stopRequest);
      logger.debug("The request in start cluster is :" + request);

      Response response =
            apiResourceRootV1.getClustersResource()
                  .getServicesResource(clusterName)
                  .startAllServices(clusterName, "true", request);
      String startServicesJson = handleAmbariResponse(response);
      logger.debug("The reponse when startAllService is :" + startServicesJson);
      return ApiUtils.jsonToObject(ApiRequest.class, startServicesJson);
   }

   @Override
   public List<String> getClusterServicesNames(String clusterName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource().readCluster(clusterName);
      String clusterJson = handleAmbariResponse(response);
      logger.trace("in getClusterServicesNames, cluster info is " + clusterJson);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      List<ApiService> apiServices = apiCluster.getApiServices();
      List<String> servicesNames = null;
      for (ApiService apiService : apiServices) {
         if (apiService != null) {
            ApiServiceInfo serviceInfo = apiService.getServiceInfo();
            if (serviceInfo != null) {
               String serviceName = serviceInfo.getServiceName();
               if (serviceName != null) {
                  if (servicesNames == null) {
                     servicesNames = new ArrayList<String>();
                  }
                  servicesNames.add(serviceName);
               }
            } else {
               logger.info("service info is empty when read cluster " + clusterName);
            }
         }
      }
      return servicesNames;
   }

   @Override
   public ApiRequest provisionCluster(String clusterName,
         ApiClusterBlueprint apiClusterBlueprint) throws AmbariApiException {
      logger.info("ApiClusterBlueprint:");
      logger.info(ApiUtils.objectToJson(apiClusterBlueprint));
      Response response =
            apiResourceRootV1.getClustersResource().createCluster(clusterName,
                  ApiUtils.objectToJson(apiClusterBlueprint));
      String requestJson = handleAmbariResponse(response);
      logger.debug("Response of provision cluster with blueprint from ambari server:");
      logger.debug(requestJson);
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestJson);
      return apiRequest;
   }

   @Override
   public ApiBlueprintList getBlueprintList() throws AmbariApiException {
      Response response =
            apiResourceRootV1.getBlueprintsResource().readBlueprints();
      String blueprintsJson = handleAmbariResponse(response);
      logger.debug("Response of blueprint list from ambari server:");
      logger.debug(blueprintsJson);
      ApiBlueprintList apiBlueprintList =
            ApiUtils.jsonToObject(ApiBlueprintList.class, blueprintsJson);
      return apiBlueprintList;
   }

   @Override
   public ApiBlueprint getBlueprint(String blueprintName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getBlueprintsResource().readBlueprint(
                  blueprintName);
      String blueprintJson = handleAmbariResponse(response);
      logger.debug("Response of blueprint from ambari server:");
      logger.debug(blueprintJson);
      ApiBlueprint apiBlueprint =
            ApiUtils.jsonToObject(ApiBlueprint.class, blueprintJson);
      return apiBlueprint;
   }

   @Override
   public ApiBlueprint createBlueprint(String blueprintName,
         ApiBlueprint apiBlueprint) throws AmbariApiException {
      logger.info("ApiBlueprint:");
      logger.info(ApiUtils.objectToJson(apiBlueprint));
      Response response =
            apiResourceRootV1.getBlueprintsResource().createBlueprint(
                  blueprintName, ApiUtils.objectToJson(apiBlueprint));
      String blueprintJson = handleAmbariResponse(response);
      logger.debug("Response of blueprint creation from ambari server:");
      logger.debug(blueprintJson);
      ApiBlueprint apiBlueprintResult =
            ApiUtils.jsonToObject(ApiBlueprint.class, blueprintJson);
      return apiBlueprintResult;
   }

   public boolean updatePersist(ApiPersist persist) throws AmbariApiException {
      String persistJson = ApiUtils.objectToJson(persist);
      logger.info("Updating persist to:" + persistJson);
      Response response = apiResourceRootV1.getPersistResource().updatePersist(persistJson);
      handleAmbariResponse(response);
      return true;
   }

   @Override
   public ApiRequest deleteHost(String clusterName, String fqdn) throws AmbariApiException {
      logger.info("Deleting host " + fqdn + " in cluster " + clusterName);
      Response response = apiResourceRootV1.getClustersResource().getHostsResource(clusterName).deleteHost(fqdn);
      String deleteHostJson = handleAmbariResponse(response);
      return ApiUtils.jsonToObject(ApiRequest.class, deleteHostJson);
   }

   public boolean deleteBlueprint(String blueprintName) throws AmbariApiException {
      logger.info("Delete apiBlueprint " + blueprintName);
      Response response =
            apiResourceRootV1.getBlueprintsResource().deleteBlueprint(
                  blueprintName);
      handleAmbariResponse(response);
      return true;
   }

   @Override
   public boolean deleteCluster(String clusterName) throws AmbariApiException {
      logger.info("Ambari is deleting cluster " + clusterName);
      Response response =
            apiResourceRootV1.getClustersResource().deleteCluster(clusterName);
      handleAmbariResponse(response);
      return HttpStatus.isSuccess(response.getStatus());
   }

   @Override
   public ApiRequestList getRequestList(String clusterName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getRequestsResource(clusterName).readRequests();
      String requestsJson = handleAmbariResponse(response);
      logger.debug("Response of request list from ambari server:");
      logger.debug(requestsJson);
      ApiRequestList apiRequestList =
            ApiUtils.jsonToObject(ApiRequestList.class, requestsJson);
      return apiRequestList;
   }

   @Override
   public ApiRequest getRequest(String clusterName, Long requestId) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getRequestsResource(clusterName).readRequest(requestId);
      String requestJson = handleAmbariResponse(response);
      logger.debug("Response of request from ambari server:");
      logger.debug(requestJson);
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestJson);
      return apiRequest;
   }

   @Override
   public ApiBootstrap createBootstrap(ApiBootstrap bootstrap) throws AmbariApiException {
      logger.info("ApiBootstrap:");
      logger.info(ApiUtils.objectToJson(bootstrap));
      Response response =
            apiResourceRootV1.getBootstrapResource().createBootstrap(
                  ApiUtils.objectToJson(bootstrap));
      String bootstrapJson = handleAmbariResponse(response);
      logger.debug("Response of bootstrap creation from ambari server:");
      logger.debug(bootstrapJson);
      ApiBootstrap apiBootstrap =
            ApiUtils.jsonToObject(ApiBootstrap.class, bootstrapJson);
      return apiBootstrap;
   }

   @Override
   public ApiBootstrapStatus getBootstrapStatus(Long bootstrapId) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getBootstrapResource().readBootstrapStatus(
                  bootstrapId);
      String bootstrapStatusJson = handleAmbariResponse(response);
      logger.debug("Response of bootstrap status from ambari server:");
      logger.debug(bootstrapStatusJson);
      ApiBootstrapStatus apiBootstrapRequest =
            ApiUtils
                  .jsonToObject(ApiBootstrapStatus.class, bootstrapStatusJson);
      return apiBootstrapRequest;
   }

   @Override
   public ApiRequest getRequestWithTasks(String clusterName, Long requestId) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getRequestsResource(clusterName)
                  .readRequestWithTasks(requestId, "*,tasks/Tasks/*");
      String requestWithTasksJson = handleAmbariResponse(response);
      logger.debug("Response of request with tasks from ambari server:");
      logger.debug(requestWithTasksJson);
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestWithTasksJson);
      return apiRequest;
   }


   public ServiceStatus getClusterStatus(String clusterName) throws AmbariApiException {
      ApiServiceAlertList serviceList = getServicesWithAlert(clusterName);
      if (serviceList.getApiServiceAlerts() != null) {
         boolean allStopped = true;
         boolean hasStartedAlert = false;
         for (ApiServiceAlert service : serviceList.getApiServiceAlerts()) {
            ApiServiceInfo info = service.getApiServiceInfo();
            ApiAlert alert = service.getApiAlert();
            if (ApiServiceStatus.STARTED.name().equalsIgnoreCase(
                  info.getState())) {
               allStopped = false;
               if (alert != null && alert.getSummary() != null
                     && alert.getSummary().getCritical() > 0) {
                  hasStartedAlert = true;
               }
            }
         }
         if (allStopped) {
            return ServiceStatus.STOPPED;
         }
         if (hasStartedAlert) {
            return ServiceStatus.ALERT;
         }
      }
      return ServiceStatus.STARTED;
   }

   private ApiServiceAlertList getServicesWithAlert(String clusterName) throws AmbariApiException {
      String fields = "alerts/summary,ServiceInfo/state";
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getServicesResource(clusterName)
                  .readServicesWithFilter(fields);
      String servicesWithAlert = handleAmbariResponse(response);
      ApiServiceAlertList serviceList =
            ApiUtils.jsonToObject(ApiServiceAlertList.class, servicesWithAlert);
      return serviceList;
   }

   @Override
   public Map<String, ServiceStatus> getHostStatus(String clusterName) throws AmbariApiException {
      ApiHostList hostList = getHostsWithRoleState(clusterName);
      Map<String, ServiceStatus> result = new HashMap<String, ServiceStatus>();

      List<ApiHost> apiHosts = hostList.getApiHosts();
      if (apiHosts != null) {
         for (ApiHost apiHost : apiHosts) {
            String state = apiHost.getApiHostInfo().getState();
            if (ApiHostStatus.HEALTHY.name().equalsIgnoreCase(state)) {
               result.put(apiHost.getApiHostInfo().getHostName(),
                     ServiceStatus.STARTED);
            } else if (ApiHostStatus.UNHEALTHY.name().equalsIgnoreCase(state)) {
               result.put(apiHost.getApiHostInfo().getHostName(),
                     ServiceStatus.UNHEALTHY);
            } else if (ApiHostStatus.ALERT.name().equalsIgnoreCase(state)) {
               result.put(apiHost.getApiHostInfo().getHostName(),
                     ServiceStatus.ALERT);
            }
         }
      }
      return result;
   }

   @Override
   public ApiHostList getHostsSummaryInfo(String clusterName) {
      Response response = apiResourceRootV1.getClustersResource().getHostsResource(clusterName).readHosts();
      String hostListInfo = handleAmbariResponse(response);
      logger.info("All hosts in cluster " + clusterName + " is " + hostListInfo);
      return ApiUtils.jsonToObject(ApiHostList.class, hostListInfo);
   }

   private ApiHostList getHostsWithRoleState(String clusterName) throws AmbariApiException {
      String fields = "Hosts/host_status,host_components/HostRoles";
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getHostsResource(clusterName).readHostsWithFilter(fields);
      String hostsWithState = handleAmbariResponse(response);
      ApiHostList hostList =
            ApiUtils.jsonToObject(ApiHostList.class, hostsWithState);
      return hostList;
   }

   public String healthCheck() throws AmbariApiException {
      Response response = apiResourceRootV1.getHealthCheck().check();
      String healthStatus = handleAmbariResponse(response);
      return healthStatus;
   }

   @Override
   public String getVersion() throws AmbariApiException {
      Response response =
            apiResourceRootV1.getRootServicesResource()
                  .readRootServiceComponents();
      String requestJson = handleAmbariResponse(response);
      ApiRootServicesComponents apiRequest =
            ApiUtils.jsonToObject(ApiRootServicesComponents.class, requestJson);
      return apiRequest.getApiRootServicesComponent().getComponentVersion();
   }

   @Override
   public boolean deleteService(String clusterName, String serviceName) {
      logger.info("Deleting service " + serviceName + " in cluster " + clusterName);
      Response response = apiResourceRootV1.getClustersResource().getServicesResource(clusterName).deleteService(serviceName);
      handleAmbariResponse(response);
      return true;
   }

   public List<String> getExistingHosts(String clusterName, List<String> hostNames)
   throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getHostsResource(clusterName).readHosts();
      String hostList = handleAmbariResponse(response);
      ApiHostList apiHostList = ApiUtils.jsonToObject(ApiHostList.class, hostList);
      List<String> existingHosts = new ArrayList<>();
      if (apiHostList.getApiHosts() != null) {
         for (ApiHost apiHost : apiHostList.getApiHosts()) {
            if (hostNames.contains(apiHost.getApiHostInfo().getHostName())) {
               existingHosts.add(apiHost.getApiHostInfo().getHostName());
            }
         }
      }
      return existingHosts;
   }

   public void addHostsToCluster(String clusterName,
         List<String> hostNames) throws AmbariApiException {
      logger.debug("Add hosts " + hostNames + " to cluster " + clusterName);
      for (String hostName : hostNames) {
         Response response =
               apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName)
                     .addHost(hostName);
         handleAmbariResponse(response);
      }
   }

   @Override
   public ApiHostList getClusterHostsList(String clusterName) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
            .getHostsResource(clusterName).readHosts();
      String hostList = handleAmbariResponse(response);
      ApiHostList apiHostList = ApiUtils.jsonToObject(ApiHostList.class, hostList);
      return apiHostList;
   }

   public ApiRequest stopAllComponentsInHosts(String clusterName,
         List<String> hostNames) throws AmbariApiException {
      ApiHostsRequest hostsRequest = new ApiHostsRequest();
      ApiHostComponents apiComponents = new ApiHostComponents();
      hostsRequest.setBody(apiComponents);
      ApiComponentInfo hostRoles = new ApiComponentInfo();
      hostRoles.setState("INSTALLED");
      apiComponents.setHostRoles(hostRoles);
      ApiHostsRequestInfo requestInfo = new ApiHostsRequestInfo();
      hostsRequest.setRequestInfo(requestInfo);
      requestInfo.setContext("Stop Hosts components");

      StringBuilder builder = new StringBuilder();
      builder.append("HostRoles/host_name.in(");
      for (String hostName : hostNames) {
         builder.append(hostName).append(",");
      }
      builder.deleteCharAt(builder.length() - 1);
      builder.append(")");
      requestInfo.setQueryString(builder.toString());
      String startJson = ApiUtils.objectToJson(hostsRequest);
      logger.debug("Stop json: " + startJson);
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getHostComponentsResource(clusterName)
                  .operationWithFilter(startJson);
      String responseJson = handleAmbariResponse(response);
      logger.debug("in stop components, reponse is :" + responseJson);
      return ApiUtils.jsonToObject(ApiRequest.class, responseJson);
   }

   public void deleteAllComponents(String clusterName, String hostName)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
            .getHostsResource(clusterName)
            .getHostComponentsResource(hostName)
            .deleteAllComponents();
      handleAmbariResponse(response);
   }

   public List<String> getAssociatedConfigGroups(String clusterName,
         String hostName) throws AmbariApiException {
      String fields = "ConfigGroup/hosts";
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getConfigGroupsResource(clusterName)
                  .readConfigGroupsWithFields(fields);
      String strConfGroups = handleAmbariResponse(response);
      ApiConfigGroupList apiConfGroupList = ApiUtils.jsonToObject(ApiConfigGroupList.class, strConfGroups);
      List<String> result = new ArrayList<>();
      if (apiConfGroupList.getConfigGroups() == null) {
         return result;
      }
      for (ApiConfigGroup group : apiConfGroupList.getConfigGroups()) {
         List<ApiHostInfo> apiHosts = group.getApiConfigGroupInfo().getHosts();
         if (apiHosts == null) {
            continue;
         }
         if (apiHosts.size() == 1) {
            if (hostName.equals(apiHosts.get(0).getHostName())) {
               result.add(group.getApiConfigGroupInfo().getId());
            }
         }
      }
      return result;
   }

   public void deleteConfigGroup(String clusterName,
         String groupId) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getConfigGroupsResource(clusterName)
                  .deleteConfigGroup(groupId);
      handleAmbariResponse(response);
   }

   public ApiRequest startComponents(String clusterName,
         List<String> hostNames, List<String> components)
         throws AmbariApiException {
      ApiHostsRequest hostsRequest = new ApiHostsRequest();
      ApiHostComponents apiComponents = new ApiHostComponents();
      hostsRequest.setBody(apiComponents);
      ApiComponentInfo hostRoles = new ApiComponentInfo();
      hostRoles.setState("STARTED");
      apiComponents.setHostRoles(hostRoles);
      ApiHostsRequestInfo requestInfo = new ApiHostsRequestInfo();
      hostsRequest.setRequestInfo(requestInfo);
      requestInfo.setContext("Start Hosts components");

      StringBuilder builder = new StringBuilder();
      builder.append("HostRoles/host_name.in(");
      for (String hostName : hostNames) {
         builder.append(hostName).append(",");
      }
      builder.deleteCharAt(builder.length() - 1);
      builder.append(")").append("&");
      builder.append("HostRoles/component_name.in(");
      for (String component : components) {
         builder.append(component).append(",");
      }
      builder.deleteCharAt(builder.length() - 1);
      builder.append(")");
      requestInfo.setQueryString(builder.toString());
      String startJson = ApiUtils.objectToJson(hostsRequest);
      logger.debug("Start json: " + startJson);
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getHostComponentsResource(clusterName)
                  .operationWithFilter(startJson);
      String responseJson = handleAmbariResponse(response);
      logger.debug("in start components, reponse is :" + responseJson);
      return ApiUtils.jsonToObject(ApiRequest.class, responseJson);
   }

   public void createConfigGroups(String clusterName,
         List<ApiConfigGroup> configGroups) throws AmbariApiException {
      String confGroups = ApiUtils.objectToJson(configGroups);
      logger.debug("Creating config groups: " + confGroups);
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getConfigGroupsResource(clusterName)
                  .createConfigGroups(confGroups);
      handleAmbariResponse(response);
   }

   public void addComponents(String clusterName, List<String> hostNames,
         ApiHostComponents components) throws AmbariApiException {
      logger.info("Adding components to hosts: " + hostNames);
      ApiHostsRequest hostsRequest = new ApiHostsRequest();
      hostsRequest.setBody(components);
      ApiHostsRequestInfo requestInfo = new ApiHostsRequestInfo();
      hostsRequest.setRequestInfo(requestInfo);
      requestInfo.setContext("Adding components");

      StringBuilder builder = new StringBuilder();
      builder.append("Hosts/host_name.in(");
      for (String hostName : hostNames) {
         builder.append(hostName).append(",");
      }
      builder.deleteCharAt(builder.length() - 1);
      builder.append(")");
      requestInfo.setQueryString(builder.toString());
      String json = ApiUtils.objectToJson(hostsRequest);
      logger.debug("add json: " + json);
      Response response =
            apiResourceRootV1.getClustersResource()
                  .getHostsResource(clusterName)
                  .addComponentsToHosts(json);
      handleAmbariResponse(response);
   }

   public ApiRequest installComponents(String clusterName) throws AmbariApiException {
      ApiHostsRequest hostsRequest = AmUtils.createInstallComponentsRequest();
      String json = ApiUtils.objectToJson(hostsRequest);
      logger.debug("install component json: " + json);
      Response response =
            apiResourceRootV1
                  .getClustersResource()
                  .getHostComponentsResource(clusterName)
                  .operationWithFilter(json);
      String installJson = handleAmbariResponse(response);
      logger.debug("in install components, reponse is :" + installJson);
      return ApiUtils.jsonToObject(ApiRequest.class, installJson);
   }

   public ApiStackServiceList getStackWithCompAndConfigs(String stackName,
         String stackVersion) throws AmbariApiException {
      return getServicesWithFilter(stackName, stackVersion,
            "configurations/StackConfigurations,serviceComponents/StackServiceComponents");
   }

   @Override
   public ApiHostList getRegisteredHosts() throws AmbariApiException {
      Response response = apiResourceRootV1.getHostsResource().readHosts();
      String apiHostListJson = handleAmbariResponse(response);
      return ApiUtils.jsonToObject(ApiHostList.class, apiHostListJson);
   }

   private ApiStackServiceList getServicesWithFilter(String stackName,
         String stackVersion, String filter) throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .readServicesWithFilter(filter);
      String apiStackServicesWithComponentsJson =
            handleAmbariResponse(response);
      logger.trace("Response of service list with components of stack from ambari server:");
      logger.trace(apiStackServicesWithComponentsJson);
      ApiStackServiceList apiStackServices =
            ApiUtils.jsonToObject(ApiStackServiceList.class,
                  apiStackServicesWithComponentsJson);
      return apiStackServices;
   }

   private ApiStackService getServiceWithFilter(String stackName,
         String stackVersion, String serviceName, String filter)
         throws AmbariApiException {
      Response response =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .readServiceWithFilter(serviceName, filter);
      String apiStackServiceWithComponentsJson = handleAmbariResponse(response);
      logger.debug("Response of service with components of stack from ambari server:");
      logger.debug(apiStackServiceWithComponentsJson);
      ApiStackService apiStackService =
            ApiUtils.jsonToObject(ApiStackService.class,
                  apiStackServiceWithComponentsJson);
      return apiStackService;
   }

   private String handleAmbariResponse(Response response)
         throws AmbariApiException {
      String result = response.readEntity(String.class);
      int errCode = response.getStatus();
      if (!HttpStatus.isSuccess(errCode)) {
         String errMessage = null;
         if (result != null && !result.isEmpty()) {
            ApiErrorMessage apiErrorMessage = ApiUtils.jsonToObject(ApiErrorMessage.class, result);
            errMessage = "status: " + apiErrorMessage.getStatus() + ", message: " + apiErrorMessage.getMessage();
         } else {
            errMessage = "status: " + errCode + ", message: " + HttpStatus.getMessage(errCode);
         }
         throw AmbariApiException.RESPONSE_EXCEPTION(errCode, errMessage);
      }
      return result;
   }
}
