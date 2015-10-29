/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ironfan.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.PlacementPolicy;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.HadoopRole.RoleComparactor;
import com.vmware.bdd.spectypes.ServiceType;

public class InfrastructureUpdator {
   private static final Logger logger = Logger.getLogger(InfrastructureUpdator.class);
   private ClusterValidator validator;

   public void setValidator(ClusterValidator validator) {
      this.validator = validator;
   }

   public void updateInfrastructure(ClusterBlueprint blueprint) {
      updateExternalConfig(blueprint);
      addTempFSServerRole(blueprint);
      sortNodeGroupRoles(blueprint);
   }

   private void updateExternalConfig(ClusterBlueprint blueprint) {
      if (blueprint.getExternalHDFS() == null) {
         setExternalHDFSFromConf(blueprint);
      } else {
         setHadoopConfFromExternalHDFS(blueprint);
      }
      if (blueprint.getExternalMapReduce() == null) {
         setExternalMapReduceFromConf(blueprint);
      } else {
         setHadoopConfFromExternalMapReduce(blueprint);
      }
   }

   private void sortNodeGroupRoles(ClusterBlueprint blueprint) {
      for (NodeGroupInfo nodeGroup : blueprint.getNodeGroups()) {
         List<String> roles = nodeGroup.getRoles();
         List<String> unSupportedRoles = new ArrayList<String>();
         EnumSet<HadoopRole> enumRoles = HadoopRole.getEnumRoles(roles, unSupportedRoles);
         if (enumRoles.isEmpty()) {
            throw ClusterConfigException.NO_HADOOP_ROLE_SPECIFIED(nodeGroup.getName());
         }
         if (!enumRoles.contains(HadoopRole.CUSTOMIZED_ROLE)) {
            logger.info("Sorting roles name based on role dependency and relationship with HDFS");
            Collections.sort(roles, new RoleComparactor());
            nodeGroup.setRoles(roles);
         }
      }
   }

