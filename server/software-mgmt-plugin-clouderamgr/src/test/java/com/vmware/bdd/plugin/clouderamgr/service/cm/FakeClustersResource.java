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

import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiCdhUpgradeArgs;
import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiCommandList;
import com.cloudera.api.model.ApiHostRef;
import com.cloudera.api.model.ApiHostRefList;
import com.cloudera.api.model.ApiRestartClusterArgs;
import com.cloudera.api.model.ApiRollingRestartClusterArgs;
import com.cloudera.api.model.ApiServiceTypeList;
import com.cloudera.api.v3.HostTemplatesResource;
import com.cloudera.api.v5.ParcelsResourceV5;
import com.cloudera.api.v6.ClustersResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;

import javax.ws.rs.DefaultValue;

/**
 * Author: Xiaoding Bian
 * Date: 7/5/14
 * Time: 11:52 PM
 */
public class FakeClustersResource implements ClustersResourceV6{

   public ApiClusterList clusters;
   public ParcelsResourceV5 parcelsResource;
   public ServicesResourceV6 servicesResource;
   public FakeRootResource rootResource;

   public FakeClustersResource(FakeRootResource root) {
      clusters = new ApiClusterList();
      parcelsResource = new FakeParcelsResource();
      servicesResource = new FakeServicesResource();
      rootResource = root;
   }

   @Override
   public ApiClusterList createClusters(ApiClusterList apiClusters) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      for (ApiCluster cluster : apiClusters) {
         clusters.add(cluster);
      }
      return clusters;
   }

   @Override
   public ApiCluster deleteCluster(String s) {
      return null;
   }

   @Override
   public ApiClusterList readClusters(@DefaultValue("summary") DataView dataView) {
      return clusters;
   }

   @Override
   public ApiCluster readCluster(String s) {
      return null;
   }

   @Override
   public ServicesResourceV6 getServicesResource(String s) {
      return servicesResource;
   }

   @Override
   public ApiCommandList listActiveCommands(String s, @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiCommand upgradeServicesCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand startCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand stopCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand restartCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand rollingRestart(String s, ApiRollingRestartClusterArgs apiRollingRestartClusterArgs) {
      return null;
   }

   @Override
   public ApiCommand refresh(String s) {
      return null;
   }

   @Override
   public ApiCommand poolsRefresh(String s) {
      return null;
   }

   @Override
   public ApiCommand restartCommand(String s, ApiRestartClusterArgs apiRestartClusterArgs) {
      return null;
   }

   @Override
   public ApiServiceTypeList listServiceTypes(String s) {
      return null;
   }

   @Override
   public ApiCluster updateCluster(String s, ApiCluster apiCluster) {
      return null;
   }

   @Override
   public ApiCommand enterMaintenanceMode(String s) {
      return null;
   }

   @Override
   public ApiCommand exitMaintenanceMode(String s) {
      return null;
   }

   @Override
   public ApiCommand deployClientConfig(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("deployClientConfig");
      command.setId(1L);
      return command;
   }

   @Override
   public void autoAssignRoles(String s) {

   }

   @Override
   public void autoConfigure(String s) {

   }

   @Override
   public ApiCommand upgradeCdhCommand(String s, ApiCdhUpgradeArgs apiCdhUpgradeArgs) {
      return null;
   }

   @Override
   public ParcelsResourceV5 getParcelsResource(String s) {
      return parcelsResource;
   }

   @Override
   public HostTemplatesResource getHostTemplatesResource(String s) {
      return null;
   }

   @Override
   public ApiHostRefList listHosts(String s) {
      rootResource.getHostsResource().readHosts(DataView.SUMMARY);
      return new ApiHostRefList();
   }

   @Override
   public ApiHostRefList addHosts(String s, ApiHostRefList apiHostRefs) {
      return null;
   }

   @Override
   public ApiHostRef removeHost(String s, String s2) {
      return null;
   }

   @Override
   public ApiHostRefList removeAllHosts(String s) {
      return null;
   }
}
