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
package com.vmware.bdd.plugin.clouderamgr.service.cm;

import com.cloudera.api.v1.CommandsResource;
import com.cloudera.api.v1.EventsResource;
import com.cloudera.api.v1.ToolsResource;
import com.cloudera.api.v1.UsersResource;
import com.cloudera.api.v2.HostsResourceV2;
import com.cloudera.api.v4.AuditsResource;
import com.cloudera.api.v6.BatchResource;
import com.cloudera.api.v6.ClouderaManagerResourceV6;
import com.cloudera.api.v6.ClustersResourceV6;
import com.cloudera.api.v6.RootResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;
import com.cloudera.api.v6.TimeSeriesResourceV6;

/**
 * Author: Xiaoding Bian
 * Date: 7/5/14
 * Time: 9:58 PM
 */

public class FakeRootResource implements RootResourceV6 {

   public ClustersResourceV6 clustersResourceV6;
   public ClouderaManagerResourceV6 clouderaManagerResourceV6;
   public HostsResourceV2 hostsResourceV2;
   public CommandsResource commandsResource;
   public ToolsResource toolsResource;

   public FakeRootResource() {
      clustersResourceV6 = new FakeClustersResource(this);
      hostsResourceV2 = new FakeHostsResource();
      clouderaManagerResourceV6 = new FakeClouderaManagerResource(hostsResourceV2);
      commandsResource = new FakeCommandsResource();
      toolsResource = new FakeToolsResource();
   }

   @Override
   public ClustersResourceV6 getClustersResource() {
      return clustersResourceV6;
   }

   @Override
   public CommandsResource getCommandsResource() {
      return commandsResource;
   }

   @Override
   public AuditsResource getAuditsResource() {
      return null;
   }

   @Override
   public BatchResource getBatchResource() {
      return null;
   }

   @Override
   public ClouderaManagerResourceV6 getClouderaManagerResource() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return clouderaManagerResourceV6;
   }

   @Override
   public HostsResourceV2 getHostsResource() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return hostsResourceV2;
   }

   @Override
   public ToolsResource getToolsResource() {
      return toolsResource;
   }

   @Override
   public UsersResource getUsersResource() {
      return null;
   }

   @Override
   public EventsResource getEventsResource() {
      return null;
   }

   @Override
   public TimeSeriesResourceV6 getTimeSeriesResource() {
      return null;
   }
}
