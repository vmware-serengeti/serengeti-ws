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

import com.vmware.bdd.plugin.ambari.utils.Constants;

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
import com.vmware.bdd.plugin.ambari.api.model.ApiPersist;
import com.vmware.bdd.plugin.ambari.api.model.ApiPostRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiPutRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiRootServicesComponents;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.blueprint.ApiBlueprintList;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterConfigurations;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponent;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiComponentList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroup;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiConfigGroupList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRestartRequiredCompent;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRestartRequiredService;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiService;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponent;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponents;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostComponentsRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiPostRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiOperationLevel;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequestList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiRequestsPostResourceFilter;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackComponentList;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.stack2.ApiStackVersionList;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.utils.AmUtils;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;


public class ApiManager implements IApiManager {

   private static final Logger logger = Logger.getLogger(ApiManager.class);

   protected RootResourceV1 apiResourceRootV1;

   public ApiManager(String amServerHost, int port, String user, String password) {
      this(new AmbariManagerClientbuilder().withHost(amServerHost)
            .withPort(port).withUsernamePassword(user, password));
   }

   public ApiManager(URL baseUrl, String user, String password) {
      this(new AmbariManagerClientbuilder().withBaseURL(baseUrl)
            .withUsernamePassword(user, password));
   }

   public ApiManager(AmbariManagerClientbuilder clientbuilder) {
      ApiRootResource amApiRootResource = clientbuilder.build();

      apiResourceRootV1 = amApiRootResource.getRootV1();
      healthCheck();
   }

   @Override
   public ApiStackList getStackList() throws AmbariApiException {
      return null;
   }

   @Override
   public ApiStack getStack(String stackName) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiService readService(String clusterName, String serviceName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getServicesResource(clusterName).readService(serviceName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      return null;
   }

   @Override
   public ApiStackVersion getStackVersion(String stackName, String stackVersion)
         throws AmbariApiException {
      return null;
   }

   @Override
   public ApiStackServiceList getStackServiceList(String stackName,
         String stackVersion) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiStackServiceList getStackServiceListWithComponents(
         String stackName, String stackVersion) throws AmbariApiException {
      return null;
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
      return null;
   }

   @Override
   public ApiStackService getStackService(String stackName,
         String stackVersion, String stackServiceName)
         throws AmbariApiException {
      return null;
   }

   @Override
   public ApiStackComponentList getStackComponentList(String stackName,
         String stackVersion, String stackServiceName)
         throws AmbariApiException {
      return null;
   }

   @Override
   public ApiStackComponent getStackComponent(String stackName,
         String stackVersion, String stackServiceName, String stackComponentName)
         throws AmbariApiException {
      return null;
   }

   @Override
   public ApiClusterList getClusterList() throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().readClusters();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String clustersJson = handleAmbariResponse(response);
      logger.debug("Response of cluster list from ambari server:");
      logger.debug(clustersJson);
      ApiClusterList apiClusterList =
            ApiUtils.jsonToObject(ApiClusterList.class, clustersJson);
      return apiClusterList;
   }

