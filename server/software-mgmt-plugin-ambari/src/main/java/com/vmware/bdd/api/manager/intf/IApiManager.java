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
package com.vmware.bdd.api.manager.intf;

import com.vmware.bdd.api.model.ApiBlueprint;
import com.vmware.bdd.api.model.ApiBlueprintList;
import com.vmware.bdd.api.model.ApiBootstrap;
import com.vmware.bdd.api.model.ApiBootstrapStatus;
import com.vmware.bdd.api.model.ApiCluster;
import com.vmware.bdd.api.model.ApiClusterList;
import com.vmware.bdd.api.model.ApiRequestList;

public interface IApiManager {

   public ApiClusterList clusterList();

   public ApiCluster cluster(String clusterName);

   public ApiBlueprintList blueprintList();

   public ApiRequestList requestList(String clusterName);

   public ApiBootstrap createBootstrap(String bootstrap);

   public ApiBootstrapStatus bootstrapStatus(Long bootstrapId);

   public ApiBlueprint createBlueprint(String blueprintName, String blueprint);

}
