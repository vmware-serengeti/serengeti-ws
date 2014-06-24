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
import java.util.Set;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
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

   boolean echo() throws SoftwareManagementPluginException;

   HealthStatus getStatus() throws SoftwareManagementPluginException;

   /**
    * The supported role names, for instance NameNode, Secondary NameNode, etc.
    * The role name will be used to validate user input in cluster spec
    * 
    * @return
    */
   Set<String> getSupportedRoles() throws SoftwareManagementPluginException;

   /**
    * Supported Hadoop stack, for instance "CDH 5", "HDP 2.1.1"
    * 
    * @return
    */
   List<HadoopStack> getSupportedStacks()
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
    */
   String getSupportedConfigs(HadoopStack stack)
         throws SoftwareManagementPluginException;

   boolean validateBlueprint(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException;

   boolean validateRoles(ClusterBlueprint blueprint, List<String> roles)
         throws SoftwareManagementPluginException;

   /**
    * Sync call to create hadoop software Plugin should should update
    * ClusterOperationReports to notify operation status change for this
    * cluster, otherwise, client cannot get information in this long operation
    * time
    * 
    * @param clusterSpec
    * @return
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
   boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes, ClusterReportQueue reports)
         throws SoftwareManagementPluginException; // for resize node group instance number

   /**
    * Sync call to start cluster Plugin should update ClusterOperationReports to
    * notify operation status change for this cluster, otherwise, client cannot
    * get information in this long operation time
    */
   boolean startCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   /**
    * Sync call to delete cluster Plugin should update ClusterOperationReports
    * to notify operation status change for this cluster, otherwise, client
    * cannot get information in this long operation time
    */
   boolean deleteCluster(String clusterName, ClusterReportQueue reports)
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
   boolean onStopCluster(String clusterName, ClusterReportQueue reports)
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
   boolean onDeleteCluster(String clusterName, ClusterReportQueue reports)
         throws SoftwareManagementPluginException;

   // Node level command is prepared for rolling update, e.g. disk fix, scale up cpu/memory/storage
   /**
    * Sync call Plugin should update ClusterOperationReports to notify operation
    * status change for this cluster, otherwise, client cannot get information
    * in this long operation time
    * 
    * @param clusterName
    * @param instances
    * @return task id
    */
   boolean decomissionNodes(String clusterName, List<NodeInfo> nodes,
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
    * @param instances
    * @return
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
    * @param clusterName
    * @return
    */
   ClusterReport queryClusterStatus(ClusterBlueprint blueprint);

   /**
    * Validate if this node group is scalable or not. Return list of unsupported
    * role names
    * 
    * @param group
    * @return
    */
   List<String> validateScaling(NodeGroupInfo group)
         throws SoftwareManagementPluginException;

   /**
    * Plugin has a chance to update infrastructure setting here. Specifically,
    * plugin can set default
    * 
    * @param blueprint
    */
   void updateInfrastructure(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException;
}
