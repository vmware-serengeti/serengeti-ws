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

import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprintList;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.ApiComponent;
import com.vmware.bdd.plugin.ambari.api.model.ApiComponentList;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequestList;
import com.vmware.bdd.plugin.ambari.api.model.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.ApiStackVersionList;
import com.vmware.bdd.plugin.ambari.api.model.ApiService;
import org.apache.log4j.Logger;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.ApiRootResource;
import com.vmware.bdd.plugin.ambari.api.manager.intf.IApiManager;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApiManager implements IApiManager {

   private static final Logger logger = Logger.getLogger(ApiManager.class);

   private RootResourceV1 apiResourceRootV1;

   public ApiManager(String amServerHost, int port, String user, String password) {
      ApiRootResource amApiRootResource =
            new AmbariManagerClientbuilder().withHost(amServerHost)
                  .withPort(port).withUsernamePassword(user, password).build();

      apiResourceRootV1 = amApiRootResource.getRootV1();
   }

   @Override
   public ApiStackList stackList() {
      String stacksJson = apiResourceRootV1.getStacks2Resource().readStacks();
      ApiStackList apiStackList =
            ApiUtils.jsonToObject(ApiStackList.class, stacksJson);
      return apiStackList;
   }

   @Override
   public ApiStack stack(String stackName) {
      String stackJson =
            apiResourceRootV1.getStacks2Resource().readStack(stackName);
      ApiStack apiStack = ApiUtils.jsonToObject(ApiStack.class, stackJson);
      return apiStack;
   }

   @Override
   public ApiStackVersionList stackVersionList(String stackName) {
      String stackVersionsJson =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName).readStackVersions();
      ApiStackVersionList apiStackVersionList =
            ApiUtils.jsonToObject(ApiStackVersionList.class, stackVersionsJson);
      return apiStackVersionList;
   }

   @Override
   public ApiStackVersion stackVersion(String stackName, String stackVersion) {
      String stackVersionJson =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .readStackVersion(stackVersion);
      ApiStackVersion apiStackVersion =
            ApiUtils.jsonToObject(ApiStackVersion.class, stackVersionJson);
      return apiStackVersion;
   }

   @Override
   public ApiStackServiceList stackServiceList(String stackName,
         String stackVersion) {
      String apiStackServicesJson =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion).readStackServices();
      ApiStackServiceList apiStackServices =
            ApiUtils.jsonToObject(ApiStackServiceList.class,
                  apiStackServicesJson);
      return apiStackServices;
   }

   @Override
   public ApiStackService stackService(String stackName, String stackVersion,
         String stackServiceName) {
      String apiStackServiceJson =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .readStackService(stackServiceName);
      ApiStackService apiStackService =
            ApiUtils.jsonToObject(ApiStackService.class, apiStackServiceJson);
      return apiStackService;
   }

   @Override
   public ApiComponentList serviceComponentList(String stackName,
         String stackVersion, String stackServiceName) {
      String serviceComponentsJson =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .getServiceComponentsResource(stackServiceName)
                  .readServiceComponents();
      ApiComponentList apiServiceComponents =
            ApiUtils
                  .jsonToObject(ApiComponentList.class, serviceComponentsJson);
      return apiServiceComponents;
   }

   @Override
   public ApiComponent serviceComponent(String stackName, String stackVersion,
         String stackServiceName, String serviceComponentName) {
      String serviceComponentJson =
            apiResourceRootV1.getStacks2Resource()
                  .getStackVersionsResource(stackName)
                  .getStackServicesResource(stackVersion)
                  .getServiceComponentsResource(stackServiceName)
                  .readServiceComponent(serviceComponentName);
      ApiComponent apiServiceComponent =
            ApiUtils.jsonToObject(ApiComponent.class, serviceComponentJson);
      return apiServiceComponent;
   }

   @Override
   public ApiClusterList clusterList() {
      String clustersJson =
            apiResourceRootV1.getClustersResource().readClusters();
      ApiClusterList apiClusterList =
            ApiUtils.jsonToObject(ApiClusterList.class, clustersJson);
      return apiClusterList;
   }

   @Override
   public ApiCluster cluster(String clusterName) {
      String clusterJson =
            apiResourceRootV1.getClustersResource().readCluster(clusterName);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      return apiCluster;
   }

   @Override
   public List<ApiService> clusterServices(String clusterName) {
      String clusterJson =
            apiResourceRootV1.getClustersResource().readCluster(clusterName);
      logger.info("in getClusterServicesNames, cluster info is " + clusterJson);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      return apiCluster.getApiServices();
   }

   /*
    * Only used to make request for stop/start services in cluster
    */
   private String makeRequestForStartStopClusterServices(String queryState, String context, String state) {
      assert(queryState != null && context != null && state != null);

      HashMap<String, String> requestInfo = new HashMap();
      requestInfo.put("query", "ServiceInfo/" + queryState);
      requestInfo.put("context", context);

      HashMap<String, HashMap> body = new HashMap();
      HashMap<String, String> serviceInfo = new HashMap();
      serviceInfo.put("state", state);
      body.put("ServiceInfo", serviceInfo);

      HashMap<String, HashMap> request = new HashMap();
      request.put("RequestInfo", requestInfo);
      request.put("Body", body);
      return ApiUtils.objectToJson(request);
   }

   @Override
   public ApiRequest stopAllServicesInCluster(String clusterName) {
      String request = makeRequestForStartStopClusterServices("state!=INSTALLED", "Ambari is stopping all services", "INSTALLED");
      logger.info("the request in stop cluster is :" + request);

      String responseJson = apiResourceRootV1.getClustersResource().getServicesResource(clusterName).stopAllServices(clusterName, request);
      return ApiUtils.jsonToObject(ApiRequest.class, responseJson);
   }

   @Override
   public ApiRequest startAllServicesInCluster(String clusterName) {
      String requestJson = makeRequestForStartStopClusterServices("state!=STARTED", "Ambari is starting all services", "STARTED");
      logger.info("the request in start cluster is :" + requestJson);

      String response = apiResourceRootV1.getClustersResource().getServicesResource(clusterName).startAllServices(clusterName, requestJson);
      logger.info("The reponse when startAllService is :" + response);
      return ApiUtils.jsonToObject(ApiRequest.class, response);
   }

   @Override
   public List<String> getClusterServicesNames(String clusterName) {
      String clusterJson =
            apiResourceRootV1.getClustersResource().readCluster(clusterName);
      logger.info("in getClusterServicesNames, cluster info is " + clusterJson);
      ApiCluster apiCluster =
            ApiUtils.jsonToObject(ApiCluster.class, clusterJson);
      List<ApiService> apiServices = apiCluster.getApiServices();
      List<String> servicesNames = null;
      for (ApiService apiService : apiServices) {
         if (apiService != null) {
            HashMap<String, String> serviceInfo = apiService.getServiceInfo();
            String serviceName = serviceInfo.get("service_name");
            if (serviceInfo != null && serviceName != null) {
               if (servicesNames == null) {
                  servicesNames = new ArrayList<String>();
               }
               servicesNames.add(serviceName);
            }
         }
      }
      return servicesNames;
   }

   @Override
   public ApiRequest provisionCluster(String clusterName,
         ApiClusterBlueprint apiClusterBlueprint) {
      String requestJson =
            apiResourceRootV1.getClustersResource().createCluster(clusterName,
                  ApiUtils.objectToJson(apiClusterBlueprint));
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestJson);
      return apiRequest;
   }

   @Override
   public ApiBlueprintList blueprintList() {
      String blueprintsJson =
            apiResourceRootV1.getBlueprintsResource().readBlueprints();
      ApiBlueprintList apiBlueprintList =
            ApiUtils.jsonToObject(ApiBlueprintList.class, blueprintsJson);
      return apiBlueprintList;
   }

   @Override
   public ApiBlueprint createBlueprint(String blueprintName,
         ApiBlueprint apiBlueprint) {
      String blueprintJson =
            apiResourceRootV1.getBlueprintsResource().createBlueprint(
                  blueprintName, ApiUtils.objectToJson(apiBlueprint));
      ApiBlueprint apiBlueprintResult =
            ApiUtils.jsonToObject(ApiBlueprint.class, blueprintJson);
      return apiBlueprintResult;
   }

   @Override
   public ApiRequest deleteCluster(String clusterName) {
      String response = apiResourceRootV1.getClustersResource().deleteCluster(clusterName);
      logger.info("in delete cluster, reponse is :" + response);
      return ApiUtils.jsonToObject(ApiRequest.class, response);
   }

   @Override
   public ApiRequestList requestList(String clusterName) {
      String requestsJson =
            apiResourceRootV1.getClustersResource()
                  .getRequestsResource(clusterName).readRequests();
      ApiRequestList apiRequestList =
            ApiUtils.jsonToObject(ApiRequestList.class, requestsJson);
      return apiRequestList;
   }

   @Override
   public ApiRequest request(String clusterName, Long requestId) {
      String requestJson =
            apiResourceRootV1.getClustersResource()
                  .getRequestsResource(clusterName).readRequest(requestId);
      ApiRequest apiRequest =
            ApiUtils.jsonToObject(ApiRequest.class, requestJson);
      return apiRequest;
   }

   @Override
   public ApiBootstrap createBootstrap(ApiBootstrap bootstrap) {
      String bootstrapJson =
            apiResourceRootV1.getBootstrapResource().createBootstrap(
                  ApiUtils.objectToJson(bootstrap));
      ApiBootstrap apiBootstrap =
            ApiUtils.jsonToObject(ApiBootstrap.class, bootstrapJson);
      return apiBootstrap;
   }

   @Override
   public ApiBootstrapStatus bootstrapStatus(Long bootstrapId) {
      String bootstrapStatusJson =
            apiResourceRootV1.getBootstrapResource().readBootstrapStatus(
                  bootstrapId);
      ApiBootstrapStatus apiBootstrapRequest =
            ApiUtils
                  .jsonToObject(ApiBootstrapStatus.class, bootstrapStatusJson);
      return apiBootstrapRequest;
   }

}
