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

import com.vmware.bdd.plugin.ambari.api.model.bootstrap.ApiBootstrap;
import com.vmware.bdd.plugin.ambari.api.utils.ApiUtils;
import com.vmware.bdd.plugin.ambari.api.v1.BootstrapResource;

public class FakeBootstrapResource implements BootstrapResource {

   @Override
   public Response createBootstrap(String bootstrap) {
      ApiBootstrap apiBootstrap = ApiUtils.jsonToObject(ApiBootstrap.class, bootstrap);
      if (apiBootstrap.getHosts().size() == 2) {
         return BuildResponse.buildResponse("clusters/simple_hosts_bootstrap.json");
      } else {
         return BuildResponse.buildResponse("clusters/simple_bootstrap.json");
      }
   }

   @Override
   public Response readBootstrapStatus(Long bootstrapId) {
      if (bootstrapId != null && bootstrapId == 10) {
         return BuildResponse.buildResponse("clusters/simple_hosts_bootstrap.json");
      }
      return BuildResponse.buildResponse("clusters/simple_bootstrap.json");
   }

}
