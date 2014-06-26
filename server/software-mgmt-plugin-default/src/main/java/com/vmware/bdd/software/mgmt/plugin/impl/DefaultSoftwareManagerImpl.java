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

import java.net.URI;
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
import com.vmware.bdd.apitypes.DiskSplitPolicy;
import com.vmware.bdd.apitypes.InstanceType;
import com.vmware.bdd.apitypes.PlacementPolicy;
import com.vmware.bdd.apitypes.PlacementPolicy.GroupAssociation;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.exception.ClusterConfigException;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ChefServerUtils;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;
import com.vmware.bdd.software.mgmt.plugin.utils.ExpandUtils;
import com.vmware.bdd.spectypes.GroupType;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.spectypes.HadoopRole.RoleComparactor;
import com.vmware.bdd.spectypes.ServiceType;
import com.vmware.bdd.utils.AppConfigValidationUtils;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.ValidateResult;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;


public class DefaultSoftwareManagerImpl implements SoftwareManager {
   private static final Logger logger = Logger.getLogger(DefaultSoftwareManagerImpl.class);
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
      if (blueprint.getHadoopStack().getDistro().equalsIgnoreCase("MAPR")) {
         return true;
      }
      return validateNoneMaprDistros(blueprint, distroRoles);
   }

   private boolean validateNoneMaprDistros(ClusterBlueprint blueprint,
         List<String> distroRoles) throws ValidationException {
      validateClusterConfig(blueprint);
      return validateRoles(blueprint, distroRoles);
   }

   private void validateClusterConfig(ClusterBlueprint blueprint) {
      validateHadoopConfig(blueprint.getConfiguration(), blueprint.isNeedToValidateConfig());
      for (NodeGroupInfo group : blueprint.getNodeGroups()) {
         validateHadoopConfig(group.getConfiguration(), blueprint.isNeedToValidateConfig());
      }
   }

   /**
    * Validate role's existence
    * 
    * @param blueprint
    * @param distroRoles
    * @return
    * @throws SoftwareManagementPluginException
    */
   private boolean validateRoles(ClusterBlueprint blueprint,
         List<String> distroRoles) throws ValidationException {
      assert (blueprint != null && distroRoles != null);
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      // only check roles validity in server side, but not in CLI and GUI, because roles info exist in server side.
      checkUnsupportedRoles(blueprint, distroRoles, failedMsgList);
      boolean result = validateRoleDependency(failedMsgList, blueprint);
      validateGroupConfig(blueprint, failedMsgList, warningMsgList);
      if (!failedMsgList.isEmpty() || !warningMsgList.isEmpty()) {
         ValidationException e =
               new ValidationException(null, null);
         e.getFailedMsgList().addAll(failedMsgList);
         e.getWarningMsgList().addAll(warningMsgList);
         throw e;
      }
      return result;
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
         } else if(!roles.contains("hbase_master")){
            // TODO: consider Ambari need specify hadoop_namenode role when use of external HDFS
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

   public void setHadoopConfFromExternalHDFS(ClusterBlueprint blueprint) {
      if (blueprint.getExternalHDFS() != null) {
         if (validateHDFSUrl(blueprint)) {
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

   private void collectInstanceNumInvalidateMsg(NodeGroupInfo nodeGroup,
         List<String> failedMsgList) {
      failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
            .append(".").append("instanceNum=")
            .append(nodeGroup.getInstanceNum()).append(".").toString());
   }

   public void validateGroupConfig(ClusterBlueprint blueprint,
         List<String> failedMsgList, List<String> warningMsgList) {
      List<NodeGroupInfo> nodeGroups = blueprint.getNodeGroups();
      // if hadoop2 namenode ha is enabled
      boolean namenodeHACheck = false;
      //role count
      int masterCount = 0, jobtrackerCount = 0, resourcemanagerCount = 0, hbasemasterCount =
            0, zookeeperCount = 0, workerCount = 0, numOfJournalNode = 0;
      for (NodeGroupInfo nodeGroup : nodeGroups) {
            // get node group role.
            List<NodeGroupRole> groupRoles = getNodeGroupRoles(nodeGroup);
            if (groupRoles != null) {
               for (NodeGroupRole role : groupRoles) {
                  switch (role) {
                  case MASTER:
                     masterCount++;
                     int numOfInstance = nodeGroup.getInstanceNum();
                     if (numOfInstance >= 0 && numOfInstance != 1) {
                        if (numOfInstance != 2) { //namenode ha only support 2 nodes currently
                           collectInstanceNumInvalidateMsg(nodeGroup,
                                 failedMsgList);
                        } else {
                           namenodeHACheck = true;
                        }
                     }
                     break;
                  case JOB_TRACKER:
                     jobtrackerCount++;
                     if (nodeGroup.getInstanceNum() >= 0
                           && nodeGroup.getInstanceNum() != 1) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_JOBTRACKER);
                     }
                     break;
                  case RESOURCEMANAGER:
                     resourcemanagerCount++;
                     if (nodeGroup.getInstanceNum() >= 0
                           && nodeGroup.getInstanceNum() != 1) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_RESOURCEMANAGER);
                     }
                     break;
                  case HBASE_MASTER:
                     hbasemasterCount++;
                     if (nodeGroup.getInstanceNum() == 0) {
                        collectInstanceNumInvalidateMsg(nodeGroup,
                              failedMsgList);
                     }
                     break;
                  case ZOOKEEPER:
                     zookeeperCount++;
                     if (nodeGroup.getInstanceNum() > 0
                           && nodeGroup.getInstanceNum() < 3) {
                        failedMsgList.add(Constants.WRONG_NUM_OF_ZOOKEEPER);
                     } else if (nodeGroup.getInstanceNum() > 0
                           && nodeGroup.getInstanceNum() % 2 == 0) {
                        warningMsgList.add(Constants.ODD_NUM_OF_ZOOKEEPER);
                     }
                     break;
                  case JOURNAL_NODE:
                     numOfJournalNode += nodeGroup.getInstanceNum();
                     if (nodeGroup.getRoles().contains(
                           HadoopRole.HADOOP_DATANODE.toString())
                           || nodeGroup.getRoles().contains(
                                 HadoopRole.HADOOP_CLIENT_ROLE.toString())) {
                        failedMsgList
                        .add(Constants.DATA_CLIENT_NODE_JOURNALNODE_COEXIST);
                     }
                     break;
                  case WORKER:
                     workerCount++;
                     if (nodeGroup.getInstanceNum() == 0) {
                        collectInstanceNumInvalidateMsg(nodeGroup,
                              failedMsgList);
                     } else if (nodeGroup.isHaEnabled()) {
                        warningMsgList.add(Constants.WORKER_CLIENT_HA_FLAG);
                     }

                     //check if datanode and region server are seperate
                     List<String> roles = nodeGroup.getRoles();
                     if (roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE
                           .toString())
                           && !roles.contains(HadoopRole.HADOOP_DATANODE
                                 .toString())) {
                        warningMsgList
                        .add(Constants.REGISONSERVER_DATANODE_SEPERATION);
                     }
                     break;
                  case CLIENT:
                     if (nodeGroup.isHaEnabled()) {
                        warningMsgList.add(Constants.WORKER_CLIENT_HA_FLAG);
                     }
                     break;
                  case NONE:
                     // server side will validate whether the roles of this group exist
                     break;
                  default:
                     break;
                  }
               }
            }
         if (!supportedWithHdfs2(blueprint)) {
            if (namenodeHACheck || masterCount > 1) {
               failedMsgList.add(Constants.CURRENT_DISTRO_CAN_NOT_SUPPORT_HDFS2);
            }
         } else if (namenodeHACheck) {
            if (numOfJournalNode >= 0 && numOfJournalNode < 3) {
               failedMsgList.add(Constants.WRONG_NUM_OF_JOURNALNODE);
            } else if (numOfJournalNode > 0 && numOfJournalNode % 2 == 0) {
               warningMsgList.add(Constants.ODD_NUM_OF_JOURNALNODE);
            }
            //check if zookeeper exists for automatic namenode ha failover
            if (zookeeperCount == 0) {
               failedMsgList.add(Constants.NAMENODE_AUTO_FAILOVER_ZOOKEEPER);
            }
         }
         if ((jobtrackerCount > 1) || (resourcemanagerCount > 1)
               || (zookeeperCount > 1) || (hbasemasterCount > 1)) {
            failedMsgList.add(Constants.WRONG_NUM_OF_NODEGROUPS);
         }
         if (numOfJournalNode > 0 && !namenodeHACheck) {
            failedMsgList.add(Constants.NO_NAMENODE_HA);
         }
      }
      if (!warningMsgList.isEmpty() && !warningMsgList.get(0).startsWith("Warning: ")) {
         warningMsgList.set(0, "Warning: " + warningMsgList.get(0));
      }
   }

   // For HDFS2, apache, mapr, and gphd distros do not have hdfs2 features.
   public boolean supportedWithHdfs2(ClusterBlueprint blueprint) {
      String vendor = blueprint.getHadoopStack().getVendor();
      if (vendor != null && (vendor.equalsIgnoreCase(Constants.DEFAULT_VENDOR)
            || vendor.equalsIgnoreCase(Constants.MAPR_VENDOR)
            || vendor.equalsIgnoreCase(Constants.GPHD_VENDOR))){
         return false;
      }
      return true;
   }

   //define role of the node group .
   private enum NodeGroupRole {
      MASTER, JOB_TRACKER, RESOURCEMANAGER, WORKER, CLIENT, HBASE_MASTER, ZOOKEEPER, JOURNAL_NODE, NONE
   }


   private List<NodeGroupRole> getNodeGroupRoles(NodeGroupInfo nodeGroup) {
      List<NodeGroupRole> groupRoles = new ArrayList<NodeGroupRole>();
      //Find roles list from current  NodeGroupCreate instance.
      List<String> roles = nodeGroup.getRoles();
      for (NodeGroupRole role : NodeGroupRole.values()) {
         if (roles != null && matchRole(role, roles)) {
            groupRoles.add(role);
         }
      }
      if (groupRoles.size() == 0) {
         groupRoles.add(NodeGroupRole.NONE);
      }
      return groupRoles;
   }

   /**
    * Check the roles was introduced, whether matching with system's specialize
    * role.
    */
   private boolean matchRole(NodeGroupRole role, List<String> roles) {
      switch (role) {
         case MASTER:
            if (roles.contains(HadoopRole.HADOOP_NAMENODE_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case JOB_TRACKER:
            if (roles.contains(HadoopRole.HADOOP_JOBTRACKER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case RESOURCEMANAGER:
            if (roles.contains(HadoopRole.HADOOP_RESOURCEMANAGER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case HBASE_MASTER:
            if (roles.contains(HadoopRole.HBASE_MASTER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case ZOOKEEPER:
            if (roles.contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case JOURNAL_NODE:
            if (roles.contains(HadoopRole.HADOOP_JOURNALNODE_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case WORKER:
            if (roles.contains(HadoopRole.HADOOP_DATANODE.toString())
                  || roles.contains(HadoopRole.HADOOP_TASKTRACKER.toString())
                  || roles.contains(HadoopRole.HBASE_REGIONSERVER_ROLE.toString())
                  || roles.contains(HadoopRole.HADOOP_NODEMANAGER_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case CLIENT:
            if (roles.contains(HadoopRole.HADOOP_CLIENT_ROLE.toString())
                  || roles.contains(HadoopRole.HIVE_ROLE.toString())
                  || roles.contains(HadoopRole.HIVE_SERVER_ROLE.toString())
                  || roles.contains(HadoopRole.PIG_ROLE.toString())
                  || roles.contains(HadoopRole.HBASE_CLIENT_ROLE.toString())) {
               return true;
            } else {
               return false;
            }
         case NONE:
            break;
         default:
            break;
      }
      return false;
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

   private void validateHadoopConfig(Map<String, Object> appConfigs, boolean checkWhiteList) {
      if (appConfigs == null || appConfigs.isEmpty())
         return;
      // validate hadoop config
      if (checkWhiteList) {
         logger.debug("Validate hadoop configuration in white list.");
         ValidateResult valid = AppConfigValidationUtils.validateConfig(ValidationType.WHITE_LIST, appConfigs);
         switch (valid.getType()) {
         case WHITE_LIST_INVALID_VALUE:
            throw ClusterConfigException.INVALID_APP_CONFIG_VALUE(valid.getFailureValues());
         case WHITE_LIST_INVALID_NAME:
            logger.warn("Hadoop configurations " + valid.getNoExistFileNames() + " " +valid.getFailureNames() + " not in white list.");
            break;
         default:
            logger.debug("Passed white list validation.");
            break;
         }
      }
      logger.debug("Validate hadoop configuration in black list.");
      ValidateResult valid = AppConfigValidationUtils.validateConfig(ValidationType.BLACK_LIST, appConfigs);
      switch (valid.getType()) {
      case NAME_IN_BLACK_LIST:
         logger.warn("Hadoop configurations " + valid.getFailureNames() + " in black list. The configuration for these parameters do not take effect.");
      default:
         logger.debug("Passed black list validation.");
         break;
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

