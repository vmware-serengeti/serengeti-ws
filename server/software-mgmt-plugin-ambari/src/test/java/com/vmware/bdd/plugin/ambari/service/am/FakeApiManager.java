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
package com.vmware.bdd.plugin.ambari.service.am;

import com.vmware.bdd.plugin.ambari.api.AmbariManagerClientbuilder;
import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.manager.ApiManager;
import com.vmware.bdd.plugin.ambari.api.model.ApiPersist;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiCluster;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiClusterList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHost;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiHostList;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequest;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiRequestInfo;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTask;
import com.vmware.bdd.plugin.ambari.api.model.cluster.ApiTaskInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qjin on 8/27/14.
 */
public class FakeApiManager extends ApiManager {

   public FakeApiManager(AmbariManagerClientbuilder clientbuilder) {
      super(clientbuilder);
   }

   @Override
   public ApiRequest startAllServicesInCluster(String clusterName) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiRequest stopAllServicesInCluster(String clusterName) throws AmbariApiException {
      return null;
   }

   @Override
   public ApiClusterList getClusterList() {
      return new ApiClusterList();
   }

   @Override
   public ApiCluster getCluster(String clusterName) {
      return null;
   }

   @Override
   public boolean deleteService(String clusterName, String serviceName) {
      return true;
   }

   @Override
   public List<String> getClusterServicesNames(String clusterName) throws AmbariApiException {
      List<String> services = new ArrayList<String>();
      services.add("HDFS");
      return services;
   }

   @Override
   public ApiHostList getHostsSummaryInfo(String clusterName) {
      ApiHostList apiHostList = new ApiHostList();
      List<ApiHost> apiHosts = new ArrayList<>();
      ApiHost host = new ApiHost();
      ApiHostInfo hostInfo = new ApiHostInfo();
      hostInfo.setHostName("test_host");
      host.setApiHostInfo(hostInfo);
      apiHosts.add(host);
      apiHostList.setApiHosts(apiHosts);
      return apiHostList;
   }

   @Override
   public ApiRequest deleteHost(String clusterName, String fqdn) throws AmbariApiException {
      return new ApiRequest();
   }

   @Override
   public boolean deleteCluster(String clusterName) throws AmbariApiException {
      return true;
   }

   @Override
   public boolean updatePersist(ApiPersist persist) throws AmbariApiException {
      return true;
   }

   @Override
   public ApiRequest getRequestWithTasks(String clusterName, Long requestId) throws AmbariApiException {
      ApiRequest apiRequest = new ApiRequest();
      List<ApiTask> apiTasks = new ArrayList<>();
      ApiTask apiTask = new ApiTask();
      ApiTaskInfo taskInfo = new ApiTaskInfo();
      taskInfo.setStatus("FAILED");
      taskInfo.setHostName("host01");
      apiTask.setApiTaskInfo(taskInfo);
      apiTasks.add(apiTask);
      apiRequest.setApiTasks(apiTasks);
      ApiRequestInfo apiRequestInfo = new ApiRequestInfo();
      apiRequestInfo.setRequestStatus("FAILED");
      apiRequest.setApiRequestInfo(apiRequestInfo);
      return  apiRequest;
   }

}
