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

import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostComponentsResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.HostsResource;

public class FakeClusterHostsResource implements HostsResource {
   private String clusterName;
   public FakeClusterHostsResource(String clusterName) {
      this.clusterName = clusterName;
   }

   @Override
   @GET
   @Path("/")
   public Response readHosts() {
      return BuildResponse.buildResponse("clusters/cluster01_hosts.json");
   }

   @Override
   @GET
   @Path("/")
   public Response readHostsWithFilter(@QueryParam("fields") String fields) {
      return BuildResponse.buildResponse("clusters/host_status.json");
   }

   @Override
   @GET
   @Path("/{hostFQDN}")
   public Response readHost(@PathParam("hostFQDN") String hostFQDN) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   @DELETE
   @Path("/{hostFQDN}")
   public Response deleteHost(@PathParam("hostFQDN") String hostFQDN) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   @POST
   @Path("/")
   @Consumes("application/xml")
   @Produces({ "application/json", "application/xml", "text/plain",
         "text/html", "text/xml" })
   public Response addComponentsToHosts(String hostComponentsWithFilter) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   @POST
   @Path("/{hostFQDN}")
   public Response addHost(@PathParam("hostFQDN") String hostFQDN) {
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   @Path("/{hostFQDN}/host_components")
   public HostComponentsResource getHostComponentsResource(
         @PathParam("hostFQDN") String hostFQDN) {
      return new FakeHostComponentsResource();
   }

   @Override
   public Response deleteHostComponentsResource(String hostFQDN) {
      //As this function is not our test point and we didn't check the response content,
      // so just reuse the simple_request.json.
      return BuildResponse.buildResponse("clusters/simple_request.json");
   }

   @Override
   public Response getHostComponents(String hostFQDN) {
      return BuildResponse.buildResponse("cluster/hosts/host_components.json");
   }

   @Override
   public Response setRackInfo(String hostsRackInfo) {
      // TODO Auto-generated method stub
      return null;
   }

}
