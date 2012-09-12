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

import jline.ConsoleReader;

import org.apache.hadoop.conf.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.impala.hive.HiveCommands;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.Cluster.ClusterType;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.ClusterRestClient;
import com.vmware.bdd.cli.rest.DistroRestClient;
import com.vmware.bdd.cli.rest.NetworkRestClient;
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

   private String hiveInfo;
   private String targetClusterName;

   private boolean alwaysAnswerYes;

   //define role of the node group .
   private enum NodeGroupRole {
      MASTER, WORKER, CLIENT, NONE
   }

   @CliAvailabilityIndicator({ "cluster help" })
   public boolean isCommandAvailable() {
      return true;
   }

   @CliCommand(value = "cluster create", help = "Create a hadoop cluster")
   public void createCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "distro" }, mandatory = false, help = "Hadoop Distro") final String distro,
         @CliOption(key = { "specFile" }, mandatory = false, help = "The spec file name path") final String specFilePath,
         @CliOption(key = { "rpNames" }, mandatory = false, help = "Resource Pools for the cluster: use \",\" among names.") final String rpNames,
         @CliOption(key = { "dsNames" }, mandatory = false, help = "Datastores for the cluster: use \",\" among names.") final String dsNames,
         @CliOption(key = { "networkName" }, mandatory = false, help = "Network Name") final String networkName,
         @CliOption(key = { "topology" }, mandatory = false, help = "Please specify the topology type: HVE or RACK_HOST or HOST_AS_RACK") final String topology,
         @CliOption(key = { "resume" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to resume cluster creation") final boolean resume,
         @CliOption(key = { "skipConfigValidation" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Skip cluster configuration validation. ") final boolean skipConfigValidation,
         @CliOption(key = { "yes" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Answer 'yes' to all Y/N questions. ") final boolean alwaysAnswerYes) {

      this.alwaysAnswerYes = alwaysAnswerYes;
      //validate the name
      if (name.indexOf("-") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_CLUSTER
                     + Constants.PARAM_NOT_CONTAIN_HORIZONTAL_LINE);

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
         clusterCreate.setTopologyPolicy(null);
      }

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
      }
      clusterCreate.setType(Enum.valueOf(ClusterType.class, "HADOOP"));
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
      if (specFilePath != null) {
         if (!validateClusterCreate(clusterCreate)) {
            return;
         }
      }

      // process topology option
      if (topology == null) {
         clusterCreate.setTopologyPolicy(TopologyType.NONE);
      } else {
         try {
            clusterCreate.setTopologyPolicy(TopologyType.valueOf(topology));
         } catch (IllegalArgumentException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL, Constants.INPUT_TOPOLOGY_INVALID_VALUE);
            System.out.println("Please specify the topology type: HVE or RACK_HOST or HOST_AS_RACK");
         }
      }

      // rest invocation
      try {
         if (!showWarningMsg(clusterCreate.getName(), warningMsgList)) {
            return;
         }
         restClient.create(clusterCreate);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_RESULT_CREAT);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CREATE,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
      }
   }

   @CliCommand(value = "cluster list", help = "Get cluster information")
   public void getCluster(
         @CliOption(key = { "name" }, mandatory = false, help = "The cluster name") final String name,
         @CliOption(key = { "detail" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show node information") final boolean detail) {

      // rest invocation
      try {
         if (name == null) {
            ClusterRead[] clusters = restClient.getAll();
            if (clusters != null) {
               prettyOutputClustersInfo(clusters, detail);
            }
         } else {
            ClusterRead cluster = restClient.get(name);
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

   @CliCommand(value = "cluster export --spec", help = "Export cluster specification")
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
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "nodeGroupName" }, mandatory = false, help = "The node group name") final String nodeGroupName,
         @CliOption(key = { "nodeName" }, mandatory = false, help = "The node name") final String nodeName) {

      Map<String, String> queryStrings = new HashMap<String, String>();
      queryStrings
            .put(Constants.QUERY_ACTION_KEY, Constants.QUERY_ACTION_START);

      //rest invocation
      try {
         if (!validateNodeGroupName(nodeGroupName)) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                  Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
                  "invalid node group name");
            return;
         }
         if (!validateNodeName(clusterName, nodeGroupName, nodeName)) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                  Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
                  "invalid node name");
            return;
         }
         String groupName = nodeGroupName;
         String fullNodeName = nodeName;
         if (nodeName != null) {
            if (nodeGroupName == null) {
               groupName = extractNodeGroupName(nodeName);
               if (groupName == null) {
                  CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                        Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
                        "missing node group name");
                  return;
               }
            } else {
               fullNodeName = autoCompleteNodeName(clusterName, nodeGroupName, nodeName);
            }
         }

         String resource = getClusterResourceName(clusterName, groupName, fullNodeName);
         if (resource != null) {
            restClient.actionOps(resource, clusterName, queryStrings);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                  Constants.OUTPUT_OP_RESULT_START);
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster stop", help = "Stop a cluster")
   public void stopCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "nodeGroupName" }, mandatory = false, help = "The node group name") final String nodeGroupName,
         @CliOption(key = { "nodeName" }, mandatory = false, help = "The node name") final String nodeName) {
      Map<String, String> queryStrings = new HashMap<String, String>();
      queryStrings.put(Constants.QUERY_ACTION_KEY, Constants.QUERY_ACTION_STOP);

      //rest invocation
      try {
         if (!validateNodeGroupName(nodeGroupName)) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                  Constants.OUTPUT_OP_STOP, Constants.OUTPUT_OP_RESULT_FAIL,
                  "invalid node group name");
            return;
         }
         if (!validateNodeName(clusterName, nodeGroupName, nodeName)) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                  Constants.OUTPUT_OP_STOP, Constants.OUTPUT_OP_RESULT_FAIL,
                  "invalid node name");
            return;
         }
         String groupName = nodeGroupName;
         String fullNodeName = nodeName;
         if (nodeName != null) {
            if (nodeGroupName == null) {
               groupName = extractNodeGroupName(nodeName);
               if (groupName == null) {
                  CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                        Constants.OUTPUT_OP_STOP, Constants.OUTPUT_OP_RESULT_FAIL,
                        "missing node group name");
                  return;
               }
            } else {
               fullNodeName = autoCompleteNodeName(clusterName, nodeGroupName, nodeName);
            }
         }

         String resource = getClusterResourceName(clusterName, groupName, fullNodeName);
         if (resource != null) {
            restClient.actionOps(resource, clusterName, queryStrings);
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
                  Constants.OUTPUT_OP_RESULT_STOP);
         }
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

   @CliCommand(value = "cluster target", help = "Set or query target cluster to run commands")
   public void targetCluster(
         @CliOption(key = { "name" }, mandatory = false, help = "The cluster name") final String name,
         @CliOption(key = { "info" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show target information") final boolean info) {

      ClusterRead cluster = null;
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
            if (hiveInfo != null && hiveInfo.length() > 0) {
               System.out.println("Hive server info: " + hiveInfo);
            }
         } else {
            if (name == null) {
               ClusterRead[] clusters = restClient.getAll();
               if (clusters != null && clusters.length > 0) {
                  cluster = clusters[0];
               }
            } else {
               cluster = restClient.get(name);
            }

            if (cluster == null) {
               System.out.println("Failed to target cluster: The cluster " + name + "is not found");
               setFsURL("");
               setJobTrackerURL("");
               this.setHiveServer("");
            } else {
               targetClusterName = cluster.getName();
               for (NodeGroupRead nodeGroup : cluster.getNodeGroups()) {
                  for (String role : nodeGroup.getRoles()) {
                     if (role.equals("hadoop_namenode")) {
                        List<NodeRead> nodes = nodeGroup.getInstances();
                        if (nodes != null && nodes.size() > 0) {
                           String nameNodeIP = nodes.get(0).getIp();
                           setNameNode(nameNodeIP);
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
                           setHiveServer(hiveServerIP);
                        } else {
                           throw new CliRestException("no hive server available");
                        }
                     }
                  }
               }
               if (cluster.getExternalHDFS() != null && !cluster.getExternalHDFS().isEmpty()) {
                  setFsURL(cluster.getExternalHDFS());
               }
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_TARGET,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         setFsURL("");
         setJobTrackerURL("");
         this.setHiveServer("");
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

   private void setHiveServer(String hiveServerAddress) {
      try {
         hiveInfo = hiveCommands.config(hiveServerAddress, 10000, null);
      } catch (Exception e) {
         throw new CliRestException("faild to set hive server address");
      }
   }

   @CliCommand(value = "cluster config", help = "Config an existing cluster")
   public void configCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "specFile" }, mandatory = true, help = "The spec file name path") final String specFilePath,
         @CliOption(key = { "skipConfigValidation" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Skip cluster configuration validation. ") final boolean skipConfigValidation,
         @CliOption(key = { "yes" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Answer 'yes' to all Y/N questions. ") final boolean alwaysAnswerYes) {

      this.alwaysAnswerYes = alwaysAnswerYes;
      //validate the name
      if (name.indexOf("-") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, Constants.OUTPUT_OP_CONFIG,
               Constants.OUTPUT_OP_RESULT_FAIL, Constants.PARAM_CLUSTER + Constants.PARAM_NOT_CONTAIN_HORIZONTAL_LINE);
         return;
      }
      try {
         ClusterRead clusterRead = restClient.get(name);
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
         if (!showWarningMsg(clusterConfig.getName(), warningMsgList)) {
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

   
   private String getClusterResourceName(String cluster, String nodeGroup, String node) {
      assert cluster != null; // Spring shell guarantees this

      if (node != null && nodeGroup == null) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, cluster,
               Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.OUTPUT_OP_NODEGROUP_MISSING);
         return null;
      }

      StringBuilder res = new StringBuilder();
      res.append(cluster);
      if (nodeGroup != null) {
         res.append("/nodegroup/").append(nodeGroup);
         if (node != null) {
            res.append("/node/").append(node);
         }
      }

      return res.toString();
   }

   private boolean validateNodeName(String cluster, String group, String node) {
      if (node != null) {
         String[] parts = node.split("-");
         if (parts.length == 1) {
            return true;
         }
         if (parts.length == 3) {
            if (!parts[0].equals(cluster)) {
               return false;
            }
            if (group != null && !parts[1].equals(group)) {
               return false;
            }
            return true;
         }
         return false;
      }

      return true;
   }

   private boolean validateNodeGroupName(String group) {
      if (group != null) {
         return group.indexOf("-") == -1;
      }

      return true;
   }

   private String autoCompleteNodeName(String cluster, String group, String node) {
      assert cluster != null;
      assert group != null;
      assert node != null;

      if (node.indexOf("-") == -1) {
         StringBuilder sb = new StringBuilder();
         sb.append(cluster).append("-").append(group).append("-").append(node);
         return sb.toString();
      }

      return node;
   }

   private String extractNodeGroupName(String node) {
      String[] parts = node.split("-");

      if (parts.length == 3) {
         return parts[1];
      }
      return null;
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

   private void prettyOutputClusterInfo(ClusterRead cluster, boolean detail) {
      TopologyType topology = cluster.getTopologyPolicy();
      if (topology == null || topology == TopologyType.NONE) {
         System.out.printf("name: %s, distro: %s, status: %s",
               cluster.getName(), cluster.getDistro(), cluster.getStatus());
      } else {
         System.out.printf("name: %s, distro: %s, topology: %s, status: %s",
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
               Constants.FORMAT_TABLE_COLUMN_NAME, Arrays.asList("getName"));
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
                     Constants.FORMAT_TABLE_COLUMN_NAME,
                     Arrays.asList("getName"));
               if (topology == TopologyType.RACK_AS_RACK || topology == TopologyType.HVE) {
                  nColumnNamesWithGetMethodNames.put(
                        Constants.FORMAT_TABLE_COLUMN_RACK,
                        Arrays.asList("getRack"));
               }
               nColumnNamesWithGetMethodNames.put(
                     Constants.FORMAT_TABLE_COLUMN_HOST,
                     Arrays.asList("getHostName"));
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

   /**
    * Validate nodeGroupCreates member formats and values in the ClusterCreate.
    */
   private boolean validateClusterCreate(ClusterCreate clusterCreate) {
      // validation status 
      boolean validated = true;
      // show warning message
      boolean warning = false;
      //role count
      int masterCount = 0, workerCount = 0, clientCount = 0;
      //Find NodeGroupCreate array from current ClusterCreate instance.
      NodeGroupCreate[] nodeGroupCreates = clusterCreate.getNodeGroups();
      if (nodeGroupCreates == null || nodeGroupCreates.length == 0) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterCreate.getName(), Constants.OUTPUT_OP_CREATE,
               Constants.OUTPUT_OP_RESULT_FAIL, Constants.MULTI_INPUTS_CHECK);
         return !validated;
      } else {
         //used for collecting failed message.
         List<String> failedMsgList = new LinkedList<String>();
         List<String> warningMsgList = new LinkedList<String>();
         //find distro roles.
         List<String> distroRoles = findDistroRoles(clusterCreate);
         if (distroRoles == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterCreate.getName(), Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.PARAM_NO_DISTRO_AVAILABLE);
            return !validated;
         }
         if (nodeGroupCreates.length < 2 || nodeGroupCreates.length > 5) {
            warningMsgList.add(Constants.PARAM_CLUSTER_WARNING);
            warning = true;
         }
         // check external HDFS
         if (clusterCreate.hasHDFSUrlConfigured() && !clusterCreate.validateHDFSUrl()) {
            failedMsgList.add(new StringBuilder()
                         .append("externalHDFS=")
                         .append(clusterCreate.getExternalHDFS()).toString());
            validated = false;
         }

         // check placement policies
         if (!clusterCreate.validateNodeGroupPlacementPolicies(failedMsgList, warningMsgList)) {
            validated = false;
         }

         if (!clusterCreate.validateNodeGroupRoles(failedMsgList)) {
            validated = false;
         }

         for (NodeGroupCreate nodeGroupCreate : nodeGroupCreates) {
            // check node group's instanceNum
            if (!checkInstanceNum(nodeGroupCreate, failedMsgList)) {
               validated = false;
            }

            // check node group's roles 
            if (!checkNodeGroupRoles(nodeGroupCreate, distroRoles,
                  failedMsgList)) {
               validated = false;
            }
            // get node group role .
            NodeGroupRole role = getNodeGroupRole(nodeGroupCreate);
            switch (role) {
            case MASTER:
               masterCount++;
               if (nodeGroupCreate.getInstanceNum() >= 0
                     && nodeGroupCreate.getInstanceNum() != 1) {
                  validated = false;
                  collectInstanceNumInvalidateMsg(nodeGroupCreate,
                        failedMsgList);
               }
               break;
            case WORKER:
               workerCount++;
               if (nodeGroupCreate.getInstanceNum() == 0) {
                  validated = false;
                  collectInstanceNumInvalidateMsg(nodeGroupCreate,
                        failedMsgList);
               } else if (isHAFlag(nodeGroupCreate)) {
                  warning = true;
               }
               break;
            case CLIENT:
               clientCount++;
               if (isHAFlag(nodeGroupCreate)) {
                  warning = true;
               }
               break;
            case NONE:
               warning = true;
               break;
            default:
            }
         }
         if ((masterCount < 1 || masterCount > 2) || (workerCount < 1 || workerCount > 2) ||
               clientCount > 1) {
            warning = true;
         }
         if (!validated) {
            showFailedMsg(clusterCreate.getName(), failedMsgList);
         } else if (warning || warningMsgList != null) {
            // If warning is true,show waring message.
            if (!showWarningMsg(clusterCreate.getName(), warningMsgList)) {
               // When exist warning message,whether to proceed
               validated = false;
            }
         }
         return validated;
      }
   }

   private boolean isContinue(String clusterName, String operateType, String promptMsg) {
      if (this.alwaysAnswerYes) {
         return true;
      }

      boolean continueCreate = true;
      boolean continueLoop = true;
      String readMsg = "";
      try {
         ConsoleReader reader = new ConsoleReader();
         // Set prompt message
         reader.setDefaultPrompt(promptMsg);
         int k = 0;
         while (continueLoop) {
            if (k >= 3) {
               continueCreate = false;
               break;
            }
            // Read user input
            readMsg = reader.readLine();
            if (readMsg.trim().equalsIgnoreCase("yes")
                  || readMsg.trim().equalsIgnoreCase("y")) {
               continueLoop = false;
            } else if (readMsg.trim().equalsIgnoreCase("no")
                  || readMsg.trim().equalsIgnoreCase("n")) {
               continueLoop = false;
               continueCreate = false;
            } else {
               k++;
            }
         }
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, operateType,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         continueCreate = false;
      }
      return continueCreate;
   }

   private NodeGroupRole getNodeGroupRole(NodeGroupCreate nodeGroupCreate) {
      //Find roles list from current  NodeGroupCreate instance.
      List<String> roles = nodeGroupCreate.getRoles();
      for (NodeGroupRole role : NodeGroupRole.values()) {
         if (matchRole(role, roles)) {
            return role;
         }
      }
      return NodeGroupRole.NONE;
   }

   /**
    * Check the roles was introduced, whether matching with system's specialize
    * role.
    */
   private boolean matchRole(NodeGroupRole role, List<String> roles) {
      List<String> matchRoles = new LinkedList<String>();
      switch (role) {
      case MASTER:
         if (roles.size() == 1) {
            String r = roles.get(0);
            return Constants.ROLE_HADOOP_NAME_NODE.equals(r) ||
                   Constants.ROLE_HADOOP_JOB_TRACKER.equals(r);
         } else if (roles.size() == 2) {
            matchRoles.add(Constants.ROLE_HADOOP_NAME_NODE);
            matchRoles.add(Constants.ROLE_HADOOP_JOB_TRACKER);
            matchRoles.removeAll(roles);
            return matchRoles.size() == 0 ? true : false;
         }
         return false;
      case WORKER:
         if (roles.size() == 1) {
            if (Constants.ROLE_HADOOP_DATANODE.equals(roles.get(0)) ||
                Constants.ROLE_HADOOP_TASKTRACKER.equals(roles.get(0))) {
               return true;
            }
            return false;
         } else {
            matchRoles.add(Constants.ROLE_HADOOP_DATANODE);
            matchRoles.add(Constants.ROLE_HADOOP_TASKTRACKER);
            matchRoles.removeAll(roles);
            return matchRoles.size() == 0 ? true : false;
         }
      case CLIENT:
         if (roles.size() < 1 || roles.size() > 4) {
            return false;
         } else {
            matchRoles.add(Constants.ROLE_HADOOP_CLIENT);
            matchRoles.add(Constants.ROLE_HIVE);
            matchRoles.add(Constants.ROLE_HIVE_SERVER);
            matchRoles.add(Constants.ROLE_PIG);
            int diffNum = matchRoles.size() - roles.size();
            matchRoles.removeAll(roles);
            return roles.contains(Constants.ROLE_HADOOP_CLIENT)
                  && (diffNum >= 0) && (diffNum == matchRoles.size()) ? true
                  : false;
         }
      }
      return false;
   }

   private void showWarningMsg() {
      System.out.println(Constants.PARAM_CLUSTER_WARNING);
   }

   private boolean checkInstanceNum(NodeGroupCreate nodeGroup,
         List<String> failedMsgList) {
      boolean validated = true;
      if (nodeGroup.getInstanceNum() < 0) {
         validated = false;
         collectInstanceNumInvalidateMsg(nodeGroup, failedMsgList);
      }
      return validated;
   }



   private void collectInstanceNumInvalidateMsg(NodeGroupCreate nodeGroup,
         List<String> failedMsgList) {
      failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
            .append(".").append("instanceNum=")
            .append(nodeGroup.getInstanceNum()).toString());
   }


   private boolean checkNodeGroupRoles(NodeGroupCreate nodeGroup,
         List<String> distroRoles, List<String> failedMsgList) {
      List<String> roles = nodeGroup.getRoles();
      boolean validated = true;
      StringBuilder rolesMsg = new StringBuilder();
      for (String role : roles) {
         if (!distroRoles.contains(role)) {
            validated = false;
            rolesMsg.append(",").append(role);
         }
      }
      if (!validated) {
         rolesMsg.replace(0, 1, "");
         failedMsgList.add(new StringBuilder().append(nodeGroup.getName())
               .append(".").append("roles=").append("\"")
               .append(rolesMsg.toString()).append("\"").toString());
      }
      return validated;
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

   private ValidateResult validateConfiguration(ClusterCreate cluster, ValidationType validationType) {
      ValidateResult validateResult = new ValidateResult();
      // validate cluster level Configuration
      ValidateResult vr = null;
      if (cluster.getConfiguration() != null && !cluster.getConfiguration().isEmpty()) {
         vr = AppConfigValidationUtils.validateConfig(validationType, cluster.getConfiguration());
         if (vr.getType() != ValidateResult.Type.VALID) {
            validateResult.setType(vr.getType());
            validateResult.setFailureNames(vr.getFailureNames());
         }
      }
      // validate nodegroup level Configuration
      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (nodeGroup.getConfiguration() != null && !nodeGroup.getConfiguration().isEmpty()) {
            vr = AppConfigValidationUtils.validateConfig(validationType, nodeGroup.getConfiguration());
            if (vr.getType() != ValidateResult.Type.VALID) {
               validateResult.setType(vr.getType());
               List<String> failureNames = new LinkedList<String>();
               failureNames.addAll(validateResult.getFailureNames());
               for (String name : vr.getFailureNames()) {
                  if (!failureNames.contains(name)) {
                     failureNames.add(name);
                  }
               }
               validateResult.setFailureNames(vr.getFailureNames());
            }
         }
      }
      return validateResult;
   }

   private void addWhiteListWarning(final String clusterName, ValidateResult whiteListResult,
         List<String> warningMsgList) {
      if (whiteListResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_NAME) {
         String warningMsg =
               getValidateWarningMsg(whiteListResult.getFailureNames(),
                     Constants.PARAM_CLUSTER_NOT_IN_WHITE_LIST_WARNING);
         if (warningMsgList != null) {
            warningMsgList.add(warningMsg);
         }
      }
   }

   private void addBlackListWarning(ValidateResult blackListResult, List<String> warningList) {
      if (blackListResult.getType() == ValidateResult.Type.NAME_IN_BLACK_LIST) {
         String warningMsg =
               getValidateWarningMsg(blackListResult.getFailureNames(), Constants.PARAM_CLUSTER_IN_BLACK_LIST_WARNING);
         if (warningList != null)
            warningList.add(warningMsg);
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

   private boolean showWarningMsg(String clusterName, List<String> warningMsgList) {
      if (warningMsgList != null && !warningMsgList.isEmpty()) {
         for (String message : warningMsgList) {
            System.out.println(message);
         }
         if (!isContinue(clusterName, Constants.OUTPUT_OP_CREATE, Constants.PARAM_PROMPT_CONTINUE_MESSAGE)) {
            return false;
         }
      }
      return true;
   }

   private boolean isHAFlag(NodeGroupCreate nodeGroupCreate) {
      return !CommandsUtils.isBlank(nodeGroupCreate.getHaFlag())
            && !nodeGroupCreate.getHaFlag().equalsIgnoreCase("off");
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
