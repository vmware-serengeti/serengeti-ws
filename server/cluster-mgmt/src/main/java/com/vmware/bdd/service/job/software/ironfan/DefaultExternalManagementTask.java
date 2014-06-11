package com.vmware.bdd.service.job.software.ironfan;

import com.vmware.bdd.manager.intf.ILockedClusterEntityManager;
import com.vmware.bdd.service.job.StatusUpdater;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;

import java.util.HashMap;
import java.util.Map;

import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManagerFactory;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.PluginInfo;
import org.apache.log4j.Logger;

/**
 * Author: Xiaoding Bian
 * Date: 6/11/14
 * Time: 2:58 PM
 */
public class DefaultExternalManagementTask implements ISoftwareManagementTask{

   private static final Logger logger = Logger.getLogger(DefaultExternalManagementTask.class);
   private String targetName;
   private ManagementOperation managementOperation;
   private ClusterBlueprint clusterBlueprint;
   private PluginInfo pluginInfo;
   private StatusUpdater statusUpdater;
   private ILockedClusterEntityManager lockedClusterEntityManager;

   public DefaultExternalManagementTask(String targetName, ManagementOperation managementOperation,
         ClusterBlueprint clusterBlueprint, PluginInfo pluginInfo, StatusUpdater statusUpdater,
         ILockedClusterEntityManager lockedClusterEntityManager) {
      this.targetName = targetName;
      this.managementOperation = managementOperation;
      this.clusterBlueprint = clusterBlueprint;
      this.pluginInfo = pluginInfo;
      this.statusUpdater = statusUpdater;
      this.lockedClusterEntityManager = lockedClusterEntityManager;
   }

   @Override
   public Map<String, Object> call() throws Exception {

      Map<String, Object> result = new HashMap<String, Object>();
      SoftwareManager softwareManager = SoftwareManagerFactory.getSoftwareManager(pluginInfo);

      // TODO: start software operation monitor

      boolean success = false;
      try {
         switch(managementOperation) {
            case CREATE:
               // TODO: set targetName
               success = softwareManager.createCluster(clusterBlueprint);
               break;
            case CONFIGURE:
               success = softwareManager.reconfigCluster(clusterBlueprint);
               break;
            default:
               success = true;
         }

      } catch (Exception e) {

      } finally {
         // TODO: stop monitor
      }

      result.put("succeed", true);

      return result;
   }
}
