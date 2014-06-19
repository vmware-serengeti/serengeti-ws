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
package com.vmware.bdd.api.v1;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.vmware.bdd.api.Parameters;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
public interface ClustersResource {

   /**
    * Lists all known clusters.
    *
    * @return List of known clusters.
    */
   @GET
   @Path("/")
   public String readClusters();

   /**
    * Reads information about a cluster.
    *
    * @param clusterName Name of cluster to look up.
    * @return Details of requested cluster.
    */
   @GET
   @Path("/{clusterName}")
   public String readCluster(@PathParam(Parameters.CLUSTER_NAME) String clusterName);

   /**
    * Creates a collection of clusters.
    *
    * @param clusters List of clusters to created.
    * @return List of created clusters.
    */
   @POST
   @Path("/{clusterName}")
   public String createCluster();

   @Path("/{clusterName}/requests")
   public RequestsResource getRequestsResource(@PathParam(Parameters.CLUSTER_NAME) String clusterName);

}
