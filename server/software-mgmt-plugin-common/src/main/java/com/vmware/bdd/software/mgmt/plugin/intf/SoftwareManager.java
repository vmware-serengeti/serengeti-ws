package com.vmware.bdd.software.mgmt.plugin.intf;

import java.util.List;
import java.util.Set;

/**
 * The registered software manager will be listed in BDE client with name as the
 * UID. User will pick up one software manager during cluster operation. And
 * then all software management requests will be sent to this instance.
 * 
 * The software manager implementation should register itself to
 * SoftwareManagerCollector during instance initialization.
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
    * The supported role names, for instance NameNode, Secondary NameNode, etc.
    * The role name will be used to validate user input in cluster spec
    * @return
    */
   Set<String> getSupportedRoles();

   /**
    * Supported Hadoop stack, for instance "CDH 5", "HDP 2.1.1"
    * @return
    */
   List<String> getSupportedStacks();

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
   String getSupportedConfigs(String stack);

   /**
    * 
    */
   void validateBlueprint(); 
//   To be decided: if BDE will help to validate the cluster, or leave software manager provide this function
//   void validateScaling();

   /**
    * asynchronous method call.
    * return task id for status query
    * TBD: add parameter
    */
   String createCluster();

   /**
    * Get task status
    * @param taskId
    * @return json string contains all node status in detail
    * TBD: define return string format, or define a status object, to avoid non-formated message
    */
   String queryTaskStatus(String taskId);
// TBD: define parameter
   /**
    * After cluster is created, user is able to change hadoop cluster configuration 
    * with this method.
    */
   String reconfigCluster();
   String scaleOutCluster(); // for resize node group instance number
   String startCluster(); // TBD: how to make sure the hadoop service is not started while VM is started?
   String deleteCluster();
   /**
    * This method will be guaranteed to invoked before BDE invoke cluster stop, allowing plugin
    * to do some clean up
    * @return
    */
   String onStopCluster();
   /**
    * This method will be guaranteed to invoked before BDE invoke cluster delete, allowing plugin
    * to do some clean up
    * @return
    */
   String onDeleteCluster();

   // Node level command is prepared for rolling update, e.g. disk fix, scale up cpu/memory/storage
   /**
    * @param clusterName
    * @param instances
    * @return task id
    */
//   String decomissionNodes(String clusterName, List<Instances> instances);
//   String comissionNodes(String clusterName, List<Instances> instances);
   /**
    * The commission nodes method is guaranteed to be invoked before this method is called.
    * @param clusterName
    * @param instances
    * @return
    */
//   String startNodes(String clusterName, List<Instances> instances);
   
   // Do we need one separate blueprint concept? Or we'd use one to one mapping between cluster and blueprint
//   exportBlueprint(String clusterName);
}
