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

import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ClustersResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ConfigGroupsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ConfigurationsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostComponentsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.RequestsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ServicesResource;

public class FakeClustersResource implements ClustersResource {

   @Override
   public Response readClusters() {
      return BuildResponse.buildResponse("clusters/simple_clusters.json");
   }

   @Override
   public Response readCluster(String clusterName) {
      return BuildResponse.buildResponse("clusters/simple_cluster01.json");
   }

   @Override
   public Response createCluster(String clusterName, String clusterBlueprint) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   public Response deleteCluster(String clusterName) {
      return null;
   }

   @Override
   public RequestsResource getRequestsResource(String clusterName) {
      return new FakeRequestsResource();
   }

   @Override
   public ServicesResource getServicesResource(String clusterName) {
      return new FakeServicesResource(clusterName);
   }

   @Override
   public HostsResource getHostsResource(String clusterName) {
      return new FakeClusterHostsResource(clusterName);
   }

   @Override
   public HostComponentsResource getHostComponentsResource(String clusterName) {
      return new FakeHostComponentsResource();
   }

   @Override
   public ConfigGroupsResource getConfigGroupsResource(String clusterName) {
      return new FakeConfigureGroupResource();
   }

   @Override
   public ConfigurationsResource getConfigurationsResource(String clusterName) {
      return new FakeConfigurationsResource();
   }

}
