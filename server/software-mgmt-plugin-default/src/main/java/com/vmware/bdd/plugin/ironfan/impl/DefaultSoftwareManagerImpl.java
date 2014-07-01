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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.spectypes.HadoopRole;


public class DefaultSoftwareManagerImpl implements SoftwareManager {
   private static final Logger logger = Logger.getLogger(DefaultSoftwareManagerImpl.class);
   private ClusterValidator validator;
   private InfrastructureUpdator updator;

   public DefaultSoftwareManagerImpl() {
      validator = new ClusterValidator();
      updator = new InfrastructureUpdator();
      updator.setValidator(validator);
   }

   @Override
   public String getName() {
      return "Default";
   }

   @Override
   public String getDescription() {
      return "Default software management tool";
   }

   @Override
   public String getType() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean echo() {
      // TODO Auto-generated method stub
      return true;

   }

   @Override
   public HealthStatus getStatus() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<String> getSupportedRoles() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint, List<String> distroRoles)
         throws ValidationException {
      return validator.validateBlueprint(blueprint, distroRoles);
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteCluster(String clusterName,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ClusterReport queryClusterStatus(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<String> validateScaling(NodeGroupInfo group) 
   throws SoftwareManagementPluginException {
      // resize of job tracker and name node is not supported
      List<String> roles = group.getRoles();
      List<String> unsupportedRoles = new ArrayList<String>();
      if (roles.isEmpty()) {
         // no unsupported roles
         return new ArrayList<String>();
      }
      if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HADOOP_NAMENODE_ROLE.toString());
      }
      if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString());
      }
      if (roles.contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
         unsupportedRoles.add(HadoopRole.ZOOKEEPER_ROLE.toString());
      }
      return unsupportedRoles;
   }

   @Override
   public void updateInfrastructure(ClusterBlueprint blueprint) {
      updator.updateInfrastructure(blueprint);
   }

   @Override
   public boolean hasHbase(ClusterBlueprint blueprint) {
      boolean hasHbase = false;
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         if (HadoopRole.hasHBaseRole(group.getRoles())) {
            hasHbase = true;
            break;
         }
      }
      return hasHbase;
   }

   @Override
   public boolean hasMgmtRole(List<String> roles) {
      return HadoopRole.hasMgmtRole(roles);
   }

   @Override
   public boolean isComputeOnlyRoles(List<String> roles) {
      if (roles != null
            && ((roles.size() == 1 && roles.get(0).equals(
                  HadoopRole.HADOOP_TASKTRACKER.toString()))
                  || (roles.size() == 2
                        && roles.contains(HadoopRole.HADOOP_TASKTRACKER
                              .toString()) && roles
                              .contains(HadoopRole.TEMPFS_CLIENT_ROLE.toString())) || (roles
                                    .contains(HadoopRole.MAPR_TASKTRACKER_ROLE.toString()) && !roles
                                    .contains(HadoopRole.MAPR_NFS_ROLE.toString())))) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean twoDataDisksRequired(NodeGroupInfo group) {
      EnumSet<HadoopRole> enumRoles = HadoopRole.getEnumRoles(group.getRoles(), new ArrayList<String>());
      if ((enumRoles.size() == 1 || (enumRoles.size() == 2 && enumRoles
            .contains(HadoopRole.HADOOP_JOURNALNODE_ROLE)))
            && (enumRoles.contains(HadoopRole.ZOOKEEPER_ROLE) || enumRoles
                  .contains(HadoopRole.MAPR_ZOOKEEPER_ROLE))) {
         return true;
      }
      return false;
   }
}

