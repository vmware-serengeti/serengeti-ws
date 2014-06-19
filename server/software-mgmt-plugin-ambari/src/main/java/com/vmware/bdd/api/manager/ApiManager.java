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
package com.vmware.bdd.api.manager;

import com.vmware.bdd.api.AmbariManagerClientbuilder;
import com.vmware.bdd.api.ApiRootResource;
import com.vmware.bdd.api.manager.intf.IApiManager;
import com.vmware.bdd.api.model.ApiBlueprint;
import com.vmware.bdd.api.model.ApiBlueprintList;
import com.vmware.bdd.api.model.ApiBootstrap;
import com.vmware.bdd.api.model.ApiBootstrapStatus;
import com.vmware.bdd.api.model.ApiCluster;
import com.vmware.bdd.api.model.ApiClusterList;
import com.vmware.bdd.api.model.ApiRequestList;
import com.vmware.bdd.api.utils.ApiToObject;
import com.vmware.bdd.api.v1.RootResourceV1;

public class ApiManager implements IApiManager {

   private RootResourceV1 apiResourceRootV1;

   public ApiManager(String amServerHost, int port, String user, String password) {
      ApiRootResource amApiRootResource =
            new AmbariManagerClientbuilder().withHost(amServerHost)
                  .withPort(port).withUsernamePassword(user, password).build();

      this.apiResourceRootV1 = amApiRootResource.getRootV1();
   }

   @Override
   public ApiClusterList clusterList() {
      String clustersJson = this.apiResourceRootV1.getClustersResource().readClusters();
      ApiClusterList apiClusterList = ApiToObject.toObject(ApiClusterList.class, clustersJson);
      return apiClusterList;
   }

   @Override
   public ApiCluster cluster(String clusterName){
      String clusterJson = this.apiResourceRootV1.getClustersResource().readCluster(clusterName);
      ApiCluster apiCluster = ApiToObject.toObject(ApiCluster.class, clusterJson);
      return apiCluster;
   }

   @Override
   public ApiBlueprintList blueprintList() {
      String blueprintsJson = this.apiResourceRootV1.getBlueprintsResource().readBlueprints();
      ApiBlueprintList apiBlueprintList = ApiToObject.toObject(ApiBlueprintList.class, blueprintsJson);
      return apiBlueprintList;
   }

   @Override
   public ApiBlueprint createBlueprint(String blueprintName, String blueprint) {
      String blueprintJson = this.apiResourceRootV1.getBlueprintsResource().createBlueprint(blueprintName, blueprint);
      ApiBlueprint apiBlueprint = ApiToObject.toObject(ApiBlueprint.class, blueprintJson);
      return apiBlueprint;
   }

   @Override
   public ApiRequestList requestList(String clusterName) {
      String requestJson = this.apiResourceRootV1.getClustersResource().getRequestsResource(clusterName).readRequests();
      ApiRequestList apiRequestList = ApiToObject.toObject(ApiRequestList.class, requestJson);
      return apiRequestList;
   }

   @Override
   public ApiBootstrap createBootstrap(String bootstrap) {
      String bootstrapJson = this.apiResourceRootV1.getBootstrapResource().createBootstrap(bootstrap);
      ApiBootstrap apiBootstrap = ApiToObject.toObject(ApiBootstrap.class, bootstrapJson);
      return apiBootstrap;
   }

   @Override
   public ApiBootstrapStatus bootstrapStatus(Long bootstrapId) {
      String bootstrapStatusJson = this.apiResourceRootV1.getBootstrapResource().readBootstrapStatus(bootstrapId);
      ApiBootstrapStatus apiBootstrapRequest = ApiToObject.toObject(ApiBootstrapStatus.class, bootstrapStatusJson);
      return apiBootstrapRequest;
   }

}
