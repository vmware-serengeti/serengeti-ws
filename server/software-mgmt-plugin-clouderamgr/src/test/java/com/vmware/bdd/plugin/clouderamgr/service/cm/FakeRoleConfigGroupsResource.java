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
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiRoleConfigGroup;
import com.cloudera.api.model.ApiRoleConfigGroupList;
import com.cloudera.api.model.ApiRoleList;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.v3.RoleConfigGroupsResource;

import javax.ws.rs.DefaultValue;

/**
 * Author: Xiaoding Bian
 * Date: 7/10/14
 * Time: 5:06 PM
 */
public class FakeRoleConfigGroupsResource implements RoleConfigGroupsResource {
   @Override
   public ApiRoleConfigGroupList createRoleConfigGroups(ApiRoleConfigGroupList apiRoleConfigGroups) {
      return null;
   }

   @Override
   public ApiRoleConfigGroupList readRoleConfigGroups() {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      return new ApiRoleConfigGroupList();
   }

   @Override
   public ApiRoleConfigGroup readRoleConfigGroup(String s) {
      return null;
   }

   @Override
   public ApiRoleConfigGroup updateRoleConfigGroup(String s, ApiRoleConfigGroup apiRoleConfigGroup, String s2) {
      return null;
   }

   @Override
   public ApiRoleConfigGroup deleteRoleConfigGroup(String s) {
      return null;
   }

   @Override
   public ApiRoleList readRoles(String s) {
      return null;
   }

   @Override
   public ApiRoleList moveRoles(String s, ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiRoleList moveRolesToBaseGroup(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiConfigList readConfig(String s, @DefaultValue("summary") DataView dataView) {
      return null;
   }

   @Override
   public ApiConfigList updateConfig(String s, String s2, ApiConfigList apiConfigs) {
      return null;
   }
}
