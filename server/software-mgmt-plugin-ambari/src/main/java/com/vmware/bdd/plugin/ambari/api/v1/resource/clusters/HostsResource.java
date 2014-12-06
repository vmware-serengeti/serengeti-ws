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
package com.vmware.bdd.plugin.ambari.api.v1.resource.clusters;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
public interface HostsResource {

   @GET
   @Path("/")
   public Response readHosts();

   @GET
   @Path("/")
   public Response readHostsWithFilter(@QueryParam("fields") String fields);

   @GET
   @Path("/{hostFQDN}")
   public Response readHost(@PathParam(Parameters.HOST_FQDN) String hostFQDN);

   @DELETE
   @Path("/{hostFQDN}")
   public Response deleteHost(@PathParam(Parameters.HOST_FQDN) String hostFQDN);

   @POST
   @Path("/")
   @Consumes({ MediaType.APPLICATION_XML })
   @Produces({  MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
         MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.TEXT_XML })
   public Response addComponentsToHosts(String hostComponentsWithFilter);

   @POST
   @Path("/{hostFQDN}")
   public Response addHost(@PathParam(Parameters.HOST_FQDN) String hostFQDN);

   @GET
   @Path("/{hostFQDN}/host_components")
   public HostComponentsResource getHostComponentsResource(@PathParam(Parameters.HOST_FQDN) String hostFQDN);

   @DELETE
   @Path("/{hostFQDN}/host_components")
   public Response deleteHostComponentsResource(@PathParam(Parameters.HOST_FQDN) String hostFQDN);

   @GET
   @Path("/{hostFQDN}/host_components")
   public Response getHostComponents(@PathParam(Parameters.HOST_FQDN) String hostFQDN);

}