   @Override
   public ApiCluster getCluster(String clusterName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().readCluster(clusterName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String clusterJson = handleAmbariResponse(response);
      logger.debug("Response of cluster from ambari server:");
      logger.debug(clusterJson);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      return apiCluster;
   }

   @Override
   public List<ApiService> getClusterServices(String clusterName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().readCluster(clusterName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      ApiPostRequestInfo requestInfo = new ApiPostRequestInfo();
      requestInfo.setContext("Stop All Services");
      ApiPutRequest stopRequest = new ApiPutRequest(requestInfo, body);
      String request = ApiUtils.objectToJson(stopRequest);
      logger.debug("The request in stop cluster is :" + request);

      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getServicesResource(clusterName)
                     .stopAllServices(clusterName, "true", request);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String stopServicesJson = handleAmbariResponse(response);
      logger.debug("The response when ambari stop cluster is :"
            + stopServicesJson);
      return ApiUtils.jsonToObject(ApiRequest.class, stopServicesJson);
   }

   private boolean isAmbari_1_6_0() {
      String ambariServerVersion = getVersion();
      return ambariServerVersion.equalsIgnoreCase(Constants.AMBARI_SERVER_VERSION_1_6_0);
   }

   @Override
   public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException {
      ApiServiceInfo serviceInfo = new ApiServiceInfo();
      serviceInfo.setState(ApiServiceStatus.STARTED.name());
      ApiBody body = new ApiBody();
      body.setServiceInfo(serviceInfo);
      ApiPostRequestInfo requestInfo = new ApiPostRequestInfo();
      requestInfo.setContext("Start All Services");
      if (!isAmbari_1_6_0()) {
         ApiOperationLevel operationLevel = new ApiOperationLevel(Constants.OPERATION_CLUSTER_LEVEL, clusterName);
         requestInfo.setOperationLevel(operationLevel);
      }
      ApiPutRequest startRequest = new ApiPutRequest(requestInfo, body);
      String request = ApiUtils.objectToJson(startRequest);
      logger.debug("The request in start cluster is :" + request);

      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getServicesResource(clusterName)
                     .startAllServices(clusterName, "false", request);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String startServicesJson = handleAmbariResponse(response);
      logger.debug("The reponse when startAllService is :" + startServicesJson);
      return ApiUtils.jsonToObject(ApiRequest.class, startServicesJson);
   }

   public ApiRequest decommissionComponent(String clusterName, String host, String serviceName, String managementRoleName, String slaveRoleName) {
      ApiPostRequestInfo requestInfo = new ApiPostRequestInfo();
      requestInfo.setCommand("DECOMMISSION");
      requestInfo.setContext("Decommission " + slaveRoleName);
      HashMap<String, String> parameters = new HashMap<>();
      parameters.put("slave_type", slaveRoleName);
      parameters.put("excluded_hosts", host);
      requestInfo.setParameters(parameters);

      ApiRequestsPostResourceFilter requestsResourceFilter = new ApiRequestsPostResourceFilter(serviceName, managementRoleName);
      List<ApiRequestsPostResourceFilter> requestsResourceFilters = new ArrayList<ApiRequestsPostResourceFilter>();
      requestsResourceFilters.add(requestsResourceFilter);

      ApiPostRequest decommissionComponentRequest = new ApiPostRequest(requestInfo, requestsResourceFilters);
      Response response = null;
      try {
         String request = ApiUtils.objectToJson(decommissionComponentRequest);
         logger.info("When decommission component, cmd is " + request);
         response = apiResourceRootV1.getClustersResource().getRequestsResource(clusterName).postRequest(request);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }

      String decommissionComponentJson = handleAmbariResponse(response);
      logger.info("The reponse when decommission component is :" + decommissionComponentJson);
      return ApiUtils.jsonToObject(ApiRequest.class, decommissionComponentJson);
   }

   @Override
   public List<String> getClusterServicesNames(String clusterName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().readCluster(clusterName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .createCluster(clusterName, ApiUtils.objectToJson(apiClusterBlueprint));
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String requestJson = handleAmbariResponse(response);
      logger.debug("Response of provision cluster with blueprint from ambari server:");
      logger.debug(requestJson);
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestJson);
      return apiRequest;
   }

   @Override
   public ApiBlueprintList getBlueprintList() throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getBlueprintsResource().readBlueprints();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String blueprintsJson = handleAmbariResponse(response);
      logger.debug("Response of blueprint list from ambari server:");
      logger.debug(blueprintsJson);
      ApiBlueprintList apiBlueprintList =
            ApiUtils.jsonToObject(ApiBlueprintList.class, blueprintsJson);
      return apiBlueprintList;
   }

   @Override
   public ApiBlueprint getBlueprint(String blueprintName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getBlueprintsResource().readBlueprint(blueprintName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getBlueprintsResource().createBlueprint(
               blueprintName, ApiUtils.objectToJson(apiBlueprint));
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getPersistResource().updatePersist(persistJson);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
      return true;
   }

   @Override
   public ApiRequest deleteHost(String clusterName, String fqdn) throws AmbariApiException {
      logger.info("Deleting host " + fqdn + " in cluster " + clusterName);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName).deleteHost(fqdn);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String deleteHostJson = handleAmbariResponse(response);
      return ApiUtils.jsonToObject(ApiRequest.class, deleteHostJson);
   }

   public boolean deleteBlueprint(String blueprintName) throws AmbariApiException {
      logger.info("Delete apiBlueprint " + blueprintName);
      Response response = null;
      try {
         response = apiResourceRootV1.getBlueprintsResource().deleteBlueprint(blueprintName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
      return true;
   }

   @Override
   public boolean deleteCluster(String clusterName) throws AmbariApiException {
      logger.info("Ambari is deleting cluster " + clusterName);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().deleteCluster(clusterName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
      return HttpStatus.isSuccess(response.getStatus());
   }

   @Override
   public ApiRequestList getRequestList(String clusterName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getRequestsResource(clusterName).readRequests();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String requestsJson = handleAmbariResponse(response);
      logger.debug("Response of request list from ambari server:");
      logger.debug(requestsJson);
      ApiRequestList apiRequestList =
            ApiUtils.jsonToObject(ApiRequestList.class, requestsJson);
      return apiRequestList;
   }

   @Override
   public ApiRequest getRequest(String clusterName, Long requestId) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getRequestsResource(clusterName).readRequest(requestId);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getBootstrapResource().createBootstrap(
               ApiUtils.objectToJson(bootstrap));
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String bootstrapJson = handleAmbariResponse(response);
      logger.debug("Response of bootstrap creation from ambari server:");
      logger.debug(bootstrapJson);
      ApiBootstrap apiBootstrap =
            ApiUtils.jsonToObject(ApiBootstrap.class, bootstrapJson);
      return apiBootstrap;
   }

   @Override
   public ApiBootstrapStatus getBootstrapStatus(Long bootstrapId) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getBootstrapResource().readBootstrapStatus(bootstrapId);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String bootstrapStatusJson = handleAmbariResponse(response);
      logger.debug("Response of bootstrap status from ambari server:");
      logger.debug(bootstrapStatusJson);
      ApiBootstrapStatus apiBootstrapRequest = ApiUtils.jsonToObject(ApiBootstrapStatus.class, bootstrapStatusJson);
      return apiBootstrapRequest;
   }

   @Override
   public ApiRequest getRequestWithTasks(String clusterName, Long requestId) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getRequestsResource(clusterName)
                     .readRequestWithTasks(requestId, "*,tasks/Tasks/*");
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String requestWithTasksJson = handleAmbariResponse(response);
      logger.debug("Response of request with tasks from ambari server:");
      logger.debug(requestWithTasksJson);
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestWithTasksJson);
      return apiRequest;
   }

