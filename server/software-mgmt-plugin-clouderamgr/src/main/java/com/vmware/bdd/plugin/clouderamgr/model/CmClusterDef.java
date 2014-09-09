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
package com.vmware.bdd.plugin.clouderamgr.model;

import com.cloudera.api.model.ApiClusterVersion;
import com.google.gson.annotations.Expose;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRole;
import com.vmware.bdd.plugin.clouderamgr.model.support.AvailableServiceRoleContainer;
import com.vmware.bdd.plugin.clouderamgr.utils.Constants;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.IOException;
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

   private static final Logger logger = Logger.getLogger(CmClusterDef.class);

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
   private List<CmNodeDef> nodes;

   @Expose
   private List<CmServiceDef> services;

   private boolean failoverEnabled;

   private ClusterReport currentReport;

   private static String NAME_SEPARATOR = "_";

   public CmClusterDef() {}

   public CmClusterDef(ClusterBlueprint blueprint) throws IOException {
      this.name = blueprint.getName();
      this.displayName = blueprint.getName();
      try {
         String[] distroInfo = blueprint.getHadoopStack().getDistro().split("-");
         this.version = distroInfo[0] + (new DefaultArtifactVersion(distroInfo[1])).getMajorVersion();
         this.fullVersion = distroInfo[1];
      } catch (Exception e) {
         // in case distro is null or not complete
         this.version = ApiClusterVersion.CDH5.toString();
         this.fullVersion = null;
      }
      this.nodes = new ArrayList<CmNodeDef>();
      this.services = new ArrayList<CmServiceDef>();
      this.currentReport = new ClusterReport(blueprint);
      this.failoverEnabled = isFailoverEnabled(blueprint);
      Integer zkIdIndex = 1;
      Integer nameServiceIndex = 0;
      boolean hasImpala = false;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         boolean alreadyHasActive = false;
         for (NodeInfo node : group.getNodes()) {
            CmNodeDef nodeDef = new CmNodeDef();
            nodeDef.setIpAddress(node.getMgtIpAddress());
            nodeDef.setFqdn(node.getMgtIpAddress());
            /*
             Rack names are slash-separated identifiers, like Unix paths. For example, "/rack1" and "/cabinet3/rack4" are both valid.
             */
            nodeDef.setName(node.getName());
            nodeDef.setRackId(node.getRack());
            nodeDef.setNodeId(node.getName()); // temp id, will be updated when installed.
            nodeDef.setConfigs(null);
            this.nodes.add(nodeDef);

            for (String type : group.getRoles()) {
               AvailableServiceRole roleType = AvailableServiceRoleContainer.load(type);
               AvailableServiceRole serviceType = roleType.getParent();
               if (serviceType.getDisplayName().equals("IMPALA")) {
                  hasImpala = true;
               }
               CmServiceDef service = serviceDefOfType(serviceType, blueprint.getConfiguration());
               CmRoleDef roleDef = new CmRoleDef();
               roleDef.setName(node.getName() + NAME_SEPARATOR + service.getType().getName() + NAME_SEPARATOR + roleType.getName()); // temp name
               roleDef.setDisplayName(roleDef.getName());
               roleDef.setType(roleType);
               roleDef.setNodeRef(nodeDef.getNodeId());
               switch (roleType.getDisplayName()) {
                  case "HDFS_NAMENODE":
                     roleDef.addConfig(Constants.CONFIG_DFS_NAME_DIR_LIST, dataDirs(node.getVolumes(), "/dfs/nn"));
                     if (failoverEnabled) {
                        if (!alreadyHasActive) {
                           nameServiceIndex++;
                        }
                        roleDef.addConfig(Constants.CONFIG_AUTO_FAILOVER_ENABLED, "true");
                        roleDef.addConfig(Constants.CONFIG_DFS_FEDERATION_NAMESERVICE, "nameservice" + nameServiceIndex.toString()); // TODO: federation
                        roleDef.addConfig(Constants.CONFIG_DFS_NAMENODE_QUORUM_JOURNAL_NAME, "nameservice" + nameServiceIndex.toString());
                        //roleDef.addConfig(Constants.CONFIG_DFS_NAMESERVICE_MOUNTPOINTS, "/");
                        roleDef.setActive(!alreadyHasActive);

                        // auto-complete Failover Controller role
                        if (!group.getRoles().contains("HDFS_FAILOVER_CONTROLLER")) {
                           CmRoleDef failoverRole = new CmRoleDef();
                           AvailableServiceRole failoverRoleType = AvailableServiceRoleContainer.load("HDFS_FAILOVER_CONTROLLER");
                           failoverRole.setName(node.getName() + NAME_SEPARATOR + service.getType().getName() + NAME_SEPARATOR + failoverRoleType.getName()); // temp name
                           failoverRole.setType(failoverRoleType);
                           failoverRole.setNodeRef(nodeDef.getNodeId());
                           failoverRole.addConfigs(blueprint.getConfiguration());
                           failoverRole.addConfigs(group.getConfiguration()); // group level configs will override cluster level configs
                           failoverRole.setActive(!alreadyHasActive);
                           service.addRole(failoverRole);
                        }
                        alreadyHasActive = true;
                     }
                     break;
                  case "HDFS_FAILOVER_CONTROLLER":
                     roleDef.setActive(!alreadyHasActive);
                     break;
                  case "HDFS_DATANODE":
                     roleDef.addConfig(Constants.CONFIG_DFS_DATA_DIR_LIST, dataDirs(node.getVolumes(), "/dfs/dn"));
                     break;
                  case "HDFS_JOURNALNODE":
                     if (!node.getVolumes().isEmpty()) {
                        roleDef.addConfig(Constants.CONFIG_DFS_JOURNALNODE_EDITS_DIR, node.getVolumes().get(0) + "/dfs/jn");
                     } else {
                        logger.warn("No disk volumes found in node " + node.getName());
                     }
                     break;
                  case "HDFS_SECONDARY_NAMENODE":
                     roleDef.addConfig(Constants.CONFIG_FS_CHECKPOINT_DIR_LIST, dataDirs(node.getVolumes(), "/dfs/snn"));
                     break;
                  case "YARN_NODE_MANAGER":
                     roleDef.addConfig(Constants.CONFIG_NM_LOCAL_DIRS, dataDirs(node.getVolumes(), "/yarn/nm"));
                     break;
                  case "MAPREDUCE_JOBTRACKER":
                     roleDef.addConfig(Constants.CONFIG_MAPRED_JT_LOCAL_DIR_LIST, dataDirs(node.getVolumes(), "/mapred/jt"));
                     break;
                  case "MAPREDUCE_TASKTRACKER":
                     roleDef.addConfig(Constants.CONFIG_MAPRED_TT_LOCAL_DIR_LIST, dataDirs(node.getVolumes(), "/mapred/tt"));
                     break;
                  case "ZOOKEEPER_SERVER":
                     roleDef.addConfig(Constants.CONFIG_ZOOKEEPER_SERVER_ID, zkIdIndex.toString());
                     zkIdIndex += 1;
                     break;
                  case "SQOOP_SERVER":
                     roleDef.addConfig(Constants.CONFIG_SQOOP_METASTORE_DATA_DIR, node.getVolumes().get(0) + "/sqoop2/metastore");
                     break;
                  default:
                     break;
               }
               roleDef.addConfigs(blueprint.getConfiguration());
               roleDef.addConfigs(group.getConfiguration()); // group level configs will override cluster level configs
               service.addRole(roleDef);
            }
         }

         // impala requires special settings for HDFS service
         if (hasImpala) {
            for (CmServiceDef serviceDef : services) {
               if (serviceDef.getType().getDisplayName().equals("HDFS")) {
                  serviceDef.addConfig("dfs_block_local_path_access_user", "impala");
                  for (CmRoleDef roleDef : serviceDef.getRoles()) {
                     if (roleDef.getType().getDisplayName().equals("HDFS_DATANODE")) {
                        roleDef.addConfig("dfs_datanode_data_dir_perm", "755");
                     }
                  }
                  break;
               }
            }
         }
      }
   }

   public boolean isFailoverEnabled() {
      return failoverEnabled;
   }

   private boolean isFailoverEnabled(ClusterBlueprint blueprint) {
      int nnNum = 0;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         for (String role : group.getRoles()) {
            if (role.equals("HDFS_NAMENODE")) {
               nnNum += group.getInstanceNum();
            }
         }
      }

      return nnNum > 1;
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
    *
    * @param serviceType
    * @return
    */

   private synchronized CmServiceDef serviceDefOfType(AvailableServiceRole serviceType, Map<String, Object> configuration) {
      if (this.services == null) {
         this.services = new ArrayList<CmServiceDef>();
      }
      for (CmServiceDef service : this.services) {
         if (service.getType().equals(serviceType)) {
            return service;
         }
      }
      CmServiceDef service = new CmServiceDef();
      service.setName(this.name + NAME_SEPARATOR + serviceType.getName());
      service.setDisplayName(service.getName());
      service.setType(serviceType);
      service.addConfigs(configuration);
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

   public List<CmNodeDef> getNodes() {
      return nodes;
   }

   public void setNodes(List<CmNodeDef> nodes) {
      this.nodes = nodes;
   }

   public ClusterReport getCurrentReport() {
      return currentReport;
   }

   public void setCurrentReport(ClusterReport currentReport) {
      this.currentReport = currentReport;
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

   /**
    * set of display name of all services
    * @return
    */
   public Set<String> allServiceTypes() {
      Set<String> allServiceTypes = new HashSet<String>();
      for (CmServiceDef serviceDef : this.services) {
         allServiceTypes.add(serviceDef.getType().getDisplayName());
      }
      return allServiceTypes;
   }

   public String serviceNameOfType(String typeName) {
      CmServiceDef serviceDef = serviceDefOfType(typeName);
      if (serviceDef == null) {
         return null;
      }
      return serviceDef.getName();
   }

   public CmServiceDef serviceDefOfType(String typeName) {
      for (CmServiceDef serviceDef : this.services) {
         if (typeName.equalsIgnoreCase(serviceDef.getType().getDisplayName())) {
            return serviceDef;
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

      Map<String, CmNodeDef> idToNodeMap = idToHosts();

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

   public Map<String, CmNodeDef> idToHosts() {
      Map<String, CmNodeDef> idToNodeMap = new HashMap<String, CmNodeDef>();
      for(CmNodeDef node : nodes) {
         idToNodeMap.put(node.getNodeId(), node);
      }
      return idToNodeMap;
   }

   public Map<String, String> hostIdToName() {
      Map<String, String> idToNodeMap = new HashMap<String, String>();
      for(CmNodeDef node : nodes) {
         idToNodeMap.put(node.getNodeId(), node.getName());
      }
      return idToNodeMap;
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
