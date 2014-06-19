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
package com.vmware.bdd.software.mgmt.plugin.impl;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.*;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.ServiceType;

import java.net.URI;
import java.util.*;


public class DefaultSoftwareManagerImpl implements SoftwareManager {
   @Override
   public String getName() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getDescription() {
      // TODO Auto-generated method stub
      return null;
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
   public Set<String> getSupportedRoles() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<HadoopStack> getSupportedStacks() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getSupportedConfigs(HadoopStack stack) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean validateBlueprint(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      //TODO(qjin): validate MAPR ?
      if (blueprint.getHadoopStack().getDistro().equalsIgnoreCase("MAPR")) {
         return true;
      }
      return validateNoneMaprConfigurations();
   }

   //TODO(qjin): add non-mapr configuration validations here( hdfsUrl? )
   private boolean validateNoneMaprConfigurations() {
      return true;
   }

   /**
    * Validate role's existence
    * 
    * @param blueprint
    * @param distroRoles
    * @return
    * @throws SoftwareManagementPluginException
    */
   @Override
   public boolean validateRoles(ClusterBlueprint blueprint,
         List<String> distroRoles) throws SoftwareManagementPluginException {
      assert (blueprint != null && distroRoles != null);
      List<String> failedMsgList = new ArrayList<String>();
      // only check roles validity in server side, but not in CLI and GUI, because roles info exist in server side.
      checkUnsupportedRoles(blueprint, distroRoles, failedMsgList);
      validateRoleDependency(failedMsgList, blueprint);

      if (!failedMsgList.isEmpty()) {
         SoftwareManagementPluginException e =
               new SoftwareManagementPluginException();
         e.getFailedMsgList().add(failedMsgList.toString());
         throw e;
      }
      return true;
   }

   @Override
   public boolean validateCliConfigurations(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException {
      return true;
   }

   /**
    * Check whether the roles used in the cluster exist in distro manifest and
    * Chef Server.
    * 
    */
   private void checkUnsupportedRoles(ClusterBlueprint blueprint,
         List<String> distroRoles, List<String> failedMsgList) {
      List<NodeGroupInfo> nodeGroupInfos = blueprint.getNodeGroups();
      assert (nodeGroupInfos != null && !nodeGroupInfos.isEmpty());

      for (NodeGroupInfo nodeGroup : nodeGroupInfos) {
         List<String> roles = nodeGroup.getRoles();
         if (roles != null) {
            for (String role : roles) {
               StringBuilder rolesMsg = new StringBuilder();
               if (!ChefServerUtils.isValidRole(role)) {
                  rolesMsg.append("role ").append(role)
                        .append(" doesn't exist");
               } else if (!distroRoles.contains(role)
                     && !HadoopRole.isCustomizedRole(role)) {
                  rolesMsg.append("role ").append(role)
                        .append(" is not supported by distro ")
                        .append(blueprint.getHadoopStack().getDistro());
               }
               if (rolesMsg.length() > 0) {
                  failedMsgList.add(rolesMsg.toString());
               }
            }
         }
      }
   }

   /*
   * Validate role dependency:
   * Case 1: compute node group with external hdfs node group.
   * Case 2: The dependency check of HDFS, MapReduce, HBase, Zookeeper,
   * Hadoop Client(Pig, Hive, Hadoop Client), and HBase Client Combinations. The rules are below:
   * - HDFS includes roles of "haddop_namenode" and "hadoop_datanode";
   * - MapReduce includes roles of "haddop_jobtracker" and "hadoop_takstracker";
   * - HBase includes roles of "hbase_master" and "hbase_regionserver;
   * - Zookeeper includes a single role of "zookeeper";
   * - Hadoop Client includes roles of "hadoop_client";
   * - HBase client includes roles of "hbase_client";
   * - Pig includes roles of "pig";
   * - Hive includes roles of "hive";
   * - Hive Server includes roles of "hive_server";
   * - MapReduce depends on HDFS, HBase depends on HDFS and Zookeeper;
   * - Pig, Hive, Hive Server depends on MapReduce, HBase Client depends on HBase;
   * - Hadoop Client depends on HDFS.
   */
   public boolean validateRoleDependency(List<String> failedMsgList,
         ClusterBlueprint blueprint) {
      boolean valid = true;
      Set<String> roles = new HashSet<String>();
      List<NodeGroupInfo> nodeGroups = blueprint.getNodeGroups();
      if (nodeGroups == null) {
         return false;
      }

      for (NodeGroupInfo nodeGroupCreate : nodeGroups) {
         List<String> nodeGroupRoles = nodeGroupCreate.getRoles();
         if (nodeGroupRoles == null || nodeGroupRoles.isEmpty()) {
            valid = false;
            failedMsgList.add("Missing role attribute for node group "
                  + nodeGroupCreate.getName() + ".");
         } else {
            roles.addAll(nodeGroupCreate.getRoles());
         }
      }

      if (validateHDFSUrl(blueprint)) {
         if (nodeGroups == null) {
            valid = false;
            failedMsgList.add("Missing JobTracker or TaskTracker role.");
         } else {
            if (roles.contains("hadoop_namenode")
                  || roles.contains("hadoop_datanode")) {
               valid = false;
               failedMsgList.add("Duplicate NameNode or DataNode role.");
            }
            if (!roles.contains("hadoop_jobtracker")
                  && !roles.contains("hadoop_resourcemanager")) {
               valid = false;
               failedMsgList.add("Missing JobTracker or ResourceManager role.");
            }
            if (!roles.contains("hadoop_tasktracker")
                  && !roles.contains("hadoop_nodemanager")) {
               valid = false;
               failedMsgList.add("Missing TaskTracker or NodeManager role.");
            }
         }
      } else { //case 2
         // get involved service types of the spec file
         EnumSet<ServiceType> serviceTypes = EnumSet.noneOf(ServiceType.class);
         for (ServiceType service : ServiceType.values()) {
            //identify partially match
            int matched = 0;
            for (HadoopRole role : service.getRoles()) {
               if (roles.contains(role.toString())) {
                  matched++;
               }
            }
            if (matched == service.getRoles().size()) {
               serviceTypes.add(service);
            } else if (matched != 0) {
               failedMsgList.add("Cannot find one or more roles in " + service
                     + " " + service.getRoles()
                     + " in the cluster specification file.");
               valid = false;
            }
         }

         boolean isYarn = serviceTypes.contains(ServiceType.YARN);
         if (isYarn && serviceTypes.contains(ServiceType.MAPRED)) {
            failedMsgList.add("You cannot set " + ServiceType.MAPRED + " "
                  + ServiceType.MAPRED.getRoles() + " and " + ServiceType.YARN
                  + " " + ServiceType.YARN.getRoles() + " \nat the same time.");
            valid = false;
         }
         //validate the relationships of services
         if (valid == true && !serviceTypes.isEmpty()) {
            for (ServiceType service : serviceTypes) {
               EnumSet<ServiceType> dependency = service.depend(isYarn);
               if (dependency != null && !serviceTypes.containsAll(dependency)) {
                  failedMsgList.add("Some dependent services " + dependency
                        + " " + service
                        + " relies on cannot be found in the spec file.");
                  valid = false;
               }
            }
         }
      }

      return valid;
   }

   private boolean validateHDFSUrl(ClusterBlueprint blueprint) {
      if (blueprint.getExternalHDFS() != null) {
         try {
            URI uri = new URI(blueprint.getExternalHDFS());
            if (!"hdfs".equalsIgnoreCase(uri.getScheme())
                  || uri.getHost() == null) {
               return false;
            }
            return true;
         } catch (Exception ex) {
            ex.printStackTrace();
            return false;
         }
      }
      return false;
   }

   @Override
   public boolean createCluster(ClusterBlueprint blueprint) throws Exception {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean reconfigCluster(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean deleteCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onStopCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean onDeleteCluster(String clusterName) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean decomissionNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean comissionNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean startNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean stopNodes(String clusterName, List<NodeInfo> nodes) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String exportBlueprint(String clusterName) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String queryClusterStatus(ClusterBlueprint blueprint) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<String> validateScaling(NodeGroupInfo group) {
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
      // TODO Auto-generated method stub

   }
}
