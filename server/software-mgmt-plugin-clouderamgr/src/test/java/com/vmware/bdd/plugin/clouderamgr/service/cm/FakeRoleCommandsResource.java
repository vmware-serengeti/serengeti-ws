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

import com.cloudera.api.model.ApiBulkCommandList;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.v6.RoleCommandsResourceV6;

/**
 * Author: Xiaoding Bian
 * Date: 7/7/14
 * Time: 3:56 PM
 */
public class FakeRoleCommandsResource implements RoleCommandsResourceV6{
   @Override
   public ApiBulkCommandList roleCommandByName(String s, ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList hdfsSaveNamespace(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList hdfsEnterSafemode(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList hdfsLeaveSafemode(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList hdfsFinalizeMetadataUpgrade(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList hdfsBootstrapStandByCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList formatCommand(ApiRoleNameList strings) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiBulkCommandList commands = new ApiBulkCommandList();
      ApiCommand command = new ApiCommand();
      command.setName("format namenode");
      command.setId(1L);
      commands.add(command);
      return commands;
   }

   @Override
   public ApiBulkCommandList hdfsInitializeAutoFailoverCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList hdfsInitializeSharedDirCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList syncHueDbCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList refreshCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList zooKeeperCleanupCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList zooKeeperInitCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList startCommand(ApiRoleNameList strings) {
      System.out.println("calling " + this.getClass().getInterfaces()[0].getName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());
      ApiBulkCommandList commands = new ApiBulkCommandList();
      ApiCommand command = new ApiCommand();
      command.setName("start role command");
      command.setId(1L);
      commands.add(command);
      return commands;
   }

   @Override
   public ApiBulkCommandList stopCommand(ApiRoleNameList strings) {
      return null;
   }

   @Override
   public ApiBulkCommandList restartCommand(ApiRoleNameList strings) {
      return null;
   }
}
