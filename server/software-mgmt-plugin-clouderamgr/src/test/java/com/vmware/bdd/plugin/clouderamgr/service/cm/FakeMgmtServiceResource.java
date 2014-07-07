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
import com.cloudera.api.model.ApiRoleTypeList;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceState;
import com.cloudera.api.v1.MgmtRoleCommandsResource;
import com.cloudera.api.v1.MgmtRolesResource;
import com.cloudera.api.v3.MgmtRoleConfigGroupsResource;
import com.cloudera.api.v6.MgmtServiceResourceV6;

import javax.ws.rs.DefaultValue;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 4:04 PM
 */
public class FakeMgmtServiceResource implements MgmtServiceResourceV6 {
   @Override
   public ApiService deleteCMS() {
      return null;
   }

   @Override
   public void autoAssignRoles() {

   }

   @Override
   public void autoConfigure() {

   }

   @Override
   public MgmtRoleConfigGroupsResource getRoleConfigGroupsResource() {
      return null;
   }

   @Override
   public ApiService readService(@DefaultValue("summary") DataView dataView) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiService service = new ApiService();
      service.setServiceState(ApiServiceState.STARTED);
      return service;
   }

   @Override
   public ApiService readService() {
      return null;
   }

   @Override
   public ApiServiceConfig readServiceConfig(@DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiServiceConfig updateServiceConfig(String s, ApiServiceConfig apiConfigs) {
      return null;
   }

   @Override
   public ApiRoleTypeList listRoleTypes() {
      return null;
   }

   @Override
   public ApiCommandList listActiveCommands(@DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiCommand startCommand() {
      return null;
   }

   @Override
   public ApiCommand stopCommand() {
      return null;
   }

   @Override
   public ApiCommand restartCommand() {
      return null;
   }

   @Override
   public ApiService setupCMS(ApiService apiService) {
      return null;
   }

   @Override
   public MgmtRolesResource getRolesResource() {
      return null;
   }

   @Override
   public MgmtRoleCommandsResource getMgmtRoleCommandsResource() {
      return null;
   }
}
