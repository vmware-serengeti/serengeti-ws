/*****************************************************************************
 *   Copyright (c) 2012-2014 VMware, Inc. All Rights Reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.vmware.bdd.utils.ListToStringConverter;
import jline.console.ConsoleReader;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.impala.hive.HiveCommands;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.vmware.bdd.apitypes.AppManagerRead;
import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.apitypes.ClusterRead;
import com.vmware.bdd.apitypes.ClusterType;
import com.vmware.bdd.apitypes.DistroRead;
import com.vmware.bdd.apitypes.ElasticityRequestBody;
import com.vmware.bdd.apitypes.ElasticityRequestBody.ElasticityMode;
import com.vmware.bdd.apitypes.FixDiskRequestBody;
import com.vmware.bdd.apitypes.NetConfigInfo.NetTrafficType;
import com.vmware.bdd.apitypes.NetworkRead;
import com.vmware.bdd.apitypes.NodeGroupCreate;
import com.vmware.bdd.apitypes.NodeGroupRead;
import com.vmware.bdd.apitypes.NodeRead;
import com.vmware.bdd.apitypes.Priority;
import com.vmware.bdd.apitypes.ResourceScale;
import com.vmware.bdd.apitypes.TaskRead;
import com.vmware.bdd.apitypes.TaskRead.NodeStatus;
import com.vmware.bdd.apitypes.TopologyType;
import com.vmware.bdd.cli.rest.AppManagerRestClient;
import com.vmware.bdd.cli.rest.CliRestException;
import com.vmware.bdd.cli.rest.ClusterRestClient;
import com.vmware.bdd.cli.rest.NetworkRestClient;
import com.vmware.bdd.utils.AppConfigValidationUtils;
import com.vmware.bdd.utils.AppConfigValidationUtils.ValidationType;
import com.vmware.bdd.utils.CommonUtil;
import com.vmware.bdd.utils.ValidateResult;

@Component
public class ClusterCommands implements CommandMarker {

   @Autowired
   private NetworkRestClient networkRestClient;

   @Autowired
   private ClusterRestClient restClient;

   @Autowired
   private AppManagerRestClient appManagerRestClient;

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
         @CliOption(key = { "appManager" }, mandatory = false, help = "The application manager name") final String appManager,
         @CliOption(key = { "type" }, mandatory = false, help = "The cluster type is Hadoop or HBase") final String type,
         @CliOption(key = { "distro" }, mandatory = false, help = "A hadoop distro name") final String distro,
         @CliOption(key = { "specFile" }, mandatory = false, help = "The spec file name path") final String specFilePath,
         @CliOption(key = { "rpNames" }, mandatory = false, help = "Resource Pools for the cluster: use \",\" among names.") final String rpNames,
         @CliOption(key = { "dsNames" }, mandatory = false, help = "Datastores for the cluster: use \",\" among names.") final String dsNames,
         @CliOption(key = { "networkName" }, mandatory = false, help = "Network Name used for management") final String networkName,
         @CliOption(key = { "hdfsNetworkName" }, mandatory = false, help = "Network Name for HDFS traffic.") final String hdfsNetworkName,
         @CliOption(key = { "mapredNetworkName" }, mandatory = false, help = "Network Name for MapReduce traffic") final String mapredNetworkName,
         @CliOption(key = { "topology" }, mandatory = false, help = "You must specify the topology type: HVE or RACK_AS_RACK or HOST_AS_RACK") final String topology,
         @CliOption(key = { "resume" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to resume cluster creation") final boolean resume,
         @CliOption(key = { "skipConfigValidation" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Skip cluster configuration validation. ") final boolean skipConfigValidation,
         @CliOption(key = { "yes" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Answer 'yes' to all Y/N questions. ") final boolean alwaysAnswerYes,
         @CliOption(key = { "password" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Answer 'yes' to set password for all VMs in this cluster.") final boolean setClusterPassword,
         @CliOption(key = { "localRepoURL" }, mandatory = false, help = "Local yum server URL for application managers, ClouderaManager/Ambari.") final String localRepoURL) {
      // validate the name
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

      // process resume
      if (resume && setClusterPassword) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.RESUME_DONOT_NEED_SET_PASSWORD);
         return;
      } else if (resume) {
         resumeCreateCluster(name);
         return;
      }

      // build ClusterCreate object
      ClusterCreate clusterCreate = new ClusterCreate();
      clusterCreate.setName(name);

      if (!CommandsUtils.isBlank(appManager)
            && !Constants.IRONFAN.equalsIgnoreCase(appManager)) {
         AppManagerRead appManagerRead = appManagerRestClient.get(appManager);
         if (appManagerRead == null) {
            CommandsUtils
                  .printCmdFailure(
                        Constants.OUTPUT_OBJECT_CLUSTER,
                        name,
                        Constants.OUTPUT_OP_CREATE,
                        Constants.OUTPUT_OP_RESULT_FAIL,
                        appManager
                              + " cannot be found in the list of application managers.");
            return;
         }
      }

      if (CommandsUtils.isBlank(appManager)) {
         clusterCreate.setAppManager(Constants.IRONFAN);
      } else {
         clusterCreate.setAppManager(appManager);

         // local yum repo url for 3rd party app managers like ClouderaMgr, Ambari etc.
         if (!CommandsUtils.isBlank(localRepoURL)) {
            clusterCreate.setLocalRepoURL(localRepoURL);
         }
      }

      if (setClusterPassword) {
         String password = getPassword();
         //user would like to set password, but failed to enter
         //a valid one, quit cluster create
         if (password == null) {
            return;
         } else {
            clusterCreate.setPassword(password);
         }
      }

      if (type != null) {
         ClusterType clusterType = ClusterType.getByDescription(type);
         if (clusterType == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE
                        + " " + "type=" + type);
            return;
         }
         clusterCreate.setType(clusterType);
      } else if (specFilePath == null) {
         // create Hadoop (HDFS + MapReduce) cluster as default
         clusterCreate.setType(ClusterType.HDFS_MAPRED);
      }

      TopologyType policy = null;
      if (topology != null) {
         policy = validateTopologyValue(name, topology);
         if (policy == null) {
            return;
         }
      } else {
         policy = TopologyType.NONE;
      }
      clusterCreate.setTopologyPolicy(policy);


      DistroRead distroRead4Create;
      try {
         if (distro != null) {
            DistroRead[] distroReads =
                  appManagerRestClient
                        .getDistros(clusterCreate.getAppManager());
            distroRead4Create = getDistroByName(distroReads, distro);

            if (distroRead4Create == null) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL, Constants.PARAM_DISTRO
                           + Constants.PARAM_NOT_SUPPORTED
                           + getDistroNames(distroReads));
               return;
            }
         } else {
            distroRead4Create =
                  appManagerRestClient.getDefaultDistro(clusterCreate
                        .getAppManager());
            if (distroRead4Create == null
                  || CommandsUtils.isBlank(distroRead4Create.getName())) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAM_NO_DEFAULT_DISTRO);
               return;
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
         return;
      }
      clusterCreate.setDistro(distroRead4Create.getName());
      clusterCreate.setDistroVendor(distroRead4Create.getVendor());
      clusterCreate.setDistroVersion(distroRead4Create.getVersion());

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
      Set<String> allNetworkNames = new HashSet<String>();
      try {
         if (specFilePath != null) {
            ClusterCreate clusterSpec =
                  CommandsUtils.getObjectByJsonString(ClusterCreate.class,
                        CommandsUtils.dataFromFile(specFilePath));
            clusterCreate.setSpecFile(true);
            clusterCreate.setExternalHDFS(clusterSpec.getExternalHDFS());
            clusterCreate.setExternalMapReduce(clusterSpec
                  .getExternalMapReduce());
            clusterCreate.setNodeGroups(clusterSpec.getNodeGroups());
            clusterCreate.setConfiguration(clusterSpec.getConfiguration());
            // TODO: W'd better merge validateConfiguration with validateClusterSpec to avoid repeated validation.
            if (CommandsUtils.isBlank(appManager)
                  || Constants.IRONFAN.equalsIgnoreCase(appManager)) {
               validateConfiguration(clusterCreate, skipConfigValidation,
                     warningMsgList, failedMsgList);
            }
            clusterCreate.validateNodeGroupNames();
            if (!validateHAInfo(clusterCreate.getNodeGroups())) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAM_CLUSTER_SPEC_HA_ERROR + specFilePath);
               return;
            }
         }
         allNetworkNames = getAllNetworkNames();
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
         return;
      }

      if (allNetworkNames.isEmpty()) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_CANNOT_FIND_NETWORK);
         return;
      }

      Map<NetTrafficType, List<String>> networkConfig =
            new HashMap<NetTrafficType, List<String>>();
      if (networkName == null) {
         if (allNetworkNames.size() == 1) {
            networkConfig.put(NetTrafficType.MGT_NETWORK,
                  new ArrayList<String>());
            networkConfig.get(NetTrafficType.MGT_NETWORK).addAll(
                  allNetworkNames);
         } else {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL, Constants.PARAM_NETWORK_NAME
                        + Constants.PARAM_NOT_SPECIFIED);
            return;
         }
      } else {
         if (!allNetworkNames.contains(networkName)
               || (hdfsNetworkName != null && !allNetworkNames
                     .contains(hdfsNetworkName))
               || (mapredNetworkName != null && !allNetworkNames
                     .contains(mapredNetworkName))) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_CREATE,
                  Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.PARAM_NETWORK_NAME + Constants.PARAM_NOT_SUPPORTED
                        + allNetworkNames.toString());
            return;
         }

         networkConfig.put(NetTrafficType.MGT_NETWORK, new ArrayList<String>());
         networkConfig.get(NetTrafficType.MGT_NETWORK).add(networkName);

         if (hdfsNetworkName != null) {
            networkConfig.put(NetTrafficType.HDFS_NETWORK,
                  new ArrayList<String>());
            networkConfig.get(NetTrafficType.HDFS_NETWORK).add(hdfsNetworkName);
         }

         if (mapredNetworkName != null) {
            networkConfig.put(NetTrafficType.MAPRED_NETWORK,
                  new ArrayList<String>());
            networkConfig.get(NetTrafficType.MAPRED_NETWORK).add(
                  mapredNetworkName);
         }
      }
      notifyNetsUsage(networkConfig, warningMsgList);
      clusterCreate.setNetworkConfig(networkConfig);

      clusterCreate.validateCDHVersion(warningMsgList);

      // Validate that the specified file is correct json format and proper value.
      //TODO(qjin): 1, in validateClusterCreate, implement roles check and validation
      //            2, consider use service to validate configuration for different appManager
      if (specFilePath != null) {
         validateClusterSpec(clusterCreate, failedMsgList, warningMsgList);
      }

      // give a warning message if both type and specFilePath are specified
      if (type != null && specFilePath != null) {
         warningMsgList.add(Constants.TYPE_SPECFILE_CONFLICT);
      }

      if (!failedMsgList.isEmpty()) {
         showFailedMsg(clusterCreate.getName(), Constants.OUTPUT_OP_CREATE,
               failedMsgList);
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
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESULT_CREAT);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               CommandsUtils.getExceptionMessage(e));
      }
   }

   private DistroRead getDistroByName(DistroRead[] distroReads,
         String distroName) {
      if (distroReads != null && distroName != null) {
         for (DistroRead distroRead : distroReads) {
            if (distroName.equals(distroRead.getName())) {
               return distroRead;
            }
         }
      }
      return null;
   }

   private void validateClusterSpec(ClusterCreate clusterCreate,
         List<String> failedMsgList, List<String> warningMsgList) {
      clusterCreate.validateClusterCreate(failedMsgList, warningMsgList);
      //validate roles and configuration.
      com.vmware.bdd.apitypes.ValidateResult vr =
            restClient.validateBlueprint(clusterCreate);
      if (!vr.isValidated()) {
         failedMsgList.addAll(vr.getFailedMsgList());
         warningMsgList.addAll(vr.getWarningMsgList());
      }
   }

   /**
    * notify user which network Serengeti will pick up for mgt/hdfs/mapred
    *
    * @param networkConfig
    * @param warningMsgList
    */
   private void notifyNetsUsage(
         Map<NetTrafficType, List<String>> networkConfig,
         List<String> warningMsgList) {
      if (!networkConfig.containsKey(NetTrafficType.HDFS_NETWORK)
            && !networkConfig.containsKey(NetTrafficType.MAPRED_NETWORK)) {
         return;
      }
      String mgtNetwork = networkConfig.get(NetTrafficType.MGT_NETWORK).get(0);
      String hdfsNetwork = mgtNetwork;
      String mapredNetwork = mgtNetwork;
      if (networkConfig.containsKey(NetTrafficType.HDFS_NETWORK)
            && !networkConfig.get(NetTrafficType.HDFS_NETWORK).isEmpty()) {
         hdfsNetwork = networkConfig.get(NetTrafficType.HDFS_NETWORK).get(0);
      }
      if (networkConfig.containsKey(NetTrafficType.MAPRED_NETWORK)
            && !networkConfig.get(NetTrafficType.MAPRED_NETWORK).isEmpty()) {
         mapredNetwork =
               networkConfig.get(NetTrafficType.MAPRED_NETWORK).get(0);
      }

      StringBuffer netsUsage =
            new StringBuffer().append("The cluster will use network ")
                  .append(mgtNetwork).append(" for management, ")
                  .append(hdfsNetwork).append(" for HDFS traffic, and ")
                  .append(mapredNetwork).append(" for MapReduce traffic.");
      warningMsgList.add(netsUsage.toString());
   }

   private String getPassword() {
      System.out.println("Hint: " + Constants.PASSWORD_HINT);
      String firstPassword = getInputedPassword(Constants.ENTER_PASSWORD);
      if (firstPassword == null) {
         return null;
      }

      String secondPassword = getInputedPassword(Constants.CONFIRM_PASSWORD);
      if (secondPassword == null) {
         return null;
      }

      if (!firstPassword.equals(secondPassword)) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, null,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PASSWORD_CONFIRMATION_FAILED);
         return null;
      }

      return firstPassword;
   }

   private String getInputedPassword(String promptMsg) {
      try {
         ConsoleReader reader = new ConsoleReader();
         reader.setPrompt(promptMsg);
         String password = null;
         try {
            password = reader.readLine(Character.valueOf('*'));
         } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("!")) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     null, Constants.OUTPUT_OP_CREATE,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PASSWORD_CHARACTER_REQUIREMENT);
               return null;
            } else {
               throw e;
            }
         }
         if (isValidPassword(password)) {
            return password;
         } else {
            return null;
         }
      } catch (IOException e) {
         return null;
      }
   }

   private boolean isValidPassword(String password) {
      if (password.length() < Constants.PASSWORD_MIN_LENGTH
            || password.length() > Constants.PASSWORD_MAX_LENGTH) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, null,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PASSWORD_LENGTH_REQUIREMENT);
         return false;
      }
      if (CommonUtil.passwordContainInvalidCharacter(password)) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, null,
               Constants.OUTPUT_OP_CREATE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PASSWORD_CHARACTER_REQUIREMENT);
         return false;
      }
      return true;
   }

   @CliCommand(value = "cluster list", help = "Get cluster information")
   public void getCluster(
         @CliOption(key = { "name" }, mandatory = false, help = "The cluster name") final String name,
         @CliOption(key = { "detail" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "flag to show node information") final boolean detail) {

      // rest invocation
      try {
         if (name == null) {
            ClusterRead[] clusters = restClient.getAll(detail);
            if (clusters != null && clusters.length > 0) {
               Arrays.sort(clusters);
               prettyOutputClustersInfo(clusters, detail);
            }
         } else {
            ClusterRead cluster = restClient.get(name, detail);
            if (cluster != null) {
               prettyOutputClusterInfo(cluster, detail);
               printSeperator();
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_LIST, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster export", help = "Export cluster data")
   public void exportClusterData(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "specFile" }, mandatory = false, help = "The cluster spec file path") final String specFileName,
         @CliOption(key = { "type" }, mandatory = false, help = "The data type to export: SPEC or RACK or IP") final String type,
         @CliOption(key = { "topology" }, mandatory = false, help = "The topology type: HVE or RACK_AS_RACK or HOST_AS_RACK") final String topology,
         @CliOption(key = { "delimiter" }, mandatory = false, help = "The string used to separate each line") final String delimeter,
         @CliOption(key = { "output" }, mandatory = false, help = "The path to the output file") final String output) {

      // when neither fileName nor type is specified, path is null and when output, it will be replaced
      // with System.out
      String path = null;
      if (!CommandsUtils.isBlank(specFileName)) {
         if (!CommandsUtils.isBlank(type)) {
            System.out.println(Constants.TYPE_SPECFILE_CONFLICT);
            return;
         }
         path = specFileName;
      } else if (!CommandsUtils.isBlank((type))) {
         if (!CommandsUtils.isBlank(output)) {
            path = output;
         }
      }

      if (topology != null && validateTopologyValue(name, topology) == null) {
         return;
      }

      try {
         if ((CommandsUtils.isBlank(specFileName) && CommandsUtils
               .isBlank(type))
               || !CommandsUtils.isBlank(specFileName)
               || type.equalsIgnoreCase(Constants.EXPORT_TYPE_SPEC)) {
            ClusterCreate cluster = restClient.getSpec(name);
            CommandsUtils.prettyJsonOutput(cluster, path);
         } else if (type.equalsIgnoreCase(Constants.EXPORT_TYPE_RACK)) {
            Map<String, String> rackTopology =
                  restClient.getRackTopology(name, topology);
            CommonUtil.gracefulRackTopologyOutput(rackTopology, path,
                  delimeter);
            CommandsUtils.writeEndingMsgToScreen(path);
         } else if (type.equalsIgnoreCase(Constants.EXPORT_TYPE_IP)) {
            ClusterRead cluster = restClient.get(name, true);
            prettyOutputClusterIPs(cluster, path, delimeter);
         } else {
            System.out.println(Constants.UNKNOWN_EXPORT_TYPE);
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

      // rest invocation
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

      // rest invocation
      try {
         restClient.actionOps(clusterName, queryStrings);
         CommandsUtils.printCmdSuccess(
               Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_RESULT_START);

      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(
               Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_START, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster stop", help = "Stop a cluster")
   public void stopCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName) {
      Map<String, String> queryStrings = new HashMap<String, String>();
      queryStrings.put(Constants.QUERY_ACTION_KEY, Constants.QUERY_ACTION_STOP);

      // rest invocation
      try {
         restClient.actionOps(clusterName, queryStrings);
         CommandsUtils.printCmdSuccess(
               Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_RESULT_STOP);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(
               Constants.OUTPUT_OBJECT_NODES_IN_CLUSTER, clusterName,
               Constants.OUTPUT_OP_STOP, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   @CliCommand(value = "cluster resize", help = "Resize a cluster")
   public void resizeCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name,
         @CliOption(key = { "nodeGroup" }, mandatory = true, help = "The node group name") final String nodeGroup,
         @CliOption(key = { "instanceNum" }, mandatory = false, unspecifiedDefaultValue = "0", help = "The resized number of instances. It should be larger that existing one") final int instanceNum,
         @CliOption(key = { "cpuNumPerNode" }, mandatory = false, unspecifiedDefaultValue = "0", help = "The number of vCPU for the nodes in this group") final int cpuNumber,
         @CliOption(key = { "memCapacityMbPerNode" }, mandatory = false, unspecifiedDefaultValue = "0", help = "The number of memory size in Mb for the nodes in this group") final long memory) {

      if ((instanceNum > 0 && cpuNumber == 0 && memory == 0)
            || (instanceNum == 0 && (cpuNumber > 0 || memory > 0))) {
         try {
            ClusterRead cluster = restClient.get(name, false);
            if (cluster == null) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     name, Constants.OUTPUT_OP_RESIZE,
                     Constants.OUTPUT_OP_RESULT_FAIL, "cluster " + name
                           + " does not exist.");
               return;
            }
            // disallow scale out zookeeper node group.
            List<NodeGroupRead> ngs = cluster.getNodeGroups();
            boolean found = false;
            for (NodeGroupRead ng : ngs) {
               if (ng.getName().equals(nodeGroup)) {
                  found = true;
                  /*if (ng.getRoles() != null
                        && ng.getRoles().contains(
                              HadoopRole.ZOOKEEPER_ROLE.toString())
                        && instanceNum > 1) {
                     CommandsUtils.printCmdFailure(
                           Constants.OUTPUT_OBJECT_CLUSTER, name,
                           Constants.OUTPUT_OP_RESIZE,
                           Constants.OUTPUT_OP_RESULT_FAIL,
                           Constants.ZOOKEEPER_NOT_RESIZE);
                     return;
                  }*/// TODO emma: do not check as client do not know who is Zookeeper
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
            TaskRead taskRead = null;
            if (instanceNum > 0) {
               restClient.resize(name, nodeGroup, instanceNum);
            } else if (cpuNumber > 0 || memory > 0) {
               if (!cluster.getStatus().isActiveServiceStatus()) {
                  CommandsUtils.printCmdFailure(
                        Constants.OUTPUT_OBJECT_CLUSTER, name,
                        Constants.OUTPUT_OP_RESIZE,
                        Constants.OUTPUT_OP_RESULT_FAIL,
                        "Cluster must be in 'RUNNING' state to scale up/down");
                  return;
               }
               ResourceScale resScale =
                     new ResourceScale(name, nodeGroup, cpuNumber, memory);
               taskRead = restClient.scale(resScale);
            }
            CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_RESULT_RESIZE);
            if (taskRead != null) {
               System.out.println();
               printScaleReport(taskRead, name, nodeGroup);
            }
         } catch (CliRestException e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_RESIZE,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      } else {
         if (instanceNum > 0 && (cpuNumber > 0 || memory > 0)) {
            CommandsUtils
                  .printCmdFailure(
                        Constants.OUTPUT_OBJECT_CLUSTER,
                        name,
                        Constants.OUTPUT_OP_RESIZE,
                        Constants.OUTPUT_OP_RESULT_FAIL,
                        "Can not scale out and scale up/down at the same time, you have to run those commands separately");
         } else if (instanceNum == 0 && cpuNumber == 0 && memory == 0) {
            CommandsUtils
                  .printCmdFailure(
                        Constants.OUTPUT_OBJECT_CLUSTER,
                        name,
                        Constants.OUTPUT_OP_RESIZE,
                        Constants.OUTPUT_OP_RESULT_FAIL,
                        "You must specify at least one positive value for instanceNum, cpuNumPerNode, or memCapacityMbPerNode");

         } else {
            List<String> invalidParams = new ArrayList<String>();
            if (instanceNum < 0) {
               invalidParams.add("instanceNum=" + instanceNum);
            }
            if (cpuNumber < 0) {
               invalidParams.add("cpuNumPerNode=" + cpuNumber);
            }
            if (memory < 0) {
               invalidParams.add("memCapacityMbPerNode=" + memory);
            }
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  name, Constants.OUTPUT_OP_RESIZE,
                  Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE
                        + " " + StringUtils.join(invalidParams, ", "));
         }
      }
   }

   private void printScaleReport(TaskRead taskRead, String clusterName,
         String nodeGroupName) {
      ClusterRead cluster = restClient.get(clusterName, true);
      List<NodeGroupRead> nodeGroups = cluster.getNodeGroups();
      List<NodeStatus> succeedNodes = taskRead.getSucceedNodes();
      List<NodeStatus> failedNodes = taskRead.getFailNodes();
      setNodeStatusInfo(succeedNodes, nodeGroups);
      setNodeStatusInfo(failedNodes, nodeGroups);
      LinkedHashMap<String, List<String>> columnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      columnNamesWithGetMethodNames.put("IP", Arrays.asList("getIp"));
      columnNamesWithGetMethodNames.put("NAME", Arrays.asList("getNodeName"));
      columnNamesWithGetMethodNames.put("CPU", Arrays.asList("getCpuNumber"));
      columnNamesWithGetMethodNames.put("MEM(MB)", Arrays.asList("getMemory"));
      columnNamesWithGetMethodNames.put("STATUS", Arrays.asList("getStatus"));
      columnNamesWithGetMethodNames.put("NOTES",
            Arrays.asList("getErrorMessage"));
      try {
         System.out.println("The resized node group: " + nodeGroupName);
         System.out
               .println("The current resized nodes: " + succeedNodes.size());
         CommandsUtils.printInTableFormat(columnNamesWithGetMethodNames,
               succeedNodes.toArray(), Constants.OUTPUT_INDENT);
         System.out.println("The failed resized nodes: " + failedNodes.size());
         CommandsUtils.printInTableFormat(columnNamesWithGetMethodNames,
               failedNodes.toArray(), Constants.OUTPUT_INDENT);
      } catch (Exception e) {
         throw new CliRestException(e.getMessage());
      }
   }

   private void setNodeStatusInfo(List<NodeStatus> nodes,
         List<NodeGroupRead> nodeGroups) {
      for (NodeStatus nodeStatus : nodes) {
         NodeRead node = getNodeRead(nodeStatus.getNodeName(), nodeGroups);
         if (node != null) {
            // only show the management Ip currently
            nodeStatus.setIp(node.fetchMgtIp());
            nodeStatus.setStatus(node.getStatus());
            nodeStatus.setCpuNumber(node.getCpuNumber());
            nodeStatus.setMemory(node.getMemory());
         }
      }
   }

   @CliCommand(value = "cluster setParam", help = "set cluster parameters")
   public void setParam(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "elasticityMode" }, mandatory = false, help = "The elasticity mode: AUTO, MANUAL") final String elasticityMode,
         @CliOption(key = { "minComputeNodeNum" }, mandatory = false, help = "The minimum number of compute nodes staying powered on (valid in auto elasticity mode)") final Integer minComputeNodeNum,
         @CliOption(key = { "maxComputeNodeNum" }, mandatory = false, help = "The maximum number of compute nodes staying powered on (valid in auto elasticity mode)") final Integer maxComputeNodeNum,
         @CliOption(key = { "targetComputeNodeNum" }, mandatory = false, help = "The number of instances powered on (valid in manual elasticity mode)") final Integer targetComputeNodeNum,
         @CliOption(key = { "ioShares" }, mandatory = false, help = "The relative disk I/O priorities: HIGH, NORNAL, LOW") final String ioShares) {
      try {
         //validate if the cluster exists
         ClusterRead cluster = restClient.get(clusterName, false);
         if (cluster == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_SET_PARAM,
                  Constants.OUTPUT_OP_RESULT_FAIL, "cluster " + clusterName
                        + " does not exist.");
            return;
         }

         //validate the node group type for elasticity params
         if (elasticityMode != null || minComputeNodeNum != null
               || maxComputeNodeNum != null || targetComputeNodeNum != null) {
            if (!cluster.validateSetManualElasticity()) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     clusterName, Constants.OUTPUT_OP_SET_PARAM,
                     Constants.OUTPUT_OP_RESULT_FAIL,
                     Constants.PARAM_SHOULD_HAVE_COMPUTE_ONLY_GROUP);
               return;
            }
         } else if (ioShares == null) {
            // in this case, no parameter is specified excpet "cluster name", return directly
            System.out
                  .println("There is nothing to adjust, please specify more parameters.");
            return;
         }

         ElasticityMode mode = null;
         //validate the input of elasticityMode
         if (elasticityMode != null) {
            try {
               mode = ElasticityMode.valueOf(elasticityMode.toUpperCase());
            } catch (IllegalArgumentException e) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     clusterName, Constants.OUTPUT_OP_SET_PARAM,
                     Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE
                           + " elasticityMode = " + elasticityMode);
               return;
            }
         }
         Boolean enableAuto = null;
         if (mode != null) {
            enableAuto = (mode == ElasticityMode.AUTO) ? true : false;
         }

         //validate the input parameters
         try {
            if (!cluster.validateSetParamParameters(targetComputeNodeNum,
                  minComputeNodeNum, maxComputeNodeNum)) {
               return;
            }
         } catch (Exception e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_SET_PARAM,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
            return;
         }

         //validate the input of ioShares
         Priority ioPriority = null;
         if (ioShares != null) {
            try {
               ioPriority = Priority.valueOf(ioShares.toUpperCase());
            } catch (IllegalArgumentException ex) {
               CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                     clusterName, Constants.OUTPUT_OP_SET_PARAM,
                     Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE
                           + " " + "ioShares = " + ioShares);
               return;
            }
         }

         ElasticityRequestBody requestBody = new ElasticityRequestBody();
         requestBody.setEnableAuto(enableAuto);
         //print warning for ignored parameters under different mode
         if (mode != null) {
            if (mode == ElasticityMode.AUTO) {
               requestBody.setMinComputeNodeNum(minComputeNodeNum);
               requestBody.setMaxComputeNodeNum(maxComputeNodeNum);
            } else {
               requestBody.setActiveComputeNodeNum(targetComputeNodeNum);
            }
         } else {
            requestBody.setMinComputeNodeNum(minComputeNodeNum);
            requestBody.setMaxComputeNodeNum(maxComputeNodeNum);
            requestBody.setActiveComputeNodeNum(targetComputeNodeNum);
         }
         requestBody.setIoPriority(ioPriority);

         restClient.setParam(cluster, requestBody);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_RESULT_ADJUST);
         //print warning for ignored parameters under different mode
         if (mode != null) {
            if (mode == ElasticityMode.AUTO) {
               if (targetComputeNodeNum != null) {
                  System.out
                        .println("\'targetComputeNodeNum\' ignored. Parameter is not applicable to AUTO elasticity mode.");
               }
            } else {
               if (minComputeNodeNum != null || maxComputeNodeNum != null) {
                  System.out
                        .println("\'minComputeNodeNum\' and \'maxComputeNodeNum\' ignored. Parameters are not applicable to MANUAL elasticity mode.");
               }
            }
         }
      } catch (CliRestException e) {
         if (e.getMessage() != null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_SET_PARAM,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      }
   }

   @CliCommand(value = "cluster resetParam", help = "reset cluster parameters")
   public void resetParam(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "all" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "reset all parameters") final boolean all,
         @CliOption(key = { "elasticityMode" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "reset elasticity mode to MANUAL") final boolean elasticityMode,
         @CliOption(key = { "minComputeNodeNum" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "reset minComputeNodeNum to -1") final boolean minComputeNodeNum,
         @CliOption(key = { "maxComputeNodeNum" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "reset maxComputeNodeNum to -1") final boolean maxComputeNodeNum,
         @CliOption(key = { "targetComputeNodeNum" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "reset targetComputeNodeNum to -1(activate all compute nodes)") final boolean targetComputeNodeNum,
         @CliOption(key = { "ioShares" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "reset disk I/O priorities to LOW") final boolean ioShares) {
      try {
         //validate if the cluster exists
         ClusterRead cluster = restClient.get(clusterName, false);
         if (cluster == null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_RESET_PARAM,
                  Constants.OUTPUT_OP_RESULT_FAIL, "cluster " + clusterName
                        + " does not exist.");
            return;
         }
         /* TODO emma: remove validation from client as client does not have knowledge about if it's compute only node group
                  //validate the node group type
                  if ((elasticityMode || minComputeNodeNum || maxComputeNodeNum || targetComputeNodeNum)
                        && !cluster.validateSetManualElasticity()) {
                     CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                           clusterName, Constants.OUTPUT_OP_RESET_PARAM,
                           Constants.OUTPUT_OP_RESULT_FAIL,
                           Constants.PARAM_SHOULD_HAVE_COMPUTE_ONLY_GROUP);
                     return;
                  }*/

         // reset Auto Elasticity parameters. The default values are:
         // elasticityMode: manual
         // targetComputeNodes: -1
         // minComputeNodes: -1
         // maxComputeNodes: -1
         // ioShares: normal
         ElasticityRequestBody requestBody = new ElasticityRequestBody();
         if (elasticityMode || all) {
            requestBody.setEnableAuto(false);
         }
         if (minComputeNodeNum || all) {
            requestBody.setMinComputeNodeNum(-1);
         }
         if (maxComputeNodeNum || all) {
            requestBody.setMaxComputeNodeNum(-1);
         }
         if (targetComputeNodeNum || all) {
            requestBody.setActiveComputeNodeNum(-1);
         }
         if (ioShares || all) {
            requestBody.setIoPriority(Priority.NORMAL);
         }
         restClient.setParam(cluster, requestBody);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_RESULT_RESET);
      } catch (CliRestException e) {
         if (e.getMessage() != null) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_RESET_PARAM,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
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
               System.out
                     .println("Warning: can't specify option --name and --info at the same time");
               return;
            }
            String fsUrl = hadoopConfiguration.get("fs.default.name");
            String jtUrl = hadoopConfiguration.get("mapred.job.tracker");
            if ((fsUrl == null || fsUrl.length() == 0)
                  && (jtUrl == null || jtUrl.length() == 0)) {
               System.out
                     .println("There is no targeted cluster. Run \"cluster target --name\" command first.");
               return;
            }
            if (targetClusterName != null && targetClusterName.length() > 0) {
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
               } else {
                  noCluster = true;
               }
            } else {
               cluster = restClient.get(name, false);
            }

            if (cluster == null) {
               if (noCluster) {
                  System.out
                        .println("There is no available cluster for targeting.");
               } else {
                  System.out.println("Failed to target cluster: The cluster "
                        + name + " not found");
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
                           String nameNodeIP = nodes.get(0).fetchMgtIp();
                           setNameNode(nameNodeIP);
                           hasHDFS = true;
                        } else {
                           throw new CliRestException("no name node available");
                        }
                     }
                     if (role.equals("hadoop_jobtracker")) {
                        List<NodeRead> nodes = nodeGroup.getInstances();
                        if (nodes != null && nodes.size() > 0) {
                           String jobTrackerIP = nodes.get(0).fetchMgtIp();
                           setJobTracker(jobTrackerIP);
                        } else {
                           throw new CliRestException(
                                 "no job tracker available");
                        }
                     }
                     if (role.equals("hive_server")) {
                        List<NodeRead> nodes = nodeGroup.getInstances();
                        if (nodes != null && nodes.size() > 0) {
                           String hiveServerIP = nodes.get(0).fetchMgtIp();
                           setHiveServerAddress(hiveServerIP);
                           hasHiveServer = true;
                        } else {
                           throw new CliRestException(
                                 "no hive server available");
                        }
                     }
                  }
               }
               if (cluster.getExternalHDFS() != null
                     && !cluster.getExternalHDFS().isEmpty()) {
                  setFsURL(cluster.getExternalHDFS());
                  hasHDFS = true;
               }
               if (!hasHDFS) {
                  setFsURL("");
               }
               if (!hasHiveServer) {
                  this.setHiveServerUrl("");
               }
            }
         }
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_TARGET, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
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

   private void setJobTrackerURL(String jobTrackerUrl) {
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
      // validate the name
      if (name.indexOf("-") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CONFIG, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_CLUSTER
                     + Constants.PARAM_NOT_CONTAIN_HORIZONTAL_LINE);
         return;
      }
      try {
         ClusterRead clusterRead = restClient.get(name, false);
         // build ClusterCreate object
         ClusterCreate clusterConfig = new ClusterCreate();
         clusterConfig.setName(clusterRead.getName());
         ClusterCreate clusterSpec =
               CommandsUtils.getObjectByJsonString(ClusterCreate.class,
                     CommandsUtils.dataFromFile(specFilePath));
         clusterConfig.setNodeGroups(clusterSpec.getNodeGroups());
         clusterConfig.setConfiguration(clusterSpec.getConfiguration());
         clusterConfig.setExternalHDFS(clusterSpec.getExternalHDFS());
         List<String> warningMsgList = new ArrayList<String>();
         List<String> failedMsgList = new ArrayList<String>();
         validateConfiguration(clusterConfig, skipConfigValidation,
               warningMsgList, failedMsgList);
         // add a confirm message for running job
         warningMsgList.add("Warning: "
               + Constants.PARAM_CLUSTER_CONFIG_RUNNING_JOB_WARNING);
         if (!CommandsUtils.showWarningMsg(clusterConfig.getName(),
               Constants.OUTPUT_OBJECT_CLUSTER, Constants.OUTPUT_OP_CONFIG,
               warningMsgList, alwaysAnswerYes)) {
            return;
         }

         if (!failedMsgList.isEmpty()) {
            showFailedMsg(clusterConfig.getName(), Constants.OUTPUT_OP_CONFIG,
                  failedMsgList);
            return;
         }

         restClient.configCluster(clusterConfig);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESULT_CONFIG);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_CONFIG, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
         return;
      }
   }

   @CliCommand(value = "cluster fix", help = "Fix a cluster failure")
   public void fixCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String clusterName,
         @CliOption(key = { "disk" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Recover a disk failure") final boolean isDiskFailure,
         @CliOption(key = { "parallel" }, mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether use parallel way to recovery node or not") final boolean parallel,
         @CliOption(key = { "nodeGroup" }, mandatory = false, help = "The node group name which failure belong to") final String nodeGroupName) {
      try {
         TaskRead taskRead = null;
         if (!isDiskFailure) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  clusterName, Constants.OUTPUT_OP_FIX,
                  Constants.OUTPUT_OP_RESULT_FAIL,
                  Constants.PARAM_SHOULD_SPECIFY_DISK);
            return;
         } else {
            FixDiskRequestBody requestBody = new FixDiskRequestBody();
            requestBody.setParallel(parallel);
            if (!CommandsUtils.isBlank(nodeGroupName)) {
               requestBody.setNodeGroupName(nodeGroupName);
            }
            taskRead = restClient.fixDisk(clusterName, requestBody);
            if (taskRead == null) {
               return;
            }
         }
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_RESULT_FIX);
         System.out.println();
         printClusterFixReport(taskRead, clusterName);
      } catch (Exception e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_FIX,
               Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         return;
      }
   }

   @CliCommand(value = "cluster upgrade", help = "Upgrade an old cluster")
   public void upgradeCluster(
         @CliOption(key = { "name" }, mandatory = true, help = "The cluster name") final String name) {
      // validate the name
      if (name.indexOf("-") != -1) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_UPGRADE, Constants.OUTPUT_OP_RESULT_FAIL,
               Constants.PARAM_CLUSTER
                     + Constants.PARAM_NOT_CONTAIN_HORIZONTAL_LINE);
         return;
      }

      // rest invocation
      try {
         restClient.upgradeCluster(name);
         CommandsUtils.printCmdSuccess(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_RESULT_UPGRADE);
      } catch (CliRestException e) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name,
               Constants.OUTPUT_OP_UPGRADE, Constants.OUTPUT_OP_RESULT_FAIL,
               e.getMessage());
      }
   }

   private void printClusterFixReport(TaskRead taskRead, String clusterName)
         throws Exception {
      ClusterRead cluster = restClient.get(clusterName, true);
      List<NodeGroupRead> nodeGroups = cluster.getNodeGroups();
      List<NodeStatus> succeedNodes = taskRead.getSucceedNodes();
      List<NodeStatus> failedNodes = taskRead.getFailNodes();
      setNodeStatusInfo(succeedNodes, nodeGroups);
      System.out.println("The fixed nodes: " + succeedNodes.size());
      LinkedHashMap<String, List<String>> columnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      columnNamesWithGetMethodNames.put("IP", Arrays.asList("getIp"));
      columnNamesWithGetMethodNames.put("NAME", Arrays.asList("getNodeName"));
      columnNamesWithGetMethodNames.put("STATUS", Arrays.asList("getStatus"));
      CommandsUtils.printInTableFormat(columnNamesWithGetMethodNames,
            succeedNodes.toArray(), Constants.OUTPUT_INDENT);
      System.out.println("The recovery-failed nodes: " + failedNodes.size());
      setNodeStatusInfo(failedNodes, nodeGroups);
      columnNamesWithGetMethodNames.put("Error Message",
            Arrays.asList("getErrorMessage"));
      CommandsUtils.printInTableFormat(columnNamesWithGetMethodNames,
            failedNodes.toArray(), Constants.OUTPUT_INDENT);
   }

   private NodeRead getNodeRead(String nodeName, List<NodeGroupRead> nodeGroups) {
      for (NodeGroupRead nodeGroup : nodeGroups) {
         List<NodeRead> nodes = nodeGroup.getInstances();
         for (NodeRead node : nodes) {
            if (node.getName().equals(nodeName)) {
               return node;
            }
         }
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

   private Set<String> getAllNetworkNames() {
      Set<String> allNetworks = new HashSet<String>();
      NetworkRead[] networks = networkRestClient.getAll(false);
      if (networks != null) {
         for (NetworkRead network : networks) {
            allNetworks.add(network.getName());
         }
      }
      return allNetworks;
   }

   private List<String> getDistroNames(DistroRead[] distroReads) {
      List<String> distroNames = new ArrayList<String>();
      if (distroReads != null) {
         for (int i = 0; i < distroReads.length; i++) {
            distroNames.add(distroReads[i].getName());
         }
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
         System.out.printf(
               "cluster name: %s, distro: %s, topology: %s, status: %s",
               cluster.getName(), cluster.getDistro(), topology,
               cluster.getStatus());
      }
      System.out.println();
      if (cluster.getExternalHDFS() != null
            && !cluster.getExternalHDFS().isEmpty()) {
         System.out.printf("external HDFS: %s\n", cluster.getExternalHDFS());
      }
      LinkedHashMap<String, List<String>> ngColumnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      List<NodeGroupRead> nodegroups = cluster.getNodeGroups();
      if (nodegroups != null) {
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_GROUP_NAME,
               Arrays.asList("getName"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_RUNNING_NODES,
               Arrays.asList("getRunningNodesNum"));
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_IOSHARES,
               Arrays.asList("getStorage", "getShares"));
         try {
            CommandsUtils.printInTableFormat(ngColumnNamesWithGetMethodNames,
                  nodegroups.toArray(), Constants.OUTPUT_INDENT);
         } catch (Exception e) {
            CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
                  cluster.getName(), Constants.OUTPUT_OP_LIST,
                  Constants.OUTPUT_OP_RESULT_FAIL, e.getMessage());
         }
      }
   }

   private void prettyOutputClusterInfo(ClusterRead cluster, boolean detail) {
      TopologyType topology = cluster.getTopologyPolicy();
      String autoElasticityStatus;
      String minComputeNodeNum = cluster.retrieveVhmMinNum();
      String maxComputeNodeNum = cluster.retrieveVhmMaxNum();
      if (cluster.getAutomationEnable() == null) {
         autoElasticityStatus = "N/A";
         minComputeNodeNum = "N/A";
         maxComputeNodeNum = "N/A";
      } else if (cluster.getAutomationEnable()) {
         autoElasticityStatus = "Enable";
      } else {
         autoElasticityStatus = "Disable";
      }

      printSeperator();

      // list cluster level params
      LinkedHashMap<String, String> clusterParams =
            new LinkedHashMap<String, String>();
      clusterParams.put("CLUSTER NAME", cluster.getName());
      clusterParams.put("AGENT VERSION", cluster.getVersion());
      clusterParams.put("APP MANAGER", cluster.getAppManager());
      clusterParams.put("DISTRO", cluster.getDistro());
      if (topology != null && topology != TopologyType.NONE) {
         clusterParams.put("TOPOLOGY", topology.toString());
      }
      clusterParams.put("AUTO ELASTIC", autoElasticityStatus);
      clusterParams.put("MIN COMPUTE NODES NUM", minComputeNodeNum);
      clusterParams.put("MAX COMPUTE NODES NUM", maxComputeNodeNum);
      clusterParams.put("IO SHARES", cluster.getIoShares() == null ? ""
            : cluster.getIoShares().toString());
      clusterParams.put("STATUS", cluster.getStatus() == null ? "" : cluster
            .getStatus().toString());
      if (cluster.getExternalHDFS() != null
            && !cluster.getExternalHDFS().isEmpty()) {
         clusterParams.put("EXTERNAL HDFS", cluster.getExternalHDFS());
      }
      //Burst out
      if (!CommandsUtils.isBlank(cluster.getExternalMapReduce())) {
         clusterParams
               .put("EXTERNAL MAPREDUCE", cluster.getExternalMapReduce());
      }
      for (String key : clusterParams.keySet()) {
         System.out.printf(Constants.OUTPUT_INDENT + "%-26s:"
               + Constants.OUTPUT_INDENT + "%s\n", key, clusterParams.get(key));
      }
      System.out.println();

      LinkedHashMap<String, List<String>> ngColumnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      List<NodeGroupRead> nodegroups = cluster.getNodeGroups();
      if (nodegroups != null) {
         ngColumnNamesWithGetMethodNames.put(
               Constants.FORMAT_TABLE_COLUMN_GROUP_NAME,
               Arrays.asList("getName"));
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
               prettyOutputDetailNodegroups(topology,
                     ngColumnNamesWithGetMethodNames, nodegroups);

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

   private void prettyOutputDetailNodegroups(TopologyType topology,
         LinkedHashMap<String, List<String>> ngColumnNamesWithGetMethodNames,
         List<NodeGroupRead> nodegroups) throws Exception {
      LinkedHashMap<String, List<String>> nColumnNamesWithGetMethodNames =
            new LinkedHashMap<String, List<String>>();
      nColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_NODE_NAME, Arrays.asList("getName"));
      nColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_NODE_VERSION,
            Arrays.asList("getVersion"));
      nColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_HOST,
            Arrays.asList("getHostName"));
      if (topology == TopologyType.RACK_AS_RACK || topology == TopologyType.HVE) {
         nColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_RACK,
               Arrays.asList("getRack"));
      }
      nColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_IP,
            Arrays.asList("fetchMgtIp"));
      nColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_HDFS_IP,
            Arrays.asList("fetchHdfsIp"));
      nColumnNamesWithGetMethodNames.put(
            Constants.FORMAT_TABLE_COLUMN_MAPRED_IP,
            Arrays.asList("fetchMapredIp"));
      nColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_STATUS,
            Arrays.asList("getStatus"));
      nColumnNamesWithGetMethodNames.put(Constants.FORMAT_TABLE_COLUMN_TASK,
            Arrays.asList("getAction"));

      for (NodeGroupRead nodegroup : nodegroups) {
         CommandsUtils.printInTableFormat(ngColumnNamesWithGetMethodNames,
               new NodeGroupRead[] { nodegroup }, Constants.OUTPUT_INDENT);
         List<NodeRead> nodes = nodegroup.getInstances();
         if (nodes != null) {
            LinkedHashMap<String, List<String>> nColumnNamesWithGetMethodNamesClone =
                  (LinkedHashMap<String, List<String>>) nColumnNamesWithGetMethodNames
                        .clone();
            if (!nodes.isEmpty()
                  && (nodes.get(0).getIpConfigs() == null || (!nodes.get(0)
                        .getIpConfigs()
                        .containsKey(NetTrafficType.HDFS_NETWORK) && !nodes
                        .get(0).getIpConfigs()
                        .containsKey(NetTrafficType.MAPRED_NETWORK)))) {
               nColumnNamesWithGetMethodNamesClone
                     .remove(Constants.FORMAT_TABLE_COLUMN_HDFS_IP);
               nColumnNamesWithGetMethodNamesClone
                     .remove(Constants.FORMAT_TABLE_COLUMN_MAPRED_IP);
            }
            System.out.println();
            CommandsUtils.printInTableFormat(
                  nColumnNamesWithGetMethodNamesClone,
                  nodes.toArray(),
                  new StringBuilder().append(Constants.OUTPUT_INDENT)
                        .append(Constants.OUTPUT_INDENT).toString());
         }
         System.out.println();
      }

      CommandsUtils.prettyOutputErrorNode(nodegroups);
   }

   private void prettyOutputClustersInfo(ClusterRead[] clusters, boolean detail) {
      for (ClusterRead cluster : clusters) {
         prettyOutputClusterInfo(cluster, detail);
      }
      printSeperator();
   }

   private void printSeperator() {
      StringBuffer seperator =
            new StringBuffer().append(Constants.OUTPUT_INDENT);
      for (int i = 0; i < Constants.SEPERATOR_LEN; i++) {
         seperator.append("=");
      }
      System.out.println(seperator.toString());
      System.out.println();
   }

   public static void prettyOutputClusterIPs(ClusterRead cluster,
         String filename, String delimeter) throws Exception {
      List<Object> list = new ArrayList<Object>();
      for (NodeGroupRead nodegroup : cluster.getNodeGroups()) {
         List<NodeRead> nodes = nodegroup.getInstances();
         if (nodes != null && !nodes.isEmpty()) {
            for (NodeRead node : nodes) {
               list.add(node.fetchMgtIp());
            }
         }
      }
      CommandsUtils.prettyOutputStrings(list, filename, delimeter);
   }

   private void showFailedMsg(String name, String op, List<String> failedMsgList) {
      // cluster creation failed message.
      StringBuilder failedMsg = new StringBuilder();
      failedMsg.append(Constants.INVALID_VALUE);
      if (failedMsgList.size() > 1) {
         failedMsg.append("s");
      }
      failedMsg.append(".\n");

      failedMsg.append(new ListToStringConverter<String>(failedMsgList, '\n'));

      CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER, name, op,
            Constants.OUTPUT_OP_RESULT_FAIL, failedMsg.toString());
   }

   private void validateConfiguration(ClusterCreate cluster,
         boolean skipConfigValidation, List<String> warningMsgList,
         List<String> failedMsgList) {

      // validate blacklist
      ValidateResult blackListResult = validateBlackList(cluster);
      addBlackListWarning(blackListResult, warningMsgList);

      if (!skipConfigValidation) {
         // validate config type
         AppConfigValidationUtils.validateSupportType(
               cluster.getConfiguration(), warningMsgList);
         // validate whitelist
         ValidateResult whiteListResult = validateWhiteList(cluster);
         addWhiteListWarningOrFailure(cluster.getName(), whiteListResult,
               warningMsgList, failedMsgList);
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
    * Validate a configuration of the cluster at first. Validate configurations
    * of all of node groups then. And merge the failed info which have been
    * producted by validation between cluster level and node group level.
    */
   private ValidateResult validateConfiguration(ClusterCreate cluster,
         ValidationType validationType) {
      ValidateResult validateResult = new ValidateResult();
      // validate cluster level Configuration
      ValidateResult vr = null;
      if (cluster.getConfiguration() != null
            && !cluster.getConfiguration().isEmpty()) {
         vr =
               AppConfigValidationUtils.validateConfig(validationType,
                     cluster.getConfiguration());
         if (vr.getType() != ValidateResult.Type.VALID) {
            validateResult.setType(vr.getType());
            if (!vr.getFailureNames().isEmpty()) {
               validateResult.setFailureNames(vr.getFailureNames());
            }
            if (!vr.getFailureValues().isEmpty()) {
               validateResult.setFailureValues(vr.getFailureValues());
            }
            if (!vr.getNoExistFileNames().isEmpty()) {
               validateResult.setNoExistFileNames(vr.getNoExistFileNames());
            }
         }
      }
      List<String> failureNames = new ArrayList<String>();
      Map<String, List<String>> noExistingFileNamesMap =
            new HashMap<String, List<String>>();
      List<String> failureValues = new ArrayList<String>();
      if (!validateResult.getFailureNames().isEmpty()) {
         failureNames.addAll(validateResult.getFailureNames());
      }
      if (!validateResult.getNoExistFileNames().isEmpty()) {
         noExistingFileNamesMap.putAll(validateResult.getNoExistFileNames());
      }
      if (!validateResult.getFailureValues().isEmpty()) {
         failureValues.addAll(validateResult.getFailureValues());
      }

      // validate nodegroup level Configuration
      for (NodeGroupCreate nodeGroup : cluster.getNodeGroups()) {
         if (nodeGroup.getConfiguration() != null
               && !nodeGroup.getConfiguration().isEmpty()) {
            vr =
                  AppConfigValidationUtils.validateConfig(validationType,
                        nodeGroup.getConfiguration());
            if (vr.getType() != ValidateResult.Type.VALID) {
               //invalid value will take higher priority than invalid name as it will throw failure
               if (validateResult.getType() != ValidateResult.Type.WHITE_LIST_INVALID_VALUE) {
                  validateResult.setType(vr.getType());
               }
               // merge failed names between cluster level and node group level.
               for (String failureName : vr.getFailureNames()) {
                  if (!failureNames.contains(failureName)) {
                     failureNames.add(failureName);
                  }
               }

               // merge failed names between cluster level and node group level.
               for (String failureValue : vr.getFailureValues()) {
                  if (!failureValues.contains(failureValue)) {
                     failureValues.add(failureValue);
                  }
               }

               // merge no existing file names between cluster level and node
               // group level
               for (Entry<String, List<String>> noExistingFileNames : vr
                     .getNoExistFileNames().entrySet()) {
                  String configType = noExistingFileNames.getKey();
                  if (noExistingFileNamesMap.containsKey(configType)) {
                     List<String> noExistingFilesTemp =
                           noExistingFileNames.getValue();
                     List<String> noExistingFiles =
                           noExistingFileNamesMap.get(configType);
                     for (String fileName : noExistingFilesTemp) {
                        if (!noExistingFiles.contains(fileName)) {
                           noExistingFiles.add(fileName);
                        }
                     }
                     noExistingFileNamesMap.put(configType, noExistingFiles);
                  } else {
                     noExistingFileNamesMap.put(configType,
                           noExistingFileNames.getValue());
                  }
               }
            }
         }
      }
      validateResult.setFailureNames(failureNames);
      validateResult.setNoExistFileNames(noExistingFileNamesMap);
      validateResult.setFailureValues(failureValues);
      return validateResult;
   }

   private void addWhiteListWarningOrFailure(final String clusterName,
         ValidateResult whiteListResult, List<String> warningMsgList,
         List<String> failedMsgList) {
      if (whiteListResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_NAME) {
         String noExistingWarningMsg =
               getValidateWarningMsg(whiteListResult.getNoExistFileNames());
         String failureNameWarningMsg =
               getValidateWarningMsg(whiteListResult.getFailureNames(),
                     Constants.PARAM_CLUSTER_NOT_IN_WHITE_LIST_WARNING);
         if (warningMsgList != null) {
            if (!CommonUtil.isBlank(noExistingWarningMsg)) {
               warningMsgList.add(noExistingWarningMsg);
            }
            if (!CommonUtil.isBlank(failureNameWarningMsg)) {
               warningMsgList.add(failureNameWarningMsg);
            }
         }
      } else if (whiteListResult.getType() == ValidateResult.Type.WHITE_LIST_INVALID_VALUE) {
         if (!whiteListResult.getFailureValues().isEmpty()) {
            failedMsgList.addAll(whiteListResult.getFailureValues());
         }
      }
   }

   private void addBlackListWarning(ValidateResult blackListResult,
         List<String> warningList) {
      if (blackListResult.getType() == ValidateResult.Type.NAME_IN_BLACK_LIST) {
         String warningMsg =
               getValidateWarningMsg(blackListResult.getFailureNames(),
                     Constants.PARAM_CLUSTER_IN_BLACK_LIST_WARNING
                           + Constants.PARAM_CLUSTER_NOT_TAKE_EFFECT);
         if (warningList != null) {
            if (!CommandsUtils.isBlank(warningMsg)) {
               warningList.add(warningMsg);
            }
         }
      }
   }

   private String getValidateWarningMsg(List<String> failureNames,
         String warningMsg) {
      StringBuilder warningMsgBuff = new StringBuilder();
      if (failureNames != null && !failureNames.isEmpty()) {
         warningMsgBuff.append("Warning: ");
         for (String failureName : failureNames) {
            warningMsgBuff.append(failureName).append(", ");
         }
         warningMsgBuff.delete(warningMsgBuff.length() - 2,
               warningMsgBuff.length());
         if (failureNames.size() > 1) {
            warningMsgBuff.append(" are ");
         } else {
            warningMsgBuff.append(" is ");
         }
         warningMsgBuff.append(warningMsg);
      }
      return warningMsgBuff.toString();
   }

   private String getValidateWarningMsg(
         Map<String, List<String>> noExistingFilesMap) {
      StringBuilder warningMsgBuff = new StringBuilder();
      if (noExistingFilesMap != null && !noExistingFilesMap.isEmpty()) {
         warningMsgBuff.append("Warning: ");
         for (Entry<String, List<String>> noExistingFilesEntry : noExistingFilesMap
               .entrySet()) {
            List<String> noExistingFileNames = noExistingFilesEntry.getValue();
            for (String noExistingFileName : noExistingFileNames) {
               warningMsgBuff.append(noExistingFileName).append(", ");
            }
            warningMsgBuff.delete(warningMsgBuff.length() - 2,
                  warningMsgBuff.length());
            if (noExistingFileNames.size() > 1) {
               warningMsgBuff.append(" are ");
            } else {
               warningMsgBuff.append(" is ");
            }
            warningMsgBuff.append("not existing in ");
            warningMsgBuff.append(noExistingFilesEntry.getKey() + " scope , ");
         }
         warningMsgBuff.replace(warningMsgBuff.length() - 2,
               warningMsgBuff.length(), ". ");
         warningMsgBuff.append(Constants.PARAM_CLUSTER_NOT_TAKE_EFFECT);
      }
      return warningMsgBuff.toString();
   }

   private boolean validateHAInfo(NodeGroupCreate[] nodeGroups) {
      List<String> haFlagList = Arrays.asList("off", "on", "ft");
      if (nodeGroups != null) {
         for (NodeGroupCreate group : nodeGroups) {
            if (!haFlagList.contains(group.getHaFlag().toLowerCase())) {
               return false;
            }
         }
      }
      return true;
   }

   private TopologyType validateTopologyValue(String clusterName,
         String topology) {
      TopologyType value = null;
      try {
         value = TopologyType.valueOf(topology);
      } catch (IllegalArgumentException ex) {
         CommandsUtils.printCmdFailure(Constants.OUTPUT_OBJECT_CLUSTER,
               clusterName, Constants.OUTPUT_OP_CREATE,
               Constants.OUTPUT_OP_RESULT_FAIL, Constants.INVALID_VALUE + " "
                     + "topologyType=" + topology);
      }
      return value;
   }
}
