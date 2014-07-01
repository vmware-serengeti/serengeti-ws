/***************************************************************************
 * Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import com.vmware.bdd.apitypes.InstanceType;
import com.vmware.bdd.apitypes.PlacementPolicy;
import com.vmware.bdd.apitypes.Datastore.DatastoreType;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.plugin.ironfan.utils.ExpandUtils;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.HadoopRole.RoleComparactor;

public class InfrastructureUpdator {
   private static final Logger logger = Logger.getLogger(InfrastructureUpdator.class);
   private ClusterValidator validator;

   public void setValidator(ClusterValidator validator) {
      this.validator = validator;
   }

   public void updateInfrastructure(ClusterBlueprint blueprint) {
      expandDefaultCluster(blueprint);
      if (blueprint.getExternalHDFS() == null) {
         setExternalHDFSFromConf(blueprint);
      } else {
         setHadoopConfFromExternalHDFS(blueprint);
      }
      addTempFSServerRole(blueprint);
      sortNodeGroupRoles(blueprint);
      sortGroups(blueprint);
   }

   private void expandDefaultCluster(ClusterBlueprint blueprint) {
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         expandNodeGroup(group);
      }
   }

   private void expandNodeGroup(NodeGroupInfo group) {
      logger.debug("Expand instance type config for group " + group.getName());
      EnumSet<HadoopRole> enumRoles = HadoopRole.getEnumRoles(group.getRoles(), new ArrayList<String>());
      GroupType groupType = GroupType.fromHadoopRole(enumRoles);
      InstanceType instanceType = group.getInstanceType();
      if (instanceType == null) {
         // replace with default instanceType
         if (groupType == GroupType.MASTER_GROUP
               || groupType == GroupType.MASTER_JOBTRACKER_GROUP
               || groupType == GroupType.HBASE_MASTER_GROUP
               || groupType == GroupType.ZOOKEEPER_GROUP) {
            instanceType = InstanceType.MEDIUM;
         } else {
            instanceType = InstanceType.SMALL;
         }
         group.setInstanceType(instanceType);
      }
      if (group.getStorageSize() <= 0) {
         group.setStorageSize(ExpandUtils.getStorage(instanceType, groupType));
         logger.debug("storage size is setting to default value: " + group.getStorageSize());
      }
      if (group.getStorageType() == null) {
         DatastoreType storeType = groupType.getStorageEnumType();
         group.setStorageExpectedType(storeType.name());
      }
   }

   private void sortGroups(ClusterBlueprint blueprint) {
      logger.debug("begin to sort node groups.");
      Collections.sort(blueprint.getNodeGroups(), new Comparator<NodeGroupInfo>() {
         public int compare(NodeGroupInfo arg0, NodeGroupInfo arg1) {
            List<String> roles = new ArrayList<String>();
            EnumSet<HadoopRole> enumRoles0 =
                  HadoopRole.getEnumRoles(arg0.getRoles(), roles);
            GroupType groupType0 = GroupType.fromHadoopRole(enumRoles0);
            EnumSet<HadoopRole> enumRoles1 =
                  HadoopRole.getEnumRoles(arg1.getRoles(), roles);
            GroupType groupType1 = GroupType.fromHadoopRole(enumRoles1);

            if (groupType0.equals(groupType1)) {
               return arg0.getName().compareTo(arg1.getName());
            } else {
               return groupType0.compareTo(groupType1);
            }
         }
      });
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
            logger.info("Soring roles based on role dependency and relationship with HDFS");
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

   private void changeClusterHDFSUrl(ClusterBlueprint blueprint) {
      Map<String, Object> conf = blueprint.getConfiguration();
      if (conf == null) {
         conf = new HashMap<String, Object>();
         blueprint.setConfiguration(conf);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> hadoopConf = (Map<String, Object>) conf.get("hadoop");
      if (hadoopConf == null) {
         hadoopConf = new HashMap<String, Object>();
         conf.put("hadoop", hadoopConf);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> coreSiteConf =
            (Map<String, Object>) hadoopConf.get("core-site.xml");
      if (coreSiteConf == null) {
         coreSiteConf = new HashMap<String, Object>();
         hadoopConf.put("core-site.xml", coreSiteConf);
      }
      coreSiteConf.put("fs.default.name", blueprint.getExternalHDFS());
   }

   @SuppressWarnings("unchecked")
   private void changeNodeGroupHDFSUrl(List<NodeGroupInfo> nodeGroups,
         String externalHDFS) {
      if (nodeGroups == null || nodeGroups.isEmpty()) {
         return;
      }
      String[] configKeyNames =
            new String[] { "hadoop", "core-site.xml", "fs.default.name" };
      for (NodeGroupInfo nodeGroup : nodeGroups) {
         Map<String, Object> conf = nodeGroup.getConfiguration();
         if (conf != null) {
            for (String configKeyName : configKeyNames) {
               if (configKeyName
                     .equals(configKeyNames[configKeyNames.length - 1])) {
                  if (conf.get(configKeyName) != null) {
                     conf.put(configKeyName, externalHDFS);
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
   }

}
