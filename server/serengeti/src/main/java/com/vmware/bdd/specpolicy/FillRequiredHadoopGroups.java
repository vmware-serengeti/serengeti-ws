/***************************************************************************
 *    Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License");
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
package com.vmware.bdd.specpolicy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.bdd.apitypes.Cluster;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.entity.ClusterEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.HadoopRole;

public class FillRequiredHadoopGroups {
   private static final Logger logger = Logger.getLogger(FillRequiredHadoopGroups.class);

   public Set<NodeGroupCreate> fillDefaultGroups(Cluster.ClusterType clusterType) {
      Set<NodeGroupCreate> nodeGroups;
      logger.debug("add default node group into cluster spec.");
      Map<GroupType, NodeGroupCreate> groupMap = TemplateClusterSpec.getTemplateGroupAttributes();
      nodeGroups = new HashSet<NodeGroupCreate>();
      NodeGroupCreate group = null;
      for (GroupType type : GroupType.values()) {
         if (type == GroupType.MASTER_JOBTRACKER_GROUP) {
            continue;
         }
         group = groupMap.get(type);
         if (clusterType == Cluster.ClusterType.HADOOP) {
            if (type == GroupType.ZOOKEEPER_GROUP) {
               continue;
            }
            removeHbaseRole(group,type);
         }
         nodeGroups.add(group);
      }
      return nodeGroups;
   }

   public Set<NodeGroupCreate> FillMissingGroups(Set<NodeGroupEntity> nodeGroups, 
         EnumSet<HadoopRole> missingRoles, ClusterEntity clusterEntity, Cluster.ClusterType clusterType) {
      Set<NodeGroupCreate> missingGroups = new HashSet<NodeGroupCreate>();
      appendWorkerGroup(nodeGroups, missingGroups, missingRoles, clusterType);
      appendMasterGroup(missingGroups, missingRoles, clusterType);
      return missingGroups;
   }

   private void appendMasterGroup(Set<NodeGroupCreate> nodeGroups, EnumSet<HadoopRole> missingRoles, Cluster.ClusterType clusterType) {
      if (missingRoles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE)
            || missingRoles.contains(HadoopRole.HADOOP_NAMENODE_ROLE)) {
         logger.debug("master roles " + missingRoles + " is missing. add master node group.");
      } else {
         return;
      }

      Map<GroupType, NodeGroupCreate> templateGroupMaps = TemplateClusterSpec.getTemplateGroupAttributes();
      GroupType groupType = null;
      NodeGroupCreate group;
      NodeGroupCreate templateGroup = null;
      if (missingRoles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE)
            && missingRoles.contains(HadoopRole.HADOOP_NAMENODE_ROLE)) {
         logger.debug("master roles are all missing. add default master node group.");
         groupType = GroupType.MASTER_GROUP;
         templateGroup = templateGroupMaps.get(groupType);
         if (clusterType == Cluster.ClusterType.HADOOP){
            removeHbaseRole(templateGroup, groupType);
         }
         group = new NodeGroupCreate(templateGroup);
      } else {
         List<String> roles = new ArrayList<String>();
         if (missingRoles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE)) {
            logger.debug("hadoop_jobtracker role is missing. add node group contains this role only.");
            groupType = GroupType.MASTER_JOBTRACKER_GROUP;
            roles.add(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString());
         } else {
            logger.debug("hadoop_jobtracker role is missing. add node group contains this role only.");
            groupType = GroupType.MASTER_GROUP;
            roles.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
         }
         if (clusterType == Cluster.ClusterType.HBASE){
            roles.add(HadoopRole.HBASE_MASTER_ROLE.toString());
         }
         groupType = GroupType.MASTER_GROUP;
         templateGroup = templateGroupMaps.get(groupType);
         group = new NodeGroupCreate(templateGroup);
         group.setRoles(roles);
      }
      group.setName("expanded_master");
      nodeGroups.add(group);
      missingRoles.remove(HadoopRole.HADOOP_JOBTRACKER_ROLE);
      missingRoles.remove(HadoopRole.HADOOP_NAMENODE_ROLE);
   }

   private void appendWorkerGroup(Set<NodeGroupEntity> nodeGroups, Set<NodeGroupCreate> missingGroups,
         EnumSet<HadoopRole> missingRoles, Cluster.ClusterType clusterType) {
      Map<GroupType, NodeGroupCreate> templateGroupMaps = TemplateClusterSpec.getTemplateGroupAttributes();
      if (missingRoles.contains(HadoopRole.HADOOP_DATANODE) &&
            missingRoles.contains(HadoopRole.HADOOP_TASKTRACKER)) {
         logger.debug("datanode and tasktracker roles are missing. add default worker node group.");
         GroupType groupType = GroupType.WORKER_GROUP;
         NodeGroupCreate nodeGroupCreate = templateGroupMaps.get(groupType);
         if (clusterType == Cluster.ClusterType.HADOOP) {
            removeHbaseRole(nodeGroupCreate, groupType);
         }
         NodeGroupCreate group = new NodeGroupCreate(nodeGroupCreate);
         group.setName("expanded_worker");
         missingGroups.add(group);
         missingRoles.remove(HadoopRole.HADOOP_DATANODE);
      } else if (missingRoles.contains(HadoopRole.HADOOP_DATANODE) ||
            missingRoles.contains(HadoopRole.HADOOP_TASKTRACKER)) {
         logger.debug("datanode or tasktracker role is missing. add missing role into existing node group.");
         for (NodeGroupEntity entity : nodeGroups) {
            List<String> strRoles = entity.getRoleNameList();
            if (strRoles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())) {
               strRoles.add(HadoopRole.HADOOP_DATANODE.toString());
            } else if (strRoles.contains(HadoopRole.HADOOP_DATANODE.toString())) {
               strRoles.add(HadoopRole.HADOOP_TASKTRACKER.toString());
            }
            if (clusterType == Cluster.ClusterType.HBASE){
               strRoles.add(HadoopRole.HBASE_REGIONSERVER_ROLE.toString());
            }
            entity.setRoles(new Gson().toJson(strRoles));
         }
      }
   }

   private void removeHbaseRole(NodeGroupCreate group,GroupType type){
      if (type == GroupType.MASTER_GROUP){
         group.getRoles().remove(HadoopRole.HBASE_MASTER_ROLE.toString());
      } else if (type == GroupType.WORKER_GROUP){
         group.getRoles().remove(HadoopRole.HBASE_REGIONSERVER_ROLE.toString());
      } else if (type == GroupType.CLIENT_GROUP){
         group.getRoles().remove(HadoopRole.HBASE_CLIENT_ROLE.toString());
      }
  }

}
