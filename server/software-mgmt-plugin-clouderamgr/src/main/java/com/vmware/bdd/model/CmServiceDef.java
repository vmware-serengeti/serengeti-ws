package com.vmware.bdd.model;

import com.google.gson.annotations.Expose;

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
   private CmConfigDef[] configs;

   @Expose
   private CmRoleDef[] roles; // TODO: validate role.type, refer to http://cloudera.github.io/cm_api/apidocs/v6/path__clusters_-clusterName-_services_-serviceName-_roles.html
   @Expose
   private String[] roleConfigGroups;

   @Expose
   private String[] replicationSchedules;

   @Expose
   private String[] snapshotPolicies;

   public static final int VERSION_UNBOUNDED = -1;

   public CmServiceDef() {}

   public CmServiceDef(String name, String type, String displayName, CmConfigDef[] configs,
         CmRoleDef[] roles, String[] roleConfigGroups, String[] replicationSchedules, String[] snapshotPolicies) {
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

   public CmConfigDef[] getConfigs() {
      return configs;
   }

   public void setConfigs(CmConfigDef[] configs) {
      this.configs = configs;
   }

   public CmRoleDef[] getRoles() {
      return roles;
   }

   public void setRoles(CmRoleDef[] roles) {
      this.roles = roles;
   }

   public String[] getRoleConfigGroups() {
      return roleConfigGroups;
   }

   public void setRoleConfigGroups(String[] roleConfigGroups) {
      this.roleConfigGroups = roleConfigGroups;
   }

   public String[] getReplicationSchedules() {
      return replicationSchedules;
   }

   public void setReplicationSchedules(String[] replicationSchedules) {
      this.replicationSchedules = replicationSchedules;
   }

   public String[] getSnapshotPolicies() {
      return snapshotPolicies;
   }

   public void setSnapshotPolicies(String[] snapshotPolicies) {
      this.snapshotPolicies = snapshotPolicies;
   }

}
