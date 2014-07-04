/*
 * **************************************************************************
 *  * Copyright (c) 2014 VMware, Inc. All Rights Reserved.
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  **************************************************************************
 */

package com.vmware.bdd.plugin.ambari.api.v1.resource.clusters;

import com.vmware.bdd.plugin.ambari.api.Parameters;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by qjin on 7/6/14.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
public interface ServicesResource {
   @PUT
   @Path("/")
   @Consumes({ MediaType.APPLICATION_XML})
   public Response stopAllServices(@PathParam(Parameters.CLUSTER_NAME) String clusterName,
                                 @QueryParam("params/run_smoke_test") String runSmockTest,
                                 String request);

   @PUT
   @Path("/")
   @Consumes({ MediaType.APPLICATION_XML})
   public Response startAllServices(@PathParam(Parameters.CLUSTER_NAME)String clusterName,
                                  @QueryParam("params/run_smoke_test") String runSmockTest,
                                  String request);

   @GET
   @Path("/")
   public Response readServicesWithFilter(@QueryParam("fields") String fields);

   @DELETE
   @Path("/{serviceName}")
   public Response deleteService(@PathParam(Parameters.SERVICE_NAME) String serviceName);

   @GET
   @Path("/{serviceName}")
   public Response readService(@PathParam(Parameters.SERVICE_NAME) String serviceName);
}