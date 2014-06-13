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
package com.vmware.bdd.model;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:54 PM
 */
public class CmServiceDef {

   @Expose
   private String name;

   @Expose
   private String type; // TODO: relate to CmServiceRoleType

   @Expose
   private String displayName;

   @Expose
   private Map<String, String> configs;

   @Expose
   private List<CmRoleDef> roles; // TODO: validate role.type, refer to http://cloudera.github.io/cm_api/apidocs/v6/path__clusters_-clusterName-_services_-serviceName-_roles.html

   @Expose
   private List<String> roleConfigGroups;

   @Expose
   private List<String> replicationSchedules;

   @Expose
   private List<String> snapshotPolicies;

   public static final int VERSION_UNBOUNDED = -1;

   public CmServiceDef() {}

   public CmServiceDef(String name, String type, String displayName, Map<String, String> configs,
         List<CmRoleDef> roles, List<String> roleConfigGroups, List<String> replicationSchedules, List<String> snapshotPolicies) {
      this.name = name;
      this.type = type;
      this.displayName = displayName;
      this.configs = configs;
      this.roles = roles;
      this.roleConfigGroups = roleConfigGroups;
      this.replicationSchedules = replicationSchedules;
      this.snapshotPolicies = snapshotPolicies;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public Map<String, String> getConfigs() {
      return configs;
   }

   public void setConfigs(Map<String, String> configs) {
      this.configs = configs;
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

}
