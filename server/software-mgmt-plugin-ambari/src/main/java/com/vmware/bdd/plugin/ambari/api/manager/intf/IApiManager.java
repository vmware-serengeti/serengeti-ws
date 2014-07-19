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

import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiBlueprintList;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.model.ApiBootstrapStatus;
import com.vmware.bdd.plugin.ambari.api.model.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.ApiClusterBlueprint;
import com.vmware.bdd.plugin.ambari.api.model.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.ApiHostList;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.ApiRequestList;
import com.vmware.bdd.plugin.ambari.api.model.ApiService;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStack;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackComponentList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackService;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackServiceComponent;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackServiceList;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersion;
import com.vmware.bdd.plugin.ambari.api.model.stack.ApiStackVersionList;
import com.vmware.bdd.software.mgmt.plugin.monitor.ServiceStatus;

public interface IApiManager {

   public ApiStackList stackList();

   public ApiStack stack(String stackName);

   public ApiStackVersionList stackVersionList(String stackName);

   public ApiStackVersion stackVersion(String stackName, String stackVersion);

   public ApiStackServiceList stackServiceList(String stackName,
         String stackVersion);

   public ApiStackServiceList stackServiceListWithComponents(String stackName,
         String stackVersion);

   public ApiStackServiceList stackServiceListWithConfigurations(String stackName,
         String stackVersion);

   public ApiStackService stackService(String stackName, String stackVersion,
         String stackServiceName);

   public ApiStackComponentList stackComponentList(String stackName,
         String stackVersion, String stackServiceName);

   public ApiStackServiceComponent stackComponent(String stackName, String stackVersion,
         String stackServiceName, String stackComponentName);

   public ApiClusterList clusterList();

   public ApiCluster cluster(String clusterName);

   public List<ApiService> clusterServices(String clusterName);

   public ApiRequest stopAllServicesInCluster(String clusterName);

   public ApiRequest startAllServicesInCluster(String clusterName);

   public List<String> getClusterServicesNames(String clusterName);

   public ApiRequest provisionCluster(String clusterName,
         ApiClusterBlueprint apiClusterBlueprint);

   public ApiBlueprintList blueprintList();

   public ApiBlueprint getBlueprint(String blueprintName);

   public ApiRequest deleteBlueprint(String blueprintName);

   public ApiRequestList requestList(String clusterName);

   public ApiRequest request(String clusterName, Long requestId);

   public ApiBootstrap createBootstrap(ApiBootstrap bootstrap);

   public ApiBootstrapStatus bootstrapStatus(Long bootstrapId);

   public ApiBlueprint createBlueprint(String blueprintName,
         ApiBlueprint blueprint);

   public ApiRequest deleteCluster(String clusterName);

   public ApiRequest requestWithTasks(String clusterName, Long requestId);

   public ServiceStatus getClusterStatus(String clusterName);

   public Map<String, ServiceStatus> getHostStatus(String clusterName);

   ApiHostList getHostsSummaryInfo(String clusterName);

   public String healthCheck();

   public String version();

   public ApiRequest deleteService(String clusterName, String serviceName);

   public ApiRequest deleteHost(String clusterName, String fqdn);
}
