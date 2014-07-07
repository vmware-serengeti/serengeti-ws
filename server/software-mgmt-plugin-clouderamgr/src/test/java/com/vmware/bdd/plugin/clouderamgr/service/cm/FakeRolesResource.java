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
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiCommandList;
import com.cloudera.api.model.ApiCommandMetadataList;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiMetricList;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleList;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.v4.ProcessResourceV4;
import com.cloudera.api.v6.RolesResourceV6;

import javax.ws.rs.DefaultValue;
import java.io.InputStream;
import java.util.List;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 3:48 PM
 */
public class FakeRolesResource implements RolesResourceV6 {

   public ApiRoleList roles;

   public FakeRolesResource(List<ApiRole> roleList) {
      roles = new ApiRoleList();
      for (ApiRole role : roleList) {
         roles.add(role);
      }
   }

   @Override
   public ApiRoleList bulkDeleteRoles(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiRoleList readRoles(@DefaultValue("") String s) {
      return null;
   }

   @Override
   public ApiCommandMetadataList listCommands(String s) {
      return null;
   }

   @Override
   public ProcessResourceV4 getProcessesResource(String s) {
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
   public ApiRoleList createRoles(ApiRoleList apiRoles) {
      return null;
   }

   @Override
   public ApiRole deleteRole(String s) {
      return null;
   }

   @Override
   public ApiRoleList readRoles() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return roles;
   }

   @Override
   public ApiRole readRole(String s) {
      return null;
   }

   @Override
   public ApiConfigList readRoleConfig(String s, @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiConfigList updateRoleConfig(String s, String s2, ApiConfigList apiConfigs) {
      return null;
   }

   @Override
   public ApiMetricList getMetrics(String s, String s2, @DefaultValue("now") String s3, List<String> strings,
         @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiCommandList listActiveCommands(String s, @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public InputStream getFullLog(String s) {
      return null;
   }

   @Override
   public InputStream getStandardOutput(String s) {
      return null;
   }

   @Override
   public InputStream getStandardError(String s) {
      return null;
   }
}
