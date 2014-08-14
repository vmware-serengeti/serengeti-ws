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

import com.cloudera.api.ApiTimeAggregation;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiCommandList;
import com.cloudera.api.model.ApiCommandMetadataList;
import com.cloudera.api.model.ApiDisableJtHaArguments;
import com.cloudera.api.model.ApiDisableNnHaArguments;
import com.cloudera.api.model.ApiDisableOozieHaArguments;
import com.cloudera.api.model.ApiDisableRmHaArguments;
import com.cloudera.api.model.ApiEnableJtHaArguments;
import com.cloudera.api.model.ApiEnableNnHaArguments;
import com.cloudera.api.model.ApiEnableOozieHaArguments;
import com.cloudera.api.model.ApiEnableRmHaArguments;
import com.cloudera.api.model.ApiHdfsDisableHaArguments;
import com.cloudera.api.model.ApiHdfsFailoverArguments;
import com.cloudera.api.model.ApiHdfsHaArguments;
import com.cloudera.api.model.ApiHdfsUsageReport;
import com.cloudera.api.model.ApiMetricList;
import com.cloudera.api.model.ApiMrUsageReport;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiRoleTypeList;
import com.cloudera.api.model.ApiRollEditsArgs;
import com.cloudera.api.model.ApiRollingRestartArgs;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.model.ApiServiceState;
import com.cloudera.api.v1.ActivitiesResource;
import com.cloudera.api.v1.NameservicesResource;
import com.cloudera.api.v3.RoleConfigGroupsResource;
import com.cloudera.api.v4.ImpalaQueriesResource;
import com.cloudera.api.v4.ReplicationsResourceV4;
import com.cloudera.api.v6.RoleCommandsResourceV6;
import com.cloudera.api.v6.RolesResourceV6;
import com.cloudera.api.v6.ServicesResourceV6;
import com.cloudera.api.v6.SnapshotsResource;
import com.cloudera.api.v6.YarnApplicationsResource;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;

import javax.ws.rs.DefaultValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 3:04 PM
 */
public class FakeServicesResource implements ServicesResourceV6 {

   public ApiServiceList services;
   public Map<String, RolesResourceV6> rolesResources;
   public RoleCommandsResourceV6 roleCommandsResource;
   public RoleConfigGroupsResource roleConfigGroupsResource;

   public FakeServicesResource() {
      services = new ApiServiceList();
      rolesResources = new HashMap<String, RolesResourceV6>();
      roleCommandsResource = new FakeRoleCommandsResource();
      roleConfigGroupsResource = new FakeRoleConfigGroupsResource();
   }

   @Override
   public ApiCommand hiveCreateMetastoreDatabaseCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand hiveUpdateMetastoreNamenodesCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand createSqoopUserDirCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand initSolrCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand createSolrHdfsHomeDirCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand createHiveUserDirCommand(String s) {
      return null;
   }

   @Override
   public InputStreamDataSource getClientConfig(String s) {
      return null;
   }

   @Override
   public ApiServiceList createServices(ApiServiceList apiServices) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      for (ApiService service : apiServices.getServices()) {
         ApiService newService = new ApiService();
         newService.setName(service.getName());
         newService.setType(service.getType());
         List<ApiRole> newRoles = new ArrayList<ApiRole>();
         for (ApiRole role : service.getRoles()) {
            ApiRole newRole = new ApiRole();
            newRole.setName(UUID.randomUUID().toString());
            newRole.setType(role.getType());
            newRole.setHostRef(role.getHostRef());
            newRole.setRoleState(ApiRoleState.STOPPED);
            newRoles.add(newRole);
         }
         newService.setRoles(newRoles);
         services.add(newService);
         RolesResourceV6 rolesResourceV6 = new FakeRolesResource(newRoles);
         rolesResources.put(service.getName(), rolesResourceV6);
      }

