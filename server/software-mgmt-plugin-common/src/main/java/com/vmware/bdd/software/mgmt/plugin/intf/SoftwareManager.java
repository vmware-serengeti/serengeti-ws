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
package com.vmware.bdd.software.mgmt.plugin.intf;

import java.util.List;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.exception.ValidationException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReportQueue;

/**
 * The software manager will be listed in BDE client with name as the UID. User
 * will pick up one software manager during cluster operation. And then all
 * software management requests will be sent to this instance.
 *
 * Annotation @BeforeClusterConfiguration should be used before cluster
 * creation, to allow infrastructure management finish all tasks
 *
 * @author line
 *
 */
public interface SoftwareManager {
   public enum HealthStatus {
      Connected, Disconnected, Unknown
   }

   /**
    * Plugin name, which should be unique in BDE scope
    *
    * @return
    */
   String getName();

   /**
    * Plugin description, which will be shown through BDE CLI/UI.
    *
    * @return
    */
   String getDescription();

   /**
    * @return the plugin type
    */
   String getType();

   /**
    * @return the software manager version
    */
   String getVersion();

   boolean echo() throws SoftwareManagementPluginException;

   boolean validateServerVersion() throws SoftwareManagementPluginException;

   HealthStatus getStatus() throws SoftwareManagementPluginException;

   /**
    * Supported Hadoop stack, for instance "CDH 5", "HDP 2.1.1"
    *
    * @return
    */
   List<HadoopStack> getSupportedStacks()
         throws SoftwareManagementPluginException;

   HadoopStack getDefaultStack()
         throws SoftwareManagementPluginException;
   /**
    * Supported configuration for specified stack. The returned value can be used to config created
    * cluster through custom cluster specification
    * @param stack, for instance "CDH 5", "HDP 2.1.1"
    * @return a Json string, with correct configuration format.
    * This format of configuration string, will be used to configure cluster.
    * For each single configuration, should provide property name, default value
    * For BDE software management tool, here is the sample configuration format
    *   {
    *       "hadoop": { // for hadoop configuration
    *             "core-site.xml": [  // configuration in core-site.xml file
    *                     {
    *                        // check for all settings at http://hadoop.apache.org/docs/stable/core-default.html
    *                        // note: any value (int, float, boolean, string) must be enclosed in double quotes and here is a sample:
    *                        // "io.file.buffer.size": "4096"
    *                     },
    *             ],
    *             "yarn-site.xml": [ // configuration in yarn-site.xml file
    *                       // check for all settings at http://hadoop.apache.org/docs/stable/hdfs-default.html
    *             ]
    *   }
    *
    *
    */
   String getSupportedConfigs(HadoopStack stack)
         throws SoftwareManagementPluginException;

   boolean validateBlueprint(ClusterBlueprint blueprint)
         throws ValidationException;

