package com.vmware.bdd.service.job;

import com.vmware.bdd.apitypes.ClusterCreate;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.manager.ClusterManager;
import com.vmware.bdd.manager.intf.IExclusiveLockedClusterEntityManager;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.utils.SyncHostsUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author: Xiaoding Bian
 * Date: 6/3/14
 * Time: 2:08 PM
 */
public class CmManagementStep extends TrackableTasklet {

   private static final Logger logger = Logger
         .getLogger(CmManagementStep.class);
   private ClusterManager clusterManager;
   private ManagementOperation managementOperation;
   private IExclusiveLockedClusterEntityManager lockClusterEntityMgr;

   public IExclusiveLockedClusterEntityManager getLockClusterEntityMgr() {
      return lockClusterEntityMgr;
   }

   @Autowired
   public void setLockClusterEntityMgr(
         IExclusiveLockedClusterEntityManager lockClusterEntityMgr) {
      this.lockClusterEntityMgr = lockClusterEntityMgr;
   }

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext,
         JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {

      String targetName =
            getJobParameters(chunkContext).getString(
                  JobConstants.TARGET_NAME_JOB_PARAM);
      String clusterName =
            getJobParameters(chunkContext).getString(
                  JobConstants.CLUSTER_NAME_JOB_PARAM);
      if (targetName == null) {
         targetName = clusterName;
      }
      String jobName = chunkContext.getStepContext().getJobName();
      logger.info("target : " + targetName + ", operation: "
            + managementOperation + ", jobname: " + jobName);

      // Only check host time for configure (config, start, disk fix, scale up)
      // operation and create (resume only) operation
      if (ManagementOperation.CONFIGURE.equals(managementOperation) ||
            JobConstants.RESUME_CLUSTER_JOB_NAME.equals(jobName)) {
         logger.info("Start to check host time.");
         List<NodeEntity> nodes = lockClusterEntityMgr.getClusterEntityMgr().findAllNodes(clusterName);
         Set<String> hostnames = new HashSet<String>();
         for (NodeEntity node : nodes) {
            hostnames.add(node.getHostName());
         }
         ClusterCreate clusterSpec = clusterManager.getClusterSpec(clusterName);

         SyncHostsUtils.SyncHosts(clusterSpec, hostnames);
      }

      StatusUpdater statusUpdater =
            new DefaultStatusUpdater(jobExecutionStatusHolder,
                  getJobExecutionId(chunkContext));

      if (false) {
         String errorMessage = "errorMessage";
         putIntoJobExecutionContext(chunkContext,
               JobConstants.CURRENT_ERROR_MESSAGE, errorMessage);
         throw TaskException.EXECUTION_FAILED(errorMessage);
      }

      return RepeatStatus.FINISHED;
   }

   public ClusterManager getClusterManager() {
      return clusterManager;
   }

   public void setClusterManager(ClusterManager clusterManager) {
      this.clusterManager = clusterManager;
   }

   /**
    * @return the managementOperation
    */
   public ManagementOperation getManagementOperation() {
      return managementOperation;
   }

   /**
    * @param managementOperation
    *           the managementOperation to set
    */
   public void setManagementOperation(ManagementOperation managementOperation) {
      this.managementOperation = managementOperation;
   }
}
