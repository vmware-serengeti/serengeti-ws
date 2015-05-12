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

import com.vmware.bdd.plugin.ambari.api.v1.BlueprintsResource;
import com.vmware.bdd.plugin.ambari.api.v1.BootstrapResource;
import com.vmware.bdd.plugin.ambari.api.v1.HealthCheckResource;
import com.vmware.bdd.plugin.ambari.api.v1.HostsResource;
import com.vmware.bdd.plugin.ambari.api.v1.PersistResource;
import com.vmware.bdd.plugin.ambari.api.v1.RootResourceV1;
import com.vmware.bdd.plugin.ambari.api.v1.RootServicesResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.clusters.ClustersResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks.StacksResource;
import com.vmware.bdd.plugin.ambari.api.v1.resource.stacks2.Stacks2Resource;

public class FakeRootResourceV1 implements RootResourceV1 {

   public ClustersResource clustersResource;
   public BootstrapResource bootstrapResource;
   public BlueprintsResource blueprintsResource;
   public Stacks2Resource stacks2Resource;
   public HealthCheckResource healthCheckResource;
   public RootServicesResource rootServicesResource;
   public PersistResource persistResource;
   public HostsResource hostsResource;

   public FakeRootResourceV1() {
      clustersResource = new FakeClustersResource();
      bootstrapResource = new FakeBootstrapResource();
      blueprintsResource = new FakeBlueprintsResource();
      stacks2Resource = new FakeStacks2Resource();
      healthCheckResource = new FakeHealthCheckResource();
      rootServicesResource = new FakeRootServicesResource();
      persistResource = new FakePersistResource();
      hostsResource = new FakeHostsResource();
   }

   @Override
   public ClustersResource getClustersResource() {
      return clustersResource;
   }

   @Override
   public BootstrapResource getBootstrapResource() {
      return bootstrapResource;
   }

   @Override
   public BlueprintsResource getBlueprintsResource() {
      return blueprintsResource;
   }

   @Override
   public Stacks2Resource getStacks2Resource() {
      return stacks2Resource;
   }

   @Override
   public HealthCheckResource getHealthCheckResource() {
      return healthCheckResource;
   }

   @Override
   public RootServicesResource getRootServicesResource() {
      return rootServicesResource;
   }

   @Override
   public PersistResource getPersistResource() {
      return persistResource;
   }

   @Override
   public HostsResource getHostsResource() {
      return hostsResource;
   }

   @Override
   public StacksResource getStacksResource() {
      // TODO Auto-generated method stub
      return null;
   }

}