   public ServiceStatus getClusterStatus(String clusterName, HadoopStack stack) throws AmbariApiException {
      return null;
   }

   // derect if input service names has non-client service
   protected boolean hasNonClientServices(HadoopStack stack,
         List<String> notStartedServiceNames) {
      for (String serviceName : notStartedServiceNames) {
         ApiStackService stackService =
               getStackServiceWithComponents(stack.getVendor(),
                     stack.getFullVersion(), serviceName);
         List<ApiStackComponent> components = stackService.getServiceComponents();
         boolean allClients = true;
         if (components != null) {
            for (ApiStackComponent component : components) {
               if (!component.getApiComponent().isClient()) {
                  allClients = false;
                  break;
               }
            }
         }
         if (!allClients) {
            return true;
         }
      }
      return false;
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
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName).readHosts();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String hostListInfo = handleAmbariResponse(response);
      logger.info("All hosts in cluster " + clusterName + " is " + hostListInfo);
      return ApiUtils.jsonToObject(ApiHostList.class, hostListInfo);
   }

   private ApiHostList getHostsWithRoleState(String clusterName) throws AmbariApiException {
      String fields = "Hosts/host_status,host_components/HostRoles";
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName).readHostsWithFilter(fields);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String hostsWithState = handleAmbariResponse(response);
      ApiHostList hostList =
            ApiUtils.jsonToObject(ApiHostList.class, hostsWithState);
      return hostList;
   }

   public List<ApiHostComponent> getHostComponents(String clusterName, String hostName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().getHostsResource(clusterName).getHostComponents(hostName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String hostComponents = handleAmbariResponse(response);
      logger.info("HostComponents are " + hostComponents);
      ApiHostComponents components =
            ApiUtils.jsonToObject(ApiHostComponents.class, hostComponents);
      return components.getHostComponents();
   }

   public String healthCheck() throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getHealthCheckResource().check();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String healthStatus = handleAmbariResponse(response);
      return healthStatus;
   }

   @Override
   public String getVersion() throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getRootServicesResource().readRootServiceComponents();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String requestJson = handleAmbariResponse(response);
      ApiRootServicesComponents apiRequest =
            ApiUtils.jsonToObject(ApiRootServicesComponents.class, requestJson);
      return apiRequest.getApiRootServicesComponent().getComponentVersion();
   }

   @Override
   public boolean deleteService(String clusterName, String serviceName) {
      logger.info("Deleting service " + serviceName + " in cluster " + clusterName);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getServicesResource(clusterName).deleteService(serviceName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
      return true;
   }

   public List<String> getExistingHosts(String clusterName,
         List<String> hostNames) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName).readHosts();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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

   public void addHostsToCluster(String clusterName, List<String> hostNames) throws AmbariApiException {
      logger.debug("Add hosts " + hostNames + " to cluster " + clusterName);
      for (String hostName : hostNames) {
         Response response = null;
         try {
            response = apiResourceRootV1.getClustersResource()
                        .getHostsResource(clusterName).addHost(hostName);
         } catch (Exception e) {
            throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
         }
         handleAmbariResponse(response);
      }
   }

   @Override
   public ApiHostList getClusterHostsList(String clusterName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName).readHosts();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String hostList = handleAmbariResponse(response);
      ApiHostList apiHostList =
            ApiUtils.jsonToObject(ApiHostList.class, hostList);
      return apiHostList;
   }

   public ApiRequest stopAllComponentsInHosts(String clusterName,
         List<String> hostNames) throws AmbariApiException {
      ApiHostsRequest hostsRequest = new ApiHostsRequest();
      ApiHostComponentsRequest apiComponents = new ApiHostComponentsRequest();
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
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostComponentsResource(clusterName)
                     .operationWithFilter(startJson);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String responseJson = handleAmbariResponse(response);
      logger.debug("in stop components, reponse is :" + responseJson);
      return ApiUtils.jsonToObject(ApiRequest.class, responseJson);
   }

   public void deleteAllComponents(String clusterName, String hostName)
         throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName)
                     .deleteHostComponentsResource(hostName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
   }

   public List<String> getAssociatedConfigGroups(String clusterName,
         String hostName) throws AmbariApiException {
      String fields = "ConfigGroup/hosts";
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getConfigGroupsResource(clusterName)
                     .readConfigGroupsWithFields(fields);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String strConfGroups = handleAmbariResponse(response);
      ApiConfigGroupList apiConfGroupList =
            ApiUtils.jsonToObject(ApiConfigGroupList.class, strConfGroups);
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
   
   public ApiConfigGroupList getConfigGroupsList(String clusterName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getConfigGroupsResource(clusterName)
                     .readConfigGroupsWithFields("*");
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String strConfGroups = handleAmbariResponse(response);
      ApiConfigGroupList apiConfGroupList = ApiUtils.jsonToObject(ApiConfigGroupList.class, strConfGroups);
      return apiConfGroupList;
   }

   public void deleteConfigGroup(String clusterName, String groupId)
         throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getConfigGroupsResource(clusterName)
                     .deleteConfigGroup(groupId);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
   }

   public ApiClusterConfigurations getClusterConfigurationsWithTypeAndTag(String clusterName, String type, String tag) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().getConfigurationsResource(clusterName).readConfigurationsWithTypeAndTag(type, tag);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String strConfigurations = handleAmbariResponse(response);
      ApiClusterConfigurations apiClusterConfigurations = ApiUtils.jsonToObject(ApiClusterConfigurations.class, strConfigurations);
      return apiClusterConfigurations;
   }

   public ApiRequest startComponents(String clusterName,
         List<String> hostNames, List<String> components)
         throws AmbariApiException {
      ApiHostsRequest hostsRequest = new ApiHostsRequest();
      ApiHostComponentsRequest apiComponents = new ApiHostComponentsRequest();
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
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostComponentsResource(clusterName)
                     .operationWithFilter(startJson);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String responseJson = handleAmbariResponse(response);
      logger.debug("in start components, reponse is :" + responseJson);
      return ApiUtils.jsonToObject(ApiRequest.class, responseJson);
   }

   public void createConfigGroups(String clusterName,
         List<ApiConfigGroup> configGroups) throws AmbariApiException {
      String confGroups = ApiUtils.objectToJson(configGroups);
      logger.debug("Creating config groups: " + confGroups);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getConfigGroupsResource(clusterName)
                     .createConfigGroups(confGroups);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
   }

   public ApiConfigGroup readConfigGroup(String clusterName, String configGroupId) {
      logger.debug("Updating config group: " + configGroupId);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
               .getConfigGroupsResource(clusterName)
               .readConfigGroup(configGroupId);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      return ApiUtils.jsonToObject(ApiConfigGroup.class, handleAmbariResponse(response));
   }

   public void updateConfigGroup(String clusterName, String groupId,
                                  ApiConfigGroup configGroup) throws AmbariApiException {
      String confGroup = ApiUtils.objectToJson(configGroup);
      logger.debug("Updating config group: " + confGroup);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
               .getConfigGroupsResource(clusterName)
               .updateConfigGroup(groupId, confGroup);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
   }

   public void addComponents(String clusterName, List<String> hostNames,
         ApiHostComponentsRequest components) throws AmbariApiException {
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
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostsResource(clusterName).addComponentsToHosts(json);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
   }

   public ApiRequest installComponents(String clusterName) throws AmbariApiException {
      ApiHostsRequest hostsRequest = AmUtils.createInstallComponentsRequest();
      String json = ApiUtils.objectToJson(hostsRequest);
      logger.debug("install component json: " + json);
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
                     .getHostComponentsResource(clusterName)
                     .operationWithFilter(json);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String installJson = handleAmbariResponse(response);
      logger.debug("in install components, reponse is :" + installJson);
      return ApiUtils.jsonToObject(ApiRequest.class, installJson);
   }

   public ApiStackServiceList getStackWithCompAndConfigs(String stackName,
         String stackVersion) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiHostList getRegisteredHosts() throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getHostsResource().readHosts();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String apiHostListJson = handleAmbariResponse(response);
      return ApiUtils.jsonToObject(ApiHostList.class, apiHostListJson);
   }

   @Override
   public ApiStackServiceList getServicesWithFilter(String stackName,
         String stackVersion, String filter) throws AmbariApiException {
      return null;
   }

   @Override
   public List<ApiRequest> restartRequiredServices(String clusterName) throws AmbariApiException {
      List<ApiRequest> restartServicesApiRequests = new ArrayList<ApiRequest>();

      List<ApiRestartRequiredService> apiRestartRequiredServices = getRestartRequiredServices(clusterName);
      for (ApiRestartRequiredService apiRestartRequiredService : apiRestartRequiredServices) {
         restartServicesApiRequests.add(restartRequiredService(clusterName, apiRestartRequiredService));
      }

      return restartServicesApiRequests;
   }

   @Override
   public ApiRequest restartRequiredService(String clusterName, ApiRestartRequiredService apiRestartRequiredService) throws AmbariApiException {
      String serviceName = apiRestartRequiredService.getName();
      ApiPostRequestInfo requestInfo = new ApiPostRequestInfo();
      requestInfo.setContext("Restart all components with Stale Configs for " + serviceName);
      requestInfo.setCommand(Constants.OPERATION_COMMAND_RESTART);
      if (!isAmbari_1_6_0()) {
         ApiOperationLevel operationLevel = new ApiOperationLevel(Constants.OPERATION_SERVICE_LEVEL, clusterName, serviceName);
         requestInfo.setOperationLevel(operationLevel);
      }
      List<ApiRequestsPostResourceFilter> apiRequestsResourceFilters = new ArrayList<ApiRequestsPostResourceFilter>();
      for (ApiRestartRequiredCompent apiRestartRequiredCompent : apiRestartRequiredService.getApiRestartRequiredCompents()) {
         ApiRequestsPostResourceFilter apiRequestResourceFilter = new ApiRequestsPostResourceFilter(serviceName, apiRestartRequiredCompent.getName(), apiRestartRequiredCompent.getStringHosts());
         apiRequestsResourceFilters.add(apiRequestResourceFilter);
      }
      ApiPostRequest restartServiceRequest = new ApiPostRequest(requestInfo, apiRequestsResourceFilters);
      String request = ApiUtils.objectToJson(restartServiceRequest);
      logger.info("The request in restarting service is :" + request);

      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().getRequestsResource(clusterName).postRequest(request);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String responseJson = handleAmbariResponse(response);

      logger.info("in restarting service, reponse is :" + responseJson);
      return ApiUtils.jsonToObject(ApiRequest.class, responseJson);
   }

   @Override
   public List<ApiRestartRequiredService> getRestartRequiredServices(String clusterName) throws AmbariApiException {
      Map<String, ApiRestartRequiredService> apiRestartRequiredServicesMap = new HashMap<String, ApiRestartRequiredService>();
      Response response = null;
      try {
         String fields = "HostRoles/service_name,HostRoles/state,HostRoles/host_name,HostRoles/stale_configs,&minimal_response=true";
         String staleConfigs = "true";
         response = apiResourceRootV1.getClustersResource().getHostComponentsResource(clusterName).readComponentsAfterConfigChange(fields, staleConfigs);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String apiComponentListJson = handleAmbariResponse(response);
      ApiComponentList apiComponentList = ApiUtils.jsonToObject(ApiComponentList.class, apiComponentListJson);
      for (ApiComponent apiComponent : apiComponentList.getComponents()) {
         ApiComponentInfo apiComponentInfo = apiComponent.getHostComponent();
         String serviceName = apiComponentInfo.getServiceName();

         ApiRestartRequiredService apiRestartRequiredService = new ApiRestartRequiredService();
         if (apiRestartRequiredServicesMap.containsKey(serviceName)) {
            apiRestartRequiredService = apiRestartRequiredServicesMap.get(serviceName);
         }
         apiRestartRequiredService.setName(serviceName);

         String componentName = apiComponentInfo.getComponentName();
         String host = apiComponentInfo.getHostName();

         List<ApiRestartRequiredCompent> apiRestartRequiredCompents = apiRestartRequiredService.getApiRestartRequiredCompents();
         if (apiRestartRequiredCompents == null) {
            apiRestartRequiredCompents = new ArrayList<ApiRestartRequiredCompent> ();
         }

         boolean isExisted = false;
         for (ApiRestartRequiredCompent apiRestartRequiredCompent : apiRestartRequiredCompents) {
            if (apiRestartRequiredCompent.getName().equals(componentName)) {
               apiRestartRequiredCompent.addHost(host);
               isExisted = true;
               break;
            }
         }

         if(!isExisted) {
            ApiRestartRequiredCompent apiRestartRequiredCompent = new ApiRestartRequiredCompent();
            apiRestartRequiredCompent.setName(componentName);
            apiRestartRequiredCompent.addHost(host);
            apiRestartRequiredCompents.add(apiRestartRequiredCompent);
         }

         apiRestartRequiredService.setApiRestartRequiredCompents(apiRestartRequiredCompents);
         apiRestartRequiredServicesMap.put(serviceName, apiRestartRequiredService);
      }

      List<ApiRestartRequiredService> apiRestartRequiredServices = new ArrayList<ApiRestartRequiredService>();
      apiRestartRequiredServices.addAll(apiRestartRequiredServicesMap.values());

      return apiRestartRequiredServices;
   }

   protected String handleAmbariResponse(Response response)
         throws AmbariApiException {
      String result = response.readEntity(String.class);
      int errCode = response.getStatus();
      if (!HttpStatus.isSuccess(errCode)) {
         String errMessage = null;
         if (result != null && !result.isEmpty()) {
            ApiErrorMessage apiErrorMessage = ApiUtils.jsonToObject(ApiErrorMessage.class, result);
            errMessage = apiErrorMessage.getStatus() + " " + apiErrorMessage.getMessage();
         } else {
            errMessage = errCode + " " + HttpStatus.getMessage(errCode);
         }
         throw AmbariApiException.RESPONSE_EXCEPTION(errCode, errMessage);
      }
      return result;
   }

}
