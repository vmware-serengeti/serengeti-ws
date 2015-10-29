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
package com.vmware.bdd.plugin.ambari.service.am;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.vmware.bdd.plugin.ambari.api.exception.AmbariApiException;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ServicesResource;

public class FakeServicesResource implements ServicesResource {

   private String clusterName;

   public FakeServicesResource(String clusterName) {
      this.clusterName = clusterName;
   }

   @Override
   @PUT
   @Path("/")
   @Consumes("application/xml")
   public Response stopAllServices(
         @PathParam("clusterName") String clusterName,
         @QueryParam("params/run_smoke_test") String runSmockTest,
         String request) {
      String expectedRequest = "{\"Body\":{\"ServiceInfo\":{\"state\":\"INSTALLED\"}},\"RequestInfo\":{\"aborted_task_count\":0,\"completed_task_count\":0,\"create_time\":0,\"end_time\":0,\"failed_task_count\":0,\"progress_percent\":0.0,\"queued_task_count\":0,\"start_time\":0,\"task_count\":0,\"timed_out_task_count\":0,\"context\":\"Stop All Services\"}}";
      if (expectedRequest.equals(request)) {
         Response.ResponseBuilder builder = Response.ok(request, "text/plain");
         return builder.build();
      }
      throw new AmbariApiException();
   }

   @Override
   @PUT
   @Path("/")
   @Consumes("application/xml")
   public Response startAllServices(
         @PathParam("clusterName") String clusterName,
         @QueryParam("params/run_smoke_test") String runSmockTest,
         String request) {
      // TODO Auto-generated method stub

      String expectedRequest = "{\"Body\":{\"ServiceInfo\":{\"state\":\"STARTED\"}},\"RequestInfo\":{\"aborted_task_count\":0,\"completed_task_count\":0,\"create_time\":0,\"end_time\":0,\"failed_task_count\":0,\"progress_percent\":0.0,\"queued_task_count\":0,\"start_time\":0,\"task_count\":0,\"timed_out_task_count\":0,\"context\":\"Start All Services\"}}";
      if (expectedRequest.equals(request)) {
         Response.ResponseBuilder builder = Response.ok(request, "text/plain");
         return builder.build();
      }
      throw new AmbariApiException();
   }

   @Override
   @GET
   @Path("/")
   public Response readServicesWithFilter(@QueryParam("fields") String fields) {
      return BuildResponse.buildResponse("clusters/simple_cluster_servicesWithAlert.json");
   }

   @Override
   @DELETE
   @Path("/{serviceName}")
   public Response deleteService(@PathParam("serviceName") String serviceName) {
      return BuildResponse.buildResponse("");
   }

   @Override
   @GET
   @Path("/{serviceName}")
   public Response readService(@PathParam("serviceName") String serviceName) {
      return BuildResponse.buildResponse("clusters/cluster01_HDFS_service.json");
   }

}
