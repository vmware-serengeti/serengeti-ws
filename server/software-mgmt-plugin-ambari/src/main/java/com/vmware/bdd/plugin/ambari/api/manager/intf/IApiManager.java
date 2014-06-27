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

public interface IApiManager {

   public ApiStackList stackList();

   public ApiStack stack(String stackName);

   public ApiStackVersionList stackVersionList(String stackName);

   public ApiStackVersion stackVersion(String stackName, String stackVersion);

   public ApiStackServiceList stackServiceList(String stackName,
         String stackVersion);

   public ApiStackService stackService(String stackName, String stackVersion,
         String stackServiceName);

   public ApiComponentList serviceComponentList(String stackName,
         String stackVersion, String stackServiceName);

   public ApiComponent serviceComponent(String stackName,
         String stackVersion, String stackServiceName,
         String serviceComponentName);

   public ApiClusterList clusterList();

   public ApiCluster cluster(String clusterName);

   public ApiRequest provisionCluster(String clusterName, ApiClusterBlueprint apiClusterBlueprint);

   public ApiBlueprintList blueprintList();

   public ApiRequestList requestList(String clusterName);

   public ApiRequest request(String clusterName, Long requestId);

   public ApiBootstrap createBootstrap(ApiBootstrap bootstrap);

   public ApiBootstrapStatus bootstrapStatus(Long bootstrapId);

   public ApiBlueprint createBlueprint(String blueprintName, ApiBlueprint blueprint);

}
