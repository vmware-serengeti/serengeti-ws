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
import com.vmware.aurora.util.StringUtil;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import org.apache.commons.lang.StringUtils;

import javax.management.relation.RoleInfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 5/23/14
 * Time: 4:45 PM
 */
public class CmClusterDef implements Serializable {

   private static final long serialVersionUID = -2922528263257124521L;

   @Expose
   private String name;

   @Expose
   private String displayName;

   @Expose
   private String version; // TODO: relate to ApiClusterVersion, support CDH3, CDH3u4X, CDH4, CDH5, and only CDH4/CDH5 are supported

   @Expose
   private String fullVersion;

   @Expose
   private Boolean isParcel;

   @Expose
   private List<CmNodeDef> nodes;

   @Expose
   private List<CmServiceDef> services;

   private static String NAME_SEPARATOR = "_";

   public CmClusterDef() {}

   public CmClusterDef(ClusterBlueprint blueprint) {
      this.name = blueprint.getName();
      this.displayName = blueprint.getName();
      this.version = blueprint.getHadoopStack().getDisro();
      this.fullVersion = blueprint.getHadoopStack().getFullVersion();
      this.isParcel = true;
      this.nodes = new ArrayList<CmNodeDef>();
      this.services = new ArrayList<CmServiceDef>();
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         for (NodeInfo node : group.getNodes()) {
            CmNodeDef nodeDef = new CmNodeDef();
            nodeDef.setIpAddress(node.getMgtIpAddress());
            nodeDef.setFqdn(node.getMgtIpAddress());
            nodeDef.setRackId(node.getRack());
            nodeDef.setNodeId(node.getName()); // temp id, will be updated when installed.
            nodeDef.setConfigs(null);
            this.nodes.add(nodeDef);

            for (String roleType : group.getRoles()) {
               CmServiceDef service = serviceDefOfRole(roleType);
               CmRoleDef roleDef = new CmRoleDef();
               roleDef.setName(node.getName() + NAME_SEPARATOR + service.getType() + NAME_SEPARATOR + roleType); // temp name
               roleDef.setType(roleType);
               roleDef.setNodeRef(nodeDef.getNodeId());
               switch (CmServiceRoleType.valueOfId(roleType)) {
                  case HDFS_NAMENODE:
                     roleDef.addConfig("dfs_name_dir_list", dataDirs(node.getVolumes(), "/dfs/nn"));
                     break;
                  case HDFS_DATANODE:
                     roleDef.addConfig("dfs_data_dir_list", dataDirs(node.getVolumes(), "/dfs/dn"));
                     break;
                  case HDFS_SECONDARY_NAMENODE:
                     roleDef.addConfig("fs_checkpoint_dir_list", dataDirs(node.getVolumes(), "/dfs/snn"));
                     break;
                  case YARN_NODE_MANAGER:
                     roleDef.addConfig("yarn_nodemanager_local_dirs", dataDirs(node.getVolumes(), "/yarn/nm"));
                     break;
                  default:
                     break;
               }
               service.addRole(roleDef);
               // TODO: service/role configs
            }
         }
      }
   }

   private String dataDirs(List<String> volumes, String postFix) {
      List<String> dirList = new ArrayList<String>();
      for (String volume : volumes) {
         dirList.add(volume + postFix);
      }
      return StringUtils.join(dirList, ",");
   }

   /**
    * get the ServiceDef of given roleName, init it if not exist
    * @param roleType
    * @return
    */
   private synchronized CmServiceDef serviceDefOfRole(String roleType) {
      String serviceType = CmServiceRoleType.serviceOfRole(roleType); // assume roleName is already validated
      if (this.services == null) {
         this.services = new ArrayList<CmServiceDef>();
      }
      for (CmServiceDef service : this.services) {
         if (service.getType().equals(serviceType)) {
            return service;
         }
      }
      CmServiceDef service = new CmServiceDef();
      service.setName(this.name + NAME_SEPARATOR + serviceType);
      service.setDisplayName(service.getName());
      service.setType(serviceType);
      this.services.add(service);
      return service;
   }


   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDisplayName() {
      return displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getFullVersion() {
      return fullVersion;
   }

   public void setFullVersion(String fullVersion) {
      this.fullVersion = fullVersion;
   }

   public Boolean getIsParcel() {
      return isParcel;
   }

   public void setIsParcel(Boolean isParcel) {
      this.isParcel = isParcel;
   }

   public List<CmNodeDef> getNodes() {
      return nodes;
   }

   public void setNodes(List<CmNodeDef> nodes) {
      this.nodes = nodes;
   }

   public List<CmServiceDef> getServices() {
      return services;
   }

   public void setServices(List<CmServiceDef> services) {
      this.services = services;
   }

   public Set<String> allServiceNames() {
      Set<String> allServiceNames = new HashSet<String>();
      for (CmServiceDef serviceDef : this.services) {
         allServiceNames.add(serviceDef.getName());
      }
      return allServiceNames;
   }

   public Set<CmServiceRoleType> allServiceTypes() {
      Set<CmServiceRoleType> allServiceTypes = new HashSet<CmServiceRoleType>();
      for (CmServiceDef serviceDef : this.services) {
         allServiceTypes.add(CmServiceRoleType.valueOf(serviceDef.getType()));
      }
      return allServiceTypes;
   }

   /**
    * A cluster should has only one service for a givin service type
    * @param type
    * @return
    */
   public String serviceNameOfType(CmServiceRoleType type) {
      for (CmServiceDef serviceDef : this.services) {
         if (type.equals(CmServiceRoleType.valueOf(serviceDef.getType()))) {
            return serviceDef.getName();
         }
      }
      return null;
   }

   public boolean isEmpty() {
      return nodes == null || nodes.isEmpty() || services == null || services.isEmpty();
   }

   public Map<String, CmNodeDef> ipToNode() {
      Map<String, CmNodeDef> ipToNodeMap = new HashMap<String, CmNodeDef>();
      for(CmNodeDef node : nodes) {
         ipToNodeMap.put(node.getIpAddress(), node);
      }
      return ipToNodeMap;
   }

   public Map<String, List<CmRoleDef>> ipToRoles() {

      Map<String, CmNodeDef> idToNodeMap = new HashMap<String, CmNodeDef>();
      for(CmNodeDef node : nodes) {
         idToNodeMap.put(node.getNodeId(), node);
      }

      Map<String, List<CmRoleDef>> ipToRolesMap = new HashMap<String, List<CmRoleDef>>();
      for (CmServiceDef service : services) {
         for (CmRoleDef role : service.getRoles()) {
            CmNodeDef nodeRef = idToNodeMap.get(role.getNodeRef());
            if (!ipToRolesMap.containsKey(nodeRef.getIpAddress())) {
               ipToRolesMap.put(nodeRef.getIpAddress(), new ArrayList<CmRoleDef>());
            }
            ipToRolesMap.get(nodeRef.getIpAddress()).add(role);
         }
      }

      return ipToRolesMap;
   }

   public Map<String, List<CmRoleDef>> nodeRefToRoles() {

      Map<String, List<CmRoleDef>> nodeRefToRolesMap = new HashMap<String, List<CmRoleDef>>();
      for (CmServiceDef service : services) {
         for (CmRoleDef role : service.getRoles()) {
            if (!nodeRefToRolesMap.containsKey(role.getNodeRef())) {
               nodeRefToRolesMap.put(role.getNodeRef(), new ArrayList<CmRoleDef>());
            }
            nodeRefToRolesMap.get(role.getNodeRef()).add(role);
         }
      }

      return nodeRefToRolesMap;
   }

}
