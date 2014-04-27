package com.vmware.bdd.software.mgmt.plugin.intf;

import java.util.List;
import java.util.Set;

import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.HadoopStack;
import com.vmware.bdd.software.mgmt.plugin.model.NodeGroupInfo;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;

import javax.security.auth.login.Configuration;

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
      Connected,
      Disconnected,
      Unknown
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
   boolean echo();

   HealthStatus getStatus();

   /**
    * The supported role names, for instance NameNode, Secondary NameNode, etc.
    * The role name will be used to validate user input in cluster spec
    *
    * @return
    */
   Set<String> getSupportedRoles();

   /**
    * Supported Hadoop stack, for instance "CDH 5", "HDP 2.1.1"
    *
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
   boolean validateBlueprint(ClusterBlueprint blueprint)
         throws SoftwareManagementPluginException;

   boolean validateRoles(ClusterBlueprint blueprint, List<String> roles)
         throws SoftwareManagementPluginException;

   boolean validateCliConfigurations(ClusterBlueprint blueprint)
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
   boolean createCluster(ClusterBlueprint blueprint) throws Exception;

   /**
    * After cluster is created, user is able to change hadoop cluster
    * configuration with this method. Sync call Plugin should update
    * ClusterOperationReports to notify operation status change for this
    * cluster, otherwise, client cannot get information in this long operation
    * time
    */
   boolean reconfigCluster(ClusterBlueprint blueprint); // for cluster config

   /**
    * Sync call to add more nodes into cluster Plugin should update
    * ClusterOperationReports to notify operation status change for this
    * cluster, otherwise, client cannot get information in this long operation
    * time
    */
   boolean scaleOutCluster(String clusterName, NodeGroupInfo group,
         List<NodeInfo> addedNodes); // for resize node group instance number

   /**
    * Sync call to start cluster Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    */
   boolean startCluster(String clusterName); // TBD: how to make sure the hadoop service is not started while VM is started?

   /**
    * Sync call to delete cluster Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    */
   boolean deleteCluster(String clusterName);

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
   boolean onStopCluster(String clusterName);

   /**
    * This method will be guaranteed to invoked before BDE invoke cluster
    * delete, allowing plugin to do some clean up
    *
    * Sync call Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    *
    * @return
    */
   boolean onDeleteCluster(String clusterName);

   // Node level command is prepared for rolling update, e.g. disk fix, scale up cpu/memory/storage
   /**
    * Sync call Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    *
    * @param clusterName
    * @param instances
    * @return task id
    */
   boolean decomissionNodes(String clusterName, List<NodeInfo> nodes);

   /**
    * Sync call Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    *
    * @param clusterName
    * @param nodes
    * @return
    */
   boolean comissionNodes(String clusterName, List<NodeInfo> nodes);

   /**
    * The commission nodes method is guaranteed to be invoked before this method
    * is called.
    *
    * Sync call Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    *
    * @param clusterName
    * @param instances
    * @return
    */
   boolean startNodes(String clusterName, List<NodeInfo> nodes);

   /**
    * Sync call Plugin should update ClusterOperationReports to notify
    * operation status change for this cluster, otherwise, client cannot get
    * information in this long operation time
    *
    * @param clusterName
    * @param nodes
    * @return
    */
   boolean stopNodes(String clusterName, List<NodeInfo> nodes);

   String exportBlueprint(String clusterName);

   /**
    * Get current cluster service status, including cluster status, and node
    * status TODO: define cluster query object
    *
    * @param clusterName
    * @return
    */
   String queryClusterStatus(ClusterBlueprint blueprint);
}
