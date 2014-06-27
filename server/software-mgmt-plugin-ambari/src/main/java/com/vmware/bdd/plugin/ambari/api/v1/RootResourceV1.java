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
package com.vmware.bdd.plugin.ambari.api.v1;

import javax.ws.rs.Path;

@Path("")
public interface RootResourceV1 {
   /**
    * Lists all known clusters.
    */
   @Path("/clusters")
   public ClustersResource getClustersResource();

   /**
    * Lists all known bootstrap.
    */
   @Path("/bootstrap")
   public BootstrapResource getBootstrapResource();

   /**
    * Lists all known blueprints.
    */
   @Path("/blueprints")
   public BlueprintsResource getBlueprintsResource();

   @Path("/stacks2")
   public Stacks2Resource getStacks2Resource();

}
