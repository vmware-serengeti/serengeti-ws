package com.vmware.bdd.service.job.vm;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.manager.JobManager;
import com.vmware.bdd.manager.ShrinkException;
import com.vmware.bdd.manager.SoftwareManagerCollector;
import com.vmware.bdd.manager.intf.IClusterEntityManager;
import com.vmware.bdd.service.job.JobConstants;
import com.vmware.bdd.service.job.JobExecutionStatusHolder;
import com.vmware.bdd.service.job.TrackableTasklet;
import com.vmware.bdd.software.mgmt.plugin.exception.SoftwareManagementPluginException;
import com.vmware.bdd.software.mgmt.plugin.intf.SoftwareManager;
import com.vmware.bdd.software.mgmt.plugin.model.ClusterBlueprint;
import com.vmware.bdd.software.mgmt.plugin.model.NodeInfo;
import org.apache.log4j.Logger;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Created by qjin on 11/15/14.
 */
public class DecommissionSingleVMStep extends TrackableTasklet {
   private static final Logger logger = Logger
         .getLogger(DecommissionSingleVMStep.class);
   private IClusterEntityManager clusterEntityManager;
   private SoftwareManagerCollector softwareManagerCollector;
   private JobManager jobManager;

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) throws Exception {
      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      String nodeGroupName = getJobParameters(chunkContext).getString(JobConstants.GROUP_NAME_JOB_PARAM);
      String nodeName = getJobParameters(chunkContext).getString(JobConstants.SUB_JOB_NODE_NAME);
      SoftwareManager softwareManager = softwareManagerCollector.getSoftwareManagerByClusterName(clusterName);
      NodeInfo nodeInfo = new NodeInfo();
      nodeInfo.setName(nodeName);
      //TODO(qjin): reportQueue?
      try {
         logger.info("decomissioning " + nodeName);
         putIntoJobExecutionContext(chunkContext, JobConstants.NODE_OPERATION_SUCCESS, false);
         ClusterBlueprint blueprint = clusterEntityManager.toClusterBluePrint(clusterName);
         softwareManager.decomissionNode(blueprint, nodeGroupName, nodeName, null);
         putIntoJobExecutionContext(chunkContext,JobConstants.NODE_OPERATION_SUCCESS, true);
      } catch (Exception e) {
         logger.error("Got exception when decommissioning " + nodeName + " :", e);
         logger.info("Recommissioning " + nodeName);
         softwareManager.recomissionNode(clusterName, nodeInfo, null);
         //TODO(qjin): can be improved
         JobExecution jobExecution = jobManager.getJobExplorer().getJobExecution(getJobExecutionId(chunkContext));
         jobExecution.setStatus(BatchStatus.FAILED);
         throw ShrinkException.DECOMISSION_FAILED(e, e.getMessage());
      }
      return RepeatStatus.FINISHED;
   }

   public IClusterEntityManager getClusterEntityManager() {
      return clusterEntityManager;
   }

   public void setClusterEntityManager(IClusterEntityManager clusterEntityManager) {
      this.clusterEntityManager = clusterEntityManager;
   }

   public SoftwareManagerCollector getSoftwareManagerCollector() {
      return softwareManagerCollector;
   }

   public void setSoftwareManagerCollector(SoftwareManagerCollector softwareManagerCollector) {
      this.softwareManagerCollector = softwareManagerCollector;
   }

   public JobManager getJobManager() {
      return jobManager;
   }

   public void setJobManager(JobManager jobManager) {
      this.jobManager = jobManager;
   }
}