   /**
    * Sync call to create hadoop software Plugin should should update
    * ClusterOperationReports to notify operation status change for this
    * cluster, otherwise, client cannot get information in this long operation
    * time
    *
    * @param blueprint
    * @param reports
    * @return
    * @throws SoftwareManagementPluginException
    */
   boolean createCluster(ClusterBlueprint blueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   /**
    * After cluster is created, user is able to change hadoop cluster
    * configuration with this method. Sync call Plugin should update
    * ClusterOperationReports to notify operation status change for this
    * cluster, otherwise, client cannot get information in this long operation
    * time
    */
   boolean reconfigCluster(ClusterBlueprint blueprint,
         ClusterReportQueue reports) throws SoftwareManagementPluginException; // for cluster config

   /**
    * Sync call to add more nodes into cluster Plugin should update
    * ClusterOperationReports to notify operation status change for this
    * cluster, otherwise, client cannot get information in this long operation
    * time
    */
   boolean scaleOutCluster(ClusterBlueprint blueprint, List<String> addedNodeNames,
         ClusterReportQueue reports)
         throws SoftwareManagementPluginException; // for resize node group instance number

   /**
    * Sync call to start cluster Plugin should update ClusterOperationReports to
    * notify operation status change for this cluster, otherwise, client cannot
    * get information in this long operation time
    */
   boolean startCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   /**
    * Sync call to delete cluster Plugin should update ClusterOperationReports
    * to notify operation status change for this cluster, otherwise, client
    * cannot get information in this long operation time
    */
   boolean deleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   /**
    * This method will be guaranteed to be invoked before BDE invoke cluster
    * stop, allowing plugin to do some clean up
    *
    * Sync call Plugin should should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    *
    * @return
    */
   boolean onStopCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   /**
    * This method will be guaranteed to invoked before BDE invoke cluster
    * delete, allowing plugin to do some clean up
    *
    * Sync call Plugin should update ClusterOperationReports to notify operation
    * status change for this cluster, otherwise, client cannot get information
    * in this long operation time
    *
    * @return
    */
   boolean onDeleteCluster(ClusterBlueprint clusterBlueprint, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   /**
    * This method will be invoked before BDE delete VMs
    * @param blueprint
    * @param nodeNames
    * @return
    * @throws SoftwareManagementPluginException
    */
   boolean onDeleteNodes(ClusterBlueprint blueprint, List<String> nodeNames) throws SoftwareManagementPluginException;

   // Node level command is prepared for rolling update, e.g. disk fix, scale up cpu/memory/storage
   /**
    * Sync call Plugin should update ClusterOperationReports to notify operation
    * status change for this cluster, otherwise, client cannot get information
    * in this long operation time
    *
    * @param clusterName
    * @param nodes
    * @param reports
    * @return
    * @throws SoftwareManagementPluginException
    */
   boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException;

   boolean decomissionNode(ClusterBlueprint blueprint, String nodeGroupName, String nodeName, ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException;

   boolean recomissionNode(String clusterName, NodeInfo node, ClusterReportQueue reportQueue)
         throws SoftwareManagementPluginException;
   /**
    * Sync call Plugin should update ClusterOperationReports to notify operation
    * status change for this cluster, otherwise, client cannot get information
    * in this long operation time
    *
    * @param clusterName
    * @param nodes
    * @return
    */
   boolean comissionNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException;

   /**
    * The commission nodes method is guaranteed to be invoked before this method
    * is called.
    *
    * Sync call Plugin should update ClusterOperationReports to notify operation
    * status change for this cluster, otherwise, client cannot get information
    * in this long operation time
    *
    * @param clusterName
    * @param nodes
    * @param reports
    * @return
    * @throws SoftwareManagementPluginException
    */
   boolean startNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException;

   /**
    * Sync call Plugin should update ClusterOperationReports to notify operation
    * status change for this cluster, otherwise, client cannot get information
    * in this long operation time
    *
    * @param clusterName
    * @param nodes
    * @return
    */
   boolean stopNodes(String clusterName, List<NodeInfo> nodes,
         ClusterReportQueue reports) throws SoftwareManagementPluginException;

   String exportBlueprint(String clusterName)
         throws SoftwareManagementPluginException;

   /**
    * Get current cluster service status, including cluster status, and node
    * status TODO: define cluster query object
    *
    * @param blueprint
    * @return
    */
   ClusterReport queryClusterStatus(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException;

   /**
    * Validate if this node group is scalable or not. Return list of unsupported
    * role names
    *
    * @param group
    * @return
    */
   List<String> validateRolesForScaleOut(NodeGroupInfo group)
         throws SoftwareManagementPluginException;

   void validateRolesForShrink(NodeGroupInfo groupInfo)
         throws SoftwareManagementPluginException;
   /**
    * Plugin has a chance to update infrastructure setting here. Specifically,
    * plugin can set default disk type
    *
    * @param blueprint
    */
   void updateInfrastructure(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException;

   boolean hasHbase(ClusterBlueprint blueprint);

   boolean hasMgmtRole(List<String> roles);

   boolean isComputeOnlyRoles(List<String> roles);

   boolean hasComputeMasterGroup(ClusterBlueprint blueprint);

   /**
    * This is the infrastructure requirement comes from software manager for one
    * specific node group. It generally happens for some special roles
    * supported. E.g. if only zookeeper role is installed in one node group,
    * only two data disk can be leveraged.
    *
    * @param group
    * @return
    */
   boolean twoDataDisksRequired(NodeGroupInfo group);
}
