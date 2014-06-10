package com.vmware.bdd.software.mgmt.plugin.intf;

import java.util.List;
import java.util.Set;

import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import com.vmware.bdd.software.mgmt.plugin.monitor.ClusterReport;

/**
 * The software manager will be listed in BDE client with name as the
 * UID. User will pick up one software manager during cluster operation. And
 * then all software management requests will be sent to this instance.
 * 
 * Annotation @BeforeClusterConfiguration should be used before cluster creation,
 * to allow infrastructure management finish all tasks
 * 
 * @author line
 * 
 */
public interface SoftwareManager {
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
    * The supported role names, for instance NameNode, Secondary NameNode, etc.
    * The role name will be used to validate user input in cluster spec
    * @return
    */
   Set<String> getSupportedRoles();

   /**
    * Supported Hadoop stack, for instance "CDH 5", "HDP 2.1.1"
    * @return
    */
   List<HadoopStack> getSupportedStacks();

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
   String getSupportedConfigs(HadoopStack stack);

   /**
    * 
    */
   void validateBlueprint(ClusterBlueprint blueprint); 
//   To be decided: if BDE will help to validate the cluster, or leave software manager provide this function
//   void validateScaling();

   /**
    * asynchronous method call.
    * return request id for status query
    * TBD: add parameter
    */
   String createCluster(ClusterBlueprint clusterSpec);

   /**
    * Get task status
    * @param opsId
    * @return
    */
   ClusterReport queryTaskStatus(String opsId);

   /**
    * After cluster is created, user is able to change hadoop cluster
    * configuration with this method.
    */
   String reconfigCluster(ClusterBlueprint clusterSpec); // for cluster config

   String scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes); // for resize node group instance number

   String startCluster(String clusterName); // TBD: how to make sure the hadoop service is not started while VM is started?

   String deleteCluster(String clusterName);

   /**
    * This method will be guaranteed to invoked before BDE invoke cluster stop,
    * allowing plugin to do some clean up
    * 
    * @return
    */
   String onStopCluster(String clusterName);

   /**
    * This method will be guaranteed to invoked before BDE invoke cluster
    * delete, allowing plugin to do some clean up
    * 
    * @return
    */
   String onDeleteCluster(String clusterName);

   // Node level command is prepared for rolling update, e.g. disk fix, scale up cpu/memory/storage
   /**
    * @param clusterName
    * @param instances
    * @return task id
    */
   String decomissionNodes(String clusterName, List<NodeInfo> nodes);

   String comissionNodes(String clusterName, List<NodeInfo> nodes);
   /**
    * The commission nodes method is guaranteed to be invoked before this method
    * is called.
    * 
    * @param clusterName
    * @param instances
    * @return
    */
   String startNodes(String clusterName, List<NodeInfo> nodes);

   String exportBlueprint(String clusterName);

   /**
    * Get current cluster service status, including cluster status, and node status
    * TODO: define cluster query object
    * @param clusterName
    * @return
    */
   String queryClusterStatus(ClusterBlueprint clusterSpec);
}
