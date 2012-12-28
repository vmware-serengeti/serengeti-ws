/*****************************************************************************
 *   Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ****************************************************************************/
package com.vmware.bdd.cli.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.impala.hive.HiveCommands;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.ClusterRestClient;
import com.vmware.bdd.cli.rest.DistroRestClient;
import com.vmware.bdd.cli.rest.NetworkRestClient;
import com.vmware.bdd.spectypes.HadoopRole;
import com.vmware.bdd.utils.AppConfigValidationUtils;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;
import com.vmware.bdd.utils.ValidateResult;

@Component
public class ClusterCommands implements CommandMarker {
   @Autowired
   private DistroRestClient distroRestClient;

   @Autowired
   private NetworkRestClient networkRestClient;

   @Autowired
   private ClusterRestClient restClient;

   @Autowired
   private Configuration hadoopConfiguration;

   @Autowired
   private HiveCommands hiveCommands;

   private String hiveServerUrl;
   private String targetClusterName;

   @CliAvailabilityIndicator({ "cluster help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "cluster create", help = "Create a hadoop cluster")
   public void createCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "type" }, mandatory = false, help = "The cluster type is Hadoop or HBase") final String type,
         @CliOption(key = { "distro" }, mandatory = false, help = "A hadoop distro name") final String distro,
         @CliOption(key = { "specFile" }, mandatory = false, help = "The spec file name path") final String specFilePath,
         @CliOption(key = { "rpNames" }, mandatory = false, help = "Resource Pools for the cluster: use \",\" among names.") final String rpNames,
         @CliOption(key = { "dsNames" }, mandatory = false, help = "Datastores for the cluster: use \",\" among names.") final String dsNames,
         @CliOption(key = { "networkName" }, mandatory = false, help = "Network Name") final String networkName,
         @CliOption(key = { "topology" }, mandatory = false, help = "Please specify the topology type: HVE or RACK_AS_RACK or HOST_AS_RACK") final String topology,
         @CliOption(key = { "resume" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to resume cluster creation") final boolean resume,
         @CliOption(key = { "skipConfigValidation" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Skip cluster configuration validation. ") final boolean skipConfigValidation,
         @CliOption(key = { "yes" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Answer 'yes' to all Y/N questions. ") final boolean alwaysAnswerYes) {
      //validate the name
      if (name.indexOf("-") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_CLUSTER
                     + Constants.PARAM_NOT_CONTAIN_HORIZONTAL_LINE);
         return;
      } else if (name.indexOf(" ") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_CLUSTER
                     + Constants.PARAM_NOT_CONTAIN_BLANK_SPACE);
         return;
      }
      //process resume
      if (resume) {
         resumeCreateCluster(name);
         return;
      }

      // build ClusterCreate object
      ClusterCreate clusterCreate = new ClusterCreate();
      clusterCreate.setName(name);

      if (type != null) {
         ClusterType clusterType = ClusterType.getByDescription(type);
         if (clusterType == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
                  Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.INVALID_VALUE + " " + "type=" + type);
            return;
         }
         clusterCreate.setType(clusterType);
      } else if (specFilePath == null) {
         // create Hadoop (HDFS + MapReduce) cluster as default
         clusterCreate.setType(ClusterType.HDFS_MAPRED);
      }

      if (topology != null) {
         try {
            clusterCreate.setTopologyPolicy(TopologyType.valueOf(topology));
         } catch (IllegalArgumentException ex) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
                  Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.INVALID_VALUE + " " + "topologyType=" + topology);
            return;
         }
      } else {
         clusterCreate.setTopologyPolicy(TopologyType.NONE);
      }

      try {
         if (distro != null) {
            List<String> distroNames = getDistroNames();
            if (validName(distro, distroNames)) {
               clusterCreate.setDistro(distro);
            } else {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL, Constants.PARAM_DISTRO
                           + Constants.PARAM_NOT_SUPPORTED + distroNames);
               return;
            }
         } else {
            String defaultDistroName = clusterCreate.getDefaultDistroName(distroRestClient.getAll());
            if (CommandsUtils.isBlank(defaultDistroName)) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL, Constants.PARAM__NO_DEFAULT_DISTRO);
               return;
            } else {
               clusterCreate.setDistro(defaultDistroName);
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CREATE,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         return;
      }
      DistroRead distroRead = distroRestClient.get(clusterCreate.getDistro());
      clusterCreate.setDistroVendor(distroRead.getVendor());
      clusterCreate.setDistroVersion(distroRead.getVersion());
      if (rpNames != null) {
         List<String> rpNamesList = CommandsUtils.inputsConvert(rpNames);
         if (rpNamesList.isEmpty()) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.INPUT_RPNAMES_PARAM + Constants.MULTI_INPUTS_CHECK);
            return;
         } else {
            clusterCreate.setRpNames(rpNamesList);
         }
      }
      if (dsNames != null) {
         List<String> dsNamesList = CommandsUtils.inputsConvert(dsNames);
         if (dsNamesList.isEmpty()) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.INPUT_DSNAMES_PARAM + Constants.MULTI_INPUTS_CHECK);
            return;
         } else {
            clusterCreate.setDsNames(dsNamesList);
         }
      }
      List<String> failedMsgList = new ArrayList<String>();
      List<String> warningMsgList = new ArrayList<String>();
      List<String> networkNames = null;
      try {
         if (specFilePath != null) {
            ClusterCreate clusterSpec =
                  CommandsUtils.getObjectByJsonString(ClusterCreate.class, CommandsUtils.dataFromFile(specFilePath));
            clusterCreate.setExternalHDFS(clusterSpec.getExternalHDFS());
            clusterCreate.setNodeGroups(clusterSpec.getNodeGroups());
            clusterCreate.setConfiguration(clusterSpec.getConfiguration());
            validateConfiguration(clusterCreate, skipConfigValidation, warningMsgList);
            if (!validateHAInfo(clusterCreate.getNodeGroups())){
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAM_CLUSTER_SPEC_HA_ERROR + specFilePath);
               return;
            }
         }
         networkNames = getNetworkNames();
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CREATE,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         return;
      }

      if (networkNames.isEmpty()) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_NETWORK_NAME + Constants.PARAM_NOT_EXISTED);
         return;
      } else {
         if (networkName != null) {
            if (validName(networkName, networkNames)) {
               clusterCreate.setNetworkName(networkName);
            } else {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAM_NETWORK_NAME
                           + Constants.PARAM_NOT_SUPPORTED + networkNames);
               return;
            }
         } else {
            if (networkNames.size() == 1) {
               clusterCreate.setNetworkName(networkNames.get(0));
            } else {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAM_NETWORK_NAME
                           + Constants.PARAM_NOT_SPECIFIED);
               return;
            }
         }
      }

      // Validate that the specified file is correct json format and proper value.
      if (specFilePath != null && !clusterCreate.getDistro().equalsIgnoreCase(com.vmware.bdd.utils.Constants.MAPR_VENDOR)) {
         List<String> distroRoles = findDistroRoles(clusterCreate);
         clusterCreate.validateClusterCreate(failedMsgList, warningMsgList, distroRoles);
      }

      // give a warning message if both type and specFilePath are specified
      if (type != null && specFilePath != null) {
         warningMsgList.add(Constants.TYPE_SPECFILE_CONFLICT);
      }

      if (!failedMsgList.isEmpty()) {
         showFailedMsg(clusterCreate.getName(), failedMsgList);
         return;
      }

      // rest invocation
      try {
         if (!CommandsUtils.showWarningMsg(clusterCreate.getName(),
               Constants.OUTPUT_OBJECT_CLUSTER, Constants.OUTPUT_OP_CREATE,
               warningMsgList, alwaysAnswerYes)) {
            return;
         }
         restClient.create(clusterCreate);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_RESULT_CREAT);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CREATE,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   private List<String> findDistroRoles(ClusterCreate clusterCreate) {
      DistroRead distroRead = null;
      distroRead =
            distroRestClient
                  .get(clusterCreate.getDistro() != null ? clusterCreate
                        .getDistro() : Constants.DEFAULT_DISTRO);
      if (distroRead != null) {
         return distroRead.getRoles();
      } else {
         return null;
      }
   }

   @CliCommand(value = "cluster list", help = "Get cluster information")
   public void getCluster(
         @CliOption(key = { "name" }, mandatory = false, help = "The cluster name") final String name,
         @CliOption(key = { "detail" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show node information") final boolean detail) {

      // rest invocation
      try {
         if (name == null) {
            ClusterRead[] clusters = restClient.getAll(detail);
            if (clusters != null) {
               prettyOutputClustersInfo(clusters, detail);
            }
         } else {
            ClusterRead cluster = restClient.get(name, detail);
            if (cluster != null) {
               prettyOutputClusterInfo(cluster, detail);
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster export", help = "Export cluster specification")
   public void exportClusterSpec(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "output" }, mandatory = false, help = "The output file name") final String fileName) {

      // rest invocation
      try {
         ClusterCreate cluster = restClient.getSpec(name);
         if (cluster != null) {
            CommandsUtils.prettyJsonOutput(cluster, fileName);
         }
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_EXPORT, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster delete", help = "Delete a cluster")
   public void deleteCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name) {

      //rest invocation
      try {
         restClient.delete(name);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESULT_DELETE);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_DELETE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster start", help = "Start a cluster")
   public void startCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName) {

      Map<String, String> queryStrings = new HashMap<String, String>();
      queryStrings
            .put(Constants.QUERY_ACTION_KEY, Constants.QUERY_ACTION_START);

      //rest invocation
      try {
         restClient.actionOps(clusterName, queryStrings);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_RESULT_START);

      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster stop", help = "Stop a cluster")
   public void stopCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName) {
      Map<String, String> queryStrings = new HashMap<String, String>();
      queryStrings.put(Constants.QUERY_ACTION_KEY, Constants.QUERY_ACTION_STOP);

      //rest invocation
      try {
         restClient.actionOps(clusterName, queryStrings);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_RESULT_STOP);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_STOP, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster resize", help = "Resize a cluster")
   public void resizeCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "nodeGroup" }, mandatory = true, help = "The node group name") final String nodeGroup,
         @CliOption(key = { "instanceNum" }, mandatory = true, help = "The resized number of instances. It should be larger that existing one") final int instanceNum) {

      if (instanceNum > 1) {
         try {
            ClusterRead cluster = restClient.get(name, false);
            if (cluster == null) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_RESIZE,
                     Constants.OUTPUT_OP_RESULT_FAIL, "cluster " + name
                           + " does not exsit.");
               return;
            }
            //disallow scale out zookeeper node group.
            List<NodeGroupRead> ngs = cluster.getNodeGroups();
            boolean found = false;
            for (NodeGroupRead ng : ngs) {
               if (ng.getName().equals(nodeGroup)) {
                  found = true;
                  if (ng.getRoles() != null && ng.getRoles().contains(HadoopRole.ZOOKEEPER_ROLE.toString())) {
                     CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                           name, Constants.OUTPUT_OP_RESIZE,
                           Constants.OUTPUT_OP_RESULT_FAIL, Constants.ZOOKEEPER_NOT_RESIZE);
                     return;
                  }
                  break;
               }
            }

            if (!found) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_RESIZE,
                     Constants.OUTPUT_OP_RESULT_FAIL, "node group " + nodeGroup
                     + " does not exist.");
               return;
            }

            restClient.resize(name, nodeGroup, instanceNum);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_RESULT_RESIZE);
         } catch (CliRestException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_RESIZE,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      } else {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESIZE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.INVALID_VALUE + " instanceNum=" + instanceNum);
      }
   }

   @CliCommand(value = "cluster limit", help = "Set the cluster resources including stopping some cmopute nodes, set disk priorities")
   public void limitCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "nodeGroup" }, mandatory = false, help = "The node group name") final String nodeGroupName,
         @CliOption(key = { "activeComputeNodeNum" }, mandatory = false, help = "The number of instances powered on") final int activeComputeNodeNum,
         @CliOption(key = { "diskIOPriority" }, mandatory = false, help = "The relative disk I/O priorities: HIGH, NORNAL, LOW") final String diskIOPriority) {

      try {
         // The active compute node number must be an integer and cannot be less than zero.
         if (activeComputeNodeNum < 0) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_LIMIT,
                  Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE
                  + "activeComputeNodeNum=" + activeComputeNodeNum);
            return;
         }
         ClusterRead cluster = restClient.get(clusterName, false);
         if (cluster == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_LIMIT,
                  Constants.OUTPUT_OP_RESULT_FAIL, "cluster " + clusterName
                  + " does not exsit.");
            return;
         }
         if (activeComputeNodeNum > 0) {
            if (!cluster.validateLimit(nodeGroupName)) {
               return;
            } else {
               //step 1: stop some compute nodes
               restClient.limitCluster(clusterName, nodeGroupName, activeComputeNodeNum);
            }
         }
         Priority ioPriority = null;
         if (diskIOPriority != null) {
            ioPriority = Priority.valueOf(diskIOPriority.toUpperCase());
         }

         //step 2: set some resource values
         if (ioPriority != null) {
            String[] taskSummary = null;
            if (activeComputeNodeNum > 0) {
               taskSummary = new String[1];
               taskSummary[0] = Constants.OUTPUT_OBJECT_CLUSTER + " " + clusterName + " " + Constants.OUTPUT_OP_RESULT_STOP_COMPUTE_NODE;
            }
            restClient.prioritizeCluster(clusterName, nodeGroupName, ioPriority, taskSummary);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, clusterName, Constants.OUTPUT_OP_RESULT_PRIORITY);
         } else if (activeComputeNodeNum > 0) {
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, clusterName, Constants.OUTPUT_OP_RESULT_STOP_COMPUTE_NODE);
         } else { //show status
            ClusterRead clusterRead = restClient.get(clusterName, true);
            updateRunningNodes(clusterRead);
            prettyOutputDynamicResourceInfo(clusterRead);
         }
      } catch (IllegalArgumentException ex) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_LIMIT,
               Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE + " "
                     + "diskIOPriority=" + diskIOPriority);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_LIMIT,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   private void updateRunningNodes(ClusterRead clusterRead) {
      if (clusterRead != null) {
         List<NodeGroupRead>  ngs = clusterRead.getNodeGroups();
         if (ngs != null) {
            for (NodeGroupRead ng : ngs) {
               int count = 0;
               List<NodeRead>  nodes = ng.getInstances();
               if (nodes != null) {
                  for (NodeRead node : nodes) {
                     if (node.getStatus().equals(Constants.NODE_RUNNING_STATUS)) {
                        count++;
                     }
                  }
                  ng.setRunningNodesNum(count);
               }
            }
         }
      }
   }

   @CliCommand(value = "cluster unlimit", help = "Reset the cluster resources to the default values including start some compute nodes and reset disk priority to the normal level")
   public void unlimitCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "nodeGroup" }, mandatory = false, help = "The node group name") final String nodeGroupName,
         @CliOption(key = { "activeComputeNodes" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The flag to power on all compute nodes") final boolean activeComputeNodes,
         @CliOption(key = { "diskIOPriority" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "The relative disk I/O priorities: HIGH, NORNAL, LOW") final boolean diskIOPriority) {

      try {
         int activeComputeNodeNum = -2;
         if (activeComputeNodes) {
            activeComputeNodeNum = -1;
         }
         ClusterRead cluster = restClient.get(clusterName, false);
         if (cluster == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_UNLIMIT,
                  Constants.OUTPUT_OP_RESULT_FAIL, "cluster " + clusterName
                  + " does not exsit.");
            return;
         }
         //step 1: start all compute nodes
         if(activeComputeNodeNum == -1) {
            if (!cluster.validateLimit(nodeGroupName)) {
               return;
            } else {
               restClient.limitCluster(clusterName, nodeGroupName, activeComputeNodeNum);
            }
         }
         //step 2: reset resources to default values
         if(diskIOPriority) {
            String[] taskSummary = null;
            if (activeComputeNodeNum == -1) {
               taskSummary = new String[1];
               taskSummary[0] = Constants.OUTPUT_OBJECT_CLUSTER + " " + clusterName + " " + Constants.OUTPUT_OP_RESULT_START_COMPUTE_NODE;
            }
            restClient.prioritizeCluster(clusterName, nodeGroupName, Priority.NORMAL, taskSummary);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, clusterName, Constants.OUTPUT_OP_RESULT_PRIORITY_RESET);
         } else if (activeComputeNodeNum == -1){
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, clusterName, Constants.OUTPUT_OP_RESULT_START_COMPUTE_NODE);
         } else {
            System.out.println(Constants.AT_LEAST_ONE_OPTION);
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_UNLIMIT,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   @CliCommand(value = "cluster target", help = "Set or query target cluster to run commands")
   public void targetCluster(
         @CliOption(key = { "name" }, mandatory = false, help = "The cluster name") final String name,
         @CliOption(key = { "info" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show target information") final boolean info) {

      ClusterRead cluster = null;
      boolean noCluster = false;
      try {
         if (info) {
            if (name != null) {
               System.out.println("Warning: can't specify option --name and --info at the same time");
               return;
            }
            String fsUrl = hadoopConfiguration.get("fs.default.name");
            String jtUrl = hadoopConfiguration.get("mapred.job.tracker");
            if ((fsUrl == null || fsUrl.length() == 0) && (jtUrl == null || jtUrl.length() == 0)) {
               System.out.println("There is no targeted cluster. Please use \"cluster target --name\" to target first");
               return;
            }
            if(targetClusterName != null && targetClusterName.length() > 0){
               System.out.println("Cluster         : " + targetClusterName);            	
            }
            if (fsUrl != null && fsUrl.length() > 0) {
               System.out.println("HDFS url        : " + fsUrl);
            }
            if (jtUrl != null && jtUrl.length() > 0) {
               System.out.println("Job Tracker url : " + jtUrl);
            }
            if (hiveServerUrl != null && hiveServerUrl.length() > 0) {
               System.out.println("Hive server info: " + hiveServerUrl);
            }
         } else {
            if (name == null) {
               ClusterRead[] clusters = restClient.getAll(false);
               if (clusters != null && clusters.length > 0) {
                  cluster = clusters[0];
               }
               else {
                  noCluster = true;
               }
            } else {
               cluster = restClient.get(name, false);
            }

            if (cluster == null) {
               if(noCluster) {
                  System.out.println("There is no available cluster for targeting.");
               }
               else {
            	  System.out.println("Failed to target cluster: The cluster " + name + " not found"); 
               }
               setFsURL("");
               setJobTrackerURL("");
               this.setHiveServerUrl("");
            } else {
               targetClusterName = cluster.getName();
               boolean hasHDFS = false;
               boolean hasHiveServer = false;
               for (NodeGroupRead nodeGroup : cluster.getNodeGroups()) {
                  for (String role : nodeGroup.getRoles()) {
                     if (role.equals("hadoop_namenode")) {
                        List<NodeRead> nodes = nodeGroup.getInstances();
                        if (nodes != null && nodes.size() > 0) {
                           String nameNodeIP = nodes.get(0).getIp();
                           setNameNode(nameNodeIP);
                           hasHDFS = true;
                        } else {
                           throw new CliRestException("no name node available");
                        }
                     }
                     if (role.equals("hadoop_jobtracker")) {
                        List<NodeRead> nodes = nodeGroup.getInstances();
                        if (nodes != null && nodes.size() > 0) {
                           String jobTrackerIP = nodes.get(0).getIp();
                           setJobTracker(jobTrackerIP);
                        } else {
                           throw new CliRestException("no job tracker available");
                        }
                     }
                     if (role.equals("hive_server")) {
                        List<NodeRead> nodes = nodeGroup.getInstances();
                        if (nodes != null && nodes.size() > 0) {
                           String hiveServerIP = nodes.get(0).getIp();
                           setHiveServerAddress(hiveServerIP);
                           hasHiveServer = true;
                        } else {
                           throw new CliRestException("no hive server available");
                        }
                     }
                  }
               }
               if (cluster.getExternalHDFS() != null && !cluster.getExternalHDFS().isEmpty()) {
                  setFsURL(cluster.getExternalHDFS());
                  hasHDFS = true;
               }
               if(!hasHDFS){
            	   setFsURL("");
               }
               if(!hasHiveServer){
            	   this.setHiveServerUrl("");
               }
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_TARGET,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         setFsURL("");
         setJobTrackerURL("");
         this.setHiveServerUrl("");
      }
   }

   private void setNameNode(String nameNodeAddress) {
      String hdfsUrl = "hdfs://" + nameNodeAddress + ":8020";
      setFsURL(hdfsUrl);
   }

   private void setFsURL(String fsURL) {
      hadoopConfiguration.set("fs.default.name", fsURL);
   }

   private void setJobTracker(String jobTrackerAddress) {
      String jobTrackerUrl = jobTrackerAddress + ":8021";
      setJobTrackerURL(jobTrackerUrl);      
   }

   private void setJobTrackerURL(String jobTrackerUrl){
	   hadoopConfiguration.set("mapred.job.tracker", jobTrackerUrl);
   }

   private void setHiveServerAddress(String hiveServerAddress) {
      try {
         hiveServerUrl = hiveCommands.config(hiveServerAddress, 10000, null);
      } catch (Exception e) {
         throw new CliRestException("faild to set hive server address");
      }
   }
   
   private void setHiveServerUrl(String hiveServerUrl) {
	   this.hiveServerUrl = hiveServerUrl;
   }

   @CliCommand(value = "cluster config", help = "Config an existing cluster")
   public void configCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "specFile" }, mandatory = true, help = "The spec file name path") final String specFilePath,
         @CliOption(key = { "skipConfigValidation" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Skip cluster configuration validation. ") final boolean skipConfigValidation,
         @CliOption(key = { "yes" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Answer 'yes' to all Y/N questions. ") final boolean alwaysAnswerYes) {
      //validate the name
      if (name.indexOf("-") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CONFIG,
               Constants.OUTPUT_OP_RESULT_FAIL, Constants.PARAM_CLUSTER + Constants.PARAM_NOT_CONTAIN_HORIZONTAL_LINE);
         return;
      }
      try {
         ClusterRead clusterRead = restClient.get(name, false);
         // build ClusterCreate object
         ClusterCreate clusterConfig = new ClusterCreate();
         clusterConfig.setName(clusterRead.getName());
         ClusterCreate clusterSpec =
               CommandsUtils.getObjectByJsonString(ClusterCreate.class, CommandsUtils.dataFromFile(specFilePath));
         clusterConfig.setNodeGroups(clusterSpec.getNodeGroups());
         clusterConfig.setConfiguration(clusterSpec.getConfiguration());
         clusterConfig.setExternalHDFS(clusterSpec.getExternalHDFS());
         List<String> warningMsgList = new ArrayList<String>();
         validateConfiguration(clusterConfig, skipConfigValidation, warningMsgList);
         // add a confirm message for running job
         warningMsgList.add("Warning: " + Constants.PARAM_CLUSTER_CONFIG_RUNNING_JOB_WARNING);
         if (!CommandsUtils.showWarningMsg(clusterConfig.getName(),
               Constants.OUTPUT_OBJECT_CLUSTER, Constants.OUTPUT_OP_CONFIG,
               warningMsgList, alwaysAnswerYes)) {
            return;
         }
         restClient.configCluster(clusterConfig);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_RESULT_CONFIG);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CONFIG,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         return;
      }
   }

   private void resumeCreateCluster(final String name) {
      Map<String, String> queryStrings = new HashMap<String, String>();
      queryStrings.put(Constants.QUERY_ACTION_KEY,
            Constants.QUERY_ACTION_RESUME);

      try {
         restClient.actionOps(name, queryStrings);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESULT_RESUME);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESUME, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   private List<String> getNetworkNames() {

      List<String> networkNames = new ArrayList<String>(0);

      NetworkRead[] networks = networkRestClient.getAll(false);

      if (networks != null) {
         for (NetworkRead network : networks)
            networkNames.add(network.getName());
      }
      return networkNames;
   }

   private List<String> getDistroNames() {

      List<String> distroNames = new ArrayList<String>(0);

      DistroRead[] distros = distroRestClient.getAll();

      if (distros != null) {
         for (DistroRead distro : distros)
            distroNames.add(distro.getName());
      }
      return distroNames;
   }

   private boolean validName(String inputName, List<String> validNames) {
      for (String name : validNames) {
         if (name.equals(inputName)) {
            return true;
         }
      }
      return false;
   }

   private void prettyOutputDynamicResourceInfo(ClusterRead cluster) {
      TopologyType topology = cluster.getTopologyPolicy();
      if (topology == null || topology == TopologyType.NONE) {
         System.out.printf("cluster name: %s, distro: %s, status: %s",
               cluster.getName(), cluster.getDistro(), cluster.getStatus());
      } else {
         System.out.printf("cluster name: %s, distro: %s, topology: %s, status: %s",
               cluster.getName(), cluster.getDistro(), topology, cluster.getStatus());
      }
      System.out.println();
      if(cluster.getExternalHDFS() != null && !cluster.getExternalHDFS().isEmpty()) {
         System.out.printf("external HDFS: %s\n", cluster.getExternalHDFS());
      }
      LinkedHashMap<String, List<String>> ngColumnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      List<NodeGroupRead> nodegroups = cluster.getNodeGroups();
      if (nodegroups != null) {
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_GROUP_NAME, Arrays.asList("getName"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_RUNNING_NODES, Arrays.asList("getRunningNodesNum"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_IOSHARES,
               Arrays.asList("getStorage", "getShares"));
         try {
            CommandsUtils.printInTableFormat(
                  ngColumnNamesWithGetMethodNames, nodegroups.toArray(),
                  Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  cluster.getName(), Constants.OUTPUT_OP_LIST,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      }
   }

   private void prettyOutputClusterInfo(ClusterRead cluster, boolean detail) {
      TopologyType topology = cluster.getTopologyPolicy();
      if (topology == null || topology == TopologyType.NONE) {
         System.out.printf("cluster name: %s, distro: %s, status: %s",
               cluster.getName(), cluster.getDistro(), cluster.getStatus());
      } else {
         System.out.printf("cluster name: %s, distro: %s, topology: %s, status: %s",
               cluster.getName(), cluster.getDistro(), topology, cluster.getStatus());
      }
      System.out.println();
      if(cluster.getExternalHDFS() != null && !cluster.getExternalHDFS().isEmpty()) {
         System.out.printf("external HDFS: %s\n", cluster.getExternalHDFS());
      }
      LinkedHashMap<String, List<String>> ngColumnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      List<NodeGroupRead> nodegroups = cluster.getNodeGroups();
      if (nodegroups != null) {
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_GROUP_NAME, Arrays.asList("getName"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_ROLES, Arrays.asList("getRoles"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_INSTANCE,
               Arrays.asList("getInstanceNum"));
         ngColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_CPU,
               Arrays.asList("getCpuNum"));
         ngColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_MEM,
               Arrays.asList("getMemCapacityMB"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_TYPE,
               Arrays.asList("getStorage", "getType"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_SIZE,
               Arrays.asList("getStorage", "getSizeGB"));

         try {
            if (detail) {
               LinkedHashMap<String, List<String>> nColumnNamesWithGetMethodNames =
                     new LinkedHashMap<String, List<String>>();
               nColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_NODE_NAME,
                     Arrays.asList("getName"));
               nColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_HOST,
                     Arrays.asList("getHostName"));
               if (topology == TopologyType.RACK_AS_RACK || topology == TopologyType.HVE) {
                  nColumnNamesWithGetMethodNames.put(
                        Constants.FORMAT_TABLE_COLUMN_RACK,
                        Arrays.asList("getRack"));
               }
               nColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_IP, Arrays.asList("getIp"));
               nColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_STATUS,
                     Arrays.asList("getStatus"));

               for (NodeGroupRead nodegroup : nodegroups) {
                  CommandsUtils.printInTableFormat(
                        ngColumnNamesWithGetMethodNames,
                        new NodeGroupRead[] { nodegroup },
                        Constants.OUTPUT_INDENT);
                  List<NodeRead> nodes = nodegroup.getInstances();
                  if (nodes != null) {
                     System.out.println();
                     CommandsUtils.printInTableFormat(
                           nColumnNamesWithGetMethodNames, nodes.toArray(),
                           new StringBuilder().append(Constants.OUTPUT_INDENT)
                                 .append(Constants.OUTPUT_INDENT).toString());
                  }
                  System.out.println();
               }
            } else
               CommandsUtils.printInTableFormat(
                     ngColumnNamesWithGetMethodNames, nodegroups.toArray(),
                     Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  cluster.getName(), Constants.OUTPUT_OP_LIST,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      }
   }

   private void prettyOutputClustersInfo(ClusterRead[] clusters, boolean detail) {
      for (ClusterRead cluster : clusters) {
         prettyOutputClusterInfo(cluster, detail);
         System.out.println();
      }
   }

   private void showFailedMsg(String name, List<String> failedMsgList) {
      //cluster creation failed message.
      StringBuilder failedMsg = new StringBuilder();
      failedMsg.append(Constants.INVALID_VALUE);
      if (failedMsgList.size() > 1) {
         failedMsg.append("s");
      }
      failedMsg.append(" ");
      StringBuilder tmpMsg = new StringBuilder();
      for (String msg : failedMsgList) {
         tmpMsg.append(",").append(msg);
      }
      tmpMsg.replace(0, 1, "");
      failedMsg.append(tmpMsg);
      failedMsg.append(".");
      CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
            Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
            failedMsg.toString());
   }

   private void validateConfiguration(ClusterCreate cluster, boolean skipConfigValidation, List<String> warningMsgList) {
      // validate blacklist
      ValidateResult blackListResult = validateBlackList(cluster);
      if (blackListResult != null) {
         addBlackListWarning(blackListResult, warningMsgList);
      }
      if (!skipConfigValidation) {
         // validate whitelist
         ValidateResult whiteListResult = validateWhiteList(cluster);
         addWhiteListWarning(cluster.getName(), whiteListResult, warningMsgList);
      } else {
         cluster.setValidateConfig(false);
      }
   }

   private ValidateResult validateBlackList(ClusterCreate cluster) {
      return validateConfiguration(cluster, ValidationType.BLACK_LIST);
   }

   private ValidateResult validateWhiteList(ClusterCreate cluster) {
      return validateConfiguration(cluster, ValidationType.WHITE_LIST);
   }

   /*
    * Validate a configuration of the cluster at first. Validate configurations of all of node groups then.
    * And merge the failed info which have been producted by validation between cluster level and node group
    * level.
    */
   private ValidateResult validateConfiguration(ClusterCreate cluster, ValidationType validationType) {
      ValidateResult validateResult = new ValidateResult();
      // validate cluster level Configuration
      ValidateResult vr = null;
      if (cluster.getConfiguration() != null && !cluster.getConfiguration().isEmpty()) {
         vr = AppConfigValidationUtils.validateConfig(validationType, cluster.getConfiguration());
         if (vr.getType() != ValidateResult.Type.VALID) {
            validateResult.setType(vr.getType());
            validateResult.setFailureNames(vr.getFailureNames());
            validateResult.setNoExistFileNames(vr.getNoExistFileNames());
         }
      }
      List<String> failureNames = new LinkedList<String>();
      Map<String,List<String>> noExistingFileNamesMap = new HashMap<String,List<String>>();
      failureNames.addAll(validateResult.getFailureNames());
      noExistingFileNamesMap.putAll(validateResult.getNoExistFileNames());
      // validate nodegroup level Configuration
      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (nodeGroup.getConfiguration() != null && !nodeGroup.getConfiguration().isEmpty()) {
            vr = AppConfigValidationUtils.validateConfig(validationType, nodeGroup.getConfiguration());
            if (vr.getType() != ValidateResult.Type.VALID) {
               validateResult.setType(vr.getType());
               // merge failed names between cluster level and node group level.
               for (String failureName : vr.getFailureNames()) {
                  if (!failureNames.contains(failureName)) {
                     failureNames.add(failureName);
                  }
               }
               // merge no existing file names between cluster level and node group level 
               for (Entry<String, List<String>> noExistingFileNames : vr.getNoExistFileNames().entrySet()) {
                  String configType = noExistingFileNames.getKey();
                  if (noExistingFileNamesMap.containsKey(configType)) {
                     List<String> noExistingFilesTemp = noExistingFileNames.getValue();
                     List<String> noExistingFiles = noExistingFileNamesMap.get(configType);
                     for (String fileName : noExistingFilesTemp) {
                        if (!noExistingFiles.contains(fileName)) {
                           noExistingFiles.add(fileName);
                        }
                     }
                     noExistingFileNamesMap.put(configType, noExistingFiles);
                  } else {
                     noExistingFileNamesMap.put(configType, noExistingFileNames.getValue());
                  }
               }
            }
         }
      }
      validateResult.setFailureNames(failureNames);
      validateResult.setNoExistFileNames(noExistingFileNamesMap);
      return validateResult;
   }

   private void addWhiteListWarning(final String clusterName, ValidateResult whiteListResult,
         List<String> warningMsgList) {
      if(whiteListResult.getType() == ValidateResult.Type.WHITE_LIST_NO_EXIST_FILE_NAME) {
         String noExistingWarningMsg = getValidateWarningMsg(whiteListResult.getNoExistFileNames());
         if (warningMsgList != null) {
            warningMsgList.add(noExistingWarningMsg);
         }
      }else if (whiteListResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_NAME) {
         String noExistingWarningMsg = getValidateWarningMsg(whiteListResult.getNoExistFileNames());
         String failureNameWarningMsg = getValidateWarningMsg(whiteListResult.getFailureNames(),
               Constants.PARAM_CLUSTER_NOT_IN_WHITE_LIST_WARNING);
         if (warningMsgList != null) {
            warningMsgList.add(noExistingWarningMsg);
            warningMsgList.add(failureNameWarningMsg);
         }
      }
   }

   private void addBlackListWarning(ValidateResult blackListResult, List<String> warningList) {
      if (blackListResult.getType() == ValidateResult.Type.NAME_IN_BLACK_LIST) {
         String warningMsg = getValidateWarningMsg(blackListResult.getFailureNames(), 
               Constants.PARAM_CLUSTER_IN_BLACK_LIST_WARNING + Constants.PARAM_CLUSTER_NOT_TAKE_EFFECT);
         if (warningList != null) {
            warningList.add(warningMsg);
         }
      }
   }

   private String getValidateWarningMsg(List<String> failureNames, String warningMsg) {
      StringBuilder warningMsgBuff = new StringBuilder();
      if (failureNames != null && !failureNames.isEmpty()) {
         warningMsgBuff.append("Warning: ");
         for (String failureName : failureNames) {
            warningMsgBuff.append(failureName).append(", ");
         }
         warningMsgBuff.delete(warningMsgBuff.length() - 2, warningMsgBuff.length());
         if (failureNames.size() > 1) {
            warningMsgBuff.append(" are ");
         } else {
            warningMsgBuff.append(" is ");
         }
         warningMsgBuff.append(warningMsg);
      }
      return warningMsgBuff.toString();
   }

   private String getValidateWarningMsg(Map<String,List<String>> noExistingFilesMap) {
      StringBuilder warningMsgBuff = new StringBuilder();
      if (noExistingFilesMap != null && !noExistingFilesMap.isEmpty()) {
         warningMsgBuff.append("Warning: ");
         for (Entry<String, List<String>> noExistingFilesEntry : noExistingFilesMap.entrySet()) {
            List<String> noExistingFileNames = noExistingFilesEntry.getValue();
            for (String noExistingFileName : noExistingFileNames) {
               warningMsgBuff.append(noExistingFileName).append(", ");
            }
            warningMsgBuff.delete(warningMsgBuff.length() - 2, warningMsgBuff.length());
            if (noExistingFileNames.size() > 1) {
               warningMsgBuff.append(" are ");
            } else {
               warningMsgBuff.append(" is ");
            }
            warningMsgBuff.append("not existing in ");
            warningMsgBuff.append(noExistingFilesEntry.getKey()+ " scope , ");
         }
         warningMsgBuff.replace(warningMsgBuff.length() - 2, warningMsgBuff.length(),". ");
         warningMsgBuff.append(Constants.PARAM_CLUSTER_NOT_TAKE_EFFECT);
      }
      return warningMsgBuff.toString();
   }

   private boolean validateHAInfo(NodeGroupCreate[] nodeGroups) {
      List<String> haFlagList = Arrays.asList("off","on","ft");
      if (nodeGroups != null){
         for(NodeGroupCreate group : nodeGroups){
            if (!haFlagList.contains(group.getHaFlag().toLowerCase())){
               return false;
            }
         }
      }
      return true;
   }

}