      return services;
   }

   @Override
   public ApiService deleteService(String s) {
      return null;
   }

   @Override
   public ApiServiceList readServices(@DefaultValue("summary") DataView dataView) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return services;
   }

   @Override
   public ApiService readService(String s) {
      ApiService apiService = new ApiService();
      apiService.setServiceState(ApiServiceState.UNKNOWN);
      return apiService;
   }

   @Override
   public ApiServiceConfig readServiceConfig(String s, @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiServiceConfig updateServiceConfig(String s, String s2, ApiServiceConfig apiConfigs) {
      return null;
   }

   @Override
   public ApiRoleTypeList listRoleTypes(String s) {
      return null;
   }

   @Override
   public ApiMetricList getMetrics(String s, String s2, @DefaultValue("now") String s3, List<String> strings,
         @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiCommandList listActiveCommands(String s, @DefaultValue("summary") DataView dataView) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return new ApiCommandList();
   }

   @Override
   public ApiCommand hdfsDisableAutoFailoverCommand(String s, String s2) {
      return null;
   }

   @Override
   public ApiCommand hdfsDisableHaCommand(String s, ApiHdfsDisableHaArguments apiHdfsDisableHaArguments) {
      return null;
   }

   @Override
   public ApiCommand hdfsEnableAutoFailoverCommand(String s, ApiHdfsFailoverArguments apiHdfsFailoverArguments) {
      return null;
   }

   @Override
   public ApiCommand hdfsEnableHaCommand(String s, ApiHdfsHaArguments apiHdfsHaArguments) {
      return null;
   }

   @Override
   public ApiCommand hdfsFailoverCommand(String s, @DefaultValue("false") boolean b, ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiCommand createBeeswaxWarehouseCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand createHBaseRootCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand decommissionCommand(String s, ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiCommand deployClientConfigCommand(String s, ApiRoleNameList strings) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("deploy client config");
      command.setId(1L);
      return command;
   }

   @Override
   public ApiCommand zooKeeperCleanupCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand zooKeeperInitCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand startCommand(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("start service");
      command.setId(1L);
      return command;
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
   public NameservicesResource getNameservicesResource(String s) {
      return null;
   }

   @Override
   public RolesResourceV6 getRolesResource(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return rolesResources.get(s);
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
   public ApiCommand recommissionCommand(String s, ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiCommand hdfsCreateTmpDir(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("hdfs create tmp dir");
      command.setId(1L);
      return command;
   }

   @Override
   public ApiCommand createOozieDb(String s) {
      return null;
   }

   @Override
   public ApiCommand enableJtHaCommand(String s, ApiEnableJtHaArguments apiEnableJtHaArguments) {
      return null;
   }

   @Override
   public ApiCommand disableJtHaCommand(String s, ApiDisableJtHaArguments apiDisableJtHaArguments) {
      return null;
   }

   @Override
   public ImpalaQueriesResource getImpalaQueriesResource(String s) {
      return null;
   }

   @Override
   public ReplicationsResourceV4 getReplicationsResource(String s) {
      return null;
   }

   @Override
   public ApiCommand rollingRestart(String s, ApiRollingRestartArgs apiRollingRestartArgs) {
      return null;
   }

   @Override
   public ApiCommand installOozieShareLib(String s) {
      return null;
   }

   @Override
   public RoleConfigGroupsResource getRoleConfigGroupsResource(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return roleConfigGroupsResource;
   }

   @Override
   public ApiCommand hdfsRollEditsCommand(String s, ApiRollEditsArgs apiRollEditsArgs) {
      return null;
   }

   @Override
   public ApiService updateService(String s, ApiService apiService) {
      return null;
   }

   @Override
   public ApiCommand hiveCreateMetastoreDatabaseTablesCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand createHiveWarehouseCommand(String s) {
      return null;
   }

   @Override
   public SnapshotsResource getSnapshotsResource(String s) {
      return null;
   }

   @Override
   public YarnApplicationsResource getYarnApplicationsResource(String s) {
      return null;
   }

   @Override
   public ApiCommand importMrConfigsIntoYarn(String s) {
      return null;
   }

   @Override
   public ApiCommand switchToMr2(String s) {
      return null;
   }

   @Override
   public ApiCommand enableRmHaCommand(String s, ApiEnableRmHaArguments apiEnableRmHaArguments) {
      return null;
   }

   @Override
   public ApiCommand disableRmHaCommand(String s, ApiDisableRmHaArguments apiDisableRmHaArguments) {
      return null;
   }

   @Override
   public ApiCommand enableOozieHaCommand(String s, ApiEnableOozieHaArguments apiEnableOozieHaArguments) {
      return null;
   }

   @Override
   public ApiCommand disableOozieHaCommand(String s, ApiDisableOozieHaArguments apiDisableOozieHaArguments) {
      return null;
   }

   @Override
   public ApiCommand hdfsEnableNnHaCommand(String s, ApiEnableNnHaArguments apiEnableNnHaArguments) {
      return null;
   }

   @Override
   public ApiCommand hdfsDisableNnHaCommand(String s, ApiDisableNnHaArguments apiDisableNnHaArguments) {
      return null;
   }

   @Override
   public ApiCommand hdfsUpgradeMetadataCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand hiveUpgradeMetastoreCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand oozieUpgradeDbCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand hbaseUpgradeCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand sqoopUpgradeDbCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand createYarnJobHistoryDirCommand(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("create yarn job history dir");
      command.setId(1L);
      return command;
   }

   @Override
   public ApiCommand createYarnNodeManagerRemoteAppLogDirCommand(String s) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiCommand command = new ApiCommand();
      command.setName("create yarn node manager remote app log dir");
      command.setId(1L);
      return command;
   }

   @Override
   public ApiCommand createImpalaUserDirCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand impalaCreateCatalogDatabaseCommand(String s) {
      return null;
   }

   @Override
   public ApiCommand impalaCreateCatalogDatabaseTablesCommand(String s) {
      return null;
   }

   @Override
   public RoleCommandsResourceV6 getRoleCommandsResource(String s) {
      return roleCommandsResource;
   }

   @Override
   public ActivitiesResource getActivitiesResource(String s) {
      return null;
   }

   @Override
   public ApiHdfsUsageReport getHdfsUsageReport(String s, String s2, String s3, @DefaultValue("now") String s4,
         @DefaultValue("daily") ApiTimeAggregation apiTimeAggregation) {
      return null;
   }

   @Override
   public ApiMrUsageReport getMrUsageReport(String s, String s2, @DefaultValue("now") String s3,
         @DefaultValue("daily") ApiTimeAggregation apiTimeAggregation) {
      return null;
   }

   @Override
   public ApiCommand serviceCommandByName(String s, String s2) {
      return null;
   }

   @Override
   public ApiCommandMetadataList listServiceCommands(String s) {
      return null;
   }
}
