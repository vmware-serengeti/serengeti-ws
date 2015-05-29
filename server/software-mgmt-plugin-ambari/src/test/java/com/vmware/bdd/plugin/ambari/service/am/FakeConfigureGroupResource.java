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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ConfigGroupsResource;

public class FakeConfigureGroupResource implements ConfigGroupsResource {

   @Override
   @POST
   @Path("/")
   @Consumes("application/xml")
   @Produces({ "application/json", "application/xml", "text/plain" })
   public Response createConfigGroups(String configGroups) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   @GET
   @Path("/")
   public Response readConfigGroupsWithFields(
         @QueryParam("fields") String fields) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   public Response readConfigGroups() {
      return null;
   }

   @Override
   public Response readConfigGroup(String groupId) {
      return BuildResponse.buildResponse("clusters/config_groups/2.json");
   }

   @Override
   @DELETE
   @Path("/{ConfigGroupId}")
   public Response deleteConfigGroup(@PathParam("ConfigGroupId") String groupId) {
      return BuildResponse.buildResponse("");
   }

   @Override
   public Response updateConfigGroup(String groupId, String configGroup) {
      return BuildResponse.buildResponse("clusters/config_groups/updated_2.json");
   }
}
