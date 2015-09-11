/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.model.ApiBody;
import com.vmware.bdd.plugin.ambari.api.model.ApiBodyHostsInfo;
import com.vmware.bdd.plugin.ambari.api.model.ApiPutRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiAlert;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceAlert;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceAlertList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiServiceStatus;
import com.vmware.bdd.plugin.ambari.api.model.cluster.request.ApiPostRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.stack2.*;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by qjin on 5/12/15.
 */
public class ApiManager_2_0_0 extends ApiManager {
   private static final Logger logger = Logger.getLogger(ApiManager.class);

   public ApiManager_2_0_0(URL baseUrl, String user, String password) {
      super(baseUrl, user, password);
   }

   @Override
   public ApiStackList getStackList() throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource().readStacks();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String stacksJson = handleAmbariResponse(response);
      logger.debug("Response of stack list from ambari server:");
      logger.debug(stacksJson);
      ApiStackList apiStackList =
            ApiUtils.jsonToObject(ApiStackList.class, stacksJson);
      return apiStackList;
   }

   @Override
   public ApiStack getStack(String stackName) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource().readStack(stackName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String stackJson = handleAmbariResponse(response);
      logger.debug("Response of stack from ambari server:");
      logger.debug(stackJson);
      ApiStack apiStack = ApiUtils.jsonToObject(ApiStack.class, stackJson);
      return apiStack;
   }

   @Override
   public ApiStackVersionList getStackVersionList(String stackName)
         throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName).readStackVersions();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .readStackVersion(stackVersion);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion).readServices();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      return getServicesWithFilter(stackName, stackVersion, "components/*,components/dependencies");
   }

   @Override
   public ApiStackService getStackServiceWithComponents(String stackName, String stackVersion, String serviceName) throws AmbariApiException {
      return getServiceWithFilter(stackName, stackVersion, serviceName, "components/*,components/dependencies");
   }

   @Override
   public ApiStackService getStackService(String stackName,
         String stackVersion, String stackServiceName)
               throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion)
               .readService(stackServiceName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion)
               .getComponentsResource(stackServiceName).readComponents();
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion)
               .getComponentsResource(stackServiceName)
               .readComponent(stackComponentName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String stackComponentJson = handleAmbariResponse(response);
      logger.debug("Response of component of service from ambari server:");
      logger.debug(stackComponentJson);
      ApiStackComponent apiServiceComponent =
            ApiUtils.jsonToObject(ApiStackComponent.class, stackComponentJson);
      return apiServiceComponent;
   }

   @Override
   public ApiStackServiceList getStackWithCompAndConfigs(String stackName,
         String stackVersion) throws AmbariApiException {
      return getServicesWithFilter(stackName, stackVersion, "configurations/StackConfigurations,components/StackServiceComponents");
   }

   @Override
   public ApiStackServiceList getServicesWithFilter(String stackName,
         String stackVersion, String filter) throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion)
               .readServicesWithFilter(filter);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
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
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion)
               .readServiceWithFilter(serviceName, filter);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String apiStackServiceWithComponentsJson = handleAmbariResponse(response);
      logger.debug("Response of service with components of stack from ambari server:");
      logger.debug(apiStackServiceWithComponentsJson);
      ApiStackService apiStackService =
            ApiUtils.jsonToObject(ApiStackService.class,
                  apiStackServiceWithComponentsJson);
      return apiStackService;
   }

   @Override
   public List<ApiConfiguration> getServiceConfiguration(String stackName,
         String stackVersion, String serviceName)
               throws AmbariApiException {
      Response response = null;
      try {
         response = apiResourceRootV1.getStacksResource()
               .getStackVersionsResource(stackName)
               .getStackServicesResource(stackVersion)
               .readServiceConfigurationWithFilter(serviceName);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String apiStackServiceWithComponentsJson = handleAmbariResponse(response);
      Gson gson = new Gson();
      List<ApiConfiguration> apiConfigurationInfo =
            gson.fromJson(apiStackServiceWithComponentsJson,
                  new TypeToken<List<ApiConfiguration>>() {
            }.getType());
      return apiConfigurationInfo;
   }

   @Override
   public ServiceStatus getClusterStatus(String clusterName, HadoopStack stack) throws AmbariApiException {
      ApiServiceAlertList serviceList = getServicesWithAlert(clusterName);
      if (serviceList.getApiServiceAlerts() != null) {
         boolean allStopped = true;
         boolean hasStartedAlert = false;
         List<String> notStartedServiceNames = new ArrayList<>();
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
            } else {
               notStartedServiceNames.add(service.getApiServiceInfo().getServiceName());
            }
         }
         if (allStopped) {
            return ServiceStatus.STOPPED;
         }
         if (notStartedServiceNames.isEmpty()) {
            if (hasStartedAlert) {
               return ServiceStatus.ALERT;
            } else {
               return ServiceStatus.STARTED;
            }
         }
         // client service will not be started at any time, so this method is to check
         // if there is non-client service stopped.
         // if yes, return service alert status
         boolean hasStoppedService =
               hasNonClientServices(stack, notStartedServiceNames);
         if (hasStoppedService) {
            return ServiceStatus.ALERT;
         } else {
            return ServiceStatus.STARTED;
         }
      }
      return ServiceStatus.UNKONWN;
   }

   @Override
   public void setClusterHostsRackInfo(String clusterName, String rackInfo, Set<String> hosts) {
      logger.info("Ambari is setting hosts rack info in cluster " + clusterName);

      if (hosts.isEmpty()) {
         return;
      }

      ApiBody body = new ApiBody();
      ApiBodyHostsInfo apiBodyHostsInfo = new ApiBodyHostsInfo();
      apiBodyHostsInfo.setRackInfo(rackInfo);
      body.setHostsInfo(apiBodyHostsInfo);

      ApiPostRequestInfo requestInfo = new ApiPostRequestInfo();
      requestInfo.setContext("Set Rack");
      String query = "Hosts/host_name.in(" + StringUtils.join(hosts, ",") + ")";
      requestInfo.setQuery(query);

      ApiPutRequest setHostsRackInfoRequest = new ApiPutRequest(requestInfo, body);
      String request = ApiUtils.objectToJson(setHostsRackInfoRequest);
      logger.info("The setting hosts rack info request content is " + request);

      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource().getHostsResource(clusterName).setRackInfo(request);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      handleAmbariResponse(response);
   }

   private ApiServiceAlertList getServicesWithAlert(String clusterName) throws AmbariApiException {
      String fields = "alerts,ServiceInfo/state";
      Response response = null;
      try {
         response = apiResourceRootV1.getClustersResource()
               .getServicesResource(clusterName)
               .readServicesWithFilter(fields);
      } catch (Exception e) {
         throw AmbariApiException.CANNOT_CONNECT_AMBARI_SERVER(e);
      }
      String servicesWithAlert = handleAmbariResponse(response);
      ApiServiceAlertList serviceList =
            ApiUtils.jsonToObject(ApiServiceAlertList.class, servicesWithAlert);
      return serviceList;
   }

}
