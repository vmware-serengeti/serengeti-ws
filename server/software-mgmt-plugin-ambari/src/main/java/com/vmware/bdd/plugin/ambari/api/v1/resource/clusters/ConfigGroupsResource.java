/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.v1.resource.clusters;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.Parameters;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
   MediaType.TEXT_PLAIN })
public interface ConfigGroupsResource {

   @POST
   @Path("/")
   @Consumes({ MediaType.APPLICATION_XML })
   @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
      MediaType.TEXT_PLAIN })
   public Response createConfigGroups(String configGroups);

   @GET
   @Path("/")
   public Response readConfigGroupsWithFields(@QueryParam("fields") String fields);

   @GET
   @Path("/")
   public Response readConfigGroups();

   @GET
   @Path("/{ConfigGroupId}")
   public Response readConfigGroup(@PathParam(Parameters.CONFIG_GROUP_ID) String groupId);

   @DELETE
   @Path("/{ConfigGroupId}")
   public Response deleteConfigGroup(@PathParam(Parameters.CONFIG_GROUP_ID) String groupId);

   @PUT
   @Path("/{ConfigGroupId}")
   @Consumes({ MediaType.APPLICATION_XML })
   @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
   public Response updateConfigGroup(@PathParam(Parameters.CONFIG_GROUP_ID) String groupId, String configGroup);
}