   private void addTempFSServerRole(ClusterBlueprint blueprint) {
      Set<String> referencedNodeGroups = new HashSet<String>();
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (DatastoreType.TEMPFS.name().equalsIgnoreCase(group.getStorageType())) {
            PlacementPolicy policies = group.getPlacement();
            if (policies != null) {
               List<GroupAssociation> associons = policies.getGroupAssociations();
               if (associons != null) {
                  for (GroupAssociation a : associons) {
                     referencedNodeGroups.add(a.getReference());
                  }
               }
            }
         }
      }
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (referencedNodeGroups.contains(group.getName())
               && !group.getRoles().contains(HadoopRole.TEMPFS_SERVER_ROLE.toString())) {
            group.getRoles().add(0, HadoopRole.TEMPFS_SERVER_ROLE.toString());
         }
      }
   }

   private void setExternalHDFSFromConf(ClusterBlueprint blueprint) {
      boolean computeOnly = true;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (group.getRoles().contains(
               HadoopRole.HADOOP_NAMENODE_ROLE.toString()) || group
               .getRoles().contains(HadoopRole.MAPR_CLDB_ROLE.toString())) {
            computeOnly = false;
         }
      }

      if (computeOnly && blueprint.getConfiguration() != null) {
         Map conf = blueprint.getConfiguration();
         Map hadoopConf = (Map) conf.get("hadoop");
         if (hadoopConf != null) {
            Map coreSiteConf = (Map) hadoopConf.get("core-site.xml");
            if (coreSiteConf != null) {
               String hdfs = (String) coreSiteConf.get("fs.default.name");
               if (hdfs != null && !hdfs.isEmpty()) {
                  logger.info("Update external HDFS URL to make spec consistent with hadoop configuration");
                  blueprint.setExternalHDFS(hdfs);
               }
            }
         }
      }
   }

   private void setExternalMapReduceFromConf(ClusterBlueprint blueprint) {
      ServiceType type = getMapReduceType(blueprint);
      if (blueprint.getConfiguration() != null) {
         Map conf = blueprint.getConfiguration();
         Map hadoopConf = (Map) conf.get("hadoop");
         if (hadoopConf != null) {
            Map coreSiteConf = null;
            String mapreduce = "";
            if (type == null || type.equals(ServiceType.MAPRED)) {
               coreSiteConf = (Map) hadoopConf.get("mapred-site.xml");
               if (coreSiteConf != null) {
                  mapreduce = (String) coreSiteConf.get("mapred.job.tracker");
               }
            } else {
               coreSiteConf = (Map) hadoopConf.get("yarn-site.xml");
               if (coreSiteConf != null) {
                  mapreduce =
                        (String) coreSiteConf
                              .get("yarn.resourcemanager.hostname");
               }
            }
            if (mapreduce != null && !mapreduce.isEmpty()) {
               logger.info("Update external MapReduce URL to make spec consistent with hadoop configuration");
               blueprint.setExternalMapReduce(mapreduce);
            }
         }
      }
   }

   private void setHadoopConfFromExternalMapReduce(ClusterBlueprint blueprint) {
      if (validator.hasMapreduceConfigured(blueprint)) {
         changeNodeGroupMapReduceUrl(blueprint.getNodeGroups(),
               blueprint.getExternalMapReduce());
         changeClusterMapReduceUrl(blueprint);
      }
   }

   @SuppressWarnings("unchecked")
   private void changeClusterMapReduceUrl(ClusterBlueprint blueprint) {
      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         blueprint.setConfiguration(conf);
      }
      Map<String, Object> hadoopConf = (Map<String, Object>) conf.get("hadoop");
      if (hadoopConf == null) {
         hadoopConf = new HashMap<String, Object>();
         conf.put("hadoop", hadoopConf);
      }
      ServiceType type = getMapReduceType(blueprint);
      Map<String, Object> coreSiteConf = null;
      if (type == null || type.equals(ServiceType.MAPRED)) {
         coreSiteConf = (Map<String, Object>) hadoopConf.get("mapred-site.xml");
         if (coreSiteConf == null) {
            coreSiteConf = new HashMap<String, Object>();
            hadoopConf.put("mapred-site.xml", coreSiteConf);
         }
         coreSiteConf.put("mapred.job.tracker",
               blueprint.getExternalMapReduce());
      } 
      if (type == null || type.equals(ServiceType.YARN)){
         coreSiteConf = (Map<String, Object>) hadoopConf.get("yarn-site.xml");
         if (coreSiteConf == null) {
            coreSiteConf = new HashMap<String, Object>();
            hadoopConf.put("yarn-site.xml", coreSiteConf);
         }
         coreSiteConf.put("yarn.resourcemanager.hostname",
               blueprint.getExternalMapReduce());
      }
   }

   private void changeNodeGroupMapReduceUrl(List<NodeGroupInfo> nodeGroups,
         String externalHDFS) {
      if (nodeGroups == null || nodeGroups.isEmpty()) {
         return;
      }
      String[] propertyPath = null;
      Map<String, Object> confTmp = null;
      for (NodeGroupInfo nodeGroup : nodeGroups) {
         Map<String, Object> conf = nodeGroup.getConfiguration();
         ServiceType type = getMapReduceType(nodeGroup);
         if (conf != null) {
            confTmp =conf;
            if (type == null || type.equals(ServiceType.MAPRED)) {
               propertyPath = new String[] { "hadoop", "mapred-site.xml", "mapred.job.tracker" };
               setPropertyOfConfiguration(conf, propertyPath, externalHDFS);
            }
            conf = confTmp;
            if (type == null || type.equals(ServiceType.YARN)) {
               propertyPath = new String[] { "hadoop", "yarn-site.xml", "yarn.resourcemanager.hostname" };
               setPropertyOfConfiguration(conf, propertyPath, externalHDFS);
            }
         }
      }
   }

   private ServiceType getMapReduceType(ClusterBlueprint blueprint) {
      ServiceType type = null;
      List<String> roles = new ArrayList<String>();
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         roles.addAll(group.getRoles());
      }
      if (roles.contains(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString())) {
         type = ServiceType.YARN;
      } else if (roles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())) {
         type = ServiceType.MAPRED;
      }
      return type;
   }

   private ServiceType getMapReduceType(NodeGroupInfo group) {
      ServiceType type = null;
      List<String> roles = new ArrayList<String>();
      roles.addAll(group.getRoles());
      if (roles.contains(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString())) {
         type = ServiceType.YARN;
      } else if (roles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())) {
         type = ServiceType.MAPRED;
      }
      return type;
   }

   private void setHadoopConfFromExternalHDFS(ClusterBlueprint blueprint) {
      if (blueprint.getExternalHDFS() != null) {
         if (validator.validateHDFSUrl(blueprint)) {
            changeNodeGroupHDFSUrl(blueprint.getNodeGroups(),
                  blueprint.getExternalHDFS());
            changeClusterHDFSUrl(blueprint);
         } else {
            throw BddException.INVALID_PARAMETER("externalHDFS",
                  blueprint.getExternalHDFS());
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void changeClusterHDFSUrl(ClusterBlueprint blueprint) {
      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         blueprint.setConfiguration(conf);
      }
      Map<String, Object> hadoopConf = (Map<String, Object>) conf.get("hadoop");
      if (hadoopConf == null) {
         hadoopConf = new HashMap<String, Object>();
         conf.put("hadoop", hadoopConf);
      }
      Map<String, Object> coreSiteConf =
            (Map<String, Object>) hadoopConf.get("core-site.xml");
      if (coreSiteConf == null) {
         coreSiteConf = new HashMap<String, Object>();
         hadoopConf.put("core-site.xml", coreSiteConf);
      }
      coreSiteConf.put("fs.default.name", blueprint.getExternalHDFS());
   }

   private void changeNodeGroupHDFSUrl(List<NodeGroupInfo> nodeGroups,
         String externalHDFS) {
      if (nodeGroups == null || nodeGroups.isEmpty()) {
         return;
      }
      String[] propertyPath =
            new String[] { "hadoop", "core-site.xml", "fs.default.name" };
      for (NodeGroupInfo nodeGroup : nodeGroups) {
         Map<String, Object> conf = nodeGroup.getConfiguration();
         if (conf != null) {
            this.setPropertyOfConfiguration(conf, propertyPath, externalHDFS);
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void setPropertyOfConfiguration(Map<String, Object> conf,
         String[] propertyPath, String value) {
      for (String configKeyName : propertyPath) {
         if (configKeyName.equals(propertyPath[propertyPath.length - 1])) {
            if (conf.get(configKeyName) != null) {
               conf.put(configKeyName, value);
            }
         } else {
            conf = (Map<String, Object>) conf.get(configKeyName);
            if (conf == null) {
               break;
            }
         }
      }
   }
}
