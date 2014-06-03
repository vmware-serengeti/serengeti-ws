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
    * Supported Hadoop version, for instance "CDH 5", "HDP 2.1.1"
    * @return
    */
   List<String> getSupportedVersions();

   /**
    * Supported configuration for specified version. The returned value will be used to validate 
    * user input configuration before actually provisioning VM
    * @param version
    * @return a Json string, with following format. For each single configuration, should provide
    * property name, type, default value
    * [
    *   {
    *       "hadoop": { // for hadoop configuration
    *             "core-site.xml": [  // configuration in core-site.xml file
    *                     {
    *                               "name": "net.topology.nodegroup.aware",
    *                               "type": "boolean",
    *                               "default": true,
    *                     },
    *             ],
    *             "yarn-site.xml": [ // configuration in yarn-site.xml file
    *             ],
    *             "fair-scheduler.xml": [ // configuration in fair-scheduler xml file
    *                     {
    *                               "name": "text"
    *                               "type": "String",
    *                               "default": "",
    *                     }
    *             ]
    *   }
    *   {
    *       "zookeeper": {
    *             "java.env": [ 
    *                     {
    *                               "name": "JVMFLAGS"
    *                               "type": "boolean",
    *                               "default": true,
    *                     }
    *             ],
    *             "log4j.properties": [
    *                     {
    *                               "name": "zookeeper.root.logger"
    *                               "type": "String",
    *                     }
    *            ]
    *       }
    *  }
    * 
    */
   String getSupportedConfigs(String version);

//   void validateCluster(); 
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
   String decomissionNodes(String clusterName, List<Instances> instances);
   String comissionNodes(String clusterName, List<Instances> instances);
   /**
    * The commission nodes method is guaranteed to be invoked before this method is called.
    * @param clusterName
    * @param instances
    * @return
    */
   String startNodes(String clusterName, List<Instances> instances);
}
