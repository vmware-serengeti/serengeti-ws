package com.vmware.bdd.service.impl;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.command.CommandUtil;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.plugin.ironfan.utils.DefaultUtils;
import com.vmware.bdd.service.job.software.ISoftwareManagementTask;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.service.job.software.SoftwareManagementTaskFactory;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.utils.Constants;
import com.vmware.bdd.utils.SyncHostsUtils;

public class SoftwareManagementService {

   private final Logger logger = Logger.getLogger(SoftwareManagementService.class);

   @Autowired
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;
   @Autowired
   private IClusterEntityManager clusterEntityMgr;
   @Autowired
   private ClusterManager clusterManager;
   @Autowired
   private SoftwareManagerCollector softwareManagerCollector;

   public void bootstrapNode(NodeEntity node, String clusterName) {
      logger.info("Start to check host time.");
      ClusterCreate clusterSpec = clusterManager.getClusterSpec(clusterName);

      Set<String> hostnames = new HashSet<String>();
      hostnames.add(node.getHostName());
      SoftwareManager softManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
      SyncHostsUtils.SyncHosts(clusterSpec, hostnames, softManager);

      operateNodes(node.getVmName(), clusterName, ManagementOperation.CONFIGURE);
   }

   public void stopCluster(String clusterName) {
      if (isIronfanCluster(clusterName)) {
         String cmd = Constants.CLUSTER_SERVICES_OPERATION + " " + clusterName + " stop";
         if (!DefaultUtils.exec(cmd)) {
            throw SoftwareManagementPluginException.STOP_CLUSTER_FAILED(Constants.IRONFAN, clusterName, null);
         }
      } else {
         operateCluster(clusterName, ManagementOperation.STOP);
      }
   }

   public void startCluster(String clusterName) {
      operateCluster(clusterName, ManagementOperation.START);
   }

   public void configCluster(String clusterName) {
      SoftwareManager softManager = getSoftwareManager(clusterName);
      if (Constants.IRONFAN.equals(softManager.getName())) {
         operateCluster(clusterName, ManagementOperation.CONFIGURE);
      } else {
         operateCluster(clusterName, ManagementOperation.CONFIGURE);
         operateCluster(clusterName, ManagementOperation.START);
      }
   }

   public void operateCluster(String clusterName, ManagementOperation operation) {
      operateNodes(clusterName, clusterName, operation);
   }

   private void operateNodes(String targetName, String clusterName, ManagementOperation operation) {
      SoftwareManager softManager = getSoftwareManager(clusterName);

      ISoftwareManagementTask task = null;
      if (Constants.IRONFAN.equals(softManager.getName())) {
         task = createThriftCommandTask(targetName, clusterName, operation);
      } else {
         task = createExternalTask(targetName, clusterName, operation);
      }

      try {
         Map<String, Object> ret = task.call();

         if (!(Boolean) ret.get("succeed")) {
            String errorMessage = (String) ret.get("errorMessage");
            throw BddException.UPGRADE(null, errorMessage);
         }
      } catch (Exception e) {
         throw BddException.UPGRADE(e, e.getMessage());
      }
   }

   private ISoftwareManagementTask createExternalTask(String targetName, String clusterName, ManagementOperation operation) {
      SoftwareManager softwareMgr = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
      ClusterBlueprint clusterBlueprint = lockClusterEntityMgr.getClusterEntityMgr().toClusterBluePrint(clusterName);
      return SoftwareManagementTaskFactory.createExternalMgtTask(targetName, operation,
            clusterBlueprint, null, lockClusterEntityMgr, softwareMgr, null);
   }

   private ISoftwareManagementTask createThriftCommandTask(String targetName, String clusterName, ManagementOperation operation) {
      // get command work directory
      File workDir = CommandUtil.createWorkDir((int) (Math.random() * 1000));
      // write cluster spec file
      String specFilePath = null;
      File specFile = clusterManager.writeClusterSpecFile(clusterName, workDir, true);
      specFilePath = specFile.getAbsolutePath();

      ISoftwareManagementTask task = SoftwareManagementTaskFactory.createThriftTask(targetName, specFilePath, null, operation,
            lockClusterEntityMgr);
      return task;
   }

   private SoftwareManager getSoftwareManager(String clusterName) {
      return softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
   }

   private boolean isIronfanCluster(String clusterName) {
      SoftwareManager softManager = getSoftwareManager(clusterName);
      return Constants.IRONFAN.equals(softManager.getName());
   }
}
