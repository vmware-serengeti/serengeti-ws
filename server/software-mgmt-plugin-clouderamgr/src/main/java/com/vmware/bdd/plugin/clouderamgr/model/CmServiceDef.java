/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.clouderamgr.model;

import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:54 PM
 */
public class CmServiceDef extends AbstractCmServiceRole{

   @Expose
   private List<CmRoleDef> roles;

   @Expose
   private List<String> roleConfigGroups;

   @Expose
   private List<String> replicationSchedules;

   @Expose
   private List<String> snapshotPolicies;

   @Expose
   private String processUserName;

   @Expose
   private String processGroupName;

   public CmServiceDef() {}

   public CmServiceDef(String name, AvailableServiceRole type, String displayName, Map<String, String> configs,
         List<CmRoleDef> roles, List<String> roleConfigGroups, List<String> replicationSchedules, List<String> snapshotPolicies) {
      setName(name);
      setType(type);
      setDisplayName(displayName);
      setConfiguration(configs);
      this.roles = roles;
      this.roleConfigGroups = roleConfigGroups;
      this.replicationSchedules = replicationSchedules;
      this.snapshotPolicies = snapshotPolicies;
   }

   public List<CmRoleDef> getRoles() {
      return roles;
   }

   public void setRoles(List<CmRoleDef> roles) {
      this.roles = roles;
   }

   public List<String> getRoleConfigGroups() {
      return roleConfigGroups;
   }

   public void setRoleConfigGroups(List<String> roleConfigGroups) {
      this.roleConfigGroups = roleConfigGroups;
   }

   public List<String> getReplicationSchedules() {
      return replicationSchedules;
   }

   public void setReplicationSchedules(List<String> replicationSchedules) {
      this.replicationSchedules = replicationSchedules;
   }

   public List<String> getSnapshotPolicies() {
      return snapshotPolicies;
   }

   public void setSnapshotPolicies(List<String> snapshotPolicies) {
      this.snapshotPolicies = snapshotPolicies;
   }

   public void addRole(CmRoleDef role) {
      if (this.roles == null) {
         this.roles = new ArrayList<CmRoleDef>();
      }
      this.roles.add(role);
   }

   @Override
   public boolean isService() {
      return true;
   }

   @Override
   public boolean isRole() {
      return false;
   }

   public String getProcessUserName() {
      return processUserName;
   }

   public void setProcessUserName(String processUserName) {
      this.processUserName = processUserName;
   }

   public String getProcessGroupName() {
      return processGroupName;
   }

   public void setProcessGroupName(String processGroupName) {
      this.processGroupName = processGroupName;
   }
}
